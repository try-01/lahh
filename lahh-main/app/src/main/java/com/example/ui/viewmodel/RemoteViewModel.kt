package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TvDevice
import com.example.data.repository.SettingsRepository
import com.example.network.SamsungTvWebSocket
import com.example.network.SsdpDiscovery
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private var activeSocket: SamsungTvWebSocket? = null
    private var scanJob: Job? = null

    val hapticsEnabledState = repository.hapticsEnabled
    val screenAlwaysOnState = repository.screenAlwaysOn
    val dynamicMeshEnabledState = repository.dynamicMeshEnabled
    val remoteScaleSizeState = repository.remoteScaleSize
    val savedTvState = repository.savedTv

    private val _connectionState = MutableStateFlow(SamsungTvWebSocket.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SamsungTvWebSocket.ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredTvs = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredTvs: StateFlow<List<TvDevice>> = _discoveredTvs.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _reconnectModalState = MutableStateFlow<ModalProgress>(ModalProgress.IDLE)
    val reconnectModalState: StateFlow<ModalProgress> = _reconnectModalState.asStateFlow()

    enum class ModalProgress {
        IDLE, LOADING, SUCCESS
    }

    init {
        val saved = savedTvState.value
        if (saved != null) {
            setupWebSocketAndConnect(saved)
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        repository.setHapticsEnabled(enabled)
        triggerVibration(10)
    }

    fun toggleScreenAlwaysOn(enabled: Boolean) {
        repository.setScreenAlwaysOn(enabled)
        triggerVibration(10)
    }

    fun toggleDynamicMesh(enabled: Boolean) {
        repository.setDynamicMeshEnabled(enabled)
        triggerVibration(10)
    }

    fun setRemoteScaleSize(size: String) {
        repository.setRemoteScaleSize(size)
        triggerVibration(10)
    }

    private fun setupWebSocketAndConnect(tv: TvDevice) {
        activeSocket?.disconnect()

        activeSocket = SamsungTvWebSocket(
            tvDevice = tv,
            onTokenReceived = { token ->
                val updatedTv = tv.copy(token = token)
                repository.saveTvDevice(updatedTv)
            },
            onStatusChanged = { status ->
                _connectionState.value = status
            }
        )
        activeSocket?.connect()
    }

    fun connectToTv(tv: TvDevice) {
        repository.saveTvDevice(tv)
        setupWebSocketAndConnect(tv)
    }

    fun reconnectTv() {
        val tv = savedTvState.value ?: TvDevice(ip = "192.168.1.42")
        viewModelScope.launch {
            _reconnectModalState.value = ModalProgress.LOADING
            setupWebSocketAndConnect(tv)
            delay(1600)
            _reconnectModalState.value = ModalProgress.SUCCESS
            triggerVibration(15)
        }
    }

    fun resetReconnectModal() {
        _reconnectModalState.value = ModalProgress.IDLE
    }

    fun connectManual(ip: String) {
        if (ip.isBlank()) return
        val newTv = TvDevice(
            ip = ip,
            name = "Samsung TV (Manual)",
            macAddress = "A1:B2:C3:D4:E5:FF",
            signalStrength = "Bagus"
        )
        connectToTv(newTv)
    }

    fun forgetTv() {
        activeSocket?.disconnect()
        repository.forgetTvDevice()
        _connectionState.value = SamsungTvWebSocket.ConnectionState.DISCONNECTED
        triggerVibration(10)
    }

    fun startSsdpDiscovery() {
        scanJob?.cancel()
        _discoveredTvs.value = emptyList()
        _isDiscovering.value = true

        scanJob = viewModelScope.launch {
            SsdpDiscovery.discoverTvs(getApplication()).collect { device ->
                val current = _discoveredTvs.value
                if (current.none { it.ip == device.ip }) {
                    _discoveredTvs.value = current + device
                }
            }
            _isDiscovering.value = false
        }
    }

    fun stopSsdpDiscovery() {
        scanJob?.cancel()
        _isDiscovering.value = false
    }

    fun sendRemoteKey(key: String) {
        triggerVibration(12)
        activeSocket?.sendKey(key)
    }

    fun triggerVibration(durationMs: Long) {
        if (!hapticsEnabledState.value) return
        val context = getApplication<Application>().applicationContext
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e("SamsungRemoteVM", "Error firing haptic feedback", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeSocket?.disconnect()
        scanJob?.cancel()
    }
}
