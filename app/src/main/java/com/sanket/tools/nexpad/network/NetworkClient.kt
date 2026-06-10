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
import java.nio.ByteBuffer

class NetworkClient : IGamepadConnection {
    private var socket: BoundDatagramSocket? = null
    private var serverAddress: InetSocketAddress? = null
    private var receiveJob: Job? = null
    
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null

    override suspend fun connect(address: String, port: Int) {
        withContext(Dispatchers.IO) {
            serverAddress = InetSocketAddress(address, port)
            val selectorManager = SelectorManager(Dispatchers.IO)
            // Bind to any local port
            socket = aSocket(selectorManager).udp().bind()
        }
        
        // Launch receive job in a separate scope so connect() can return immediately!
        receiveJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
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

//    import java.nio.ByteBuffer
//    import io.ktor.utils.io.core.ByteReadPacket
//    import io.ktor.network.sockets.Datagram
//
//    override suspend fun sendInput(input: GamepadInput) = withContext(Dispatchers.IO) {
//        val currentSocket = socket ?: return@withContext
//        val target = serverAddress ?: return@withContext
//
//        try {
//            // 48 bytes accommodates 1 Int (buttons) + 11 Floats (triggers, sticks, sensors)
//            val buffer = ByteBuffer.allocate(48)
//
//            // 1. Bitmask all 21 booleans into a single 32-bit Integer
//            var buttonMask = 0
//            if (input.btnA) buttonMask = buttonMask or (1 shl 0)
//            if (input.btnB) buttonMask = buttonMask or (1 shl 1)
//            if (input.btnX) buttonMask = buttonMask or (1 shl 2)
//            if (input.btnY) buttonMask = buttonMask or (1 shl 3)
//            if (input.dpadUp) buttonMask = buttonMask or (1 shl 4)
//            if (input.dpadDown) buttonMask = buttonMask or (1 shl 5)
//            if (input.dpadLeft) buttonMask = buttonMask or (1 shl 6)
//            if (input.dpadRight) buttonMask = buttonMask or (1 shl 7)
//            if (input.btnL1) buttonMask = buttonMask or (1 shl 8)
//            if (input.btnR1) buttonMask = buttonMask or (1 shl 9)
//            if (input.btnL3) buttonMask = buttonMask or (1 shl 10)
//            if (input.btnR3) buttonMask = buttonMask or (1 shl 11)
//            if (input.btnStart) buttonMask = buttonMask or (1 shl 12)
//            if (input.btnSelect) buttonMask = buttonMask or (1 shl 13)
//            if (input.btnGuide) buttonMask = buttonMask or (1 shl 14)
//            if (input.btnShare) buttonMask = buttonMask or (1 shl 15)
//            if (input.btnScreenshot) buttonMask = buttonMask or (1 shl 16)
//            if (input.btnM1) buttonMask = buttonMask or (1 shl 17)
//            if (input.btnM2) buttonMask = buttonMask or (1 shl 18)
//            if (input.btnM3) buttonMask = buttonMask or (1 shl 19)
//            if (input.btnM4) buttonMask = buttonMask or (1 shl 20)
//            if (input.btnProfile) buttonMask = buttonMask or (1 shl 21)
//            if (input.btnTurbo) buttonMask = buttonMask or (1 shl 22)
//
//            buffer.putInt(buttonMask)
//
//            // 2. Add analog triggers and joysticks
//            buffer.putFloat(input.triggerL2)
//            buffer.putFloat(input.triggerR2)
//            buffer.putFloat(input.leftStickX)
//            buffer.putFloat(input.leftStickY)
//            buffer.putFloat(input.rightStickX)
//            buffer.putFloat(input.rightStickY)
//
//            // 3. Add exact sensor data variables
//            buffer.putFloat(input.gyroX)
//            buffer.putFloat(input.gyroY)
//            buffer.putFloat(input.gyroZ)
//            buffer.putFloat(input.accelX)
//            buffer.putFloat(input.accelY)
//            buffer.putFloat(input.accelZ)
//
//            // Fire the 48-byte packet over Ktor UDP
//            val packet = Datagram(
//                ByteReadPacket(buffer.array()),
//                target
//            )
//            currentSocket.send(packet)
//        } catch (e: Exception) {
//            // Ignore silent drops
//        }
//    }

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
