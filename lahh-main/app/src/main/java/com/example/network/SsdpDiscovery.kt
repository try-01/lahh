package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException

object SsdpDiscovery {
    private const val SSDP_MULTICAST_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val TIMEOUT_MS = 4000L

    private val SEARCH_TARGETS = listOf(
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "ssdp:all"
    )

    private fun buildSearchMessage(st: String): ByteArray {
        return """
M-SEARCH * HTTP/1.1
HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
MAN: "ssdp:discover"
MX: 3
ST: $st

""".trimIndent().replace("\n", "\r\n").toByteArray()
    }

    fun discoverTvs(context: Context? = null): Flow<TvDevice> = flow {
        Log.d("SamsungRemoteDiscovery", "Starting SSDP scan...")

        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("ssdp_discovery_lock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        val foundIps = mutableSetOf<String>()

        try {
            val multicastOk = tryDiscoverWithMulticastSocket(foundIps)
            if (!multicastOk) {
                Log.d("SamsungRemoteDiscovery", "MulticastSocket returned no results, trying DatagramSocket...")
                tryDiscoverWithDatagramSocket(foundIps)
            }
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "SSDP discovery error", e)
        } finally {
            try {
                multicastLock?.release()
            } catch (_: Exception) {}
        }

        Log.d("SamsungRemoteDiscovery", "SSDP scan finished. Found ${foundIps.size} device(s)")
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<TvDevice>.tryDiscoverWithMulticastSocket(
        foundIps: MutableSet<String>
    ): Boolean {
        var socket: MulticastSocket? = null
        try {
            val group = InetAddress.getByName(SSDP_MULTICAST_IP)
            socket = MulticastSocket(SSDP_PORT)
            socket.soTimeout = (TIMEOUT_MS / 2).toInt()
            socket.joinGroup(group)

            for (st in SEARCH_TARGETS) {
                val data = buildSearchMessage(st)
                val packet = DatagramPacket(data, data.size, group, SSDP_PORT)
                socket.send(packet)
                Log.d("SamsungRemoteDiscovery", "MulticastSocket sent M-SEARCH for ST: $st")
            }

            listenForResponses(socket, foundIps)
            return foundIps.isNotEmpty()
        } catch (e: Exception) {
            Log.w("SamsungRemoteDiscovery", "MulticastSocket failed, will try DatagramSocket", e)
            return false
        } finally {
            try {
                socket?.leaveGroup(InetAddress.getByName(SSDP_MULTICAST_IP))
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun FlowCollector<TvDevice>.tryDiscoverWithDatagramSocket(
        foundIps: MutableSet<String>
    ) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = (TIMEOUT_MS / 2).toInt()
            socket.broadcast = true

            val multicastGroup = InetAddress.getByName(SSDP_MULTICAST_IP)

            for (st in SEARCH_TARGETS) {
                val data = buildSearchMessage(st)
                val packet = DatagramPacket(data, data.size, multicastGroup, SSDP_PORT)
                socket.send(packet)
                Log.d("SamsungRemoteDiscovery", "DatagramSocket sent M-SEARCH for ST: $st")
            }

            listenForResponses(socket, foundIps)
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "DatagramSocket discovery error", e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun FlowCollector<TvDevice>.listenForResponses(
        socket: DatagramSocket,
        foundIps: MutableSet<String>
    ) {
        val buffer = ByteArray(2048)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            try {
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length)
                val senderIp = receivePacket.address.hostAddress ?: continue

                if (foundIps.contains(senderIp)) continue

                if (isSamsungDevice(response)) {
                    foundIps.add(senderIp)
                    Log.d("SamsungRemoteDiscovery", "Found Samsung TV at $senderIp")

                    val tvName = extractDeviceName(response, senderIp)
                    emit(
                        TvDevice(
                            ip = senderIp,
                            port = 8001,
                            name = tvName,
                            model = "Samsung Smart TV",
                            macAddress = "AA:BB:CC:DD:EE:FF"
                        )
                    )
                }
            } catch (_: SocketTimeoutException) {
                // Expected timeout, continue looping
            }
        }
    }

    private fun isSamsungDevice(response: String): Boolean {
        if (!response.contains("200 OK", ignoreCase = true)) return false

        val st = extractHeader(response, "ST")
        val server = extractHeader(response, "SERVER")
        val usn = extractHeader(response, "USN")
        val location = extractHeader(response, "LOCATION")

        return response.contains("Samsung", ignoreCase = true) ||
                response.contains("Tizen", ignoreCase = true) ||
                st.contains("samsung", ignoreCase = true) ||
                st.contains("RemoteControlReceiver", ignoreCase = true) ||
                server.contains("Samsung", ignoreCase = true) ||
                server.contains("Tizen", ignoreCase = true) ||
                usn.contains("samsung", ignoreCase = true) ||
                location.contains("samsung", ignoreCase = true)
    }

    private fun extractHeader(response: String, headerName: String): String {
        val lines = response.split("\r\n", "\n")
        for (line in lines) {
            if (line.startsWith(headerName, ignoreCase = true)) {
                val colonIndex = line.indexOf(':')
                if (colonIndex >= 0) {
                    return line.substring(colonIndex + 1).trim()
                }
            }
        }
        return ""
    }

    private fun extractDeviceName(response: String, ip: String): String {
        val server = extractHeader(response, "SERVER")
        return when {
            server.contains("Tizen", ignoreCase = true) -> "Samsung TV (Tizen)"
            server.contains("Samsung", ignoreCase = true) -> "Samsung TV"
            else -> "Samsung TV"
        }
    }
}
