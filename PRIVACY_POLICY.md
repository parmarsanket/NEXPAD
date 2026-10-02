# Privacy Policy for NEXPAD

**Last updated:** October 2, 2026

**NEXPAD** ("we", "our", or "the application") is an open-source virtual gamepad and HUD controller client designed and developed by Sanket Parmar. We respect your privacy and are committed to protecting it.

---

## 1. Zero Data Collection & Zero Telemetry
NEXPAD operates entirely as a local, peer-to-peer input controller:
- **No Personal Data Collected**: We do not collect, harvest, store, or transmit your name, email, phone number, location, or device identifiers.
- **No Analytics / No Tracking**: There are no third-party tracking SDKs, advertising networks, or telemetry trackers embedded in the application.
- **No Cloud Servers**: NEXPAD does not communicate with external cloud servers. All communications occur strictly between your Android device and your designated local PC over your private Wi-Fi network or direct USB cable.

---

## 2. Permissions & Their Purpose
NEXPAD requests only the minimal set of hardware permissions required to function as a physical gamepad replacement:

| Permission | Purpose | Data Handling |
|---|---|---|
| **Internet & Wi-Fi (`INTERNET`, `ACCESS_WIFI_STATE`)** | Transmit low-latency binary controller UDP packets to NEXPAD Desktop on your local network. | Transmitted directly to your PC IP address. Never sent to the internet or third parties. |
| **Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`)** | Optional wireless serial RFCOMM gamepad connection to your PC. | Local peer-to-peer pairing only. |
| **USB Accessory (`android.hardware.usb.accessory`)** | Zero-driver USB connection via Android Open Accessory (AOA) protocol. | Direct hardware wire communication with host PC. |
| **Motion Sensors (Gyroscope & Accelerometer)** | 6-Axis motion steering and CemuHook DSU motion input emulation for PC games/emulators. | Real-time sensor coordinates are streamed directly to your local PC. Never logged or recorded. |
| **Vibrate (`VIBRATE`)** | Provide tactile haptic rumble feedback when pressing buttons and receiving PC game motor packets. | Device hardware control only. |

---

## 3. Data Storage & Local Preferences
All configuration data (such as custom HUD layouts, button coordinates, color presets, and connection IP addresses) is stored locally on your device via Android Jetpack DataStore / SharedPreferences. This data never leaves your device unless you manually export a `.nxprc` profile.

---

## 4. Children’s Privacy
NEXPAD does not knowingly collect or solicit any personal information from children under the age of 13.

---

## 5. Open Source & Transparency
NEXPAD is open-source software licensed under the **Apache License 2.0**. The source code is publicly accessible for audit and verification.

---

## 6. Contact
If you have questions or concerns about this Privacy Policy, you can open an issue on the GitHub repository or contact:
- **Developer:** Sanket Parmar
- **Email:** parmarsanket265@gmail.com
- **Project:** NEXPAD Controller Project
