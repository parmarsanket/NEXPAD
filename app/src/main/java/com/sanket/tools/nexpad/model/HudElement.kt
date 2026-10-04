package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.SubCategoryDefinition
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.plugin.NxprcDocument

/**
 * Normalized 2D transform for positioning controls on any display ratio.
 */
data class LayoutTransform(
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f,
    val sensitivity: Float = 2.0f,
    val heightScale: Float = 1.0f,
    val isFlipped: Boolean = false,
    val isLocked: Boolean = true,
    val joystickMode: String = "LOCKED",
    val hitboxScale: Float = 1.5f
)

/**
 * Unified representation of a button's visual skin.
 */
sealed interface LayoutSkin {
    val id: String?
    val name: String

    /** Authentic 3D realistic controller button from ui/components/controller/ */
    data object NativeDefault : LayoutSkin {
        override val id: String? = null
        override val name: String = "Default 3D"
    }

    /** Dynamic JSON plugin loaded from ComponentRegistry */
    data class CustomComponent(val def: NxpComponentDef) : LayoutSkin {
        override val id: String = def.manifest.id
        override val name: String = def.manifest.name
    }

    /** Remote Compose binary plugin (.nxprc) loaded from RemoteComponentRegistry */
    data class RemoteComponent(val doc: NxprcDocument) : LayoutSkin {
        override val id: String = doc.manifest.id
        override val name: String = doc.manifest.name
    }
}

/**
 * Concrete placed HUD element representing one controller button/input on screen.
 * Backed directly by protocol CategoryManager's SubCategoryDefinition.
 * Completely eliminates any local redundant control category or enum definitions.
 */
data class HudElement(
    val controlKey: String,
    val transform: LayoutTransform,
    val skinId: String? = null // null indicates LayoutSkin.NativeDefault
) {
    val spec: SubCategoryDefinition?
        get() = CategoryManager.getControl(controlKey)

    val displayName: String
        get() = spec?.label ?: if (controlKey.equals("GYRO", ignoreCase = true)) "Gyro Toggle" else controlKey

    val categoryTitle: String
        get() = spec?.categoryType?.title ?: if (controlKey.equals("GYRO", ignoreCase = true)) "Special" else "Control"

    val emoji: String
        get() = spec?.emoji ?: if (controlKey.equals("GYRO", ignoreCase = true)) "🎯" else ""

    fun toPosition(): Position = Position(
        xRatio = transform.xRatio,
        yRatio = transform.yRatio,
        scale = transform.scale,
        opacity = transform.opacity,
        customComponentId = skinId,
        sensitivity = transform.sensitivity,
        heightScale = transform.heightScale,
        isFlipped = transform.isFlipped,
        isLocked = transform.isLocked,
        joystickMode = transform.joystickMode,
        hitboxScale = transform.hitboxScale
    )

    companion object {
        fun fromPosition(key: String, position: Position): HudElement {
            val resolvedMode = position.joystickMode ?: if (position.isLocked == false) "BOX" else "LOCKED"
            val resolvedLocked = (resolvedMode == "LOCKED")
            return HudElement(
                controlKey = key.uppercase(),
                transform = LayoutTransform(
                    xRatio = position.xRatio,
                    yRatio = position.yRatio,
                    scale = position.scale,
                    opacity = position.opacity,
                    sensitivity = position.sensitivity ?: 2.0f,
                    heightScale = position.heightScale ?: 1.0f,
                    isFlipped = position.isFlipped ?: false,
                    isLocked = resolvedLocked,
                    joystickMode = resolvedMode,
                    hitboxScale = position.hitboxScale ?: 1.5f
                ),
                skinId = position.customComponentId
            )
        }
    }
}
