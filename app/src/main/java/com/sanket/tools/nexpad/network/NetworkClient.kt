package com.sanket.tools.nexpad.network

import android.util.Log
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.Arrays
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class NetworkClient : IGamepadConnection {
    companion object {
        private const val RTT_RING_SIZE = 128
        private const val RTT_HISTORY_SIZE = 100
    }

    private var channel: DatagramChannel? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    private var connectionScope: kotlinx.coroutines.CoroutineScope? = null
    private val disconnecting = AtomicBoolean(false)
    
    private val sendBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
    private val sendByteArray = ByteArray(NexpadProtocol.INPUT_PACKET_SIZE)
    private val sendLock = Any()
    
    // RTT Measurement (Zero-allocation Ring Buffer matching BaseStreamConnection)
    private val sendTimestamps = LongArray(RTT_RING_SIZE)
    private val sendSeqNumbers = IntArray(RTT_RING_SIZE) { -1 }
    private val rttHistory = DoubleArray(RTT_HISTORY_SIZE)
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L
    private var lastPacketReceivedTime = 0L
    private var packetsReceived = 0
    private var packetsSent = 0
    private var connectionStartTime = 0L

    // Rumble & Sensor Mode State Deduplication (Avoids 100 coroutine/Flow emissions per second)
    private var lastLeftMotor = -1
    private var lastRightMotor = -1
    private var lastRequestedSensorMode: Byte = -1
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null
    override var onNetworkPerformanceUpdated: ((latencyMs: Float, jitterMs: Float, packetLoss: Float) -> Unit)? = null
    override var onServerNameResolved: ((String) -> Unit)? = null

    @Volatile
    private var isHandshakeComplete = false

    override suspend fun connect(address: String, port: Int) {
        rttSamples = 0
        rttHistoryIndex = 0
        lastLeftMotor = -1
        lastRightMotor = -1
        lastRequestedSensorMode = -1
        packetsSent = 0
        packetsReceived = 0
        connectionStartTime = System.currentTimeMillis()
        lastPacketReceivedTime = System.currentTimeMillis()
        Arrays.fill(sendSeqNumbers, -1)
        disconnecting.set(false)

        // Clean up previous connection if any
        channel?.close()
        receiveJob?.cancel()
        connectionScope?.cancel()

        var connected = false
        withContext(Dispatchers.IO) {
            try {
                serverAddress = InetSocketAddress(address, port)
                channel = DatagramChannel.open().apply {
                    configureBlocking(true)
                    // 0xB8 = (46 << 2) = DSCP EF (Expedited Forwarding) -> maps to WMM AC_VO (Voice) Queue
                    try {
                        setOption(java.net.StandardSocketOptions.IP_TOS, 0xB8)
                    } catch (_: Exception) {}
                    socket().sendBufferSize = 65536
                    socket().receiveBufferSize = 65536
                    socket().bind(InetSocketAddress(0)) // Bind to any local port
                    connect(serverAddress) // Restrict UDP channel to this server to fix NAT/Firewall drops
                }
                
                Log.d("NEXPAD", "📡 Connecting to UDP Server at $address:$port...")
                onStatusChanged?.invoke("Connecting to $address:$port...")
                isHandshakeComplete = false
                connected = true
            } catch (e: Exception) {
                try { channel?.close() } catch (_: Exception) {}
                channel = null
                onConnectionStateChanged?.invoke(false)
                onStatusChanged?.invoke("Connection failed: ${e.message}")
                Log.e("NEXPAD", "❌ Connection failed: ${e.message}")
                return@withContext
            }
        }

        if (!connected || channel == null || channel?.isOpen != true) {
            return
        }
        
        val myChannel = channel
        val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)
        connectionScope = scope
        
        // Watchdog Coroutine to detect true PC disconnection (5s timeout, resilient to Wi-Fi jitter)
        scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                if (isHandshakeComplete) {
                    val timeSince = System.currentTimeMillis() - lastPacketReceivedTime
                    if (timeSince in 2501..3500) {
                        Log.w("NEXPAD", "⚠️ [UDP CLIENT] Wi-Fi Warning: No packet from PC for ${timeSince}ms (timeout limit 5000ms). Sent: $packetsSent, Recv: $packetsReceived")
                    }
                    if (timeSince > 5000) {
                        val durationSec = (System.currentTimeMillis() - connectionStartTime) / 1000
                        Log.w("NEXPAD", "⏳ [UDP CLIENT] Connection Timeout! PC stopped responding for ${timeSince}ms. Active duration: ${durationSec}s | Sent: $packetsSent | Recv: $packetsReceived | Channel open: ${myChannel?.isOpen}")
                        onStatusChanged?.invoke("Connection Lost")
                        disconnect()
                        break
                    }
                }
            }
        }
        
        // Handshake Coroutine
        scope.launch {
            val deviceName = android.os.Build.MODEL
            val nameBytes = deviceName.toByteArray(Charsets.UTF_8)
            val safeLength = nameBytes.size.coerceAtMost(255)
            
            val isUsbTethering = NetworkInterfaceHelper.isUsbTetheringAddress(address)
            val connectionType: Byte = if (isUsbTethering) 2 else 1
            Log.d("NEXPAD", "Handshake target $address -> connectionType=$connectionType (isUsbTethering=$isUsbTethering)")
            
            val handshakeBuffer = ByteBuffer.allocateDirect(3 + safeLength)
            handshakeBuffer.put(NexpadProtocol.PACKET_TYPE_CONNECT)
            handshakeBuffer.put(connectionType)
            handshakeBuffer.put(safeLength.toByte())
            handshakeBuffer.put(nameBytes, 0, safeLength)
            handshakeBuffer.flip()
            
            var handshakeAttempts = 0
            while (isActive && !isHandshakeComplete) {
                if (handshakeAttempts >= 30) {
                    onConnectionStateChanged?.invoke(false)
                    onStatusChanged?.invoke("Connection Timeout")
                    disconnect()
                    return@launch
                }
                try {
                    handshakeBuffer.rewind()
                    myChannel?.send(handshakeBuffer, serverAddress)
                    handshakeAttempts++
                } catch (e: Exception) {
                    Log.e("NEXPAD", "Handshake attempt #$handshakeAttempts error: ${e.message}", e)
                }
                
                kotlinx.coroutines.delay(100)
            }
        }
        
        // Receive Job
        receiveJob = scope.launch {
            val receiveBuffer = ByteBuffer.allocateDirect(1024)
            
            while (isActive) {
                try {
                    receiveBuffer.clear()
                    val senderAddress = myChannel?.receive(receiveBuffer)
                    
                    if (senderAddress != null || receiveBuffer.position() > 0) {
                        lastPacketReceivedTime = System.currentTimeMillis()
                        packetsReceived++
                        receiveBuffer.flip()
                        val bytes = ByteArray(receiveBuffer.remaining())
                        receiveBuffer.get(bytes)
                        
                        if (packetsReceived == 1) {
                            Log.d("NEXPAD", "📡 [UDP CLIENT] First packet received from PC! Size: ${bytes.size} bytes | First byte: ${bytes.getOrNull(0)}")
                        } else if (packetsReceived % 120 == 0) {
                            Log.d("NEXPAD", "📡 [UDP CLIENT] Packets received: $packetsReceived | Sent: $packetsSent | Last RTT samples: $rttSamples")
                        }

                        // Check for Handshake Reply
                        if (bytes.isNotEmpty() && bytes[0] == NexpadProtocol.PACKET_TYPE_CONNECTED) {
                            if (!isHandshakeComplete) {
                                isHandshakeComplete = true
                                var resolvedName: String? = null
                                if (bytes.size >= 2) {
                                    val nameLen = bytes[1].toInt() and 0xFF
                                    if (bytes.size >= 2 + nameLen) {
                                        resolvedName = String(bytes, 2, nameLen, Charsets.UTF_8).trim()
                                    }
                                }
                                Log.d("NEXPAD", "🤝 [UDP CLIENT] Handshake complete! Connected to PC: ${resolvedName ?: serverAddress?.hostString}")
                                if (!resolvedName.isNullOrBlank()) {
                                    onServerNameResolved?.invoke(resolvedName)
                                }
                                onConnectionStateChanged?.invoke(true)
                                onStatusChanged?.invoke("Connected to ${resolvedName ?: serverAddress?.hostString}")
                            }
                            continue
                        }
                        
                        // Feedback & RTT Echo Decode
                        val feedbackPair = NexpadProtocol.decodeFeedback(bytes)
                        if (feedbackPair != null) {
                            val feedback = feedbackPair.first
                            val echoSeq = feedbackPair.second
                            
                            // Only emit feedback if motor speeds or requested sensor mode actually changed
                            if (feedback.leftMotorSpeed != lastLeftMotor ||
                                feedback.rightMotorSpeed != lastRightMotor ||
                                feedback.requestedSensorMode != lastRequestedSensorMode) {
                                lastLeftMotor = feedback.leftMotorSpeed
                                lastRightMotor = feedback.rightMotorSpeed
                                lastRequestedSensorMode = feedback.requestedSensorMode
                                onFeedbackReceived?.invoke(feedback)
                            }
                            
                            // Match RTT echo sequence number
                            val slot = echoSeq and (RTT_RING_SIZE - 1)
                            val expectedSeq = sendSeqNumbers[slot]
                            if (expectedSeq == echoSeq) {
                                val sentTimeNanos = sendTimestamps[slot]
                                val rttNanos = System.nanoTime() - sentTimeNanos
                                val rttMs = rttNanos / 1_000_000.0
                                sendSeqNumbers[slot] = -1
                                
                                recordRttSample(rttMs, feedback.packetLossPct)
                            }
                        } else if (bytes.isNotEmpty() && packetsReceived % 60 == 0) {
                            Log.w("NEXPAD", "⚠️ [UDP CLIENT] Received non-feedback packet (${bytes.size} bytes, type: 0x${bytes[0].toString(16)})")
                        }
                    }
                } catch (e: java.nio.channels.AsynchronousCloseException) {
                    Log.d("NEXPAD", "ℹ️ [UDP CLIENT] Receive channel closed asynchronously")
                    break // Normal closure
                } catch (e: java.nio.channels.ClosedChannelException) {
                    Log.d("NEXPAD", "ℹ️ [UDP CLIENT] Receive channel closed")
                    break // Normal closure
                } catch (e: java.io.IOException) {
                    if (isActive) {
                        Log.e("NEXPAD", "❌ [UDP CLIENT] Receive IOException: ${e.javaClass.simpleName} - ${e.message}", e)
                        if (channel === myChannel) {
                            disconnect()
                        }
                    }
                    break
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e("NEXPAD", "❌ [UDP CLIENT] Receive unexpected exception: ${e.javaClass.simpleName} - ${e.message}", e)
                    }
                }
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

            val sorted = rttHistory.copyOf(count).apply { Arrays.sort(this) }
            val p50 = sorted[count / 2]
            val p95 = sorted[(count * 0.95).toInt().coerceAtMost(count - 1)]
            val p99 = sorted[(count * 0.99).toInt().coerceAtMost(count - 1)]
            val rttMsg = String.format(Locale.US, "p50 %.2fms / p95 %.2fms / p99 %.2fms / avg %.2fms", p50, p95, p99, avgRtt)
            val logMsg = "📡 [WIFI] RTT: $rttMsg | Samples: $count | Jitter: ±${String.format(Locale.US, "%.2f", jitterMs)}ms | Loss: ${(lossPctFloat * 100).toInt()}%"
            onDiagnosticLog?.invoke(logMsg)

            onNetworkPerformanceUpdated?.invoke(avgRtt.toFloat(), jitterMs.toFloat(), lossPctFloat)
        }
    }

    override suspend fun sendInput(input: GamepadInput) {
        if (!isHandshakeComplete) return
        
        val currentChannel = channel ?: return
        val target = serverAddress ?: return
        
        try {
            synchronized(sendLock) {
                val seq = NexpadProtocol.nextSequenceNumber()
                input.sequenceNumber = seq
                val slot = seq and (RTT_RING_SIZE - 1)
                sendSeqNumbers[slot] = seq
                sendTimestamps[slot] = System.nanoTime()
                
                NexpadProtocol.encodeInput(input, sendByteArray)
                
                sendBuffer.clear()
                sendBuffer.put(sendByteArray)
                sendBuffer.flip()
                
                currentChannel.send(sendBuffer, target)
                
                packetsSent++
                if (packetsSent % 120 == 0) {
                    val logMsg = "📡 [UDP CLIENT] Packets sent: $packetsSent to ${target.hostString}:${target.port}"
                    Log.d("NEXPAD", logMsg)
                    onDiagnosticLog?.invoke(logMsg)
                }
            }
        } catch (e: java.io.IOException) {
            Log.e("NEXPAD", "❌ [UDP CLIENT] sendInput IOException: ${e.javaClass.simpleName} - ${e.message}", e)
            onDiagnosticLog?.invoke("Network dropped: ${e.message}")
            if (channel === currentChannel) {
                disconnect()
            }
        } catch (e: Exception) {
            Log.e("NEXPAD", "❌ [UDP CLIENT] sendInput Exception: ${e.javaClass.simpleName} - ${e.message}", e)
            onDiagnosticLog?.invoke("Send error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    override fun disconnect() {
        if (!disconnecting.compareAndSet(false, true)) return
        
        val durationSec = if (connectionStartTime > 0L) (System.currentTimeMillis() - connectionStartTime) / 1000 else 0L
        Log.d("NEXPAD", "🔌 [UDP CLIENT] Disconnecting session. Duration: ${durationSec}s | Sent: $packetsSent | Recv: $packetsReceived")

        val currentChannel = channel
        val target = serverAddress
        if (currentChannel != null && target != null && currentChannel.isOpen) {
            try {
                val buffer = ByteBuffer.allocateDirect(1)
                buffer.put(NexpadProtocol.PACKET_TYPE_DISCONNECT)
                buffer.flip()
                currentChannel.send(buffer, target)
            } catch (e: Exception) {}
        }
        
        receiveJob?.cancel()
        connectionScope?.cancel()
        connectionScope = null
        channel?.close()
        channel = null
        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
    }

    override fun close() = disconnect()
}
