package com.example.wolpanel.data

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

/** A device paired with its most recently observed status, as shown in the list. */
data class DeviceUiState(
    val device: Device,
    val status: DeviceStatus = DeviceStatus.UNKNOWN,
)
