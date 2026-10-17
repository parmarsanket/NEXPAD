package com.sanket.tools.nexpad.utils

/**
 * Strongly-typed Android hardware intent actions, broadcast extras, and system constants.
 * Eliminates magic strings and hardcoded literals when interacting with USB, ADB, and system services.
 */
object NexpadHardwareActions {

    // ─── USB Hardware Broadcasts ─────────────────────────────────────────
    /**
     * Undocumented / internal Android broadcast action fired when USB state changes (connected, adb, etc.).
     */
    const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"

    /** Boolean extra indicating whether the USB physical cable is connected to a host/charger. */
    const val EXTRA_USB_CONNECTED = "connected"

    /** Boolean extra indicating whether ADB (Android Debug Bridge) is active on the USB link. */
    const val EXTRA_USB_ADB = "adb"

    // ─── NEXPAD Desktop ADB Handshake Broadcasts ─────────────────────────
    /** Broadcast fired by desktop runner when ADB reverse tunnel is ready for input streaming. */
    const val ACTION_ADB_DEVICE_READY = "com.sanket.tools.nexpad.ADB_DEVICE_READY"

    /** Broadcast fired when ADB connection is closed or host is disconnected. */
    const val ACTION_ADB_DEVICE_DISCONNECTED = "com.sanket.tools.nexpad.ADB_DEVICE_DISCONNECTED"

    /** String extra containing the hostname or friendly name of the connected PC. */
    const val EXTRA_PC_NAME = "pc_name"

    /** Default fallback host display name when PC name is empty or null. */
    const val DEFAULT_PC_NAME = "Windows PC"
}
