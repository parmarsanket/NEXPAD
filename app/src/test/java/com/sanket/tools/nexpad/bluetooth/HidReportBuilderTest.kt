package com.sanket.tools.nexpad.bluetooth

import com.sanket.tools.nexpad.model.GamepadInput
import org.junit.Assert.assertEquals
import org.junit.Test

class HidReportBuilderTest {
    @Test
    fun neutralReportHasExpectedSizeAndNullHat() {
        val report = HidReportBuilder.build(GamepadInput())

        assertEquals(HidDescriptors.GAMEPAD_REPORT_SIZE, report.size)
        assertEquals(8, report[12].toInt())
    }

    @Test
    fun mapsButtonsAxesTriggersAndDiagonalHat() {
        val report = HidReportBuilder.build(
            GamepadInput(
                btnA = true,
                btnStart = true,
                btnM4 = true,
                dpadUp = true,
                dpadRight = true,
                triggerL2 = 1f,
                leftStickX = 1f,
                leftStickY = 1f
            )
        )

        assertEquals(0x81, report[0].toInt() and 0xFF)
        assertEquals(0x80, report[1].toInt() and 0xFF)
        assertEquals(0x7FFF, littleEndianShort(report, 2))
        assertEquals(0x8001, littleEndianShort(report, 4))
        assertEquals(255, report[10].toInt() and 0xFF)
        assertEquals(1, report[12].toInt())
    }

    private fun littleEndianShort(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
}
