# 📝 Changelog

All notable changes to the **NEXPAD Android** client application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-10-25

### 🚀 Added
- **100% Procedural Vector Graphics Engine**:
  - Full hardware-accelerated Jetpack Compose Canvas rendering operating up to **120 FPS** on ProMotion displays.
  - Zero raster bitmap dependencies: all buttons, thumbsticks, and triggers are mathematically drawn using path geometry, sweep shaders, and dynamic vector arcs.
  - 7-Layer canonical rendering stack featuring atmospheric kinetic aura bloom, recessed bezels, beveled body rims, dynamic Z-axis depression, and specular optical sheen.
- **Ultra-Low Latency Multi-Transport Engine**:
  - **Android Open Accessory (AOA 2.0)**: Direct USB-C streaming with dedicated background TX/RX threads delivering $< 1\,\text{ms}$ input latency without USB debugging or root.
  - **High-Speed UDP Client**: Non-blocking `DatagramSocket` executing on pinned `Dispatchers.IO` coroutines at sustained **200 Hz** ($5\,\text{ms}$ ticks) with microsecond RTT and jitter telemetry.
  - **ADB Reverse TCP Bridge**: High-reliability fallback over standard Android Debug Bridge (`adb reverse tcp:9999 tcp:9999`).
  - **Bluetooth RFCOMM**: Direct serial socket wireless connection with bufferbloat mitigation and one-way input latency tracking.
- **Interactive HUD Studio & Layout Builder**:
  - Drag-and-drop visual layout editor allowing custom placement, scaling ($50\%\dots 200\%$), and opacity tuning ($10\%\dots 100\%$) for every control.
  - Built-in presets: *Xbox 360 Default*, *Racing Sim*, *FPS / Tactical*, and *Retro Arcade*.
  - Endless custom layout profiles saved securely via Android Jetpack DataStore.
- **Over 42 Bespoke Procedural Component Variants**:
  - Action Buttons (ABXY): Realistic, Liquid, Facet, Flip, Ripple, Magnify, Orbit, Capsules, and Eclipse.
  - Directional Pads: Classic Cross, 3D Rocker, Lens D-Pad, and Four Lenses D-Pad.
  - Thumbsticks: Precision Polar Thumbsticks with dynamic deadzones and axial L3/R3 tactile clicks.
  - Analog Triggers: Continuous $0\dots 255$ sliders, VU Slabs, Liquid Chamber, Radar Target, and Needle Gauge.
- **Tactile Force-Feedback & Visual Game Rumble**:
  - Ingests real-time 8-byte rumble packets from Windows PC games and actuates phone vibration motors via `Vibrator` / `VibratorManager`.
  - Visual rumble micro-jitter: buttons physically vibrate on screen during high-intensity in-game collisions and explosions.
- **6-Axis Motion Steering & CemuHook DSU Protocol**:
  - Accelerometer and gyroscope fusion for realistic PC racing wheel steering.
  - Built-in CemuHook DSU motion client broadcasting 6-axis orientation vectors over UDP port `26760` to console emulators (Cemu, Yuzu, Ryujinx, Dolphin).
- **NXPRC Native Runtime**:
  - 100% Google Play compliant Server-Driven UI (SDUI) vector package interpreter.
  - One-click package synchronization via local FTP (port `9996`) or ADB from NEXPAD Desktop.
- **eSports Multi-Touch Coordinator**:
  - Pinned zero-allocation touch event loop with eSports tournament input mode toggle.

### ⚡ Changed
- Standardized all network input streaming and sensor sampling rates strictly to **200 Hz** ($5\,\text{ms}$ intervals).
- Transitioned network payload serialization to high-speed 44-byte binary `ByteBuffer` packet structures (`NexpadProtocol`).
- Modernized navigation backstack with `rememberNavBackStack` and predictive pop animations.

### 🛡️ Fixed
- Eliminated Android UI thread ANR freezes during rapid joystick gestures through zero-allocation pointer normalization.
- Calibrated DualShock 4 motion stillness drift filter and aligned 6-axis gyro/accelerometer coordinate mapping.
- Resolved layout state race conditions and persistent DataStore profile overwrites on navigation.
- Added cold-start USB accessory attachment support in `MainActivity.onCreate`.
