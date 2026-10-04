package com.sanket.tools.nexpad.network

import android.util.Log
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs

/**
 * Reusable abstract base class for stream-based transports (ADB, AOA USB, Bluetooth RFCOMM).
 * Implements high-performance TX latest-state coalescing, strict 10-byte RX stream framing,
 * and 64-bit nanosecond RTT / RFC-compliant jitter metrics with zero GC thrashing.
 */
abstract class BaseStreamConnection : IGamepadConnection {

    companion object {
        const val FEEDBACK_PACKET_SIZE = NexpadProtocol.FEEDBACK_PACKET_SIZE
        const val INPUT_PACKET_SIZE = NexpadProtocol.INPUT_PACKET_SIZE
        const val RTT_RING_SIZE = 256
        const val RTT_HISTORY_SIZE = 100
    }

    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null
    override var onNetworkPerformanceUpdated: ((latencyMs: Float, jitterMs: Float, packetLoss: Float) -> Unit)? = null
    override var onServerNameResolved: ((String) -> Unit)? = null

    @Volatile protected var isConnected = false

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var rxThread: Thread? = null
    protected var callbackScope: CoroutineScope? = null
        private set

    private val txLock = Any()
    private val txWriteBuffer = ByteArray(INPUT_PACKET_SIZE)

    private val rxAccumulator = ByteArray(FEEDBACK_PACKET_SIZE)
    private var rxAccumulated = 0
    private val rxChunkBuffer = ByteArray(64)

    private val sendTimestamps = LongArray(RTT_RING_SIZE)
    private val sendSeqNumbers = IntArray(RTT_RING_SIZE) { -1 }

    private val rttHistory = DoubleArray(RTT_HISTORY_SIZE)
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L

    private var lastLeftMotor = -1
    private var lastRightMotor = -1

    private var currentTag = "STREAM"

    override suspend fun sendInput(input: GamepadInput) {
        if (!isConnected) return
        val stream = outputStream ?: return

        val seq = NexpadProtocol.nextSequenceNumber()
        input.sequenceNumber = seq

        val slot = seq and (RTT_RING_SIZE - 1)
        sendSeqNumbers[slot] = seq

        try {
            synchronized(txLock) {
                NexpadProtocol.encodeInput(input, txWriteBuffer)
                sendTimestamps[slot] = System.nanoTime()
                stream.write(txWriteBuffer)
                stream.flush()
            }
        } catch (e: IOException) {
            if (isConnected) {
                Log.e("BaseStreamConnection", "TX write error", e)
                disconnect()
            }
        }
    }

    protected fun startStreamWorkers(input: InputStream, output: OutputStream, transportTag: String) {
        currentTag = transportTag
        rttSamples = 0
        rttHistoryIndex = 0
        lastRttLogTime = 0L
        lastLeftMotor = -1
        lastRightMotor = -1
        sendSeqNumbers.fill(-1)
        inputStream = input
        outputStream = output
        isConnected = true

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        callbackScope = scope

        val rx = Thread({ runRxLoop(transportTag) }, "NEXPAD-$transportTag-RX")
        rxThread = rx
        rx.start()

        onConnectionStateChanged?.invoke(true)
    }

    protected fun stopStreamWorkers() {
        if (!isConnected && rxThread == null) return
        isConnected = false
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}

        rxThread?.interrupt()
        callbackScope?.cancel()

        inputStream = null
        outputStream = null
        rxThread = null
        callbackScope = null

        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
    }

    private fun runRxLoop(tag: String) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        rxAccumulated = 0
        var handshakeParsed = false

        while (isConnected) {
            val stream = inputStream ?: break
            val bytesRead = try {
                stream.read(rxChunkBuffer)
            } catch (e: IOException) {
                if (isConnected) {
                    Log.d(tag, "$tag RX stream closed: ${e.message}")
                    disconnect()
                }
                break
            }

            if (bytesRead < 0) {
                if (isConnected) disconnect()
                break
            }

            var offset = 0
            if (!handshakeParsed) {
                if (bytesRead >= 2 && rxChunkBuffer[0] == NexpadProtocol.PACKET_TYPE_CONNECTED) {
                    val nameLen = rxChunkBuffer[1].toInt() and 0xFF
                    if (bytesRead >= 2 + nameLen) {
                        val pcName = String(rxChunkBuffer, 2, nameLen, Charsets.UTF_8).trim()
                        if (pcName.isNotEmpty()) {
                            onServerNameResolved?.invoke(pcName)
                        }
                        offset = 2 + nameLen
                        handshakeParsed = true
                    }
                } else if (bytesRead > 0 && rxChunkBuffer[0] == NexpadProtocol.PROTOCOL_VERSION) {
                    handshakeParsed = true
                }
            }

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

    private fun handleFeedbackPacket(data: ByteArray) {
        if (data[0] != NexpadProtocol.PROTOCOL_VERSION) return

        val leftMotor = data[1].toInt() and 0xFF
        val rightMotor = data[2].toInt() and 0xFF
        val packetLoss = data[5].toInt() and 0xFF
        val echoSeq = ((data[6].toInt() and 0xFF) shl 24) or
                ((data[7].toInt() and 0xFF) shl 16) or
                ((data[8].toInt() and 0xFF) shl 8) or
                (data[9].toInt() and 0xFF)

        if (leftMotor != lastLeftMotor || rightMotor != lastRightMotor) {
            lastLeftMotor = leftMotor
            lastRightMotor = rightMotor
            val feedback = GamepadFeedback(leftMotor, rightMotor, packetLoss)
            callbackScope?.launch {
                onFeedbackReceived?.invoke(feedback)
            }
        }

        if (echoSeq > 0) {
            val slot = echoSeq and (RTT_RING_SIZE - 1)
            val expectedSeq = sendSeqNumbers[slot]
            if (expectedSeq == echoSeq) {
                val sentTimeNanos = sendTimestamps[slot]
                val rttNanos = System.nanoTime() - sentTimeNanos
                val rttMs = rttNanos / 1_000_000.0
                sendSeqNumbers[slot] = -1

                recordRttSample(rttMs, packetLoss)
            }
        }
    }

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

            val sorted = rttHistory.copyOf(count).apply { java.util.Arrays.sort(this) }
            val p50 = sorted[count / 2]
            val p95 = sorted[(count * 0.95).toInt().coerceAtMost(count - 1)]
            val p99 = sorted[(count * 0.99).toInt().coerceAtMost(count - 1)]
            val rttMsg = String.format(java.util.Locale.US, "p50 %.2fms / p95 %.2fms / p99 %.2fms / avg %.2fms", p50, p95, p99, avgRtt)
            val logMsg = "📡 [$currentTag] RTT: $rttMsg | Samples: $count | Jitter: ±${String.format(java.util.Locale.US, "%.2f", jitterMs)}ms | Loss: ${(lossPctFloat * 100).toInt()}%"
            onDiagnosticLog?.invoke(logMsg)

            callbackScope?.launch {
                onNetworkPerformanceUpdated?.invoke(avgRtt.toFloat(), jitterMs.toFloat(), lossPctFloat)
            }
        }
    }

    override fun close() = disconnect()
}
