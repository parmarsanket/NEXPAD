package com.sanket.tools.nexpad.ui.studio.model

import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef

/**
 * Operating mode for Button Studio:
 * - MANAGE: View existing buttons, test in sandbox, import AI JSON, copy JSON, delete custom buttons.
 * - SELECTION: Custom layout builder. Select one button skin per control type. Unselected buttons are ignored.
 */
enum class ButtonStudioMode(val label: String) {
    MANAGE("Manager"),
    SELECTION("Builder")
}

/**
 * Categorization badges for the 3 button types in Button Studio:
 * - DEFAULT: Authentic native 3D controller component from ui/components/controller/
 * - SVG: Cyber / Vector / Geometric polygon skin from DefaultComponents.kt
 * - PLUGIN: Dynamic Compose / AI imported JSON plugin from ComponentRegistry
 */
enum class ButtonStudioType(val label: String, val badgeColor: Color, val badgeBg: Color) {
    DEFAULT("DEFAULT", Color(0xFF00E5FF), Color(0x2200E5FF)),
    SVG("SVG / VECTOR", Color(0xFF39FF14), Color(0x2239FF14)),
    PLUGIN("PLUGIN / NXP", Color(0xFFB400FF), Color(0x22B400FF)),
    REMOTE_COMPOSE("REMOTE / RC", Color(0xFFFF9100), Color(0x22FF9100))
}

/**
 * Resolves the ButtonStudioType for a given component definition.
 */
fun resolveButtonSourceType(def: NxpComponentDef): ButtonStudioType =
    resolveButtonSourceType(def.manifest.id)

/**
 * Resolves the ButtonStudioType for a given component ID.
 */
fun resolveButtonSourceType(id: String?): ButtonStudioType {
    if (id == null) return ButtonStudioType.DEFAULT
    return when {
        id.startsWith("rc.") -> ButtonStudioType.REMOTE_COMPOSE
        id.startsWith("builtin.default_") -> ButtonStudioType.DEFAULT
        id.startsWith("builtin.") -> ButtonStudioType.SVG
        else -> ButtonStudioType.PLUGIN
    }
}

/** Backward compatible alias */
fun getButtonStudioType(def: NxpComponentDef): ButtonStudioType = resolveButtonSourceType(def)
