<div align="center">

  # 📱 NEXPAD
  ### Turn Any Android Smartphone into an Ultra-Low-Latency PC Gaming Controller

  <p align="center">
    <b>High-Speed Multi-Transport Engine &nbsp;•&nbsp; 100% Procedural Vector Graphics &nbsp;•&nbsp; 120 FPS ProMotion &nbsp;•&nbsp; Zero Telemetry</b>
  </p>

  <p align="center">
    <a href="https://github.com/parmarsanket/NEXPAD/releases/tag/v1.0.0">
      <img src="https://img.shields.io/badge/Release-v1.0.0%20Production-00E5FF.svg?style=for-the-badge&logo=github&logoColor=white" alt="GitHub Release v1.0.0" />
    </a>
    <a href="https://play.google.com/store/apps/details?id=com.sanket.tools.nexpad">
      <img src="https://img.shields.io/badge/Google%20Play-Get%20it%20on%20Google%20Play-414141.svg?style=for-the-badge&logo=googleplay&logoColor=white" alt="Google Play Store" />
    </a>
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-brightgreen.svg?style=flat-square&logo=android" alt="Platform" />
    <img src="https://img.shields.io/badge/UI-100%25%20Jetpack%20Compose-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Display-120Hz%20%2F%2090Hz%20ProMotion-00E5FF.svg?style=flat-square" alt="120Hz ProMotion" />
    <img src="https://img.shields.io/badge/Polling%20Rate-200Hz%20Low%20Latency-3FD25A.svg?style=flat-square" alt="200Hz Low Latency" />
    <img src="https://img.shields.io/badge/DSU%20Motion-CemuHook%20(Port%2026760)-FF9800.svg?style=flat-square" alt="DSU Motion Client" />
    <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%200%20Telemetry-00C853.svg?style=flat-square" alt="Zero Telemetry" />
    <img src="https://img.shields.io/badge/Code%20Standard-Zero%20%40Suppress%20Guarantee-purple.svg?style=flat-square" alt="Zero Suppress Guarantee" />
    <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License" />
  </p>

</div>

<br/>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Quick Install](#-quick-install)
- [Key Features](#-key-features)
- [Device & System Requirements](#-device--system-requirements)
- [Transport Comparison Matrix](#-transport-comparison-matrix)
- [User Quick Start Guide](#-user-quick-start-guide-4-simple-steps)
- [Procedural Vector Component Catalog](#-procedural-vector-component-catalog)
- [6-Axis Motion Steering & CemuHook DSU Setup](#-6-axis-motion-steering--cemuhook-dsu-setup)
- [Troubleshooting & Common Fixes](#-troubleshooting--common-fixes)
- [Subsystem Navigation](#-subsystem-navigation)
- [Developer & Build Guide](#-developer--build-guide)
- [Privacy & Security Transparency](#-privacy--security-transparency)
- [Legal Disclosures & Trademark Notice](#-legal-disclosures--trademark-notice)
- [License & Attribution](#-license--attribution)

---

## ⚡ Overview

**NEXPAD for Android** transforms your smartphone or tablet into an eSports-grade, ultra-low-latency virtual **Xbox 360 controller** and motion steering wheel for Windows PC gaming.

Built completely from scratch using **100% native Jetpack Compose** and hardware-accelerated **Canvas rendering**, NEXPAD eliminates raster bitmaps entirely. Every button, analog thumbstick, D-Pad, and trigger is rendered procedurally at up to **120 FPS** on ProMotion displays with harmonic spring physics, dynamic tactile haptics, and responsive RGB atmospheric blooms.

With four interchangeable transport engines—including zero-driver **Android Open Accessory (AOA 2.0)** USB direct streaming ($< 1\,\text{ms}$) and a high-speed **200 Hz** non-blocking UDP engine—NEXPAD pairs seamlessly with the [**NEXPAD Desktop**](https://github.com/parmarsanket/NEXPAD-Desktop) companion server to provide an authentic gaming experience across Steam, Xbox Game Pass, Epic Games, and console emulators.

---

## 🚀 Quick Install

Select your platform to download or install the pre-compiled production release:

### 📱 Mobile (Android Client)

| Platform | Download | Details |
|---|---|---|
| **Google Play Store** | [**Get it on Google Play**](https://play.google.com/store/apps/details?id=com.sanket.tools.nexpad) | Official native release for Android smartphones and tablets (supports Android 8.0+ / API 26–35). *(Coming Soon)* |
| **Standalone APK** | [**Download `NEXPAD-release.apk`**](https://github.com/parmarsanket/NEXPAD/releases/tag/v1.0.0) | **Direct Download:** Production signed standalone `.apk` for instant sideloading and offline installations. |

### 🖥️ Desktop (Windows Companion Server)

| Platform | Download | Details |
|---|---|---|
| **Windows Setup** | [**Download `NEXPAD_Desktop_Setup.exe`**](https://github.com/parmarsanket/NEXPAD-Desktop/releases/tag/v1.0.0) | **Recommended:** Complete Windows companion installer. Automatically bundles and configures the ViGEmBus kernel driver. |
| **Microsoft Store** | [**Get it from Microsoft Store**](https://github.com/parmarsanket/NEXPAD-Desktop/releases/tag/v1.0.0) | Official native package for Windows 10 & 11 via the Microsoft Store *(Coming Soon)*. |

### 📦 Standalone Packages & Assets

For direct downloads, offline installers, or manual installation, fetch the assets directly from GitHub Releases:

[![Download Standalone Binaries](https://img.shields.io/badge/GitHub%20Releases-Download%20v1.0.0%20Assets-2ea44f?style=for-the-badge&logo=github&logoColor=white)](https://github.com/parmarsanket/NEXPAD/releases/tag/v1.0.0)

**Standalone assets available on GitHub Releases:**
* 🤖 **Android Mobile APK**: `NEXPAD-release.apk` (Signed production release for Android 8.0+)
* 🪟 **Windows Companion Setup**: `NEXPAD_Desktop_Setup.exe` (Complete Windows All-in-One Installer)

---

## 🚀 Key Features

### 1. ⚡ Ultra-Low Latency Multi-Transport Engine
NEXPAD features an asynchronous, non-blocking I/O pipeline supporting four high-speed transports:
* **Zero-Driver USB (AOA 2.0 - Android Open Accessory)**: Connect directly via a standard USB-C cable. Delivers sub-millisecond ($< 1\,\text{ms}$) hardware transmission without enabling USB Debugging, ADB, or Root.
* **Instant Event-Driven Dispatch**: Button presses, releases, and analog triggers bypass periodic loops and dispatch immediately with zero frame delay ($< 1\,\text{ms}$).
* **200 Hz Continuous Stream Pipeline**: Transmits compact 44-byte binary Xbox controller reports at sustained **200 Hz** ($5\,\text{ms}$) over local 5GHz Wi-Fi or USB with sub-frame precision.
* **ADB Reverse TCP Bridge**: High-reliability fallback over standard Android Debug Bridge (`adb reverse tcp:9999 tcp:9999`).
* **Bluetooth RFCOMM (125 Hz / 8 ms)**: Optimal ACL slot-aligned wireless direct connection when local Wi-Fi infrastructure is unavailable.

### 2. 🎨 100% Procedural Vector Engine & 120 FPS Rendering
* **Zero Bitmaps**: All controls are rendered mathematically using Jetpack Compose `Canvas` and `DrawScope` paths, arcs, and gradients.
* **ProMotion 120 Hz / 90 Hz Display Support**: Fluid animations backed by damped harmonic spring kinematics (`stiffness = 440f`, `dampingRatio = 0.68f`).
* **7-Layer Canonical Display Stack**: Every component adheres to an unyielding 7-layer rendering hierarchy:
  1. *Layer 0: Kinetic Aura (`.drawBehind`)* — Atmospheric neon bloom expanding dynamically with interaction depth.
  2. *Layer 1: Recessed Bezel* — Concave structural well with ambient shadow occlusion.
  3. *Layer 2: Beveled Body Rim* — Micro-chamfered metallic or matte perimeter stroke.
  4. *Layer 3: Dynamic State Plane* — Spring-interpolated Z-axis physical depression.
  5. *Layer 4: Procedural Graphic Core* — Concentric machining grooves, liquid fill meniscus, radar sweeps, or VU meters.
  6. *Layer 5: Typography / Symbol* — High-contrast vector labels with back-face depth.
  7. *Layer 6: Specular Optical Sheen* — Micro-lens reflections and glass glare arcs.

### 3. 🎛️ Interactive HUD Studio & Layout Builder
* **Real-Time Layout Customizer**: Long-press any button or enter HUD Studio to move, scale ($50\%\dots 200\%$), and adjust opacity ($10\%\dots 100\%$) of any control on screen.
* **Grid Snapping & Symmetry Guides**: Align thumbsticks, ABXY clusters, and D-Pads with millimeter precision.
* **Built-in Presets**:
  * *Xbox 360 Default* — Classic asymmetrical ergonomic layout.
  * *Racing Sim* — Centralized steering arc with oversized throttle/brake trigger sliders.
  * *FPS / Tactical* — Clustered bumper/trigger controls for rapid twitch reflex aiming.
  * *Retro Arcade* — Symmetrical D-Pad and face button positioning for 2D platformers.
  * *Custom Profiles* — Create, export, and load endless bespoke layouts saved locally in encrypted DataStore.

### 4. 📳 Tactile Force-Feedback & Visual Game Rumble
* Ingests real-time 8-byte rumble motor packets (`GamepadFeedback`) sent by Windows PC games.
* Drives the smartphone's internal linear resonant actuator (LRA) or eccentric rotating mass (ERM) motor via Android's `Vibrator` / `VibratorManager` APIs.
* **Visual Rumble Micro-Jitter**: Buttons on screen physically vibrate with high-frequency micro-jitter in response to in-game explosions, collisions, and engine revs.

### 5. 🏎️ 6-Axis Motion Steering & CemuHook DSU Emulation
* Turns your phone into an ultra-responsive steering wheel for PC racing titles (*Forza*, *F1*, *Assetto Corsa*).
* Built-in **CemuHook DSU Motion Client**: Streams 6-axis gyroscope and accelerometer orientation vectors over UDP port `26760` to NEXPAD Desktop for console emulators (**Cemu**, **Yuzu**, **Ryujinx**, **RPCS3**, **Dolphin**).

---

## 📱 Device & System Requirements

| Requirement | Minimum Specification | Recommended Specification |
|---|---|---|
| **Android OS** | Android 8.0 (Oreo, API Level 26) | Android 12+ (API Level 31+) |
| **Processor** | Quad-core 1.8 GHz ARM64 | Octa-core 2.4 GHz ARM64 |
| **Display** | 60 Hz Full HD (1080p) | 90 Hz / 120 Hz AMOLED ProMotion |
| **RAM** | 2 GB | 4 GB or higher |
| **Sensors** | 3-axis Accelerometer | Hardware 6-axis Gyroscope + Accelerometer |
| **Connectivity** | 2.4 GHz Wi-Fi or USB 2.0 OTG | 5 GHz Wi-Fi 6 or USB-C 3.1 Direct (AOA 2.0) |

---

## 📊 Transport Comparison Matrix

| Connection Mode | Typical Latency | Jitter | Setup Complexity | Best For |
|---|---|---|---|---|
| **USB Direct (AOA 2.0)** | **$< 1\,\text{ms}$** | $< 0.1\,\text{ms}$ | **Zero-Config** (Plug-and-play USB-C cable) | Competitive eSports, fighting games, zero-interference gaming |
| **Wi-Fi (UDP 200 Hz)** | **$1.2\text{--}2.5\,\text{ms}$** | $< 0.8\,\text{ms}$ | **Low** (Same local Wi-Fi / Hotspot) | Couch gaming, racing titles, wireless freedom |
| **ADB Reverse TCP** | **$< 1\,\text{ms}$** | $< 0.2\,\text{ms}$ | **Medium** (Requires USB Debugging enabled) | Wired fallback if OEM disables AOA |
| **Bluetooth RFCOMM** | **$8\text{--}12\,\text{ms}$** | $< 2.0\,\text{ms}$ | **Medium** (Standard Bluetooth pairing) | Untethered gaming without local Wi-Fi infrastructure |

---

## 🕹️ User Quick Start Guide (4 Simple Steps)

### Step 1: Launch NEXPAD Desktop on Your PC
Download and run [**`NEXPAD_Desktop_Setup.exe`**](https://github.com/parmarsanket/NEXPAD-Desktop/releases/tag/v1.0.0) on your Windows PC. The setup automatically configures the bundled ViGEmBus kernel driver and Windows Firewall rules.

### Step 2: Open NEXPAD on Your Phone
Launch the NEXPAD app. Grant motion sensor and local network permissions when requested.

### Step 3: Choose Your Connection Mode
* **USB Connection (Best Performance — $< 1\,\text{ms}$)**:
  1. Plug your phone into your PC using a standard USB-C cable.
  2. In the NEXPAD app, tap **Connect USB (AOA)**.
  3. Accept the Android USB accessory prompt.
* **Wi-Fi Connection (Wireless — $1.2\text{--}2.5\,\text{ms}$)**:
  1. Ensure your phone and PC are connected to the same Wi-Fi network (5 GHz recommended).
  2. NEXPAD Desktop will display its local IP address (e.g., `192.168.1.150`).
  3. In the mobile app, tap **Connect Wi-Fi**, enter the IP address, and tap **Connect**.

### Step 4: Play Your Games!
Windows will sound the hardware chime as it mounts a virtual **Xbox 360 Controller**. Open Steam, Xbox Game Pass, Epic Games, or any emulator and start playing immediately!

---

## 🕹️ Procedural Vector Component Catalog

NEXPAD features over **42 custom-engineered procedural vector components**:

| Category | Available Variants | Unique Interactive Behaviors |
|---|---|---|
| **Action Buttons (ABXY)** | Realistic, Liquid, Facet, Flip, Ripple, Magnify, Orbit, Capsules, Eclipse | Sine-wave liquid fill, 3D 180° card-flip, counter-rotating planetary rings, 1.25x optical zoom, radial corona eclipse |
| **D-Pads** | Classic Cross, 3D Rocker, Lens D-Pad, Four Lenses D-Pad | Smooth 360° rocking tilt vector, laser chevrons, floating optical gem pods |
| **Thumbsticks (LS / RS)** | Precision Polar Thumbsticks | Dynamic deadzone gating, spring tension recentering, L3/R3 tactile click |
| **Analog Triggers (LT / RT)** | Classic Slider, VU Slabs, Liquid Chamber, Radar Target, Needle Gauge | Continuous $0\dots 255$ analog depression, VU audio-style LED steps, sweeping radar ping, physical dial sweep |
| **Bumpers (LB / RB)** | Ergonomic Curved Bumpers | Chamfered tactile edge with spring micro-bounce on actuation |
| **System Buttons** | Back, Start, Guide (Home), Profile Switcher | Optical lens system buttons with recessed wells and specular sheen arcs |

---

## 🏎️ 6-Axis Motion Steering & CemuHook DSU Setup

To enable motion steering or gyro aiming in console emulators:

1. In NEXPAD mobile, toggle **Motion Mode** to *Steering Wheel* or *Gyro Aiming*.
2. Open your emulator on PC (Cemu, Yuzu, Ryujinx, Dolphin):
   * Navigate to **Options $\to$ Input Settings $\to$ DSU Client**.
   * Set IP to `127.0.0.1` and Port to `26760`.
3. Your phone's gyroscope and accelerometer will immediately drive native motion controls.

---

## 🔧 Troubleshooting & Common Fixes

> [!TIP]
> Always verify that NEXPAD Desktop is running on your Windows PC before launching the mobile app.

* **USB AOA Prompt Not Appearing**:
  * Unplug and replug the USB cable. Ensure your Android USB mode is set to "File Transfer / MTP" rather than "Charging Only".
* **Wi-Fi Connection Refused**:
  * Verify that both PC and phone are on the same local subnet.
  * Check Windows Firewall: ensure inbound traffic on UDP port `9999` is allowed (the NSIS installer adds this automatically).
* **Game Not Detecting Controller**:
  * Check Windows `joy.cpl` (press `Win + R`, type `joy.cpl`, press Enter). Verify `Xbox 360 Controller for Windows` appears with status "OK".

---

## 🧭 Subsystem Navigation

```
                    ┌────────────────────────────┐
                    │      NEXPAD/README.md      │
                    │      (Android Client)      │
                    └─────────────┬──────────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
             HISTORY.md                    ARCHITECTURE.md
        "Where We Came From"                "How It Works"
```

- 📖 **[`HISTORY.md`](./HISTORY.md)** — Chronological engineering log of all 68 development milestones.
- 📝 **[`CHANGELOG.md`](./CHANGELOG.md)** — Semantic release notes and feature changelog for v1.0.0.
- 🏛️ **[`ARCHITECTURE.md`](./ARCHITECTURE.md)** — Deep technical specification of AOA endpoints, 200 Hz NIO pipeline, and vector canvas shaders.
- 🤖 **[`AGENTS.md`](./AGENTS.md)** — Machine-readable technical reference, boundaries, and deterministic commands for AI coding assistants.
- 🔒 **[`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md)** — Google Play Store compliant zero-telemetry privacy policy.
- ⚖️ **[`DISCLAIMER.md`](./DISCLAIMER.md)** — Nominative Fair Use, driver safety, and legal non-affiliation disclosure.
- 📄 **[`NOTICE`](./NOTICE)** — Third-party open-source attributions (AndroidX, Jetpack Compose, Kotlin, Ktor).

---

## 🛠️ Developer & Build Guide

### Prerequisites
* **JDK**: Eclipse Temurin or Amazon Corretto **JDK 21**
* **Android SDK**: Build Tools `35.0.0`, Platform SDK `35` (Android 15)
* **Gradle**: 8.x / 9.x (Wrapper included)

### Build Commands

```powershell
# Clone the repository
git clone https://github.com/parmarsanket/NEXPAD.git
cd NEXPAD

# Verify Kotlin compilation
.\gradlew.bat compileDebugKotlin

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK (outputs to app/build/outputs/apk/debug/)
.\gradlew.bat assembleDebug

# Assemble Release APK (requires signing credentials in local.properties)
.\gradlew.bat assembleRelease

# Assemble Release Android App Bundle (for Google Play Console)
.\gradlew.bat bundleRelease
```

---

## 🔒 Privacy & Security Transparency

NEXPAD is engineered with total respect for user privacy:
* **Zero Telemetry**: No analytics SDKs, no Google Analytics, no Firebase Crashlytics, and no ad SDKs.
* **100% Local Communication**: Network communication occurs exclusively between your phone and your PC on your local private subnet or via direct physical USB cable.
* **Permissions Transparency**:
  * `INTERNET` & `ACCESS_NETWORK_STATE`: Required strictly for local peer-to-peer UDP packet transmission to your PC.
  * `VIBRATE`: Required for tactile haptic feedback in response to game rumble.
  * `HIGH_SAMPLING_RATE_SENSORS`: Required to sample the gyroscope at 200 Hz ($5\,\text{ms}$) for steering and motion controls.
  * `WAKE_LOCK`: Prevents the screen from dimming or sleeping while you are actively playing games.

For full legal disclosures, read [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md).

---

## ⚖️ Legal Disclosures & Trademark Notice

NEXPAD is an independent, open-source software project. 

* **Non-Affiliation**: NEXPAD is not affiliated with, endorsed by, sponsored by, or associated with Microsoft Corporation, Sony Interactive Entertainment Inc., Nintendo Co., Ltd., Valve Corporation, or any of their subsidiaries.
* **Nominative Fair Use**: All mentions of **Xbox**, **Xbox 360**, **PlayStation**, **Nintendo Switch**, **Steam**, and associated controller layouts are used solely under the doctrine of **Nominative Fair Use** to accurately describe device compatibility and technical interoperability.
* For complete terms, see [`DISCLAIMER.md`](./DISCLAIMER.md).

---

## 📄 License & Attribution

This project is licensed under the **Apache License, Version 2.0**.
* See the full [LICENSE](./LICENSE) file for legal terms.
* Third-party open-source libraries and component attributions are documented in the [NOTICE](./NOTICE) file.

<br/>

<div align="center">
  <sub>Built with ❤️ by <b>Sanket Parmar</b> for the mobile and PC gaming community.</sub>
</div>
