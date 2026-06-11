package com.sanket.tools.nexpad.bluetooth

object HidDescriptors {
    const val GAMEPAD_REPORT_ID = 1
    const val GAMEPAD_REPORT_SIZE = 13 // 13 bytes standard layout

    /**
     * Standard Generic Gamepad Descriptor.
     * Report 1 (13 Bytes):
     * Bytes 0-3: 32 Buttons
     * Bytes 4-7: Left Stick (X, Y) - 16-bit
     * Bytes 8-11: Right Stick (Rx, Ry) - 16-bit
     * Byte 12: D-Pad (Hat Switch)
     */
    val XBOX_ONE_S_DESCRIPTOR = byteArrayOf(
        0x05, 0x01,        // Usage Page (Generic Desktop Ctrls)
        0x09, 0x05,        // Usage (Game Pad)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), 0x01, //   Report ID (1)
        
        // 32 Buttons - 1 bit each (4 Bytes)
        0x05, 0x09,        //   Usage Page (Button)
        0x19, 0x01,        //   Usage Minimum (0x01)
        0x29, 0x20,        //   Usage Maximum (0x20)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x95.toByte(), 0x20, //   Report Count (32)
        0x75, 0x01,        //   Report Size (1)
        0x81.toByte(), 0x02, //   Input (Data,Var,Abs)
        
        // Left Stick (X, Y) - 16-bit (4 Bytes)
        0x05, 0x01,        //   Usage Page (Generic Desktop Ctrls)
        0x09, 0x01,        //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x09, 0x30,        //     Usage (X)
        0x09, 0x31,        //     Usage (Y)
        0x15, 0x00,        //     Logical Minimum (0)
        0x27, 0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00, // Logical Maximum (65535)
        0x95.toByte(), 0x02, //     Report Count (2)
        0x75, 0x10,        //     Report Size (16)
        0x81.toByte(), 0x02, //     Input (Data,Var,Abs)
        0xC0.toByte(),       //   End Collection
        
        // Right Stick (Z, Rz) - 16-bit (4 Bytes)
        0x05, 0x01,        //   Usage Page (Generic Desktop Ctrls)
        0x09, 0x01,        //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x09, 0x32,        //     Usage (Z)
        0x09, 0x35,        //     Usage (Rz)
        0x15, 0x00,        //     Logical Minimum (0)
        0x27, 0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00, // Logical Maximum (65535)
        0x95.toByte(), 0x02, //     Report Count (2)
        0x75, 0x10,        //     Report Size (16)
        0x81.toByte(), 0x02, //     Input (Data,Var,Abs)
        0xC0.toByte(),       //   End Collection
        
        // D-Pad (Hat Switch) - 4-bit + 4-bit padding (1 Byte)
        0x09, 0x39,        //   Usage (Hat switch)
        0x15, 0x01,        //   Logical Minimum (1)
        0x25, 0x08,        //   Logical Maximum (8)
        0x35, 0x00,        //   Physical Minimum (0)
        0x46, 0x3B, 0x01,  //   Physical Maximum (315)
        0x65, 0x14,        //   Unit (English Rotation)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x04,        //   Report Size (4)
        0x81.toByte(), 0x42, //   Input (Data,Var,Abs,Null State)
        0x65, 0x00,        //   Unit (None)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x04,        //   Report Size (4)
        0x81.toByte(), 0x03, //   Input (Const,Var,Abs) -> Padding
        
        // --- Rumble Output Report (Report ID 3) ---
        0x05, 0x0F,        //   Usage Page (PID Page)
        0x09, 0x21,        //   Usage (0x21 - Set Effect Report)
        0x85.toByte(), 0x03, //   Report ID (3)
        0x09, 0x97.toByte(), //   Usage (0x97 - DC Motor Enable)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x04,        //   Report Size (4)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)
        
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x00,        //   Logical Maximum (0)
        0x75, 0x04,        //   Report Size (4)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x91.toByte(), 0x03, //   Output (Const,Var,Abs)
        
        0x09, 0x70,        //   Usage (0x70 - Magnitude)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95.toByte(), 0x04, //   Report Count (4 motors: Strong, Weak, LeftTrigger, RightTrigger)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)
        
        0x09, 0x50,        //   Usage (0x50 - Duration)
        0x66, 0x01, 0x10,  //   Unit (System: SI Linear, Time: Seconds)
        0x55, 0x0E,        //   Unit Exponent (-2)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)
        
        0x09, 0xA7.toByte(), //   Usage (0xA7 - Start Delay)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)
        0x65, 0x00,        //   Unit (None)
        0x55, 0x00,        //   Unit Exponent (0)
        
        0x09, 0x7C,        //   Usage (0x7C - Loop Count)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)

        
        // --- Battery Report (Report ID 4) ---
        0x05, 0x06,        //   Usage Page (Generic Device Ctrls)
        0x09, 0x20,        //   Usage (Battery Strength)
        0x85.toByte(), 0x04, //   Report ID (4)
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x00, // Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x81.toByte(), 0x02, //   Input (Data,Var,Abs)
        
        0xC0.toByte()        // End Collection
    )
}
