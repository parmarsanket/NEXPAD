package com.sanket.tools.nexpad.sensors
//
//| Sensor                        | Use                                       | Should Use?                           |
//| ----------------------------- | ----------------------------------------- | ------------------------------------- |
//| `TYPE_GAME_ROTATION_VECTOR`   | 6-axis orientation (Gyro + Accelerometer) | ✅ **Primary choice**                  |
//| `TYPE_GYROSCOPE_UNCALIBRATED` | Raw angular velocity                      | ✅ Optional (advanced)                 |
//| `TYPE_GYROSCOPE`              | Angular velocity                          | ✅ If Game Rotation Vector unavailable |
//| `TYPE_GRAVITY`                | Gravity direction                         | ✅ Very useful                         |
//| `TYPE_LINEAR_ACCELERATION`    | Movement without gravity                  | ✅ Useful                              |
//| `TYPE_ACCELEROMETER`          | Raw acceleration                          | ⚠️ Fallback only                      |
//| `TYPE_ROTATION_VECTOR`        | 9-axis (includes magnetometer)            | ⚠️ Only if absolute heading needed    |
//| `TYPE_MAGNETIC_FIELD`         | Compass                                   | ❌ Usually unnecessary for games       |
