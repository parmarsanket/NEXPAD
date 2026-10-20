package com.sanket.tools.nexpad

import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import com.sanket.tools.nexpad.viewmodel.SensorController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SensorControllerTest {

    private lateinit var inputState: GamepadInput
    private lateinit var controller: SensorController

    @Before
    fun setUp() {
        inputState = GamepadInput()
        controller = SensorController(inputState)
    }

    @Test
    fun testSanitizeAccelRejectsNaNAndInfinity() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)

        controller.updateAccel(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)

        assertEquals(0f, inputState.accelX, 0.0001f)
        assertEquals(0f, inputState.accelY, 0.0001f)
        assertEquals(0f, inputState.accelZ, 0.0001f)
    }

    @Test
    fun testValidAccelUpdatesProperly() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)

        controller.updateAccel(1.5f, -2.3f, 9.8f)

        assertEquals(1.5f, inputState.accelX, 0.0001f)
        assertEquals(-2.3f, inputState.accelY, 0.0001f)
        assertEquals(9.8f, inputState.accelZ, 0.0001f)
    }

    @Test
    fun testSanitizeGyroRejectsNaN() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)

        controller.update6AxisGyro(Float.NaN, 0.5f, Float.POSITIVE_INFINITY)

        assertEquals(0f, inputState.gyroX, 0.0001f)
        assertEquals(0.5f, inputState.gyroY, 0.0001f)
        assertEquals(0f, inputState.gyroZ, 0.0001f)
    }

    @Test
    fun testGameRotationQuaternionZeroResetsToIdentity() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_GAME_ROTATION)

        // Pass 0 length quaternion (division by zero / uninitialized sensor state)
        controller.updateGameRotation(0f, 0f, 0f, 0f)

        assertEquals(0f, inputState.quatX, 0.0001f)
        assertEquals(0f, inputState.quatY, 0.0001f)
        assertEquals(0f, inputState.quatZ, 0.0001f)
        assertEquals(1f, inputState.quatW, 0.0001f)

        // Pass NaN quaternion
        controller.updateGameRotation(Float.NaN, Float.NaN, Float.NaN, Float.NaN)

        assertEquals(0f, inputState.quatX, 0.0001f)
        assertEquals(0f, inputState.quatY, 0.0001f)
        assertEquals(0f, inputState.quatZ, 0.0001f)
        assertEquals(1f, inputState.quatW, 0.0001f)
    }

    @Test
    fun testValidGameRotationPreserved() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_GAME_ROTATION)

        controller.updateGameRotation(0f, 0.7071f, 0f, 0.7071f)

        assertEquals(0f, inputState.quatX, 0.0001f)
        assertEquals(0.7071f, inputState.quatY, 0.0001f)
        assertEquals(0f, inputState.quatZ, 0.0001f)
        assertEquals(0.7071f, inputState.quatW, 0.0001f)
    }

    @Test
    fun testLinearGestureSanitizesAndCombinesGravity() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_LINEAR_GESTURE)

        controller.updateLinearGesture(
            lx = 0.5f, ly = Float.NaN, lz = 1.0f,
            gx = 0f, gy = 9.8f, gz = 0f,
            rx = 0.1f, ry = 0.2f, rz = Float.POSITIVE_INFINITY
        )

        assertEquals(0.5f, inputState.linearAccelX, 0.0001f)
        assertEquals(0f, inputState.linearAccelY, 0.0001f)
        assertEquals(1.0f, inputState.linearAccelZ, 0.0001f)
        assertEquals(0f + 0.5f, inputState.accelX, 0.0001f)
        assertEquals(9.8f + 0f, inputState.accelY, 0.0001f)
        assertEquals(0.1f, inputState.gyroX, 0.0001f)
        assertEquals(0.2f, inputState.gyroY, 0.0001f)
        assertEquals(0f, inputState.gyroZ, 0.0001f)
    }

    @Test
    fun testMuteZeroesAllSensors() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)
        controller.updateAccel(1f, 2f, 3f)
        controller.update6AxisGyro(4f, 5f, 6f)

        controller.isMotionMuted = true

        assertEquals(0f, inputState.accelX, 0.0001f)
        assertEquals(0f, inputState.accelY, 0.0001f)
        assertEquals(0f, inputState.accelZ, 0.0001f)
        assertEquals(0f, inputState.gyroX, 0.0001f)
        assertEquals(0f, inputState.gyroY, 0.0001f)
        assertEquals(0f, inputState.gyroZ, 0.0001f)

        // Further updates while muted are ignored
        controller.updateAccel(9f, 9f, 9f)
        assertEquals(0f, inputState.accelX, 0.0001f)
    }

    @Test
    fun testModeSwitchClearsChannels() {
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_IMU_6AXIS)
        controller.updateAccel(1f, 2f, 3f)

        // Switch to Gravity mode
        controller.setRequestedSensorMode(NexpadProtocol.SENSOR_MODE_GRAVITY)

        // IMU accel must be zeroed on mode transition
        assertEquals(0f, inputState.accelX, 0.0001f)
        assertEquals(NexpadProtocol.SENSOR_MODE_GRAVITY, controller.requestedSensorMode)
    }
}
