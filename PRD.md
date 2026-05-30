# Product Requirements Document (PRD)
**Project:** VirtualGamepad (NEXPAD)
**Phase:** 1 - PC Companion Mode (WiFi & USB)

## 1. Objective
To build a low-latency, highly responsive virtual Android gamepad app that allows users to control Windows PC games. Phase 1 focuses exclusively on the "Companion App Architecture" to emulate an Xbox 360 controller via the ViGEmBus driver.

## 2. Target Audience
*   PC Gamers who need an extra controller for split-screen/local multiplayer.
*   Casual gamers who want to play PC games from their couch using their phone.
*   Competitive gamers who require zero-latency inputs via a wired USB connection.

## 3. Core Features (Phase 1)
### Android App (The Client)
*   **UI/UX:** A visually appealing, customizable Jetpack Compose interface mimicking a standard Xbox controller layout (D-Pad, 2 Analog Sticks, ABXY buttons, Bumpers, and Triggers).
*   **Touch Handling:** Multi-touch support allowing simultaneous use of joysticks and buttons without dropping inputs.
*   **Networking:** High-frequency UDP or WebSocket transmission using Ktor.
*   **Haptics:** Basic vibration feedback when buttons are pressed.

### Desktop Companion App (The Server)
*   **Platform:** Windows and Linux (built using Kotlin Multiplatform - KMP).
*   **Networking:** A background server listening for packets from the Android app on a specific port.
*   **Driver Integration:** 
    *   *Windows:* Receives packets and translates them into Xbox 360 controller inputs using the **ViGEmBus** API.
    *   *Linux:* Translates packets using `uinput`.

## 4. Non-Functional Requirements
*   **Latency:** Network payload transmission should occur at 60Hz (approx. 16ms intervals). Total input-to-action latency over USB tethering must be < 20ms. Total latency over 5GHz WiFi must be < 50ms.
*   **Reliability:** The app must automatically attempt to reconnect if the network socket is dropped.
*   **Security:** (Phase 1) Local network transmission only. No internet routing required.

## 5. Technical Specifications & File Structure
*   **Language:** Kotlin (Android app) / Kotlin (KMP Companion App).
*   **UI Framework:** Jetpack Compose (Material Design 3).
*   **State Management:** MVVM Architecture with Kotlin Flow.
*   **Data Payload:** `GamepadInput.kt` (Serialized to JSON or ByteArray via `kotlinx.serialization`).

## 6. Success Criteria
*   The Android app successfully builds and runs on a physical device.
*   The user can press multiple buttons simultaneously on the screen.
*   The PC Companion app receives the exact button states.
*   Windows `joy.cpl` registers the virtual controller, and the inputs reflect the phone's screen in real-time.
