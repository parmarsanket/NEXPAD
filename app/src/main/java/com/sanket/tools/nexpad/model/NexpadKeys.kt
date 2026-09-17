package com.sanket.tools.nexpad.model

import com.sanket.tools.nexpad.category.CategoryManager

/**
 * Canonical control key constants for the entire NEXPAD app layer.
 *
 * Every key is sourced directly from [CategoryManager] — the single source of truth.
 * Use these constants instead of raw string literals anywhere a control key is needed
 * (layout positions, HUD elements, skin lookups, etc.).
 *
 * If a key doesn't exist in CategoryManager at runtime, an [IllegalStateException]
 * is thrown immediately so the error is caught early — not silently swallowed.
 */
object NexpadKeys {

    /** Safely retrieves a canonical key from CategoryManager. Throws if not found. */
    private fun key(k: String): String =
        CategoryManager.getControl(k)?.key
            ?: error("NexpadKeys: '$k' not found in CategoryManager — ensure CategoryManager is up to date.")

    // ── ABXY ──────────────────────────────────────────────────────────────────
    val A: String get() = key("A")
    val B: String get() = key("B")
    val X: String get() = key("X")
    val Y: String get() = key("Y")

    // ── D-Pad (individual + composite) ────────────────────────────────────────
    val UP: String    get() = key("UP")
    val DOWN: String  get() = key("DOWN")
    val LEFT: String  get() = key("LEFT")
    val RIGHT: String get() = key("RIGHT")
    /** Composite 4-way D-Pad cluster (single renderable unit). */
    val DPAD: String  get() = CategoryManager.getControl("DPAD")?.key ?: "DPAD"

    // ── Triggers ──────────────────────────────────────────────────────────────
    val LT: String get() = key("LT")
    val RT: String get() = key("RT")

    // ── Bumpers ───────────────────────────────────────────────────────────────
    val LB: String get() = key("LB")
    val RB: String get() = key("RB")

    // ── Sticks ────────────────────────────────────────────────────────────────
    val LS: String get() = key("LS")
    val RS: String get() = key("RS")

    // ── System ────────────────────────────────────────────────────────────────
    /** Xbox / Home / Guide center button. */
    val GUIDE: String   get() = key("GUIDE")
    /** Menu / Start / Options button. */
    val START: String   get() = key("START")
    /** View / Back / Select / Map button. */
    val BACK: String    get() = key("BACK")
    val SHARE: String   get() = key("SHARE")
    val TURBO: String   get() = key("TURBO")
    val PROFILE: String get() = key("PROFILE")

    // ── Macros ────────────────────────────────────────────────────────────────
    val M1: String get() = key("M1")
    val M2: String get() = key("M2")
    val M3: String get() = key("M3")
    val M4: String get() = key("M4")
}
