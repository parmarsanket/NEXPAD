package com.sanket.tools.nexpad.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.category.ControllerLabelStyle
import com.sanket.tools.nexpad.category.SubCategoryDefinition
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.model.getControlDefaultPosition
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.utils.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for HUD Layout Editing sessions.
 * Manages active profile, element transforms, dynamic category-safe skin binding,
 * and asynchronous persistence off the main thread.
 *
 * Backed entirely by protocol CategoryManager — zero enum duplication or hardcoded heuristics.
 */
class HudEditorViewModel(
    private val layoutManager: LayoutManager,
    private val componentRegistry: ComponentRegistry,
    private val remoteComponentRegistry: RemoteComponentRegistry? = null
) : ViewModel() {

    private val _currentProfile = MutableStateFlow(layoutManager.getActiveProfile())
    val currentProfile: StateFlow<LayoutProfile> = _currentProfile.asStateFlow()

    val elements = mutableStateMapOf<String, HudElement>()

    private val _selectedControl = MutableStateFlow<String?>(null)
    val selectedControl: StateFlow<String?> = _selectedControl.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    /**
     * Dynamically checks if a component definition is compatible with a given target control key.
     * 100% dynamic — queries the protocol CategoryManager's canonical control resolution.
     * Prevents cross-button leakage (e.g. A on B, LB on RB) without brittle hardcoded button names or heuristics.
     */
    fun isSkinCompatible(
        componentDefaultControl: String,
        componentCategory: String,
        componentId: String,
        targetSpec: SubCategoryDefinition
    ): Boolean {
        // 1. Primary: Match via component defaultControl (resolves keys, aliases, and labels)
        if (componentDefaultControl.isNotBlank()) {
            val resolved = CategoryManager.resolveControl(componentDefaultControl)
            if (resolved != null) {
                return resolved.key == targetSpec.key
            }
        }

        // 2. Secondary: Match via componentId (for default or preset IDs e.g. "builtin.default_lb", "rc.bumper_lb")
        if (componentId.isNotBlank()) {
            val resolved = CategoryManager.resolveControl(componentId)
            if (resolved != null) {
                return resolved.key == targetSpec.key
            }
        }

        // 3. Fallback: Category cluster controls where individual control keys are unassigned (e.g. DPAD)
        if (componentDefaultControl.isBlank() && componentCategory.isNotBlank()) {
            val cat = CategoryManager.getCategory(componentCategory)
            if (cat != null) {
                return cat.type == targetSpec.categoryType
            }
        }

        return false
    }

    fun isSkinCompatible(
        componentDefaultControl: String,
        componentCategory: String,
        componentId: String,
        targetControlKey: String
    ): Boolean {
        if (targetControlKey.equals("GYRO", ignoreCase = true)) {
            return componentDefaultControl.equals("GYRO", ignoreCase = true) ||
                   componentCategory.equals("SPECIAL", ignoreCase = true)
        }
        val targetSpec = CategoryManager.getControl(targetControlKey) ?: return false
        return isSkinCompatible(componentDefaultControl, componentCategory, componentId, targetSpec)
    }

    /**
     * Dynamically retrieves all compatible skins for a specific control without duplicates.
     * Adapts in real-time as users import or delete skins.
     *
     * Guarantee:
     * - Index 0 is ALWAYS LayoutSkin.NativeDefault (the native 3D hardware element).
     * - Any built-in default definition (e.g. "builtin.default_*") is unified under index 0.
     * - Each custom/remote skin appears exactly once.
     */
    fun getCompatibleSkins(controlKey: String): List<LayoutSkin> {
        val targetSpec = CategoryManager.getControl(controlKey)
        if (targetSpec == null) {
            val skins = mutableListOf<LayoutSkin>(LayoutSkin.NativeDefault)
            val seenIds = mutableSetOf<String>()
            val allComponents = componentRegistry.installedComponents.value
            allComponents.forEach { def ->
                if (def.manifest.defaultControl.equals(controlKey, ignoreCase = true) ||
                    def.manifest.category.equals("SPECIAL", ignoreCase = true)) {
                    if (seenIds.add(def.manifest.id)) {
                        skins.add(LayoutSkin.CustomComponent(def))
                    }
                }
            }
            return skins
        }
        val skins = mutableListOf<LayoutSkin>(LayoutSkin.NativeDefault)
        val seenIds = mutableSetOf<String>()

        val allComponents = componentRegistry.installedComponents.value
        val remoteDocs = remoteComponentRegistry?.loadedComponents?.value ?: emptyList()

        // 1. Tier 2: User-imported or remote .nxprc skins
        remoteDocs.forEach { doc ->
            if (isSkinCompatible(doc.manifest.defaultControl, doc.manifest.category, doc.manifest.id, targetSpec)) {
                if (seenIds.add(doc.manifest.id)) {
                    skins.add(LayoutSkin.RemoteComponent(doc))
                }
            }
        }

        // 2. Tier 1: NXP JSON and built-in styled presets (excluding defaults and already-added remote components)
        allComponents.forEach { def ->
            val id = def.manifest.id
            // Builtin defaults (e.g. builtin.default_lb) are unified under LayoutSkin.NativeDefault (index 0)
            if (id.startsWith("builtin.default_")) return@forEach
            if (seenIds.contains(id)) return@forEach

            if (isSkinCompatible(def.manifest.defaultControl, def.manifest.category, id, targetSpec)) {
                if (seenIds.add(id)) {
                    skins.add(LayoutSkin.CustomComponent(def))
                }
            }
        }

        return skins
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val compatibleSkins: StateFlow<List<LayoutSkin>> = _selectedControl.flatMapLatest { selected ->
        if (selected == null) {
            flowOf(emptyList())
        } else {
            combine(
                componentRegistry.installedComponents,
                remoteComponentRegistry?.loadedComponents ?: MutableStateFlow(emptyList())
            ) { _, _ ->
                getCompatibleSkins(selected)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadActiveProfile()
    }

    fun loadActiveProfile() {
        loadProfileInternal(layoutManager.getActiveProfile())
    }

    /**
     * Load a specific profile by name without altering which layout is globally active in LayoutManager.
     * Used when HudEditorScreen is opened for a specific profile (e.g. from VirtualControllerScreen)
     * so that the user can inspect and edit this layout independently while leaving their
     * active gameplay layout untouched.
     */
    fun loadProfileByName(name: String) {
        val profile = layoutManager.getAllProfiles().find { it.name.equals(name, ignoreCase = true) }
            ?: layoutManager.loadProfile(name)
            ?: layoutManager.getActiveProfile()
        loadProfileInternal(profile)
    }

    private fun loadProfileInternal(profile: LayoutProfile) {
        _currentProfile.value = profile

        val elementMap = mutableMapOf<String, HudElement>()
        profile.canonicalPositions().forEach { (key, pos) ->
            val canonicalKey = ControlKey.fromIdentifier(key)?.key ?: key.uppercase()
            if (!elementMap.containsKey(canonicalKey)) {
                elementMap[canonicalKey] = HudElement.fromPosition(canonicalKey, pos)
            }
        }
        elements.clear()
        elements.putAll(elementMap)
        _hasUnsavedChanges.value = false
    }

    fun selectControl(controlKey: String?) {
        if (controlKey == null) {
            _selectedControl.value = null
            return
        }
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonical = targetCtrl?.key ?: controlKey.uppercase()
        val matchingKey = elements.keys.firstOrNull {
            if (targetCtrl != null) ControlKey.fromIdentifier(it) == targetCtrl
            else it.equals(canonical, ignoreCase = true)
        }
        _selectedControl.value = matchingKey ?: canonical
    }

    fun updateTransform(
        controlKey: String,
        xRatio: Float,
        yRatio: Float,
        scale: Float? = null,
        opacity: Float? = null
    ) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val updated = current.copy(
            transform = current.transform.copy(
                xRatio = xRatio.coerceIn(0.0f, 1.0f),
                yRatio = yRatio.coerceIn(0.0f, 1.0f),
                scale = scale?.coerceIn(0.4f, 3.5f) ?: current.transform.scale,
                opacity = opacity?.coerceIn(0.1f, 1.0f) ?: current.transform.opacity
            )
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun nudge(controlKey: String, dxRatio: Float, dyRatio: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val newX = (current.transform.xRatio + dxRatio).coerceIn(0.0f, 1.0f)
        val newY = (current.transform.yRatio + dyRatio).coerceIn(0.0f, 1.0f)
        updateTransform(key, newX, newY)
    }

    fun setScale(controlKey: String, newScale: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        updateTransform(key, current.transform.xRatio, current.transform.yRatio, scale = newScale)
    }

    fun setOpacity(controlKey: String, newOpacity: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        updateTransform(key, current.transform.xRatio, current.transform.yRatio, opacity = newOpacity)
    }

    fun setSensitivity(controlKey: String, newSensitivity: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val clamped = newSensitivity.coerceIn(0.5f, 4.0f)
        val updated = current.copy(
            transform = current.transform.copy(sensitivity = clamped)
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
        layoutManager.updateTouchpadSensitivity(key, clamped)
    }

    fun setHeightScale(controlKey: String, newHeightScale: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val clamped = newHeightScale.coerceIn(0.5f, 2.5f)
        val updated = current.copy(
            transform = current.transform.copy(heightScale = clamped)
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun toggleFlip(controlKey: String) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val updated = current.copy(
            transform = current.transform.copy(isFlipped = !current.transform.isFlipped)
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun toggleLock(controlKey: String) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val newLocked = !current.transform.isLocked
        val newMode = if (newLocked) "LOCKED" else "BOX"
        val updated = current.copy(
            transform = current.transform.copy(
                isLocked = newLocked,
                joystickMode = newMode
            )
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun setLock(controlKey: String, isLocked: Boolean) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val newMode = if (isLocked) "LOCKED" else "BOX"
        val updated = current.copy(
            transform = current.transform.copy(
                isLocked = isLocked,
                joystickMode = newMode
            )
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun setJoystickMode(controlKey: String, mode: String) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val isLocked = (mode == "LOCKED")
        val updated = current.copy(
            transform = current.transform.copy(
                joystickMode = mode,
                isLocked = isLocked
            )
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun setHitboxScale(controlKey: String, scale: Float) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val clamped = scale.coerceIn(1.2f, 3.0f)
        val updated = current.copy(
            transform = current.transform.copy(hitboxScale = clamped)
        )
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun setSkin(controlKey: String, skinId: String?) {
        val key = controlKey.uppercase()
        val current = elements[key] ?: return
        val updated = current.copy(skinId = skinId)
        elements[key] = updated
        _hasUnsavedChanges.value = true
    }

    fun cycleNextSkin(controlKey: String) {
        val key = controlKey.uppercase()
        val available = getCompatibleSkins(key)
        if (available.isEmpty()) return

        val currentSkinId = elements[key]?.skinId?.takeIf { it.isNotBlank() }
        val isCurrentDefault = currentSkinId == null || currentSkinId.startsWith("builtin.default_")

        val currentIndex = if (isCurrentDefault) {
            0
        } else {
            available.indexOfFirst { it.id == currentSkinId }
        }

        val nextIndex = if (currentIndex == -1) {
            // Unknown or deleted skin: cleanly reset to Native Default (0)
            0
        } else {
            (currentIndex + 1) % available.size
        }

        val nextSkin = available[nextIndex]
        setSkin(key, nextSkin.id)
    }

    fun addControl(controlKey: String, skinId: String? = null) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()

        // Check if an element for this canonical control already exists
        val existingEntry = elements.entries.firstOrNull { (k, _) ->
            if (targetCtrl != null) ControlKey.fromIdentifier(k) == targetCtrl
            else k.equals(canonicalKey, ignoreCase = true)
        }

        if (existingEntry != null) {
            // Already present — update skin if provided, but NEVER add duplicate
            if (skinId != null && existingEntry.value.skinId != skinId) {
                setSkin(existingEntry.key, skinId)
            }
            _selectedControl.value = existingEntry.key
            return
        }

        // Industry-standard mutual exclusivity:
        // 1. Integrated 4-Way D-Pad (DPAD) and discrete directional buttons (UP, DOWN, LEFT, RIGHT)
        // cannot coexist on the same gamepad HUD layout.
        var inheritedTransform: LayoutTransform? = null

        if (targetCtrl?.isDpadComposite == true) {
            // Adding composite 4-way D-Pad cross removes any discrete directional buttons
            val toRemove = elements.keys.filter { k ->
                ControlKey.fromIdentifier(k)?.isDpadDiscrete == true
            }
            toRemove.forEach { elements.remove(it) }
        } else if (targetCtrl?.isDpadDiscrete == true) {
            // Adding a discrete directional button removes any composite 4-way D-Pad cross
            val toRemove = elements.keys.filter { k ->
                ControlKey.fromIdentifier(k)?.isDpadComposite == true
            }
            toRemove.forEach { elements.remove(it) }
        }

        // 2. Left Stick (LS) & Left Touchpad (LTP) Mutual Exclusivity:
        // When replacing LS with LTP (or vice-versa), inherit the exact center position, scale,
        // and opacity so the user does not have to manually reposition it.
        if (canonicalKey == ControlKey.LTP.key || targetCtrl == ControlKey.LTP) {
            val lsEntry = elements.entries.firstOrNull {
                ControlKey.fromIdentifier(it.key) == ControlKey.LS || it.key.equals(ControlKey.LS.key, ignoreCase = true)
            }
            if (lsEntry != null) {
                inheritedTransform = lsEntry.value.transform
                elements.remove(lsEntry.key)
            }
        } else if (canonicalKey == ControlKey.LS.key || targetCtrl == ControlKey.LS) {
            val ltpEntry = elements.entries.firstOrNull {
                ControlKey.fromIdentifier(it.key) == ControlKey.LTP || it.key.equals(ControlKey.LTP.key, ignoreCase = true)
            }
            if (ltpEntry != null) {
                inheritedTransform = ltpEntry.value.transform
                elements.remove(ltpEntry.key)
            }
        }

        // 3. Right Stick (RS) & Right Touchpad (RTP) Mutual Exclusivity:
        // When replacing RS with RTP (or vice-versa), inherit the exact center position, scale,
        // and opacity so the user does not have to manually reposition it.
        if (canonicalKey == ControlKey.RTP.key || targetCtrl == ControlKey.RTP) {
            val rsEntry = elements.entries.firstOrNull {
                ControlKey.fromIdentifier(it.key) == ControlKey.RS || it.key.equals(ControlKey.RS.key, ignoreCase = true)
            }
            if (rsEntry != null) {
                inheritedTransform = rsEntry.value.transform
                elements.remove(rsEntry.key)
            }
        } else if (canonicalKey == ControlKey.RS.key || targetCtrl == ControlKey.RS) {
            val rtpEntry = elements.entries.firstOrNull {
                ControlKey.fromIdentifier(it.key) == ControlKey.RTP || it.key.equals(ControlKey.RTP.key, ignoreCase = true)
            }
            if (rtpEntry != null) {
                inheritedTransform = rtpEntry.value.transform
                elements.remove(rtpEntry.key)
            }
        }

        val transform = if (inheritedTransform != null) {
            inheritedTransform.copy()
        } else {
            val defPos = getControlDefaultPosition(canonicalKey) ?: defaultPositions()[controlKey]
            LayoutTransform(
                xRatio = defPos?.xRatio ?: 0.5f,
                yRatio = defPos?.yRatio ?: 0.5f,
                scale = defPos?.scale ?: 1.0f,
                opacity = defPos?.opacity ?: 1.0f
            )
        }

        val element = HudElement(controlKey = canonicalKey, transform = transform, skinId = skinId)
        elements[canonicalKey] = element
        _selectedControl.value = canonicalKey
        _hasUnsavedChanges.value = true
    }

    /**
     * Swaps Left Stick (LS) with Left Touchpad (LTP), or Right Stick (RS) with Right Touchpad (RTP),
     * preserving the exact center position, scale, and opacity without manual repositioning.
     */
    fun swapStickAndTouchpad(controlKey: String) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()
        val targetSwapKey = when (canonicalKey) {
            ControlKey.LS.key -> ControlKey.LTP.key
            ControlKey.LTP.key -> ControlKey.LS.key
            ControlKey.RS.key -> ControlKey.RTP.key
            ControlKey.RTP.key -> ControlKey.RS.key
            else -> return
        }
        addControl(targetSwapKey)
    }

    fun removeControl(controlKey: String) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()
        val matchingKeys = elements.keys.filter {
            if (targetCtrl != null) ControlKey.fromIdentifier(it) == targetCtrl
            else it.equals(canonicalKey, ignoreCase = true)
        }
        if (matchingKeys.isNotEmpty()) {
            matchingKeys.forEach { elements.remove(it) }
            if (_selectedControl.value in matchingKeys) {
                _selectedControl.value = null
            }
            _hasUnsavedChanges.value = true
        }
    }

    fun resetControlToDefault(controlKey: String) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()
        val defPos = getControlDefaultPosition(canonicalKey) ?: defaultPositions()[controlKey] ?: return
        val currentEntry = elements.entries.firstOrNull { (k, _) ->
            if (targetCtrl != null) ControlKey.fromIdentifier(k) == targetCtrl
            else k.equals(canonicalKey, ignoreCase = true)
        }
        val targetKey = currentEntry?.key ?: canonicalKey
        val updated = HudElement(
            controlKey = targetKey,
            transform = LayoutTransform(
                xRatio = defPos.xRatio,
                yRatio = defPos.yRatio,
                scale = defPos.scale,
                opacity = defPos.opacity
            ),
            skinId = currentEntry?.value?.skinId
        )
        elements[targetKey] = updated
        _hasUnsavedChanges.value = true
    }

    fun restoreAllDefaultButtons() {
        val defaultMap = defaultPositions()
        val elementMap = mutableMapOf<String, HudElement>()
        defaultMap.forEach { (key, pos) ->
            val canonicalKey = ControlKey.fromIdentifier(key)?.key ?: key.uppercase()
            elementMap[canonicalKey] = HudElement(
                controlKey = canonicalKey,
                transform = LayoutTransform(
                    xRatio = pos.xRatio,
                    yRatio = pos.yRatio,
                    scale = pos.scale,
                    opacity = pos.opacity
                ),
                skinId = elements[canonicalKey]?.skinId
            )
        }
        elements.clear()
        elements.putAll(elementMap)
        _hasUnsavedChanges.value = true
    }

    /** Updates the button labeling style (Xbox vs PlayStation) for the current layout. */
    fun updateLabelStyle(style: ControllerLabelStyle) {
        _currentProfile.value = _currentProfile.value.copy(labelStyle = style.id)
        _hasUnsavedChanges.value = true
    }

    /**
     * Persists layout changes asynchronously to SharedPreferences on Dispatchers.IO.
     */
    fun saveProfile(onSaved: () -> Unit = {}) {
        val profile = _currentProfile.value
        val elementsSnapshot = elements.values.toList()
        val positionMap = mutableMapOf<String, Position>()
        elementsSnapshot.forEach { element ->
            val canonical = ControlKey.fromIdentifier(element.controlKey)?.key ?: element.controlKey.uppercase()
            if (!positionMap.containsKey(canonical)) {
                positionMap[canonical] = element.toPosition()
            }
        }
        val updatedProfile = profile.copy(positions = positionMap)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wasActive = layoutManager.getActiveProfile().name.equals(updatedProfile.name, ignoreCase = true)
                layoutManager.saveProfile(updatedProfile, activate = wasActive)
                _currentProfile.value = updatedProfile
                _hasUnsavedChanges.value = false
                launch(Dispatchers.Main) {
                    onSaved()
                }
            } catch (e: Exception) {
                android.util.Log.e("HudEditorViewModel", "Failed to save profile: ${e.message}", e)
            }
        }
    }
}

class HudEditorViewModelFactory(
    private val layoutManager: LayoutManager,
    private val componentRegistry: ComponentRegistry,
    private val remoteComponentRegistry: RemoteComponentRegistry? = null
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HudEditorViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return modelClass.cast(
            HudEditorViewModel(layoutManager, componentRegistry, remoteComponentRegistry)
        )!!
    }
}

