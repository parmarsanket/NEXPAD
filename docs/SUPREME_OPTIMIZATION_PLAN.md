# NEXPAD — SUPREME ARCHITECTURE, LOGIC & PERFORMANCE OPTIMIZATION PLAN

> **Branch:** `feature/finaltouch-polish-and-optimization`
> **Status:** PLANNING PHASE — No code changes made yet
> **Rule:** Do not optimize code before optimizing architecture.
> **Rule:** Zero `@Suppress` / `@SuppressLint` anywhere in `app/src/`.

---

## TABLE OF CONTENTS

1. [Architecture Scorecard](#1-architecture-scorecard)
2. [Hot Path Analysis](#2-hot-path-analysis)
3. [State Ownership Map](#3-state-ownership-map)
4. [Data Structure Audit](#4-data-structure-audit)
5. [Algorithm Audit](#5-algorithm-audit)
6. [OOP Refactoring Map](#6-oop-refactoring-map)
7. [Compose Performance Audit](#7-compose-performance-audit)
8. [Network & Input Architecture](#8-network--input-architecture)
9. [Component Resolution Pipeline](#9-component-resolution-pipeline)
10. [HUD Editor Architecture](#10-hud-editor-architecture)
11. [Navigation Flow](#11-navigation-flow)
12. [Sensor & Haptics](#12-sensor--haptics)
13. [Threading & Coroutines](#13-threading--coroutines)
14. [Memory Allocation](#14-memory-allocation)
15. [Persistence & Error Handling](#15-persistence--error-handling)
16. [Testing Strategy](#16-testing-strategy)
17. [Prioritized Implementation Table](#17-prioritized-implementation-table)
18. [Recommended Final Architecture Diagram](#18-recommended-final-architecture-diagram)

---

## 1. ARCHITECTURE SCORECARD

Evaluated across 10 criteria. Scale: 🟢 Good · 🟡 Needs Work · 🔴 Critical Issue

| Area | Score | Key Finding |
|---|---|---|
| State ownership clarity | 🟡 | Multiple ViewModels partially own overlapping state |
| Component resolution | 🟡 | 4-tier fallback spread across UI composables |
| Input hot path | 🔴 | `Pair` allocation on every joystick event; race condition on `sendImmediate` |
| HUD editor mutations | 🔴 | `Map` copy on every single element mutation (O(n) alloc per drag frame) |
| Network architecture | 🔴 | `sendImmediate` + `startTransmitting` race on shared `connection` |
| `@SuppressLint` violations | 🔴 | `GamepadNetworkManager.kt:123` — must fix |
| Compose recomposition scope | 🟡 | `getCompatibleSkins` recomputes on any component change, not only selection |
| ControlKey lookup hot path | 🟡 | `ControlKey.fromIdentifier` called every button press without caching |
| Algorithm complexity | 🟡 | Repeated O(n) scans in HudEditorViewModel, ButtonStudio filter pipeline |
| Code noise | 🟡 | Benchmark `Log` statements in production network code |

---

## 2. HOT PATH ANALYSIS

Hot paths = code executed at high frequency (≥60 Hz or on every user input event).

### 2.1 Joystick Input (200 Hz Wi-Fi / 125 Hz BT)

```
GamepadScreen touch event
  → GamepadViewModel.updateLeftStick(x, y)
      → leftStickState = Pair(x, y)          ← 🔴 Pair allocation every frame
      → networkManager.sendImmediate()        ← 🔴 launches new IO coroutine every call
          → connection.sendInput(inputState)  ← shared with startTransmitting → RACE
```

**Issues:**
- `Pair(x, y)` creates a new heap object at 200 Hz = ~200 allocations/second on this path alone
- `sendImmediate` launches a new coroutine each call on `Dispatchers.IO`
- `startTransmitting` runs on its own `gamepadDispatcher` thread and also calls `connection.sendInput()` — no synchronization, potential corrupt write

### 2.2 Button Input

```
GamepadScreen touch event
  → GamepadViewModel.updateButton(buttonName, isPressed)
      → ControlKey.fromIdentifier(buttonName)  ← repeated string lookup every press
      → applyButtonState(key, isPressed)
      → networkManager.sendImmediate()         ← same race as above
```

**Issues:**
- `ControlKey.fromIdentifier` is called on every press. If this does string matching internally it should be pre-cached per control.

### 2.3 HUD Element Drag (editor)

```
HudCanvas drag event
  → HudEditorViewModel.updateElementPosition(id, offset)
      → _elements.value = _elements.value + (id to updated)  ← 🔴 full Map copy every frame
```

At 60fps drag, this creates 60 new `Map` objects per second holding all elements.

### 2.4 Transmit Loop (GamepadNetworkManager)

```
gamepadDispatcher thread (THREAD_PRIORITY_URGENT_AUDIO)
  → LockSupport.parkNanos(5_000_000)   // 200Hz
  → connection.sendInput(inputState)   ← NO lock
```

This thread shares `connection` with `sendImmediate` on `Dispatchers.IO`. No mutex or `@Volatile`.

---

## 3. STATE OWNERSHIP MAP

### Who owns what — current state

| State | Owner | Consumers | Problem |
|---|---|---|---|
| `leftStickState`, `rightStickState` | `GamepadViewModel` | `GamepadScreen` | `Pair` allocation hot path |
| `connectionState`, `connectionStats` | `GamepadNetworkManager` (flows) | `GamepadViewModel` (re-exposed), `GamepadScreen` | Fine, flow-based |
| `elements: StateFlow<Map<String,HudElement>>` | `HudEditorViewModel` | `HudCanvas`, `HudDockedInspector` | Map copy per mutation |
| `selectedControl`, `selectedAsset` | `NavigationViewModel` | `ButtonStudio`, `HudEditor` | Correct — graph-scoped |
| `isRgbEnabled`, `hapticLevel`, etc. | `SettingsScreen`/SharedPrefs | Multiple screens | SharedPrefs read on UI thread possibly |
| USB receiver | `GamepadViewModel` inline | Self | BroadcastReceiver inline in VM — should be extracted |
| `NxprcSyncServer` | `GamepadNetworkManager.init{}` | — | Started unconditionally in init, never lifecycle-aware |

### Recommended ownership model

```
UI Layer
  └── Reads StateFlow / collectAsState
Domain Layer
  ├── GamepadInputController     ← owns raw input state (zero-alloc)
  ├── GamepadSessionCoordinator  ← owns connection lifecycle
  ├── HudLayoutEditor            ← owns element MutableStateMap (SnapshotStateMap)
  └── ComponentCatalog           ← owns component index, skin compatibility
Infrastructure Layer
  ├── GamepadNetworkManager      ← transport-agnostic sending
  ├── LayoutManager              ← SharedPreferences I/O
  └── SensorController           ← sensor callbacks → flow
```

---

## 4. DATA STRUCTURE AUDIT

### 4.1 `leftStickState`, `rightStickState` — `mutableStateOf(Pair<Float,Float>)`

| Property | Current | Recommended |
|---|---|---|
| Structure | `Pair<Float,Float>` (heap object) | Two separate `mutableFloatStateOf` OR inline `data class StickState(x,y)` in a single `mutableStateOf` that uses structural equality |
| Allocation | New `Pair` every frame on 200 Hz path | Zero new allocation: update two Float states |
| Recomposition | Both x+y update triggers one recompose | Same, but no GC pressure |

**Fix:** Replace with `var leftStickX by mutableFloatStateOf(0f)` + `var leftStickY by mutableFloatStateOf(0f)`, or a stable `data class` with `equals` so Compose skips recompose when values are identical.

### 4.2 `elements: StateFlow<Map<String, HudElement>>`

| Property | Current | Recommended |
|---|---|---|
| Structure | Immutable `Map`, replaced entirely on each mutation | `SnapshotStateMap<String, HudElement>` (Compose's mutable map — granular diff) |
| Mutation cost | O(n) copy per mutation | O(1) update |
| Observer cost | Full flow emission on each mutation | Compose reads only changed keys |

**Fix:** Change `_elements` from `MutableStateFlow<Map<…>>` to a `SnapshotStateMap<String, HudElement>` exposed as a `@Stable` snapshot state. Consumers use `elements[key]` directly — Compose will only recompose the composable reading that specific key.

### 4.3 Component Registry / Catalog

| Property | Current | Recommended |
|---|---|---|
| Lookup structure | `List<NxpComponentDef>` scanned with `first{}` or `filter{}` | `Map<String, NxpComponentDef>` primary index + `Map<Category, List<NxpComponentDef>>` category index |
| Filter on every recompose | Yes — filter + sort inside composable | Pre-built sorted lists per category; filter only on search text change |
| Asset resolution | Spread across multiple composables and renderers | Single `ComponentResolver.resolve(assetId)` function, one canonical lookup chain |

### 4.4 `compatibleSkins` — `combine()` over all components

Current: `combine(selectedControlFlow, installedComponentsFlow, remoteDocsFlow)` → re-runs O(n) scan on any component install/remove.

Recommended: Derive compatible skins only when `selectedControl` changes. Components that are unrelated to the selected control type should not trigger a recomputation.

**Fix:**
```kotlin
val compatibleSkins = selectedControlFlow
    .flatMapLatest { control ->
        installedComponentsFlow.map { components ->
            components.filter { isSkinCompatible(it, control) }
        }
    }
```
This recomputes only when the selected control changes OR when the component list changes — not whenever an unrelated remote doc arrives.

---

## 5. ALGORITHM AUDIT

### 5.1 Component Filtering in ButtonStudio

**Current flow (on every recompose):**
```
allComponents
  → filter(category)
  → filter(type)
  → filter(searchText)
  → sortedBy(name)
```

**Problem:** All four operations run together. Category changes trigger a full re-sort. Search text changes trigger a full category-filter first.

**Recommended:**
```
On startup/component-install:
  ComponentCatalog.buildIndexes()
    → orderedByCategory: Map<Category, List<NxpComponentDef>>  // pre-sorted

On category change:
  activeList = orderedByCategory[selectedCategory]  // O(1) lookup

On search text change:
  filteredList = activeList.filter { it.name.contains(searchText, ignoreCase=true) }  // O(n) on smaller list
```
No re-sorting on search. No re-categorizing on search. Two separate lightweight operations.

### 5.2 Control Lookup in HudEditorViewModel

**Current:** `_elements.value.entries.firstOrNull { it.value.controlKey == key }` — called multiple times in `addControl`.

**Recommended:** If `HudElement` has a unique `controlKey`, use `_elements[controlKey]` directly once the map key is the `controlKey` string (which it appears to be from `key to updated`).

### 5.3 `ControlKey.fromIdentifier` on Every Button Press

**Problem:** Called on every `updateButton()` call (hot path). If `fromIdentifier` does string matching via `values().first { it.identifier == name }`, this is O(n) every press.

**Fix:**
```kotlin
// In GamepadViewModel init:
private val controlKeyCache = ControlKey.values().associateBy { it.identifier }

// In updateButton:
val key = controlKeyCache[buttonName] ?: return
```
This converts O(n) string scan to O(1) map lookup, cached once at VM init.

### 5.4 `isSkinCompatible` — Repeated `CategoryManager` Calls

**Current:** For each component, `isSkinCompatible` calls `CategoryManager.getControl()` then `CategoryManager.resolveControl()` — potentially two separate lookups per component per compatibility check.

**Fix:** Cache the resolved control type for the currently selected `controlKey` once, then pass that resolved type into the filter function. Don't call `CategoryManager` inside the inner loop.

---

## 6. OOP REFACTORING MAP

### 6.1 God Object Analysis

| Class | Lines | Too Many Responsibilities? | Action |
|---|---|---|---|
| `GamepadViewModel` | 368 | USB BroadcastReceiver inline, sensor routing, button/stick dispatch, discovery lifecycle, connection stats | Extract USB receiver to separate `UsbConnectionMonitor`; keep VM as orchestrator |
| `GamepadNetworkManager` | 453 | 5 transport types, signal polling, NxprcSyncServer lifecycle, WifiLock, transmit loop, `sendImmediate` | Split into `GamepadTransmitter` (hot send loop) + `GamepadConnectionManager` (transport switching + lifecycle) |
| `HudEditorViewModel` | 573 | Element CRUD, skin compatibility, layout persistence, profile loading, control mutual-exclusivity logic | Extract `HudCompatibilityService` for skin-compat; keep VM as UI state owner |
| `NavigationViewModel` | 106 | Navigation backstack + asset selection + gamepad session management | Currently well-scoped; minor: gamepad session start/stop could be an event rather than imperative call |
| `ButtonStudioScreen` | 250 | Composable doing category state, component fetch, preview selection, asset commit | Already separated into sub-composables; main improvement is moving filter logic to a `StudioViewModel` |

### 6.2 Recommended Class Responsibilities

```
GamepadViewModel
  Responsibility: UI state for gamepad screen, routing events to correct subsystems
  Owns: leftStickX/Y, rightStickX/Y, isConnected (derived), sensorEnabled
  Does NOT own: connection logic, transmit scheduling, USB detection details

GamepadTransmitter (extract from GamepadNetworkManager)
  Responsibility: High-frequency transmit loop ONLY
  Owns: gamepadDispatcher, transmitInterval, sendInput loop
  Thread: Single dedicated thread, THREAD_PRIORITY_URGENT_AUDIO
  Synchronization: Uses AtomicReference<IGamepadConnection> or mutex for connection swap

GamepadConnectionManager (from GamepadNetworkManager)
  Responsibility: Transport type selection, connect/disconnect lifecycle, WifiLock, NxprcSync
  Owns: active IGamepadConnection, ConnectionType, ConnectionStats
  Exposes: StateFlow<ConnectionState>, StateFlow<ConnectionStats>

UsbConnectionMonitor (extract from GamepadViewModel)
  Responsibility: USB state BroadcastReceiver, emits USB events as Flow
  Owns: receiver registration/unregistration
  Exposes: Flow<UsbEvent>

HudCompatibilityService (extract from HudEditorViewModel)
  Responsibility: Given a ControlKey + component list, return compatible skins
  Pure function, no state
  Testable in isolation

ComponentCatalog (new — consolidates ComponentRegistry + RemoteComponentRegistry)
  Responsibility: Single source of truth for all available components
  Owns: orderedByCategory index, byId index, remote + native merged list
  Exposes: fun resolve(assetId: String): NxpComponentDef?
           fun forCategory(category: Category): List<NxpComponentDef>
```

### 6.3 What NOT to Create

- Do NOT create a `ComponentRepository` (no remote data source needing repository pattern)
- Do NOT create a `ComponentFactory` (plain functions are sufficient for component construction)
- Do NOT wrap `LayoutManager` in a `LayoutRepository` (it's already a simple persistence layer)
- Do NOT add Hilt for DI — manual injection via `ViewModelProvider.Factory` is already in use and sufficient

---

## 7. COMPOSE PERFORMANCE AUDIT

### 7.1 Stability

| Composable | Issue | Fix |
|---|---|---|
| `ControllerElementRenderer` | Takes `NxpComponentDef?` — if this is not `@Stable`, Compose can't skip recompose | Annotate `NxpComponentDef` as `@Immutable` or ensure it's a `data class` |
| `HudCanvas` | Reads `elements: Map<String, HudElement>` from StateFlow | Switch to `SnapshotStateMap` — Compose reads only changed elements |
| `ButtonStudioScreen` | `compatibleSkins` reacts to all component changes | Fix reactivity scope (see §4.4) |
| Sandbox preview | Animates independently inside the studio — correct isolation | Keep as-is |

### 7.2 Recomposition Scope

**Rule:** Every `collectAsState()` at the screen level causes the entire screen to recompose when the flow emits.

**Current risk:**
- `GamepadScreen` likely collects multiple flows at top level (connection state, RGB state, input stats)
- Any emission in any of these causes the top-level composable to recompose, which Compose then tries to skip for stable children

**Fix pattern:** Use `derivedStateOf` or separate sub-composables for each independent state domain:
```kotlin
// Instead of reading all at top level:
val connectionStats by viewModel.connectionStats.collectAsState()
val connectionState by viewModel.connectionState.collectAsState()

// Isolate reads into leaf composables that only care about one state:
@Composable
fun ConnectionBadge(viewModel: GamepadViewModel) {
    val state by viewModel.connectionState.collectAsState()
    // only recomposes when connectionState changes
}
```

### 7.3 Canvas Drawing

- `InbuildTouchpad`: static grid constants are correct — `drawLine` with fixed alpha avoids conditional branching in draw.
- Touch aura uses `animateFloatAsState` which drives invalidation via `canvas.invalidate()` correctly.
- Recommendation: Ensure `drawInbuildTouchpadGrid` is called inside a `Canvas {}` block (not `drawWithContent`) so its reads are properly tracked.

---

## 8. NETWORK & INPUT ARCHITECTURE

### 8.1 Critical Race Condition Fix

**Problem:** Two concurrent writers to `connection`:
1. `startTransmitting()` — on `gamepadDispatcher` (dedicated thread)
2. `sendImmediate()` — on `Dispatchers.IO` (thread pool)

Both call `connection.sendInput(inputState)` with no synchronization.

**Fix options:**

**Option A (Recommended): Route all sends through `gamepadDispatcher`**
```kotlin
// sendImmediate posts to the same single-threaded dispatcher
fun sendImmediate() {
    scope.launch(gamepadDispatcher) {
        connection?.sendInput(inputState.snapshot())
    }
}
```
This eliminates the race by making `gamepadDispatcher` the only writer. Single-threaded means no mutex needed.

**Option B: Mutex**
```kotlin
private val sendMutex = Mutex()

suspend fun sendInput() {
    sendMutex.withLock { connection?.sendInput(inputState) }
}
```
Adds lock overhead on the 200 Hz path — less preferred.

**Recommendation:** Option A. Route `sendImmediate` to `gamepadDispatcher`. The single dedicated thread becomes the sole owner of `connection.sendInput()`.

### 8.2 Connection Swap Safety

When swapping transport: `connection = NewType()` — if `sendImmediate` is on IO and `startTransmitting` is mid-send, the new connection object is visible to both threads without synchronization.

**Fix:** Use `@Volatile var connection: IGamepadConnection?` OR wrap swap in the same `gamepadDispatcher` scope so connection swap and sends are serialized.

### 8.3 Remove `@SuppressLint("MissingPermission")`

**File:** `GamepadNetworkManager.kt:123`

**Fix:** Add proper Bluetooth permission check before calling `getPairedBluetoothDevices()`:
```kotlin
if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        != PackageManager.PERMISSION_GRANTED) {
    return emptyList()
}
```
Then remove the `@SuppressLint`. Permission is already requested at the app level — the check is purely defensive.

### 8.4 Remove Production Log Noise

**File:** `GamepadNetworkManager.kt` — `connect()` function

Remove or gate all `Log.d("BENCHMARK_…")` calls behind `BuildConfig.DEBUG`:
```kotlin
if (BuildConfig.DEBUG) Log.d("BENCHMARK", "…")
```
Or replace with a lightweight `Timber` call if logging infrastructure is added.

### 8.5 `NxprcSyncServer` Lifecycle

Currently started in `GamepadNetworkManager.init{}` unconditionally. Should be started on first connection attempt and stopped when there are no active connections, to avoid holding a socket when not needed.

### 8.6 Signal Polling Interval

Current: 2000ms polling for signal strength. This is fine and does not need to change.

---

## 9. COMPONENT RESOLUTION PIPELINE

### 9.1 Current 4-Tier Fallback (spread across UI)

```
ControllerElementRenderer.kt
  Tier 1: assetId.startsWith("builtin.") → NativeComponentRegistry
  Tier 2: assetId.startsWith("rc.")     → NxpComposeInterpreter / NxprcCanvasRenderer
  Tier 3: assetId == null               → Realistic* fallbacks
  Tier 4: else                          → Geometric placeholder
```

This logic lives inside a UI composable. Every render call re-evaluates this.

### 9.2 Recommended Resolution Pipeline

```
ComponentResolver (pure object/singleton)
  fun resolve(assetId: String?): ResolvedComponent

  internal:
    if (assetId == null) → ResolvedComponent.Default(controlKey)
    else if (assetId starts "builtin.") → NativeComponentRegistry.find(assetId)
                                         ?: ResolvedComponent.GeometricPlaceholder
    else if (assetId starts "rc.")      → RemoteComponentRegistry.find(assetId)
                                         ?: ResolvedComponent.GeometricPlaceholder
    else                                → ResolvedComponent.GeometricPlaceholder
```

**ControllerElementRenderer** becomes:
```kotlin
val resolved = remember(assetId) { ComponentResolver.resolve(assetId) }
when (resolved) {
    is ResolvedComponent.Native   -> NativeRenderer(resolved, ...)
    is ResolvedComponent.Remote   -> RemoteRenderer(resolved, ...)
    is ResolvedComponent.Default  -> RealisticFallback(controlKey, ...)
    is ResolvedComponent.Placeholder -> GeometricPlaceholder(...)
}
```

`remember(assetId)` means resolution is cached per `assetId` — no repeated registry scans per frame.

### 9.3 ComponentCatalog as Single Source of Truth

```kotlin
object ComponentCatalog {
    private val byId: Map<String, NxpComponentDef>
    private val byCategory: Map<Category, List<NxpComponentDef>>

    fun resolve(assetId: String): NxpComponentDef?
    fun forCategory(category: Category): List<NxpComponentDef>
    fun allCategories(): List<Category>

    fun install(def: NxpComponentDef)   // from remote .nxprc
    fun uninstall(assetId: String)
}
```

Both `NativeComponentRegistry` and `RemoteComponentRegistry` feed into `ComponentCatalog`. ButtonStudio, HudEditor, and ControllerElementRenderer all query `ComponentCatalog`. No divergent views of the component list.

---

## 10. HUD EDITOR ARCHITECTURE

### 10.1 Current Issues

1. `Map<String, HudElement>` copied on every drag event → O(n) allocation
2. `compatibleSkins` recomputes on any component install, not just selection change
3. `addControl` mutual exclusivity logic is correct but uses `firstOrNull` scans
4. `isSkinCompatible` calls `CategoryManager` multiple times per component per check

### 10.2 Recommended Changes

#### Switch to `SnapshotStateMap`

```kotlin
// Before
private val _elements = MutableStateFlow<Map<String, HudElement>>(emptyMap())
val elements: StateFlow<Map<String, HudElement>> = _elements

// After
val elements: SnapshotStateMap<String, HudElement> = mutableStateMapOf()
```

Consumers: replace `collectAsState()` with direct `elements` read — Compose handles observation automatically. HudCanvas and HudDockedInspector only recompose when the specific element they read changes.

#### Fix `compatibleSkins` Reactivity

```kotlin
val compatibleSkins: StateFlow<List<NxpComponentDef>> = selectedControlFlow
    .flatMapLatest { control ->
        allComponentsFlow.map { components ->
            val resolvedType = CategoryManager.resolveControl(control)  // cached once per control
            components.filter { ComponentCatalog.isSkinCompatible(it, resolvedType) }
        }
    }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

#### Cache Resolved Control Type in `isSkinCompatible`

```kotlin
// Pass in pre-resolved type instead of calling CategoryManager inside the loop
fun isSkinCompatible(def: NxpComponentDef, resolvedControlType: ControlType): Boolean
```

#### `addControl` Mutual Exclusivity

The existing logic is functionally correct. No algorithmic change needed. Minor: replace `firstOrNull` scans with direct `elements[controlKey]` lookups where `controlKey` is the map key.

---

## 11. NAVIGATION FLOW

### 11.1 Current State

- Route sealed interface with 7 routes — correct
- `NavigationViewModel` graph-scoped — correct
- `typealias ScreenKey = Route` — kept for backward compat

### 11.2 Issues

- No issues with navigation correctness
- `NavigationViewModel` stores `selectedAssetId` and `selectedProfileName` as state — slightly couples navigation state with domain selection. Acceptable at current scale.
- If Button Studio grows to multiple sub-pages, consider a `ButtonStudioViewModel` scoped to the studio backstack entry

### 11.3 Minor Improvement

Ensure `Route.Gamepad` and `Route.ButtonStudio` route arguments are strongly typed (not raw strings) to catch mismatches at compile time. Nav3 supports typed routes — use them consistently.

---

## 12. SENSOR & HAPTICS

### 12.1 `SensorController`

- Currently composed inside `GamepadViewModel` — correct
- Emits accelerometer/gyroscope data as callbacks → passed to network manager
- No issues found

**Recommendation:** Ensure `SensorController` unregisters listeners in `onCleared()` — verify this is the case.

### 12.2 `HapticFeedbackHelper`

- Currently a static utility — correct pattern for haptics
- Verify that haptic calls are dispatched on the main thread (required by Android)
- Do not call `HapticFeedbackHelper` from `gamepadDispatcher` (wrong thread for haptics)

---

## 13. THREADING & COROUTINES

### 13.1 Threading Model (Current)

| Thread / Scope | Purpose | Issue |
|---|---|---|
| `gamepadDispatcher` (single, URGENT_AUDIO) | Transmit loop | Also needs to be sole writer for `sendImmediate` |
| `Dispatchers.IO` | `sendImmediate`, file I/O, SharedPrefs | Should NOT send to `connection` |
| `Dispatchers.Main` | UI, ViewModel state updates | Correct |
| `viewModelScope` | Coroutine lifecycle tied to VM | Correct |

### 13.2 Recommended Threading Model

```
Main Thread           → UI state reads, Compose recomposition
IO Thread             → SharedPrefs, file I/O, layout save/load ONLY
gamepadDispatcher     → ALL connection.sendInput() calls (transmit loop + immediate sends)
viewModelScope.Main   → StateFlow emissions, connection lifecycle events
```

### 13.3 Coroutine Anti-Patterns to Fix

- `sendImmediate` launching a new `Job` per call on `Dispatchers.IO` → channel or post to `gamepadDispatcher`
- Signal polling `while(true) { delay(2000) }` — acceptable, but should check `isActive` and cancel on disconnect

---

## 14. MEMORY ALLOCATION

### 14.1 High-Frequency Allocations

| Location | Allocation | Frequency | Fix |
|---|---|---|---|
| `GamepadViewModel` | `Pair(x, y)` for stick state | 200 Hz | Replace with two `mutableFloatStateOf` |
| `HudEditorViewModel` | Full `Map` copy per mutation | 60 Hz (drag) | Switch to `SnapshotStateMap` |
| `sendImmediate` | New `Job`/coroutine per call | Per button press | Route to `gamepadDispatcher` channel |
| `GamepadInput` | Single shared instance (line 29) | — | ✅ Already zero-allocation model |

### 14.2 Canvas Drawing

- All `Paint`/`Color` objects inside `drawInbuildTouchpadGrid` should be created outside the draw lambda if possible, or use `drawContext.canvas.nativeCanvas` with pre-allocated objects.
- For static grid lines, the `baseLineAlpha`, `tickBaseAlpha`, `gradAlpha` constants are already correctly placed outside the draw call.

### 14.3 `ControlKey.fromIdentifier` Cache

Pre-build `Map<String, ControlKey>` once in `GamepadViewModel.init`. Eliminates repeated iteration on every button press.

---

## 15. PERSISTENCE & ERROR HANDLING

### 15.1 `LayoutManager` (SharedPreferences + File I/O)

- Profile save uses `Dispatchers.IO` — correct
- Profile load: verify it is NOT called on the main thread
- Error handling: ensure `try/catch` around JSON parsing for `.nxprc` files; malformed files should emit a `Result.Failure` state, not crash

### 15.2 `.nxprc` Sync (NxprcSyncReceiver / NxprcSyncServer)

- File receipt and parsing should be fully on `Dispatchers.IO`
- Parsed `NxpComponentDef` should be validated before insertion into `ComponentCatalog`
- Invalid files: log warning, skip insertion, notify UI via `SharedFlow<SyncError>`

### 15.3 Error Propagation Pattern

Recommended: Use sealed `Result` types for all async operations that can fail:

```kotlin
sealed class ConnectionResult {
    data class Connected(val type: ConnectionType) : ConnectionResult()
    data class Failed(val reason: String, val exception: Exception?) : ConnectionResult()
    object Disconnected : ConnectionResult()
}
```

Expose as `SharedFlow<ConnectionResult>` — UI layer shows the appropriate error message.

---

## 16. TESTING STRATEGY

### 16.1 What to Test

| Component | Test Type | Why |
|---|---|---|
| `GamepadNetworkManager.sendImmediate` race fix | Unit + Thread safety | Critical correctness |
| `ControlKey.fromIdentifier` cache | Unit | Hot path correctness |
| `HudEditorViewModel.addControl` mutual exclusivity | Unit | Complex business logic |
| `ComponentResolver.resolve` | Unit | 4-tier fallback correctness |
| `HudCompatibilityService.isSkinCompatible` | Unit | Pure function, easily testable |
| `ComponentCatalog.forCategory` | Unit | Index correctness |
| Layout save/load round-trip | Integration | Persistence correctness |

### 16.2 What NOT to Test (yet)

- Canvas rendering output — not worth the setup complexity at this stage
- Navigation graph — functional behavior is the test
- Sensor callbacks — platform-dependent, test at integration level only

### 16.3 Test Infrastructure

- Use `kotlinx.coroutines.test.runTest` for all ViewModel unit tests
- `TestScope` + `UnconfinedTestDispatcher` for flow-based tests
- No Robolectric needed for pure ViewModel logic
- Use fakes (not mocks) for `IGamepadConnection` in network tests

---

## 17. PRIORITIZED IMPLEMENTATION TABLE

Priority: 🔴 Critical → 🟡 Important → 🟢 Enhancement

| # | File | Current Problem | Change | Priority | Risk | Test Required |
|---|---|---|---|---|---|---|
| 1 | `GamepadNetworkManager.kt:123` | `@SuppressLint("MissingPermission")` | Add runtime BT permission check, remove annotation | 🔴 | Low | Unit |
| 2 | `GamepadNetworkManager.kt` | Race: `sendImmediate` on IO + `startTransmitting` on `gamepadDispatcher` | Route `sendImmediate` to `gamepadDispatcher` | 🔴 | Medium | Thread-safety test |
| 3 | `GamepadNetworkManager.kt` | `@Volatile` missing on `connection` field | Add `@Volatile` to connection field | 🔴 | Low | Unit |
| 4 | `GamepadViewModel.kt` | `Pair(x,y)` allocation on 200 Hz stick path | Replace with two `mutableFloatStateOf` per stick | 🔴 | Low | No regression |
| 5 | `HudEditorViewModel.kt` | Full `Map` copy on every element drag | Switch to `SnapshotStateMap` | 🔴 | Medium | Drag perf test |
| 6 | `GamepadViewModel.kt` | `ControlKey.fromIdentifier` called per press | Pre-build `Map<String, ControlKey>` cache in `init` | 🟡 | Low | Unit |
| 7 | `HudEditorViewModel.kt` | `compatibleSkins` recomputes on any component change | Use `flatMapLatest` on `selectedControlFlow` only | 🟡 | Low | Flow emission test |
| 8 | `HudEditorViewModel.kt` | `isSkinCompatible` calls `CategoryManager` in inner loop | Pre-resolve control type once, pass into filter | 🟡 | Low | Unit |
| 9 | `ComponentRegistry` / `RemoteComponentRegistry` | Separate registries, O(n) scan | Merge into `ComponentCatalog` with ID + category indexes | 🟡 | Medium | Lookup unit tests |
| 10 | `ControllerElementRenderer.kt` | Resolution logic in UI composable, re-runs per frame | Extract `ComponentResolver`, wrap in `remember(assetId)` | 🟡 | Medium | Integration |
| 11 | `GamepadNetworkManager.kt` | Benchmark `Log.d` in production | Gate behind `BuildConfig.DEBUG` | 🟡 | Very Low | None |
| 12 | `GamepadViewModel.kt` | USB BroadcastReceiver inline in ViewModel | Extract to `UsbConnectionMonitor` exposing `Flow<UsbEvent>` | 🟢 | Medium | Unit |
| 13 | `GamepadNetworkManager.kt` | `NxprcSyncServer` started in `init{}` unconditionally | Lifecycle-aware start/stop | 🟢 | Low | Manual |
| 14 | `GamepadNetworkManager.kt` | New `Job` per `sendImmediate` call | Channel-based queuing | 🟢 | Low | Perf benchmark |
| 15 | ButtonStudio filter pipeline | Full filter+sort on every recompose | Pre-built category indexes in `ComponentCatalog` | 🟢 | Low | Unit |
| 16 | `HudEditorViewModel.kt` | `saveProfile` swallows errors silently | Expose `Result<Unit>` via `SharedFlow` | 🟢 | Low | Unit |
| 17 | `NavigationGraph.kt` | Route string arguments | Use strongly-typed Nav3 route parameters | 🟢 | Low | Manual |

---

## 18. RECOMMENDED FINAL ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────┐
│                      UI LAYER                       │
│  GamepadScreen  HudEditorScreen  ButtonStudioScreen  │
│  SettingsScreen  HomeScreen  ConnectionScreen        │
│  ─────────────────────────────────────────────────  │
│  Reads: StateFlow / SnapshotState / collectAsState   │
│  Writes: calls ViewModel functions only              │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                  VIEWMODEL LAYER                    │
│                                                     │
│  GamepadViewModel          HudEditorViewModel        │
│  ├─ leftStickX/Y (Float)   ├─ elements (SnapshotMap)│
│  ├─ rightStickX/Y (Float)  ├─ selectedControl       │
│  └─ isConnected (derived)  └─ compatibleSkins (flow)│
│                                                     │
│  NavigationViewModel       StudioViewModel (new)    │
│  ├─ backstack              ├─ selectedCategory      │
│  ├─ selectedAssetId        └─ filtered list         │
│  └─ selectedProfile                                 │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────────┐
│                 DOMAIN / SERVICE LAYER               │
│                                                      │
│  GamepadConnectionManager   GamepadTransmitter        │
│  ├─ transport selection     ├─ gamepadDispatcher      │
│  ├─ connect/disconnect      ├─ 200Hz / 125Hz loop     │
│  ├─ WifiLock lifecycle      └─ SOLE writer to conn.  │
│  └─ StateFlow<ConnState>                             │
│                                                      │
│  ComponentCatalog           HudCompatibilityService  │
│  ├─ byId index              └─ pure function         │
│  ├─ byCategory index                                 │
│  └─ resolve(assetId)                                 │
│                                                      │
│  ComponentResolver          UsbConnectionMonitor     │
│  └─ 4-tier lookup           └─ Flow<UsbEvent>        │
└────────────────┬─────────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────────┐
│              INFRASTRUCTURE LAYER                    │
│                                                      │
│  IGamepadConnection  LayoutManager  SensorController │
│  ├─ NetworkClient    ├─ SharedPrefs  ├─ accelerometer│
│  ├─ AoaConnection    └─ JSON I/O     └─ gyroscope    │
│  ├─ AdbBridgeConn                                    │
│  └─ BtRfcommConn    NativeComponentRegistry          │
│                     RemoteComponentRegistry          │
│                     NxprcSyncServer/Receiver         │
└──────────────────────────────────────────────────────┘
```

---

## EXECUTION ORDER (Recommended)

### Phase 1 — Critical Bugs (Do First, No Feature Risk)
1. Fix `@SuppressLint` in `GamepadNetworkManager` (#1)
2. Fix race condition: route `sendImmediate` to `gamepadDispatcher` (#2)
3. Add `@Volatile` to `connection` field (#3)
4. Remove benchmark log noise (#11)

### Phase 2 — Hot Path Performance (High ROI)
5. Replace stick `Pair` with two `mutableFloatStateOf` (#4)
6. Cache `ControlKey.fromIdentifier` in VM init (#6)
7. Fix `compatibleSkins` reactivity (#7)
8. Fix `isSkinCompatible` inner-loop CategoryManager calls (#8)

### Phase 3 — Architecture Consolidation (Medium Effort)
9. Switch `HudEditorViewModel._elements` to `SnapshotStateMap` (#5)
10. Create `ComponentCatalog` merging both registries (#9)
11. Extract `ComponentResolver` with `remember(assetId)` (#10)

### Phase 4 — Code Quality & Maintainability (Polish)
12. Extract `UsbConnectionMonitor` (#12)
13. Lifecycle-aware `NxprcSyncServer` (#13)
14. Channel-based `sendImmediate` (#14)
15. Pre-built ButtonStudio category indexes (#15)
16. Error `Result` flows (#16)
17. Strongly-typed Nav3 routes (#17)

---

## RULES REMINDER FOR IMPLEMENTATION

> ✅ `./gradlew compileDebugKotlin testDebugUnitTest` after every change
> ❌ Zero `@Suppress` or `@SuppressLint` in `app/src/` — absolute
> 🌿 All changes on branch `feature/finaltouch-polish-and-optimization`
> 🚫 No new features — polish, optimization, bug-fix only
> 🏗️ Fix architecture before fixing code
> 💡 Use abstraction only when it improves reusability, testability, or separation of responsibility

---

*Generated by Antigravity — NEXPAD Supreme Architecture Audit*
*Date: 2026-10-04*
