package com.sanket.tools.nexpad.network

import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlinx.coroutines.CoroutineScope

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int = 9999
)

class DiscoveryClient {
    private var channel: DatagramChannel? = null
    private var listenJob: Job? = null
    private var broadcastJob: Job? = null

    private fun getBroadcastAddresses(): List<InetSocketAddress> {
        val addresses = mutableListOf<InetSocketAddress>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                for (networkInterface in interfaces) {
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue

                    for (interfaceAddress in networkInterface.interfaceAddresses) {
                        val broadcast = interfaceAddress.broadcast
                        if (broadcast != null) {
                            addresses.add(InetSocketAddress(broadcast, 9998))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Always include the global broadcast as a fallback
        addresses.add(InetSocketAddress("255.255.255.255", 9998))
        // And typical hotspot fallback
        addresses.add(InetSocketAddress("192.168.43.255", 9998))
        return addresses.distinct()
    }

    suspend fun startDiscovery(
        scope: CoroutineScope,
        onServerDiscovered: (DiscoveredServer) -> Unit
    ) = withContext(Dispatchers.IO) {
        stopDiscovery() // Ensure clean state

        try {
            channel = DatagramChannel.open().apply {
                socket().broadcast = true
                bind(null)
                configureBlocking(false)
            }

            // Start listening for responses
            listenJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteBuffer.allocate(1024)
                while (isActive) {
                    try {
                        buffer.clear()
                        val senderAddr = channel?.receive(buffer) as? InetSocketAddress
                        if (senderAddr != null) {
                            buffer.flip()
                            if (buffer.remaining() > 0 && buffer.get() == NexpadProtocol.PACKET_TYPE_SERVER_INFO) {
                                if (buffer.remaining() > 0) {
                                    val nameLength = buffer.get().toInt() and 0xFF
                                    if (buffer.remaining() >= nameLength) {
                                        val nameBytes = ByteArray(nameLength)
                                        buffer.get(nameBytes)
                                        val serverName = String(nameBytes, Charsets.UTF_8)
                                        
                                        withContext(Dispatchers.Main) {
                                            onServerDiscovered(
                                                DiscoveredServer(
                                                    name = serverName,
                                                    ipAddress = senderAddr.address.hostAddress ?: "",
                                                    port = 9999
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            delay(50)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        delay(1000) // Back off if network is temporarily down
                    }
                }
            }

            // Start broadcasting every 2 seconds
            broadcastJob = scope.launch(Dispatchers.IO) {
                val broadcastBuffer = ByteBuffer.allocate(1).apply {
                    put(NexpadProtocol.PACKET_TYPE_DISCOVER)
                }
                while (isActive) {
                    val addresses = getBroadcastAddresses()
                    for (addr in addresses) {
                        broadcastBuffer.rewind()
                        try {
                            channel?.send(broadcastBuffer, addr)
                        } catch (e: Exception) {
                            // Ignore send failures on specific interfaces
                        }
                    }
                    delay(2000)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopDiscovery() {
        broadcastJob?.cancel()
        listenJob?.cancel()
        try {
            channel?.close()
        } catch (e: Exception) {}
        channel = null
    }
}
