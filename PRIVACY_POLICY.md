# Privacy Policy for NEXPAD

**Effective Date:** October 21, 2026  
**Application:** NEXPAD Virtual Gamepad & HUD Controller  
**Developer:** Sanket Parmar  
**Repository:** [github.com/parmarsanket/NEXPAD](https://github.com/parmarsanket/NEXPAD)  

---

## Executive Summary
**NEXPAD** is an open-source, ultra-low-latency virtual gamepad controller and customizable HUD client developed by Sanket Parmar. NEXPAD is engineered on a strict **offline-first, zero-telemetry, and peer-to-peer local communication** architecture:

* **Zero Personal Information Collected:** We do not collect, store, log, harvest, or transmit your name, email, phone number, location, or device identifiers.
* **Zero Third-Party Trackers or Ads:** NEXPAD contains no advertising SDKs, no analytics trackers, no telemetry hooks, and no commercial data brokers.
* **Zero Cloud Servers:** NEXPAD does not communicate with external cloud servers. All communications occur strictly between your Android device and your designated local PC over your private Wi-Fi network, physical USB cable, or local Bluetooth pairing.
* **100% Open Source:** NEXPAD is licensed under the Apache License 2.0. The entire source code is publicly accessible for independent security audit and verification.

---

## 1. Zero Data Collection & Zero Telemetry
NEXPAD operates entirely as a local hardware controller emulation client.

### A. Information We Do NOT Collect
* **Personal Data:** No names, usernames, passwords, email addresses, phone numbers, or physical addresses.
* **Financial Information:** No payment details, credit card numbers, billing addresses, or in-app purchase histories.
* **Location Data:** No GPS, fine, or coarse geographic location is ever accessed or derived.
* **Device Identifiers:** No Android Advertising ID (AAID), IMEI, MAC address, hardware serial numbers, or persistent fingerprints are collected or transmitted.
* **Usage Telemetry:** No user activity logs, session heatmaps, button-press statistics, or analytics metrics are transmitted to any cloud servers.

### B. No Accounts or Sign-In
NEXPAD requires **no user registration**, **no account creation**, and **no third-party logins** (e.g., Google, Facebook, Apple). The application functions immediately and fully without signing in.

---

## 2. Local Transports & Network Communications
NEXPAD provides three primary hardware transports to stream virtual controller inputs to the companion NEXPAD Desktop software running on your computer.

### A. Local Wi-Fi (UDP Protocol)
* **How It Operates:** When connected via Wi-Fi, NEXPAD packages gamepad inputs (analog thumbstick deflections, trigger pulls, button states, and gyroscope coordinates) into compact, binary UDP packets.
* **Destination:** Packets are transmitted strictly to the private local IPv4 address of your computer (e.g., `192.168.x.x`) on designated local ports (`26760` / `26761`).
* **Privacy Guarantee:** These packets never traverse the public internet. No network data is routed through intermediary proxy servers or third-party cloud infrastructure.

### B. Direct USB Connection (AOA 2.0 & ADB Port Forwarding)
* **How It Operates:** NEXPAD supports zero-driver wired connectivity using the official Android Open Accessory (AOA 2.0) protocol and ADB port forwarding.
* **Destination:** Real-time input packets travel directly across the physical USB cable connected to your PC host.
* **Privacy Guarantee:** Direct hardware wire transport. Data never touches any network interface or the internet.

### C. Bluetooth RFCOMM (Serial Port Profile)
* **How It Operates:** When Bluetooth mode is active, NEXPAD pairs directly with your host PC's Bluetooth adapter using native RFCOMM serial communication.
* **Privacy Guarantee:** Communication is strictly point-to-point between paired hardware devices.

---

## 3. Sensors & Motion Processing
NEXPAD utilizes your mobile device's on-board Inertial Measurement Unit (IMU) sensors to deliver esports-grade motion aiming and virtual steering wheel emulation:

* **Sensors Used:** Gyroscope, Accelerometer, Gravity Vector, and Game Rotation Vector (Quaternion).
* **High-Speed Translation:** Motion signals are sampled in real-time at up to 200 Hz and processed through mathematical algorithms (deadzone compensation, non-linear response curves, and exponential moving average filters) directly on your device.
* **Zero Logging / Zero Transmission:** Sensor coordinates are streamed instantaneously in memory to your local PC for game camera control. Sensor readings are never written to disk, never recorded, and never transmitted to external third parties.

---

## 4. Android System Permissions & Purpose
In strict accordance with Google Play Policy and Android security guidelines, NEXPAD requests only the minimal permissions required for physical controller emulation:

| Permission / Feature | Technical Purpose | Data Handling & Scope |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | Allows the app to open local UDP sockets to stream controller packets to your PC over your private home Wi-Fi network. | Data stays strictly within your private local network. Never routed to external internet servers. |
| `android.permission.ACCESS_NETWORK_STATE`<br>`android.permission.ACCESS_WIFI_STATE` | Detects whether your device is connected to Wi-Fi to display local IP connectivity and signal status. | Used exclusively for local UI connection diagnostics. Never transmitted. |
| `android.permission.BLUETOOTH_CONNECT`<br>`android.permission.BLUETOOTH_SCAN` | Enables wireless Bluetooth RFCOMM communication with your host PC on Android 12+ (API 31+). | Declared with `android:usesPermissionFlags="neverForLocation"` to cryptographically guarantee no physical location data is accessed. |
| `android.hardware.usb.accessory` | Enables direct hardware communication over USB cable using the Android Open Accessory protocol. | Direct physical wire data transfer. |
| `android.permission.VIBRATE` | Drives the device's linear resonant actuator (vibration motor) for tactile haptic button clicks and game force-feedback rumble. | Hardware control only. |
| `android.permission.WAKE_LOCK` | Keeps the screen illuminated during active gameplay sessions to prevent device sleep. | Power state management only. |
| Hardware Sensors (`accelerometer`, `gyroscope`) | Emulates 6-Axis motion aiming and virtual steering wheel tilt. | Marked as `required="false"` so devices lacking gyro can still run button controls. |

---

## 5. Local Data Storage & Preferences
* **Preferences:** User settings (such as thumbstick sensitivities, selected controller layout, haptic intensity, and target IP addresses) are stored exclusively in your device's private sandbox storage using Android Jetpack DataStore / SharedPreferences.
* **Custom Layouts & Presets:** Custom HUD profiles (`.nxlayout`) and button studio styling configurations (`.nxprc`) reside locally on your device.
* **Exporting:** If you choose to export or share a layout file, it is executed via standard Android storage access frameworks under your direct control.
* **No Cloud Backups:** We do not maintain cloud backups of your personal settings.

---

## 6. Third-Party Libraries & Software Development Kits (SDKs)
* **Zero Ads:** NEXPAD does not integrate Google AdMob, Unity Ads, AppLovin, or any other commercial advertising SDK.
* **Zero Analytics:** NEXPAD does not include Google Analytics for Firebase, Flurry, Adjust, AppsFlyer, Mixpanel, or Facebook SDK.
* **Open Source Dependencies:** NEXPAD is built exclusively on official Google Android Jetpack libraries (Compose, Navigation, Lifecycle, Coroutines, Serialization).

---

## 7. Children's Privacy (COPPA & GDPR-K Compliance)
NEXPAD is a general-utility gaming tool that does not target, collect, or solicit personal information from children under the age of 13 (or under 16 in the European Economic Area). Because the application collects zero personal information, it complies fully with the Children’s Online Privacy Protection Act (COPPA) and international child privacy standards.

---

## 8. Data Retention, Deletion, and User Rights (GDPR & CCPA / CPRA)
* **GDPR (European Union) & UK GDPR:** Under the General Data Protection Regulation, users have the right to access, rectify, or delete their personal data. Because NEXPAD collects zero personal data on external servers, there is no remote data to request or delete.
* **CCPA / CPRA (California):** We do not sell, share, or monetize user data.
* **Complete Local Data Deletion:** You have complete control over your local data. You can delete all locally stored preferences, layouts, and configurations at any time by:
  1. Opening your device **Settings** &rarr; **Apps** &rarr; **NEXPAD**.
  2. Selecting **Storage & cache** &rarr; **Clear storage**.
  3. Or simply uninstalling the application.

---

## 9. Security
Although NEXPAD does not process sensitive personal data, we enforce industry-standard security practices:
* Direct, low-latency binary streams with strict packet bounds validation to prevent buffer overflows.
* Support for local network pairing codes and token handshakes to ensure unauthorized devices cannot connect to your PC.
* Complete source code transparency under the Apache License 2.0.

---

## 10. Changes to This Privacy Policy
We may update this Privacy Policy periodically to reflect changes in legal requirements, Android operating system guidelines, or new application features. Any updates will be published to our public repository with an updated "Effective Date."

---

## 11. Contact Information
If you have questions, feedback, or concerns regarding this Privacy Policy or NEXPAD's security architecture, please contact:

* **Developer:** Sanket Parmar
* **Email:** [parmarsanket265@gmail.com](mailto:parmarsanket265@gmail.com)
* **GitHub Repository:** [github.com/parmarsanket/NEXPAD](https://github.com/parmarsanket/NEXPAD)
