package com.sanket.tools.nexpad.runtime.model

import kotlinx.serialization.Serializable

@Serializable
data class NxpComponentDef(
    val manifest: NxpManifest,
    val geometry: NxpGeometry = NxpGeometry(),
    val visual: NxpVisual = NxpVisual(),
    val pressed: NxpPressedState? = NxpPressedState(),
    val interaction: NxpInteraction = NxpInteraction(),
    val label: NxpLabel? = null,
    val size: NxpSize = NxpSize()
)

@Serializable
data class NxpManifest(
    val id: String,
    val name: String,
    val author: String = "AI Assistant",
    val version: String = "1.0.0",
    val category: String = "BUTTON", // BUTTON, JOYSTICK, TRIGGER, DPAD
    val defaultControl: String = "A",
    val description: String = ""
)

@Serializable
data class NxpGeometry(
    val type: String = "Polygon", // Polygon, Circle, RoundedRect, SvgPath
    val sides: Int = 6,
    val cornerRadius: Float = 8f,
    val pathData: String? = null
)

@Serializable
data class NxpVisual(
    val fillColor: String = "#0A192F",
    val opacity: Float = 0.9f,
    val borderColor: String = "#00F0FF",
    val borderWidth: Float = 2f,
    val glowColor: String? = "#00F0FF",
    val glowRadius: Float = 8f,
    val agslShader: String? = null
)

@Serializable
data class NxpPressedState(
    val scale: Float = 0.88f,
    val rotation: Float = 0f,
    val fillColor: String? = "#00F0FF",
    val borderColor: String? = "#FFFFFF",
    val glowRadius: Float = 16f,
    val springDamping: Float = 0.6f,
    val springStiffness: Float = 800f
)

@Serializable
data class NxpInteraction(
    val type: String = "Momentary", // Momentary, Joystick, Trigger
    val haptic: String = "Light",
    val deadzone: Float = 0.05f,
    val maxRadiusRatio: Float = 1.0f
)

@Serializable
data class NxpLabel(
    val text: String = "A",
    val color: String = "#00F0FF",
    val pressedColor: String = "#000000",
    val fontSize: Float = 20f
)

@Serializable
data class NxpSize(
    val widthDp: Int = 76,
    val heightDp: Int = 76
)
