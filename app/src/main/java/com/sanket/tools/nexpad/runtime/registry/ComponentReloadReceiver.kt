package com.sanket.tools.nexpad.runtime.registry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BroadcastReceiver responsible for reloading local and remote custom controller components
 * triggered by desktop ADB push workflows or local sync operations.
 */
class ComponentReloadReceiver(
    private val scope: CoroutineScope
) : BroadcastReceiver() {

    companion object {
        const val ACTION_RELOAD_COMPONENTS = "com.sanket.tools.nexpad.RELOAD_COMPONENTS"
        const val ACTION_RELOAD_REMOTE_COMPONENTS = "com.sanket.tools.nexpad.RELOAD_REMOTE_COMPONENTS"
        private const val TOAST_RELOAD_SUCCESS = "⚡ Components reloaded from Desktop!"

        /** Creates the IntentFilter containing all component reload action constants. */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(ACTION_RELOAD_COMPONENTS)
                addAction(ACTION_RELOAD_REMOTE_COMPONENTS)
            }
        }

        /** Registers this receiver using RECEIVER_NOT_EXPORTED for internal security. */
        fun register(context: Context, scope: CoroutineScope): ComponentReloadReceiver {
            val receiver = ComponentReloadReceiver(scope)
            ContextCompat.registerReceiver(
                context,
                receiver,
                createIntentFilter(),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            return receiver
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val targetContext = context ?: return
        scope.launch(Dispatchers.IO) {
            RemoteComponentRegistry.getInstance(targetContext).reloadAll()
            ComponentRegistry.getInstance(targetContext).reloadAll()
            withContext(Dispatchers.Main) {
                Toast.makeText(targetContext, TOAST_RELOAD_SUCCESS, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Unregisters this receiver safely. */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: Exception) {}
    }
}
