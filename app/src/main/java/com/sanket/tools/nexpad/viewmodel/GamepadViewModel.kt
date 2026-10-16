package com.sanket.tools.nexpad.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.GamepadInput
import com.sanket.tools.nexpad.category.ControlKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sanket.tools.nexpad.network.DiscoveryClient
import com.sanket.tools.nexpad.network.DiscoveredServer
import com.sanket.tools.nexpad.network.NetworkInterfaceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GamepadViewModel(application: Application) : AndroidViewModel(application) {
    // Single shared instance for zero-allocation state updates
    val inputState = GamepadInput()
    
    private val discoveryClient = DiscoveryClient()
    
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val networkManager = GamepadNetworkManager(
        scope = viewModelScope,
        context = application.applicationContext,
        inputState = inputState
    )

    // Expose flows for the UI
    val isConnected = networkManager.isConnected
    val connectionStatus = networkManager.connectionStatus
    val diagnosticLog = networkManager.diagnosticLog
    val feedbackFlow = networkManager.feedbackFlow
    val connectionStats = networkManager.connectionStats
    val connectedServerName = networkManager.connectedServerName

    private val sensorController = SensorController(
        inputState = inputState
    )
    val requestedSensorMode: Byte get() = sensorController.requestedSensorMode
    val requestedSensorModeFlow: StateFlow<Byte> get() = sensorController.requestedSensorModeFlow

    private val sharedPreferences = application.getSharedPreferences("nexpad_prefs", android.content.Context.MODE_PRIVATE)
    private val _emulationMode = MutableStateFlow(sharedPreferences.getString("emulation_mode", "generic") ?: "generic")
    val emulationMode: StateFlow<String> = _emulationMode.asStateFlow()

    fun setEmulationMode(mode: String) {
        sharedPreferences.edit().putString("emulation_mode", mode).apply()
        _emulationMode.value = mode
    }

    private val _isAoaAttached = MutableStateFlow(false)
    val isAoaAttached: StateFlow<Boolean> = _isAoaAttached.asStateFlow()

    private val _isUsbCableConnected = MutableStateFlow(false)
    val isUsbCableConnected: StateFlow<Boolean> = _isUsbCableConnected.asStateFlow()

    private val _isUsbAdbActive = MutableStateFlow(false)
    val isUsbAdbActive: StateFlow<Boolean> = _isUsbAdbActive.asStateFlow()

    private val _isAdbAvailable = MutableStateFlow(false)
    val isAdbAvailable: StateFlow<Boolean> = _isAdbAvailable.asStateFlow()

    private val _adbServerName = MutableStateFlow<String?>(null)
    val adbServerName: StateFlow<String?> = _adbServerName.asStateFlow()

    private var lastAdbSeenTimestamp = 0L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val usbReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "android.hardware.usb.action.USB_STATE" -> {
                    val connected = intent.getBooleanExtra("connected", false)
                    val adb = intent.getBooleanExtra("adb", false)
                    _isUsbCableConnected.value = connected
                    _isUsbAdbActive.value = connected && adb
                    if (!connected) {
                        _isAdbAvailable.value = false
                        _adbServerName.value = null
                        _isAoaAttached.value = false
                        if (connectionStats.value.transport == ConnectionType.USB) {
                            disconnect()
                        }
                    }
                }
                android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    android.util.Log.d("NEXPAD", "AOA accessory attached intent received.")
                    checkAoaAccessory()
                }
                android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    android.util.Log.d("NEXPAD", "AOA accessory detached intent received.")
                    _isAoaAttached.value = false
                    if (connectionStats.value.transport == ConnectionType.USB) {
                        disconnect()
                    }
                }
                "com.sanket.tools.nexpad.ADB_DEVICE_READY" -> {
                    val pcName = intent.getStringExtra("pc_name")?.trim()
                    if (_isUsbCableConnected.value && isUsbDebuggingEnabled()) {
                        _adbServerName.value = if (!pcName.isNullOrEmpty()) pcName else "Windows PC"
                        _isAdbAvailable.value = true
                        lastAdbSeenTimestamp = System.currentTimeMillis()
                        android.util.Log.d("NEXPAD", "🤝 Received ADB_DEVICE_READY from: ${_adbServerName.value}")
                    }
                }
                "com.sanket.tools.nexpad.ADB_DEVICE_DISCONNECTED" -> {
                    _isAdbAvailable.value = false
                    _adbServerName.value = null
                }
            }
        }
    }

    fun checkAoaAccessory(): Boolean {
        return try {
            val usbManager = getApplication<Application>().getSystemService(android.content.Context.USB_SERVICE) as? android.hardware.usb.UsbManager
            val attached = !usbManager?.accessoryList.isNullOrEmpty()
            _isAoaAttached.value = attached
            attached
        } catch (_: Exception) {
            false
        }
    }

    init {
        // Initial sticky USB state query
        val stickyUsb = application.registerReceiver(null, IntentFilter("android.hardware.usb.action.USB_STATE"))
        if (stickyUsb != null) {
            val connected = stickyUsb.getBooleanExtra("connected", false)
            val adb = stickyUsb.getBooleanExtra("adb", false)
            _isUsbCableConnected.value = connected
            _isUsbAdbActive.value = connected && adb
        }

        // Register receiver for USB state, accessory, and ADB readiness
        val filter = IntentFilter().apply {
            addAction("android.hardware.usb.action.USB_STATE")
            addAction(android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction("com.sanket.tools.nexpad.ADB_DEVICE_READY")
            addAction("com.sanket.tools.nexpad.ADB_DEVICE_DISCONNECTED")
        }
        ContextCompat.registerReceiver(
            getApplication<Application>(),
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Monitor network connectivity in real-time to purge disconnected interfaces immediately
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val netCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                val app = getApplication<Application>()
                val hasWifi = NetworkInterfaceHelper.hasActiveWifiOrEthernet(app)
                val hasTethering = NetworkInterfaceHelper.hasActiveUsbTethering()

                viewModelScope.launch(Dispatchers.Main) {
                    val current = _discoveredServers.value
                    val filtered = current.filter { server ->
                        if (server.isUsbTethering) hasTethering else hasWifi
                    }
                    if (filtered.size != current.size) {
                        _discoveredServers.value = filtered
                    }
                }
            }

            override fun onAvailable(network: Network) {
                viewModelScope.launch(Dispatchers.Main) {
                    if (!isConnected.value) {
                        startDiscovery()
                    }
                }
            }
        }
        networkCallback = netCallback
        try {
            cm?.registerDefaultNetworkCallback(netCallback)
        } catch (_: Exception) {}

        // Periodic pruning of stale ADB readiness and dead network servers
        viewModelScope.launch {
            while (isActive) {
                delay(1500)
                val now = System.currentTimeMillis()
                // Prune ADB readiness if no heartbeat for 4.5s
                if (_isAdbAvailable.value && (now - lastAdbSeenTimestamp > 4500L)) {
                    _isAdbAvailable.value = false
                    _adbServerName.value = null
                }
                // Prune stale discovered servers (older than 10 seconds to prevent Wi-Fi jitter/flicker)
                val currentServers = _discoveredServers.value
                if (currentServers.isNotEmpty()) {
                    val fresh = currentServers.filter { now - it.lastSeenTimestamp <= 10000L }
                    if (fresh.size != currentServers.size) {
                        _discoveredServers.value = fresh
                    }
                }
            }
        }
        
        // Single Active Transport Guard: stop discovery when connected, auto-resume when disconnected
        viewModelScope.launch {
            isConnected.collect { connected ->
                if (connected) {
                    stopDiscovery()
                    _discoveredServers.value = emptyList()
                } else {
                    // Note: leftStickX/Y, rightStickX/Y Compose states are reset inside
                    // updateLeftStick(0f,0f) / updateRightStick(0f,0f) called by GamepadNetworkManager
                    // on disconnect, so we must NOT access them here (init-order NPE risk).
                    startDiscovery()
                }
            }
        }

        // Sync requestedSensorMode from Desktop feedback packets
        viewModelScope.launch {
            networkManager.feedbackFlow.collect { feedback ->
                sensorController.setRequestedSensorMode(feedback.requestedSensorMode)
            }
        }

        checkAoaAccessory()
    }



    fun startDiscovery() {
        val now = System.currentTimeMillis()
        // Prune only genuinely dead servers (> 10s) instead of wiping UI to empty on every scan
        _discoveredServers.value = _discoveredServers.value.filter { now - it.lastSeenTimestamp <= 10000L }
        viewModelScope.launch {
            discoveryClient.startDiscovery(viewModelScope) { server ->
                val current = _discoveredServers.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.ipAddress == server.ipAddress }
                val updatedServer = server.copy(lastSeenTimestamp = System.currentTimeMillis())
                if (existingIndex >= 0) {
                    current[existingIndex] = updatedServer
                } else {
                    current.add(updatedServer)
                }
                // Prioritize USB Tethering servers over Wi-Fi
                current.sortByDescending { it.isUsbTethering }
                _discoveredServers.value = current
            }
        }
    }

    fun stopDiscovery() {
        discoveryClient.stopDiscovery()
    }

    private var screenOffDisconnectJob: kotlinx.coroutines.Job? = null

    /**
     * Starts a 10-minute idle disconnect countdown when the screen is OFF or app is backgrounded.
     * If user does not return within 10 minutes, cleanly disconnects to save battery & CPU.
     */
    fun startScreenOffDisconnectTimer() {
        if (!isConnected.value) return
        if (screenOffDisconnectJob?.isActive == true) return
        screenOffDisconnectJob = viewModelScope.launch(Dispatchers.Default) {
            networkManager.refreshWakeLock(10 * 60 * 1000L)
            android.util.Log.d("NEXPAD", "🌙 Screen OFF / Background: 10-minute idle disconnect countdown started.")
            delay(10 * 60 * 1000L)
            if (isConnected.value) {
                android.util.Log.i("NEXPAD", "⏰ Screen OFF for 10 minutes! Disconnecting session to conserve CPU & battery.")
                disconnect()
            }
        }
    }

    /**
     * Cancels the screen-off idle timer when the user returns to the app and turns the screen ON.
     */
    fun cancelScreenOffDisconnectTimer() {
        if (screenOffDisconnectJob != null) {
            screenOffDisconnectJob?.cancel()
            screenOffDisconnectJob = null
            android.util.Log.d("NEXPAD", "☀️ Screen ON / Foreground: Screen-off idle timer cancelled. Session active.")
        }
    }

    fun connect(ip: String, port: Int, serverName: String? = null) = networkManager.connect(ip, port, serverName)
    fun disconnect() {
        cancelScreenOffDisconnectTimer()
        networkManager.disconnect()
    }
    fun switchToAoaConnection() = networkManager.switchToAoaConnection()
    fun switchToAdbConnection(serverName: String? = null) = networkManager.switchToAdbConnection(serverName ?: _adbServerName.value)
    fun getPairedBluetoothDevices() = networkManager.getPairedBluetoothDevices()
    fun switchToBluetoothConnection(macAddress: String, deviceName: String? = null) = networkManager.switchToBluetoothConnection(macAddress, deviceName)

    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            android.provider.Settings.Global.getInt(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun updateAccel(x: Float, y: Float, z: Float) = sensorController.updateAccel(x, y, z)
    fun updateGravity(x: Float, y: Float, z: Float) = sensorController.updateGravity(x, y, z)
    fun update6AxisGyro(x: Float, y: Float, z: Float) = sensorController.update6AxisGyro(x, y, z)
    fun updateGameRotation(qx: Float, qy: Float, qz: Float, qw: Float) = sensorController.updateGameRotation(qx, qy, qz, qw)
    fun updateLinearGesture(lx: Float, ly: Float, lz: Float, gx: Float, gy: Float, gz: Float, rx: Float = 0f, ry: Float = 0f, rz: Float = 0f) = sensorController.updateLinearGesture(lx, ly, lz, gx, gy, gz, rx, ry, rz)
    fun updateRawUncalibrated(rx: Float, ry: Float, rz: Float, bx: Float, by: Float, bz: Float) = sensorController.updateRawUncalibrated(rx, ry, rz, bx, by, bz)

    private val _isGyroEnabled = MutableStateFlow(true)
    val isGyroEnabled: StateFlow<Boolean> = _isGyroEnabled.asStateFlow()

    private val _isGamepadScreenActive = MutableStateFlow(false)
    val isGamepadScreenActive: StateFlow<Boolean> = _isGamepadScreenActive.asStateFlow()

    fun onGamepadScreenActive(active: Boolean) {
        _isGamepadScreenActive.value = active
        if (!active) {
            sensorController.resetAllSensors()
        }
    }

    fun toggleGyro(): Boolean {
        val newState = !_isGyroEnabled.value
        _isGyroEnabled.value = newState
        if (!newState) {
            sensorController.isMotionMuted = true
            sensorController.resetAllSensors()
            networkManager.sendImmediate()
        } else {
            sensorController.isMotionMuted = false
        }
        return newState
    }

    fun setGyroEnabled(enabled: Boolean) {
        _isGyroEnabled.value = enabled
        if (!enabled) {
            sensorController.isMotionMuted = true
            sensorController.resetAllSensors()
            networkManager.sendImmediate()
        } else {
            sensorController.isMotionMuted = false
        }
    }
    
    fun updateButton(buttonName: String, isPressed: Boolean) {
        applyButtonState(buttonName, isPressed)
        // Zero-Delay dispatch: Fire a UDP packet immediately, bypassing the 16ms loop
        networkManager.sendImmediate()
    }

    /**
     * Updates an analog trigger with a continuous normalized float value [0.0f, 1.0f],
     * which encodes into NexpadProtocol as [0, 255].
     * Dispatches immediately via UDP for physical trigger clips and continuous sliders.
     */
    fun updateTrigger(key: String, value: Float) {
        val ctrl = resolveControlKey(key)
        val clamped = value.coerceIn(0f, 1f)
        when (ctrl) {
            ControlKey.LT -> inputState.triggerL2 = clamped
            ControlKey.RT -> inputState.triggerR2 = clamped
            else -> {}
        }
        networkManager.sendImmediate()
    }

    private fun applyButtonState(buttonName: String, isPressed: Boolean) {
        if (buttonName.equals("GYRO", ignoreCase = true)) {
            if (isPressed) {
                toggleGyro()
            }
            return
        }

        // Canonical keys come from NexpadKeys (backed by CategoryManager).
        // Hardware alias keys (L1/L2/L3, R1/R2/R3, BACK, SELECT, HOME, XBOX, MENU, VIEW)
        // Dispatches through ControlKey single source of truth; all aliases, synonyms
        // (L1, R1, L2, R2, L3, R3, MENU, VIEW, XBOX, CAPTURE, etc.) resolve automatically.
        val ctrl = resolveControlKey(buttonName)
        when (ctrl) {
            ControlKey.A       -> inputState.btnA = isPressed
            ControlKey.B       -> inputState.btnB = isPressed
            ControlKey.X       -> inputState.btnX = isPressed
            ControlKey.Y       -> inputState.btnY = isPressed
            ControlKey.UP      -> inputState.dpadUp = isPressed
            ControlKey.DOWN    -> inputState.dpadDown = isPressed
            ControlKey.LEFT    -> inputState.dpadLeft = isPressed
            ControlKey.RIGHT   -> inputState.dpadRight = isPressed
            ControlKey.LB      -> inputState.btnL1 = isPressed
            ControlKey.RB      -> inputState.btnR1 = isPressed
            ControlKey.LT      -> inputState.triggerL2 = if (isPressed) 1f else 0f
            ControlKey.RT      -> inputState.triggerR2 = if (isPressed) 1f else 0f
            ControlKey.LS, ControlKey.LSB -> inputState.btnL3 = isPressed
            ControlKey.RS, ControlKey.RSB -> inputState.btnR3 = isPressed
            ControlKey.START   -> inputState.btnStart = isPressed
            ControlKey.BACK    -> inputState.btnSelect = isPressed
            ControlKey.GUIDE   -> inputState.btnGuide = isPressed
            ControlKey.SHARE   -> {
                inputState.btnShare = isPressed
                inputState.btnScreenshot = isPressed
            }
            ControlKey.M1      -> inputState.btnM1 = isPressed
            ControlKey.M2      -> inputState.btnM2 = isPressed
            ControlKey.M3      -> inputState.btnM3 = isPressed
            ControlKey.M4      -> inputState.btnM4 = isPressed
            null, ControlKey.DPAD, ControlKey.LTP, ControlKey.RTP -> {}
        }
    }

    var leftStickX by mutableFloatStateOf(0f)
        private set
    var leftStickY by mutableFloatStateOf(0f)
        private set
    var rightStickX by mutableFloatStateOf(0f)
        private set
    var rightStickY by mutableFloatStateOf(0f)
        private set

    val leftStickState: Pair<Float, Float>
        get() = Pair(leftStickX, leftStickY)
    val rightStickState: Pair<Float, Float>
        get() = Pair(rightStickX, rightStickY)

    fun updateLeftStick(x: Float, y: Float) {
        val dx = kotlin.math.abs(x - inputState.leftStickX)
        val dy = kotlin.math.abs(y - inputState.leftStickY)
        val changed = dx > 0.002f || dy > 0.002f || (x == 0f && inputState.leftStickX != 0f) || (y == 0f && inputState.leftStickY != 0f)

        inputState.leftStickX = x
        inputState.leftStickY = y
        leftStickX = x
        leftStickY = y

        if (changed) {
            networkManager.sendImmediate()
        }
    }

    fun updateRightStick(x: Float, y: Float) {
        val dx = kotlin.math.abs(x - inputState.rightStickX)
        val dy = kotlin.math.abs(y - inputState.rightStickY)
        val changed = dx > 0.002f || dy > 0.002f || (x == 0f && inputState.rightStickX != 0f) || (y == 0f && inputState.rightStickY != 0f)

        inputState.rightStickX = x
        inputState.rightStickY = y
        rightStickX = x
        rightStickY = y

        if (changed) {
            networkManager.sendImmediate()
        }
    }

    companion object {
        private val controlKeyCache = ConcurrentHashMap<String, ControlKey?>()
        fun resolveControlKey(identifier: String): ControlKey? {
            return controlKeyCache.computeIfAbsent(identifier) { ControlKey.fromIdentifier(it) }
        }
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { cm?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        discoveryClient.stopDiscovery()
        networkManager.close()
    }
}
