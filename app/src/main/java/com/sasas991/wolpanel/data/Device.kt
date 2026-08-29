package com.sasas991.wolpanel.data

import java.util.UUID

/**
 * A device that can be woken over LAN and probed for reachability.
 *
 * @param mac the target's MAC address, e.g. "AA:BB:CC:DD:EE:FF". Used to build the magic packet.
 * @param host hostname or IP used for the online/offline ping (e.g. "192.168.1.50").
 * @param broadcast broadcast address the magic packet is sent to (e.g. "192.168.1.255").
 * @param port UDP port for the magic packet (commonly 9, sometimes 7).
 * @param pingPort TCP port used to probe reachability when ICMP is unavailable (e.g. 22, 80, 3389).
 * @param sshHost SSH target passed to `ssh` in Termux, e.g. "pi@192.168.1.50" or "myserver". Empty = SSH disabled.
 */
data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mac: String,
    val host: String,
    val broadcast: String = "255.255.255.255",
    val port: Int = 9,
    val pingPort: Int = 0,
    val sshHost: String = "",
)

/** Reachability state of a device, refreshed by the auto-check loop. */
enum class DeviceStatus { UNKNOWN, CHECKING, ONLINE, OFFLINE }

/**
 * Progress of the most recent wake attempt, surfaced on the device card so the user
 * can see both that the magic packet went out and whether the host actually woke.
 *
 * SENDING  -> building/broadcasting the magic packet
 * WAKING   -> packet sent; polling the host, waiting for it to come online
 * AWAKE    -> host responded after the packet — the wake worked
 * NO_REPLY -> packet was sent but the host never came online within the wait window
 * FAILED   -> the packet could not be sent (bad MAC, I/O error, …)
 */
enum class WakePhase { SENDING, WAKING, AWAKE, NO_REPLY, FAILED }

/** Feedback for a single wake attempt. [at] disambiguates stale updates from newer taps. */
data class WakeSignal(
    val phase: WakePhase,
    val at: Long,
    val detail: String = "",
)

/** A device paired with its most recently observed status, as shown in the list. */
data class DeviceUiState(
    val device: Device,
    val status: DeviceStatus = DeviceStatus.UNKNOWN,
    val wake: WakeSignal? = null,
)
