package com.sanket.tools.nexpad.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * Encapsulates an active local network interface target for multi-homed broadcast discovery.
 */
data class NetworkBroadcastTarget(
    val localAddress: Inet4Address,
    val broadcastAddress: InetSocketAddress,
    val interfaceName: String,
    val isUsbTethering: Boolean
)

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
     * Finds the local IPv4 address on this Android device that shares the same subnet
     * as the specified target IP address. This enables binding sockets to the exact physical
     * network interface (e.g. Wi-Fi wlan0 vs USB Tethering rndis0), preventing multi-homed routing conflicts.
     */
    fun findMatchingLocalAddress(targetAddress: String): Inet4Address? {
        try {
            val targetInet = InetAddress.getByName(targetAddress)
            if (targetInet !is Inet4Address) return null
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                for (ifAddr in iface.interfaceAddresses) {
                    val addr = ifAddr.address
                    val prefixLength = ifAddr.networkPrefixLength.toInt()
                    if (addr is Inet4Address) {
                        if (isInSameSubnet(addr.address, targetInet.address, prefixLength)) {
                            return addr
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Finds the active Wi-Fi or physical Ethernet Network object from ConnectivityManager.
     */
    fun findWifiNetwork(context: Context): Network? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val networks = cm.allNetworks
            networks.firstOrNull { net ->
                val caps = cm.getNetworkCapabilities(net)
                caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns all active broadcast targets across all active network interfaces (Wi-Fi, USB Tethering, Hotspot).
     * Each target includes the interface's local IPv4 and its subnet broadcast address.
     */
    fun getActiveBroadcastTargets(port: Int = 9998): List<NetworkBroadcastTarget> {
        val targets = mutableListOf<NetworkBroadcastTarget>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val isUsb = name.startsWith("rndis") || name.startsWith("usb") || name.startsWith("ncm")

                for (ifAddr in iface.interfaceAddresses) {
                    val addr = ifAddr.address
                    val broadcast = ifAddr.broadcast
                    if (addr is Inet4Address && broadcast != null) {
                        targets.add(
                            NetworkBroadcastTarget(
                                localAddress = addr,
                                broadcastAddress = InetSocketAddress(broadcast, port),
                                interfaceName = iface.name,
                                isUsbTethering = isUsb
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return targets
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
     * Uses modern ConnectivityManager activeNetwork and allNetworks capabilities with a physical interface fallback.
     */
    fun hasActiveWifiOrEthernet(context: android.content.Context): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            
            // Check all networks across interfaces (not just default activeNetwork)
            val networks = cm?.allNetworks ?: emptyArray()
            for (net in networks) {
                val caps = cm?.getNetworkCapabilities(net) ?: continue
                if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                ) {
                    return true
                }
            }

            val activeNet = cm?.activeNetwork
            if (activeNet != null) {
                val caps = cm.getNetworkCapabilities(activeNet)
                if (caps != null && (
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                )) {
                    return true
                }
            }

            // Fallback for offline LAN or unmetered local routing where activeNetwork may differ:
            // scan network interfaces for an UP Wi-Fi (wlan) or Ethernet (eth) adapter with an assigned IPv4 address.
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val name = iface.name.lowercase()
                val isWifiOrEth = name.startsWith("wlan") || name.startsWith("eth")
                if (isWifiOrEth && iface.interfaceAddresses.any { it.address is Inet4Address }) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}

