package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    
    private val sendBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
    private val sendByteArray = ByteArray(NexpadProtocol.INPUT_PACKET_SIZE)
    
    // RTT Measurement (128-element Ring Buffer)
    private val rttMap = LongArray(128) { 0L }
    private val rttHistory = DoubleArray(100) { 0.0 }
    private var rttHistoryIndex = 0
    private var rttSamples = 0
    private var lastRttLogTime = 0L
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null

    override suspend fun connect(address: String, port: Int) {
        withContext(Dispatchers.IO) {
            try {
                serverAddress = InetSocketAddress(address, port)
                channel = DatagramChannel.open().apply {
                    configureBlocking(false)
                    socket().bind(InetSocketAddress(0)) // Bind to any local port
                }
                
                onConnectionStateChanged?.invoke(true)
                onStatusChanged?.invoke("Connected to $address:$port")
                android.util.Log.d("NEXPAD", "📡 Connected to UDP Server at $address:$port")
            } catch (e: Exception) {
                onConnectionStateChanged?.invoke(false)
                onStatusChanged?.invoke("Connection failed: ${e.message}")
                android.util.Log.e("NEXPAD", "❌ Connection failed: ${e.message}")
                return@withContext
            }
        }
        
        // Launch receive job in a separate scope so connect() can return immediately
        receiveJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val receiveBuffer = ByteBuffer.allocateDirect(1024)
            
            while (isActive) {
                try {
                    receiveBuffer.clear()
                    val senderAddress = channel?.receive(receiveBuffer)
                    
                    if (senderAddress != null) {
                        receiveBuffer.flip()
                        val bytes = ByteArray(receiveBuffer.remaining())
                        receiveBuffer.get(bytes)
                        
                        // Attempt binary decode first (10 bytes for feedback)
                        val feedbackPair = NexpadProtocol.decodeFeedback(bytes)
                        
                        if (feedbackPair != null) {
                            val feedback = feedbackPair.first
                            val echoSeq = feedbackPair.second
                            
                            // Calculate RTT
                            val sentTime = rttMap[echoSeq % 128]
                            if (sentTime > 0) {
                                val rttMs = (System.nanoTime() - sentTime) / 1_000_000.0
                                rttMap[echoSeq % 128] = 0L // Clear to prevent stale matching
                                
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
                                    val rttMsg = String.format("📡 RTT: min %.1fms / avg %.1fms / max %.1fms", minRtt, avgRtt, maxRtt)
                                    onDiagnosticLog?.invoke(rttMsg)
                                    android.util.Log.d("NEXPAD", rttMsg)
                                }
                            }
                            
                            onFeedbackReceived?.invoke(feedback)
                        } else {
                            // Fallback to JSON if binary decode fails (backward compatibility)
                            try {
                                val json = String(bytes)
                                val feedback = Json.decodeFromString<GamepadFeedback>(json)
                                onFeedbackReceived?.invoke(feedback)
                            } catch (e: Exception) {
                                // Ignored: Not valid JSON either
                            }
                        }
                    } else {
                        // Non-blocking wait
                        kotlinx.coroutines.delay(10)
                    }
                } catch (e: java.nio.channels.ClosedChannelException) {
                    break // Normal closure
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
        val currentChannel = channel ?: return
        val target = serverAddress ?: return
        
        try {
            sendMutex.withLock {
                // Write directly to the pre-allocated sendBuffer
                NexpadProtocol.encodeInput(input, sendByteArray)
                
                // Track sent time for RTT calculation
                val seq = NexpadProtocol.getCurrentSequenceNumber()
                rttMap[seq % 128] = System.nanoTime()
                
                sendBuffer.clear()
                sendBuffer.put(sendByteArray)
                sendBuffer.flip()
                
                if (ENABLE_CHAOS_MONKEY) {
                    val rand = Math.random()
                    if (rand < 0.1) {
                        // 10% chance to drop the packet entirely
                        return@withLock
                    }
                    if (rand < 0.3) {
                        // 20% chance to intentionally delay the packet to arrive OUT OF ORDER
                        val delayedBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
                        delayedBuffer.put(sendByteArray).flip()
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            kotlinx.coroutines.delay(30)
                            try { currentChannel.send(delayedBuffer, target) } catch (e: Exception) {}
                        }
                        return@withLock
                    }
                }
                
                // Send the pre-allocated packet
                currentChannel.send(sendBuffer, target)
            }
            
            packetsSent++
            if (packetsSent % 60 == 0) {
                onDiagnosticLog?.invoke("Sent $packetsSent packets to ${target.hostString}:${target.port}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onDiagnosticLog?.invoke("Send error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    override fun disconnect() {
        receiveJob?.cancel()
        channel?.close()
        channel = null
        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
    }

    override fun startAdvertising() {
        // No-op for network client
    }

    override fun close() = disconnect()
}
