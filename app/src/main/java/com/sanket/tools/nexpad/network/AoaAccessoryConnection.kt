package com.sanket.tools.nexpad.network

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.IntentCompat
import com.sanket.tools.nexpad.model.GamepadInput
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Ultra-low latency, zero-allocation AOA (Android Open Accessory) Transport.
 * Inherits stream workers, framing, coalescing, and RTT/jitter computation from BaseStreamConnection.
 */
class AoaAccessoryConnection(private val context: Context) : BaseStreamConnection() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.sanket.tools.nexpad.USB_PERMISSION"
        private const val TAG = "NEXPAD_AOA"
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var isReceiverRegistered = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val accessory: UsbAccessory? = IntentCompat.getParcelableExtra(
                        intent, UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java
                    )
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        accessory?.let { openAccessory(it) }
                    } else {
                        onStatusChanged?.invoke("USB permission denied")
                        Log.e(TAG, "Permission denied for accessory $accessory")
                    }
                }
            }
        }
    }

    override suspend fun connect(address: String, port: Int) {
        val accessoryList = usbManager.accessoryList
        if (accessoryList.isNullOrEmpty()) {
            onStatusChanged?.invoke("No USB accessory found")
            return
        }

        val accessory = accessoryList[0]
        if (usbManager.hasPermission(accessory)) {
            openAccessory(accessory)
        } else {
            onStatusChanged?.invoke("Requesting USB permission...")
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            if (!isReceiverRegistered) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(permissionReceiver, filter)
                }
                isReceiverRegistered = true
            }
            val permissionIntent = Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, 0, permissionIntent, flags)
            usbManager.requestPermission(accessory, pendingIntent)
        }
    }

    private fun openAccessory(accessory: UsbAccessory) {
        try {
            val pfd = usbManager.openAccessory(accessory)
            if (pfd != null) {
                fileDescriptor = pfd
                val fd = pfd.fileDescriptor
                val inputStream = FileInputStream(fd)
                val outputStream = FileOutputStream(fd)

                startStreamWorkers(inputStream, outputStream, "AOA")

                // Immediate initial handshake packet to notify Desktop transport of active session
                callbackScope?.launch {
                    sendInput(GamepadInput())
                }

                val pcName = accessory.description?.takeIf { it.isNotBlank() && it != "NEXPAD USB Gamepad" } ?: "Windows PC"
                onServerNameResolved?.invoke(pcName)
                onStatusChanged?.invoke("Connected to $pcName via USB")
                Log.d(TAG, "AOA Accessory opened with dedicated real-time threads (Host: $pcName)")
            } else {
                onStatusChanged?.invoke("Failed to open accessory")
                Log.e(TAG, "Accessory open failed")
            }
        } catch (e: Exception) {
            onStatusChanged?.invoke("Error: ${e.message}")
            Log.e(TAG, "Exception opening accessory", e)
        }
    }

    override fun disconnect() {
        if (!isConnected && fileDescriptor == null) return
        stopStreamWorkers()

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        fileDescriptor = null

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(permissionReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }

        Log.d(TAG, "AOA Accessory Disconnected cleanly")
    }
}
