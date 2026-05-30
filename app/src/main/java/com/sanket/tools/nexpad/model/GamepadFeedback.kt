package com.sanket.tools.nexpad.model

import kotlinx.serialization.Serializable

@Serializable
data class GamepadFeedback(
    val leftMotorSpeed: Int, // 0-255 (Heavy rumble)
    val rightMotorSpeed: Int // 0-255 (Light rumble)
)
