# Universal Android Game Controller - Implementation Plan

## Goal Description
Build a "Hybrid" Universal Android Game Controller application. The app will feature two distinct connection modes to ensure maximum compatibility across PCs, Android TVs, and consoles.

## Architecture & Technology Stack

**Core Android App:**
*   **UI:** Jetpack Compose (Material Design 3) for the controller interface.
*   **Networking (Phase 1):** Ktor (WebSockets/UDP) for WiFi/USB communication with the PC server.
*   **Bluetooth Protocol (Phase 2):** Android `BluetoothHidDevice` API (API 28+) to emulate a standard gamepad HID descriptor.
*   **Architecture Pattern:** MVVM (Model-View-ViewModel) with Hilt.

**PC Companion App (Phase 1 Bridge):**
*   **Framework:** Kotlin Multiplatform (KMP) for Desktop (Windows & Linux).
*   **Virtual Driver:** **ViGEmBus** (Windows) and `uinput` (Linux) to translate network packets into a Virtual Xbox 360 controller.

## Connection Modes Supported

### 🎮 Mode 1: PC Gaming Mode (WiFi / USB)
*   **How it works:** Uses the Companion App and ViGEmBus.
*   **Supported Connections:** WiFi (Local Network) and USB (Tethering/ADB).
*   **Best for:** Windows PCs (Provides 100% compatibility and zero-latency USB options).

### 📺 Mode 2: Smart TV Mode (Bluetooth HID)
*   **How it works:** Phone uses native Bluetooth HID to pretend it's a real physical controller. No companion app required.
*   **Supported Connections:** Bluetooth.
*   **Best for:** Android TVs, Apple TVs, tablets, and MacBooks.

## Proposed File Structure (.kt Files)
*   `models/GamepadInput.kt`: Data class representing button/stick states.
*   `network/ConnectionManager.kt`: Handles WiFi/USB socket transmission.
*   `bluetooth/BluetoothHidManager.kt`: Handles the native Bluetooth connection (Phase 2).
*   `ui/components/...`: Reusable UI elements (`AnalogStick.kt`, `GameButton.kt`).
*   `ui/screens/GamepadScreen.kt`: The main gameplay interface with a mode toggle.
*   `viewmodel/GamepadViewModel.kt`: Central state management, routing data to either Network or Bluetooth based on the active mode.

## Execution Roadmap

### Phase 1: PC Companion Mode (WiFi/USB)
1.  **Project Setup:** Initialize the Android Studio project.
2.  **UI Foundation:** Build the interactive on-screen layout (Xbox style).
3.  **Networking (Android):** Implement the Ktor transmission layer.
4.  **PC Companion App:** Build the PC server (Node.js/C#) and integrate ViGEmBus.
5.  **Testing:** Verify zero-latency USB and WiFi play on PC.

### Phase 2: TV & Console Mode (Bluetooth HID)
1.  **Bluetooth Setup:** Add permissions and `BluetoothHidDevice` API implementation.
2.  **HID Descriptor:** Define the raw gamepad byte array for Bluetooth pairing.
3.  **UI Toggle:** Add a switch to jump between "PC Mode" and "TV Mode".
4.  **Testing:** Pair directly to an Android TV and navigate the menus.
