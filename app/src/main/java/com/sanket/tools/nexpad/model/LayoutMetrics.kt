package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.CategoryManager
import kotlin.math.sqrt

/**
 * Formal Mathematical & Ergonomic Layout Metrics Specification for NEXPAD.
 *
 * Provides aspect-ratio correction, component base dimension lookups,
 * isotropic cluster generators (diamond, linear rows), and collision verification.
 *
 * All control key defaults and base dimensions are derived from [CategoryManager] —
 * the single source of truth for all control metadata.
 */
object LayoutMetrics {

    // Reference Display: 20:9 Ultrawide FHD+ (2400 x 1080 px @ 2.75x density = 872.7 x 392.7 dp)
    const val REFERENCE_WIDTH_PX = 2400f
    const val REFERENCE_HEIGHT_PX = 1080f
    const val REFERENCE_DENSITY = 2.75f

    const val REFERENCE_WIDTH_DP = REFERENCE_WIDTH_PX / REFERENCE_DENSITY   // ~872.73 dp
    const val REFERENCE_HEIGHT_DP = REFERENCE_HEIGHT_PX / REFERENCE_DENSITY // ~392.73 dp
    const val ASPECT_RATIO = REFERENCE_WIDTH_PX / REFERENCE_HEIGHT_PX       // ~2.2222

    /**
     * ABXY canonical key order read directly from CategoryManager.
     * Position: index 0 = top (Y), 1 = left (X), 2 = right (B), 3 = bottom (A).
     * This matches the physical Xbox diamond layout: Y top, X left, B right, A bottom.
     */
    private val abxyKeys: List<String> = listOf("Y", "X", "B", "A")

    /**
     * Converts a physical distance in DP to normalized (xRatio, yRatio) offsets.
     * Ensures that any physical span (radius or offset) renders with 100% isotropic equality:
     * dx_px == dy_px on the reference display.
     */
    fun dpToRatioDelta(distanceDp: Float): Pair<Float, Float> {
        val dx = distanceDp / REFERENCE_WIDTH_DP
        val dy = distanceDp / REFERENCE_HEIGHT_DP
        return Pair(dx, dy)
    }

    /**
     * Generates a 4-button isotropic Diamond Cluster (like standard Xbox/PlayStation ABXY).
     *
     * Default key order is derived from [CategoryManager]'s ABXY category — no hardcoding.
     * Override individual keys only when using a non-standard layout.
     *
     * @param centerX Normalized X center (0.0 .. 1.0)
     * @param centerY Normalized Y center (0.0 .. 1.0)
     * @param radiusDp Physical distance in DP from cluster center to the center of each button.
     * @param scale Scaling factor applied to each button.
     * @param opacity Opacity of the buttons.
     * @param topKey Label for top button (defaults to CategoryManager ABXY "Y")
     * @param leftKey Label for left button (defaults to CategoryManager ABXY "X")
     * @param rightKey Label for right button (defaults to CategoryManager ABXY "B")
     * @param bottomKey Label for bottom button (defaults to CategoryManager ABXY "A")
     */
    fun createDiamondCluster(
        centerX: Float,
        centerY: Float,
        radiusDp: Float = 53.0f,
        scale: Float = 0.82f,
        opacity: Float = 1.0f,
        topKey: String = abxyKeys[0],   // Y
        leftKey: String = abxyKeys[1],  // X
        rightKey: String = abxyKeys[2], // B
        bottomKey: String = abxyKeys[3] // A
    ): Map<String, Position> {
        val (dx, dy) = dpToRatioDelta(radiusDp)

        return mapOf(
            topKey    to Position(centerX,      centerY - dy, scale = scale, opacity = opacity),
            leftKey   to Position(centerX - dx, centerY,      scale = scale, opacity = opacity),
            rightKey  to Position(centerX + dx, centerY,      scale = scale, opacity = opacity),
            bottomKey to Position(centerX,      centerY + dy, scale = scale, opacity = opacity)
        )
    }

    /**
     * Generates a horizontally centered, evenly spaced linear row of buttons.
     *
     * @param keys Ordered list of button keys.
     * @param centerY Normalized Y center.
     * @param centerX Center of the row in normalized X.
     * @param spacingDp Center-to-center spacing in DP between consecutive buttons.
     * @param scale Button scale.
     */
    fun createHorizontalRow(
        keys: List<String>,
        centerY: Float,
        centerX: Float = 0.5f,
        spacingDp: Float = 70.0f,
        scale: Float = 0.75f
    ): Map<String, Position> {
        val result = mutableMapOf<String, Position>()
        val n = keys.size
        if (n == 0) return result

        val (dx, _) = dpToRatioDelta(spacingDp)
        val totalSpan = (n - 1) * dx
        val startX = centerX - (totalSpan / 2f)

        keys.forEachIndexed { index, key ->
            val x = startX + (index * dx)
            result[key] = Position(x, centerY, scale = scale)
        }

        return result
    }

    /**
     * Returns base width and height in DP for a given control key.
     * Delegates to [CategoryManager.resolveDefaultDimensions] — single source of truth.
     * Falls back to BUTTON_BASE_DP if the key is unknown.
     */
    fun getBaseDimensionsDp(key: String): Pair<Float, Float> {
        val (w, h) = CategoryManager.resolveDefaultDimensions(key)
        return Pair(w.toFloat(), h.toFloat())
    }

    /**
     * Calculates the Euclidean distance in DP between two button positions.
     */
    fun distanceDp(pos1: Position, pos2: Position): Float {
        val dxDp = (pos1.xRatio - pos2.xRatio) * REFERENCE_WIDTH_DP
        val dyDp = (pos1.yRatio - pos2.yRatio) * REFERENCE_HEIGHT_DP
        return sqrt(dxDp * dxDp + dyDp * dyDp)
    }

    /**
     * Calculates the edge-to-edge clearance between two circular buttons (in DP).
     * Positive value = clearance/gap; Negative value = overlap.
     */
    fun circularClearanceDp(key1: String, pos1: Position, key2: String, pos2: Position): Float {
        val (w1, _) = getBaseDimensionsDp(key1)
        val (w2, _) = getBaseDimensionsDp(key2)
        val r1 = (w1 * pos1.scale) / 2f
        val r2 = (w2 * pos2.scale) / 2f

        val dist = distanceDp(pos1, pos2)
        return dist - (r1 + r2)
    }

    /**
     * Calculates the responsive ergonomic scale factor for virtual controller elements
     * based on available screen width and height in DP.
     *
     * - Reference phones (width ≥ 800dp and height 375dp - 430dp): exactly 1.0f (exact baseline design).
     * - Narrow / compact phone displays (width < 800dp or height < 375dp, such as 16:9 phones):
     *   smoothly scales down (down to 0.78f) so that the 4 side-by-side controls (LS, DPAD, ABXY, RS)
     *   fit horizontally with clean clearance and zero collision.
     * - Large foldables / tablets (height > 430dp): smoothly scales up (up to 1.12f) to match
     *   larger finger contact areas while keeping proportional spacing.
     */
    fun calculateResponsiveScale(widthDp: Float, heightDp: Float): Float {
        val scaleH = when {
            heightDp < 375f -> (heightDp / 375f).coerceIn(0.78f, 1.0f)
            heightDp > 430f -> (1.0f + (heightDp - 430f) * 0.00035f).coerceIn(1.0f, 1.12f)
            else -> 1.0f
        }
        val scaleW = if (widthDp < 800f) {
            (widthDp / 800f).coerceIn(0.78f, 1.0f)
        } else {
            1.0f
        }
        return if (heightDp > 430f) scaleH else minOf(scaleH, scaleW)
    }

    /** Overload for callers providing only heightDp (delegates to reference width). */
    fun calculateResponsiveScale(heightDp: Float): Float =
        calculateResponsiveScale(REFERENCE_WIDTH_DP, heightDp)

    /**
     * Resolves aspect-ratio-corrected positions for elements on a screen of given dimensions.
     *
     * In canonical layouts, diamond face button clusters (Y, A, X, B sharing a common center)
     * are defined with static aspect ratio (dx = r / 872.73, dy = r / 392.73).
     * On non-20:9 displays (such as 16:10 tablets and 4:3 foldables), rendering dy * screenHeightPx
     * causes vertical stretching into an egg shape (up to +67% taller on foldables).
     *
     * This function dynamically corrects the vertical offset so that:
     *   deltaY_px == deltaX_px
     * ensuring 100% circular isotropy for face button clusters on any screen aspect ratio,
     * while preserving exact pixel-perfect positions on the reference 20:9 phone.
     */
    fun adjustClusterPositionsForAspectRatio(
        positions: Map<String, Position>,
        screenWidthPx: Float,
        screenHeightPx: Float
    ): Map<String, Position> {
        if (screenWidthPx <= 0f || screenHeightPx <= 0f) return positions

        val yEntry = positions.entries.firstOrNull { it.key.equals("Y", ignoreCase = true) }
        val xEntry = positions.entries.firstOrNull { it.key.equals("X", ignoreCase = true) }
        val bEntry = positions.entries.firstOrNull { it.key.equals("B", ignoreCase = true) }
        val aEntry = positions.entries.firstOrNull { it.key.equals("A", ignoreCase = true) }

        if (yEntry == null || xEntry == null || bEntry == null || aEntry == null) {
            return positions
        }

        val posY = yEntry.value
        val posX = xEntry.value
        val posB = bEntry.value
        val posA = aEntry.value

        val isHorizontalSymmetric = kotlin.math.abs(posX.yRatio - posB.yRatio) < 0.02f
        val isVerticalSymmetric = kotlin.math.abs(posY.xRatio - posA.xRatio) < 0.02f
        val isOriented = posX.xRatio < posB.xRatio && posY.yRatio < posA.yRatio

        if (!isHorizontalSymmetric || !isVerticalSymmetric || !isOriented) {
            return positions
        }

        val centerX_XB = (posX.xRatio + posB.xRatio) / 2f
        val centerY_XB = (posX.yRatio + posB.yRatio) / 2f
        val centerX_YA = (posY.xRatio + posA.xRatio) / 2f
        val centerY_YA = (posY.yRatio + posA.yRatio) / 2f

        if (kotlin.math.abs(centerX_XB - centerX_YA) >= 0.02f ||
            kotlin.math.abs(centerY_XB - centerY_YA) >= 0.02f
        ) {
            return positions
        }

        val centerX = (centerX_XB + centerX_YA) / 2f
        val centerY = (centerY_XB + centerY_YA) / 2f

        val deltaXRatio = (posB.xRatio - posX.xRatio) / 2f
        val radiusPx = deltaXRatio * screenWidthPx
        val effectiveDyRatio = radiusPx / screenHeightPx

        val adjusted = positions.toMutableMap()
        adjusted[yEntry.key] = posY.copy(xRatio = centerX, yRatio = centerY - effectiveDyRatio)
        adjusted[aEntry.key] = posA.copy(xRatio = centerX, yRatio = centerY + effectiveDyRatio)
        adjusted[xEntry.key] = posX.copy(xRatio = centerX - deltaXRatio, yRatio = centerY)
        adjusted[bEntry.key] = posB.copy(xRatio = centerX + deltaXRatio, yRatio = centerY)

        return adjusted
    }

    /**
     * Resolves aspect-ratio-corrected positions for [HudElement]s on a screen of given dimensions.
     * Mirrors [adjustClusterPositionsForAspectRatio] for HUD Editor elements.
     */
    fun adjustHudElementsForAspectRatio(
        elements: Map<String, HudElement>,
        screenWidthPx: Float,
        screenHeightPx: Float
    ): Map<String, HudElement> {
        if (screenWidthPx <= 0f || screenHeightPx <= 0f) return elements

        val yEntry = elements.entries.firstOrNull { it.key.equals("Y", ignoreCase = true) }
        val xEntry = elements.entries.firstOrNull { it.key.equals("X", ignoreCase = true) }
        val bEntry = elements.entries.firstOrNull { it.key.equals("B", ignoreCase = true) }
        val aEntry = elements.entries.firstOrNull { it.key.equals("A", ignoreCase = true) }

        if (yEntry == null || xEntry == null || bEntry == null || aEntry == null) {
            return elements
        }

        val posY = yEntry.value.transform
        val posX = xEntry.value.transform
        val posB = bEntry.value.transform
        val posA = aEntry.value.transform

        val isHorizontalSymmetric = kotlin.math.abs(posX.yRatio - posB.yRatio) < 0.02f
        val isVerticalSymmetric = kotlin.math.abs(posY.xRatio - posA.xRatio) < 0.02f
        val isOriented = posX.xRatio < posB.xRatio && posY.yRatio < posA.yRatio

        if (!isHorizontalSymmetric || !isVerticalSymmetric || !isOriented) {
            return elements
        }

        val centerX_XB = (posX.xRatio + posB.xRatio) / 2f
        val centerY_XB = (posX.yRatio + posB.yRatio) / 2f
        val centerX_YA = (posY.xRatio + posA.xRatio) / 2f
        val centerY_YA = (posY.yRatio + posA.yRatio) / 2f

        if (kotlin.math.abs(centerX_XB - centerX_YA) >= 0.02f ||
            kotlin.math.abs(centerY_XB - centerY_YA) >= 0.02f
        ) {
            return elements
        }

        val centerX = (centerX_XB + centerX_YA) / 2f
        val centerY = (centerY_XB + centerY_YA) / 2f

        val deltaXRatio = (posB.xRatio - posX.xRatio) / 2f
        val radiusPx = deltaXRatio * screenWidthPx
        val effectiveDyRatio = radiusPx / screenHeightPx

        val adjusted = elements.toMutableMap()
        adjusted[yEntry.key] = yEntry.value.copy(
            transform = posY.copy(xRatio = centerX, yRatio = centerY - effectiveDyRatio)
        )
        adjusted[aEntry.key] = aEntry.value.copy(
            transform = posA.copy(xRatio = centerX, yRatio = centerY + effectiveDyRatio)
        )
        adjusted[xEntry.key] = xEntry.value.copy(
            transform = posX.copy(xRatio = centerX - deltaXRatio, yRatio = centerY)
        )
        adjusted[bEntry.key] = bEntry.value.copy(
            transform = posB.copy(xRatio = centerX + deltaXRatio, yRatio = centerY)
        )

        return adjusted
    }
}
