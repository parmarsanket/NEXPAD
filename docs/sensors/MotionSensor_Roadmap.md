# Future Improvements Roadmap (MotionSensorManager)

This document outlines the technical debt and future improvements required to bring the `MotionSensorManager` up to a commercial-grade SDK level. It is split into two tables: a detailed technical breakdown and a simplified priority list.

## 1. Technical Roadmap

| Issue | Current Implementation | Future Improvement | Impact / Why it matters |
| :--- | :--- | :--- | :--- |
| **1. Network Spam (Emission)** | Emits packet on *every* sensor event (1000+ packets/sec). | Only emit `onMotionPacket` during the `GAME_ROTATION_VECTOR` tick. | Drastically reduces network traffic and CPU overhead while keeping data perfectly synchronized. |
| **2. Mutability / Thread Safety** | Modifies and sends the exact same `MotionPacket` instance. | Implement a lock-free structure (`AtomicReference`) or Direct ByteBuffers. | Prevents severe race conditions if the UDP Network thread reads the packet while the Sensor thread is updating it. |
| **3. GC Object Churn** | Safe right now, but a naive `.copy()` fix would cause massive GC spikes. | Build a `MotionPacketPool` or RingBuffer to recycle packet objects. | Keeps memory allocations at zero, preventing the Android Garbage Collector from causing micro-stutters during gameplay. |
| **4. Quaternion Remapping** | Quaternion is pulled directly from hardware, skipping the display remap. | Derive the quaternion mathematically from the *remapped* rotation matrix. | Ensures the quaternion strictly matches the Yaw/Pitch/Roll axes if the device is rotated. |
| **5. Display Rotation Caching** | Cached only once during `start()`. | Implement a `DisplayListener` or `onConfigurationChanged`. | Keeps axes correct if the user accidentally unlocks and rotates their screen mid-game. |
| **6. Dead Zones** | Global `0.02f` threshold for all sensors. | Split thresholds: `0.005f` for Gyro, `0.02f` for Acceleration. | Gyroscopes are much more precise than accelerometers and need a tighter threshold for micro-aiming. |
| **7. Sensor Support / Fallback** | Assumes `GAME_ROTATION_VECTOR` is always available. | Implement a fallback chain (Rotation Vector ➔ Gyro/Accel Fusion). | Prevents total controller failure on cheaper Android devices missing specific hardware sensors. |
| **8. Hardware Limits** | Hardcoded to `5000` microseconds (200Hz). | Clamp delay to `max(sensor.minDelay, 5000)`. | Prevents crashes or erratic behavior on older phones that cannot physically poll at 200Hz. |
| **9. Android 12+ Throttling** | Missing manifest permissions. | Add `<uses-permission android:name="HIGH_SAMPLING_RATE_SENSORS"/>`. | Prevents Android 12+ from aggressively throttling our polling rate to save battery. |
| **10. Accuracy Drops** | Ignores `onAccuracyChanged`. | Ignore packets when accuracy drops to `SENSOR_STATUS_UNRELIABLE`. | Prevents the PC cursor/camera from wildly snapping if magnetic interference messes up the sensors. |


## 2. Priority & Simple Explanation

| Priority | Issue | Simple Explanation (Why we need this) |
| :--- | :--- | :--- |
| **High** | **1. Network Spam** | Sending too much data clogs your Wi-Fi or Bluetooth connection and causes lag. We should only send data to the PC exactly when the main sensor updates, rather than spamming it. |
| **High** | **2. Data Collisions (Thread Safety)** | If the app tries to send the sensor data to the PC at the *exact* millisecond the hardware is updating that data, the data gets corrupted. We need to lock it so this doesn't happen. |
| **High** | **3. Android 12+ Throttling** | Newer Android phones limit sensor speeds to save battery. We need to add a special permission in the app so Android knows we are a high-speed gaming app and lets us run at maximum speed. |
| **High** | **4. 3D Math Mismatch (Quaternion)** | If the 3D math isn't perfectly synced to how the screen is flipped, pushing "Up" on the controller might result in "Left" on the PC. We need to fix the math to match the screen. |
| **Medium** | **5. Micro-Stutters (Memory Churn)** | Creating and deleting data objects 200 times a second forces the phone to pause and clean up its memory (Garbage Collection). This causes annoying micro-stutters in your game. We need to recycle memory instead. |
| **Medium** | **6. Custom Dead Zones** | Gyroscopes are incredibly sensitive, but accelerometers are a bit messy. They need different filter rules so your sniper crosshair doesn't drift when your hands are perfectly still. |
| **Medium** | **7. Hardware Speed Limits** | We are telling the phone to run at ultra-fast esports speeds (200Hz). We need to check the phone's maximum speed limit first so we don't crash older phones. |
| **Low** | **8. Screen Rotation Bugs** | Right now, if you accidentally unlock your screen rotation and flip the phone while playing, the controls will break. We need the app to detect mid-game screen flips. |
| **Low** | **9. Budget Phone Fallbacks** | Cheaper Android phones might not have all the high-end sensors we are requesting. We need a backup plan so the app still works on budget phones. |
| **Low** | **10. Magnetic Interference** | Phone cases with magnets or being near certain electronics can confuse the sensors. We need to tell the app to ignore crazy sensor spikes so your camera doesn't spin wildly out of control. |
