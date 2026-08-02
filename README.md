# 🎮 NEXPAD (Android Virtual Gamepad)

Welcome to the **NEXPAD** Android App! This app turns your Android smartphone into a fully customizable, highly responsive virtual Xbox 360 controller, complete with a 3D HUD editor, RGB lighting, and Gyroscope motion steering.

This README is designed to be a **newbie-friendly guide** to help you understand exactly how the code works and where to find everything.

---

## 🏗️ How the Code is Structured

The code follows a clean, modern Android architecture using **Jetpack Compose** (for the UI) and **MVVM** (Model-View-ViewModel) to keep things organized.

All the main code lives in this folder:
`app/src/main/java/com/sanket/tools/nexpad/`

### 1. 🖥️ The UI (`/ui`)
This folder contains everything you see on the screen.
*   **`GamepadScreen.kt`**: The main game controller screen. It dynamically draws all the joysticks and buttons exactly where the user placed them in the HUD editor.
*   **`HudEditorScreen.kt`**: The drag-and-drop editor where users can move buttons around, resize them, and change their transparency.
*   **`SettingsScreen.kt`**: The scrollable menu where users can toggle 2D Gyro Steering, 6-Axis motion, and RGB lighting.
*   **`components/`**: A folder containing the reusable, custom-built UI pieces (like `RealisticJoystick.kt`, `RealisticTrigger.kt`, `RealisticButton.kt`). 

### 2. 🧠 The Brains (`/viewmodel`)
This is where the actual logic happens. When you press a button on the UI, it sends a signal here.
*   **`GamepadViewModel.kt`**: The master coordinator. It holds the current state of every button (is 'A' pressed? where is the joystick?).
*   **`SensorController.kt`**: This file specifically handles reading the phone's hardware Gyroscope and Accelerometer, calculating 2D steering angles and 6-Axis motion data.
*   **`GamepadNetworkManager.kt`**: This file manages the background loop that constantly sends your button presses over Wi-Fi to your PC at 60 times a second.

### 3. 🌐 The Network (`/network`)
*   **`NetworkClient.kt`**: This handles the raw UDP (User Datagram Protocol) networking. It takes the button data, packs it into a fast, lightweight JSON string, and shoots it across your local Wi-Fi to the Desktop app.

### 4. 📦 The Data (`/model`)
*   **`GamepadInput.kt`**: A simple data class that lists every single button and axis on a controller (e.g., `btnA = true`, `leftStickX = 0.5f`). This is the exact package of data we send to the PC.
*   **`Profile.kt`**: Saves the user's custom HUD layout coordinates.

---

## 🚀 How the App Actually Works (Step-by-Step)

1. **You tap the screen**: You place your thumb on the virtual Left Joystick (`RealisticJoystick.kt`).
2. **UI tells ViewModel**: The joystick component calculates that you dragged it up. It calls `viewModel.updateLeftStick(0f, 1.0f)`.
3. **Data is Updated**: The `GamepadViewModel` updates the `GamepadInput` data object to reflect that the Left Stick is pushed forward.
4. **Network Sends Data**: Over in `GamepadNetworkManager.kt`, a fast loop running 60 times a second notices the updated `GamepadInput` data. It passes it to `NetworkClient`.
5. **Data flies over Wi-Fi**: `NetworkClient` turns the data into JSON and fires a UDP network packet to your PC's IP address.

*All of this happens in less than 16 milliseconds!*