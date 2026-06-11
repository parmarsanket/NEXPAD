package com.sanket.tools.nexpad.bluetooth

import com.sanket.tools.nexpad.model.GamepadInput

object HidReportBuilder {
    fun build(input: GamepadInput): ByteArray {
        val report = ByteArray(HidDescriptors.GAMEPAD_REPORT_SIZE)

        // 32 Buttons (Bytes 0-3)
        setButton(report, 0, input.btnA)
        setButton(report, 1, input.btnB)
        setButton(report, 2, input.btnX)
        setButton(report, 3, input.btnY)
        setButton(report, 4, input.btnL1) // Button 5
        setButton(report, 5, input.btnR1) // Button 6
        setButton(report, 6, input.triggerL2 > 0.5f) // Button 7 (Digital LT Fallback)
        setButton(report, 7, input.triggerR2 > 0.5f) // Button 8 (Digital RT Fallback)
        setButton(report, 8, input.btnSelect) // Button 9
        setButton(report, 9, input.btnStart)  // Button 10
        setButton(report, 10, input.btnL3)    // Button 11
        setButton(report, 11, input.btnR3)    // Button 12
        setButton(report, 12, input.btnGuide) // Button 13

        // Extra buttons mapped safely away from standard layout
        setButton(report, 16, input.btnM1)    // Button 17
        setButton(report, 17, input.btnM2)    // Button 18
        setButton(report, 18, input.btnM3)    // Button 19
        setButton(report, 19, input.btnM4)    // Button 20
        setButton(report, 20, input.btnProfile)
        setButton(report, 21, input.btnTurbo)
        setButton(report, 22, input.btnScreenshot)

        // Axes (Bytes 4-11)
        putAxis(report, 4, input.leftStickX)
        putAxis(report, 6, -input.leftStickY)
        putAxis(report, 8, input.rightStickX)
        putAxis(report, 10, -input.rightStickY)

        // D-Pad (Byte 12)
        report[12] = hatValue(input).toByte()

        return report
    }

    private fun setButton(report: ByteArray, bit: Int, pressed: Boolean) {
        if (pressed) {
            val byteIndex = bit / 8
            report[byteIndex] = (report[byteIndex].toInt() or (1 shl (bit % 8))).toByte()
        }
    }

    private fun putAxis(report: ByteArray, offset: Int, value: Float) {
        // Map from [-1f, 1f] to [0, 65535]
        // value + 1.0f -> [0f, 2f] -> * 32767.5f
        val axis = ((value.coerceIn(-1f, 1f) + 1f) * 32767.5f).toInt().coerceIn(0, 65535)
        report[offset] = axis.toByte()
        report[offset + 1] = (axis shr 8).toByte()
    }

    private fun hatValue(input: GamepadInput): Int = when {
        input.dpadUp && input.dpadRight -> 2
        input.dpadRight && input.dpadDown -> 4
        input.dpadDown && input.dpadLeft -> 6
        input.dpadLeft && input.dpadUp -> 8
        input.dpadUp -> 1
        input.dpadRight -> 3
        input.dpadDown -> 5
        input.dpadLeft -> 7
        else -> 0 // Null state
    }
}
