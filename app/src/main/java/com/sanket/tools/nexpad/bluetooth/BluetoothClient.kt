package com.sanket.tools.nexpad.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.network.IGamepadConnection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class BluetoothClient(private val context: Context) : IGamepadConnection {
    override var onFeedbackReceived: ((GamepadFeedback) -> Unit)? = null
    override var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    override var onStatusChanged: ((String) -> Unit)? = null
    override var onDiagnosticLog: ((String) -> Unit)? = null

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null
    @Volatile private var pendingDevice: BluetoothDevice? = null
    @Volatile private var isRegistered = false
    @Volatile private var profileRequested = false
    @Volatile private var closed = false
    @Volatile private var pairingSessionActive = false
    @Volatile private var lastReport = ByteArray(HidDescriptors.GAMEPAD_REPORT_SIZE).apply {
        this[lastIndex] = 8
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            isRegistered = registered
            log("App registration changed: registered=$registered, plugged=${deviceLabel(pluggedDevice)}")
            if (!registered) {
                connectedDevice = null
                onConnectionStateChanged?.invoke(false)
                status("HID registration stopped")
                return
            }

            status("HID controller ready. Pair from the TV or computer.")
            val target = pendingDevice
            if (target != null) {
                connectRegisteredDevice(target)
            } else if (pluggedDevice != null) {
                log("Virtual cable exists for ${deviceLabel(pluggedDevice)}; waiting for host or user connect")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            log("Connection ${deviceLabel(device)}: ${stateName(state)}")
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> status("Connecting to ${deviceName(device)}...")
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    pendingDevice = null
                    status("Connected to ${deviceName(device)} as a HID gamepad")
                    onConnectionStateChanged?.invoke(true)
                    sendCurrentReport("initial neutral report")
                }
                BluetoothProfile.STATE_DISCONNECTING -> status("Disconnecting from ${deviceName(device)}...")
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice?.address == device.address) connectedDevice = null
                    onConnectionStateChanged?.invoke(false)
                    status("Disconnected from ${deviceName(device)}")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            log("GET_REPORT from ${deviceLabel(device)}: type=$type id=$id bufferSize=$bufferSize")
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && id.toInt() == HidDescriptors.GAMEPAD_REPORT_ID) {
                val reply = if (bufferSize > 0) {
                    lastReport.copyOf(minOf(bufferSize, lastReport.size))
                } else {
                    lastReport
                }
                val accepted = hidDevice?.replyReport(device, type, id, reply) == true
                log("GET_REPORT reply accepted=$accepted size=${reply.size}")
            } else {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                log("Rejected unsupported GET_REPORT type=$type id=$id", Log.WARN)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            log("SET_REPORT from ${deviceLabel(device)}: type=$type id=$id size=${data.size}")
            if (id.toInt() == 3 && data.size >= 3) {
                val strong = data[1].toUByte().toInt()
                val weak = data[2].toUByte().toInt()
                onFeedbackReceived?.invoke(GamepadFeedback(strong, weak))
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } else {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            log("Interrupt data from ${deviceLabel(device)}: id=$reportId size=${data.size}")
            if (reportId.toInt() == 3 && data.size >= 3) {
                val strong = data[1].toUByte().toInt()
                val weak = data[2].toUByte().toInt()
                onFeedbackReceived?.invoke(GamepadFeedback(strong, weak))
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            log("Protocol changed by ${deviceLabel(device)}: protocol=$protocol")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            log("Virtual cable unplugged by ${deviceLabel(device)}", Log.WARN)
            if (connectedDevice?.address == device.address) connectedDevice = null
            pendingDevice = null
            onConnectionStateChanged?.invoke(false)
            status("Host removed the NEXPAD pairing. Pair again from the host.")
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val previous = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                    log("Bond ${deviceLabel(device)}: ${bondStateName(previous)} -> ${bondStateName(state)}")
                    if (state == BluetoothDevice.BOND_BONDED && pairingSessionActive && device != null) {
                        pendingDevice = device
                        if (isRegistered) connectRegisteredDevice(device)
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> log("ACL connected: ${deviceLabel(device)}")
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED ->
                    log("ACL disconnect requested: ${deviceLabel(device)}", Log.WARN)
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    log("ACL disconnected: ${deviceLabel(device)}", Log.WARN)
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            log("onServiceConnected called: profile=$profile, closed=$closed")
            if (profile != BluetoothProfile.HID_DEVICE || closed) return
            hidDevice = proxy as BluetoothHidDevice
            log("HID_DEVICE profile proxy connected")
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            log("onServiceDisconnected called: profile=$profile")
            if (profile != BluetoothProfile.HID_DEVICE) return
            log("HID_DEVICE profile proxy disconnected", Log.WARN)
            hidDevice = null
            isRegistered = false
            connectedDevice = null
            onConnectionStateChanged?.invoke(false)
            status("Bluetooth HID service disconnected")
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(bluetoothReceiver, filter)
        }
    }

    override suspend fun connect(address: String, port: Int) {
        if (!checkReady()) return
        val adapter = bluetoothAdapter ?: return status("This device has no Bluetooth adapter")
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (error: IllegalArgumentException) {
            log("Invalid Bluetooth address: $address", Log.ERROR, error)
            return status("Invalid Bluetooth device address")
        }

        pendingDevice = device
        pairingSessionActive = false
        status("Preparing HID connection to ${deviceName(device)}...")
        ensureProfile()
        if (isRegistered) connectRegisteredDevice(device)
    }

    override suspend fun sendInput(input: GamepadInput) {
        lastReport = HidReportBuilder.build(input)
        sendCurrentReport(null)
    }

    override fun disconnect() {
        pendingDevice = null
        val device = connectedDevice
        if (device == null) {
            status("HID controller ready; no host is connected")
            return
        }
        val accepted = hidDevice?.disconnect(device) == true
        log("disconnect(${deviceLabel(device)}) accepted=$accepted")
        if (!accepted) status("Android rejected the disconnect request")
    }

    override fun startAdvertising() {
        if (!checkReady()) return
        pairingSessionActive = true
        ensureProfile()
        status("HID is starting. Pair NEXPAD from the host Bluetooth settings.")
    }

    override fun close() {
        if (closed) return
        closed = true
        pairingSessionActive = false
        pendingDevice = null
        connectedDevice?.let { hidDevice?.disconnect(it) }
        if (isRegistered) hidDevice?.unregisterApp()
        hidDevice?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        hidDevice = null
        connectedDevice = null
        isRegistered = false
        executor.shutdownNow()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already removed.
        }
        onConnectionStateChanged?.invoke(false)
        log("Bluetooth HID client closed")
    }

    private fun ensureProfile() {
        if (closed || profileRequested || hidDevice != null) return
        val adapter = bluetoothAdapter
        if (adapter == null) {
            status("This device has no Bluetooth adapter")
            return
        }
        profileRequested = true
        val requested = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        log("Requested HID_DEVICE profile proxy: accepted=$requested")
        if (!requested) {
            profileRequested = false
            status("Android rejected the HID profile request. This phone may not support HID Device mode.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun registerApp() {
        val hid = hidDevice ?: return
        if (!checkReady() || isRegistered) return

        val sdpName = "NEXPAD Gamepad"
        val descriptor = HidDescriptors.GENERIC_DESCRIPTOR

        val settings = BluetoothHidDeviceAppSdpSettings(
            sdpName,
            "Bluetooth HID game controller",
            "NEXPAD",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD, // HID SDP gamepad subclass. // Trick TV into thinking it's a Keyboard/Mouse combo
            descriptor
        )
        val accepted = hid.registerApp(settings, null, null, executor, callback)
        log("registerApp accepted=$accepted descriptorBytes=${descriptor.size}")
        status(if (accepted) "Registering as $sdpName..." else "Android rejected HID registration")
    }

    private fun connectRegisteredDevice(device: BluetoothDevice) {
        if (!isRegistered) return
        val accepted = hidDevice?.connect(device) == true
        log("connect(${deviceLabel(device)}) accepted=$accepted")
        if (!accepted) status("Android rejected the connection to ${deviceName(device)}")
    }

    private fun sendCurrentReport(reason: String?) {
        val device = connectedDevice ?: return
        val accepted = hidDevice?.sendReport(
            device,
            HidDescriptors.GAMEPAD_REPORT_ID,
            lastReport
        ) == true
        if (reason != null || !accepted) {
            log("sendReport(${reason ?: "input"}) accepted=$accepted size=${lastReport.size}", if (accepted) Log.DEBUG else Log.WARN)
        }
    }

    private fun checkReady(): Boolean {
        if (!BluetoothPermissionHelper.hasAllPermissions(context)) {
            status("Bluetooth permission is required")
            log("Bluetooth operation blocked: missing permission", Log.WARN)
            return false
        }
        if (bluetoothAdapter?.isEnabled != true) {
            status("Turn on Bluetooth first")
            log("Bluetooth operation blocked: adapter disabled", Log.WARN)
            return false
        }
        return true
    }

    private fun status(message: String) {
        Log.i(TAG, message)
        onStatusChanged?.invoke(message)
    }

    private fun log(message: String, priority: Int = Log.DEBUG, error: Throwable? = null) {
        val fullMessage = if (error == null) message else "$message: ${Log.getStackTraceString(error)}"
        Log.println(priority, TAG, fullMessage)
        onDiagnosticLog?.invoke("${System.currentTimeMillis() % 100000}: $message")
    }

    private fun deviceLabel(device: BluetoothDevice?): String =
        device?.let { "${deviceName(it)} (${it.address})" } ?: "none"

    private fun deviceName(device: BluetoothDevice): String = device.name ?: "Bluetooth host"

    private fun stateName(state: Int): String = when (state) {
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "UNKNOWN($state)"
    }

    private fun bondStateName(state: Int): String = when (state) {
        BluetoothDevice.BOND_NONE -> "NONE"
        BluetoothDevice.BOND_BONDING -> "BONDING"
        BluetoothDevice.BOND_BONDED -> "BONDED"
        else -> "UNKNOWN($state)"
    }

    private companion object {
        const val TAG = "NEXPAD_BT"
    }
}
