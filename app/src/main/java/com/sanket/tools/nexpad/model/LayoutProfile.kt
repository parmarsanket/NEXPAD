package com.sanket.tools.nexpad.model

import kotlinx.serialization.Serializable

@Serializable
data class LayoutProfile(
    val name: String = "Standard",
    val isRgbEnabled: Boolean = true,
    val positions: Map<String, Position> = defaultPositions()
)

@Serializable
data class Position(
    val xRatio: Float, 
    val yRatio: Float,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f
)

fun defaultPositions(): Map<String, Position> {
    // Exact Xbox Controller Layout Mapping (Relative coordinates 0.0 to 1.0)
    return mapOf(
        // Triggers and Bumpers (Top Corners)
        "LT" to Position(0.1f, 0.1f, scale = 1.2f),
        "LB" to Position(0.1f, 0.25f),
        "RT" to Position(0.9f, 0.1f, scale = 1.2f),
        "RB" to Position(0.9f, 0.25f),
        
        // Sticks and D-Pad (Left side)
        "LS" to Position(0.2f, 0.45f, scale = 1.4f), // Left Stick upper
        "DPAD" to Position(0.35f, 0.75f, scale = 1.3f), // D-Pad lower right of LS
        
        // Right Stick and Face Buttons (Right side)
        "Y" to Position(0.8f, 0.4f),
        "X" to Position(0.72f, 0.52f),
        "B" to Position(0.88f, 0.52f),
        "A" to Position(0.8f, 0.64f),
        "RS" to Position(0.65f, 0.75f, scale = 1.4f), // Right Stick lower left of ABXY
        
        // System Buttons (Center)
        "VIEW" to Position(0.42f, 0.45f, scale = 0.8f), // Select/Back
        "MENU" to Position(0.58f, 0.45f, scale = 0.8f), // Start
        "XBOX" to Position(0.5f, 0.3f, scale = 1.2f), // Guide/Home

        // Extra Elite Macros (Hidden by default / stored for add menu)
        "SHARE" to Position(0.5f, 0.55f, scale = 0.8f),
        "M1" to Position(0.3f, 0.85f),
        "M2" to Position(0.4f, 0.85f),
        "M3" to Position(0.6f, 0.85f),
        "M4" to Position(0.7f, 0.85f)
    )
}
