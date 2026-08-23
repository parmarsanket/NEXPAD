package com.sanket.tools.nexpad.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.GamepadInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sanket.tools.nexpad.network.DiscoveryClient
import com.sanket.tools.nexpad.network.DiscoveredServer
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

    // Expose flows for the UI
    val isConnected = networkManager.isConnected
    val connectionStatus = networkManager.connectionStatus
    val diagnosticLog = networkManager.diagnosticLog
    val feedbackFlow = networkManager.feedbackFlow
    val connectionStats = networkManager.connectionStats



    fun startDiscovery() {
        // Clear stale servers so UI drops back to "Scanning" state instantly
        _discoveredServers.value = emptyList()
        viewModelScope.launch {
            discoveryClient.startDiscovery(viewModelScope) { server ->
                val current = _discoveredServers.value.toMutableList()
                if (current.none { it.ipAddress == server.ipAddress }) {
                    current.add(server)
                    _discoveredServers.value = current
                }
            }
        }
    }

    fun stopDiscovery() {
        discoveryClient.stopDiscovery()
    }

    fun connect(ip: String, port: Int) = networkManager.connect(ip, port)
    fun disconnect() = networkManager.disconnect()

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
        discoveryClient.stopDiscovery()
        networkManager.close()
    }
}
