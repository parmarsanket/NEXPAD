package com.sanket.tools.nexpad.runtime.registry

import android.content.Context
import com.sanket.tools.nexpad.runtime.model.NxpComponentDef
import com.sanket.tools.nexpad.runtime.model.NxpManifest
import com.sanket.tools.nexpad.runtime.model.NxpSize
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

class ComponentRegistry private constructor(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val componentsDir = File(context.filesDir, "nxp_components").apply {
        if (!exists()) mkdirs()
    }

    private val _installedComponents = MutableStateFlow<List<NxpComponentDef>>(emptyList())
    val installedComponents: StateFlow<List<NxpComponentDef>> = _installedComponents.asStateFlow()

    init {
        reloadAll()
    }

    fun reloadAll() {
        val list = mutableListOf<NxpComponentDef>()
        // 1. Always include built-in presets
        list.addAll(DefaultComponents.ALL_PRESETS)

        // 2. Load user-imported components from disk
        componentsDir.listFiles { file -> file.extension.lowercase() in listOf("json", "nxpcomponent") }?.forEach { file ->
            try {
                val content = file.readText()
                val def = json.decodeFromString<NxpComponentDef>(content)
                list.add(def)
            } catch (_: Exception) {
                // Ignore corrupt or invalid files safely
            }
        }

        // 3. Bridge remote .nxprc components
        try {
            val remoteRegistry = RemoteComponentRegistry.getInstance(context)
            remoteRegistry.loadedComponents.value.forEach { doc ->
                val bridgedDef = NxpComponentDef(
                    manifest = NxpManifest(
                        id = doc.manifest.id,
                        name = doc.manifest.name,
                        author = doc.manifest.author,
                        version = doc.manifest.version,
                        category = doc.manifest.category,
                        defaultControl = doc.manifest.defaultControl,
                        description = doc.manifest.description
                    ),
                    size = NxpSize(
                        widthDp = doc.manifest.widthDp,
                        heightDp = doc.manifest.heightDp
                    )
                )
                list.add(bridgedDef)
            }
        } catch (_: Exception) {
            // Safe fallback
        }

        _installedComponents.value = list
    }

    fun getComponent(id: String): NxpComponentDef? {
        return _installedComponents.value.find { it.manifest.id == id }
    }

    fun importFromJson(jsonString: String): Result<NxpComponentDef> {
        return try {
            val def = json.decodeFromString<NxpComponentDef>(jsonString)

            // Strict Validation
            if (def.manifest.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Component ID cannot be blank"))
            }
            if (def.manifest.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Component Name cannot be blank"))
            }
            if (def.size.widthDp !in 32..350 || def.size.heightDp !in 32..350) {
                return Result.failure(IllegalArgumentException("Component dimensions must be between 32dp and 350dp"))
            }

            // Sanitize filename
            val safeFileName = def.manifest.id.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".json"
            val targetFile = File(componentsDir, safeFileName)
            targetFile.writeText(json.encodeToString(NxpComponentDef.serializer(), def))

            reloadAll()
            Result.success(def)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteComponent(id: String): Boolean {
        if (id.startsWith("builtin.")) return false // Cannot delete system presets
        if (id.startsWith("rc.")) {
            return RemoteComponentRegistry.getInstance(context).deleteComponent(id)
        }

        val safeFileName = id.replace(Regex("[^a-zA-Z0-9_.-]"), "_") + ".json"
        val targetFile = File(componentsDir, safeFileName)
        val deleted = if (targetFile.exists()) targetFile.delete() else false
        if (deleted) {
            reloadAll()
        }
        return deleted
    }

    fun exportToJson(id: String): String? {
        val comp = getComponent(id) ?: return null
        return try {
            json.encodeToString(NxpComponentDef.serializer(), comp)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: ComponentRegistry? = null

        fun getInstance(context: Context): ComponentRegistry {
            return instance ?: synchronized(this) {
                instance ?: ComponentRegistry(context.applicationContext).also { instance = it }
            }
        }
    }
}
