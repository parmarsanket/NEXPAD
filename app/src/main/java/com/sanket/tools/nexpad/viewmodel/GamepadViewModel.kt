package com.sanket.tools.nexpad.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.GamepadInput
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            getApplication<Application>().registerReceiver(usbReceiver, filter)
        }

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
                // Prune stale discovered servers (older than 4 seconds)
                val currentServers = _discoveredServers.value
                if (currentServers.isNotEmpty()) {
                    val fresh = currentServers.filter { now - it.lastSeenTimestamp <= 4000L }
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
                    startDiscovery()
                }
            }
        }

        checkAoaAccessory()
    }



    fun startDiscovery() {
        // Clear stale servers so UI drops back to "Scanning" state instantly
        _discoveredServers.value = emptyList()
        viewModelScope.launch {
            discoveryClient.startDiscovery(viewModelScope) { server ->
                val current = _discoveredServers.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.ipAddress == server.ipAddress }
                if (existingIndex >= 0) {
                    current[existingIndex] = server
                } else {
                    current.add(server)
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

    fun connect(ip: String, port: Int, serverName: String? = null) = networkManager.connect(ip, port, serverName)
    fun disconnect() {
        networkManager.disconnect()
        startDiscovery()
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
    
    fun updateButton(buttonName: String, isPressed: Boolean) {
        applyButtonState(buttonName, isPressed)
        // Zero-Delay dispatch: Fire a UDP packet immediately, bypassing the 16ms loop
        networkManager.sendImmediate()
    }

    private fun applyButtonState(buttonName: String, isPressed: Boolean) {
        when (buttonName) {
            "A" -> inputState.btnA = isPressed
            "B" -> inputState.btnB = isPressed
            "X" -> inputState.btnX = isPressed
            "Y" -> inputState.btnY = isPressed
            "UP" -> inputState.dpadUp = isPressed
            "DOWN" -> inputState.dpadDown = isPressed
            "LEFT" -> inputState.dpadLeft = isPressed
            "RIGHT" -> inputState.dpadRight = isPressed
            "LB" -> inputState.btnL1 = isPressed
            "RB" -> inputState.btnR1 = isPressed
            "LT" -> inputState.triggerL2 = if (isPressed) 1f else 0f
            "RT" -> inputState.triggerR2 = if (isPressed) 1f else 0f
            "L3" -> inputState.btnL3 = isPressed
            "R3" -> inputState.btnR3 = isPressed
            "MENU" -> inputState.btnStart = isPressed
            "VIEW" -> inputState.btnSelect = isPressed
            "XBOX" -> inputState.btnGuide = isPressed
            "SHARE" -> inputState.btnShare = isPressed
            "SCREENSHOT" -> inputState.btnScreenshot = isPressed
            "M1" -> inputState.btnM1 = isPressed
            "M2" -> inputState.btnM2 = isPressed
            "M3" -> inputState.btnM3 = isPressed
            "M4" -> inputState.btnM4 = isPressed
            "PROFILE" -> inputState.btnProfile = isPressed
            "TURBO" -> inputState.btnTurbo = isPressed
        }
    }

    fun updateLeftStick(x: Float, y: Float) {
        inputState.leftStickX = x
        inputState.leftStickY = y
    }

    fun updateRightStick(x: Float, y: Float) {
        inputState.rightStickX = x
        inputState.rightStickY = y
    }

    override fun onCleared() {
        super.onCleared()
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
