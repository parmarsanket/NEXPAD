# 📖 NEXPAD Development History & Milestone Timeline

This document details the chronological evolution of the **NEXPAD Android** application, mapping every development branch to its permanent Git Tag.

---

## 📅 Chronological Milestone Index

| Tag | Date & Time | Original Branch | Key Breakthroughs & Deliverables |
|:---:|:---:|---|---|
| **[`v0.1`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.1)** | `2026-05-31 03:41` | `init` | **Project Genesis**: Initial Android Studio project, Jetpack Compose setup, and package structure. |
| **[`v0.2`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.2)** | `2026-05-31 10:56` | `feature/testing-ui` | **Initial UI Exploration**: Prototyped virtual controller buttons and touch interaction. |
| **[`v0.3`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.3)** | `2026-06-01 06:07` | `feature/modularization-and-scalability` | **Architecture Refactor**: Separated UI composables, viewmodels, and input state models. |
| **[`v0.4`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.4)** | `2026-06-02 19:21` | `feature/hud-editor-3d-ui` | **3D HUD Editor Genesis**: Interactive drag, scale, and positioning canvas for HUD buttons. |
| **[`v0.5`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.5)** | `2026-06-03 12:00` | `feature/testing-and-bug-fixing` | **Event Pipeline Stabilization**: Fixed touch pointer consumption and gesture conflicts. |
| **[`v0.6`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.6)** | `2026-06-10 13:30` | `feature/implementation-refinement` | **Input Listener Precision**: Refined multi-touch tracking and button press states. |
| **[`v0.7`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.7)** | `2026-06-11 16:52` | `feature/implementation-refinement-networking` | **Network Client Pipeline**: Introduced asynchronous socket loop for fast packet transmission. |
| **[`v0.8`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.8)** | `2026-06-11 21:54` | `feature/BT_config` | **Bluetooth State Prototype**: Added initial Bluetooth configuration UI and permission handshakes. |
| **[`v0.9`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.9)** | `2026-07-21 19:20` | `feature/xinput-config` | **Xbox 360 Emulation Mapping**: Aligned input keys with official Xbox 360 digital and analog mappings. |
| **[`v0.10`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.10)** | `2026-08-03 08:31` | `feature/UI-Layout-improvement` | **Theme & Layout Upgrades**: Implemented layout versions 1 through 5 with dark theme palettes. |
| **[`v0.11`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.11)** | `2026-08-03 12:35` | `core/high-speed-binary-protocol` | **1000Hz Binary Protocol**: Replaced text/JSON with raw 44-byte ByteBuffer packets. |
| **[`v0.12`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.12)** | `2026-08-11 10:48` | `feature/network-resilience` | **Network Watchdog**: Added packet sequence counters, timeout detection, and auto-reconnect. |
| **[`v0.13`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.13)** | `2026-08-11 13:40` | `feature/honest-telemetry` | **Honest Telemetry System**: Real-time measurement of RTT latency, packet loss, and transmission jitter. |
| **[`v0.14`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.14)** | `2026-08-11 14:18` | `feature/dynamic-ui-state` | **Dynamic StateFlow UI**: Connected live network telemetry to Command Center HUD chips. |
| **[`v0.15`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.15)** | `2026-08-11 16:47` | `fix/deep-audit-networking-fixes` | **Deep Concurrency Audit**: Fixed socket buffer overflows, race conditions, and coroutine leakage. |
| **[`v0.16`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.16)** | `2026-08-11 17:47` | `feature/ultra-low-latency-polling` | **Ultra-Low Latency Polling**: Microsecond-precision input loop decoupled from Android UI thread. |
| **[`v0.17`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.17)** | `2026-08-12 19:52` | `fix/ui-layout-and-connection-watchdog` | **Crash Protection**: Resolved NullPointerExceptions and stale layout state overwrites on drag. |
| **[`v0.18`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.18)** | `2026-08-12 19:54` | `main` | **Production Baseline**: Synchronized master branch with all stabilized core network and HUD features. |
| **[`v0.19`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.19)** | `2026-08-15 22:41` | `feature/kmp-protocol-migration` | **KMP Protocol Integration**: Bound Android runtime to shared multiplatform protocol library. |
| **[`v0.20`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.20)** | `2026-08-24 01:17` | `feature/gravity-sensor-support` | **6-Axis Sensor & Steering**: Implemented 2D gyro steering angles and CemuHook DSU motion streaming. |
| **[`v0.21`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.21)** | `2026-08-24 03:51` | `feature/rumble-optimization-and-fixes` | **Haptic Vibration Tuning**: Inverted gyro axis fix and smooth haptic feedback actuation. |
| **[`v0.22`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.22)** | `2026-09-02 13:14` | `feature/haptic-engine-3-tier-upgrade` | **3-Tier Tactile Engine**: System-level haptic effects with intensity curves matching controller states. |
| **[`v0.23`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.23)** | `2026-09-03 17:38` | `feature/aoa-accessory` | **Android Open Accessory (AOA)**: Zero-driver direct USB streaming via `UsbManager.openAccessory`. |
| **[`v0.24`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.24)** | `2026-09-04 18:22` | `feature/feedback-packet` | **Bidirectional Rumble Packets**: Integrated 8-byte PC feedback flow into `GamepadViewModel`. |
| **[`v0.25`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.25)** | `2026-09-04 19:23` | `feature/aoa-bugfixes` | **AOA Connection Lifecycle**: Broadcast receiver for USB accessory attach/detach events. |
| **[`v0.26`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.26)** | `2026-09-04 21:31` | `feature/aoa-bugfixes-v2` | **AOA Multi-Device Stability**: Robust descriptor validation and reconnection retry backoff. |
| **[`v0.27`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.27)** | `2026-09-05 05:43` | `feature/adb-bridge` | **Dynamic ADB Port Forwarding**: Automatic fallback to reverse port forwarding when AOA is inactive. |
| **[`v0.28`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.28)** | `2026-09-06 03:57` | `feature/bt-connection` | **RFCOMM Sockets**: Full Bluetooth socket connection with UUID discovery. |
| **[`v0.29`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.29)** | `2026-09-07 05:34` | `feature/homescreen-logic-improvements` | **Command Center Redesign**: Glass card connection switchers, IP resolver, and diagnostics drawer. |
| **[`v0.30`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.30)** | `2026-09-09 05:58` | `feature/ui-logic-improvements-v2` | **Button Studio Architecture**: Component registry supporting Default, SVG, and Dynamic plugins. |
| **[`v0.31`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.31)** | `2026-09-09 13:19` | `feature/hud-metrics-and-plugins` | **Live HUD Performance Overlay**: In-game transparent HUD metrics chip and FTP plugin receiver. |
| **[`v0.32`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.32)** | `2026-09-10 01:25` | `feature/nxprc-html-css-engine` | **NXPRC Native Runtime**: `NxprcCanvasRenderer` delivering 100% Play Store compliant SDUI vector buttons. |
| **[`v0.33`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.33)** | `2026-09-11 02:40` | `fix/nxprc-engine-bugs` | **Low-Level Shader Parity**: Anisotropic conic gradients, offscreen compositing, and variable corner radii. |
| **[`v0.34`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.34)** | `2026-09-11 10:57` | `test/nxprc-engine-testing` | **Pixel Differential Verification**: Certified 93.5% parity against Headless Google Chrome rasterizer. |
| **[`v0.35`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.35)** | `2026-09-11 12:16` | `test/nxprc-engine-testing-v2` | **6 Gamepad Categories Audited**: ABXY Buttons, D-Pad, Triggers, Bumpers, Joysticks, and System buttons. |
| **[`v0.36`](https://github.com/parmarsanket/NEXPAD/releases/tag/v0.36)** | `2026-09-12 09:06` | `test/nxprc-engine-testing-v3` | **Universal Timeline Track Engine & SVG**: 120 FPS piecewise-linear track interpolation, joystick physics, and game rumble. |
