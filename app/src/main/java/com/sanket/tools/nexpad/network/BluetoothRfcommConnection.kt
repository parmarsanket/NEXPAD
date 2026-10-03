package com.sanket.tools.nexpad.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Ultra-low latency, zero-allocation Bluetooth Classic RFCOMM Transport.
 * Inherits stream workers, framing, coalescing, and RTT/jitter computation from BaseStreamConnection.
 */
class BluetoothRfcommConnection(private val context: Context) : BaseStreamConnection() {

    companion object {
        private const val TAG = "NEXPAD_BT"
        private const val NEXPAD_BT_UUID_STRING = "457a7be2-36c1-4b2e-a342-e1d90479d28a"
        private const val STANDARD_SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private var bluetoothSocket: BluetoothSocket? = null

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connect(address: String, port: Int) {
        withContext(Dispatchers.IO) {
            disconnect()

            onStatusChanged?.invoke("Connecting to Bluetooth PC ($address)...")
            Log.d(TAG, "Attempting Bluetooth RFCOMM connection to address: $address")

            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            val adapter = bluetoothManager?.adapter

            if (adapter == null || !adapter.isEnabled) {
                val msg = "Bluetooth is disabled or unavailable"
                Log.e(TAG, msg)
                onStatusChanged?.invoke(msg)
                return@withContext
            }

            try {
                try {
                    adapter.cancelDiscovery()
                } catch (_: SecurityException) {
                    // BLUETOOTH_SCAN not required for connecting to known paired device
                } catch (_: Exception) {}

                val device: BluetoothDevice = adapter.getRemoteDevice(address)

                var socket: BluetoothSocket? = null
                var lastException: Exception? = null

                // Tier 1: Insecure RFCOMM with NEXPAD custom UUID
                try {
                    Log.d(TAG, "Trying Tier 1: createInsecureRfcommSocketToServiceRecord(NEXPAD_UUID)")
                    socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(NEXPAD_BT_UUID_STRING))
                    socket.connect()
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Tier 1 connection failed: ${e.message}. Trying Tier 2 (Secure)...")
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }

                // Tier 2: Secure RFCOMM with NEXPAD custom UUID
                if (socket == null || !socket.isConnected) {
                    try {
                        Log.d(TAG, "Trying Tier 2: createRfcommSocketToServiceRecord(NEXPAD_UUID)")
                        socket = device.createRfcommSocketToServiceRecord(UUID.fromString(NEXPAD_BT_UUID_STRING))
                        socket.connect()
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Tier 2 connection failed: ${e.message}. Trying Tier 3 (Standard SPP)...")
                        try { socket?.close() } catch (_: Exception) {}
                        socket = null
                    }
                }

                // Tier 3: Insecure RFCOMM with standard SPP UUID
                if (socket == null || !socket.isConnected) {
                    try {
                        Log.d(TAG, "Trying Tier 3: createInsecureRfcommSocketToServiceRecord(SPP_UUID)")
                        socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(STANDARD_SPP_UUID_STRING))
                        socket.connect()
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Tier 3 connection failed: ${e.message}. Trying Tier 4 (Reflection Port)...")
                        try { socket?.close() } catch (_: Exception) {}
                        socket = null
                    }
                }

                // Tier 4: Direct Channel Reflection fallback (bypasses SDP)
                if (socket == null || !socket.isConnected) {
                    try {
                        val targetChannel = if (port > 0) port else 1
                        Log.d(TAG, "Trying Tier 4: reflection createInsecureRfcommSocket(channel $targetChannel)")
                        val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, targetChannel) as BluetoothSocket
                        socket.connect()
                    } catch (e: Exception) {
                        lastException = e
                        Log.e(TAG, "Tier 4 reflection connection failed: ${e.message}")
                        try { socket?.close() } catch (_: Exception) {}
                        socket = null
                    }
                }

                if (socket == null || !socket.isConnected) {
                    throw lastException ?: IOException("Failed to establish Bluetooth RFCOMM connection")
                }

                bluetoothSocket = socket

                // Send initial in-band handshake packet so Desktop receives the friendly device name
                try {
                    val modelName = android.os.Build.MODEL ?: "Android Device"
                    val nameBytes = modelName.toByteArray(Charsets.UTF_8)
                    val safeLen = nameBytes.size.coerceAtMost(255)
                    val handshake = ByteArray(3 + safeLen)
                    handshake[0] = NexpadProtocol.PACKET_TYPE_CONNECT
                    handshake[1] = 3 // ConnectionType = 3 (Bluetooth)
                    handshake[2] = safeLen.toByte()
                    System.arraycopy(nameBytes, 0, handshake, 3, safeLen)
                    socket.outputStream.write(handshake)
                    socket.outputStream.flush()
                    Log.d(TAG, "Sent Bluetooth CONNECT handshake with model: $modelName")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send initial BT handshake: ${e.message}")
                }

                startStreamWorkers(socket.inputStream, socket.outputStream, "BT")

                val resolvedPcName = try { device.name } catch (_: SecurityException) { null } ?: "Windows PC"
                onServerNameResolved?.invoke(resolvedPcName)
                onStatusChanged?.invoke("Connected to $resolvedPcName via Bluetooth")
                Log.d(TAG, "Connected to Bluetooth PC $resolvedPcName ($address) successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Bluetooth PC", e)
                onStatusChanged?.invoke("Bluetooth connection failed: ${e.message}")
                disconnect()
            }
        }
    }

    override fun disconnect() {
        if (!isConnected && bluetoothSocket == null) return
        stopStreamWorkers()

        try { bluetoothSocket?.close() } catch (_: Exception) {}
        bluetoothSocket = null

        Log.d(TAG, "Bluetooth transport disconnected cleanly.")
    }
}
