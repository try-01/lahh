package com.example.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.TvDevice
import okhttp3.*
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class SamsungTvWebSocket(
    private val tvDevice: TvDevice,
    private val onTokenReceived: (String) -> Unit,
    private val onStatusChanged: (ConnectionState) -> Unit
) {
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 30000L
        private const val WSS_TO_WS_DELAY_MS = 500L
        private const val PING_INTERVAL_SECONDS = 30L
    }

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isClosedManually = false
    private var retryCount = 0
    private var isUsingWss = true
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        okHttpClient = createTrustAllOkHttpClient()
    }

    fun connect() {
        Log.d("SamsungRemote", "connect() called")
        isClosedManually = false
        retryCount = 0
        isUsingWss = true
        onStatusChanged(ConnectionState.CONNECTING)
        connectInternal()
    }

    private fun connectInternal() {
        if (isUsingWss) connectWss() else connectWs()
    }

    private fun connectWss() {
        val client = okHttpClient ?: return
        val appNameEncoded = "SamsungRemote"
        val tokenParam = if (!tvDevice.token.isNullOrEmpty()) "&token=${tvDevice.token}" else ""
        val wssUrl = "wss://${tvDevice.ip}:8002/api/v2/channels/samsung.remote.control?name=$appNameEncoded$tokenParam"

        Log.d("SamsungRemote", "Connecting to WSS: $wssUrl")
        val request = Request.Builder().url(wssUrl).build()

        webSocket = client.newWebSocket(request, createListener())
    }

    private fun connectWs() {
        val client = okHttpClient ?: return
        val tokenParam = if (!tvDevice.token.isNullOrEmpty()) "&token=${tvDevice.token}" else ""
        val wsUrl = "ws://${tvDevice.ip}:8001/api/v2/channels/samsung.remote.control?name=SamsungRemote$tokenParam"

        Log.d("SamsungRemote", "Connecting to WS: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, createListener())
    }

    private fun createListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val tag = if (isUsingWss) "WSS" else "WS"
                Log.d("SamsungRemote", "$tag Open! (port ${if (isUsingWss) 8002 else 8001})")
                retryCount = 0
                onStatusChanged(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SamsungRemote", "WS message: $text")
                try {
                    val json = JSONObject(text)
                    if (json.optString("event") == "ms.channel.connect") {
                        val data = json.optJSONObject("data")
                        val token = data?.optString("token")
                        if (!token.isNullOrEmpty() && token != tvDevice.token) {
                            Log.d("SamsungRemote", "Received pairing token: $token")
                            onTokenReceived(token)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SamsungRemote", "Error parsing WS message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                val tag = if (isUsingWss) "WSS" else "WS"
                Log.d("SamsungRemote", "$tag Closed: $code / $reason")
                if (!isClosedManually) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val tag = if (isUsingWss) "WSS" else "WS"
                Log.e("SamsungRemote", "$tag Failure: ${t.message}")

                if (isClosedManually) return

                if (isUsingWss) {
                    // WSS failed: fall back to WS once
                    Log.d("SamsungRemote", "WSS failed, falling back to WS (port 8001)")
                    isUsingWss = false
                    mainHandler.postDelayed({ connectInternal() }, WSS_TO_WS_DELAY_MS)
                } else {
                    // WS failed: exponential backoff reconnect
                    scheduleReconnect()
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (retryCount >= MAX_RETRIES) {
            Log.e("SamsungRemote", "Max retries ($MAX_RETRIES) reached, giving up")
            onStatusChanged(ConnectionState.DISCONNECTED)
            return
        }

        val delay = calculateBackoff()
        retryCount++
        Log.d("SamsungRemote", "Reconnect $retryCount/$MAX_RETRIES in ${delay}ms")
        onStatusChanged(ConnectionState.CONNECTING)

        mainHandler.postDelayed({
            if (!isClosedManually) {
                connectInternal()
            }
        }, delay)
    }

    private fun calculateBackoff(): Long {
        val delay = BASE_DELAY_MS * (1L shl (retryCount))
        return minOf(delay, MAX_DELAY_MS)
    }

    fun sendKey(key: String) {
        val ws = webSocket
        if (ws == null) {
            Log.e("SamsungRemote", "Cannot send key: WebSocket not connected")
            return
        }
        val message = """
            {
                "method": "ms.remote.control",
                "params": {
                    "Cmd": "Click",
                    "DataOfCmd": "$key",
                    "Option": "false",
                    "TypeOfRemote": "SendRemoteKey"
                }
            }
        """.trimIndent()
        Log.d("SamsungRemote", "Sending key: $key")
        ws.send(message)
    }

    fun disconnect() {
        Log.d("SamsungRemote", "disconnect() called")
        isClosedManually = true
        webSocket?.close(1000, "Disconnected manually")
        webSocket = null
        onStatusChanged(ConnectionState.DISCONNECTED)
    }

    private fun createTrustAllOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e("SamsungRemote", "Error creating OkHttpClient", e)
            OkHttpClient.Builder()
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }
}
