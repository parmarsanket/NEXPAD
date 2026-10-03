package com.sanket.tools.nexpad.utils

import android.content.Context
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LayoutManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("NEXPAD_LAYOUTS_V3", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _profilesFlow = MutableStateFlow<List<LayoutProfile>>(emptyList())
    val profilesFlow: StateFlow<List<LayoutProfile>> = _profilesFlow.asStateFlow()

    private val _activeProfileNameFlow = MutableStateFlow<String>(
        prefs.getString("active_profile", "Standard Elite")?.let {
            if (it == "Standard") "Standard Elite" else it
        } ?: "Standard Elite"
    )
    val activeProfileNameFlow: StateFlow<String> = _activeProfileNameFlow.asStateFlow()

    init {
        // Migration & Cache Cleanup:
        // Always purge cached default layouts if they contain obsolete paddles (M1-M4) or old cutouts (0.055, 0.080)
        val defaults = getDefaultLayoutProfiles()
        val defaultNames = defaults.map { it.name.trim().lowercase() }.toSet()
        val editor = prefs.edit()
        var needsCommit = false

        // 1. Purge obsolete default profiles saved in prefs
        defaults.forEach { defaultProfile ->
            val savedJson = prefs.getString("profile_${defaultProfile.name}", null)
            if (savedJson != null) {
                if (savedJson.contains("\"M1\"") || savedJson.contains("\"M2\"") ||
                    savedJson.contains("\"M3\"") || savedJson.contains("\"M4\"") ||
                    savedJson.contains("0.055") || savedJson.contains("0.080") ||
                    savedJson.contains("0.280") ||
                    savedJson.contains("0.265") || savedJson.contains("0.645") ||
                    savedJson.contains("0.165") || savedJson.contains("0.335") ||
                    savedJson.contains("0.125") || savedJson.contains("0.820") ||
                    // Layout 2 (DualSense) old values
                    savedJson.contains("0.300") || savedJson.contains("0.385") ||
                    savedJson.contains("0.615") ||
                    // Layout 3 (FPS Tactical) old values
                    savedJson.contains("0.120") ||
                    // Layout 4 (Retro Arcade) old values
                    savedJson.contains("0.190") || savedJson.contains("0.640") ||
                    savedJson.contains("0.340") || savedJson.contains("0.415") ||
                    savedJson.contains("0.665") ||
                    // Layout 5 (Sim Racing) old values
                    savedJson.contains("0.095") || savedJson.contains("0.145") ||
                    savedJson.contains("0.905") || savedJson.contains("0.350") ||
                    savedJson.contains("0.185") || savedJson.contains("0.680") ||
                    savedJson.contains("0.815") ||
                    // Layout 6 (Grand MOBA) old values
                    savedJson.contains("0.670") || savedJson.contains("0.900") ||
                    // Fresh retune old coordinates for all 5 non-elite layouts
                    savedJson.contains("0.770") || savedJson.contains("0.840") ||
                    savedJson.contains("0.730") || savedJson.contains("0.850") ||
                    savedJson.contains("0.530") || savedJson.contains("0.555") ||
                    savedJson.contains("0.480") || savedJson.contains("0.590") ||
                    savedJson.contains("0.890") || savedJson.contains("0.780") ||
                    savedJson.contains("0.800") || savedJson.contains("0.660") ||
                    savedJson.contains("0.775") || savedJson.contains("0.885") ||
                    savedJson.contains("0.700") || savedJson.contains("0.810") ||
                    savedJson.contains("0.230") || savedJson.contains("0.380") ||
                    savedJson.contains("0.440")
                ) {
                    editor.remove("profile_${defaultProfile.name}")
                    needsCommit = true
                }
            }
        }

        // 2. Sanitize custom_profile_names to ensure no default layout is ever treated as custom
        val rawCustomNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
        val cleanedCustomNames = rawCustomNames.filterNot { defaultNames.contains(it.trim().lowercase()) }.toSet()
        if (cleanedCustomNames.size != rawCustomNames.size) {
            editor.putStringSet("custom_profile_names", HashSet(cleanedCustomNames))
            needsCommit = true
        }

        if (needsCommit) {
            editor.apply()
        }

        val savedName = prefs.getString("active_profile", "Standard Elite") ?: "Standard Elite"
        _activeProfileNameFlow.value = if (savedName == "Standard") "Standard Elite" else savedName
        refreshProfilesFlow()
    }

    private fun refreshProfilesFlow() {
        _profilesFlow.value = getAllProfiles()
    }

    /** Applies a button skin (or default) to the active profile. */
    fun applyButtonSkinToActiveProfile(key: String, customComponentId: String?) {
        val active = getActiveProfile()
        val posMap = active.positions.toMutableMap()
        val currentPos = posMap[key] ?: defaultPositions()[key] ?: Position(0.5f, 0.5f)
        posMap[key] = currentPos.copy(customComponentId = customComponentId)
        saveProfile(active.copy(positions = posMap), activate = true)
    }

    /** Returns all available profiles: default layouts (using factory positions) plus user custom layouts. */
    fun getAllProfiles(): List<LayoutProfile> {
        val defaults = getDefaultLayoutProfiles()
        val result = mutableListOf<LayoutProfile>()

        // 1. Load default profiles (preserving user skins/labels, but ALWAYS using factory positions from code)
        for (defaultProfile in defaults) {
            val savedJson = prefs.getString("profile_${defaultProfile.name}", null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    val mergedPositions = defaultProfile.canonicalPositions().toMutableMap()
                    loaded.canonicalPositions().forEach { (k, v) ->
                        if (mergedPositions.containsKey(k) && v.customComponentId != null) {
                            mergedPositions[k] = mergedPositions[k]!!.copy(customComponentId = v.customComponentId)
                        }
                    }
                    result.add(
                        defaultProfile.copy(
                            isDefault = true,
                            isRgbEnabled = loaded.isRgbEnabled,
                            labelStyle = loaded.labelStyle,
                            positions = mergedPositions
                        )
                    )
                } catch (e: Exception) {
                    result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
                }
            } else {
                result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
            }
        }

        // 2. Load custom profiles (ignoring any name that collides with a default layout)
        val defaultNames = defaults.map { it.name.trim().lowercase() }.toSet()
        val customNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
        for (name in customNames) {
            if (defaultNames.contains(name.trim().lowercase())) continue

            val savedJson = prefs.getString("profile_$name", null)
                ?: prefs.getString("profile_${name.trim()}", null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    result.add(loaded.copy(isDefault = false, positions = loaded.canonicalPositions()))
                } catch (e: Exception) {
                    // Ignore corrupted profile
                }
            }
        }

        // 3. Apply custom ordering if saved
        val savedOrderJson = prefs.getString("profile_order", null)
        if (savedOrderJson != null) {
            try {
                val orderList = json.decodeFromString<List<String>>(savedOrderJson)
                val profileMap = result.associateBy { it.name }
                val orderedResult = mutableListOf<LayoutProfile>()
                for (name in orderList) {
                    profileMap[name]?.let { orderedResult.add(it) }
                }
                // Append any unlisted profiles (e.g., newly created or defaults not yet in order)
                for (profile in result) {
                    if (orderedResult.none { it.name.equals(profile.name, ignoreCase = true) }) {
                        orderedResult.add(profile)
                    }
                }
                return orderedResult
            } catch (e: Exception) {
                // Fallback to un-ordered result on decode issue
            }
        }

        return result
    }

    /** Saves custom display ordering of layouts and notifies active observers. */
    fun saveProfileOrder(order: List<String>) {
        val jsonOrder = json.encodeToString(order)
        prefs.edit().putString("profile_order", jsonOrder).apply()
        refreshProfilesFlow()
    }

    /** Save profile. If it's custom, adds to custom profiles set. */
    fun saveProfile(profile: LayoutProfile, activate: Boolean = false) {
        val canonicalProfile = profile.copy(positions = profile.canonicalPositions())
        val jsonString = json.encodeToString(canonicalProfile)
        val editor = prefs.edit()
        editor.putString("profile_${canonicalProfile.name}", jsonString)

        if (!canonicalProfile.isDefault) {
            val rawCustomNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
            val customNames = HashSet(rawCustomNames)
            customNames.removeAll { it.trim().equals(canonicalProfile.name.trim(), ignoreCase = true) }
            customNames.add(canonicalProfile.name)
            editor.putStringSet("custom_profile_names", customNames)
        }
        editor.apply()

        if (activate) {
            setActiveProfile(canonicalProfile.name)
        } else {
            refreshProfilesFlow()
        }
    }

    /** Load profile by name. */
    fun loadProfile(name: String): LayoutProfile? {
        val trimmedName = name.trim()
        val defaultProfile = getDefaultLayoutProfiles().find { it.name.trim().equals(trimmedName, ignoreCase = true) }
        val savedJson = prefs.getString("profile_$trimmedName", null)
            ?: prefs.getString("profile_$name", null)

        if (defaultProfile != null) {
            if (savedJson != null) {
                return try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    val mergedPositions = defaultProfile.canonicalPositions().toMutableMap()
                    loaded.canonicalPositions().forEach { (k, v) ->
                        if (mergedPositions.containsKey(k) && v.customComponentId != null) {
                            mergedPositions[k] = mergedPositions[k]!!.copy(customComponentId = v.customComponentId)
                        }
                    }
                    defaultProfile.copy(
                        isDefault = true,
                        isRgbEnabled = loaded.isRgbEnabled,
                        labelStyle = loaded.labelStyle,
                        positions = mergedPositions
                    )
                } catch (e: Exception) {
                    defaultProfile.copy(positions = defaultProfile.canonicalPositions())
                }
            }
            return defaultProfile.copy(positions = defaultProfile.canonicalPositions())
        }

        if (savedJson != null) {
            return try {
                val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                loaded.copy(isDefault = false, positions = loaded.canonicalPositions())
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    /**
     * Delete profile.
     * Default layouts (1 to 6) CANNOT be deleted. Returns false if default.
     */
    fun deleteProfile(name: String): Boolean {
        val trimmedName = name.trim()
        val isDefault = getDefaultLayoutProfiles().any { it.name.trim().equals(trimmedName, ignoreCase = true) }
        if (isDefault) {
            // Protected: Default layouts cannot be deleted
            return false
        }

        val rawCustomNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
        val customNames = HashSet(rawCustomNames)
        val removed = customNames.removeAll { it.trim().equals(trimmedName, ignoreCase = true) }

        val editor = prefs.edit()
            .putStringSet("custom_profile_names", customNames)
            .remove("profile_$trimmedName")
            .remove("profile_$name")

        // Also remove any key starting with "profile_" matching trimmedName case-insensitively
        prefs.all.keys.forEach { key ->
            if (key.startsWith("profile_") && key.substringAfter("profile_").trim().equals(trimmedName, ignoreCase = true)) {
                editor.remove(key)
            }
        }

        // Also update saved profile_order
        val savedOrderJson = prefs.getString("profile_order", null)
        if (savedOrderJson != null) {
            try {
                val orderList = json.decodeFromString<List<String>>(savedOrderJson).toMutableList()
                if (orderList.removeAll { it.trim().equals(trimmedName, ignoreCase = true) }) {
                    editor.putString("profile_order", json.encodeToString(orderList))
                }
            } catch (_: Exception) {}
        }
        editor.apply()

        // If the deleted profile was active, switch to Default 1 (Standard Elite)
        val activeName = prefs.getString("active_profile", "Standard Elite")
        if (activeName == null || activeName.trim().equals(trimmedName, ignoreCase = true)) {
            setActiveProfile("Standard Elite")
        } else {
            refreshProfilesFlow()
        }

        return removed || true
    }

    /**
     * Renames a custom layout profile.
     * Default layouts (1 to 6) cannot be renamed.
     * Returns true on success, false if oldName is default or not found.
     */
    fun renameProfile(oldName: String, newName: String): Boolean {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isEmpty()) return false
        if (trimmedOld.equals(trimmedNew, ignoreCase = false)) return true

        val isDefault = getDefaultLayoutProfiles().any { it.name.trim().equals(trimmedOld, ignoreCase = true) }
        if (isDefault) {
            return false
        }

        val existing = loadProfile(trimmedOld) ?: return false
        val wasActive = getActiveProfile().name.trim().equals(trimmedOld, ignoreCase = true)

        val rawCustomNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
        val customNames = HashSet(rawCustomNames)
        customNames.removeAll { it.trim().equals(trimmedOld, ignoreCase = true) }
        customNames.add(trimmedNew)

        val renamed = existing.copy(name = trimmedNew, isDefault = false)
        val canonicalProfile = renamed.copy(positions = renamed.canonicalPositions())
        val jsonString = json.encodeToString(canonicalProfile)

        val renameEditor = prefs.edit()
            .putStringSet("custom_profile_names", customNames)
            .remove("profile_$trimmedOld")
            .remove("profile_$oldName")
            .putString("profile_$trimmedNew", jsonString)

        prefs.all.keys.forEach { key ->
            if (key.startsWith("profile_") && key.substringAfter("profile_").trim().equals(trimmedOld, ignoreCase = true)) {
                renameEditor.remove(key)
            }
        }

        // Also update saved profile_order
        val savedOrderJson = prefs.getString("profile_order", null)
        if (savedOrderJson != null) {
            try {
                val orderList = json.decodeFromString<List<String>>(savedOrderJson).toMutableList()
                val idx = orderList.indexOfFirst { it.trim().equals(trimmedOld, ignoreCase = true) }
                if (idx != -1) {
                    orderList[idx] = trimmedNew
                    renameEditor.putString("profile_order", json.encodeToString(orderList))
                }
            } catch (_: Exception) {}
        }
        renameEditor.apply()

        if (wasActive) {
            setActiveProfile(trimmedNew)
        } else {
            refreshProfilesFlow()
        }

        return true
    }

    /** Reset a default layout to its original factory coordinates. */
    fun resetDefaultProfile(name: String): LayoutProfile? {
        val trimmedName = name.trim()
        val factoryDefault = getDefaultLayoutProfiles().find { it.name.trim().equals(trimmedName, ignoreCase = true) } ?: return null
        val editor = prefs.edit()
            .remove("profile_$trimmedName")
            .remove("profile_$name")
        prefs.all.keys.forEach { key ->
            if (key.startsWith("profile_") && key.substringAfter("profile_").trim().equals(trimmedName, ignoreCase = true)) {
                editor.remove(key)
            }
        }
        editor.apply()
        refreshProfilesFlow()
        return factoryDefault
    }

    /** Create a new custom profile from a base profile and optional subset of buttons. */
    fun createCustomProfile(
        name: String,
        baseProfile: LayoutProfile,
        selectedButtons: Set<String>? = null,
        activate: Boolean = false
    ): LayoutProfile {
        val baseCanonical = baseProfile.canonicalPositions()
        val positions = if (selectedButtons != null) {
            val selectedCanonical = selectedButtons.map {
                ControlKey.fromIdentifier(it)?.key ?: it.uppercase()
            }.toSet()
            baseCanonical.filterKeys { k ->
                val can = ControlKey.fromIdentifier(k)?.key ?: k.uppercase()
                selectedCanonical.contains(can)
            }
        } else {
            baseCanonical
        }

        val newProfile = LayoutProfile(
            name = name.trim(),
            isDefault = false,
            isRgbEnabled = baseProfile.isRgbEnabled,
            positions = positions,
            description = "Custom Layout based on ${baseProfile.name}",
            labelStyle = baseProfile.labelStyle
        )

        saveProfile(newProfile, activate = activate)
        return newProfile
    }

    /** Updates the button labeling style (Xbox vs PlayStation) for a profile. */
    fun setProfileLabelStyle(profileName: String, style: ControllerLabelStyle) {
        val profile = loadProfile(profileName) ?: return
        val wasActive = getActiveProfile().name.trim().equals(profileName.trim(), ignoreCase = true)
        saveProfile(profile.copy(labelStyle = style.id), activate = wasActive)
    }

    /** Set the active layout profile. */
    fun setActiveProfile(name: String) {
        val effectiveName = if (name.trim().equals("Standard", ignoreCase = true)) "Standard Elite" else name.trim()
        prefs.edit().putString("active_profile", effectiveName).commit()
        _activeProfileNameFlow.value = effectiveName
        refreshProfilesFlow()
    }

    /** Returns currently active profile. */
    fun getActiveProfile(): LayoutProfile {
        val activeName = prefs.getString("active_profile", "Standard Elite") ?: "Standard Elite"
        // Handle legacy "Standard" name mapping to "Standard Elite"
        val effectiveName = if (activeName.trim().equals("Standard", ignoreCase = true)) "Standard Elite" else activeName.trim()

        val all = getAllProfiles()
        return all.find { it.name.trim().equals(effectiveName, ignoreCase = true) }
            ?: all.firstOrNull()
            ?: LayoutProfile(name = effectiveName)
    }

    /** Updates touchpad sensitivity preferences in nexpad_prefs. */
    fun updateTouchpadSensitivity(key: String, sens: Float) {
        val sp = appContext.getSharedPreferences("nexpad_prefs", Context.MODE_PRIVATE)
        val upperKey = key.uppercase()
        sp.edit()
            .putFloat("TOUCHPAD_SENSITIVITY_$upperKey", sens)
            .putFloat("TOUCHPAD_SENSITIVITY", sens)
            .apply()
    }
}
