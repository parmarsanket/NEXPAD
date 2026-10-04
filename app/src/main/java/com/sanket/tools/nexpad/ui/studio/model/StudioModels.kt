package com.sanket.tools.nexpad.ui.studio.model

import androidx.compose.ui.graphics.Color
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.registry.NativeComponentRegistry

/**
 * Operating mode for Button Studio:
 * - VIEWER: Universal Button Showcase & Library. Browse, test in sandbox, import new buttons. Not tied to any layout.
 * - EDITOR: Layout Customizer. Opened from Virtual Controller list or HUD Editor for a particular layout.
 */
enum class ButtonStudioMode(val label: String) {
    VIEWER("Viewer"),
    EDITOR("Editor"),
    BUTTON_EDITOR("Button Editor");

    companion object {
        val MANAGE get() = VIEWER
        val SELECTION get() = EDITOR
    }
}

/**
 * Categorization badges for button sources in Button Studio:
 * - DEFAULT: Authentic native controller component from ui/components/controller/ and NativeComponentRegistry
 * - PLUGIN: Dynamic Compose / AI imported JSON plugin from ComponentRegistry
 * - REMOTE_COMPOSE: Remote Compose components (.nxprc)
 */
enum class ButtonStudioType(val label: String, val badgeColor: Color, val badgeBg: Color) {
    DEFAULT("DEFAULT", Color(0xFF00E5FF), Color(0x2200E5FF)),
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
        NativeComponentRegistry.isNativeBuiltin(id) || id.startsWith("builtin.") -> ButtonStudioType.DEFAULT
        else -> ButtonStudioType.PLUGIN
    }
}

/** Backward compatible alias */
fun getButtonStudioType(def: NxpComponentDef): ButtonStudioType = resolveButtonSourceType(def)
