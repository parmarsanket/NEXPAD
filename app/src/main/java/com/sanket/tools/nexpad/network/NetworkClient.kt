package com.sanket.tools.nexpad.network

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
import java.nio.channels.DatagramChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex

// ... (imports remain)

class NetworkClient : IGamepadConnection {
    private var channel: DatagramChannel? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    private var connectionScope: kotlinx.coroutines.CoroutineScope? = null
    private val disconnecting = java.util.concurrent.atomic.AtomicBoolean(false)
    
    private val sendBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
    private val sendByteArray = ByteArray(NexpadProtocol.INPUT_PACKET_SIZE)
    
    // RTT Measurement (128-element Ring Buffer)
    private val rttMap = java.util.concurrent.atomic.AtomicLongArray(128)
    private var lastPacketReceivedTime = 0L
    private val rttHistory = DoubleArray(100) { 0.0 }
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L
    
    // SEQ_STAMP_MASK: pack low 16 bits of seq into low 16 bits of nanoTime slot.
    // This lets us detect stale ring-buffer slots without a second array.
    // Precision cost: ~0.066ms (65536 ns) — irrelevant for a ping display.
    private val SEQ_STAMP_MASK = 0xFFFFL
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null
    override var onNetworkPerformanceUpdated: ((latencyMs: Long, jitterMs: Long, packetLoss: Float) -> Unit)? = null

    @Volatile
    private var isHandshakeComplete = false

    override suspend fun connect(address: String, port: Int) {
        disconnecting.set(false)
        // Clean up previous connection if any
        channel?.close()
        receiveJob?.cancel()
        connectionScope?.cancel()

        withContext(Dispatchers.IO) {
            try {
                serverAddress = InetSocketAddress(address, port)
                channel = DatagramChannel.open().apply {
                    configureBlocking(true)
                    // 0xB8 = (46 << 2) = DSCP EF (Expedited Forwarding) -> maps to WMM AC_VO (Voice) Queue
                    setOption(java.net.StandardSocketOptions.IP_TOS, 0xB8)
                    socket().bind(InetSocketAddress(0)) // Bind to any local port
                    connect(serverAddress) // Restrict UDP channel to this server to fix NAT/Firewall drops
                }
                
                // Do NOT fire onConnectionStateChanged(true) here.
                // The socket is open but the handshake has not completed yet.
                // Locks (WakeLock, WifiLock) are acquired only on handshake success below.
                android.util.Log.d("NEXPAD", "📡 Connecting to UDP Server at $address:$port...")
                onStatusChanged?.invoke("Connecting to $address:$port...")
                isHandshakeComplete = false
            } catch (e: Exception) {
                onConnectionStateChanged?.invoke(false)
                onStatusChanged?.invoke("Connection failed: ${e.message}")
                android.util.Log.e("NEXPAD", "❌ Connection failed: ${e.message}")
                return@withContext
            }
        }
        
        val myChannel = channel
        connectionScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        // Watchdog Coroutine to detect PC disconnection
        connectionScope!!.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                if (isHandshakeComplete) {
                    if (System.currentTimeMillis() - lastPacketReceivedTime > 2000) {
                        android.util.Log.w("NEXPAD", "⏳ Connection Timeout! PC stopped responding.")
                        onStatusChanged?.invoke("Connection Lost")
                        disconnect()
                        break
                    }
                }
            }
        }
        
        connectionScope!!.launch {
            val deviceName = android.os.Build.MODEL
            val nameBytes = deviceName.toByteArray(Charsets.UTF_8)
            val safeLength = nameBytes.size.coerceAtMost(255)
            
            // Simple heuristic: USB tethering often uses specific subnets like 192.168.42.x or 192.168.137.x
            val connectionType: Byte = if (address.startsWith("192.168.42.") || address.startsWith("192.168.137.")) 2 else 1
            
            val handshakeBuffer = ByteBuffer.allocateDirect(3 + safeLength)
            handshakeBuffer.put(NexpadProtocol.PACKET_TYPE_CONNECT)
            handshakeBuffer.put(connectionType)
            handshakeBuffer.put(safeLength.toByte())
            handshakeBuffer.put(nameBytes, 0, safeLength)
            handshakeBuffer.flip()
            
            var handshakeAttempts = 0
            while (isActive && !isHandshakeComplete) {
                if (handshakeAttempts >= 5) {
                    onConnectionStateChanged?.invoke(false) // triggers releaseWifiLock() via callback
                    onStatusChanged?.invoke("Connection Timeout")
                    return@launch
                }
                try {
                    handshakeBuffer.rewind()
                    myChannel?.send(handshakeBuffer, serverAddress)
                    handshakeAttempts++
                } catch (e: Exception) {}
                
                kotlinx.coroutines.delay(500)
            }
        }
        
        // Launch receive job in a separate scope so connect() can return immediately
        receiveJob = connectionScope!!.launch {
            val receiveBuffer = ByteBuffer.allocateDirect(1024)
            
            while (isActive) {
                try {
                    receiveBuffer.clear()
                    val senderAddress = myChannel?.receive(receiveBuffer)
                    
                    if (senderAddress != null || receiveBuffer.position() > 0) {
                        lastPacketReceivedTime = System.currentTimeMillis()
                        receiveBuffer.flip()
                        val bytes = ByteArray(receiveBuffer.remaining())
                        receiveBuffer.get(bytes)
                        
                        // android.util.Log.d("NEXPAD", "📥 UDP Rx ${bytes.size}b | First: ${if(bytes.isNotEmpty()) bytes[0] else -1}")
                        
                        // Check for Handshake Reply
                        if (bytes.isNotEmpty() && bytes[0] == NexpadProtocol.PACKET_TYPE_CONNECTED) {
                            if (!isHandshakeComplete) {
                                isHandshakeComplete = true
                                onConnectionStateChanged?.invoke(true)
                                onStatusChanged?.invoke("Connected to ${serverAddress?.hostString}")
                                android.util.Log.d("NEXPAD", "🤝 Handshake successful!")
                            }
                            continue
                        }
                        
                        // Attempt binary decode first (10 bytes for feedback)
                        val feedbackPair = NexpadProtocol.decodeFeedback(bytes)
                        
                        if (feedbackPair != null) {
                            val feedback = feedbackPair.first
                            val echoSeq = feedbackPair.second
                            
                            // Calculate RTT
                            val idx = echoSeq % 128
                            val packed = rttMap.get(idx)
                            if (packed != 0L) {
                                val ownerStamp = packed and SEQ_STAMP_MASK
                                if (ownerStamp == (echoSeq.toLong() and SEQ_STAMP_MASK)) {
                                    val sentTimeNanos = packed and SEQ_STAMP_MASK.inv()
                                    val rttMs = (System.nanoTime() - sentTimeNanos) / 1_000_000.0
                                    rttMap.set(idx, 0L) // Clear to prevent stale matching
                                    
                                    // Rolling min/max/avg
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
                                        val jitterMs = maxRtt - minRtt
                                        val rttMsg = String.format("min %.1fms / avg %.1fms / max %.1fms", minRtt, avgRtt, maxRtt)
                                        val logMsg = "📡 RTT: $rttMsg"
                                        onDiagnosticLog?.invoke(logMsg)
                                        
                                        val lossPctFloat = feedback.packetLossPct / 255f
                                        onNetworkPerformanceUpdated?.invoke(avgRtt.toLong(), jitterMs.toLong(), lossPctFloat)
                                        android.util.Log.d("NEXPAD", logMsg)
                                    }
                                }
                            }
                            
                            onFeedbackReceived?.invoke(feedback)
                        }
                    }
                } catch (e: java.nio.channels.AsynchronousCloseException) {
                    break // Normal closure
                } catch (e: java.nio.channels.ClosedChannelException) {
                    break // Normal closure
                } catch (e: java.io.IOException) {
                    if (isActive) {
                        e.printStackTrace()
                        if (channel === myChannel) {
                            disconnect()
                        }
                    }
                    break
                } catch (e: Exception) {
                    if (isActive) e.printStackTrace()
                }
            }
        }
    }

    private var packetsSent = 0
    private val sendMutex = Mutex()
    
    // DEBUG: Chaos Monkey - Set to true to artificially drop and scramble UDP packets for testing.
    private val ENABLE_CHAOS_MONKEY = false
    
    override suspend fun sendInput(input: GamepadInput) {
        if (!isHandshakeComplete) return
        
        val currentChannel = channel ?: return
        val target = serverAddress ?: return
        
        try {
            sendMutex.withLock {
                // Write directly to the pre-allocated sendBuffer
                // Track sent time for RTT calculation
                input.sequenceNumber = NexpadProtocol.nextSequenceNumber()
                val seq = input.sequenceNumber
                val idx = seq % 128
                val stamped = (System.nanoTime() and SEQ_STAMP_MASK.inv()) or (seq.toLong() and SEQ_STAMP_MASK)
                rttMap.set(idx, stamped)
                
                NexpadProtocol.encodeInput(input, sendByteArray)
                
                sendBuffer.clear()
                sendBuffer.put(sendByteArray)
                sendBuffer.flip()
                
                if (ENABLE_CHAOS_MONKEY) {
                    val rand = Math.random()
                    if (rand < 0.1) {
                        // 10% chance to drop the packet entirely — exercises packet-loss counter
                        return@withLock
                    }
                    if (rand < 0.3) {
                        // 20% chance to delay the packet.
                        // Use 1500ms to reproduce RTT staleness bug (128 slots ÷ 120Hz ≈ 1.07s threshold).
                        // Revert delay to 30ms after staleness testing is confirmed.
                        val CHAOS_DELAY_MS = 1500L
                        val delayedBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
                        delayedBuffer.put(sendByteArray).flip()
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(CHAOS_DELAY_MS)
                            try { currentChannel.send(delayedBuffer, target) } catch (e: Exception) {}
                        }
                        return@withLock
                    }
                }
                
                // Send the pre-allocated packet
                currentChannel.send(sendBuffer, target)
                
                packetsSent++
                if (packetsSent % 60 == 0) {
                    onDiagnosticLog?.invoke("Sent $packetsSent packets to ${target.hostString}:${target.port}")
                }
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            onDiagnosticLog?.invoke("Network dropped: ${e.message}")
            if (channel === currentChannel) {
                disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onDiagnosticLog?.invoke("Send error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    override fun disconnect() {
        if (!disconnecting.compareAndSet(false, true)) return
        
        // Notify server of disconnect before closing channel
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
