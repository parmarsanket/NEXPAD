---
description: Comprehensive architecture map and index for NEXPAD (Android) and NEXPADDesktop (Compose Multiplatform). Consult to locate modules, protocols, and contracts without reading large files.
always_on: true
---

# NEXPAD Architecture & Symbol Map

This repository contains two primary modules:
1. **NEXPAD (`NEXPAD/`)**: Android Gamepad Controller App (Jetpack Compose, Native USB/UDP, Remote Compose SDUI).
2. **NEXPADDesktop (`NEXPADDesktop/`)**: Desktop Gamepad Server & Plugin Studio (Compose Multiplatform JVM, LibWDI/WinUSB, FTP Server, HTML/CSS-to-NXPRC Compiler).

---

## 1. Network & Hardware Protocols

### Gamepad Input Protocol (`NexpadProtocol.kt`)
- **UDP Controller Stream**: Default port `9997` (or dynamically configured).
- **Packet Structure**: 44-byte `ByteBuffer` Little-Endian:
  - Sequence number, Timestamp, Digital Button bitmasks (A, B, X, Y, DPad, Triggers, Bumpers, Sticks).
  - Analog axes: Left Stick (X, Y), Right Stick (X, Y), Left Trigger, Right Trigger.
- **Haptic/Rumble Feedback**: 8-byte packet returned from Desktop to Android.

### USB AOA (Android Open Accessory) & Dynamic WinUSB
- **Desktop AOA Pipeline**: `NEXPADDesktop/desktopApp/.../network/AoaManager.kt` and `AoaTransport.kt`.
- **Driver Broker**: `service/DriverBrokerService.kt` & `LibWdiBinding.kt` (WinUSB driver swap via Named Pipe `\\.\pipe\nexpad-driver-ipc`).
- **Android Accessory Connection**: `NEXPAD/app/.../network/AoaAccessoryConnection.kt` (claims accessory `Nexpad / Nexpad Controller 1.0`, VID `0x18D1` PID `0x2D00`).

### Plugin FTP Transfer Pipeline
- **Port**: `9996` (`FTP_PORT`).
- **Transfer**: Chunked transfer (`encodeFtpStart`, `encodeFtpChunk`, `encodeFtpComplete`). Pushes `.nxprc` binary and companion `.json` metadata to `/data/data/.../files/nxp_remote/`.

---

## 2. Remote Compose SDUI (Tier 2 Plugin System)

Binary format `.nxprc` allows rich button authoring in HTML/CSS on Desktop with native Skia/Android Canvas rendering.

### Key Data Models (`shared/.../nxprc/`)
- `NxprcDocument`: Root document containing `NxprcManifest` and `NxprcCanvas`.
- `CanvasLayer`:
  - `BoxLayer`: Keycaps, bezels, surfaces (supports multiple fills, stroke, inset/drop shadows, border-radius).
  - `CenterGlyph`: Centered text/symbol (font-size, color, multi-tier text-shadows).
  - `VectorPath`: SVG path data emblems (`M... Z`, fills, strokes).
  - `GlowRing`: Atmospheric neon/glow aura (blur radius, pulse animation).
  - `GlossReflection` / `InnerShadow`: Pseudo-element specular highlights and depth.

### Desktop Authoring & Compiler (`NEXPADDesktop/desktopApp/.../plugins/`)
- `NxprcHtmlCssConverter.kt`: Converts HTML/CSS markup into optimized `NxprcDocument`.
- `NxprcPresets.kt`: Pre-built hardware button presets (Neo Tactile, Cyberpunk, D-Pads, Analog Sticks, Triggers).
- `NxprcLayerCodeGenerator.kt`: Decomposes `CanvasLayer` into readable CSS rules, synthetic selectors (`.layer-X-glyph`), and single-layer AI prompts.
- `NxprcSurgicalReplacer.kt`: Splicing engine that applies AI/hand edits to single layers in `htmlSource` while preserving untouched layers and markup.
- `DesktopPluginManager.kt`: ADB push and FTP streaming for `.nxprc` and metadata.

### Android Runtime (`NEXPAD/app/.../runtime/`)
- `RemoteComponentRegistry.kt`: Scans `nxp_remote/` directory, loads binary `.nxprc` documents and metadata.
- `RemoteComposeRenderer.kt`: Renders `.nxprc` layers on Compose canvas and wires touch gesture inputs (`detectTapGestures`) to gamepad input targets.
- `ControllerElementRenderer.kt`: Dispatches rendering: Tier 2 Remote Compose -> Tier 1 JSON interpreter -> Native hardware renderers.

---

## 3. UI & Studio Screen Hierarchy

### Desktop (`NEXPADDesktop/desktopApp/.../ui/`)
- `PluginsScreen.kt`: Tab 0 (NXP Components), Tab 1 (Remote Compose Studio). Hosts live preview sandbox, preset selector, and Layer Studio modal.
- `designer/LayerStudioFullScreen.kt` & `LayerStudioPanel.kt`:
  - **Zone 1 (Left)**: Layer stack hierarchy, reordering, visibility toggles.
  - **Zone 2 (Center)**: Isolated layer stage & full live interactive sandbox.
  - **Zone 3 (Right)**: Monospace code editor, AI copilot prompt builder, quick actions (`Edit Direct`, `Paste`), and `⚡ Apply Code & Update Live Preview`.

### Android (`NEXPAD/app/.../ui/`)
- `VirtualControllerScreen.kt`: Main landscape gameplay HUD canvas.
- `HudEditorScreen.kt`: Visual drag-and-drop layout builder (drag, resize, snap-to-grid, assign skins).
- `studio/ButtonStudioScreen.kt`: Skin selector modal (Default, SVG, Tier 1 Plugin, Tier 2 Remote Compose).

---

## 4. Token Conservation & Coding Rules

1. **Avoid Viewing Giant Files in Full**:
   - Files like `NxprcHtmlCssConverter.kt` exceed 1,000 lines. Never use `view_file` on the entire file.
   - Always use `grep_search` to find the exact target function or class, then view only the required 50-100 lines using `StartLine` and `EndLine`.
2. **Surgical Line Editing**:
   - Always use `replace_file_content` targeting small, contiguous blocks (20-50 lines) rather than rewriting large files.
3. **Subagent Delegation for Research**:
   - For web searches, new API lookups, or deep investigation, invoke a `research` subagent with `Model: flash` to keep the main conversation context clean and token-light.
