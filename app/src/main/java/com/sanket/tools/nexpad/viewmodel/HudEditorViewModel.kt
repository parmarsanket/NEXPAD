package com.sanket.tools.nexpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.category.CategoryManager
import com.sanket.tools.nexpad.category.ControlKey
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.model.Position
import com.sanket.tools.nexpad.model.defaultPositions
import com.sanket.tools.nexpad.runtime.plugin.RemoteComponentRegistry
import com.sanket.tools.nexpad.runtime.registry.ComponentRegistry
import com.sanket.tools.nexpad.utils.LayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _elements = MutableStateFlow<Map<String, HudElement>>(emptyMap())
    val elements: StateFlow<Map<String, HudElement>> = _elements.asStateFlow()

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
        targetControlKey: String
    ): Boolean {
        val targetSpec = CategoryManager.getControl(targetControlKey) ?: return false

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
        val skins = mutableListOf<LayoutSkin>(LayoutSkin.NativeDefault)
        val seenIds = mutableSetOf<String>()

        val allComponents = componentRegistry.installedComponents.value
        val remoteDocs = remoteComponentRegistry?.loadedComponents?.value ?: emptyList()

        // 1. Tier 2: User-imported or remote .nxprc skins
        remoteDocs.forEach { doc ->
            if (isSkinCompatible(doc.manifest.defaultControl, doc.manifest.category, doc.manifest.id, controlKey)) {
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

            if (isSkinCompatible(def.manifest.defaultControl, def.manifest.category, id, controlKey)) {
                if (seenIds.add(id)) {
                    skins.add(LayoutSkin.CustomComponent(def))
                }
            }
        }

        return skins
    }

    val compatibleSkins: StateFlow<List<LayoutSkin>> = combine(
        _selectedControl,
        componentRegistry.installedComponents,
        remoteComponentRegistry?.loadedComponents ?: MutableStateFlow(emptyList())
    ) { selected, _, _ ->
        if (selected == null) emptyList()
        else getCompatibleSkins(selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadActiveProfile()
    }

    fun loadActiveProfile() {
        val profile = layoutManager.getActiveProfile()
        _currentProfile.value = profile

        val elementMap = mutableMapOf<String, HudElement>()
        profile.canonicalPositions().forEach { (key, pos) ->
            val canonicalKey = ControlKey.fromIdentifier(key)?.key ?: key.uppercase()
            if (!elementMap.containsKey(canonicalKey)) {
                elementMap[canonicalKey] = HudElement.fromPosition(canonicalKey, pos)
            }
        }
        _elements.value = elementMap
        _hasUnsavedChanges.value = false
    }

    /**
     * Load a specific profile by name.
     * Used when HudEditorScreen is opened from VirtualControllerScreen with an explicit profileName
     * in the ScreenKey — ensures the correct layout is always loaded regardless of which profile
     * is currently "active" in LayoutManager.
     */
    fun loadProfileByName(name: String) {
        val profile = layoutManager.getAllProfiles().find { it.name == name }
        if (profile != null) {
            // Activate it so subsequent saves write to the right profile
            layoutManager.setActiveProfile(profile.name)
        }
        // Reload (now with the correct active profile)
        loadActiveProfile()
    }

    fun selectControl(controlKey: String?) {
        if (controlKey == null) {
            _selectedControl.value = null
            return
        }
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonical = targetCtrl?.key ?: controlKey.uppercase()
        val matchingKey = _elements.value.keys.firstOrNull {
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
        val current = _elements.value[key] ?: return
        val updated = current.copy(
            transform = current.transform.copy(
                xRatio = xRatio.coerceIn(0.0f, 1.0f),
                yRatio = yRatio.coerceIn(0.0f, 1.0f),
                scale = scale?.coerceIn(0.5f, 2.5f) ?: current.transform.scale,
                opacity = opacity?.coerceIn(0.1f, 1.0f) ?: current.transform.opacity
            )
        )
        _elements.value = _elements.value + (key to updated)
        _hasUnsavedChanges.value = true
    }

    fun nudge(controlKey: String, dxRatio: Float, dyRatio: Float) {
        val key = controlKey.uppercase()
        val current = _elements.value[key] ?: return
        val newX = (current.transform.xRatio + dxRatio).coerceIn(0.0f, 1.0f)
        val newY = (current.transform.yRatio + dyRatio).coerceIn(0.0f, 1.0f)
        updateTransform(key, newX, newY)
    }

    fun setScale(controlKey: String, newScale: Float) {
        val key = controlKey.uppercase()
        val current = _elements.value[key] ?: return
        updateTransform(key, current.transform.xRatio, current.transform.yRatio, scale = newScale)
    }

    fun setOpacity(controlKey: String, newOpacity: Float) {
        val key = controlKey.uppercase()
        val current = _elements.value[key] ?: return
        updateTransform(key, current.transform.xRatio, current.transform.yRatio, opacity = newOpacity)
    }

    fun setSkin(controlKey: String, skinId: String?) {
        val key = controlKey.uppercase()
        val current = _elements.value[key] ?: return
        val updated = current.copy(skinId = skinId)
        _elements.value = _elements.value + (key to updated)
        _hasUnsavedChanges.value = true
    }

    fun cycleNextSkin(controlKey: String) {
        val key = controlKey.uppercase()
        val available = getCompatibleSkins(key)
        if (available.isEmpty()) return

        val currentSkinId = _elements.value[key]?.skinId?.takeIf { it.isNotBlank() }
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
        val existingEntry = _elements.value.entries.firstOrNull { (k, _) ->
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

        val defPos = defaultPositions()[canonicalKey] ?: defaultPositions()[controlKey]
        val transform = LayoutTransform(
            xRatio = defPos?.xRatio ?: 0.5f,
            yRatio = defPos?.yRatio ?: 0.5f,
            scale = defPos?.scale ?: 1.0f,
            opacity = defPos?.opacity ?: 1.0f
        )
        val element = HudElement(controlKey = canonicalKey, transform = transform, skinId = skinId)
        _elements.value = _elements.value + (canonicalKey to element)
        _selectedControl.value = canonicalKey
        _hasUnsavedChanges.value = true
    }

    fun removeControl(controlKey: String) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()
        val matchingKeys = _elements.value.keys.filter {
            if (targetCtrl != null) ControlKey.fromIdentifier(it) == targetCtrl
            else it.equals(canonicalKey, ignoreCase = true)
        }
        if (matchingKeys.isNotEmpty()) {
            _elements.value = _elements.value - matchingKeys.toSet()
            if (_selectedControl.value in matchingKeys) {
                _selectedControl.value = null
            }
            _hasUnsavedChanges.value = true
        }
    }

    fun resetControlToDefault(controlKey: String) {
        val targetCtrl = ControlKey.fromIdentifier(controlKey)
        val canonicalKey = targetCtrl?.key ?: controlKey.uppercase()
        val defPos = defaultPositions()[canonicalKey] ?: defaultPositions()[controlKey] ?: return
        val currentEntry = _elements.value.entries.firstOrNull { (k, _) ->
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
        _elements.value = _elements.value + (targetKey to updated)
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
                skinId = _elements.value[canonicalKey]?.skinId
            )
        }
        _elements.value = elementMap
        _hasUnsavedChanges.value = true
    }

    /**
     * Persists layout changes asynchronously to SharedPreferences on Dispatchers.IO.
     */
    fun saveProfile(onSaved: () -> Unit = {}) {
        val profile = _currentProfile.value
        val positionMap = mutableMapOf<String, Position>()
        _elements.value.values.forEach { element ->
            val canonical = ControlKey.fromIdentifier(element.controlKey)?.key ?: element.controlKey.uppercase()
            if (!positionMap.containsKey(canonical)) {
                positionMap[canonical] = element.toPosition()
            }
        }
        val updatedProfile = profile.copy(positions = positionMap)

        viewModelScope.launch(Dispatchers.IO) {
            layoutManager.saveProfile(updatedProfile)
            _currentProfile.value = updatedProfile
            _hasUnsavedChanges.value = false
            launch(Dispatchers.Main) {
                onSaved()
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
        if (modelClass.isAssignableFrom(HudEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HudEditorViewModel(layoutManager, componentRegistry, remoteComponentRegistry) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

