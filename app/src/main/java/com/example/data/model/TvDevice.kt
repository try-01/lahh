package com.example.data.model

data class TvDevice(
    val ip: String,
    val port: Int = 8001, // 8001 (WS) or 8002 (WSS)
    val name: String = "Samsung UA32N4300",
    val model: String = "N-Series · Tizen 5.0",
    val macAddress: String = "A1:B2:C3:D4:E5",
    val token: String? = null,
    val signalStrength: String = "Bagus"
)
