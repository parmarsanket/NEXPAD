package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NetworkClient : IGamepadConnection {
    private var socket: BoundDatagramSocket? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null

    override suspend fun connect(address: String, port: Int) = withContext(Dispatchers.IO) {
        serverAddress = InetSocketAddress(address, port)
        val selectorManager = SelectorManager(Dispatchers.IO)
        // Bind to any local port
        socket = aSocket(selectorManager).udp().bind()
        
        receiveJob = launch {
            while (isActive) {
                try {
                    val datagram = socket?.receive() ?: break
                    val json = String(datagram.packet.readBytes())
                    val feedback = Json.decodeFromString<GamepadFeedback>(json)
                    onFeedbackReceived?.invoke(feedback)
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
            // Convert state to JSON string
            val jsonString = Json.encodeToString(input)
            // Send as UDP Datagram
            val packet = Datagram(
                ByteReadPacket(jsonString.toByteArray()),
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
    }
}
