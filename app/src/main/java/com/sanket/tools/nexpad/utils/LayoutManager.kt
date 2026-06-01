package com.sanket.tools.nexpad.utils

import android.content.Context
import com.sanket.tools.nexpad.model.LayoutProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LayoutManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("NEXPAD_LAYOUTS_V2", Context.MODE_PRIVATE)

    fun saveProfile(profile: LayoutProfile) {
        val json = Json.encodeToString(profile)
        prefs.edit().putString("profile_${profile.name}", json).apply()
        // Save as active profile
        prefs.edit().putString("active_profile", profile.name).apply()
    }

    fun loadProfile(name: String): LayoutProfile? {
        val json = prefs.getString("profile_$name", null) ?: return null
        return try {
            Json.decodeFromString<LayoutProfile>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun getActiveProfile(): LayoutProfile {
        val activeName = prefs.getString("active_profile", "Standard") ?: "Standard"
        return loadProfile(activeName) ?: LayoutProfile(name = activeName)
    }
}
