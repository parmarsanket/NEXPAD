package com.sanket.tools.nexpad.runtime.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight TCP file transfer server listening on port 9996 for incoming .nxpcomponent files
 * streamed from NEXPAD Desktop via Wi-Fi or forwarded USB ADB.
 */
class FtpTransferReceiver(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var listenJob: Job? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(NexpadProtocol.FTP_PORT)
                while (isActive && isRunning.get()) {
                    val client = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // 1. Read FTP Start Packet
            val headerBuf = ByteArray(512)
            val headerBytes = input.read(headerBuf)
            if (headerBytes <= 0) {
                socket.close()
                return
            }

            val startPacket = NexpadProtocol.decodeFtpStart(headerBuf, 0)
            if (startPacket == null) {
                socket.close()
                return
            }

            // Send Start ACK
            output.write(NexpadProtocol.encodeFtpAck(0, 0))
            output.flush()

            val fileBuffer = ByteArrayOutputStream(startPacket.fileSize)
            val chunkBuf = ByteArray(4096)

            // 2. Read Chunks
            while (fileBuffer.size() < startPacket.fileSize) {
                val bytesRead = input.read(chunkBuf)
                if (bytesRead <= 0) break

                val chunkPacket = NexpadProtocol.decodeFtpChunk(chunkBuf, 0)
                if (chunkPacket != null) {
                    fileBuffer.write(chunkPacket.payload)
                    output.write(NexpadProtocol.encodeFtpAck(chunkPacket.chunkIndex, 0))
                    output.flush()
                }
            }

            // 3. Read FTP Complete Packet
            val completeBytes = input.read(chunkBuf)
            if (completeBytes > 0) {
                NexpadProtocol.decodeFtpComplete(chunkBuf, 0)
            }

            socket.close()

            // 4. Save to filesDir/nxp_components/
            val componentsDir = File(context.filesDir, "nxp_components").apply {
                if (!exists()) mkdirs()
            }
            val safeName = startPacket.fileName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetFile = File(componentsDir, safeName)
            targetFile.writeBytes(fileBuffer.toByteArray())

            // 5. Reload registry & notify user
            ComponentRegistry.getInstance(context).reloadAll()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "⚡ New Component Received: ${startPacket.fileName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    fun stop() {
        isRunning.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
        listenJob?.cancel()
    }
}
