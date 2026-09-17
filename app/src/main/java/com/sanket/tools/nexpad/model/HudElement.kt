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
    val opacity: Float = 1.0f
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

    /** Vector, SVG, or dynamic Compose plugin loaded from ComponentRegistry */
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
        get() = spec?.label ?: controlKey

    val categoryTitle: String
        get() = spec?.categoryType?.title ?: "Control"

    val emoji: String
        get() = spec?.emoji ?: ""

    fun toPosition(): Position = Position(
        xRatio = transform.xRatio,
        yRatio = transform.yRatio,
        scale = transform.scale,
        opacity = transform.opacity,
        customComponentId = skinId
    )

    companion object {
        fun fromPosition(key: String, position: Position): HudElement = HudElement(
            controlKey = key.uppercase(),
            transform = LayoutTransform(
                xRatio = position.xRatio,
                yRatio = position.yRatio,
                scale = position.scale,
                opacity = position.opacity
            ),
            skinId = position.customComponentId
        )
    }
}
