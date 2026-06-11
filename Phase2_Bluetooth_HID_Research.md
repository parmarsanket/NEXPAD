# 🎮 NEXPAD Phase 2: Bluetooth HID — Complete Research & Implementation Guide

> **Last Updated:** June 2026  
> **Status:** Research Complete → Ready for Implementation  
> **API:** Android `BluetoothHidDevice` (API Level 28+)

---

## Table of Contents
1. [Overview](#overview)
2. [Technology: Android BluetoothHidDevice API](#technology-android-bluetoothhiddevice-api)
3. [Device Compatibility Matrix](#device-compatibility-matrix)
4. [Console Authentication Research](#console-authentication-research)
5. [Play Store Apps — How They Actually Work](#play-store-apps--how-they-actually-work)
6. [Open-Source Reference Projects](#open-source-reference-projects)
7. [ESP32 Libraries Feature Analysis](#esp32-libraries-feature-analysis)
8. [Features We Can Port to Android](#features-we-can-port-to-android)
9. [NEXPAD HID Descriptor Design](#nexpad-hid-descriptor-design)
10. [Implementation Architecture](#implementation-architecture)
11. [Permissions & Requirements](#permissions--requirements)
12. [Latency Optimization Strategies](#latency-optimization-strategies)
13. [Implementation Roadmap](#implementation-roadmap)

---

## Overview

Phase 2 adds **Bluetooth HID Mode** to NEXPAD, allowing the phone to act as a **native wireless gamepad** — no companion app needed on the receiving device. The phone will register as a standard Bluetooth HID controller, just like a real Xbox/PlayStation controller.

### Connection Modes (Complete Picture)

| Mode | Phase | Technology | Companion App? | Best For |
|:---|:---:|:---|:---:|:---|
| WiFi (UDP) | Phase 1 ✅ | Ktor + ViGEmBus | Yes (Desktop) | PC Gaming — lowest latency on local network |
| USB Tethering | Phase 1 ✅ | ADB + ViGEmBus | Yes (Desktop) | PC Gaming — zero latency wired |
| **Bluetooth HID** | **Phase 2** | **BluetoothHidDevice API** | **No** | **TVs, Macs, PCs, Tablets — universal** |

---

## Technology: Android BluetoothHidDevice API

### API Details

| Detail | Info |
|:---|:---|
| **Full Class** | `android.bluetooth.BluetoothHidDevice` |
| **Package** | `android.bluetooth` |
| **Added in** | API Level 28 (Android 9 Pie — August 2018) |
| **Last Updated** | No functional updates since release (stable & mature) |
| **Status** | ✅ Production-ready, battle-tested |
| **Documentation** | [developer.android.com/reference/android/bluetooth/BluetoothHidDevice](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice) |

### Why This API?

- **Only option** for making Android phone act as a Bluetooth HID device
- Built into every Android 9+ phone — no extra library needed
- No root required, no hidden APIs
- Every Bluetooth gamepad app (Pocket-Pad, BlueHID, etc.) uses this same API
- Stable since 2018 — won't break with Android updates

### Are There Better Alternatives?

| Option | Better? | Usable in NEXPAD? | Why Not |
|:---|:---:|:---:|:---|
| BLE HID (HOGP) | ⚠️ Lower power | ❌ | Inconsistent across hosts, especially TVs |
| ESP32-BLE-Gamepad | ✅ Lower latency | ❌ | Hardware library for ESP32 microchips, not Android phones |
| ESP32-BLE-CompositeHID | ✅ More features | ❌ | Same — hardware only |
| Hidden/Root APIs | ❌ Worse | ❌ | Requires root, unreliable |
| RFCOMM Socket | ❌ Worse | ❌ | Requires app on BOTH devices |

**Conclusion:** `BluetoothHidDevice` is the ONLY door to Bluetooth HID on Android. No alternative exists.

### Core API Methods

```kotlin
// 1. Get the Bluetooth HID Device proxy
bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)

// 2. Register as a gamepad
hidDevice.registerApp(sdpSettings, null, null, executor, callback)

// 3. Connect to a device
hidDevice.connect(bluetoothDevice)

// 4. Send button/stick data
hidDevice.sendReport(device, reportId, reportData)

// 5. Receive rumble/vibration from host
// → Handled in callback.onSetReport() or callback.onInterruptData()

// 6. Disconnect
hidDevice.disconnect(device)

// 7. Unregister
hidDevice.unregisterApp()
```

---

## Device Compatibility Matrix

### ✅ WORKS — Standard Bluetooth HID (No Special Auth)

| Device | Status | Notes |
|:---|:---:|:---|
| **Windows PC** | ✅ YES | Recognized as native gamepad in Windows Settings |
| **Android TV** | ✅ YES | Works for gaming and navigation |
| **Amazon Fire TV / Stick** | ✅ YES | Android-based, same BT HID support |
| **MacBook / Mac** | ✅ YES | macOS supports standard BT HID gamepads |
| **iPad / iPhone** | ✅ YES | iOS 13+ supports BT HID gamepads |
| **Linux PC** | ✅ YES | Native BT HID support via BlueZ |
| **Steam Deck** | ✅ YES | Linux-based, standard BT HID |
| **Chromebook** | ✅ YES | ChromeOS supports BT HID |
| **Samsung Smart TV (Tizen)** | ✅ YES | Supports BT HID gamepads |
| **LG Smart TV (webOS)** | ✅ YES | Supports BT HID gamepads |
| **Raspberry Pi** | ✅ YES | Linux-based |
| **Meta Quest VR** | ✅ YES | Android-based |

### ❌ DOES NOT WORK — Console Authentication Barriers

| Console | Status | Reason |
|:---|:---:|:---|
| **PS4** | ❌ NO | Hardware crypto chip (NXP A710x), challenge-response every 4 sec |
| **PS5** | ❌ NO | Stricter DualSense authentication, hardware DRM |
| **Xbox Series X/S** | ❌ NO | Doesn't use Bluetooth at all — proprietary Xbox Wireless Protocol |
| **Xbox One** | ❌ NO | Same — proprietary wireless, not Bluetooth |
| **Nintendo Switch** | ⚠️ MAYBE | Uses BT HID but with proprietary handshake — fragile, can break with firmware updates |

---

## Console Authentication Research

### PlayStation 4 / PS5 — Why It's Impossible

```
PS4/PS5 Bluetooth Connection Flow:

1. Controller broadcasts → Console discovers
2. Standard BT pairing (this part works normally)
3. Console sends CRYPTO CHALLENGE packet every ~4 seconds
4. Controller's HARDWARE CHIP (NXP A710x) signs the response with SHA-1
5. If response is invalid → Console IGNORES the controller
6. No software can fake this — keys are burned into physical silicon
```

**Key Facts:**
- Sony uses **periodic challenge-response** (every ~4 sec via Bluetooth)
- Requires dedicated **NXP A710x security chip** inside the controller
- Uses **SHA-1 based cryptographic signing** — proprietary to Sony
- PS5 is even stricter — DualShock 4 can't play native PS5 games (wrong auth chip)
- **No open-source bypass exists.** Only hardware adapters (Cronus Zen, Brook Wingman) work

### Xbox Console — Why It's Impossible

```
Xbox Controller Connection:

Phone (Bluetooth) ──✗──→ Xbox Console
                         ↑
Xbox Controller ─────────┘ (Xbox Wireless Protocol — NOT Bluetooth)
```

**Key Facts:**
- Xbox uses a **completely separate proprietary wireless protocol** (not Bluetooth)
- Xbox controllers have **TWO radios**: Bluetooth (for PC/Phone) + Xbox Wireless (for console)
- Only **"Designed for Xbox" licensed** controllers can connect wirelessly to Xbox
- Microsoft actively **blocks unauthorized adapters** via firmware updates

### Nintendo Switch — Theoretically Possible (But Hard)

**Key Facts:**
- Switch uses standard **Bluetooth HID profile** ✅
- BUT requires a **proprietary handshake** mimicking Joy-Con/Pro Controller
- Console polls controllers every **~15ms** — drops connections that don't respond correctly
- Community has **reverse-engineered** much of the protocol (GitHub repos available)
- Third-party controllers work by mimicking Pro Controller's firmware packets
- **Fragile:** Nintendo can break compatibility with firmware updates

---

## Play Store Apps — How They Actually Work

> **IMPORTANT:** No Play Store app connects to PS4/PS5/Xbox as a Bluetooth controller. They use WiFi streaming.

### Method 1: PS Remote Play (Official)
- Sony's official app
- **WiFi streaming** — console streams video to phone, phone sends touch inputs back
- NOT a Bluetooth controller connection
- Requires same WiFi network + PSN login

### Method 2: Chiaki / Chiaki-ng (Open Source)
- Open-source alternative to PS Remote Play
- Reverse-engineers Sony's **Remote Play streaming protocol**
- Authenticates via PSN Account ID + console PIN
- **WiFi streaming**, NOT Bluetooth
- GitHub: [github.com/streetpea/chiaki-ng](https://github.com/streetpea/chiaki-ng)

### Method 3: Fake/Misleading Apps
- Many apps claiming "PS4 Controller" are:
  - Remote Play wrappers
  - WiFi-based apps requiring receiver app on the other device
  - Completely fake/scam apps

---

## Open-Source Reference Projects

### 1. Pocket-Pad ⭐ (Most Relevant)
- **Repo:** [github.com/Bifcen/Pocket-Pad](https://github.com/Bifcen/Pocket-Pad)
- **What:** Android app → Bluetooth HID gamepad
- **API Used:** Same `BluetoothHidDevice` we'll use
- **Features:** Native controller recognition, no host software needed, low latency
- **Compatibility:** Windows 10+, Android TV, Mac, Linux
- **What to study:** HID descriptor bytes, connection flow, report sending

### 2. BlueHID
- **Repo:** [github.com/ralismark/bluehid](https://github.com/ralismark/bluehid)
- **What:** Earlier proof-of-concept for Android BT HID
- **Good for:** Understanding the low-level API mechanics

### 3. Chiaki-ng (For Reference Only)
- **Repo:** [github.com/streetpea/chiaki-ng](https://github.com/streetpea/chiaki-ng)
- **What:** Open-source PS Remote Play client
- **NOT relevant** for Bluetooth controller — uses WiFi streaming

### Key Takeaway
All these apps use the **exact same Android API** — `BluetoothHidDevice`. The only difference between them is the **HID descriptor byte array** and the **UI**. NEXPAD already has a superior UI from Phase 1.

---

## ESP32 Libraries Feature Analysis

### ESP32-BLE-Gamepad (by lemmingDev)
**Repo:** [github.com/lemmingDev/ESP32-BLE-Gamepad](https://github.com/lemmingDev/ESP32-BLE-Gamepad)

| Feature | Details |
|:---|:---|
| **Buttons** | Up to 128 buttons |
| **Hat Switches** | Up to 4 POV hats (D-Pad) |
| **Axes** | 6 axes, configurable up to 16-bit (X, Y, Z, Rx, Ry, Rz) |
| **Sliders** | 2 sliders, configurable up to 16-bit |
| **Simulation Controls** | Rudder, throttle, accelerator, brake, steering |
| **HID Descriptor** | Fully configurable |
| **Custom VID/PID** | Customizable Vendor/Product IDs |
| **Battery Reporting** | Built-in battery level reporting |
| **BT Stack** | NimBLE (optimized, fast, lightweight) |
| **Special Buttons** | Start, Select, Menu, Home, Volume controls |
| **Compatibility** | Windows, Android, Linux (NOT iOS) |

### ESP32-BLE-CompositeHID (by Mystfit)
**Repo:** [github.com/Mystfit/ESP32-BLE-CompositeHID](https://github.com/Mystfit/ESP32-BLE-CompositeHID)

| Feature | Details |
|:---|:---|
| **Composite Device** | Gamepad + Mouse + Keyboard simultaneously |
| **XInput Mode** | Act as an Xbox controller |
| **Rumble/Vibration** | Host sends vibration data → strong + weak motors |
| **Custom Report Maps** | Define completely custom HID reports |
| **Multiple Report IDs** | Different data streams in one connection |
| **NimBLE** | Same optimized BT stack |

---

## Features We Can Port to Android

### ✅ CAN Port (90% of features)

These features are ALL just **HID descriptor configurations** — the byte array is universal regardless of hardware:

| ESP32 Feature | Port to Android? | How to Implement |
|:---|:---:|:---|
| 128 Buttons | ✅ YES | Expand `USAGE_MAXIMUM` in descriptor |
| 4 Hat Switches (D-Pad) | ✅ YES | Add Hat Switch usage in descriptor |
| 6 Axes (16-bit precision) | ✅ YES | Define X, Y, Z, Rx, Ry, Rz with 16-bit `REPORT_SIZE` |
| 2 Sliders | ✅ YES | Add Slider usage in descriptor |
| Simulation Controls | ✅ YES | Add Simulation usage page (0x02) to descriptor |
| Configurable HID Descriptor | ✅ YES | `byteArrayOf()` passed to `registerApp()` |
| Battery Reporting | ✅ YES | Android `BluetoothHidDevice` has battery reporting |
| Special Buttons (Start, Select, Home) | ✅ YES | Add as standard button usages |
| **Composite Device (Gamepad + Mouse + Keyboard)** | ✅ YES | Use multiple Report IDs in ONE descriptor |
| **Rumble/Vibration from Host** | ✅ YES | Handle `onSetReport()` callback → `phone.vibrate()` |
| Custom Report Maps | ✅ YES | The HID descriptor IS the report map |
| **XInput Descriptor** | ⚠️ PARTIAL | Can use Xbox-style descriptor bytes, but BLE support varies by host |

### ❌ CANNOT Port (Hardware-Level)

| ESP32 Feature | Port? | Why Not |
|:---|:---:|:---|
| NimBLE Stack | ❌ NO | Android controls its own BT stack — can't replace it |
| Custom VID/PID | ❌ NO | Phone's BT chip hardware controls this |
| Low-level connection parameters | ❌ NO | Android OS manages BT connection intervals |

### The Core Insight

```
ESP32 C++ Code:                      NEXPAD Kotlin Code:

uint8_t descriptor[] = {             val descriptor = byteArrayOf(
  0x05, 0x01,  // Usage Page           0x05.toByte(), 0x01.toByte(),
  0x09, 0x05,  // Gamepad              0x09.toByte(), 0x05.toByte(),
  ...                                  ...
};                                   )

NimBLE.setHidDescriptor(descriptor)  hidDevice.registerApp(..., descriptor, ...)
         ↓                                      ↓
    SAME BYTES ──────────────────────── SAME BYTES
    SAME RESULT ─────────────────────── SAME RESULT
```

**The HID descriptor is a universal standard (USB-IF). The same bytes work whether sent from an ESP32 chip or from an Android phone.**

---

## NEXPAD HID Descriptor Design

### Full Gamepad Descriptor (Inspired by ESP32 Libraries)

```kotlin
/**
 * NEXPAD Bluetooth HID Gamepad Descriptor
 * 
 * Capabilities:
 * - 16 Buttons (A, B, X, Y, LB, RB, LT, RT, Select, Start, L3, R3, Home, Share, + 2 extra)
 * - 4 Axes: Left Stick X/Y, Right Stick X/Y (16-bit precision: -32768 to 32767)
 * - 2 Triggers: LT, RT (8-bit: 0-255)
 * - 1 Hat Switch: D-Pad (8 directions)
 * - Rumble Output: 2 motors (strong + weak) — host can send vibration to phone
 */
val NEXPAD_HID_DESCRIPTOR = byteArrayOf(
    // ===== GAMEPAD COLLECTION =====
    0x05, 0x01,                  // USAGE_PAGE (Generic Desktop)
    0x09, 0x05,                  // USAGE (Gamepad)
    0xA1.toByte(), 0x01,         // COLLECTION (Application)
    0x85.toByte(), 0x01,         //   REPORT_ID (1) — Gamepad Input

    // ----- 16 Buttons -----
    0x05, 0x09,                  //   USAGE_PAGE (Button)
    0x19, 0x01,                  //   USAGE_MINIMUM (Button 1)
    0x29, 0x10,                  //   USAGE_MAXIMUM (Button 16)
    0x15, 0x00,                  //   LOGICAL_MINIMUM (0)
    0x25, 0x01,                  //   LOGICAL_MAXIMUM (1)
    0x75, 0x01,                  //   REPORT_SIZE (1 bit per button)
    0x95.toByte(), 0x10,         //   REPORT_COUNT (16 buttons)
    0x81.toByte(), 0x02,         //   INPUT (Data, Variable, Absolute)

    // ----- 4 Axes: Left Stick + Right Stick (16-bit precision) -----
    0x05, 0x01,                  //   USAGE_PAGE (Generic Desktop)
    0x09, 0x30,                  //   USAGE (X)  — Left Stick X
    0x09, 0x31,                  //   USAGE (Y)  — Left Stick Y
    0x09, 0x32,                  //   USAGE (Z)  — Right Stick X
    0x09, 0x35,                  //   USAGE (Rz) — Right Stick Y
    0x16, 0x00, 0x80.toByte(),   //   LOGICAL_MINIMUM (-32768)
    0x26, 0xFF.toByte(), 0x7F,   //   LOGICAL_MAXIMUM (32767)
    0x75, 0x10,                  //   REPORT_SIZE (16 bits per axis)
    0x95.toByte(), 0x04,         //   REPORT_COUNT (4 axes)
    0x81.toByte(), 0x02,         //   INPUT (Data, Variable, Absolute)

    // ----- 2 Triggers: LT + RT (8-bit, 0-255) -----
    0x05, 0x01,                  //   USAGE_PAGE (Generic Desktop)
    0x09, 0x33,                  //   USAGE (Rx) — Left Trigger
    0x09, 0x34,                  //   USAGE (Ry) — Right Trigger
    0x15, 0x00,                  //   LOGICAL_MINIMUM (0)
    0x26, 0xFF.toByte(), 0x00,   //   LOGICAL_MAXIMUM (255)
    0x75, 0x08,                  //   REPORT_SIZE (8 bits per trigger)
    0x95.toByte(), 0x02,         //   REPORT_COUNT (2 triggers)
    0x81.toByte(), 0x02,         //   INPUT (Data, Variable, Absolute)

    // ----- Hat Switch: D-Pad (8 directions) -----
    0x05, 0x01,                  //   USAGE_PAGE (Generic Desktop)
    0x09, 0x39,                  //   USAGE (Hat Switch)
    0x15, 0x01,                  //   LOGICAL_MINIMUM (1)
    0x25, 0x08,                  //   LOGICAL_MAXIMUM (8)
    0x75, 0x04,                  //   REPORT_SIZE (4 bits)
    0x95.toByte(), 0x01,         //   REPORT_COUNT (1)
    0x81.toByte(), 0x02,         //   INPUT (Data, Variable, Absolute)
    // Padding to fill the byte
    0x75, 0x04,                  //   REPORT_SIZE (4 bits padding)
    0x95.toByte(), 0x01,         //   REPORT_COUNT (1)
    0x81.toByte(), 0x01,         //   INPUT (Constant) — padding bits

    // ----- Rumble OUTPUT (Host → Phone Vibration) -----
    0x05, 0x0F,                  //   USAGE_PAGE (Physical Interface / Force Feedback)
    0x09, 0x21,                  //   USAGE (Set Effect Report)
    0x85.toByte(), 0x02,         //   REPORT_ID (2) — Rumble Output
    0x15, 0x00,                  //   LOGICAL_MINIMUM (0)
    0x26, 0xFF.toByte(), 0x00,   //   LOGICAL_MAXIMUM (255)
    0x75, 0x08,                  //   REPORT_SIZE (8 bits per motor)
    0x95.toByte(), 0x02,         //   REPORT_COUNT (2 — strong motor + weak motor)
    0x91.toByte(), 0x02,         //   OUTPUT (Data, Variable, Absolute)

    0xC0.toByte()                // END_COLLECTION
)
```

### Report Data Format (What Gets Sent Every Frame)

```
Report ID 1 — Gamepad Input (14 bytes total):
┌──────────┬──────────┬────────┬────────┬────────┬────────┬──────┬──────┬──────┐
│ Buttons  │ Buttons  │ LStickX│ LStickY│ RStickX│ RStickY│  LT  │  RT  │D-Pad │
│ (byte 1) │ (byte 2) │(16-bit)│(16-bit)│(16-bit)│(16-bit)│(8-bit│(8-bit│(4+4) │
│ 8 bits   │ 8 bits   │ 2 bytes│ 2 bytes│ 2 bytes│ 2 bytes│1 byte│1 byte│1 byte│
└──────────┴──────────┴────────┴────────┴────────┴────────┴──────┴──────┴──────┘

Report ID 2 — Rumble Output (2 bytes, received FROM host):
┌──────────────┬──────────────┐
│ Strong Motor │  Weak Motor  │
│   (0-255)    │   (0-255)    │
└──────────────┴──────────────┘
```

### Button Mapping (16 Buttons)

| Bit | Button | Xbox Equivalent | PS Equivalent |
|:---:|:---|:---|:---|
| 0 | Button 1 | A | Cross (✕) |
| 1 | Button 2 | B | Circle (○) |
| 2 | Button 3 | X | Square (□) |
| 3 | Button 4 | Y | Triangle (△) |
| 4 | Button 5 | LB | L1 |
| 5 | Button 6 | RB | R1 |
| 6 | Button 7 | Back / Select | Share |
| 7 | Button 8 | Start / Menu | Options |
| 8 | Button 9 | Left Stick Click | L3 |
| 9 | Button 10 | Right Stick Click | R3 |
| 10 | Button 11 | Guide / Home | PS Button |
| 11 | Button 12 | Share | Touchpad Click |
| 12-15 | Buttons 13-16 | (Reserved/Extra) | (Reserved/Extra) |

---

## Implementation Architecture

### File Structure

```
NEXPAD/app/src/main/kotlin/com/sanket/tools/nexpad/
├── bluetooth/
│   ├── BluetoothHidManager.kt        // Core BT HID connection manager
│   ├── HidDescriptors.kt             // HID descriptor byte arrays
│   ├── HidReportBuilder.kt           // Builds report byte arrays from GamepadInput
│   ├── BluetoothPermissionHelper.kt  // Runtime permission handling
│   └── BluetoothDeviceScanner.kt     // Scan & discover nearby devices
├── viewmodel/
│   └── GamepadViewModel.kt           // Updated — routes to Network OR Bluetooth
├── ui/
│   ├── screens/
│   │   ├── GamepadScreen.kt          // Main gameplay (already built)
│   │   ├── ConnectionScreen.kt       // NEW — Mode selection (WiFi / BT)
│   │   └── BluetoothPairScreen.kt    // NEW — BT device discovery & pairing
│   └── components/
│       └── ModeToggle.kt             // NEW — Switch between PC Mode / TV Mode
└── models/
    └── GamepadInput.kt               // Already built — shared between modes
```

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                    NEXPAD Android App                    │
│                                                         │
│  ┌──────────────┐    ┌──────────────────────────┐      │
│  │  Touch Input  │───→│    GamepadViewModel      │      │
│  │  (UI Buttons) │    │                          │      │
│  └──────────────┘    │  GamepadInput {           │      │
│  ┌──────────────┐    │    buttons: Int,           │      │
│  │  Gyroscope   │───→│    leftStickX: Float,     │      │
│  │  (Sensors)   │    │    leftStickY: Float,     │      │
│  └──────────────┘    │    rightStickX: Float,    │      │
│                      │    ...                     │      │
│                      │  }                         │      │
│                      │         │                  │      │
│                      │    ┌────┴────┐             │      │
│                      │    ▼         ▼             │      │
│                      │ PC Mode   TV Mode          │      │
│                      └──┬──────────┬──────────────┘      │
│                         │          │                     │
│                    ┌────▼────┐ ┌───▼──────────────┐     │
│                    │  Ktor   │ │ BluetoothHidMgr  │     │
│                    │  UDP    │ │ registerApp()     │     │
│                    │ Socket  │ │ sendReport()      │     │
│                    └────┬────┘ └───┬──────────────┘     │
│                         │          │                     │
└─────────────────────────┼──────────┼─────────────────────┘
                          │          │
                     WiFi/USB    Bluetooth
                          │          │
                     ┌────▼────┐ ┌───▼──────────┐
                     │ Desktop │ │ TV / PC / Mac │
                     │Companion│ │ (No app!)     │
                     │  App    │ │               │
                     └─────────┘ └───────────────┘
```

---

## Permissions & Requirements

### AndroidManifest.xml

```xml
<!-- Bluetooth Permissions -->
<!-- Android 11 and below -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Android 12+ (API 31+) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" 
    android:usesPermissionFlags="neverForLocation" />

<!-- Feature declaration -->
<uses-feature android:name="android.hardware.bluetooth" android:required="true" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

### Runtime Permissions (Kotlin)

```kotlin
// Android 12+
val btPermissions = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_ADVERTISE,
    Manifest.permission.BLUETOOTH_SCAN
)

// Android 11 and below
val btPermissionsLegacy = arrayOf(
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.ACCESS_FINE_LOCATION
)
```

### Minimum Requirements

| Requirement | Value |
|:---|:---|
| **Min Android Version** | Android 9 (API 28) |
| **Target Android Version** | Android 15+ (API 35+) |
| **Bluetooth** | Classic Bluetooth (BR/EDR) required |
| **BLE** | Optional (not used for gamepad) |
| **OEM Support** | Most phones support it, some budget phones may not have HID profile |

---

## Latency Optimization Strategies

Since we can't change Android's BT stack (like ESP32's NimBLE), we optimize what we CAN control:

### 1. Dedicated Send Thread
```kotlin
// DON'T send on main thread!
private val sendExecutor = Executors.newSingleThreadExecutor()

fun sendGamepadState(input: GamepadInput) {
    sendExecutor.execute {
        val report = buildReport(input)
        hidDevice.sendReport(connectedDevice, REPORT_ID, report)
    }
}
```

### 2. High Frequency Reporting (60Hz+)
```kotlin
// Send reports at 60Hz = every ~16ms
private val reportJob = CoroutineScope(Dispatchers.Default).launch {
    while (isActive) {
        sendGamepadState(currentInput)
        delay(16) // 60 Hz
    }
}
```

### 3. Delta-Only Updates
```kotlin
// Only send when input actually changes
fun onInputChanged(newInput: GamepadInput) {
    if (newInput != lastSentInput) {
        sendGamepadState(newInput)
        lastSentInput = newInput
    }
}
```

### 4. Compact Report Format
```kotlin
// Pack data efficiently — 14 bytes per report
// 2 bytes buttons + 8 bytes sticks + 2 bytes triggers + 1 byte dpad + 1 byte padding
fun buildReport(input: GamepadInput): ByteArray {
    val report = ByteArray(14)
    // ... pack tightly, no wasted bytes
    return report
}
```

### Expected Latency

| Connection | Expected Latency |
|:---|:---|
| Phase 1: USB Tethering | < 5ms |
| Phase 1: WiFi (5GHz) | 10-30ms |
| **Phase 2: Bluetooth HID** | **15-40ms** (device dependent) |
| Real Xbox BT Controller | 8-15ms |

---

## 🛠️ Step-by-Step Implementation Plan (Ready for Phase 2)

**Core Principle:** *DO NOT touch existing NEXPAD PC/WiFi logic. The app will swap the underlying `IGamepadConnection` interface dynamically based on user toggle.*

### Step 1: Settings Screen Toggle (Mode Selection)
- **Target File:** `app/src/main/java/com/sanket/tools/nexpad/ui/SettingsScreen.kt`
- **Action:** Add a Segmented Button / Toggle Switch at the top for **[ Desktop Mode (WiFi) | Bluetooth Mode (HID) ]**.
- **Logic:** 
  - If "Desktop Mode" is selected: Show the existing IP Address input and Connect button.
  - If "Bluetooth Mode" is selected: Hide the IP input. Show a "Start Bluetooth Pairing" button and connection status.
  - Save the selected mode to `SharedPreferences` so it remembers the user's choice.

### Step 2: Extracting XInput HID Descriptor
- **Target File:** `app/src/main/java/com/sanket/tools/nexpad/bluetooth/HidDescriptors.kt` (New)
- **Action:** Define `XBOX_ONE_S_DESCRIPTOR` byte array.
- **Why XInput?** Bluetooth doesn't natively speak "XInput" (which is a USB protocol). However, if we use the *exact* HID Descriptor of an official Xbox Wireless Controller, Windows will automatically translate our Bluetooth inputs into XInput! This is the secret to getting perfect PC compatibility wirelessly.
- **ESP32 Features Included:** We will expand the descriptor to support **16-bit analog sticks** (for ultra-precise aiming) and **128 buttons** (to map M1-M4, Profile, and Turbo).

### Step 3: Bluetooth Client (The Core Engine)
- **Target File:** `app/src/main/java/com/sanket/tools/nexpad/bluetooth/BluetoothClient.kt` (New)
- **Interface:** Must implement `IGamepadConnection` (just like `NetworkClient` does).
- **Action:**
  - Initialize `BluetoothHidDevice`.
  - `registerApp()` using the XInput descriptor.
  - `sendInput(input: GamepadInput)`: Convert the NEXPAD state into the 14-byte byte array expected by the Xbox descriptor and call `sendReport()`.
  - Listen for incoming `onSetReport` requests from the host.

### Step 4: Routing the ViewModel
- **Target File:** `app/src/main/java/com/sanket/tools/nexpad/viewmodel/GamepadNetworkManager.kt`
- **Action:** Modify the hardcoded `private val connection: IGamepadConnection = NetworkClient()`.
- **Logic:** 
  - Change to `var connection: IGamepadConnection`.
  - Add a function `setConnectionMode(isBluetooth: Boolean)`.
  - If `isBluetooth` == true, instantiate `BluetoothClient()`. Else, instantiate `NetworkClient()`.
  - *Result:* The UI buttons, joysticks, and sensors don't need a single line of code changed. They just update the ViewModel, and the ViewModel sends it to whichever connection is active!

### Step 5: Android Permissions
- **Target File:** `app/src/main/java/com/sanket/tools/nexpad/bluetooth/BluetoothPermissionHelper.kt` (New)
- **Action:** Handle Android 12+ strict Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`). If the user toggles to Bluetooth Mode, prompt them for these permissions immediately before trying to register the HID device.

### Step 6: Rumble Feedback Integration
- **Target File:** `BluetoothClient.kt`
- **Action:** When `onSetReport` receives 2 bytes from the PC (Left Motor / Right Motor), trigger `onFeedbackReceived` callback. The existing `GamepadScreen.kt` already listens to this callback and shakes the phone!

---

## Summary

| Topic | Key Decision |
|:---|:---|
| **API** | `BluetoothHidDevice` — only option, stable since 2018 |
| **Console Support** | ❌ PS4/PS5/Xbox impossible, ⚠️ Switch maybe |
| **Universal Support** | ✅ PC, Mac, TV, Tablet, Steam Deck — ALL work |
| **ESP32 Features** | 90% portable via HID descriptor — same byte format |
| **Rumble** | ✅ Possible — OUTPUT report → phone vibration |
| **Composite (Gamepad+Mouse)** | ✅ Possible — multiple Report IDs |
| **Latency** | 15-40ms expected, optimizable with send thread |
| **Play Store Apps** | All use WiFi streaming for consoles, NOT Bluetooth |
| **Best Reference** | Pocket-Pad (same API, same approach) |
