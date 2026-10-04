package com.sanket.tools.nexpad.network

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ultra-low latency, zero-allocation ADB Bridge Transport using Unix Domain Sockets (localabstract).
 * Connects directly to adbd via LocalSocket(Namespace.ABSTRACT), bypassing Android's TCP/IP stack.
 */
class AdbBridgeConnection(private val context: Context) : BaseStreamConnection() {

    companion object {
        private const val ABSTRACT_SOCKET_NAME = "nexpad_controller"
        private const val TAG = "NEXPAD_ADB"
    }

    private var localSocket: LocalSocket? = null

    override suspend fun connect(address: String, port: Int) {
        withContext(Dispatchers.IO) {
            disconnect()

            onStatusChanged?.invoke("Connecting to ADB Bridge...")
            Log.d(TAG, "Attempting LocalSocket connection to abstract namespace: $ABSTRACT_SOCKET_NAME")

            try {
                val socket = LocalSocket()
                socket.connect(LocalSocketAddress(ABSTRACT_SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT))
                try {
                    socket.sendBufferSize = 65536
                    socket.receiveBufferSize = 65536
                } catch (_: Exception) {}

                localSocket = socket
                startStreamWorkers(socket.inputStream, socket.outputStream, "ADB")
                onStatusChanged?.invoke("Connected via USB (ADB)")
                Log.d(TAG, "Connected to ADB Bridge successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to ADB Bridge", e)
                onStatusChanged?.invoke("ADB connection failed: ${e.message}")
                disconnect()
            }
        }
    }

    override fun disconnect() {
        if (!isConnected && localSocket == null) return
        stopStreamWorkers()
        try { localSocket?.close() } catch (_: Exception) {}
        localSocket = null
        Log.d(TAG, "ADB Bridge Disconnected cleanly")
    }
}
