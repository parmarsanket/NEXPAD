package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Handles the UDP network connection for sending gamepad input and receiving feedback.
 * Uses the NEXPAD Universal Binary Protocol for optimal performance,
 * with a fallback to JSON decoding for backward compatibility with older servers.
 */
class NetworkClient : IGamepadConnection {
    private var socket: BoundDatagramSocket? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null

    override suspend fun connect(address: String, port: Int) {
        withContext(Dispatchers.IO) {
            serverAddress = InetSocketAddress(address, port)
            val selectorManager = SelectorManager(Dispatchers.IO)
            // Bind to any local port
            socket = aSocket(selectorManager).udp().bind()
            onConnectionStateChanged?.invoke(true)
            onStatusChanged?.invoke("Connected to $address:$port")
        }
        
        // Launch receive job in a separate scope so connect() can return immediately
        receiveJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val datagram = socket?.receive() ?: break
                    val bytes = datagram.packet.readBytes()
                    
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
                } catch (e: Exception) {
                    // Ignore silent drops
                }
            }
        }
    }

    override suspend fun sendInput(input: GamepadInput) = withContext(Dispatchers.IO) {
        val currentSocket = socket
        val target = serverAddress
        if (currentSocket == null || target == null) return@withContext
        
        try {
            // Encode input state to binary packet using the NEXPAD protocol
            val packetData = NexpadProtocol.encodeInput(input)
            
            // Send as UDP Datagram
            val packet = Datagram(
                ByteReadPacket(packetData),
                target
            )
            currentSocket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun disconnect() {
        receiveJob?.cancel()
        socket?.close()
        socket = null
        onConnectionStateChanged?.invoke(false)
        onStatusChanged?.invoke("Disconnected")
    }

    override fun startAdvertising() {
        // No-op for network client
    }

    override fun close() = disconnect()
}
