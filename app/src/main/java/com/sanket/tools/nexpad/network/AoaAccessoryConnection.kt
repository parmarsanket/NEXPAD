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

    // RTT Measurement (128-element Ring Buffer)
    private val rttMap = java.util.concurrent.atomic.AtomicLongArray(128)
    private val rttHistory = DoubleArray(100) { 0.0 }
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L
    
    // SEQ_STAMP_MASK: pack low 16 bits of seq into low 16 bits of nanoTime slot.
    private val SEQ_STAMP_MASK = 0xFFFFL

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
                    val buffer = ByteArray(NexpadProtocol.FEEDBACK_PACKET_SIZE)
                    while (isActive && isConnected) {
                        try {
                            val bytesRead = inputStream?.read(buffer) ?: -1
                            if (bytesRead > 0) {
                                val feedbackPair = NexpadProtocol.decodeFeedback(buffer.copyOfRange(0, bytesRead))
                                if (feedbackPair != null) {
                                    val feedback = feedbackPair.first
                                    val echoSeq = feedbackPair.second
                                    
                                    onFeedbackReceived?.invoke(feedback)
                                    
                                    // Calculate RTT
                                    val idx = echoSeq % 128
                                    val packed = rttMap.get(idx)
                                    if (packed != 0L) {
                                        val ownerStamp = packed and SEQ_STAMP_MASK
                                        if (ownerStamp == (echoSeq.toLong() and SEQ_STAMP_MASK)) {
                                            val sentTimeNanos = packed and SEQ_STAMP_MASK.inv()
                                            val rttMs = (System.nanoTime() - sentTimeNanos) / 1_000_000.0
                                            rttMap.set(idx, 0L) // Clear
                                            
                                            rttHistory[rttHistoryIndex] = rttMs
                                            rttHistoryIndex = (rttHistoryIndex + 1) % 100
                                            if (rttSamples < 100) rttSamples++
                                            
                                            val now = System.currentTimeMillis()
                                            if (rttSamples > 0 && (now - lastRttLogTime > 1000)) {
                                                lastRttLogTime = now
                                                
                                                var minRtt = Double.MAX_VALUE
                                                var maxRtt = Double.MIN_VALUE
                                                var sumRtt = 0.0
                                                for (i in 0 until rttSamples) {
                                                    val v = rttHistory[i]
                                                    if (v < minRtt) minRtt = v
                                                    if (v > maxRtt) maxRtt = v
                                                    sumRtt += v
                                                }
                                                val avgRtt = sumRtt / rttSamples
                                                val jitter = maxRtt - minRtt
                                                
                                                val lossPctFloat = (buffer[5].toInt() and 0xFF) / 255f
                                                
                                                onNetworkPerformanceUpdated?.invoke(avgRtt.toLong(), jitter.toLong(), lossPctFloat)
                                            }
                                        }
                                    }
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
            
            val seq = input.sequenceNumber
            val idx = seq % 128
            val stamped = (System.nanoTime() and SEQ_STAMP_MASK.inv()) or (seq.toLong() and SEQ_STAMP_MASK)
            rttMap.set(idx, stamped)
            
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
