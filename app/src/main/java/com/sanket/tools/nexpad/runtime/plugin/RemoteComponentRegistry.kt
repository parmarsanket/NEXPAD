package com.sanket.tools.nexpad.runtime.plugin

import android.content.Context
import android.net.Uri
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Registry and offline storage manager for imported .nxprc Remote Composable components.
 * Persists binary .nxprc packages under context.filesDir/nxp_remote/
 */
class RemoteComponentRegistry private constructor(private val context: Context) {

    private val remoteDir = File(context.filesDir, "nxp_remote").apply {
        if (!exists()) mkdirs()
    }

    private val _loadedComponents = MutableStateFlow<List<NxprcDocument>>(emptyList())
    val loadedComponents: StateFlow<List<NxprcDocument>> = _loadedComponents.asStateFlow()

    init {
        reloadAll()
    }

    fun reloadAll() {
        val list = mutableListOf<NxprcDocument>()
        remoteDir.listFiles { file -> file.extension.lowercase() == "nxprc" }?.forEach { file ->
            try {
                val bytes = file.readBytes()
                val result = NxprcDocument.decodeFromBytes(bytes)
                result.getOrNull()?.let { doc ->
                    list.add(doc)
                }
            } catch (_: Exception) {
                // Ignore corrupt or invalid files safely
            }
        }
        _loadedComponents.value = list
    }

    fun getComponent(id: String): NxprcDocument? {
        return _loadedComponents.value.find { it.manifest.id == id }
    }

    /**
     * Import a .nxprc file from a Content Uri (e.g. system file picker).
     */
    fun importFromUri(context: Context, uri: Uri): Result<NxprcDocument> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("Could not read data from URI: $uri"))

            val decodeResult = NxprcDocument.decodeFromBytes(bytes)
            val doc = decodeResult.getOrThrow()

            // Strict Validation
            if (doc.manifest.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Manifest id cannot be blank"))
            }
            if (doc.manifest.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Manifest name cannot be blank"))
            }

            val safeFileName = doc.manifest.id.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".nxprc"
            val targetFile = File(remoteDir, safeFileName)
            targetFile.writeBytes(bytes)

            reloadAll()
            ComponentRegistry.getInstance(context).reloadAll()

            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import directly from raw ByteArray (e.g. preview or manual test).
     */
    fun importFromBytes(bytes: ByteArray): Result<NxprcDocument> {
        return try {
            val decodeResult = NxprcDocument.decodeFromBytes(bytes)
            val doc = decodeResult.getOrThrow()

            val safeFileName = doc.manifest.id.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".nxprc"
            val targetFile = File(remoteDir, safeFileName)
            targetFile.writeBytes(bytes)

            reloadAll()
            ComponentRegistry.getInstance(context).reloadAll()

            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteComponent(id: String): Boolean {
        val safeFileName = id.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".nxprc"
        val targetFile = File(remoteDir, safeFileName)
        val deleted = if (targetFile.exists()) targetFile.delete() else false
        if (deleted) {
            reloadAll()
            ComponentRegistry.getInstance(context).reloadAll()
        }
        return deleted
    }

    companion object {
        @Volatile
        private var instance: RemoteComponentRegistry? = null

        fun getInstance(context: Context): RemoteComponentRegistry {
            return instance ?: synchronized(this) {
                instance ?: RemoteComponentRegistry(context.applicationContext).also { instance = it }
            }
        }
    }
}
