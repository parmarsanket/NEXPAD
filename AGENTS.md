# 🤖 AGENTS.md — Machine-Readable Developer Guide for AI Agents

> This document defines architecture boundaries, system constraints, deterministic CLI commands, and code quality invariants for AI coding assistants (Google Antigravity, Claude Code, GitHub Copilot, Cursor, Devin, etc.) working on the **NEXPAD Android** codebase.

---

## 1. Project Context & Purpose

* **Repository**: `NEXPAD`
* **Target Runtime**: Android 8.0+ (API 26 — 35 / Android 15)
* **Language & Compiler**: Kotlin 2.x, JDK 21
* **UI Framework**: 100% Jetpack Compose (Compose BOM 2024.x, Material Design 3)
* **Graphics Engine**: Hardware-accelerated Skia `Canvas` / `DrawScope` (Zero Bitmaps)
* **Sensors**: Hardware IMU (`TYPE_ACCELEROMETER`, `TYPE_GYROSCOPE`, `TYPE_ROTATION_VECTOR`)
* **Transports**:
  * Android Open Accessory (AOA 2.0 via `UsbManager`, VID `0x18D1`, PID `0x2D00` / `0x2D01`)
  * High-Speed UDP Datagram socket (Port `9999`)
  * ADB Reverse TCP socket (`127.0.0.1:9999` and `127.0.0.1:9997`)
  * Bluetooth RFCOMM serial socket

---

## 2. Strict Invariants & Constraints (Negative Rules)

AI agents working on this codebase **MUST ALWAYS** obey the following rules:

1. **Zero Suppression Guarantee**:
   * **STRICTLY ZERO** `@Suppress` and `@SuppressLint` annotations across all Kotlin source files.
   * Never suppress warnings or type errors. Fix the underlying types, null-safety, or imports properly.
2. **Fixed 200 Hz Polling Rate**:
   * The network UDP streaming and timer scheduling pipeline is strictly tuned for **200 Hz** ($5\,\text{ms}$ interval).
   * Do not alter the 200 Hz rate or introduce arbitrary sleep intervals in high-speed input pipelines.
3. **Zero Raster Bitmaps in Gameplay**:
   * All button components, analog sticks, D-Pads, and triggers must remain 100% procedural vector graphics drawn mathematically via Jetpack Compose `Canvas` and `DrawScope`.
   * Never introduce static PNG/JPEG/WEBP assets for controller buttons.
4. **Zero Garbage Collection Allocation on Pointer Loop**:
   * Reusable buffers, matrix transforms, and coordinate points must be pre-allocated and mutated in-place inside high-frequency touch gesture callbacks.
5. **Zero Telemetry Guarantee**:
   * The application is 100% offline and local subnet only.
   * Never introduce analytics SDKs, advertising frameworks, crashlytics, or external network beacons.

---

## 3. Subsystem Map & Key Directories

```
NEXPAD/app/src/main/java/com/sanket/tools/nexpad/
├── model/                # Controller state models, layout presets, haptic tokens
├── network/              # AoaManager (USB), NetworkClient (200Hz UDP), AdbBridge, BtConnection
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

---

## 4. Deterministic Shell Commands

Agents must execute these exact Gradle commands for building, compiling, and testing:

```powershell
# Compile Kotlin debug sources for app module
.\gradlew.bat compileDebugKotlin

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK (outputs to app/build/outputs/apk/debug/)
.\gradlew.bat assembleDebug

# Assemble Release APK (requires signing credentials in local.properties)
.\gradlew.bat assembleRelease

# Assemble Release Android App Bundle (for Google Play Console submission)
.\gradlew.bat bundleRelease
```

---

## 5. Architectural Standards

* **Single Active Transport**:
  Managed via `IGamepadConnection`. Exactly one transport (`USB_AOA`, `USB_ADB`, `WIFI`, or `BLUETOOTH`) streams active gamepad reports at any given time.
* **CemuHook Motion Protocol**:
  Broadcasts on standard port **`26760`** using standard DSU packet structures (`0x44 0x53 0x55 0x53` = `DSUS`). Gyro quaternions and accelerometer coordinates are normalized in `MotionSensorManager`.
* **Bi-directional Haptics**:
  Receives 8-byte `PACKET_TYPE_FEEDBACK` packets from NEXPAD Desktop containing `largeMotor` ($0\dots 255$) and `smallMotor` ($0\dots 255$), translated into device vibration effects via `Vibrator` / `VibratorManager` and button visual micro-jitter.
