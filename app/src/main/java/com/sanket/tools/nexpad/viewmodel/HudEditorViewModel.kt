package com.sanket.tools.nexpad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanket.tools.nexpad.model.ControlCategory
import com.sanket.tools.nexpad.model.GamepadControl
import com.sanket.tools.nexpad.model.HudElement
import com.sanket.tools.nexpad.model.LayoutProfile
import com.sanket.tools.nexpad.model.LayoutSkin
import com.sanket.tools.nexpad.model.LayoutTransform
import com.sanket.tools.nexpad.model.defaultPositions
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
 * Manages active profile, element transforms, strict category-safe skin binding,
 * and asynchronous persistence off the main thread.
 */
class HudEditorViewModel(
    private val layoutManager: LayoutManager,
    private val componentRegistry: ComponentRegistry
) : ViewModel() {

    private val _currentProfile = MutableStateFlow(layoutManager.getActiveProfile())
    val currentProfile: StateFlow<LayoutProfile> = _currentProfile.asStateFlow()

    private val _elements = MutableStateFlow<Map<GamepadControl, HudElement>>(emptyMap())
    val elements: StateFlow<Map<GamepadControl, HudElement>> = _elements.asStateFlow()

    private val _selectedControl = MutableStateFlow<GamepadControl?>(null)
    val selectedControl: StateFlow<GamepadControl?> = _selectedControl.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    /**
     * Category-safe list of compatible skins for the currently selected control.
     * Prevents cross-contamination (e.g. analog stick skin on a face button).
     */
    val compatibleSkins: StateFlow<List<LayoutSkin>> = combine(
        _selectedControl,
        componentRegistry.installedComponents
    ) { selected, allComponents ->
        if (selected == null) return@combine emptyList()

        val skins = mutableListOf<LayoutSkin>(LayoutSkin.NativeDefault)
        val expectedCategory = when (selected.category) {
            ControlCategory.BUTTON -> "BUTTON"
            ControlCategory.JOYSTICK -> "JOYSTICK"
            ControlCategory.TRIGGER -> "TRIGGER"
            ControlCategory.BUMPER -> "BUMPER"
            ControlCategory.DPAD -> "DPAD"
            ControlCategory.HOME -> "HOME"
            ControlCategory.SYSTEM -> "SYSTEM"
            ControlCategory.MACRO -> "MACRO"
        }

        allComponents.filter { def ->
            // Exclude default native components because LayoutSkin.NativeDefault already represents them
            if (def.manifest.id.startsWith("builtin.default_")) return@filter false
            val cat = def.manifest.category.uppercase()
            val defaultCtrl = def.manifest.defaultControl.uppercase()
            cat == expectedCategory || defaultCtrl == selected.key
        }.forEach {
            skins.add(LayoutSkin.CustomComponent(it))
        }

        skins
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadActiveProfile()
    }

    fun loadActiveProfile() {
        val profile = layoutManager.getActiveProfile()
        _currentProfile.value = profile

        val elementMap = mutableMapOf<GamepadControl, HudElement>()
        profile.positions.forEach { (key, pos) ->
            val control = GamepadControl.fromKey(key)
            if (control != null) {
                elementMap[control] = HudElement(
                    control = control,
                    transform = LayoutTransform(
                        xRatio = pos.xRatio,
                        yRatio = pos.yRatio,
                        scale = pos.scale,
                        opacity = pos.opacity
                    ),
                    skinId = pos.customComponentId
                )
            }
        }
        _elements.value = elementMap
        _hasUnsavedChanges.value = false

        // Consume any pending selected control requested by Button Studio
        layoutManager.pendingSelectedKey?.let { key ->
            layoutManager.pendingSelectedKey = null
            val target = GamepadControl.fromKey(key)
            if (target != null) {
                // If element is not yet placed, auto-add it with default position
                if (!elementMap.containsKey(target)) {
                    addControl(target)
                }
                _selectedControl.value = target
            }
        }
    }

    fun selectControl(control: GamepadControl?) {
        _selectedControl.value = control
    }

    fun updateTransform(
        control: GamepadControl,
        xRatio: Float,
        yRatio: Float,
        scale: Float? = null,
        opacity: Float? = null
    ) {
        val current = _elements.value[control] ?: return
        val updated = current.copy(
            transform = current.transform.copy(
                xRatio = xRatio.coerceIn(0.0f, 1.0f),
                yRatio = yRatio.coerceIn(0.0f, 1.0f),
                scale = scale?.coerceIn(0.5f, 2.5f) ?: current.transform.scale,
                opacity = opacity?.coerceIn(0.1f, 1.0f) ?: current.transform.opacity
            )
        )
        _elements.value = _elements.value + (control to updated)
        _hasUnsavedChanges.value = true
    }

    fun nudge(control: GamepadControl, dxRatio: Float, dyRatio: Float) {
        val current = _elements.value[control] ?: return
        val newX = (current.transform.xRatio + dxRatio).coerceIn(0.0f, 1.0f)
        val newY = (current.transform.yRatio + dyRatio).coerceIn(0.0f, 1.0f)
        updateTransform(control, newX, newY)
    }

    fun setScale(control: GamepadControl, newScale: Float) {
        val current = _elements.value[control] ?: return
        updateTransform(control, current.transform.xRatio, current.transform.yRatio, scale = newScale)
    }

    fun setOpacity(control: GamepadControl, newOpacity: Float) {
        val current = _elements.value[control] ?: return
        updateTransform(control, current.transform.xRatio, current.transform.yRatio, opacity = newOpacity)
    }

    fun setSkin(control: GamepadControl, skinId: String?) {
        val current = _elements.value[control] ?: return
        val updated = current.copy(skinId = skinId)
        _elements.value = _elements.value + (control to updated)
        _hasUnsavedChanges.value = true
    }

    fun cycleNextSkin(control: GamepadControl) {
        val available = compatibleSkins.value
        if (available.isEmpty()) return

        val currentSkinId = _elements.value[control]?.skinId
        val currentIndex = available.indexOfFirst {
            when (it) {
                is LayoutSkin.NativeDefault -> currentSkinId == null
                is LayoutSkin.CustomComponent -> it.def.manifest.id == currentSkinId
            }
        }

        val nextIndex = (currentIndex + 1) % available.size
        val nextSkin = available[nextIndex]
        val newSkinId = when (nextSkin) {
            is LayoutSkin.NativeDefault -> null
            is LayoutSkin.CustomComponent -> nextSkin.def.manifest.id
        }
        setSkin(control, newSkinId)
    }

    fun addControl(control: GamepadControl, skinId: String? = null) {
        val defPos = defaultPositions()[control.key]
        val transform = LayoutTransform(
            xRatio = defPos?.xRatio ?: 0.5f,
            yRatio = defPos?.yRatio ?: 0.5f,
            scale = defPos?.scale ?: 1.0f,
            opacity = defPos?.opacity ?: 1.0f
        )
        val element = HudElement(control = control, transform = transform, skinId = skinId)
        _elements.value = _elements.value + (control to element)
        _selectedControl.value = control
        _hasUnsavedChanges.value = true
    }

    fun removeControl(control: GamepadControl) {
        if (_elements.value.containsKey(control)) {
            _elements.value = _elements.value - control
            if (_selectedControl.value == control) {
                _selectedControl.value = null
            }
            _hasUnsavedChanges.value = true
        }
    }

    fun resetControlToDefault(control: GamepadControl) {
        val defPos = defaultPositions()[control.key] ?: return
        val current = _elements.value[control]
        val updated = HudElement(
            control = control,
            transform = LayoutTransform(
                xRatio = defPos.xRatio,
                yRatio = defPos.yRatio,
                scale = defPos.scale,
                opacity = defPos.opacity
            ),
            skinId = current?.skinId
        )
        _elements.value = _elements.value + (control to updated)
        _hasUnsavedChanges.value = true
    }

    fun restoreAllDefaultButtons() {
        val defaultMap = defaultPositions()
        val elementMap = mutableMapOf<GamepadControl, HudElement>()
        defaultMap.forEach { (key, pos) ->
            val control = GamepadControl.fromKey(key)
            if (control != null) {
                elementMap[control] = HudElement(
                    control = control,
                    transform = LayoutTransform(
                        xRatio = pos.xRatio,
                        yRatio = pos.yRatio,
                        scale = pos.scale,
                        opacity = pos.opacity
                    ),
                    skinId = _elements.value[control]?.skinId
                )
            }
        }
        _elements.value = elementMap
        _hasUnsavedChanges.value = true
    }

    /**
     * Persists layout changes asynchronously to SharedPreferences on Dispatchers.IO.
     */
    fun saveProfile(onSaved: () -> Unit = {}) {
        val profile = _currentProfile.value
        val positionMap = _elements.value.values.associate { it.control.key to it.toPosition() }
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
    private val componentRegistry: ComponentRegistry
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HudEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HudEditorViewModel(layoutManager, componentRegistry) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

