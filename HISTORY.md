# 📖 NEXPAD Development History & Branch Timeline

This document details the chronological evolution of the **NEXPAD Android** application across its development branches.

---

## 📅 Chronological Branch Milestone Index

| # | Date & Time | Branch Name | Key Breakthroughs & Deliverables |
|:---:|:---:|---|---|
| **01** | `2026-05-31 03:41` | [`init`](https://github.com/parmarsanket/NEXPAD/tree/init) | **Project Genesis**: Initial Android Studio project, Jetpack Compose setup, and package structure. |
| **02** | `2026-05-31 10:56` | [`feature/testing-ui`](https://github.com/parmarsanket/NEXPAD/tree/feature/testing-ui) | **Initial UI Exploration**: Prototyped virtual controller buttons and touch interaction. |
| **03** | `2026-06-01 06:07` | [`feature/modularization-and-scalability`](https://github.com/parmarsanket/NEXPAD/tree/feature/modularization-and-scalability) | **Architecture Refactor**: Separated UI composables, viewmodels, and input state models. |
| **04** | `2026-06-02 19:21` | [`feature/hud-editor-3d-ui`](https://github.com/parmarsanket/NEXPAD/tree/feature/hud-editor-3d-ui) | **3D HUD Editor Genesis**: Interactive drag, scale, and positioning canvas for HUD buttons. |
| **05** | `2026-06-03 12:00` | [`feature/testing-and-bug-fixing`](https://github.com/parmarsanket/NEXPAD/tree/feature/testing-and-bug-fixing) | **Event Pipeline Stabilization**: Fixed touch pointer consumption and gesture conflicts. |
| **06** | `2026-06-10 13:30` | [`feature/implementation-refinement`](https://github.com/parmarsanket/NEXPAD/tree/feature/implementation-refinement) | **Input Listener Precision**: Refined multi-touch tracking and button press states. |
| **07** | `2026-06-11 16:52` | [`feature/implementation-refinement-networking`](https://github.com/parmarsanket/NEXPAD/tree/feature/implementation-refinement-networking) | **Network Client Pipeline**: Introduced asynchronous socket loop for fast packet transmission. |
| **08** | `2026-06-11 21:54` | [`feature/BT_config`](https://github.com/parmarsanket/NEXPAD/tree/feature/BT_config) | **Bluetooth State Prototype**: Added initial Bluetooth configuration UI and permission handshakes. |
| **09** | `2026-07-21 19:20` | [`feature/xinput-config`](https://github.com/parmarsanket/NEXPAD/tree/feature/xinput-config) | **Xbox 360 Emulation Mapping**: Aligned input keys with official Xbox 360 digital and analog mappings. |
| **10** | `2026-08-03 08:31` | [`feature/UI-Layout-improvement`](https://github.com/parmarsanket/NEXPAD/tree/feature/UI-Layout-improvement) | **Theme & Layout Upgrades**: Implemented layout versions 1 through 5 with dark theme palettes. |
| **11** | `2026-08-03 12:35` | [`core/high-speed-binary-protocol`](https://github.com/parmarsanket/NEXPAD/tree/core/high-speed-binary-protocol) | **1000Hz Binary Protocol**: Replaced text/JSON with raw 44-byte ByteBuffer packets. |
| **12** | `2026-08-11 10:48` | [`feature/network-resilience`](https://github.com/parmarsanket/NEXPAD/tree/feature/network-resilience) | **Network Watchdog**: Added packet sequence counters, timeout detection, and auto-reconnect. |
| **13** | `2026-08-11 13:40` | [`feature/honest-telemetry`](https://github.com/parmarsanket/NEXPAD/tree/feature/honest-telemetry) | **Honest Telemetry System**: Real-time measurement of RTT latency, packet loss, and transmission jitter. |
| **14** | `2026-08-11 14:18` | [`feature/dynamic-ui-state`](https://github.com/parmarsanket/NEXPAD/tree/feature/dynamic-ui-state) | **Dynamic StateFlow UI**: Connected live network telemetry to Command Center HUD chips. |
| **15** | `2026-08-11 16:47` | [`fix/deep-audit-networking-fixes`](https://github.com/parmarsanket/NEXPAD/tree/fix/deep-audit-networking-fixes) | **Deep Concurrency Audit**: Fixed socket buffer overflows, race conditions, and coroutine leakage. |
| **16** | `2026-08-11 17:47` | [`feature/ultra-low-latency-polling`](https://github.com/parmarsanket/NEXPAD/tree/feature/ultra-low-latency-polling) | **Ultra-Low Latency Polling**: Microsecond-precision input loop decoupled from Android UI thread. |
| **17** | `2026-08-12 19:52` | [`fix/ui-layout-and-connection-watchdog`](https://github.com/parmarsanket/NEXPAD/tree/fix/ui-layout-and-connection-watchdog) | **Crash Protection**: Resolved NullPointerExceptions and stale layout state overwrites on drag. |
| **18** | `2026-08-12 19:54` | [`main`](https://github.com/parmarsanket/NEXPAD/tree/main) | **Production Baseline**: Synchronized master branch with all stabilized core network and HUD features. |
| **19** | `2026-08-15 22:41` | [`feature/kmp-protocol-migration`](https://github.com/parmarsanket/NEXPAD/tree/feature/kmp-protocol-migration) | **KMP Protocol Integration**: Bound Android runtime to shared multiplatform protocol library. |
| **20** | `2026-08-24 01:17` | [`feature/gravity-sensor-support`](https://github.com/parmarsanket/NEXPAD/tree/feature/gravity-sensor-support) | **6-Axis Sensor & Steering**: Implemented 2D gyro steering angles and CemuHook DSU motion streaming. |
| **21** | `2026-08-24 03:51` | [`feature/rumble-optimization-and-fixes`](https://github.com/parmarsanket/NEXPAD/tree/feature/rumble-optimization-and-fixes) | **Haptic Vibration Tuning**: Inverted gyro axis fix and smooth haptic feedback actuation. |
| **22** | `2026-09-02 13:14` | [`feature/haptic-engine-3-tier-upgrade`](https://github.com/parmarsanket/NEXPAD/tree/feature/haptic-engine-3-tier-upgrade) | **3-Tier Tactile Engine**: System-level haptic effects with intensity curves matching controller states. |
| **23** | `2026-09-03 17:38` | [`feature/aoa-accessory`](https://github.com/parmarsanket/NEXPAD/tree/feature/aoa-accessory) | **Android Open Accessory (AOA)**: Zero-driver direct USB streaming via `UsbManager.openAccessory`. |
| **24** | `2026-09-04 18:22` | [`feature/feedback-packet`](https://github.com/parmarsanket/NEXPAD/tree/feature/feedback-packet) | **Bidirectional Rumble Packets**: Integrated 8-byte PC feedback flow into `GamepadViewModel`. |
| **25** | `2026-09-04 19:23` | [`feature/aoa-bugfixes`](https://github.com/parmarsanket/NEXPAD/tree/feature/aoa-bugfixes) | **AOA Connection Lifecycle**: Broadcast receiver for USB accessory attach/detach events. |
| **26** | `2026-09-04 21:31` | [`feature/aoa-bugfixes-v2`](https://github.com/parmarsanket/NEXPAD/tree/feature/aoa-bugfixes-v2) | **AOA Multi-Device Stability**: Robust descriptor validation and reconnection retry backoff. |
| **27** | `2026-09-05 05:43` | [`feature/adb-bridge`](https://github.com/parmarsanket/NEXPAD/tree/feature/adb-bridge) | **Dynamic ADB Port Forwarding**: Automatic fallback to reverse port forwarding when AOA is inactive. |
| **28** | `2026-09-06 03:57` | [`feature/bt-connection`](https://github.com/parmarsanket/NEXPAD/tree/feature/bt-connection) | **RFCOMM Sockets**: Full Bluetooth socket connection with UUID discovery. |
| **29** | `2026-09-07 05:34` | [`feature/homescreen-logic-improvements`](https://github.com/parmarsanket/NEXPAD/tree/feature/homescreen-logic-improvements) | **Command Center Redesign**: Glass card connection switchers, IP resolver, and diagnostics drawer. |
| **30** | `2026-09-09 05:58` | [`feature/ui-logic-improvements-v2`](https://github.com/parmarsanket/NEXPAD/tree/feature/ui-logic-improvements-v2) | **Button Studio Architecture**: Component registry supporting Default, SVG, and Dynamic plugins. |
| **31** | `2026-09-09 13:19` | [`feature/hud-metrics-and-plugins`](https://github.com/parmarsanket/NEXPAD/tree/feature/hud-metrics-and-plugins) | **Live HUD Performance Overlay**: In-game transparent HUD metrics chip and FTP plugin receiver. |
| **32** | `2026-09-10 01:25` | [`feature/nxprc-html-css-engine`](https://github.com/parmarsanket/NEXPAD/tree/feature/nxprc-html-css-engine) | **NXPRC Native Runtime**: `NxprcCanvasRenderer` delivering 100% Play Store compliant SDUI vector buttons. |
| **33** | `2026-09-11 02:40` | [`fix/nxprc-engine-bugs`](https://github.com/parmarsanket/NEXPAD/tree/fix/nxprc-engine-bugs) | **Low-Level Shader Parity**: Anisotropic conic gradients, offscreen compositing, and variable corner radii. |
| **34** | `2026-09-11 10:57` | [`test/nxprc-engine-testing`](https://github.com/parmarsanket/NEXPAD/tree/test/nxprc-engine-testing) | **Pixel Differential Verification**: Certified 93.5% parity against Headless Google Chrome rasterizer. |
| **35** | `2026-09-11 12:16` | [`test/nxprc-engine-testing-v2`](https://github.com/parmarsanket/NEXPAD/tree/test/nxprc-engine-testing-v2) | **6 Gamepad Categories Audited**: ABXY Buttons, D-Pad, Triggers, Bumpers, Joysticks, and System buttons. |
| **36** | `2026-09-12 09:06` | [`test/nxprc-engine-testing-v3`](https://github.com/parmarsanket/NEXPAD/tree/test/nxprc-engine-testing-v3) | **Universal Timeline Track Engine & SVG**: 120 FPS piecewise-linear track interpolation, joystick physics, and game rumble. |

---

## 🔍 How to Browse Historical Code
To view any development branch directly on GitHub:
👉 **[github.com/parmarsanket/NEXPAD/branches](https://github.com/parmarsanket/NEXPAD/branches)**
*(Note: Older branches appear under the "Stale" tab or via branch search)*
