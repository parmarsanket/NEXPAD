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
        "L2" to Position(0.1f, 0.1f),
        "L1" to Position(0.1f, 0.3f),
        "DPAD" to Position(0.15f, 0.6f),
        "L3" to Position(0.3f, 0.4f),
        
        "SELECT" to Position(0.4f, 0.1f),
        "GUIDE" to Position(0.5f, 0.1f),
        "START" to Position(0.6f, 0.1f),
        
        "R2" to Position(0.9f, 0.1f),
        "R1" to Position(0.9f, 0.3f),
        "ABXY" to Position(0.85f, 0.4f),
        "R3" to Position(0.7f, 0.6f)
    )
}
