package com.sanket.tools.nexpad.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.runtime.plugin.NxprcDocument
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.utils.LayoutManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Portable representation of an exported NEXPAD layout (.nxlayout).
 * Stored as a binary package with magic header "NXLY" (0x4E, 0x58, 0x4C, 0x59).
 *
 * Designed per NEXPAD specification:
 * When exported/shared, custom component skin references are stripped (customComponentId = null)
 * so that any recipient device automatically detects and renders its native default buttons
 * for the layout's button types and positions.
 */
@Serializable
data class NxLayoutDocument(
    val version: Int = 1,
    val name: String,
    val labelStyle: String = "XBOX",
    val isRgbEnabled: Boolean = true,
    val description: String = "",
    val positions: Map<String, Position>,
    val exportedAt: Long = System.currentTimeMillis()
) {
    fun toLayoutProfile(): LayoutProfile = LayoutProfile(
        name = name.trim(),
        isDefault = false,
        isRgbEnabled = isRgbEnabled,
        description = description,
        labelStyle = labelStyle,
        // Canonicalize and ensure all buttons use default native renderers on the receiving phone
        positions = positions.mapValues { (_, pos) -> pos.copy(customComponentId = null) }
    )

    companion object {
        fun fromLayoutProfile(profile: LayoutProfile): NxLayoutDocument = NxLayoutDocument(
            version = 1,
            name = profile.name.trim(),
            labelStyle = profile.labelStyle,
            isRgbEnabled = profile.isRgbEnabled,
            description = profile.description,
            // Clean positions: retain geometry/settings but clear customComponentId for portable sharing
            positions = profile.canonicalPositions().mapValues { (_, pos) ->
                pos.copy(customComponentId = null)
            }
        )
    }
}

/**
 * Centralized manager for all sharing operations in NEXPAD:
 * 1. Exporting & importing portable controller layouts (.nxlayout).
 * 2. Exporting & sharing custom Remote Compose buttons (.nxprc).
 */
object ShareManager {

    /** Magic bytes for .nxlayout files: "NXLY" (0x4E, 0x58, 0x4C, 0x59) */
    val MAGIC_NXLAYOUT = byteArrayOf(0x4E, 0x58, 0x4C, 0x59)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Encodes a LayoutProfile into binary .nxlayout package (NXLY magic header + UTF-8 JSON).
     */
    fun encodeLayout(profile: LayoutProfile): ByteArray {
        val doc = NxLayoutDocument.fromLayoutProfile(profile)
        val jsonBytes = json.encodeToString(doc).toByteArray(Charsets.UTF_8)
        val payload = ByteArray(MAGIC_NXLAYOUT.size + jsonBytes.size)
        System.arraycopy(MAGIC_NXLAYOUT, 0, payload, 0, MAGIC_NXLAYOUT.size)
        System.arraycopy(jsonBytes, 0, payload, MAGIC_NXLAYOUT.size, jsonBytes.size)
        return payload
    }

    /**
     * Decodes binary or raw JSON .nxlayout bytes into a clean LayoutProfile.
     * Supports both NXLY-prefixed binary packages and plain JSON documents.
     */
    fun decodeLayout(bytes: ByteArray): Result<LayoutProfile> {
        return try {
            val jsonString = if (bytes.size >= 4 &&
                bytes[0] == MAGIC_NXLAYOUT[0] &&
                bytes[1] == MAGIC_NXLAYOUT[1] &&
                bytes[2] == MAGIC_NXLAYOUT[2] &&
                bytes[3] == MAGIC_NXLAYOUT[3]
            ) {
                String(bytes, 4, bytes.size - 4, Charsets.UTF_8)
            } else {
                String(bytes, Charsets.UTF_8)
            }

            val profile = try {
                json.decodeFromString<NxLayoutDocument>(jsonString).toLayoutProfile()
            } catch (_: Exception) {
                // Fallback direct decode as LayoutProfile
                val direct = json.decodeFromString<LayoutProfile>(jsonString)
                direct.copy(
                    isDefault = false,
                    positions = direct.canonicalPositions().mapValues { (_, pos) ->
                        pos.copy(customComponentId = null)
                    }
                )
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares a layout profile as a `.nxlayout` file via Android Intent.ACTION_SEND.
     */
    fun shareLayout(context: Context, profile: LayoutProfile) {
        try {
            val cleanName = profile.name.trim().ifBlank { "NEXPAD_Layout" }
            val safeFileName = cleanName.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".nxlayout"
            val shareDir = File(context.cacheDir, "shared_layouts").apply { mkdirs() }
            val targetFile = File(shareDir, safeFileName)

            val bytes = encodeLayout(profile)
            targetFile.writeBytes(bytes)

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, targetFile)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TITLE, "NEXPAD Layout: $cleanName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🎮 NEXPAD Controller Layout: $cleanName (${profile.positions.size} controls)\nDesigned with NEXPAD."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share '$cleanName' (.nxlayout)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to share layout: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Imports a `.nxlayout` file from a Content Uri, saves it as a new profile in LayoutManager,
     * and sets it as the active layout.
     */
    fun importLayoutFromUri(context: Context, uri: Uri, layoutManager: LayoutManager): Result<LayoutProfile> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("Unable to read data from URI: $uri"))

            val decoded = decodeLayout(bytes).getOrThrow()

            // Resolve name collision against existing profiles
            val existingProfiles = layoutManager.getAllProfiles()
            val existingNames = existingProfiles.map { it.name.trim().lowercase() }.toSet()

            var candidateName = decoded.name.trim().ifBlank { "Imported Layout" }
            if (existingNames.contains(candidateName.lowercase())) {
                var counter = 1
                while (existingNames.contains("${candidateName} ($counter)".lowercase())) {
                    counter++
                }
                candidateName = "$candidateName ($counter)"
            }

            val profileToSave = decoded.copy(
                name = candidateName,
                isDefault = false,
                positions = decoded.canonicalPositions().mapValues { (_, pos) ->
                    pos.copy(customComponentId = null)
                }
            )

            layoutManager.saveProfile(profileToSave, activate = true)
            Result.success(profileToSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares a Remote Compose button component as a `.nxprc` binary file via Android Intent.ACTION_SEND.
     */
    fun shareNxprcComponent(
        context: Context,
        componentId: String,
        componentName: String,
        document: NxprcDocument? = null
    ) {
        try {
            val safeName = componentId.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".nxprc"
            val shareDir = File(context.cacheDir, "shared_components").apply { mkdirs() }
            val targetFile = File(shareDir, safeName)

            // 1. Try finding existing file on disk in internal or external nxp_remote directories
            val internalFile = File(File(context.filesDir, "nxp_remote"), safeName)
            val externalFile = context.getExternalFilesDir("nxp_remote")?.let { File(it, safeName) }

            if (internalFile.exists()) {
                internalFile.copyTo(targetFile, overwrite = true)
            } else if (externalFile != null && externalFile.exists()) {
                externalFile.copyTo(targetFile, overwrite = true)
            } else {
                // 2. Fall back to encoding in-memory NxprcDocument
                val doc = document ?: RemoteComponentRegistry.getInstance(context).getComponent(componentId)
                if (doc != null) {
                    val encodedBytes = NxprcDocument.encodeToBytes(doc)
                    targetFile.writeBytes(encodedBytes)
                } else {
                    throw IllegalStateException("Button component definition not found: $componentId")
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, targetFile)

            val displayName = componentName.ifBlank { componentId }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TITLE, "NEXPAD Button: $displayName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🎮 NEXPAD Custom Button: $displayName (.nxprc)\nDesigned with NEXPAD Remote Compose."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share '$displayName' (.nxprc)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to share button: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Handles an incoming ACTION_VIEW Intent URI (e.g. user taps .nxlayout or .nxprc in WhatsApp).
     * Automatically identifies whether it's a layout or a button component,
     * imports it, activates/reloads it, and shows user feedback.
     */
    suspend fun handleIncomingFileUri(
        context: Context,
        uri: Uri,
        layoutManager: LayoutManager
    ) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val fileName = getFileNameFromUri(context, uri)?.lowercase()

        val isExplicitLayout = fileName?.endsWith(".nxlayout") == true
        val isExplicitNxprc = fileName?.endsWith(".nxprc") == true

        if (isExplicitLayout) {
            val result = importLayoutFromUri(context, uri, layoutManager)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.onSuccess { profile ->
                    Toast.makeText(
                        context,
                        "🎮 Layout '${profile.name}' imported & activated!",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        "Failed to import layout: ${err.localizedMessage ?: "Invalid file"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return@withContext
        }

        if (isExplicitNxprc) {
            val result = RemoteComponentRegistry.getInstance(context).importFromUri(context, uri)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.onSuccess { doc ->
                    Toast.makeText(
                        context,
                        "⚡ Button '${doc.manifest.name}' (.nxprc) imported!",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        "Failed to import button: ${err.localizedMessage ?: "Invalid file"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return@withContext
        }

        // Fallback: Messaging apps (like WhatsApp) sometimes strip filenames in content URIs.
        // We inspect the 4-byte magic header ("NXLY" for layout, "NXRC" for nxprc).
        try {
            val header = ByteArray(4)
            val readBytes = context.contentResolver.openInputStream(uri)?.use { it.read(header) } ?: 0
            if (readBytes >= 4) {
                if (header.contentEquals(MAGIC_NXLAYOUT)) {
                    val result = importLayoutFromUri(context, uri, layoutManager)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        result.onSuccess { profile ->
                            Toast.makeText(
                                context,
                                "🎮 Layout '${profile.name}' imported & activated!",
                                Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { err ->
                            Toast.makeText(
                                context,
                                "Failed to import layout: ${err.localizedMessage ?: "Invalid file"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    return@withContext
                } else if (header.contentEquals(NxprcDocument.MAGIC)) {
                    val result = RemoteComponentRegistry.getInstance(context).importFromUri(context, uri)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        result.onSuccess { doc ->
                            Toast.makeText(
                                context,
                                "⚡ Button '${doc.manifest.name}' (.nxprc) imported!",
                                Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { err ->
                            Toast.makeText(
                                context,
                                "Failed to import button: ${err.localizedMessage ?: "Invalid file"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    return@withContext
                }
            }
        } catch (_: Exception) {}

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "Unrecognized NEXPAD file format", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Resolves display name from ContentResolver OpenableColumns if available.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            return cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }
}
