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
            Log.d("RemoteViewModel", "Auto-connecting to saved TV: ${saved.name} at ${saved.ip}")
            setupWebSocketAndConnect(saved)
        } else {
            Log.d("RemoteViewModel", "No saved TV found.")
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

        Log.d("RemoteViewModel", "Setting up WebSocket to ${tv.ip}:${tv.port}")

        activeSocket = SamsungTvWebSocket(
            tvDevice = tv,
            onTokenReceived = { token ->
                Log.d("RemoteViewModel", "Token received, saving")
                val updatedTv = tv.copy(token = token)
                repository.saveTvDevice(updatedTv)
            },
            onStatusChanged = { status ->
                Log.d("RemoteViewModel", "Connection status: $status")
                _connectionState.value = status
            }
        )
        activeSocket?.connect()
    }

    fun connectToTv(tv: TvDevice) {
        Log.d("RemoteViewModel", "Connecting to TV: ${tv.name} at ${tv.ip}")
        repository.saveTvDevice(tv)
        setupWebSocketAndConnect(tv)
    }

    fun reconnectTv() {
        val tv = savedTvState.value
        if (tv == null) {
            Log.e("RemoteViewModel", "Cannot reconnect: no saved TV")
            _reconnectModalState.value = ModalProgress.IDLE
            return
        }

        viewModelScope.launch {
            Log.d("RemoteViewModel", "Reconnecting to saved TV: ${tv.name}")
            _reconnectModalState.value = ModalProgress.LOADING

            activeSocket?.disconnect()
            delay(500)

            setupWebSocketAndConnect(tv)

            delay(2000)

            if (_connectionState.value == SamsungTvWebSocket.ConnectionState.CONNECTED) {
                _reconnectModalState.value = ModalProgress.SUCCESS
                triggerVibration(15)
            } else {
                Log.w("RemoteViewModel", "Reconnection did not succeed within timeout")
                _reconnectModalState.value = ModalProgress.IDLE
            }
        }
    }

    fun resetReconnectModal() {
        _reconnectModalState.value = ModalProgress.IDLE
    }

    fun connectManual(ip: String) {
        if (ip.isBlank()) {
            Log.w("RemoteViewModel", "IP address is blank")
            return
        }

        Log.d("RemoteViewModel", "Manual connection to IP: $ip")
        val newTv = TvDevice(
            ip = ip,
            port = 8002,
            name = "Samsung TV (Manual)",
            model = "Smart TV",
            macAddress = "AA:BB:CC:DD:EE:FF",
            signalStrength = "Bagus"
        )
        connectToTv(newTv)
    }

    fun forgetTv() {
        Log.d("RemoteViewModel", "Forgetting saved TV")
        activeSocket?.disconnect()
        activeSocket = null
        repository.forgetTvDevice()
        _connectionState.value = SamsungTvWebSocket.ConnectionState.DISCONNECTED
        triggerVibration(10)
    }

    fun startSsdpDiscovery() {
        scanJob?.cancel()
        _discoveredTvs.value = emptyList()
        _isDiscovering.value = true

        Log.d("RemoteViewModel", "Starting SSDP discovery")

        scanJob = viewModelScope.launch {
            try {
                SsdpDiscovery.discoverTvs().collect { device ->
                    Log.d("RemoteViewModel", "Discovered: ${device.name} at ${device.ip}")
                    val current = _discoveredTvs.value
                    if (current.none { it.ip == device.ip }) {
                        _discoveredTvs.value = current + device
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteViewModel", "SSDP discovery error", e)
            } finally {
                _isDiscovering.value = false
                Log.d("RemoteViewModel", "Discovery finished. Found ${_discoveredTvs.value.size} devices")
            }
        }
    }

    fun stopSsdpDiscovery() {
        Log.d("RemoteViewModel", "Stopping SSDP discovery")
        scanJob?.cancel()
        scanJob = null
        _isDiscovering.value = false
    }

    fun sendRemoteKey(key: String) {
        if (_connectionState.value != SamsungTvWebSocket.ConnectionState.CONNECTED) {
            Log.w("RemoteViewModel", "Cannot send key '$key': not connected")
            return
        }

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
            Log.e("RemoteViewModel", "Error firing haptic feedback", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("RemoteViewModel", "ViewModel cleared, disconnecting")
        activeSocket?.disconnect()
        scanJob?.cancel()
    }
}
