package com.sanket.tools.nexpad.utils

import android.content.pm.ActivityInfo
import android.view.ViewTreeObserver
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return

    DisposableEffect(orientation) {
        val previousOrientation = activity.requestedOrientation

        activity.requestedOrientation = orientation

        onDispose {
            activity.requestedOrientation = previousOrientation
        }
    }
}

/**
 * Automatically hides system bars (status bar & navigation bar) in sticky immersive mode
 * while an immersive screen is active (e.g. GamepadScreen, HudEditorScreen), and restores them
 * with transparent edge-to-edge bars when leaving the screen.
 */
@Composable
fun ImmersiveSystemBars() {
    val activity = LocalActivity.current ?: return
    val window = activity.window ?: return
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, lifecycleOwner, view) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}