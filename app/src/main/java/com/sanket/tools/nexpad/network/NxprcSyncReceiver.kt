package com.sanket.tools.nexpad.network

import android.content.Context
import android.util.Log
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import java.io.EOFException
import java.io.File
import java.io.InputStream

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
     * @return Result<String> with the component ID on success, or failure
     */
    fun receiveFromStream(
        context: Context,
        input: InputStream,
        initialBuffer: ByteArray? = null,
        initialOffset: Int = 0,
        initialLength: Int = 0
    ): Result<String> {
        return try {
            // 1. Read header
            // Header is at least 10 bytes: [0xAF][fileSize: 4B][idLen: 1B][idBytes: NB][checksum: 4B]
            val headerBuffer = ByteArray(270) // max possible header is ~266 bytes
            var headerBytesRead = 0

            if (initialBuffer != null && initialLength > 0) {
                val toCopy = minOf(initialLength, headerBuffer.size)
                System.arraycopy(initialBuffer, initialOffset, headerBuffer, 0, toCopy)
                headerBytesRead = toCopy
            }

            // Ensure we have at least 6 bytes to read idLen
            while (headerBytesRead < 6) {
                val r = input.read(headerBuffer, headerBytesRead, 6 - headerBytesRead)
                if (r < 0) throw EOFException("EOF while reading sync header prefix")
                headerBytesRead += r
            }

            if (headerBuffer[0] != NexpadProtocol.PACKET_TYPE_FILE_SYNC_START) {
                return Result.failure(IllegalArgumentException("Invalid sync packet magic: ${headerBuffer[0]}"))
            }

            val idLen = headerBuffer[5].toInt() and 0xFF
            val totalHeaderSize = 1 + 4 + 1 + idLen + 4

            // Ensure we have the full header
            while (headerBytesRead < totalHeaderSize) {
                val r = input.read(headerBuffer, headerBytesRead, totalHeaderSize - headerBytesRead)
                if (r < 0) throw EOFException("EOF while reading sync header payload info")
                headerBytesRead += r
            }

            val header = NexpadProtocol.decodeFileSyncHeader(headerBuffer, 0)
                ?: return Result.failure(IllegalArgumentException("Failed to decode FileSyncHeader"))

            val payload = ByteArray(header.fileSize)
            var payloadOffset = 0

            // Copy any payload bytes that were already read into headerBuffer
            val extraHeaderBytes = headerBytesRead - totalHeaderSize
            if (extraHeaderBytes > 0) {
                val toCopy = minOf(extraHeaderBytes, header.fileSize)
                System.arraycopy(headerBuffer, totalHeaderSize, payload, 0, toCopy)
                payloadOffset = toCopy
            }

            // Read remaining payload bytes from stream
            while (payloadOffset < header.fileSize) {
                val r = input.read(payload, payloadOffset, header.fileSize - payloadOffset)
                if (r < 0) throw EOFException("Unexpected EOF while receiving payload (${payloadOffset}/${header.fileSize})")
                payloadOffset += r
            }

            // 2. Verify CRC32
            val computedCrc = NexpadProtocol.computeCrc32(payload)
            if (computedCrc != header.checksum) {
                return Result.failure(IllegalStateException("CRC32 mismatch! Expected ${header.checksum}, got $computedCrc"))
            }

            // 3. Atomically write to filesDir/nxp_remote/
            val safeId = header.componentId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            val targetDir = File(context.filesDir, "nxp_remote").apply { if (!exists()) mkdirs() }
            val targetFile = File(targetDir, "$safeId.nxprc")
            val tempFile = File(targetDir, "$safeId.tmp")
            tempFile.writeBytes(payload)
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                targetFile.writeBytes(payload)
                tempFile.delete()
            }

            // 4. Hot-reload RemoteComponentRegistry
            RemoteComponentRegistry.getInstance(context).reloadAll()
            Log.i(TAG, "⚡ Successfully installed & hot-reloaded '${header.componentId}' (${header.fileSize} bytes)")

            Result.success(header.componentId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive NXPRC sync frame: ${e.message}", e)
            Result.failure(e)
        }
    }
}
