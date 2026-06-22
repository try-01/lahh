package com.example.network

import android.util.Log
import com.example.data.model.TvDevice
import okhttp3.*
import okhttp3.internal.concurrent.Task
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

    init {
        okHttpClient = createTrustAllOkHttpClient()
    }

    fun connect() {
        val client = okHttpClient ?: return
        isClosedManually = false
        onStatusChanged(ConnectionState.CONNECTING)

        val appNameEncoded = "SamsungRemote"
        
        // Step 1: Attempt secure websocket on port 8002 if we have a token or by default
        val tokenParam = if (!tvDevice.token.isNullOrEmpty()) "&token=${tvDevice.token}" else ""
        val wssUrl = "wss://${tvDevice.ip}:8002/api/v2/channels/samsung.remote.control?name=$appNameEncoded$tokenParam"
        
        Log.d("SamsungRemote", "Connecting to WSS: $wssUrl")
        val request = Request.Builder().url(wssUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SamsungRemote", "WSS Open!")
                onStatusChanged(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SamsungRemote", "Message received: $text")
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
                    Log.e("SamsungRemote", "Error parsing incoming WS message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "WSS Closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "WSS Closed: $code / $reason")
                if (!isClosedManually) {
                    onStatusChanged(ConnectionState.DISCONNECTED)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SamsungRemote", "WSS Failure: ${t.message}. Retrying via insecure WS on 8001...", t)
                if (!isClosedManually) {
                    connectInsecureFallback()
                }
            }
        })
    }

    private fun connectInsecureFallback() {
        val client = okHttpClient ?: return
        val wsUrl = "ws://${tvDevice.ip}:8001/api/v2/channels/samsung.remote.control?name=SamsungRemote"
        
        Log.d("SamsungRemote", "Connecting fallback to WS: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SamsungRemote", "WS Open!")
                onStatusChanged(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SamsungRemote", "WS Message received: $text")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SamsungRemote", "WS Closed")
                if (!isClosedManually) {
                    onStatusChanged(ConnectionState.DISCONNECTED)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SamsungRemote", "WS Fallback Failure: ${t.message}", t)
                if (!isClosedManually) {
                    onStatusChanged(ConnectionState.DISCONNECTED)
                }
            }
        })
    }

    fun sendKey(key: String) {
        val ws = webSocket
        if (ws == null) {
            Log.e("SamsungRemote", "Cannot send key: WebSocket is not connected.")
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
        Log.d("SamsungRemote", "Sending key payload: $key")
        ws.send(message)
    }

    fun disconnect() {
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
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e("SamsungRemote", "Error creating TrustAll OkHttpClient", e)
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }
}
