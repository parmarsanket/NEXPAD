package com.sanket.tools.nexpad.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*


@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return

    DisposableEffect(orientation) {
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation = orientation

        onDispose {
            previousOrientation?.let {
                activity.requestedOrientation = it
            } ?: run {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}