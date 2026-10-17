package com.sanket.tools.nexpad

import android.content.Context
import android.content.SharedPreferences
import com.sanket.tools.nexpad.model.DefaultProfileType
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.utils.LayoutManager
import com.sanket.tools.nexpad.utils.ProfileNameValidation
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

class LayoutManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var layoutManager: LayoutManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        val dummyContext = object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = fakePrefs
        }

        layoutManager = LayoutManager(dummyContext, prefsOverride = fakePrefs)
    }

    // ── 1. Name Validation Tests ─────────────────────────────────────────────

    @Test
    fun testValidateProfileName_rejectsBlankAndWhitespace() {
        val blankResult = layoutManager.validateProfileName("")
        assertTrue(blankResult is ProfileNameValidation.Error)
        assertEquals("Profile name cannot be blank.", (blankResult as ProfileNameValidation.Error).message)

        val whitespaceResult = layoutManager.validateProfileName("    ")
        assertTrue(whitespaceResult is ProfileNameValidation.Error)
    }

    @Test
    fun testValidateProfileName_rejectsExceedingLength() {
        val longName = "A".repeat(33)
        val result = layoutManager.validateProfileName(longName)
        assertTrue(result is ProfileNameValidation.Error)
        assertTrue((result as ProfileNameValidation.Error).message.contains("cannot exceed 32 characters"))
    }

    @Test
    fun testValidateProfileName_rejectsControlCharacters() {
        val newlineResult = layoutManager.validateProfileName("Claw\nLayout")
        assertTrue(newlineResult is ProfileNameValidation.Error)

        val tabResult = layoutManager.validateProfileName("Claw\tLayout")
        assertTrue(tabResult is ProfileNameValidation.Error)
    }

    @Test
    fun testValidateProfileName_rejectsDefaultCollisions() {
        // Exact and case-insensitive matching
        val defaultCollision = layoutManager.validateProfileName("Standard Elite")
        assertTrue(defaultCollision is ProfileNameValidation.Error)
        assertTrue((defaultCollision as ProfileNameValidation.Error).message.contains("reserved default layout name"))

        val caseInsensitive = layoutManager.validateProfileName("standard elite")
        assertTrue(caseInsensitive is ProfileNameValidation.Error)

        val dualSense = layoutManager.validateProfileName("playstation dualsense pro")
        assertTrue(dualSense is ProfileNameValidation.Error)
    }

    @Test
    fun testValidateProfileName_rejectsCustomCollisions() {
        // Create custom profile
        val base = layoutManager.getAllProfiles().first()
        layoutManager.createCustomProfile("My Custom Claw", base)

        val collision = layoutManager.validateProfileName("My Custom Claw")
        assertTrue(collision is ProfileNameValidation.Error)
        assertTrue((collision as ProfileNameValidation.Error).message.contains("already exists"))

        // Case-insensitive collision
        val collisionCase = layoutManager.validateProfileName("my custom claw")
        assertTrue(collisionCase is ProfileNameValidation.Error)

        // Same name allowed when renaming itself
        val selfRename = layoutManager.validateProfileName("My Custom Claw", currentName = "my custom claw")
        assertTrue(selfRename is ProfileNameValidation.Valid)
    }

    // ── 2. Rename & Save Safety Tests ────────────────────────────────────────

    @Test
    fun testRenameProfile_rejectsDefaultProfileRename() {
        val result = layoutManager.renameProfile("Standard Elite", "Super Elite")
        assertFalse("Default layouts must never be renamed", result)
    }

    @Test
    fun testRenameProfile_preventsDestinationCollision() {
        val base = layoutManager.getAllProfiles().first()
        layoutManager.createCustomProfile("Layout Alpha", base)
        layoutManager.createCustomProfile("Layout Beta", base)

        // Attempt to rename Alpha to Beta
        val collisionRename = layoutManager.renameProfile("Layout Alpha", "Layout Beta")
        assertFalse("Renaming to an existing profile name must be rejected", collisionRename)

        // Verify Layout Alpha still exists and was not corrupted
        assertNotNull(layoutManager.loadProfile("Layout Alpha"))
        assertNotNull(layoutManager.loadProfile("Layout Beta"))
    }

    @Test
    fun testSaveProfile_rejectsSavingCustomNamedAfterDefault() {
        val imposterProfile = LayoutProfile(
            name = "Standard Elite",
            isDefault = false,
            positions = emptyMap()
        )
        val saved = layoutManager.saveProfile(imposterProfile)
        assertFalse("Custom profile cannot overwrite default layout name", saved)
    }

    // ── 3. Active Profile Consistency Tests ──────────────────────────────────

    @Test
    fun testSetActiveProfile_validatesExistenceAndEmitsCanonicalName() {
        val initialActive = layoutManager.activeProfileNameFlow.value
        assertEquals("Standard Elite", initialActive)

        // Activating lowercase default should succeed and publish exact canonical name
        val success = layoutManager.setActiveProfile("standard elite")
        assertTrue(success)
        assertEquals("Standard Elite", layoutManager.activeProfileNameFlow.value)

        // Activating non-existent profile must fail and leave flow unchanged
        val fail = layoutManager.setActiveProfile("NonExistentProfile123")
        assertFalse(fail)
        assertEquals("Standard Elite", layoutManager.activeProfileNameFlow.value)
    }

    // ── 4. Case-Insensitive Canonical Resolution Tests ───────────────────────

    @Test
    fun testLoadProfile_caseInsensitiveCanonicalResolution() {
        // Load default layout with lowercase request
        val loaded = layoutManager.loadProfile("standard elite")
        assertNotNull("Case-insensitive lookup must succeed", loaded)
        assertEquals("Standard Elite", loaded!!.name)
        assertTrue(loaded.isDefault)

        // Create custom layout with mixed case and load with different case
        val base = layoutManager.getAllProfiles().first()
        layoutManager.createCustomProfile("ApexProClaw", base)

        val loadedCustom = layoutManager.loadProfile("apexproclaw")
        assertNotNull(loadedCustom)
        assertEquals("ApexProClaw", loadedCustom!!.name)
    }

    // ── 5. Profile Order Hardening Tests ─────────────────────────────────────

    @Test
    fun testSaveProfileOrder_deduplicatesAndSanitizes() {
        val base = layoutManager.getAllProfiles().first()
        layoutManager.createCustomProfile("Custom One", base)

        // Input contains duplicates and unknown profile names
        val messyOrder = listOf(
            "Custom One",
            "custom one",
            "Unknown Layout X",
            "Standard Elite",
            "STANDARD ELITE"
        )

        layoutManager.saveProfileOrder(messyOrder)

        val allProfiles = layoutManager.getAllProfiles()
        val names = allProfiles.map { it.name }

        // Must contain no duplicates
        assertEquals(names.size, names.distinct().size)

        // Custom One must appear before Standard Elite per the order
        val idxCustom = names.indexOf("Custom One")
        val idxStandard = names.indexOf("Standard Elite")
        assertTrue(idxCustom != -1 && idxStandard != -1)
        assertTrue("Custom One should precede Standard Elite", idxCustom < idxStandard)
    }

    // ── 6. Position Sanitization Tests ───────────────────────────────────────

    @Test
    fun testPositionSanitization_cleansNaNAndInfinity() {
        val dirtyPosition = Position(
            xRatio = Float.NaN,
            yRatio = Float.POSITIVE_INFINITY,
            scale = -5.0f,
            opacity = 100.0f
        )
        val clean = dirtyPosition.sanitized()

        assertTrue(clean.xRatio.isFinite())
        assertEquals(0.5f, clean.xRatio, 0.001f) // NaN fallback to 0.5f

        assertTrue(clean.yRatio.isFinite())
        assertEquals(0.5f, clean.yRatio, 0.001f) // Infinity fallback to 0.5f

        assertEquals(0.2f, clean.scale, 0.001f) // Clamped to min 0.2f
        assertEquals(1.0f, clean.opacity, 0.001f) // Clamped to max 1.0f
    }

    // ── 7. Delete Protection Tests ───────────────────────────────────────────

    @Test
    fun testDeleteProfile_protectsDefaults() {
        val deleted = layoutManager.deleteProfile("Standard Elite")
        assertFalse("Default profiles cannot be deleted", deleted)
        assertNotNull(layoutManager.loadProfile("Standard Elite"))
    }
}

/**
 * Lightweight, thread-safe in-memory SharedPreferences implementation for unit testing.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = ConcurrentHashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String, defValue: String?): String? =
        (data[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
        val set = data[key] as? Set<String>
        return set?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int =
        (data[key] as? Int) ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        (data[key] as? Long) ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        (data[key] as? Float) ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        (data[key] as? Boolean) ?: defValue

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        private val staging = HashMap<String, Any?>()
        private val toRemove = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
            staging[key] = values?.toSet()
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            staging[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            toRemove.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) {
                prefs.data.clear()
            }
            toRemove.forEach { prefs.data.remove(it) }
            staging.forEach { (k, v) ->
                if (v == null) prefs.data.remove(k) else prefs.data[k] = v
            }
        }
    }
}
