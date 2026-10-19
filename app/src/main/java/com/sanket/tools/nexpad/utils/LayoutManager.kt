package com.sanket.tools.nexpad.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.model.DefaultProfileType
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.getDefaultLayoutProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Result of validating a layout profile name.
 */
sealed class ProfileNameValidation {
    object Valid : ProfileNameValidation()
    data class Error(val message: String) : ProfileNameValidation()
}

/**
 * Production-hardened layout profile manager for NEXPAD.
 * Manages factory default layouts, custom user configurations, persistent JSON storage,
 * atomic in-memory caching, validation, and observable StateFlow streams.
 */
class LayoutManager(
    context: Context,
    prefsOverride: android.content.SharedPreferences? = null
) {
    private val appContext = context.applicationContext ?: context
    private val prefs = prefsOverride ?: appContext.getSharedPreferences(NexpadPreferenceKeys.FILE_LAYOUTS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowSpecialFloatingPointValues = true }

    private val lock = Any()

    @Volatile
    private var cacheVersion = 0L

    @Volatile
    private var cachedProfiles: List<LayoutProfile>? = null

    private val _profilesFlow = MutableStateFlow<List<LayoutProfile>>(emptyList())
    val profilesFlow: StateFlow<List<LayoutProfile>> = _profilesFlow.asStateFlow()

    private val _activeProfileNameFlow = MutableStateFlow<String>(
        DefaultProfileType.resolveCanonicalName(
            prefs.getString(NexpadPreferenceKeys.KEY_ACTIVE_PROFILE, DefaultProfileType.DEFAULT.profileName)
        )
    )
    val activeProfileNameFlow: StateFlow<String> = _activeProfileNameFlow.asStateFlow()

    init {
        // Schema Migration & Cache Cleanup:
        // Version 0 represents initial release baseline. When layout schema changes in future updates,
        // increment KEY_LAYOUT_MIGRATION_VERSION to trigger update migrations.
        val defaults = getDefaultLayoutProfiles()
        val defaultNames = defaults.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val savedVersion = prefs.getInt(NexpadPreferenceKeys.KEY_LAYOUT_MIGRATION_VERSION, 0)
        val shouldUpdateVersion = savedVersion != NexpadPreferenceKeys.DEFAULT_LAYOUT_MIGRATION_VERSION

        synchronized(lock) {
            val rawCustomNames = getCustomProfileNamesInternal()
            val cleanedCustomNames = rawCustomNames.filterNot { defaultNames.contains(it.trim().lowercase(Locale.ROOT)) }.toSet()
            val shouldCleanCustomNames = cleanedCustomNames.size != rawCustomNames.size

            if (shouldUpdateVersion || shouldCleanCustomNames) {
                prefs.edit {
                    if (shouldUpdateVersion) {
                        putInt(NexpadPreferenceKeys.KEY_LAYOUT_MIGRATION_VERSION, NexpadPreferenceKeys.DEFAULT_LAYOUT_MIGRATION_VERSION)
                    }
                    if (shouldCleanCustomNames) {
                        putStringSet(NexpadPreferenceKeys.KEY_CUSTOM_PROFILE_NAMES, HashSet(cleanedCustomNames))
                    }
                }
            }

            val savedName = prefs.getString(NexpadPreferenceKeys.KEY_ACTIVE_PROFILE, DefaultProfileType.DEFAULT.profileName)
            _activeProfileNameFlow.value = DefaultProfileType.resolveCanonicalName(savedName)
            refreshProfilesFlowLocked()
        }
    }

    private fun getCustomProfileNamesInternal(): Set<String> {
        return prefs.getStringSet(NexpadPreferenceKeys.KEY_CUSTOM_PROFILE_NAMES, emptySet()) ?: emptySet()
    }

    /**
     * Validates a candidate layout profile name against system constraints:
     * 1. Rejects blank or whitespace-only names.
     * 2. Enforces maximum length limit (32 characters).
     * 3. Rejects control characters, newlines, and tabs.
     * 4. Enforces case-insensitive uniqueness against reserved factory default profiles.
     * 5. Enforces case-insensitive uniqueness against existing custom profiles (excluding [currentName]).
     */
    fun validateProfileName(name: String, currentName: String? = null): ProfileNameValidation {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return ProfileNameValidation.Error("Profile name cannot be blank.")
        }
        if (trimmed.length > MAX_PROFILE_NAME_LENGTH) {
            return ProfileNameValidation.Error("Profile name cannot exceed $MAX_PROFILE_NAME_LENGTH characters.")
        }
        if (trimmed.any { it.isISOControl() || it == '\n' || it == '\r' || it == '\t' }) {
            return ProfileNameValidation.Error("Profile name contains invalid control characters.")
        }

        // Check collision against factory defaults
        val isDefaultCollision = DefaultProfileType.isDefaultProfile(trimmed) ||
            getDefaultLayoutProfiles().any { it.name.trim().equals(trimmed, ignoreCase = true) }
        if (isDefaultCollision) {
            return ProfileNameValidation.Error("Cannot use reserved default layout name '$trimmed'.")
        }

        // Check collision against other custom profiles
        val customNames = getCustomProfileNamesInternal()
        val isCustomCollision = customNames.any {
            it.trim().equals(trimmed, ignoreCase = true) &&
                (currentName == null || !it.trim().equals(currentName.trim(), ignoreCase = true))
        }
        if (isCustomCollision) {
            return ProfileNameValidation.Error("A layout with name '$trimmed' already exists.")
        }

        return ProfileNameValidation.Valid
    }

    /**
     * Resolves the canonical profile name matching [name] case-insensitively.
     * Returns the exact casing stored in factory defaults or custom profile preferences,
     * or null if the profile does not exist.
     */
    fun findCanonicalProfileName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check default layouts
        getDefaultLayoutProfiles().find { it.name.trim().equals(trimmed, ignoreCase = true) }?.let {
            return it.name
        }

        // 2. Check custom profiles
        val customNames = getCustomProfileNamesInternal()
        return customNames.find { it.trim().equals(trimmed, ignoreCase = true) }
    }

    private fun refreshProfilesFlowLocked() {
        cacheVersion++
        cachedProfiles = null
        val loaded = getAllProfilesInternal()
        cachedProfiles = loaded
        _profilesFlow.value = loaded
    }

    private fun refreshProfilesFlow() {
        synchronized(lock) {
            refreshProfilesFlowLocked()
        }
    }

    /** Returns all available profiles: default layouts plus user custom layouts. Fully thread-safe. */
    fun getAllProfiles(): List<LayoutProfile> {
        cachedProfiles?.let { return it }
        synchronized(lock) {
            cachedProfiles?.let { return it }
            val loaded = getAllProfilesInternal()
            cachedProfiles = loaded
            return loaded
        }
    }

    private fun getAllProfilesInternal(): List<LayoutProfile> {
        val defaults = getDefaultLayoutProfiles()
        val result = mutableListOf<LayoutProfile>()

        // 1. Load default profiles (preserving user customizations, falling back to factory positions for missing keys)
        for (defaultProfile in defaults) {
            val savedJson = prefs.getString(NexpadPreferenceKeys.profileKey(defaultProfile.name), null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    val mergedPositions = defaultProfile.canonicalPositions().toMutableMap()
                    loaded.canonicalPositions().forEach { (k, v) ->
                        mergedPositions[k] = v.sanitized()
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
                    Log.w(TAG, "Failed to decode customizations for default profile '${defaultProfile.name}'. Resetting to factory defaults.", e)
                    result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
                }
            } else {
                result.add(defaultProfile.copy(positions = defaultProfile.canonicalPositions()))
            }
        }

        // 2. Load custom profiles (ignoring any name that collides with a default layout)
        val defaultNames = defaults.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val customNames = getCustomProfileNamesInternal()
        for (name in customNames) {
            if (defaultNames.contains(name.trim().lowercase(Locale.ROOT))) continue

            val savedJson = prefs.getString(NexpadPreferenceKeys.profileKey(name), null)
            if (savedJson != null) {
                try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    result.add(loaded.copy(isDefault = false, positions = loaded.canonicalPositions()))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode custom profile '$name'. Skipping corrupt entry.", e)
                }
            }
        }

        // 3. Apply custom ordering if saved (deduplicating and filtering unknown names)
        val savedOrderJson = prefs.getString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, null)
        if (savedOrderJson != null) {
            try {
                val orderList = json.decodeFromString<List<String>>(savedOrderJson)
                val profileMap = result.associateBy { it.name.lowercase(Locale.ROOT) }
                val orderedResult = mutableListOf<LayoutProfile>()

                val seenKeys = mutableSetOf<String>()
                for (rawName in orderList) {
                    val normKey = rawName.trim().lowercase(Locale.ROOT)
                    if (seenKeys.add(normKey)) {
                        profileMap[normKey]?.let { orderedResult.add(it) }
                    }
                }

                // Append any unlisted profiles (newly created or defaults not yet in order)
                for (profile in result) {
                    val normKey = profile.name.trim().lowercase(Locale.ROOT)
                    if (seenKeys.add(normKey)) {
                        orderedResult.add(profile)
                    }
                }
                return orderedResult
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode profile order JSON. Falling back to default order.", e)
            }
        }

        return result
    }

    /**
     * Saves custom display ordering of layouts and notifies active observers.
     * Automatically deduplicates entries, normalizes case, and appends any omitted valid profiles.
     */
    fun saveProfileOrder(order: List<String>) {
        synchronized(lock) {
            val allProfiles = getAllProfilesInternal()
            val validNameMap = allProfiles.associateBy { it.name.lowercase(Locale.ROOT) }

            val sanitizedOrder = order
                .map { it.trim() }
                .filter { validNameMap.containsKey(it.lowercase(Locale.ROOT)) }
                .distinctBy { it.lowercase(Locale.ROOT) }
                .map { validNameMap[it.lowercase(Locale.ROOT)]!!.name }
                .toMutableList()

            // Append any valid profiles missing from order
            allProfiles.forEach { profile ->
                if (sanitizedOrder.none { it.equals(profile.name, ignoreCase = true) }) {
                    sanitizedOrder.add(profile.name)
                }
            }

            val jsonOrder = json.encodeToString(sanitizedOrder)
            prefs.edit { putString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, jsonOrder) }
            refreshProfilesFlowLocked()
        }
    }

    /**
     * Persists a profile to disk.
     * Validates custom profile names against collisions with default layouts.
     * Returns true on success, false if validation fails.
     */
    fun saveProfile(profile: LayoutProfile, activate: Boolean = false): Boolean {
        val trimmedName = profile.name.trim()

        if (!profile.isDefault) {
            val validation = validateProfileName(trimmedName, currentName = trimmedName)
            if (validation is ProfileNameValidation.Error &&
                !validation.message.contains("already exists", ignoreCase = true)
            ) {
                Log.w(TAG, "Cannot save custom profile '${profile.name}': ${validation.message}")
                return false
            }
        }

        val canonicalProfile = profile.copy(
            name = trimmedName,
            positions = profile.canonicalPositions()
        )
        val jsonString = json.encodeToString(canonicalProfile)

        synchronized(lock) {
            prefs.edit {
                putString(NexpadPreferenceKeys.profileKey(canonicalProfile.name), jsonString)

                if (!canonicalProfile.isDefault) {
                    val rawCustomNames = getCustomProfileNamesInternal()
                    val customNames = HashSet(rawCustomNames)
                    customNames.removeAll { it.trim().equals(canonicalProfile.name, ignoreCase = true) }
                    customNames.add(canonicalProfile.name)
                    putStringSet(NexpadPreferenceKeys.KEY_CUSTOM_PROFILE_NAMES, customNames)
                }
            }
            refreshProfilesFlowLocked()
        }

        if (activate) {
            setActiveProfile(canonicalProfile.name)
        }
        return true
    }

    /**
     * Loads a profile by name.
     * Resolves the canonical profile name first to ensure case-insensitive requests
     * (e.g. "standard elite") correctly retrieve saved customizations.
     */
    fun loadProfile(name: String): LayoutProfile? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null

        // 1. Check default layouts
        val defaultProfile = getDefaultLayoutProfiles().find { it.name.trim().equals(trimmedName, ignoreCase = true) }
        if (defaultProfile != null) {
            val savedJson = prefs.getString(NexpadPreferenceKeys.profileKey(defaultProfile.name), null)
            if (savedJson != null) {
                return try {
                    val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                    val mergedPositions = defaultProfile.canonicalPositions().toMutableMap()
                    loaded.canonicalPositions().forEach { (k, v) ->
                        mergedPositions[k] = v.sanitized()
                    }
                    defaultProfile.copy(
                        isDefault = true,
                        isRgbEnabled = loaded.isRgbEnabled,
                        labelStyle = loaded.labelStyle,
                        positions = mergedPositions
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode customizations for default profile '${defaultProfile.name}'. Falling back to factory default.", e)
                    defaultProfile.copy(positions = defaultProfile.canonicalPositions())
                }
            }
            return defaultProfile.copy(positions = defaultProfile.canonicalPositions())
        }

        // 2. Check custom profiles using canonical name
        val canonicalCustomName = findCanonicalProfileName(trimmedName) ?: trimmedName
        val savedJson = prefs.getString(NexpadPreferenceKeys.profileKey(canonicalCustomName), null)
        if (savedJson != null) {
            return try {
                val loaded = json.decodeFromString<LayoutProfile>(savedJson)
                loaded.copy(isDefault = false, positions = loaded.canonicalPositions())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode custom profile '$canonicalCustomName'.", e)
                null
            }
        }
        return null
    }

    /**
     * Deletes a custom profile.
     * Default layouts (1 to 6) CANNOT be deleted. Returns false if default or not found.
     */
    fun deleteProfile(name: String): Boolean {
        val trimmedName = name.trim()
        val isDefault = DefaultProfileType.isDefaultProfile(trimmedName) ||
            getDefaultLayoutProfiles().any { it.name.trim().equals(trimmedName, ignoreCase = true) }
        if (isDefault) {
            Log.w(TAG, "Cannot delete protected default layout '$trimmedName'.")
            return false
        }

        val removed = synchronized(lock) {
            val rawCustomNames = getCustomProfileNamesInternal()
            val customNames = HashSet(rawCustomNames)
            val wasRemoved = customNames.removeAll { it.trim().equals(trimmedName, ignoreCase = true) }

            prefs.edit {
                putStringSet(NexpadPreferenceKeys.KEY_CUSTOM_PROFILE_NAMES, customNames)
                remove(NexpadPreferenceKeys.profileKey(trimmedName))
                remove(NexpadPreferenceKeys.profileKey(name))

                prefs.all.keys.forEach { key ->
                    if (NexpadPreferenceKeys.isProfileKey(key) &&
                        NexpadPreferenceKeys.extractProfileName(key).equals(trimmedName, ignoreCase = true)
                    ) {
                        remove(key)
                    }
                }

                val savedOrderJson = prefs.getString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, null)
                if (savedOrderJson != null) {
                    try {
                        val orderList = json.decodeFromString<List<String>>(savedOrderJson).toMutableList()
                        if (orderList.removeAll { it.trim().equals(trimmedName, ignoreCase = true) }) {
                            putString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, json.encodeToString(orderList))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating profile order during delete", e)
                    }
                }
            }
            refreshProfilesFlowLocked()
            wasRemoved
        }

        // If the deleted profile was active, switch to Default 1 (Standard Elite)
        val activeName = prefs.getString(NexpadPreferenceKeys.KEY_ACTIVE_PROFILE, DefaultProfileType.DEFAULT.profileName)
        if (activeName == null || activeName.trim().equals(trimmedName, ignoreCase = true)) {
            setActiveProfile(DefaultProfileType.DEFAULT.profileName)
        }

        return removed
    }

    /**
     * Renames a custom layout profile.
     * Default layouts (1 to 6) cannot be renamed.
     * Rejects empty names, collisions with defaults, or collisions with other custom layouts.
     * Returns true on success, false if invalid or not found.
     */
    fun renameProfile(oldName: String, newName: String): Boolean {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isEmpty()) return false
        if (trimmedOld.equals(trimmedNew, ignoreCase = false)) return true

        val isDefault = DefaultProfileType.isDefaultProfile(trimmedOld) ||
            getDefaultLayoutProfiles().any { it.name.trim().equals(trimmedOld, ignoreCase = true) }
        if (isDefault) {
            Log.w(TAG, "Cannot rename protected default profile '$trimmedOld'.")
            return false
        }

        val validation = validateProfileName(trimmedNew, currentName = trimmedOld)
        if (validation is ProfileNameValidation.Error) {
            Log.w(TAG, "Cannot rename profile to '$trimmedNew': ${validation.message}")
            return false
        }

        val existing = loadProfile(trimmedOld) ?: return false
        val wasActive = getActiveProfile().name.trim().equals(trimmedOld, ignoreCase = true)

        synchronized(lock) {
            val rawCustomNames = getCustomProfileNamesInternal()
            val customNames = HashSet(rawCustomNames)
            customNames.removeAll { it.trim().equals(trimmedOld, ignoreCase = true) }
            customNames.add(trimmedNew)

            val renamed = existing.copy(name = trimmedNew, isDefault = false)
            val canonicalProfile = renamed.copy(positions = renamed.canonicalPositions())
            val jsonString = json.encodeToString(canonicalProfile)

            prefs.edit {
                putStringSet(NexpadPreferenceKeys.KEY_CUSTOM_PROFILE_NAMES, customNames)
                remove(NexpadPreferenceKeys.profileKey(trimmedOld))
                remove(NexpadPreferenceKeys.profileKey(oldName))
                putString(NexpadPreferenceKeys.profileKey(trimmedNew), jsonString)

                prefs.all.keys.forEach { key ->
                    if (NexpadPreferenceKeys.isProfileKey(key) &&
                        NexpadPreferenceKeys.extractProfileName(key).equals(trimmedOld, ignoreCase = true)
                    ) {
                        remove(key)
                    }
                }

                // Update saved profile_order if present
                val savedOrderJson = prefs.getString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, null)
                if (savedOrderJson != null) {
                    try {
                        val orderList = json.decodeFromString<List<String>>(savedOrderJson).toMutableList()
                        val idx = orderList.indexOfFirst { it.trim().equals(trimmedOld, ignoreCase = true) }
                        if (idx != -1) {
                            orderList[idx] = trimmedNew
                            putString(NexpadPreferenceKeys.KEY_PROFILE_ORDER, json.encodeToString(orderList))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating profile order during rename", e)
                    }
                }
            }
            refreshProfilesFlowLocked()
        }

        if (wasActive) {
            setActiveProfile(trimmedNew)
        }
        return true
    }

    /** Reset a default layout to its original factory coordinates. */
    fun resetDefaultProfile(name: String): LayoutProfile? {
        val trimmedName = name.trim()
        val factoryDefault = getDefaultLayoutProfiles().find { it.name.trim().equals(trimmedName, ignoreCase = true) } ?: return null
        synchronized(lock) {
            prefs.edit {
                remove(NexpadPreferenceKeys.profileKey(factoryDefault.name))
                remove(NexpadPreferenceKeys.profileKey(trimmedName))
                prefs.all.keys.forEach { key ->
                    if (NexpadPreferenceKeys.isProfileKey(key) &&
                        NexpadPreferenceKeys.extractProfileName(key).equals(trimmedName, ignoreCase = true)
                    ) {
                        remove(key)
                    }
                }
            }
            refreshProfilesFlowLocked()
        }
        return factoryDefault
    }

    /** Create a new custom profile from a base profile and optional subset of buttons. */
    fun createCustomProfile(
        name: String,
        baseProfile: LayoutProfile,
        selectedButtons: Set<String>? = null,
        activate: Boolean = false
    ): LayoutProfile {
        val trimmed = name.trim()
        val baseCanonical = baseProfile.canonicalPositions()
        val positions = if (selectedButtons != null) {
            val selectedCanonical = selectedButtons.map {
                ControlKey.fromIdentifier(it)?.key ?: it.uppercase(Locale.ROOT)
            }.toSet()
            baseCanonical.filterKeys { k ->
                val can = ControlKey.fromIdentifier(k)?.key ?: k.uppercase(Locale.ROOT)
                selectedCanonical.contains(can)
            }
        } else {
            baseCanonical
        }

        val newProfile = LayoutProfile(
            name = trimmed,
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

    /**
     * Set the active layout profile.
     * Validates that the requested profile exists before persisting.
     * Persists the exact canonical casing to SharedPreferences and publishes to [activeProfileNameFlow].
     * Returns true on success, false if the requested profile was not found.
     */
    fun setActiveProfile(name: String): Boolean {
        val trimmed = name.trim()
        synchronized(lock) {
            val all = getAllProfiles()
            val target = all.find { it.name.trim().equals(trimmed, ignoreCase = true) }
            if (target == null) {
                Log.w(TAG, "Cannot activate layout '$name': profile not found.")
                return false
            }
            val canonicalName = target.name
            prefs.edit { putString(NexpadPreferenceKeys.KEY_ACTIVE_PROFILE, canonicalName) }
            _activeProfileNameFlow.value = canonicalName
            return true
        }
    }

    /** Returns currently active profile. */
    fun getActiveProfile(): LayoutProfile {
        val activeName = prefs.getString(NexpadPreferenceKeys.KEY_ACTIVE_PROFILE, DefaultProfileType.DEFAULT.profileName)
        val effectiveName = DefaultProfileType.resolveCanonicalName(activeName)

        val all = cachedProfiles ?: _profilesFlow.value.ifEmpty { getAllProfiles() }
        return all.find { it.name.trim().equals(effectiveName, ignoreCase = true) }
            ?: all.firstOrNull()
            ?: LayoutProfile(name = effectiveName)
    }

    /** Updates touchpad sensitivity preferences via centralized NexpadPreferences repository. */
    fun updateTouchpadSensitivity(key: String, sens: Float) {
        com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(appContext).setTouchpadSensitivity(key, sens)
    }

    /** Updates touchpad response curve preferences (POWER vs LINEAR) via centralized repository. */
    fun updateTouchpadCurve(key: String, curveId: String) {
        val curve = com.sanket.tools.nexpad.model.TouchpadCurve.fromId(curveId)
        com.sanket.tools.nexpad.storage.NexpadPreferences.getInstance(appContext).setTouchpadCurve(key, curve)
    }

    companion object {
        private const val TAG = "NexpadLayoutManager"
        const val MAX_PROFILE_NAME_LENGTH = 32
    }
}
