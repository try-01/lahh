package com.example.data.repository

import android.content.Context
import com.example.data.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("samsung_remote_prefs", Context.MODE_PRIVATE)

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _screenAlwaysOn = MutableStateFlow(prefs.getBoolean("screen_always_on", true))
    val screenAlwaysOn: StateFlow<Boolean> = _screenAlwaysOn.asStateFlow()

    private val _dynamicMeshEnabled = MutableStateFlow(prefs.getBoolean("dynamic_mesh_enabled", true))
    val dynamicMeshEnabled: StateFlow<Boolean> = _dynamicMeshEnabled.asStateFlow()

    private val _remoteScaleSize = MutableStateFlow(prefs.getString("remote_scale_size", "fit") ?: "fit")
    val remoteScaleSize: StateFlow<String> = _remoteScaleSize.asStateFlow()

    private val _savedTv = MutableStateFlow(getSavedTvDevice())
    val savedTv: StateFlow<TvDevice?> = _savedTv.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setScreenAlwaysOn(enabled: Boolean) {
        prefs.edit().putBoolean("screen_always_on", enabled).apply()
        _screenAlwaysOn.value = enabled
    }

    fun setDynamicMeshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_mesh_enabled", enabled).apply()
        _dynamicMeshEnabled.value = enabled
    }

    fun setRemoteScaleSize(size: String) {
        prefs.edit().putString("remote_scale_size", size).apply()
        _remoteScaleSize.value = size
    }

    fun saveTvDevice(tv: TvDevice) {
        prefs.edit().apply {
            putString("tv_ip", tv.ip)
            putInt("tv_port", tv.port)
            putString("tv_name", tv.name)
            putString("tv_model", tv.model)
            putString("tv_mac", tv.macAddress)
            putString("tv_token", tv.token)
            putString("tv_signal", tv.signalStrength)
        }.apply()
        _savedTv.value = tv
    }

    fun forgetTvDevice() {
        prefs.edit().apply {
            remove("tv_ip")
            remove("tv_port")
            remove("tv_name")
            remove("tv_model")
            remove("tv_mac")
            remove("tv_token")
            remove("tv_signal")
        }.apply()
        _savedTv.value = null
    }

    private fun getSavedTvDevice(): TvDevice? {
        val ip = prefs.getString("tv_ip", null) ?: return null
        val port = prefs.getInt("tv_port", 8001)
        val name = prefs.getString("tv_name", "Samsung UA32N4300") ?: "Samsung UA32N4300"
        val model = prefs.getString("tv_model", "N-Series · Tizen 5.0") ?: "N-Series · Tizen 5.0"
        val mac = prefs.getString("tv_mac", "A1:B2:C3:D4:E5") ?: "A1:B2:C3:D4:E5"
        val token = prefs.getString("tv_token", null)
        val signal = prefs.getString("tv_signal", "Bagus") ?: "Bagus"
        return TvDevice(ip, port, name, model, mac, token, signal)
    }
}
