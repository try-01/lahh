package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException

object SsdpDiscovery {
    private const val TAG = "SamsungRemoteDiscovery"
    private const val SSDP_MULTICAST_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val TIMEOUT_MS = 6000L
    private const val SOCKET_TIMEOUT_MS = 2000

    private val SEARCH_TARGETS = listOf(
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "ssdp:all",
        "upnp:rootdevice",
        "urn:schemas-upnp-org:device:MediaRenderer:1"
    )

    fun discoverTvs(context: Context? = null): Flow<TvDevice> = flow {
        Log.d(TAG, "Starting SSDP scan...")

        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("ssdp_discovery_lock")
        multicastLock?.setReferenceCounted(true)
        multicastLock?.acquire()

        try {
            val devices = scanWithMulticastSocket()
            if (devices.isEmpty()) {
                Log.d(TAG, "MulticastSocket returned no results, trying DatagramSocket fallback...")
                devices += scanWithDatagramSocket()
            }
            devices.forEach { emit(it) }
            Log.d(TAG, "SSDP scan finished. Found ${devices.size} device(s)")
        } catch (e: Exception) {
            Log.e(TAG, "SSDP discovery error", e)
        } finally {
            try {
                multicastLock?.release()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun scanWithMulticastSocket(): List<TvDevice> {
        val found = mutableListOf<TvDevice>()
        val seenIps = mutableSetOf<String>()
        val buffer = ByteArray(4096)
        val group = InetAddress.getByName(SSDP_MULTICAST_IP)

        try {
            MulticastSocket(0).use { socket ->
                socket.reuseAddress = true
                socket.soTimeout = SOCKET_TIMEOUT_MS

                val wifiInterface = findWifiInterface()
                if (wifiInterface != null) {
                    try {
                        socket.networkInterface = wifiInterface
                        Log.d(TAG, "Bound MulticastSocket to WiFi interface: ${wifiInterface.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set network interface", e)
                    }
                }

                socket.joinGroup(group)
                Log.d(TAG, "Joined multicast group $SSDP_MULTICAST_IP")
                Log.d(TAG, "MulticastSocket bound to port ${socket.localPort}")

                val startTime = System.currentTimeMillis()

                for (st in SEARCH_TARGETS) {
                    sendBurst(socket, st, group)
                }

                Log.d(TAG, "Listening for SSDP responses on MulticastSocket...")
                listenForResponses(socket, buffer, startTime, found, seenIps)

                try {
                    socket.leaveGroup(group)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "MulticastSocket approach failed", e)
        }

        return found
    }

    private fun scanWithDatagramSocket(): List<TvDevice> {
        val found = mutableListOf<TvDevice>()
        val seenIps = mutableSetOf<String>()
        val buffer = ByteArray(4096)
        val group = InetAddress.getByName(SSDP_MULTICAST_IP)

        try {
            val bindAddr = InetSocketAddress(Inet4Address.getByName("0.0.0.0"), 0)
            DatagramSocket(bindAddr).use { socket ->
                socket.broadcast = true
                socket.reuseAddress = true
                socket.soTimeout = SOCKET_TIMEOUT_MS
                Log.d(TAG, "DatagramSocket bound to ${socket.localAddress}:${socket.localPort}")

                val startTime = System.currentTimeMillis()

                for (st in SEARCH_TARGETS) {
                    sendBurst(socket, st, group)
                }

                Log.d(TAG, "Listening for SSDP responses on DatagramSocket...")
                listenForResponses(socket, buffer, startTime, found, seenIps)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DatagramSocket approach failed", e)
        }

        return found
    }

    private fun findWifiInterface(): NetworkInterface? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.firstOrNull { ni ->
                    ni.name.startsWith("wlan") ||
                    ni.displayName?.contains("wlan", ignoreCase = true) == true
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not enumerate network interfaces", e)
            null
        }
    }

    private fun sendBurst(socket: DatagramSocket, st: String, group: InetAddress) {
        val data = buildSearchMessage(st)
        for (i in 1..3) {
            try {
                val packet = DatagramPacket(data, data.size, group, SSDP_PORT)
                socket.send(packet)
                Log.d(TAG, "Sent M-SEARCH (burst $i/3) ST: $st")
                Thread.sleep(200)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send M-SEARCH for ST: $st", e)
            }
        }
    }

    private fun listenForResponses(
        socket: DatagramSocket,
        buffer: ByteArray,
        startTime: Long,
        found: MutableList<TvDevice>,
        seenIps: MutableSet<String>
    ) {
        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val raw = String(packet.data, 0, packet.length)
                val ip = packet.address.hostAddress ?: continue

                Log.v(TAG, "Received packet from $ip (${packet.length} bytes)")
                Log.v(TAG, "Raw: ${raw.take(300)}")

                if (ip in seenIps) continue
                if (!isSamsungDevice(raw)) {
                    Log.v(TAG, "Ignored non-Samsung response from $ip")
                    continue
                }

                seenIps.add(ip)
                val name = extractDeviceName(raw)
                Log.i(TAG, "FOUND Samsung TV: $name @ $ip")
                Log.d(TAG, "Response:\n${raw.take(500)}")

                found.add(
                    TvDevice(
                        ip = ip,
                        port = 8001,
                        name = name,
                        model = "Samsung Smart TV",
                        macAddress = "AA:BB:CC:DD:EE:FF"
                    )
                )
            } catch (_: SocketTimeoutException) {
                // Expected, continue
            }
        }
    }

    private fun buildSearchMessage(st: String): ByteArray {
        val msg = """
M-SEARCH * HTTP/1.1
HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
MAN: "ssdp:discover"
MX: 3
ST: $st

""".trimIndent()
        return msg.replace("\n", "\r\n").toByteArray()
    }

    private fun isSamsungDevice(response: String): Boolean {
        if (!response.contains("200 OK", ignoreCase = true)) return false

        val combined = sequenceOf(
            response,
            extractHeader(response, "ST"),
            extractHeader(response, "SERVER"),
            extractHeader(response, "USN"),
            extractHeader(response, "LOCATION")
        ).joinToString(" ")

        return combined.contains("samsung", ignoreCase = true) ||
                combined.contains("tizen", ignoreCase = true) ||
                combined.contains("remotereceiver", ignoreCase = true) ||
                combined.contains("sec_hub", ignoreCase = true) ||
                combined.contains("smarttv", ignoreCase = true) ||
                combined.contains("samsungtizen", ignoreCase = true)
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

    private fun extractDeviceName(response: String): String {
        val server = extractHeader(response, "SERVER")
        return when {
            server.contains("Tizen", ignoreCase = true) -> "Samsung TV (Tizen)"
            server.contains("Samsung", ignoreCase = true) -> "Samsung TV"
            else -> "Samsung Smart TV"
        }
    }
}
