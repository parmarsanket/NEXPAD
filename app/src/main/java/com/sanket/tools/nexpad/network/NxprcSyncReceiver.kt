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

            // 7. Non-destructive write to filesDir/nxp_remote/
            // Decode incoming document to inspect metadata and allow variant preservation
            val decodedDoc = try {
                com.sanket.tools.nexpad.nxprc.NxprcDocument.decodeFromBytes(payload).getOrNull()
            } catch (_: Exception) {
                null
            }

            val safeId = componentId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetDir = File(context.filesDir, "nxp_remote").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "$safeId.nxprc")

            var finalId = componentId
            var finalName = decodedDoc?.manifest?.name ?: componentId
            var finalPayload = payload
            var finalFile = targetFile

            // If targetFile already exists and has DIFFERENT code/content, DO NOT delete it!
            // Preserve the existing button and save this incoming design as a new variant.
            if (targetFile.exists() && !targetFile.readBytes().contentEquals(payload)) {
                val baseId = decodedDoc?.manifest?.id?.ifBlank { null } ?: componentId
                val rawName = decodedDoc?.manifest?.name?.ifBlank { null } ?: componentId
                val baseName = rawName.replace(Regex("""\s*\(Variant\s*\d+\)"""), "").trim()

                var variantNum = 2
                var candidateId = "${baseId}_v$variantNum"
                var candidateSafeId = candidateId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
                var candidateFile = File(targetDir, "$candidateSafeId.nxprc")

                while (candidateFile.exists()) {
                    if (candidateFile.readBytes().contentEquals(payload)) {
                        // Already stored under this variant
                        break
                    }
                    variantNum++
                    candidateId = "${baseId}_v$variantNum"
                    candidateSafeId = candidateId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
                    candidateFile = File(targetDir, "$candidateSafeId.nxprc")
                }

                finalId = candidateId
                finalName = "$baseName (Variant $variantNum)"
                finalFile = candidateFile

                if (decodedDoc != null) {
                    val updatedDoc = decodedDoc.copy(
                        manifest = decodedDoc.manifest.copy(
                            id = finalId,
                            name = finalName
                        )
                    )
                    finalPayload = com.sanket.tools.nexpad.nxprc.NxprcDocument.encodeToBytes(updatedDoc)
                }
                Log.i(TAG, "🛡️ Preserved existing '$componentId' and saved incoming design as variant '$finalId' ('$finalName')")
            }

            val tempFile = File(targetDir, "${finalFile.nameWithoutExtension}.tmp")
            tempFile.writeBytes(finalPayload)
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                finalFile.writeBytes(finalPayload)
                tempFile.delete()
            }

            // 8. Hot-reload RemoteComponentRegistry & ComponentRegistry
            RemoteComponentRegistry.getInstance(context).reloadAll()
            com.sanket.tools.nexpad.runtime.registry.ComponentRegistry.getInstance(context).reloadAll()
            Log.i(TAG, "⚡ Successfully installed & hot-reloaded '$finalId' ('$finalName') (${finalPayload.size} bytes)")

            // 9. Instantaneous Toast notification on Main UI thread for user feedback
            val toastMessage = if (finalId != componentId) {
                "⚡ Preserved previous design! Saved as '$finalName' in Button Studio"
            } else {
                "⚡ Received & installed '$finalName' into Button Studio!"
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    android.widget.Toast.makeText(
                        context.applicationContext,
                        toastMessage,
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
            Result.success(SyncResult(componentId = finalId, bytesConsumedFromInitial = consumedFromInitial))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive NXPRC sync frame: ${e.message}", e)
            Result.failure(e)
        }
    }
}
