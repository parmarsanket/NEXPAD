# 🗺️ NEXPAD Android Roadmap

Strategic vision, architectural milestones, and upcoming technical tracks for the **NEXPAD Android** ecosystem.

```
                    ┌───────────────┐
                    │   README.md   │
                    │ "What is it?" │
                    └───────┬───────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
     HISTORY.md         ROADMAP.md       ARCHITECTURE.md
     "Where we         "Where we're       "How it
      came from"          going"           works"
```

---

## 🎯 Current Milestone: `v0.36` (Completed)
- **Universal Keyframe Timeline Track Engine**: 120 FPS continuous multi-property interpolation for color, glow, transforms, and gradients.
- **Full SVG Vector Geometry Support**: Native `<line>`, `<polyline>`, and `<polygon>` path compilation.
- **Physical Gamepad Mechanics**: 2D joystick deflection physics, analog trigger travel, and game rumble micro-jitter actuation.
- **Comprehensive Cross-Engine Verification**: Certified across ABXY, D-Pad, Thumbsticks, Bumpers, Triggers, and System buttons.

---

## 🚀 Near-Term Horizons (`v0.37` — `v0.40`)

### 1. Bluetooth Low Energy (BLE) HID Controller Profile
- Emulate standard HID Gamepad over BLE (`BluetoothHidDevice` API on Android 9+).
- Connect directly to Smart TVs, Steam Deck, Android TV, Raspberry Pi, and consoles with zero desktop companion app installed.
- Maintain fallback to custom NEXPAD binary RFCOMM protocol for PC client pairing.

### 2. Microsecond Dual-Motor Tactile Waveforms
- Modernize Android `Vibrator` to `VibratorManager` with `VibrationEffect.Composition` primitive haptics.
- Synthesize realistic dual-frequency rumble:
  - Low-frequency heavy off-balance motor (explosions, terrain rumble).
  - High-frequency light weight motor (reloads, UI clicks, engine rpm buzz).
- Low-latency haptic buffering with time-to-live expiration to prevent rumble desync over wireless hops.

### 3. Dynamic P2P HUD Sharing & QR Code Sync
- Instant layout sharing via compressed zlib QR codes: scan another player's phone to instantly clone their button layout.
- Direct Wi-Fi Direct / Nearby Share transmission of `.nxprc` packages and layout profiles.
- Integrated in-app community layout repository.

---

## 🔮 Long-Term Horizons (`v0.41`+)

### 4. Wi-Fi Direct / Local SoftAP Zero-Router Mode
- Autonomous Wi-Fi Direct group owner negotiation between Android phone and Windows PC.
- Sub-millisecond peer-to-peer Wi-Fi connection with zero router latency or channel interference.

### 5. Bidirectional Game Audio & Chat Passthrough
- Stream high-definition stereo game audio from PC to phone's 3.5mm headphone jack or wireless earbuds via Opus codec (< 15ms latency).
- Passthrough phone microphone to PC as a virtual recording device for Discord and game voice chat.

### 6. Hardware-Accelerated Vulkan Vector Pipeline
- Offload complex path tessellation and multi-pass blur shaders directly to Vulkan / RenderScript / Skia GPU pipelines for sub-millisecond draw overhead at 144Hz refresh rates.
