package com.sasas991.wolpanel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sasas991.wolpanel.data.Device
import com.sasas991.wolpanel.data.DeviceStatus
import com.sasas991.wolpanel.data.DeviceStore
import com.sasas991.wolpanel.data.DeviceUiState
import com.sasas991.wolpanel.data.WakePhase
import com.sasas991.wolpanel.data.WakeSignal
import com.sasas991.wolpanel.net.Pinger
import com.sasas991.wolpanel.net.TermuxSsh
import com.sasas991.wolpanel.net.WakeOnLan
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
        val startedAt = System.currentTimeMillis()
        setWake(device.id, WakeSignal(WakePhase.SENDING, startedAt))
        viewModelScope.launch {
            val result = runCatching {
                WakeOnLan.send(device.mac, device.broadcast, device.port)
            }
            if (result.isFailure) {
                val reason = result.exceptionOrNull()?.message ?: "unknown error"
                setWake(device.id, WakeSignal(WakePhase.FAILED, startedAt, reason))
                _message.value = "Failed to wake ${device.name}: $reason"
                clearWakeLater(device.id, startedAt)
                return@launch
            }

            // Packet is out — tell the user, then confirm the host actually wakes.
            setWake(device.id, WakeSignal(WakePhase.WAKING, startedAt))
            _message.value = "Magic packet sent to ${device.name}"

            val woke = awaitOnline(device)
            val phase = if (woke) WakePhase.AWAKE else WakePhase.NO_REPLY
            setWake(device.id, WakeSignal(phase, startedAt))
            clearWakeLater(device.id, startedAt)
        }
    }

    /** Polls [device] until it responds or the wait window elapses. Returns true if it came online. */
    private suspend fun awaitOnline(device: Device): Boolean {
        val deadline = System.currentTimeMillis() + WAKE_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            setStatus(listOf(device.id), DeviceStatus.CHECKING)
            if (Pinger.isOnline(device.host, device.pingPort)) {
                setStatus(listOf(device.id), DeviceStatus.ONLINE)
                return true
            }
            setStatus(listOf(device.id), DeviceStatus.OFFLINE)
            delay(WAKE_POLL_INTERVAL_MS)
        }
        return false
    }

    private fun setWake(id: String, signal: WakeSignal) {
        _devices.update { list ->
            list.map { if (it.device.id == id) it.copy(wake = signal) else it }
        }
    }

    /** Clears the wake chip a little after it reaches a terminal state, unless a newer wake replaced it. */
    private fun clearWakeLater(id: String, startedAt: Long) {
        viewModelScope.launch {
            delay(WAKE_CHIP_LINGER_MS)
            _devices.update { list ->
                list.map {
                    if (it.device.id == id && it.wake?.at == startedAt) it.copy(wake = null) else it
                }
            }
        }
    }

    fun consumeMessage() { _message.value = null }

    // --- SSH ---------------------------------------------------------------

    fun openSsh(device: Device) {
        if (device.sshHost.isBlank()) {
            _message.value = "Set an SSH host for ${device.name} first"
            return
        }
        _message.value = when (val r = TermuxSsh.open(getApplication(), device.sshHost)) {
            is TermuxSsh.Result.CommandDispatched -> "Opening ssh ${device.sshHost} in Termux"
            is TermuxSsh.Result.OpenedWithClipboard ->
                "Termux opened — paste the ssh command (copied to clipboard)"
            is TermuxSsh.Result.TermuxNotInstalled -> "Termux is not installed"
            is TermuxSsh.Result.Failed -> "SSH launch failed: ${r.reason}"
        }
    }

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
        const val WAKE_WAIT_MS = 90_000L
        const val WAKE_POLL_INTERVAL_MS = 3_000L
        const val WAKE_CHIP_LINGER_MS = 8_000L
    }
}
