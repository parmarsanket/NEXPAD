package com.sanket.tools.nexpad.model

import kotlinx.serialization.Serializable

@Serializable
data class GamepadInput(
    // Face Buttons
    var btnA: Boolean = false,
    var btnB: Boolean = false,
    var btnX: Boolean = false,
    var btnY: Boolean = false,

    // D-Pad
    var dpadUp: Boolean = false,
    var dpadDown: Boolean = false,
    var dpadLeft: Boolean = false,
    var dpadRight: Boolean = false,

    // Bumpers & System
    var btnL1: Boolean = false,
    var btnR1: Boolean = false,
    var btnL3: Boolean = false,
    var btnR3: Boolean = false,
    var btnStart: Boolean = false,
    var btnSelect: Boolean = false,
    var btnGuide: Boolean = false,

    // Triggers (0.0 to 1.0)
    var triggerL2: Float = 0f,
    var triggerR2: Float = 0f,

    // Left Joystick (-1.0 to 1.0)
    var leftStickX: Float = 0f,
    var leftStickY: Float = 0f,

    // Right Joystick (-1.0 to 1.0)
    var rightStickX: Float = 0f,
    var rightStickY: Float = 0f,

    // Gyroscope data
    var gyroX: Float = 0f,
    var gyroY: Float = 0f,
    var gyroZ: Float = 0f
)
