package com.sanket.tools.nexpad.protocol

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * NEXPAD Universal Binary Protocol encoder and decoder.
 *
 * Provides optimized serialization and deserialization using BIG_ENDIAN network byte order.
 *
 * Input Packet Byte Layout (40 bytes):
 * | Offset | Size | Type  | Description                             |
 * |--------|------|-------|-----------------------------------------|
 * | 0      | 1    | Int8  | Protocol Version                        |
 * | 1      | 4    | Int32 | Buttons bitmask                         |
 * | 5      | 8    | Int16 | Sticks (L_X, L_Y, R_X, R_Y) * 4 x 2B    |
 * | 13     | 2    | UInt8 | Triggers (L2, R2) * 2 x 1B              |
 * | 15     | 24   | Float | Sensors (Gyro X/Y/Z, Accel X/Y/Z) * 6x4 |
 * | 39     | 1    | Int8  | Reserved (0x00)                         |
 *
 * Feedback Packet Byte Layout (6 bytes):
 * | Offset | Size | Type  | Description                             |
 * |--------|------|-------|-----------------------------------------|
 * | 0      | 1    | Int8  | Protocol Version                        |
 * | 1      | 2    | UInt8 | Motors (Left, Right) * 2 x 1B           |
 * | 3      | 3    | UInt8 | Lightbar RGB (R, G, B) * 3 x 1B         |
 */
object NexpadProtocol {

    const val PROTOCOL_VERSION: Byte = 1
    const val INPUT_PACKET_SIZE = 44
    const val FEEDBACK_PACKET_SIZE = 10

    private val packetSequenceNumber = java.util.concurrent.atomic.AtomicInteger(0)

    private val encodeBuffer = ByteBuffer.allocate(INPUT_PACKET_SIZE).apply {
        order(ByteOrder.BIG_ENDIAN)
    }

    /**
     * Encodes [GamepadInput] into a 44-byte binary packet.
     * Writes directly into [outputData] to avoid memory allocations.
     * Uses BIG_ENDIAN network byte order.
     * 
     * Button mapping (23 bits used):
     * Bit 0: A, Bit 1: B, Bit 2: X, Bit 3: Y
     * Bit 4: DpadUp, Bit 5: DpadDown, Bit 6: DpadLeft, Bit 7: DpadRight
     * Bit 8: L1, Bit 9: R1, Bit 10: L3, Bit 11: R3
     * Bit 12: Start, Bit 13: Select, Bit 14: Guide, Bit 15: Share, Bit 16: Screenshot
     * Bit 17: M1, Bit 18: M2, Bit 19: M3, Bit 20: M4
     * Bit 21: Profile, Bit 22: Turbo
     */
    fun encodeInput(input: GamepadInput, outputData: ByteArray) {
        synchronized(encodeBuffer) {
            encodeBuffer.clear()

            // 1. Version (1 byte)
            encodeBuffer.put(PROTOCOL_VERSION)

            // 2. Buttons bitmask (4 bytes)
            var buttonMask = 0
            if (input.btnA) buttonMask = buttonMask or (1 shl 0)
            if (input.btnB) buttonMask = buttonMask or (1 shl 1)
            if (input.btnX) buttonMask = buttonMask or (1 shl 2)
            if (input.btnY) buttonMask = buttonMask or (1 shl 3)
            if (input.dpadUp) buttonMask = buttonMask or (1 shl 4)
            if (input.dpadDown) buttonMask = buttonMask or (1 shl 5)
            if (input.dpadLeft) buttonMask = buttonMask or (1 shl 6)
            if (input.dpadRight) buttonMask = buttonMask or (1 shl 7)
            if (input.btnL1) buttonMask = buttonMask or (1 shl 8)
            if (input.btnR1) buttonMask = buttonMask or (1 shl 9)
            if (input.btnL3) buttonMask = buttonMask or (1 shl 10)
            if (input.btnR3) buttonMask = buttonMask or (1 shl 11)
            if (input.btnStart) buttonMask = buttonMask or (1 shl 12)
            if (input.btnSelect) buttonMask = buttonMask or (1 shl 13)
            if (input.btnGuide) buttonMask = buttonMask or (1 shl 14)
            if (input.btnShare) buttonMask = buttonMask or (1 shl 15)
            if (input.btnScreenshot) buttonMask = buttonMask or (1 shl 16)
            if (input.btnM1) buttonMask = buttonMask or (1 shl 17)
            if (input.btnM2) buttonMask = buttonMask or (1 shl 18)
            if (input.btnM3) buttonMask = buttonMask or (1 shl 19)
            if (input.btnM4) buttonMask = buttonMask or (1 shl 20)
            if (input.btnProfile) buttonMask = buttonMask or (1 shl 21)
            if (input.btnTurbo) buttonMask = buttonMask or (1 shl 22)
            
            encodeBuffer.putInt(buttonMask)

            // 3. Sticks (8 bytes) - Convert float [-1.0, 1.0] to Int16 [-32768, 32767]
            encodeBuffer.putShort(floatToInt16(input.leftStickX))
            encodeBuffer.putShort(floatToInt16(input.leftStickY))
            encodeBuffer.putShort(floatToInt16(input.rightStickX))
            encodeBuffer.putShort(floatToInt16(input.rightStickY))

            // 4. Triggers (2 bytes) - Convert float [0.0, 1.0] to UInt8 [0, 255]
            encodeBuffer.put(floatToUInt8(input.triggerL2))
            encodeBuffer.put(floatToUInt8(input.triggerR2))

            // 5. Sensors (24 bytes) - 6x4 Floats
            encodeBuffer.putFloat(input.gyroX)
            encodeBuffer.putFloat(input.gyroY)
            encodeBuffer.putFloat(input.gyroZ)
            encodeBuffer.putFloat(input.accelX)
            encodeBuffer.putFloat(input.accelY)
            encodeBuffer.putFloat(input.accelZ)

            // 6. Reserved (1 byte)
            encodeBuffer.put(0.toByte())
            
            // 7. Sequence Number (4 bytes)
            encodeBuffer.putInt(packetSequenceNumber.getAndIncrement())

            System.arraycopy(encodeBuffer.array(), 0, outputData, 0, INPUT_PACKET_SIZE)
        }
    }

    // Expose the current sequence number for logging
    fun getCurrentSequenceNumber(): Int = packetSequenceNumber.get() - 1

    /**
     * Decodes a 10-byte binary packet into [GamepadFeedback] and the Echoed Sequence Number.
     * Uses BIG_ENDIAN network byte order.
     * 
     * @return Pair of GamepadFeedback and Echo Sequence Number. Null if invalid.
     */
    fun decodeFeedback(data: ByteArray): Pair<GamepadFeedback, Int>? {
        if (data.size != FEEDBACK_PACKET_SIZE) return null
        
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // 1. Version (1 byte)
        val version = buffer.get()
        if (version != PROTOCOL_VERSION) return null

        // 2. Motors (2 bytes)
        val leftMotorSpeed = buffer.get().toInt() and 0xFF
        val rightMotorSpeed = buffer.get().toInt() and 0xFF
        
        // 3. Lightbar RGB (3 bytes) - Ignored in the current model
        buffer.get()
        buffer.get()
        buffer.get()
        
        // 4. Echoed Sequence Number (4 bytes)
        val echoSequence = buffer.getInt()

        return Pair(GamepadFeedback(leftMotorSpeed, rightMotorSpeed), echoSequence)
    }

    private fun floatToInt16(value: Float): Short {
        val clamped = value.coerceIn(-1.0f, 1.0f)
        return if (clamped >= 0) {
            (clamped * 32767f).toInt().toShort()
        } else {
            (clamped * 32768f).toInt().toShort()
        }
    }

    private fun floatToUInt8(value: Float): Byte {
        val clamped = value.coerceIn(0.0f, 1.0f)
        return (clamped * 255).toInt().toByte()
    }
}
