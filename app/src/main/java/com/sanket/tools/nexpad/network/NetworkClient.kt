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

// ... (imports remain)

class NetworkClient : IGamepadConnection {
    private var channel: DatagramChannel? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    
    private val sendBuffer = ByteBuffer.allocateDirect(NexpadProtocol.INPUT_PACKET_SIZE)
    private val sendByteArray = ByteArray(NexpadProtocol.INPUT_PACKET_SIZE)
    
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
            } catch (e: Exception) {
                onConnectionStateChanged?.invoke(false)
                onStatusChanged?.invoke("Connection failed: ${e.message}")
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
                        
                        // Attempt binary decode first (6 bytes for feedback)
                        var feedback = NexpadProtocol.decodeFeedback(bytes)
                        
                        // Fallback to JSON if binary decode fails (backward compatibility)
                        if (feedback == null) {
                            try {
                                val json = String(bytes)
                                feedback = Json.decodeFromString<GamepadFeedback>(json)
                            } catch (e: Exception) {
                                // Ignored: Not valid JSON either
                            }
                        }
                        
                        feedback?.let { 
                            onFeedbackReceived?.invoke(it) 
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
    
    override suspend fun sendInput(input: GamepadInput) {
        val currentChannel = channel ?: return
        val target = serverAddress ?: return
        
        try {
            // Write directly to the pre-allocated sendBuffer
            NexpadProtocol.encodeInput(input, sendByteArray)
            
            sendBuffer.clear()
            sendBuffer.put(sendByteArray)
            sendBuffer.flip()
            
            // Send the pre-allocated packet
            currentChannel.send(sendBuffer, target)
            
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
