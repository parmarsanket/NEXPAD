package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.model.GamepadFeedback
import com.sanket.tools.nexpad.model.GamepadInput

/**
 * Scalable interface for any type of gamepad connection (UDP, Bluetooth, etc.)
 * This enables Phase 2 (Bluetooth integration) without changing the ViewModel.
 */
interface IGamepadConnection {
    var onFeedbackReceived: ((GamepadFeedback) -> Unit)?
    var onConnectionStateChanged: ((Boolean) -> Unit)?
    var onStatusChanged: ((String) -> Unit)?
    var onDiagnosticLog: ((String) -> Unit)?
    var onNetworkPerformanceUpdated: ((latencyMs: Long, jitterMs: Long, packetLoss: Float) -> Unit)?
    
    suspend fun connect(address: String, port: Int)
    suspend fun sendInput(input: GamepadInput)
    fun disconnect()
    fun close()
}
