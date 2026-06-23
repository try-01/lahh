package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

object SsdpDiscovery {
    private const val TAG = "SamsungRemoteDiscovery"
    private const val SSDP_MULTICAST_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val TIMEOUT_MS = 5000L
    private const val BURST_COUNT = 3
    private const val BURST_DELAY_MS = 150L
    private const val SOCKET_TIMEOUT_MS = 1500

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
            val devices = scanWithDatagramSocket()
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

    private fun scanWithDatagramSocket(): List<TvDevice> {
        val found = mutableListOf<TvDevice>()
        val seenIps = mutableSetOf<String>()
        val buffer = ByteArray(4096)
        val multicastGroup = InetAddress.getByName(SSDP_MULTICAST_IP)

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.reuseAddress = true
            socket.soTimeout = SOCKET_TIMEOUT_MS

            Log.d(TAG, "Socket bound to ${socket.localAddress}:${socket.localPort}")

            val startTime = System.currentTimeMillis()

            for (st in SEARCH_TARGETS) {
                val data = buildSearchMessage(st)
                repeat(BURST_COUNT) { burst ->
                    try {
                        val packet = DatagramPacket(data, data.size, multicastGroup, SSDP_PORT)
                        socket.send(packet)
                        Log.d(TAG, "Sent M-SEARCH (burst ${burst + 1}/$BURST_COUNT) ST: $st")
                        Thread.sleep(BURST_DELAY_MS)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send M-SEARCH for ST: $st", e)
                    }
                }
            }

            Log.d(TAG, "Listening for SSDP responses...")

            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val response = String(packet.data, 0, packet.length)
                    val ip = packet.address.hostAddress ?: continue

                    if (ip in seenIps) continue
                    if (!isSamsungDevice(response)) continue

                    seenIps.add(ip)
                    val name = extractDeviceName(response, ip)
                    Log.i(TAG, "Found device: $name @ $ip")
                    Log.d(TAG, "Response headers:\n${response.take(500)}")

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
                    // Timeout between receives, continue
                }
            }
        }

        return found
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
        if (!response.contains("200 OK", ignoreCase = true)) {
            return false
        }

        val combined = sequenceOf(
            response,
            extractHeader(response, "ST"),
            extractHeader(response, "SERVER"),
            extractHeader(response, "USN"),
            extractHeader(response, "LOCATION")
        ).joinToString(" ")

        val keywords = listOf(
            "samsung", "tizen", "remotereceiver",
            "samsungtizen", "smarttv", "sec_",
            "dne-", "smt-", "ua6", "un5", "un6", "un7"
        )

        return keywords.any { combined.contains(it, ignoreCase = true) }
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
            response.contains("samsung", ignoreCase = true) -> "Samsung TV"
            else -> "Samsung TV @ $ip"
        }
    }
}
