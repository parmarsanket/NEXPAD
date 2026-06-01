package com.sanket.tools.nexpad.model

import kotlinx.serialization.Serializable

@Serializable
data class LayoutProfile(
    val name: String = "Standard",
    val isRgbEnabled: Boolean = true,
    val positions: Map<String, Position> = defaultPositions()
)

@Serializable
data class Position(val xRatio: Float, val yRatio: Float)

fun defaultPositions(): Map<String, Position> {
    // Relative coordinates (0.0 to 1.0) of screen width/height
    return mapOf(
        // Triggers and Bumpers
        "LT" to Position(0.1f, 0.1f),
        "LB" to Position(0.1f, 0.25f),
        "RT" to Position(0.9f, 0.1f),
        "RB" to Position(0.9f, 0.25f),
        
        // Sticks and D-Pad
        "LS" to Position(0.2f, 0.45f), // Left Stick
        "DPAD" to Position(0.35f, 0.7f),
        "RS" to Position(0.65f, 0.7f), // Right Stick
        
        // Face Buttons
        "A" to Position(0.8f, 0.55f),
        "B" to Position(0.85f, 0.45f),
        "X" to Position(0.75f, 0.45f),
        "Y" to Position(0.8f, 0.35f),

        // System
        "VIEW" to Position(0.4f, 0.3f), // Select/Back
        "MENU" to Position(0.6f, 0.3f), // Start
        "XBOX" to Position(0.5f, 0.2f), // Guide/Home
        "SHARE" to Position(0.5f, 0.4f),
        "SCREENSHOT" to Position(0.5f, 0.5f),

        // Elite Macros
        "M1" to Position(0.3f, 0.85f),
        "M2" to Position(0.4f, 0.85f),
        "M3" to Position(0.6f, 0.85f),
        "M4" to Position(0.7f, 0.85f),
        "PROFILE" to Position(0.45f, 0.7f),
        "TURBO" to Position(0.55f, 0.7f)
    )
}
