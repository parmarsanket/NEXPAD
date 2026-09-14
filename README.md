# 🎮 NEXPAD (Android Virtual Gamepad & HUD Engine)

**NEXPAD** turns your Android smartphone into an ultra-low latency, highly responsive virtual Xbox 360 controller with zero-driver USB connectivity, 6-Axis motion gyroscope steering, tactile force feedback rumble, and a vector HUD Studio engine.

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

## 🧭 Documentation Pillars

- 📖 **[`HISTORY.md`](./HISTORY.md)** — **Where We Came From**: Chronological timeline of all 36 development branches from Genesis (`init`) to the Universal Timeline Track Engine.
- 🗺️ **[`ROADMAP.md`](./ROADMAP.md)** — **Where We're Going**: Bluetooth LE HID, dynamic marketplace, and micro-second haptics.
- 🏛️ **[`ARCHITECTURE.md`](./ARCHITECTURE.md)** — **How It Works**: High-speed binary UDP pipeline, AOA USB streaming, NXPRC Compose Canvas rendering, and sensor math.

---

## 🚀 Key Features

1. **Multiple High-Speed Transports**:
   - **Zero-Driver USB (AOA)**: Connect via standard USB cable using Android Open Accessory mode without ADB or Root.
   - **Ultra-Low Latency UDP**: 1000Hz binary packet streaming over local 5GHz Wi-Fi with sub-millisecond precision.
   - **ADB Bridge & Bluetooth**: Seamless automatic fallback transports.
2. **Interactive 3D HUD Studio & Button Customization**:
   - Drag-and-drop layout builder with live scale, opacity, and coordinate alignment.
   - Button Studio supporting native vector components, custom SVG shapes, and `.nxprc` dynamic packages.
3. **Universal Keyframe Timeline Track Engine**:
   - Renders complex animated buttons, pulsing glows, radar sweeps, and RGB rainbow loops at $120\,\text{FPS}$ using Compose Canvas.
4. **Physical Controller Mechanics & Rumble**:
   - Spring-centered 2D joystick deflection ($LS, RS$) with tension physics.
   - Continuous analog trigger depression travel ($LT, RT$).
   - Real-time Game Rumble visual micro-jitter driven by PC motor vibration packets.
5. **6-Axis Motion & Gyroscope Steering**:
   - High-precision steering wheel emulation for racing games and CemuHook DSU motion controls for emulators.

---

## 🛠️ Build & Run

```bash
# Debug compilation
./gradlew compileDebugKotlin

# Assemble Debug APK
./gradlew assembleDebug
```