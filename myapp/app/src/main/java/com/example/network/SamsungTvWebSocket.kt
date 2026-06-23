package com.example.network

import android.util.Log
import com.example.data.model.TvDevice
import okhttp3.*
import okio.ByteString
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

    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isClosedManually = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    init {
        okHttpClient = createTrustAllOkHttpClient()
    }

    fun connect() {
        val client = okHttpClient ?: return
        isClosedManually = false
        reconnectAttempts = 0
        onStatusChanged(ConnectionState.CONNECTING)

        val appNameEncoded = "U2Ftc3VuZ1JlbW90ZQ=="
        val tokenParam = if (!tvDevice.token.isNullOrEmpty()) "&token=${tvDevice.token}" else ""
        val wssUrl = "wss://${tvDevice.ip}:8002/api/v2/channels/samsung.remote.control?name=$appNameEncoded$tokenParam"

        Log.d("SamsungRemote", "Connecting to WSS: $wssUrl")
        connectToUrl(client, wssUrl, isSecure = true)
    }

    private fun connectToUrl(client: OkHttpClient, url: String, isSecure: Boolean) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SamsungRemote", "${if (isSecure) "WSS" else "WS"} connection opened")
                reconnectAttempts = 0
                onStatusChanged(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SamsungRemote", "Message received: $text")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("SamsungRemote", "Binary message received: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "Connection closing: code=$code, reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "Connection closed: code=$code, reason=$reason")
                if (!isClosedManually) {
                    onStatusChanged(ConnectionState.DISCONNECTED)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SamsungRemote", "Connection failure: ${t.message}", t)
                if (!isClosedManually && isSecure && reconnectAttempts < maxReconnectAttempts) {
                    reconnectAttempts++
                    Log.d("SamsungRemote", "Attempting fallback to insecure (attempt $reconnectAttempts)")
                    connectInsecureFallback()
                } else if (!isClosedManually) {
                    onStatusChanged(ConnectionState.DISCONNECTED)
                }
            }
        })
    }

    private fun connectInsecureFallback() {
        val client = okHttpClient ?: return
        val appNameEncoded = "U2Ftc3VuZ1JlbW90ZQ=="
        val tokenParam = if (!tvDevice.token.isNullOrEmpty()) "&token=${tvDevice.token}" else ""
        val wsUrl = "ws://${tvDevice.ip}:8001/api/v2/channels/samsung.remote.control?name=$appNameEncoded$tokenParam"

        Log.d("SamsungRemote", "Connecting to fallback WS: $wsUrl")
        connectToUrl(client, wsUrl, isSecure = false)
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")

            when (event) {
                "ms.channel.connect" -> {
                    val data = json.optJSONObject("data")
                    val token = data?.optString("token")
                    if (!token.isNullOrEmpty() && token != tvDevice.token) {
                        Log.d("SamsungRemote", "Received pairing token: $token")
                        onTokenReceived(token)
                    }
                }
                "ms.channel.ready" -> {
                    Log.d("SamsungRemote", "Channel is ready")
                }
                "ms.error" -> {
                    val data = json.optJSONObject("data")
                    val errorMessage = data?.optString("message") ?: "Unknown error"
                    Log.e("SamsungRemote", "TV error: $errorMessage")
                }
                else -> {
                    Log.d("SamsungRemote", "Unhandled event: $event")
                }
            }
        } catch (e: Exception) {
            Log.e("SamsungRemote", "Error parsing WebSocket message", e)
        }
    }

    fun sendKey(key: String) {
        val ws = webSocket
        if (ws == null) {
            Log.e("SamsungRemote", "Cannot send key: WebSocket not connected")
            return
        }

        val message = JSONObject().apply {
            put("method", "ms.remote.control")
            put("params", JSONObject().apply {
                put("Cmd", "Click")
                put("DataOfCmd", key)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            })
        }.toString()

        Log.d("SamsungRemote", "Sending key: $key")
        val sent = ws.send(message)
        if (!sent) {
            Log.e("SamsungRemote", "Failed to send key: $key")
        }
    }

    fun disconnect() {
        isClosedManually = true
        reconnectAttempts = maxReconnectAttempts
        webSocket?.close(1000, "Disconnected by user")
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

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            Log.e("SamsungRemote", "Error creating OkHttpClient", e)
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
