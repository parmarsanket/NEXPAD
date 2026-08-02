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

    // Bumpers & Clicks
    var btnL1: Boolean = false, // LB
    var btnR1: Boolean = false, // RB
    var btnL3: Boolean = false, // LS Click
    var btnR3: Boolean = false, // RS Click

    // System Buttons
    var btnStart: Boolean = false, // Menu
    var btnSelect: Boolean = false, // View
    var btnGuide: Boolean = false, // Xbox / Home
    var btnShare: Boolean = false, // Share
    var btnScreenshot: Boolean = false,

    // Advanced / Elite
    var btnM1: Boolean = false,
    var btnM2: Boolean = false,
    var btnM3: Boolean = false,
    var btnM4: Boolean = false,
    var btnProfile: Boolean = false,
    var btnTurbo: Boolean = false,

    // Triggers (0.0 to 1.0)
    var triggerL2: Float = 0f,
    var triggerR2: Float = 0f,

    // Left Joystick (-1.0 to 1.0)
    var leftStickX: Float = 0f,
    var leftStickY: Float = 0f,

    // Right Joystick (-1.0 to 1.0)
    var rightStickX: Float = 0f,
    var rightStickY: Float = 0f,

    // Gyroscope data (Angular Velocity)
    var gyroX: Float = 0f,
    var gyroY: Float = 0f,
    var gyroZ: Float = 0f,

    // Accelerometer data (G-Force)
    var accelX: Float = 0f,
    var accelY: Float = 0f,
    var accelZ: Float = 0f
)