package com.sanket.tools.nexpad.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkInterfaceHelper {

    /**
     * Checks if a target IP address belongs to the same subnet as an active USB tethering interface
     * (such as rndis0, usb0, ncm0) on this Android device.
     */
    fun isUsbTetheringAddress(targetAddress: String): Boolean {
        try {
            val targetInet = InetAddress.getByName(targetAddress)
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val isUsbIface = name.startsWith("rndis") || name.startsWith("usb") || name.startsWith("ncm")
                if (isUsbIface) {
                    for (ifAddr in iface.interfaceAddresses) {
                        val addr = ifAddr.address
                        val prefixLength = ifAddr.networkPrefixLength.toInt()
                        if (addr is Inet4Address && targetInet is Inet4Address) {
                            if (isInSameSubnet(addr.address, targetInet.address, prefixLength)) {
                                return true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Compares two IPv4 byte arrays up to a given CIDR network prefix length.
     */
    fun isInSameSubnet(addr1: ByteArray, addr2: ByteArray, prefixLength: Int): Boolean {
        if (addr1.size != 4 || addr2.size != 4) return false
        if (prefixLength <= 0) return true
        if (prefixLength > 32) return false

        var remainingBits = prefixLength
        for (i in 0 until 4) {
            if (remainingBits >= 8) {
                if (addr1[i] != addr2[i]) return false
                remainingBits -= 8
            } else if (remainingBits > 0) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((addr1[i].toInt() and mask) != (addr2[i].toInt() and mask)) return false
                remainingBits = 0
            } else {
                break
            }
        }
        return true
    }

    /**
     * Checks if any active USB tethering network interface (rndis/usb/ncm) is currently UP and has an IPv4 address.
     */
    fun hasActiveUsbTethering(): Boolean {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val isUsbIface = name.startsWith("rndis") || name.startsWith("usb") || name.startsWith("ncm")
                if (isUsbIface) {
                    for (ifAddr in iface.interfaceAddresses) {
                        if (ifAddr.address is Inet4Address) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Checks if any active Wi-Fi or Ethernet network connection is currently available.
     */
    fun hasActiveWifiOrEthernet(context: android.content.Context): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            cm?.allNetworks?.any { net ->
                val caps = cm.getNetworkCapabilities(net)
                caps != null && (
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}

