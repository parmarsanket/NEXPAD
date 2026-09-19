package com.sanket.tools.nexpad.utils

import android.content.Context
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LayoutManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("NEXPAD_LAYOUTS_V3", Context.MODE_PRIVATE)
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
        saveProfile(active.copy(positions = posMap))
    }

    /** Returns all available profiles: 5 default layouts (with any saved overrides) plus user custom layouts. */
    fun getAllProfiles(): List<LayoutProfile> {
        val defaults = getDefaultLayoutProfiles()
        val result = mutableListOf<LayoutProfile>()

        // 1. Load default profiles (with user position overrides if saved)
        for (defaultProfile in defaults) {
            val savedJson = prefs.getString("profile_${defaultProfile.name}", null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    result.add(loaded.copy(isDefault = true, positions = loaded.canonicalPositions()))
                } catch (e: Exception) {
                    result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
                }
            } else {
                result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
            }
        }

        // 2. Load custom profiles
        val customNames = prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()
        for (name in customNames) {
            val savedJson = prefs.getString("profile_$name", null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    result.add(loaded.copy(isDefault = false, positions = loaded.canonicalPositions()))
                } catch (e: Exception) {
                    // Ignore corrupted profile
                }
            }
        }

        return result
    }

    /** Save profile. If it's custom, adds to custom profiles set. */
    fun saveProfile(profile: LayoutProfile, activate: Boolean = true) {
        val canonicalProfile = profile.copy(positions = profile.canonicalPositions())
        val jsonString = json.encodeToString(canonicalProfile)
        prefs.edit().putString("profile_${canonicalProfile.name}", jsonString).apply()

        if (!canonicalProfile.isDefault) {
            val customNames = (prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()).toMutableSet()
            customNames.add(canonicalProfile.name)
            prefs.edit().putStringSet("custom_profile_names", customNames).apply()
        }

        if (activate) {
            setActiveProfile(canonicalProfile.name)
        } else {
            refreshProfilesFlow()
        }
    }

    /** Load profile by name. */
    fun loadProfile(name: String): LayoutProfile? {
        val savedJson = prefs.getString("profile_$name", null)
        if (savedJson != null) {
            return try {
                val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                loaded.copy(positions = loaded.canonicalPositions())
            } catch (e: Exception) {
                null
            }
        }
        return getDefaultLayoutProfiles().find { it.name.equals(name, ignoreCase = true) }?.let {
            it.copy(positions = it.canonicalPositions())
        }
    }

    /**
     * Delete profile.
     * Default layouts (1 to 5) CANNOT be deleted. Returns false if default.
     */
    fun deleteProfile(name: String): Boolean {
        val isDefault = getDefaultLayoutProfiles().any { it.name.equals(name, ignoreCase = true) }
        if (isDefault) {
            // Protected: Default layouts cannot be deleted
            return false
        }

        val customNames = (prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()).toMutableSet()
        val removed = customNames.remove(name)
        prefs.edit()
            .putStringSet("custom_profile_names", customNames)
            .remove("profile_$name")
            .apply()

        // If the deleted profile was active, switch to Default 1 (Standard Elite)
        val activeName = prefs.getString("active_profile", "Standard Elite")
        if (activeName == name) {
            setActiveProfile("Standard Elite")
        } else {
            refreshProfilesFlow()
        }

        return removed
    }

    /**
     * Renames a custom layout profile.
     * Default layouts (1 to 5) cannot be renamed.
     * Returns true on success, false if oldName is default or not found.
     */
    fun renameProfile(oldName: String, newName: String): Boolean {
        val trimmedNew = newName.trim()
        if (trimmedNew.isEmpty()) return false
        if (oldName == trimmedNew) return true

        val isDefault = getDefaultLayoutProfiles().any { it.name.equals(oldName, ignoreCase = true) }
        if (isDefault) {
            return false
        }

        val existing = loadProfile(oldName) ?: return false
        val wasActive = getActiveProfile().name.equals(oldName, ignoreCase = true)

        val customNames = (prefs.getStringSet("custom_profile_names", emptySet()) ?: emptySet()).toMutableSet()
        customNames.remove(oldName)
        customNames.add(trimmedNew)

        val renamed = existing.copy(name = trimmedNew, isDefault = false)
        val canonicalProfile = renamed.copy(positions = renamed.canonicalPositions())
        val jsonString = json.encodeToString(canonicalProfile)

        prefs.edit()
            .putStringSet("custom_profile_names", customNames)
            .remove("profile_$oldName")
            .putString("profile_$trimmedNew", jsonString)
            .apply()

        if (wasActive) {
            setActiveProfile(trimmedNew)
        } else {
            refreshProfilesFlow()
        }

        return true
    }

    /** Reset a default layout to its original factory coordinates. */
    fun resetDefaultProfile(name: String): LayoutProfile? {
        val factoryDefault = getDefaultLayoutProfiles().find { it.name.equals(name, ignoreCase = true) } ?: return null
        prefs.edit().remove("profile_$name").apply()
        refreshProfilesFlow()
        return factoryDefault
    }

    /** Create a new custom profile from a base profile and optional subset of buttons. */
    fun createCustomProfile(
        name: String,
        baseProfile: LayoutProfile,
        selectedButtons: Set<String>? = null
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
            name = name,
            isDefault = false,
            isRgbEnabled = baseProfile.isRgbEnabled,
            positions = positions,
            description = "Custom Layout based on ${baseProfile.name}"
        )

        saveProfile(newProfile, activate = true)
        return newProfile
    }

    /** Set the active layout profile. */
    fun setActiveProfile(name: String) {
        val effectiveName = if (name == "Standard") "Standard Elite" else name
        prefs.edit().putString("active_profile", effectiveName).apply()
        _activeProfileNameFlow.value = effectiveName
        refreshProfilesFlow()
    }

    /** Returns currently active profile. */
    fun getActiveProfile(): LayoutProfile {
        val activeName = prefs.getString("active_profile", "Standard Elite") ?: "Standard Elite"
        // Handle legacy "Standard" name mapping to "Standard Elite"
        val effectiveName = if (activeName == "Standard") "Standard Elite" else activeName

        val all = getAllProfiles()
        return all.find { it.name.equals(effectiveName, ignoreCase = true) }
            ?: all.firstOrNull()
            ?: LayoutProfile(name = effectiveName)
    }
}
