package com.example.wolpanel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wolpanel.data.Device
import com.example.wolpanel.data.DeviceStatus
import com.example.wolpanel.data.DeviceStore
import com.example.wolpanel.data.DeviceUiState
import com.example.wolpanel.net.Pinger
import com.example.wolpanel.net.WakeOnLan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WolViewModel(app: Application) : AndroidViewModel(app) {

    private val store = DeviceStore(app)

    private val _devices = MutableStateFlow<List<DeviceUiState>>(emptyList())
    val devices: StateFlow<List<DeviceUiState>> = _devices.asStateFlow()

    /** One-shot user feedback (e.g. "Magic packet sent"), consumed by the UI snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var autoCheckJob: Job? = null

    init {
        _devices.value = store.load().map { DeviceUiState(it) }
        startAutoCheck()
    }

    // --- Device CRUD -------------------------------------------------------

    fun addDevice(device: Device) = mutate { it + DeviceUiState(device) }

    fun updateDevice(device: Device) = mutate { list ->
        list.map { if (it.device.id == device.id) it.copy(device = device) else it }
    }

    fun removeDevice(id: String) = mutate { list -> list.filterNot { it.device.id == id } }

    private fun mutate(transform: (List<DeviceUiState>) -> List<DeviceUiState>) {
        val updated = transform(_devices.value)
        _devices.value = updated
        store.save(updated.map { it.device })
        refreshAll()
    }

    // --- Wake --------------------------------------------------------------

    fun wake(device: Device) {
        viewModelScope.launch {
            val result = runCatching {
                WakeOnLan.send(device.mac, device.broadcast, device.port)
            }
            _message.value = if (result.isSuccess) {
                "Magic packet sent to ${device.name}"
            } else {
                "Failed to wake ${device.name}: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun consumeMessage() { _message.value = null }

    // --- Reachability auto-check ------------------------------------------

    private fun startAutoCheck() {
        autoCheckJob?.cancel()
        autoCheckJob = viewModelScope.launch {
            while (true) {
                refreshAll()
                delay(AUTO_CHECK_INTERVAL_MS)
            }
        }
    }

    /** Probes every device concurrently and updates its status. */
    fun refreshAll() {
        val current = _devices.value
        if (current.isEmpty()) return
        setStatus(current.map { it.device.id }, DeviceStatus.CHECKING)
        current.forEach { ui ->
            viewModelScope.launch {
                val online = Pinger.isOnline(ui.device.host, ui.device.pingPort)
                setStatus(listOf(ui.device.id), if (online) DeviceStatus.ONLINE else DeviceStatus.OFFLINE)
            }
        }
    }

    private fun setStatus(ids: List<String>, status: DeviceStatus) {
        _devices.update { list ->
            list.map { if (it.device.id in ids) it.copy(status = status) else it }
        }
    }

    private companion object {
        const val AUTO_CHECK_INTERVAL_MS = 15_000L
    }
}
