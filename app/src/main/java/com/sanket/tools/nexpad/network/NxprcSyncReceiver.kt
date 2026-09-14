package com.sanket.tools.nexpad.network

import android.content.Context
import android.util.Log
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream

data class SyncResult(
    val componentId: String,
    val bytesConsumedFromInitial: Int
)

object NxprcSyncReceiver {
    private const val TAG = "NxprcSyncReceiver"

    /**
     * Reads a full sync frame from an InputStream, verifies the CRC32 checksum,
     * writes the component to context.filesDir/nxp_remote/<safeFileName>.nxprc,
     * and reloads the RemoteComponentRegistry.
     *
     * @param context Android context
     * @param input Stream containing the header + payload
     * @param initialBuffer Optional pre-read chunk that starts with 0xAF
     * @param initialOffset Offset of 0xAF in initialBuffer
     * @param initialLength Available bytes in initialBuffer from initialOffset
     * @return Result<SyncResult> with the component ID and bytes consumed from initialBuffer on success
     */
    fun receiveFromStream(
        context: Context,
        input: InputStream,
        initialBuffer: ByteArray? = null,
        initialOffset: Int = 0,
        initialLength: Int = 0
    ): Result<SyncResult> {
        val preStream = if (initialBuffer != null && initialLength > 0) {
            ByteArrayInputStream(initialBuffer, initialOffset, initialLength)
        } else null

        val combinedStream = if (preStream != null) {
            SequenceInputStream(preStream, input)
        } else {
            input
        }

        val dataIn = DataInputStream(combinedStream)

        return try {
            // 1. Read and validate Magic Byte (0xAF)
            val magic = dataIn.readByte()
            if (magic != NexpadProtocol.PACKET_TYPE_FILE_SYNC_START) {
                return Result.failure(IllegalArgumentException("Invalid sync packet magic: 0x${(magic.toInt() and 0xFF).toString(16)}"))
            }

            // 2. Read File Size (big-endian Int)
            val fileSize = dataIn.readInt()
            if (fileSize <= 0 || fileSize > 10 * 1024 * 1024) {
                return Result.failure(IllegalArgumentException("Invalid file size in sync header: $fileSize bytes"))
            }

            // 3. Read Component ID
            val idLen = dataIn.readUnsignedByte()
            val idBytes = ByteArray(idLen)
            dataIn.readFully(idBytes)
            val componentId = idBytes.decodeToString()

            // 4. Read Checksum (big-endian Int)
            val checksum = dataIn.readInt()

            // 5. Read Payload
            val payload = ByteArray(fileSize)
            dataIn.readFully(payload)

            // 6. Verify CRC32
            val computedCrc = NexpadProtocol.computeCrc32(payload)
            if (computedCrc != checksum) {
                return Result.failure(IllegalStateException("CRC32 mismatch! Expected $checksum, got $computedCrc"))
            }

            // 7. Atomically write to filesDir/nxp_remote/
            val safeId = componentId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetDir = File(context.filesDir, "nxp_remote").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "$safeId.nxprc")
            val tempFile = File(targetDir, "$safeId.tmp")
            tempFile.writeBytes(payload)
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                targetFile.writeBytes(payload)
                tempFile.delete()
            }

            // 8. Hot-reload RemoteComponentRegistry & ComponentRegistry
            RemoteComponentRegistry.getInstance(context).reloadAll()
            com.sanket.tools.nexpad.runtime.registry.ComponentRegistry.getInstance(context).reloadAll()
            Log.i(TAG, "⚡ Successfully installed & hot-reloaded '$componentId' ($fileSize bytes)")

            // 9. Instantaneous Toast notification on Main UI thread for user feedback
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    android.widget.Toast.makeText(
                        context.applicationContext,
                        "⚡ Received & installed '$componentId' into Button Studio!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to show toast: ${e.message}")
                }
            }

            // 10. Broadcast component reload to any active Activities/Screens
            try {
                context.sendBroadcast(android.content.Intent("com.sanket.tools.nexpad.RELOAD_COMPONENTS"))
            } catch (_: Exception) {}

            val consumedFromInitial = if (preStream != null) initialLength - preStream.available() else 0
            Result.success(SyncResult(componentId = componentId, bytesConsumedFromInitial = consumedFromInitial))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive NXPRC sync frame: ${e.message}", e)
            Result.failure(e)
        }
    }
}
