# NEXPAD Custom Components, HUD & Plugin Architecture Reference

> **Document Status:** Active Architecture Reference & Roadmap  
> **Target Repositories:** `NEXPAD` (Android Client) & `NEXPADDesktop` (Windows Server)  
> **Last Updated:** 2026-09-09  
> **Current Branches:** `feature/ui-logic-improvements-v2` (Android), `feature/aoa-winusb-poc` (Desktop)

---

## 1. Executive Summary & Core Philosophy

**NEXPAD** is an ultra-low latency, professional-grade virtual gamepad companion and hardware bridge for Android and Windows. It turns any Android smartphone or tablet into an ultra-responsive, highly customizable gaming controller with sub-millisecond input performance, full motion telemetry (gyroscope/accelerometer via Cemuhook DSU), dual-motor haptic rumble, and multi-transport connectivity (Wi-Fi LAN, USB Tethering, USB ADB Bridge, USB Kernel AOA, and Bluetooth Classic).

While input responsiveness and zero packet loss are the foundation, the **visual and ergonomic layer** is what defines the user experience:
1. **Ergonomic Adaptation:** Different game genres (FPS, MOBA, Racing, Retro, Fighting) demand radically different button placements, sizes, and active control sets.
2. **Visual & Tactile Feedback:** Players expect cyberpunk, sci-fi, realistic, or minimalist aesthetic skins, reactive touch animations, spring physics, and neon shader glows.
3. **Extensibility:** Users and AI assistants should be able to create, test, and share custom button designs and complete HUD layouts without needing to recompile the main NEXPAD Android application.

To solve this, NEXPAD embraces a dual-tier component and layout architecture.

---

## 2. The Dual-Tier Custom Component Architecture

```
                                  ┌─────────────────────────────────────────────────────────┐
                                  │                NEXPAD COMPONENT ECOSYSTEM               │
                                  └────────────┬───────────────────────────────┬─────────────┘
                                               │                               │
                                               ▼                               ▼
                     ┌───────────────────────────────────┐   ┌───────────────────────────────────┐
                     │              TIER 1               │   │              TIER 2               │
                     │    Smart Declarative NXP Engine   │   │   "Real Compose" Plugin System    │
                     │          (.nxpcomponent)          │   │      (.nxpplugin / .dex / .apk)   │
                     ├───────────────────────────────────┤   ├───────────────────────────────────┤
                     │ • Format: JSON + SVG + AGSL       │   │ • Format: Raw Kotlin + Compose    │
                     │ • Execution: Native Canvas Engine │   │ • Execution: Runtime DEX / Context│
                     │ • Safety: 100% Sandboxed, No RCE  │   │ • Power: 100% Android / Compose API│
                     │ • Play Store: 100% Compliant      │   │ • Play Store: Restricted (No OTA) │
                     │ • Generation: Instant AI / QR     │   │ • Compilation: Desktop / Cloud SDK│
                     │ • Target: Universal (All Users)   │   │ • Target: Power Users / Sideload  │
                     └───────────────────────────────────┘   └───────────────────────────────────┘
```

---

### 2.1. Tier 1: Smart Declarative NXP Engine (`.nxpcomponent`)

#### Philosophy & Purpose
Tier 1 represents the universal, zero-friction standard for custom buttons and widgets in NEXPAD. It requires **no compiler**, **no dynamic bytecode execution**, and **no security permissions**. Any user, web tool, or LLM (such as Gemini or ChatGPT) can generate a `.nxpcomponent` JSON structure and import it into NEXPAD instantly via QR code, clipboard paste, or file drop.

#### Technical Specification
A Tier 1 component is defined strictly by declarative schemas:
- **Geometry Definition:** SVG vector paths (`pathData`) for shapes, outer bezels, glyph icons, and tactile grooves.
- **Color & Material Gradients:** Radial gradients, linear sweeps, neon glows, glassmorphic frosted tints, and cyberpunk palette tokens.
- **Runtime AGSL Shaders (Android 13+):** GPU-accelerated runtime shaders compiled on the phone GPU (e.g. holographic scanlines, animated neon pulses, digital distortion).
- **Physical Spring Physics:** Configurable spring stiffness and damping ratios (`dampingRatio`, `stiffness`) rendered natively through Jetpack Compose transition primitives.
- **Discrete Visual States:**
  - `REST`: Idle resting state.
  - `HOVER`: Finger hovering or approaching.
  - `PRESSED`: Full tactile engagement (scale down, glow surge, chromatic aberration).
  - `DISABLED`: Muted or inactive state.

#### Performance & Safety Guarantees
1. **Zero Input Latency:** Touch gestures are intercepted by native Compose `pointerInput` on the UI thread and forwarded immediately to `NexPadInputTarget` (which maps directly into `GamepadViewModel.updateButton` / `updateLeftStick`). There are no IPC bridges, no JavaScript V8 overhead, and no serialization bottlenecks.
2. **Zero Remote Code Execution (RCE) Risk:** The JSON file contains only data (points, colors, dimensions, labels). It cannot make network requests, access file systems, invoke system APIs, or read device memory.
3. **100% Google Play Store Compliant:** Complies fully with Google Play Device and Network Abuse policies.

---

### 2.2. Tier 2: "Real Compose" Compiled Plugins (`.nxpplugin` / `.dex`)

#### Philosophy & Purpose
"Real Compose" means developers are **not** writing JSON, schemas, or templates. They write **100% pure, unconstrained, native Kotlin and Jetpack Compose code**, with direct access to:
- Every Jetpack Compose API (`remember`, `LaunchedEffect`, `animateFloatAsState`, Canvas, Gestures, Custom Layouts, SubcomposeLayout).
- Third-party Android libraries (Box2D physics engines, real-time audio synthesizers, 3D Canvas renderers, particle emitters).
- Arbitrary coroutines, background logic, hardware sensor taps, and custom state machines.

The component is compiled into a separate binary (`.dex` or plugin `.apk`), and NEXPAD dynamically loads that compiled code into runtime memory.

#### How "Real Compose" Works Under the Hood
```
┌────────────────────────────────────────────────────────┐
│ 1. DEVELOPER WORKSTATION                               │
│    Writes pure Kotlin @Composable MyButton(...)        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. DESKTOP COMPILATION PIPELINE (NEXPAD Desktop)       │
│    • kotlinc + Jetpack Compose Compiler Plugin         │
│    • d8 / R8 Android Dexer -> classes.dex              │
│    • Packages into .nxpplugin bundle (DEX + Manifest)  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼ (FTP Sidechannel / USB / ADB)
┌────────────────────────────────────────────────────────┐
│ 3. NEXPAD ANDROID CLIENT RUNTIME                       │
│    • DexClassLoader / InMemoryDexClassLoader           │
│    • Dynamic Class Loading -> Implements INexPadPlugin │
│    • Injects @Composable into Controller Canvas        │
└───────────────────────────┘
```

#### The Critical Security & Policy Debate

During architecture planning, two fundamental real-world constraints were identified and analyzed:

##### Constraint 1: Google Play Store Device & Network Abuse Policy
- **The Rule:** Google Play policies strictly prohibit downloading, installing, or executing executable code (such as `.dex`, `.jar`, or `.so` files) from any external source outside Google Play.
- **The Catch:** Attempting to disguise a `.dex` file inside an `.nxppack` or `.nxpplugin` container and downloading it over Wi-Fi/FTP directly violates this policy and leads to immediate app suspension or developer ban.
- **Policy Carve-out:** The only exception allowed by Google Play is code running inside an isolated interpreter or virtual machine with no direct native OS or JVM API access (such as a JavaScript engine in a WebView or a declarative JSON engine like Tier 1). A `.dex` file executed via `DexClassLoader` runs on ART with direct JVM/Android API access and **does not** qualify.
- **Conclusion:** Over-the-air hot-loading of DEX plugins is **impossible for the official Google Play Store build**.

##### Constraint 2: Sideloading Security & RCE Vulnerability
- Sideloading (GitHub APK / direct APK install) removes Google Play policy restrictions, but **it does not eliminate the operating system security risks**.
- Any dynamic `.dex` executed inside NEXPAD's process runs with the **exact same privileges** as NEXPAD itself:
  - It can read files in `filesDir` and `shared_prefs`.
  - It can send unauthorized network packets over the internet.
  - It could execute crypto-miners, log button inputs (keylogging), or crash the app.
- Loading untrusted community `.dex` files directly into memory is a critical security hazard.

##### The Solution: Hybrid Dual-Track Delivery
To achieve maximum power without sacrificing security or Play Store distribution, NEXPAD adopts a clean dual-track strategy:

| Feature Dimension | Google Play Store Track (Production) | Developer / Sideload Track (Elite) |
| :--- | :--- | :--- |
| **Component Engine** | **Tier 1 Declarative Engine (`.nxpcomponent`)** | **Tier 1 + Tier 2 "Real Compose" DEX** |
| **Distribution** | Google Play Store | GitHub Releases / F-Droid / Sideload |
| **Plugin Packaging** | Declarative JSON / SVG / AGSL bundles | Compiled `.dex` or Android Plugin APKs |
| **Security Mechanism** | Sandboxed parser, zero dynamic execution | Cryptographic APK signing + Explicit Developer Mode Toggle |
| **Play Store Policy** | 100% Compliant | N/A (Self-hosted distribution) |

---

## 3. Desktop Sidechannel & File Transfer Protocol (FTP)

NEXPAD's core gamepad input protocol requires maximum bandwidth and minimal jitter. The input pipeline and file transfer system must never interfere with one another.

### 3.1. Strict Separation of Channels
1. **Gamepad Hot-Path (Untouched & Zero-Latency):**
   - **44-byte Input Packet (Android -> PC):** Fixed binary layout containing sequence number, 32-bit button bitmask, 4 analog stick axes, 2 trigger axes, and 6 gyro/accelerometer floats.
   - **10-byte Feedback Packet (PC -> Android):** Fixed binary layout containing dual-motor rumble intensities, packet loss statistics, and 32-bit echo sequence for nanosecond round-trip time (RTT) tracking.
   - **Rule:** These protocols remain permanently untouched. No file transfer or plugin data is ever injected into these streams.
2. **File Transfer Sidechannel (Non-Interfering):**
   - Transferred over a dedicated sidechannel port (`9996`) or over the existing ADB / TCP socket connection when idle.
   - Operates on a chunked acknowledge protocol (`PACKET_TYPE_FTP_START (0x20)`, `PACKET_TYPE_FTP_CHUNK (0x21)`, `PACKET_TYPE_FTP_ACK (0x22)`, `PACKET_TYPE_FTP_COMPLETE (0x23)`).
   - Allows transferring plugin assets, `.nxpcomponent` definitions, and layout backups between Desktop and Android smoothly in the background.

---

## 4. What Has Been Achieved Till Now

The following systems have been designed, implemented, thoroughly debugged, and verified on live physical Android hardware:

### 4.1. Factory Default Layout Profiles & Registry
- **File:** [LayoutProfile.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/model/LayoutProfile.kt)
- Created **5 factory default layout profiles**:
  1. **Standard Elite:** Balanced dual-stick, D-Pad, ABXY, bumpers, triggers, and rear macro buttons (M1/M2).
  2. **FPS Tactical:** Enlarged right joystick for precise aiming, prominent Left Trigger (ADS) and Right Trigger (Fire), quick-action face buttons.
  3. **MOBA Action:** Shifted right-hand face button diamond (A/B/X/Y) with skill bumpers in a curved radial arc.
  4. **Racing Sim:** Linear-style oversized shoulder triggers for throttle/brake, high-precision steering stick, gear shift buttons.
  5. **Retro Arcade:** Classic 4-way D-Pad and 6-button arcade layout (A, B, X, Y, LB, RB) for fighting and platforming games.
- **Factory Protection Invariant:** Default profiles have `isDefault = true`. They are strictly permanent: they **cannot be deleted** and **cannot be renamed**. Users can customize button positions on defaults, with the ability to "Reset to Factory Defaults" at any time.

### 4.2. Layout Manager & Reactive Persistence
- **File:** [LayoutManager.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/model/LayoutManager.kt)
- File-backed persistence using Android `SharedPreferences` under namespace `NEXPAD_LAYOUTS_V3`.
- Exposes a reactive Kotlin `StateFlow<List<LayoutProfile>>` (`profilesFlow`) and `StateFlow<String>` (`activeProfileNameFlow`).
- Implemented `createCustomProfile()`, `saveProfile()`, `deleteProfile()` (blocks deletion of defaults), `duplicateProfile()`, and `resetDefaultProfile()`.

### 4.3. Virtual Controller Management Hub
- **File:** [VirtualControllerScreen.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/VirtualControllerScreen.kt)
- Located in Command Center under the "Virtual Controller" tile (`navController.navigate("virtual_controller")`).
- Features:
  - Profile cards showing profile name, description, button count, and badges (`🔒 DEFAULT` vs `✨ CUSTOM`).
  - Active layout radio toggle with visual glowing feedback.
  - Direct action buttons on every card: "Play", "Edit HUD", "Duplicate", and "Delete" (custom profiles only).
  - "+ Add Custom Layout" modal dialog: allows naming the new profile, selecting an archetype, and selecting which buttons to include via an interactive checklist.

### 4.4. Live Home Screen Carousel Sync
- **Files:** [HomeScreen.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/HomeScreen.kt), [VShapedPanel.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/components/home/VShapedPanel.kt), [InnerLayoutCard.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/components/card/InnerLayoutCard.kt)
- The "Ready to Play" carousel on the Home Screen is dynamically wired to `LayoutManager.profilesFlow`.
- Swiping or clicking layout cards in the carousel updates the active layout profile in real time across the entire app.

### 4.5. Button Studio & Component Gallery
- **File:** [ButtonStudioScreen.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/studio/ButtonStudioScreen.kt)
- Implemented horizontal controller category slider:
  - `ALL`, `ABXY Buttons`, `Bumpers`, `Triggers`, `Joysticks`, `D-Pad`, `Home / Guide`, `System`, `Macros`.
- Filters available `.nxpcomponent` designs by target control.
- Provides interactive touch testing arena ([SandboxPreviewModal.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/studio/SandboxPreviewModal.kt)) with live sub-millisecond telemetry.
- Includes 14 built-in cyberpunk component presets in [DefaultComponents.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/runtime/registry/DefaultComponents.kt).

### 4.6. Complete HUD Editor Overhaul & Bug Fixes
- **File:** [HudEditorScreen.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/ui/HudEditorScreen.kt)
- **Resolved Button Desync Bug:** Previously, unselecting M1/M2/M3 caused remaining buttons to jump or inherit incorrect offsets. This was caused by unkeyed local `remember { mutableStateOf(...) }` offset states. Resolved by:
  - Wrapping each button in an explicit Compose `key(key) { ... }` block.
  - Passing keys into `pointerInput(key, screenWidth, screenHeight)`.
  - Mutating the underlying `LayoutProfile` state directly instead of holding orphaned local coordinates.
- **Smart Dynamic Inspector Docking:** The Inspector / Tuning card automatically computes whether the selected button is on the upper or lower half of the screen (`yRatio < 0.48f`). If the button is on top, the card docks to the bottom; if the button is on the bottom, the card docks to the top. The inspector **never covers the button being edited**.
- **Pixel-Perfect Micro-Nudge Controls:** Added 4 directional nudge buttons (`⬅`, `➡`, `⬆`, `⬇`) that move the selected button by exactly 1% increments for ergonomic fine-tuning.
- **Scale Steppers:** Added `[-]` and `[+]` buttons that scale buttons up or down in 5% increments, with a live percentage readout.
- **Grouped Button Visibility Manager:** A structured dialog categorized into Face Buttons, Shoulder/Triggers, Sticks/DPad, System, and Macros, with instant 1-tap presets ("All (18)", "Standard Only (14)", "Clear All").
- **D8 Compiler Cleanliness:** Fixed Compose synthetic non-local return compiler errors by replacing `return@key` with standard conditional branches (`if (position != null)`).

---

## 5. Next Steps & Ergonomic Calibration Workflow

### 5.1. User Layout Calibration Flow (Current Milestone)
Because real ergonomics depend on physical hand sizes and screen aspect ratios, the user calibrates the exact button positions on their own physical device:
1. User opens **HUD Editor** for the **Standard Elite** layout profile on their phone.
2. User drags and fine-tunes all buttons (ABXY, sticks, D-Pad, bumpers, triggers, macros) to their preferred ergonomic positions using drag and nudge controls.
3. User taps **"SAVE LAYOUT"**.
4. The system inspects the saved layout via ADB:
   ```bash
   adb shell run-as com.sanket.tools.nexpad cat shared_prefs/NEXPAD_LAYOUTS_V3.xml
   ```
5. The extracted coordinates (`xRatio`, `yRatio`, `scale`) are baked directly into `standardElitePositions()` in [LayoutProfile.kt](file:///C:/Users/parma/OneDrive/Desktop/NEXPAD%20Project/NEXPAD/app/src/main/java/com/sanket/tools/nexpad/model/LayoutProfile.kt).
6. Future app installations will start with these ergonomic positions as permanent factory Default 1.

### 5.2. Future Roadmap
1. **Desktop Plugin Studio Tab:** Add a "Plugins" navigation tab in NEXPAD Desktop with hot-reload compiler integration.
2. **Community Repository:** Online repository of `.nxpcomponent` packs that can be previewed on the web and imported via QR code.
3. **Multi-Touch Macro Combos:** Allow custom macro buttons (M1-M4) to execute timed multi-key combos (e.g. fighting game inputs) via declarative sequence steps in the NXP Engine.
