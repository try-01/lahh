package com.example.data.model

data class TvDevice(
    val ip: String,
    val port: Int = 8002,
    val name: String = "Samsung TV",
    val model: String = "Smart TV",
    val macAddress: String = "AA:BB:CC:DD:EE:FF",
    val token: String? = null,
    val signalStrength: String = "Bagus"
)
