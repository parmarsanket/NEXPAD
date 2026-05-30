package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.model.GamepadInput
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NetworkClient {
    private var socket: BoundDatagramSocket? = null
    private var serverAddress: InetSocketAddress? = null

    suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {
        serverAddress = InetSocketAddress(ip, port)
        val selectorManager = SelectorManager(Dispatchers.IO)
        // Bind to any local port
        socket = aSocket(selectorManager).udp().bind()
    }

    suspend fun sendInput(input: GamepadInput) = withContext(Dispatchers.IO) {
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

    fun disconnect() {
        socket?.close()
        socket = null
    }
}
