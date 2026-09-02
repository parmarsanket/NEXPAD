package com.sanket.tools.nexpad.network

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class AoaAccessoryConnection(private val context: Context) : IGamepadConnection {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.sanket.tools.nexpad.USB_PERMISSION"
        private const val TAG = "NEXPAD_AOA"
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    private var connectionScope: CoroutineScope? = null
    private var receiveJob: Job? = null
    @Volatile private var isConnected = false

    private val sendByteArray = ByteArray(NexpadProtocol.INPUT_PACKET_SIZE)

    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null
    override var onNetworkPerformanceUpdated: ((latencyMs: Long, jitterMs: Long, packetLoss: Float) -> Unit)? = null

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val accessory: UsbAccessory? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        accessory?.let { openAccessory(it) }
                    } else {
                        onStatusChanged?.invoke("USB permission denied")
                        Log.e(TAG, "Permission denied for accessory $accessory")
                    }
                }
            }
        }
    }

    override suspend fun connect(address: String, port: Int) {
        val accessoryList = usbManager.accessoryList
        if (accessoryList.isNullOrEmpty()) {
            onStatusChanged?.invoke("No USB accessory found")
            return
        }

        val accessory = accessoryList[0]
        if (usbManager.hasPermission(accessory)) {
            openAccessory(accessory)
        } else {
            onStatusChanged?.invoke("Requesting USB permission...")
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            usbManager.requestPermission(accessory, permissionIntent)
        }
    }

    private fun openAccessory(accessory: UsbAccessory) {
        try {
            fileDescriptor = usbManager.openAccessory(accessory)
            if (fileDescriptor != null) {
                val fd = fileDescriptor!!.fileDescriptor
                inputStream = FileInputStream(fd)
                outputStream = FileOutputStream(fd)
                
                isConnected = true
                onConnectionStateChanged?.invoke(true)
                onStatusChanged?.invoke("Connected via AOA")
                Log.d(TAG, "AOA Accessory opened")

                connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                receiveJob = connectionScope?.launch {
                    val buffer = ByteArray(8) // Expecting 8-byte feedback packets
                    while (isActive && isConnected) {
                        try {
                            val bytesRead = inputStream?.read(buffer) ?: -1
                            if (bytesRead > 0) {
                                val feedbackPair = NexpadProtocol.decodeFeedback(buffer.copyOfRange(0, bytesRead))
                                if (feedbackPair != null) {
                                    onFeedbackReceived?.invoke(feedbackPair.first)
                                }
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Error reading from accessory", e)
                            break
                        }
                    }
                    disconnect()
                }
            } else {
                onStatusChanged?.invoke("Failed to open accessory")
                Log.e(TAG, "Accessory open failed")
            }
        } catch (e: Exception) {
            onStatusChanged?.invoke("Error: ${e.message}")
            Log.e(TAG, "Exception opening accessory", e)
        }
    }

    override suspend fun sendInput(input: GamepadInput) {
        if (!isConnected) return
        try {
            input.sequenceNumber = NexpadProtocol.nextSequenceNumber()
            NexpadProtocol.encodeInput(input, sendByteArray)
            outputStream?.write(sendByteArray)
        } catch (e: IOException) {
            Log.e(TAG, "Send error", e)
            disconnect()
        }
    }

    override fun disconnect() {
        isConnected = false
        receiveJob?.cancel()
        connectionScope?.cancel()
        
        try { fileDescriptor?.close() } catch (e: IOException) {}
        try { context.unregisterReceiver(permissionReceiver) } catch (e: Exception) {}
        
        fileDescriptor = null
        inputStream = null
        outputStream = null
        
        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
        Log.d(TAG, "AOA Disconnected")
    }

    override fun close() = disconnect()
}
