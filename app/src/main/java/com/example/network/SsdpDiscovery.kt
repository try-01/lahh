package com.example.network

import android.util.Log
import com.example.data.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.regex.Pattern

object SsdpDiscovery {
    private const val SSDP_MULTICAST_IP = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val TIMEOUT_MS = 2500

    private val SSDP_QUERY_REMOTE = """
        M-SEARCH * HTTP/1.1
        HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
        MAN: "ssdp:discover"
        MX: 2
        ST: urn:samsung.com:device:RemoteControlReceiver:1
        
    """.trimIndent().replace("\n", "\r\n")

    private val SSDP_QUERY_ALL = """
        M-SEARCH * HTTP/1.1
        HOST: $SSDP_MULTICAST_IP:$SSDP_PORT
        MAN: "ssdp:discover"
        MX: 2
        ST: ssdp:all
        
    """.trimIndent().replace("\n", "\r\n")

    fun discoverTvs(): Flow<TvDevice> = flow {
        Log.d("SamsungRemoteDiscovery", "Starting SSDP scan...")
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS

            val multicastGroup = InetAddress.getByName(SSDP_MULTICAST_IP)
            
            // Send queries
            val packetDataRemote = SSDP_QUERY_REMOTE.toByteArray()
            val packetRemote = DatagramPacket(packetDataRemote, packetDataRemote.size, multicastGroup, SSDP_PORT)
            socket.send(packetRemote)

            val packetDataAll = SSDP_QUERY_ALL.toByteArray()
            val packetAll = DatagramPacket(packetDataAll, packetDataAll.size, multicastGroup, SSDP_PORT)
            socket.send(packetAll)

            val buffer = ByteArray(2048)
            val ipPattern = Pattern.compile("LOCATION:? http://([^:/]+)")
            val tvNamePattern = Pattern.compile("USN:? uuid:([0-9a-fA-F-]+)")

            val foundIps = mutableSetOf<String>()

            // Listen for replies until socket timeout
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                val receivePacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val senderIp = receivePacket.address.hostAddress ?: continue
                    
                    Log.d("SamsungRemoteDiscovery", "SSDP response from $senderIp: $response")

                    if (response.contains("Samsung") || response.contains("Tizen") || response.contains("RemoteControlReceiver")) {
                        if (foundIps.add(senderIp)) {
                            // Extract IP and details
                            var ip = senderIp
                            val mIp = ipPattern.matcher(response)
                            if (mIp.find()) {
                                ip = mIp.group(1) ?: senderIp
                            }
                            
                            val mUsn = tvNamePattern.matcher(response)
                            val uuid = if (mUsn.find()) mUsn.group(1)?.take(6)?.uppercase() ?: "" else ""
                            val tvName = if (uuid.isNotEmpty()) "Samsung TV ($uuid)" else "Samsung TV"

                            Log.d("SamsungRemoteDiscovery", "Found TV Device: name: $tvName, ip: $ip")
                            emit(TvDevice(
                                ip = ip,
                                port = 8001,
                                name = tvName,
                                model = "Connected Smart TV",
                                macAddress = "AA:BB:CC:DD:EE:FF"
                            ))
                        }
                    }
                } catch (e: Exception) {
                    // Timeout or other network hiccups, check time and keep looping or exit
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("SamsungRemoteDiscovery", "SSDP discovery issue occurred", e)
        } finally {
            try {
                socket?.close()
            } catch (ex: Exception) {
                // ignored
            }
        }
    }.flowOn(Dispatchers.IO)
}
