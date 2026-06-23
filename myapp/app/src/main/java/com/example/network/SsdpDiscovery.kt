package com.example.network

import android.util.Log
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.regex.Pattern

object SsdpDiscovery {
    private const val SSDP_MULTICAST_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val TIMEOUT_MS = 5000
    private const val MAX_RETRY = 2

    private val SSDP_QUERY_REMOTE = """
        M-SEARCH * HTTP/1.1
        HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
        MAN: "ssdp:discover"
        MX: 3
        ST: urn:samsung.com:device:RemoteControlReceiver:1
        
    """.trimIndent().replace("\n", "\r\n") + "\r\n"

    private val SSDP_QUERY_ALL = """
        M-SEARCH * HTTP/1.1
        HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
        MAN: "ssdp:discover"
        MX: 3
        ST: ssdp:all
        
    """.trimIndent().replace("\n", "\r\n") + "\r\n"

    fun discoverTvs(): Flow<TvDevice> = flow {
        Log.d("SamsungRemoteDiscovery", "Starting SSDP scan...")

        val foundIps = mutableSetOf<String>()

        for (attempt in 1..MAX_RETRY) {
            Log.d("SamsungRemoteDiscovery", "Scan attempt $attempt of $MAX_RETRY")

            try {
                scanWithMulticast(foundIps) { device ->
                    emit(device)
                }
            } catch (e: Exception) {
                Log.e("SamsungRemoteDiscovery", "Multicast scan failed: ${e.message}", e)
            }

            try {
                scanWithUnicast(foundIps) { device ->
                    emit(device)
                }
            } catch (e: Exception) {
                Log.e("SamsungRemoteDiscovery", "Unicast scan failed: ${e.message}", e)
            }

            if (attempt < MAX_RETRY) {
                kotlinx.coroutines.delay(500)
            }
        }

        Log.d("SamsungRemoteDiscovery", "Scan completed. Found ${foundIps.size} devices")
    }.flowOn(Dispatchers.IO)

    private suspend fun scanWithMulticast(foundIps: MutableSet<String>, emit: suspend (TvDevice) -> Unit) {
        var socket: MulticastSocket? = null
        try {
            val multicastGroup = InetAddress.getByName(SSDP_MULTICAST_IP)
            socket = MulticastSocket(null)
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(SSDP_PORT))
            socket.soTimeout = TIMEOUT_MS
            socket.timeToLive = 4

            val networkInterface = getActiveNetworkInterface()
            if (networkInterface != null) {
                socket.networkInterface = networkInterface
                Log.d("SamsungRemoteDiscovery", "Using interface: ${networkInterface.displayName}")
            }

            socket.joinGroup(multicastGroup)

            val packetDataRemote = SSDP_QUERY_REMOTE.toByteArray(Charsets.UTF_8)
            val packetRemote = DatagramPacket(packetDataRemote, packetDataRemote.size, multicastGroup, SSDP_PORT)
            socket.send(packetRemote)
            Log.d("SamsungRemoteDiscovery", "Sent multicast SSDP query (RemoteControlReceiver)")

            kotlinx.coroutines.delay(200)

            val packetDataAll = SSDP_QUERY_ALL.toByteArray(Charsets.UTF_8)
            val packetAll = DatagramPacket(packetDataAll, packetDataAll.size, multicastGroup, SSDP_PORT)
            socket.send(packetAll)
            Log.d("SamsungRemoteDiscovery", "Sent multicast SSDP query (ssdp:all)")

            receiveResponses(socket, foundIps, emit)

            socket.leaveGroup(multicastGroup)
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "Multicast error: ${e.message}", e)
            throw e
        } finally {
            try {
                socket?.close()
            } catch (ex: Exception) {
                Log.e("SamsungRemoteDiscovery", "Error closing multicast socket", ex)
            }
        }
    }

    private suspend fun scanWithUnicast(foundIps: MutableSet<String>, emit: suspend (TvDevice) -> Unit) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = TIMEOUT_MS

            val broadcastAddress = InetAddress.getByName("255.255.255.255")

            val packetDataRemote = SSDP_QUERY_REMOTE.toByteArray(Charsets.UTF_8)
            val packetRemote = DatagramPacket(packetDataRemote, packetDataRemote.size, broadcastAddress, SSDP_PORT)
            socket.send(packetRemote)
            Log.d("SamsungRemoteDiscovery", "Sent broadcast SSDP query (RemoteControlReceiver)")

            kotlinx.coroutines.delay(200)

            val packetDataAll = SSDP_QUERY_ALL.toByteArray(Charsets.UTF_8)
            val packetAll = DatagramPacket(packetDataAll, packetDataAll.size, broadcastAddress, SSDP_PORT)
            socket.send(packetAll)
            Log.d("SamsungRemoteDiscovery", "Sent broadcast SSDP query (ssdp:all)")

            receiveResponses(socket, foundIps, emit)
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "Broadcast error: ${e.message}", e)
            throw e
        } finally {
            try {
                socket?.close()
            } catch (ex: Exception) {
                Log.e("SamsungRemoteDiscovery", "Error closing broadcast socket", ex)
            }
        }
    }

    private suspend fun receiveResponses(socket: DatagramSocket, foundIps: MutableSet<String>, emit: suspend (TvDevice) -> Unit) {
        val buffer = ByteArray(4096)
        val ipPattern = Pattern.compile("LOCATION:?\\s*http://([^:/]+)", Pattern.CASE_INSENSITIVE)
        val usnPattern = Pattern.compile("USN:?\\s*uuid:([0-9a-fA-F-]+)", Pattern.CASE_INSENSITIVE)
        val modelPattern = Pattern.compile("(?:friendlyName|modelName):?\\s*([^\\r\\n]+)", Pattern.CASE_INSENSITIVE)

        val startTime = System.currentTimeMillis()
        var consecutiveTimeouts = 0

        while (System.currentTimeMillis() - startTime < TIMEOUT_MS && consecutiveTimeouts < 3) {
            val receivePacket = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(receivePacket)
                consecutiveTimeouts = 0

                val response = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                val senderIp = receivePacket.address.hostAddress ?: continue

                Log.v("SamsungRemoteDiscovery", "Response from $senderIp: ${response.take(200)}")

                if (response.contains("Samsung", ignoreCase = true) ||
                    response.contains("Tizen", ignoreCase = true) ||
                    response.contains("RemoteControlReceiver", ignoreCase = true) ||
                    response.contains("MediaRenderer", ignoreCase = true)) {

                    if (foundIps.add(senderIp)) {
                        var ip = senderIp
                        val mIp = ipPattern.matcher(response)
                        if (mIp.find()) {
                            val extractedIp = mIp.group(1)
                            if (extractedIp != null && extractedIp.isNotEmpty()) {
                                ip = extractedIp
                            }
                        }

                        val mUsn = usnPattern.matcher(response)
                        val uuid = if (mUsn.find()) mUsn.group(1)?.take(8)?.uppercase() ?: "" else ""

                        val mModel = modelPattern.matcher(response)
                        val modelInfo = if (mModel.find()) mModel.group(1)?.trim() ?: "" else ""

                        val tvName = when {
                            modelInfo.isNotEmpty() -> "Samsung $modelInfo"
                            uuid.isNotEmpty() -> "Samsung TV ($uuid)"
                            else -> "Samsung TV"
                        }

                        val tvModel = if (modelInfo.isNotEmpty()) modelInfo else "Smart TV"

                        Log.d("SamsungRemoteDiscovery", "Found TV: name=$tvName, ip=$ip")
                        emit(TvDevice(
                            ip = ip,
                            port = 8002,
                            name = tvName,
                            model = tvModel,
                            macAddress = "AA:BB:CC:DD:EE:FF",
                            signalStrength = "Bagus"
                        ))
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                consecutiveTimeouts++
            } catch (e: Exception) {
                Log.e("SamsungRemoteDiscovery", "Error receiving packet: ${e.message}", e)
                break
            }
        }
    }

    private fun getActiveNetworkInterface(): NetworkInterface? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp && networkInterface.supportsMulticast()) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                            Log.d("SamsungRemoteDiscovery", "Active network: ${networkInterface.displayName}, IP: ${address.hostAddress}")
                            return networkInterface
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "Error getting network interface", e)
        }
        return null
    }
}
