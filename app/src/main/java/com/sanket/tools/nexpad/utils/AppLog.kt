package com.sanket.tools.nexpad.utils

import android.util.Log
import com.sanket.tools.nexpad.BuildConfig

/**
 * Release-safe logging facade for NEXPAD.
 *
 * Ensures all diagnostic, verbose, debug, and informational logs are active during development
 * and testing, but automatically silenced in release builds to eliminate logcat telemetry leakage
 * and string allocation overhead.
 */
object AppLog {

    @JvmStatic
    fun d(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) Log.d(tag, msg) else 0
    }

    @JvmStatic
    fun d(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) Log.d(tag, msg, tr) else 0
    }

    @JvmStatic
    fun v(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) Log.v(tag, msg) else 0
    }

    @JvmStatic
    fun v(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) Log.v(tag, msg, tr) else 0
    }

    @JvmStatic
    fun i(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) Log.i(tag, msg) else 0
    }

    @JvmStatic
    fun i(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) Log.i(tag, msg, tr) else 0
    }

    @JvmStatic
    fun w(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) Log.w(tag, msg) else 0
    }

    @JvmStatic
    fun w(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) Log.w(tag, msg, tr) else 0
    }

    @JvmStatic
    fun e(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) Log.e(tag, msg) else 0
    }

    @JvmStatic
    fun e(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) Log.e(tag, msg, tr) else 0
    }
}
