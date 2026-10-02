# 📱 NEXPAD — Android Virtual Gamepad & HUD Studio

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-brightgreen.svg)](https://developer.android.com)
[![UI Framework](https://img.shields.io/badge/UI-100%25%20Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Refresh Rate](https://img.shields.io/badge/Display-120Hz%20%2F%2090Hz%20ProMotion-00E5FF.svg)](#-procedural-vector-engine--120-fps-rendering)
[![Response Time](https://img.shields.io/badge/Response%20Time-%3C1ms%20(Instant%20Event%20Bypass)-00E5FF.svg)](#-key-features)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![Zero Telemetry](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%200%20Telemetry-success.svg)](./PRIVACY_POLICY.md)
[![Code Quality](https://img.shields.io/badge/Code%20Standard-Zero%20%40Suppress%20Guarantee-purple.svg)](#-developer--contributor-guide)

**NEXPAD for Android** transforms your smartphone or tablet into an eSports-grade, ultra-low latency virtual **Xbox 360 controller** and motion steering wheel for Windows PC gaming.

Built completely from scratch using **100% native Jetpack Compose** and hardware-accelerated **Canvas rendering**, NEXPAD eliminates raster bitmaps entirely. Every button, analog stick, D-Pad, and trigger is rendered procedurally at up to **120 FPS** with harmonic spring physics, dynamic tactile haptics, and responsive RGB atmospheric blooms.

---

## 🧭 Subsystem Navigation

```
                       ┌──────────────────────┐
                       │   NEXPAD/README.md   │
                       │    (Android Client)  │
                       └──────────┬───────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
     HISTORY.md              ROADMAP.md            ARCHITECTURE.md
   "Where We Came From"    "Where We're Going"      "How It Works"
```

- 📖 **[`HISTORY.md`](./HISTORY.md)** — Comprehensive record of all 36 feature branches from initial prototype to the Universal Timeline Track Engine.
- 🗺️ **[`ROADMAP.md`](./ROADMAP.md)** — Architectural roadmap including Bluetooth LE HID, dynamic asset marketplace, and haptic synthesizer.
- 🏛️ **[`ARCHITECTURE.md`](./ARCHITECTURE.md)** — Low-level deep dive into UDP packet serialization, AOA USB streaming, and sensor fusion algorithms.
- 🎨 **[`LAYOUT_METRICS_AND_ERGONOMICS.md`](./LAYOUT_METRICS_AND_ERGONOMICS.md)** — Anthropometric thumb arc curves, touch target guidelines, and multi-touch cluster spacing.
- 📋 **[`PRD.md`](./PRD.md)** — Product Requirements Document and technical functional specifications.
- 🔒 **[`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md)** — Google Play Store compliant zero-telemetry privacy policy.
- ⚖️ **[`DISCLAIMER.md`](./DISCLAIMER.md)** — Nominative Fair Use and legal non-affiliation disclosure.

---

## 🚀 Key Features

### 1. ⚡ Ultra-Low Latency Multi-Transport Engine
NEXPAD features an asynchronous, non-blocking I/O pipeline supporting four high-speed transports:
- **Zero-Driver USB (AOA - Android Open Accessory)**: Connect directly via a standard USB-C cable. Provides sub-millisecond ($<1\,\text{ms}$) hardware transmission without enabling USB Debugging, ADB, or Root.
- **Instant Event-Driven Dispatch**: Button taps, releases, and analog triggers bypass periodic loops and dispatch immediately with **zero delay ($<1\,\text{ms}$)**.
- **200Hz Continuous Stream Pipeline**: Transmits compact 44-byte binary Xbox controller reports at a sustained 200Hz ($5\,\text{ms}$) over local 5GHz Wi-Fi or USB with sub-frame precision.
- **ADB Reverse TCP Bridge**: High-reliability fallback over standard Android Debug Bridge (`adb reverse tcp:9999 tcp:9999`).
- **Bluetooth RFCOMM (125Hz / 8ms)**: Optimal ACL slot-aligned wireless direct connection when local Wi-Fi infrastructure is unavailable.

### 2. 🎨 100% Procedural Vector Engine & 120 FPS Rendering
- **Zero Bitmaps**: All controls are rendered mathematically using Jetpack Compose `Canvas` and `DrawScope` paths, arcs, and gradients.
- **ProMotion 120Hz / 90Hz Display Support**: Fluid animations backed by damped harmonic spring kinematics (`stiffness = 440f`, `dampingRatio = 0.68f`).
- **7-Layer Canonical Display List**: Every component adheres to an unyielding 7-layer rendering hierarchy:
  1. *Layer 0: Kinetic Aura (`.drawBehind`)* — Atmospheric neon bloom expanding dynamically with interaction depth.
  2. *Layer 1: Recessed Bezel* — Concave structural well with ambient shadow occlusion.
  3. *Layer 2: Beveled Body Rim* — Micro-chamfered metallic or matte perimeter stroke.
  4. *Layer 3: Dynamic State Plane* — Spring-interpolated Z-axis physical depression.
  5. *Layer 4: Procedural Graphic Core* — Concentric machining grooves, liquid fill meniscus, radar sweeps, or VU meters.
  6. *Layer 5: Typography / Symbol* — High-contrast vector labels with back-face depth.
  7. *Layer 6: Specular Optical Sheen* — Micro-lens reflections and glass glare arcs.

### 3. 🎛️ Interactive HUD Studio & Layout Builder
- **Real-Time Layout Customizer**: Long-press any button or enter HUD Studio to move, scale ($50\%\dots 200\%$), and adjust opacity ($10\%\dots 100\%$) of any control on screen.
- **Grid Snapping & Symmetry Guides**: Align thumbsticks, ABXY clusters, and D-Pads with millimeter precision.
- **Built-in Presets**:
  - *Xbox 360 Default* — Classic asymmetrical ergonomic layout.
  - *Racing Sim* — Centralized steering arc with oversized throttle/brake trigger sliders.
  - *FPS / Tactical* — Clustered bumper/trigger controls for rapid twitch reflex aiming.
  - *Retro Arcade* — Symmetrical D-Pad and face button positioning for 2D platformers.
  - *Custom Profiles* — Create, export, and load endless bespoke layouts saved locally in encrypted DataStore.

### 4. 🕹️ Over 42 Bespoke Procedural Component Variants
Choose from a rich catalog of custom-engineered vector components:

| Category | Available Variants | Unique Interactive Behaviors |
|---|---|---|
| **Action Buttons (ABXY)** | Realistic, Liquid, Facet, Flip, Ripple, Magnify, Orbit, Capsules, Eclipse | Sine-wave liquid fill, 3D 180° card-flip, counter-rotating planetary rings, 1.25x optical zoom, radial corona eclipse |
| **D-Pads** | Classic Cross, 3D Rocker, Lens D-Pad, Four Lenses D-Pad | Smooth 360° rocking tilt vector, laser chevrons, floating optical gem pods |
| **Thumbsticks (LS / RS)** | Precision Polar Thumbsticks | Dynamic deadzone gating, spring tension recentering, L3/R3 tactile click |
| **Analog Triggers (LT / RT)** | Classic Slider, VU Slabs, Liquid Chamber, Radar Target, Needle Gauge | Continuous $0\dots 255$ analog depression, VU audio-style LED steps, sweeping radar ping, physical dial sweep |
| **Bumpers (LB / RB)** | Ergonomic Curved Bumpers | Chamfered tactile edge with spring micro-bounce on actuation |
| **System Buttons** | Back, Start, Guide (Home), Profile Switcher | Optical lens system buttons with recessed wells and specular sheen arcs |

### 5. 📳 Tactile Force-Feedback & Visual Game Rumble
- Receives real-time 8-byte rumble motor packets (`GamepadFeedback`) sent by Windows PC games.
- Drives the smartphone's internal linear resonant actuator (LRA) or eccentric rotating mass (ERM) motor via Android's `Vibrator` / `VibratorManager` APIs.
- **Visual Rumble Micro-Jitter**: Buttons on screen physically vibrate with high-frequency micro-jitter in response to in-game explosions, collisions, and engine revs.

### 6. 🏎️ 6-Axis Motion Steering & CemuHook DSU Emulation
- Turns your phone into an ultra-responsive steering wheel for PC racing titles (*Forza*, *F1*, *Assetto Corsa*).
- Built-in **CemuHook DSU Motion Client**: Streams 6-axis gyroscope and accelerometer orientation vectors to NEXPAD Desktop for console emulators (**Cemu**, **Yuzu**, **Ryujinx**, **RPCS3**, **Dolphin**).

---

## 📥 Downloads & Installation

### Option A: Google Play Store
1. Search for **NEXPAD Gamepad** on the [Google Play Store](https://play.google.com/). *(Coming Soon)*
2. Tap **Install**. All updates will be managed automatically.

### Option B: Standalone APK (GitHub Releases)
1. Download the latest `NEXPAD-release.apk` from [GitHub Releases](https://github.com/).
2. On your Android device, open the downloaded APK file.
3. If prompted, allow "Install unknown apps" for your browser or file manager.
4. Tap **Install** to finish setup.

### Option C: F-Droid (Free & Open Source Repository)
1. Open the [F-Droid Client](https://f-droid.org/). *(Coming Soon)*
2. Search for **NEXPAD** and tap **Install** to receive verified reproducible builds.

---

## 🕹️ User Quick Start Guide (4 Simple Steps)

### Step 1: Launch NEXPAD Desktop on Your PC
Ensure [NEXPAD Desktop](../NEXPADDesktop) is running on your Windows PC and the **ViGEmBus** driver is installed.

### Step 2: Open NEXPAD on Your Phone
Launch the NEXPAD app. Grant motion sensor and local network permissions when requested.

### Step 3: Choose Your Connection Mode
- **USB Connection (Best Performance — $<1\,\text{ms}$)**:
  1. Plug your phone into your PC using a standard USB-C cable.
  2. In the NEXPAD app, select **Connect USB (AOA)**.
  3. Accept the USB accessory prompt.
- **Wi-Fi Connection (Wireless — $1.2\text{--}2.5\,\text{ms}$)**:
  1. Ensure your phone and PC are connected to the same Wi-Fi network (5GHz recommended).
  2. NEXPAD Desktop will display its local IP address (e.g., `192.168.1.150`).
  3. In the mobile app, tap **Connect Wi-Fi**, enter the IP address, and tap **Connect**.

### Step 4: Play Your Games!
Windows will sound the hardware chime as it mounts a virtual **Xbox 360 Controller**. Open Steam, Game Pass, Epic Games, or any emulator and start playing immediately!

---

## 🛠️ Developer & Contributor Guide

### Technical Stack & Dependencies
- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (BOM 2024.x)
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Architecture**: MVI / MVVM with `GamepadViewModel`, Kotlin Coroutines, and `StateFlow`
- **Data Persistence**: Android Jetpack DataStore (Preferences & Proto)
- **Shared Engine**: Protocol module (`:protocol`) for 44-byte binary packet serialization

### Project Architecture Overview
```
NEXPAD/app/src/main/java/com/sanket/tools/nexpad/
├── model/                # Controller state models, layout presets, haptic tokens
├── network/              # AoaManager (USB), UdpClient (1000Hz), AdbBridge, BluetoothClient
├── runtime/              # NXPRC vector runtime interpreter and dynamic package parser
├── sensors/              # MotionSensorManager, Gyroscope fusion, CemuHook DSU packet builder
├── ui/                   # Jetpack Compose UI subsystem
│   ├── components/       # 42+ Procedural vector components (ABXY, D-Pad, Joysticks, Triggers)
│   │   └── controller/   # LiquidButton, FacetButton, FlipButton, RippleButton, etc.
│   ├── hud/              # In-game HUD overlay and status telemetry bar
│   ├── studio/           # HUD Studio, Button Studio, interactive drag-and-drop editor
│   ├── theme/            # Material3 cyber-industrial theme, neon palettes, typography
│   └── virtualcontroller/# VirtualControllerScreen main gameplay surface
├── utils/                # HapticFeedbackHelper, MathUtils, SoundManager
└── viewmodel/            # GamepadViewModel, StudioViewModel, ConnectionViewModel
```

### Strict Code Quality Guarantee
To maintain the highest level of stability, type safety, and maintainability:
- **Zero Suppression Rule**: Strictly **0 `@Suppress`** and **0 `@SuppressLint`** across the entire codebase.
- **No Unchecked Casts**: All state conversions and pointer events are strongly typed.
- **Garbage Collection Optimization**: Zero memory allocations inside high-frequency pointer input and render loops.

### Building from Source

```powershell
# Clone the repository
git clone https://github.com/parmarsanket/nexpad.git
cd nexpad/NEXPAD

# Verify Kotlin compilation
.\gradlew.bat compileDebugKotlin

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK (outputs to app/build/outputs/apk/debug/)
.\gradlew.bat assembleDebug

# Assemble Release APK (requires signing credentials in local.properties)
.\gradlew.bat assembleRelease
```

### 🤖 Modern Android CLI & Agent Skills Integration

NEXPAD integrates with the official **Android CLI** and Google Agent Skills to maximize development productivity, accelerate testing, and enforce Play Store policy compliance:

- **Installing the Android CLI**:
  ```powershell
  # Windows installation
  curl -fsSL https://dl.google.com/android/cli/latest/windows_x86_64/install.cmd -o "%TEMP%\i.cmd" && "%TEMP%\i.cmd"
  ```
- **Equipping Official Google Agent Skills**:
  ```powershell
  android skills add play-policy-insights android-profiler r8-analyzer edge-to-edge testing-setup
  ```
- **Accelerated Development Commands**:
  - `android run --use-delta-install`: Fast incremental delta deploy directly to a connected phone or emulator without full APK re-transfers.
  - `android layout`: Dumps and inspects the live Jetpack Compose UI layout tree directly in JSON format to rapidly debug HUD positioning and touch bounds.
  - `android screen capture`: Takes automated high-resolution screenshots from connected devices.
  - `android docs search "..."`: Queries official Android knowledge base documentation and architectural patterns directly from the terminal.
  - `play-policy-insights`: Runs an automated pre-submission audit against Google Play Store policy domains (permissions hygiene, privacy disclosure, and data safety).

---

## 🔒 Privacy, Security & Permissions Transparency

NEXPAD is engineered with total respect for user privacy:
- **Zero Telemetry**: No trackers, no Google Analytics, no Firebase Crashlytics, and no ad SDKs.
- **100% Local Communication**: Network communication occurs exclusively between your phone and your PC on your local private subnet or via direct physical USB cable.
- **Permissions Breakdown**:
  - `INTERNET` & `ACCESS_NETWORK_STATE`: Required strictly for local peer-to-peer UDP packet transmission to your PC.
  - `VIBRATE`: Required for tactile haptic feedback in response to game rumble.
  - `HIGH_SAMPLING_RATE_SENSORS`: Required to sample the gyroscope at 200Hz (5ms) for steering and motion controls.
  - `WAKE_LOCK`: Prevents the screen from dimming or sleeping while you are actively playing games.

For full legal disclosures, read [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).

---

## ⚖️ Legal Disclaimer & Trademark Notice

NEXPAD is an independent, open-source software project. 

- **Non-Affiliation**: NEXPAD is not affiliated with, endorsed by, sponsored by, or associated with Microsoft Corporation, Sony Interactive Entertainment Inc., Nintendo Co., Ltd., Valve Corporation, or any of their subsidiaries.
- **Nominative Fair Use**: All mentions of **Xbox**, **Xbox 360**, **PlayStation**, **Nintendo Switch**, **Steam**, and associated controller layouts are used solely under the doctrine of **Nominative Fair Use** to accurately describe device compatibility and technical interoperability.
- For complete terms and DMCA safe harbor disclosures, see [`DISCLAIMER.md`](./DISCLAIMER.md).

---

## 📄 License & Attribution

This project is licensed under the **Apache License, Version 2.0**.
- See the full [LICENSE](./LICENSE) file for legal terms.
- Third-party open-source libraries and component attributions are documented in the [NOTICE](./NOTICE) file.

Copyright © 2026 **Sanket Parmar**. All rights reserved.