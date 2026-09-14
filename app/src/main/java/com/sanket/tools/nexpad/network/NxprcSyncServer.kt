package com.sanket.tools.nexpad.network

import android.content.Context
import android.util.Log
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException

/**
 * Lightweight background TCP server listening on SYNC_TCP_PORT (9995)
 * for out-of-band NXPRC plugin file pushes over Wi-Fi / USB Tethering / local LAN.
 *
 * Gamepad UDP streaming (port 9999) continues simultaneously with zero disruption or jitter.
 */
class NxprcSyncServer(private val context: Context) {

    companion object {
        private const val TAG = "NxprcSyncServer"
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    @Synchronized
    fun start(scope: CoroutineScope) {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(NexpadProtocol.SYNC_TCP_PORT).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "📡 NxprcSyncServer listening on TCP port ${NexpadProtocol.SYNC_TCP_PORT}")

                while (isActive) {
                    val socket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: SocketException) {
                        if (!isActive) break
                        Log.d(TAG, "ServerSocket closed: ${e.message}")
                        break
                    }

                    launch(Dispatchers.IO) {
                        try {
                            socket.soTimeout = 10000 // 10s timeout
                            val input = socket.getInputStream()
                            val output = socket.getOutputStream()

                            val result = NxprcSyncReceiver.receiveFromStream(context, input)
                            if (result.isSuccess) {
                                output.write(NexpadProtocol.FILE_SYNC_ACK.toInt())
                                output.flush()
                                Log.i(TAG, "✅ Synced '${result.getOrNull()?.componentId}' successfully over TCP sidecar")
                            } else {
                                output.write(NexpadProtocol.FILE_SYNC_NACK.toInt())
                                output.flush()
                                Log.w(TAG, "❌ Failed to sync component: ${result.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Sync connection error: ${e.message}")
                        } finally {
                            try { socket.close() } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: IOException) {
                if (isActive) {
                    Log.e(TAG, "Failed to bind NxprcSyncServer on port ${NexpadProtocol.SYNC_TCP_PORT}: ${e.message}")
                }
            } finally {
                stop()
            }
        }
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }
}
