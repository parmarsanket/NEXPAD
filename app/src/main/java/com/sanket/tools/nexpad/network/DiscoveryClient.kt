package com.sanket.tools.nexpad.network

import android.content.Context
import com.sanket.tools.nexpad.protocol.NexpadProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

data class DiscoveredServer(
    val name: String,
    val ipAddress: String,
    val port: Int = 9999,
    val isUsbTethering: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class DiscoveryClient(
    private val context: Context? = null,
    private val defaultPortProvider: () -> Int = { 9999 }
) {
    private class ChannelSession(
        val channel: DatagramChannel,
        val target: NetworkBroadcastTarget?,
        val listenJob: Job
    )

    private val activeSessions = ConcurrentHashMap<String, ChannelSession>()
    private var broadcastSupervisorJob: Job? = null

    suspend fun startDiscovery(
        scope: CoroutineScope,
        onServerDiscovered: (DiscoveredServer) -> Unit
    ) = withContext(Dispatchers.IO) {
        stopDiscovery() // Ensure clean state before starting

        broadcastSupervisorJob = scope.launch(Dispatchers.IO) {
            val broadcastBuffer = ByteBuffer.allocate(1).apply {
                put(NexpadProtocol.PACKET_TYPE_DISCOVER)
            }

            while (isActive) {
                try {
                    val currentTargets = NetworkInterfaceHelper.getActiveBroadcastTargets()
                    val targetKeys = currentTargets.map { "${it.localAddress.hostAddress}:${it.interfaceName}" }.toSet()

                    // 1. Purge sessions for interfaces that were disconnected or went down
                    val iterator = activeSessions.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.key != "fallback_wildcard" && entry.key !in targetKeys) {
                            entry.value.listenJob.cancel()
                            try { entry.value.channel.close() } catch (_: Exception) {}
                            iterator.remove()
                        }
                    }

                    // 2. Open dedicated channels for newly discovered or existing physical interfaces
                    for (target in currentTargets) {
                        val key = "${target.localAddress.hostAddress}:${target.interfaceName}"
                        if (!activeSessions.containsKey(key)) {
                            try {
                                val ch = DatagramChannel.open().apply {
                                    configureBlocking(true)
                                    socket().broadcast = true
                                    socket().soTimeout = 1500
                                    
                                    // If Wi-Fi/Ethernet interface, bind directly to the Wi-Fi physical radio
                                    if (!target.isUsbTethering && context != null) {
                                        val wifiNet = NetworkInterfaceHelper.findWifiNetwork(context)
                                        if (wifiNet != null) {
                                            try { wifiNet.bindSocket(socket()) } catch (_: Exception) {}
                                        }
                                    }
                                    socket().bind(InetSocketAddress(target.localAddress, 0))
                                }

                                val listenJob = launch(Dispatchers.IO) {
                                    listenChannel(ch, target.isUsbTethering, onServerDiscovered)
                                }
                                activeSessions[key] = ChannelSession(ch, target, listenJob)
                            } catch (_: Exception) {
                                // Interface binding failed, ignore
                            }
                        }
                    }

                    // 3. Ensure fallback wildcard channel is open for global broadcast & hotspot subnets
                    if (!activeSessions.containsKey("fallback_wildcard")) {
                        try {
                            val wildcardCh = DatagramChannel.open().apply {
                                configureBlocking(true)
                                socket().broadcast = true
                                socket().soTimeout = 1500
                                socket().bind(InetSocketAddress(0))
                            }
                            val listenJob = launch(Dispatchers.IO) {
                                listenChannel(wildcardCh, false, onServerDiscovered)
                            }
                            activeSessions["fallback_wildcard"] = ChannelSession(wildcardCh, null, listenJob)
                        } catch (_: Exception) {}
                    }

                    // 4. Send DISCOVER packets across all active channels
                    for ((key, session) in activeSessions) {
                        try {
                            if (session.target != null) {
                                broadcastBuffer.rewind()
                                session.channel.send(broadcastBuffer, session.target.broadcastAddress)
                            } else if (key == "fallback_wildcard") {
                                broadcastBuffer.rewind()
                                session.channel.send(broadcastBuffer, InetSocketAddress("255.255.255.255", 9998))
                                broadcastBuffer.rewind()
                                session.channel.send(broadcastBuffer, InetSocketAddress("192.168.43.255", 9998))
                            }
                        } catch (_: Exception) {
                            // Transient send failure on this interface, continue
                        }
                    }
                } catch (_: Exception) {
                    // Interface scanning error, continue
                }

                delay(1200.milliseconds)
            }
        }
    }

    private suspend fun listenChannel(
        channel: DatagramChannel,
        isInterfaceUsbTethering: Boolean,
        onServerDiscovered: (DiscoveredServer) -> Unit
    ) {
        val buffer = ByteBuffer.allocate(1024)
        while (currentCoroutineContext().isActive && channel.isOpen) {
            try {
                buffer.clear()
                val senderAddr = channel.receive(buffer) as? InetSocketAddress ?: continue
                buffer.flip()
                if (buffer.remaining() > 0 && buffer.get() == NexpadProtocol.PACKET_TYPE_SERVER_INFO) {
                    if (buffer.remaining() > 0) {
                        val nameLength = buffer.get().toInt() and 0xFF
                        if (buffer.remaining() >= nameLength) {
                            val nameBytes = ByteArray(nameLength)
                            buffer.get(nameBytes)
                            val serverName = String(nameBytes, Charsets.UTF_8).trim()
                            if (serverName.isBlank()) continue

                            val ip = senderAddr.address.hostAddress ?: ""
                            if (ip.isBlank()) continue
                            val isUsb = isInterfaceUsbTethering || NetworkInterfaceHelper.isUsbTetheringAddress(ip)

                            val configuredPort = defaultPortProvider()
                            val serverPort = if (buffer.remaining() >= 2) {
                                val p = buffer.short.toInt() and 0xFFFF
                                if (p in 1024..65535) p else configuredPort
                            } else {
                                configuredPort
                            }

                            // Strict Port Match: If Desktop's broadcast port does not match Mobile's configured port,
                            // reject this server so the user must configure both to the same port.
                            if (serverPort != configuredPort) {
                                continue
                            }

                            withContext(Dispatchers.Main) {
                                onServerDiscovered(
                                    DiscoveredServer(
                                        name = serverName,
                                        ipAddress = ip,
                                        port = serverPort,
                                        isUsbTethering = isUsb
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {
                // Expected timeout when no server responds within 1500ms; continue listening
            } catch (_: java.nio.channels.ClosedChannelException) {
                break
            } catch (_: java.nio.channels.AsynchronousCloseException) {
                break
            } catch (e: Exception) {
                if (channel.isOpen) {
                    delay(1000.milliseconds)
                }
            }
        }
    }

    fun stopDiscovery() {
        broadcastSupervisorJob?.cancel()
        broadcastSupervisorJob = null
        for ((_, session) in activeSessions) {
            session.listenJob.cancel()
            try {
                session.channel.close()
            } catch (_: Exception) {}
        }
        activeSessions.clear()
    }
}
