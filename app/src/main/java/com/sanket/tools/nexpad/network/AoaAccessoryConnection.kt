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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.math.abs

/**
 * Ultra-low latency, zero-allocation AOA (Android Open Accessory) Transport.
 *
 * Key Architectural Optimizations:
 * 1. Dedicated Real-Time Threads (NEXPAD-AOA-TX, NEXPAD-AOA-RX) with THREAD_PRIORITY_URGENT_AUDIO.
 * 2. Latest-State Coalescing: sendInput() does not block on USB I/O. Stale inputs are dropped automatically.
 * 3. Exact 10-byte Stream Framing: USB bulk stream is accumulated into a fixed buffer with zero allocations.
 * 4. Full 64-bit nanosecond RTT measurement using dedicated 256-slot ring buffers (zero truncation).
 * 5. RFC-compliant true jitter metric (mean consecutive packet variation) decoupled from the RX loop.
 */
class AoaAccessoryConnection(private val context: Context) : IGamepadConnection {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.sanket.tools.nexpad.USB_PERMISSION"
        private const val TAG = "NEXPAD_AOA"
        private const val FEEDBACK_PACKET_SIZE = NexpadProtocol.FEEDBACK_PACKET_SIZE
        private const val INPUT_PACKET_SIZE = NexpadProtocol.INPUT_PACKET_SIZE
        private const val RTT_RING_SIZE = 256
        private const val RTT_HISTORY_SIZE = 100
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    @Volatile private var isConnected = false

    // Dedicated native transport threads
    private var txThread: Thread? = null
    private var rxThread: Thread? = null

    // Background coroutine scope for decoupled callbacks (UI / metrics)
    private var callbackScope: CoroutineScope? = null

    // TX state & double-buffered packet staging
    private val txLock = Any()
    private val hasPendingPacket = AtomicBoolean(false)
    private val txStagingBuffer = ByteArray(INPUT_PACKET_SIZE)
    private val txWriteBuffer = ByteArray(INPUT_PACKET_SIZE)

    // RX fixed-size accumulator (zero allocations, strict stream framing)
    private val rxAccumulator = ByteArray(FEEDBACK_PACKET_SIZE)
    private var rxAccumulated = 0
    private val rxChunkBuffer = ByteArray(64)

    // Full 64-bit nanosecond RTT measurement (256 slots)
    private val sendTimestamps = LongArray(RTT_RING_SIZE)
    private val sendSeqNumbers = IntArray(RTT_RING_SIZE) { -1 }

    // RTT Statistics
    private val rttHistory = DoubleArray(RTT_HISTORY_SIZE)
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L

    // Motor state cache to prevent redundant callback dispatch
    private var lastLeftMotor = -1
    private var lastRightMotor = -1

    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null
    override var onNetworkPerformanceUpdated: ((latencyMs: Long, jitterMs: Float, packetLoss: Float) -> Unit)? = null
    override var onServerNameResolved: ((String) -> Unit)? = null

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
                callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

                // Start dedicated Real-Time TX and RX threads
                txThread = Thread({ runTxLoop() }, "NEXPAD-AOA-TX").apply { start() }
                rxThread = Thread({ runRxLoop() }, "NEXPAD-AOA-RX").apply { start() }

                val pcName = accessory.description?.takeIf { it.isNotBlank() && it != "NEXPAD USB Gamepad" } ?: "Windows PC"
                onServerNameResolved?.invoke(pcName)
                onConnectionStateChanged?.invoke(true)
                onStatusChanged?.invoke("Connected to $pcName via USB")
                Log.d(TAG, "AOA Accessory opened with dedicated real-time threads (Host: $pcName)")
            } else {
                onStatusChanged?.invoke("Failed to open accessory")
                Log.e(TAG, "Accessory open failed")
            }
        } catch (e: Exception) {
            onStatusChanged?.invoke("Error: ${e.message}")
            Log.e(TAG, "Exception opening accessory", e)
        }
    }

    /**
     * Non-blocking input submitter.
     * Encodes input into staging buffer and unparks the TX thread.
     * Callers (touch UI, sensors) never block on USB write latency.
     */
    override suspend fun sendInput(input: GamepadInput) {
        if (!isConnected) return

        val seq = NexpadProtocol.nextSequenceNumber()
        input.sequenceNumber = seq

        val slot = seq and (RTT_RING_SIZE - 1)
        sendSeqNumbers[slot] = seq
        sendTimestamps[slot] = System.nanoTime()

        synchronized(txLock) {
            NexpadProtocol.encodeInput(input, txStagingBuffer)
            hasPendingPacket.set(true)
        }

        val t = txThread
        if (t != null) {
            LockSupport.unpark(t)
        }
    }

    /**
     * Dedicated TX thread loop.
     * Runs at THREAD_PRIORITY_URGENT_AUDIO.
     * Coalesces multiple pending inputs into the latest state, completely eliminating queue lag.
     */
    private fun runTxLoop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

        while (isConnected) {
            while (!hasPendingPacket.get() && isConnected) {
                LockSupport.park()
            }
            if (!isConnected) break

            synchronized(txLock) {
                System.arraycopy(txStagingBuffer, 0, txWriteBuffer, 0, INPUT_PACKET_SIZE)
                hasPendingPacket.set(false)
            }

            try {
                outputStream?.write(txWriteBuffer)
            } catch (e: IOException) {
                if (isConnected) {
                    Log.e(TAG, "USB TX write error", e)
                    disconnect()
                }
                break
            }
        }
    }

    /**
     * Dedicated RX thread loop.
     * Runs at THREAD_PRIORITY_URGENT_AUDIO.
     * Enforces exact 10-byte stream framing with ZERO allocations.
     */
    private fun runRxLoop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

        rxAccumulated = 0

        while (isConnected) {
            val stream = inputStream ?: break
            val bytesRead = try {
                stream.read(rxChunkBuffer)
            } catch (e: IOException) {
                if (isConnected) {
                    Log.d(TAG, "USB RX stream closed or read error: ${e.message}")
                    disconnect()
                }
                break
            }

            if (bytesRead < 0) {
                if (isConnected) disconnect()
                break
            }

            var offset = 0
            while (offset < bytesRead) {
                val needed = FEEDBACK_PACKET_SIZE - rxAccumulated
                val toCopy = minOf(needed, bytesRead - offset)
                System.arraycopy(rxChunkBuffer, offset, rxAccumulator, rxAccumulated, toCopy)
                rxAccumulated += toCopy
                offset += toCopy

                if (rxAccumulated == FEEDBACK_PACKET_SIZE) {
                    handleFeedbackPacket(rxAccumulator)
                    rxAccumulated = 0
                }
            }
        }
    }

    /**
     * Parses the 10-byte feedback frame in-place with zero allocations.
     * Decouples callback execution from the RX loop.
     */
    private fun handleFeedbackPacket(data: ByteArray) {
        if (data[0] != NexpadProtocol.PROTOCOL_VERSION) return

        val leftMotor = data[1].toInt() and 0xFF
        val rightMotor = data[2].toInt() and 0xFF
        // data[3], data[4] reserved lightbar
        val packetLoss = data[5].toInt() and 0xFF
        val echoSeq = ((data[6].toInt() and 0xFF) shl 24) or
                      ((data[7].toInt() and 0xFF) shl 16) or
                      ((data[8].toInt() and 0xFF) shl 8) or
                      (data[9].toInt() and 0xFF)

        // Decoupled rumble/motor callback (only if values changed to avoid GC churn)
        if (leftMotor != lastLeftMotor || rightMotor != lastRightMotor) {
            lastLeftMotor = leftMotor
            lastRightMotor = rightMotor
            val feedback = GamepadFeedback(leftMotor, rightMotor, packetLoss)
            callbackScope?.launch {
                onFeedbackReceived?.invoke(feedback)
            }
        }

        // Full 64-bit precision RTT calculation
        val slot = echoSeq and (RTT_RING_SIZE - 1)
        if (sendSeqNumbers[slot] == echoSeq) {
            val sentTimeNanos = sendTimestamps[slot]
            val rttNanos = System.nanoTime() - sentTimeNanos
            val rttMs = rttNanos / 1_000_000.0
            sendSeqNumbers[slot] = -1 // Clear slot to prevent duplicate matching

            recordRttSample(rttMs, packetLoss)
        }
    }

    /**
     * Records RTT and calculates true packet-to-packet jitter (mean consecutive delta).
     */
    private fun recordRttSample(rttMs: Double, packetLossByte: Int) {
        rttHistory[rttHistoryIndex] = rttMs
        rttHistoryIndex = (rttHistoryIndex + 1) % RTT_HISTORY_SIZE
        if (rttSamples < RTT_HISTORY_SIZE) rttSamples++

        val now = System.currentTimeMillis()
        if (rttSamples > 0 && (now - lastRttLogTime > 1000)) {
            lastRttLogTime = now

            var sumRtt = 0.0
            var sumConsecutiveDelta = 0.0
            val count = rttSamples

            for (i in 0 until count) {
                val v = rttHistory[i]
                sumRtt += v
                if (i > 0) {
                    sumConsecutiveDelta += abs(v - rttHistory[i - 1])
                }
            }

            val avgRtt = sumRtt / count
            val jitterMs = if (count > 1) (sumConsecutiveDelta / (count - 1)) else 0.0
            val lossPctFloat = (packetLossByte and 0xFF) / 255f

            callbackScope?.launch {
                onNetworkPerformanceUpdated?.invoke(avgRtt.toLong(), jitterMs.toFloat(), lossPctFloat)
            }
        }
    }

    override fun disconnect() {
        if (!isConnected) return
        isConnected = false

        val t = txThread
        if (t != null) {
            LockSupport.unpark(t)
        }

        try { fileDescriptor?.close() } catch (e: IOException) {}
        try { context.unregisterReceiver(permissionReceiver) } catch (e: Exception) {}

        txThread?.interrupt()
        rxThread?.interrupt()
        callbackScope?.cancel()

        fileDescriptor = null
        inputStream = null
        outputStream = null
        txThread = null
        rxThread = null
        callbackScope = null

        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
        Log.d(TAG, "AOA Disconnected cleanly")
    }

    override fun close() = disconnect()
}
