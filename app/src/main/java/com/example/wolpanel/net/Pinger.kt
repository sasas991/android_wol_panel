package com.example.wolpanel.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Probes whether a host is reachable.
 *
 * Android can rarely send raw ICMP without root, so [InetAddress.isReachable] is unreliable.
 * We therefore prefer a TCP connect to a known-open port when one is configured, and fall back
 * to [InetAddress.isReachable] (which may use ICMP or a port-7 TCP echo internally).
 */
object Pinger {

    private const val TIMEOUT_MS = 1500

    /** Returns true if [host] looks online. [pingPort] of 0 means "no TCP probe, use isReachable". */
    suspend fun isOnline(host: String, pingPort: Int): Boolean = withContext(Dispatchers.IO) {
        if (host.isBlank()) return@withContext false

        if (pingPort > 0 && tcpReachable(host, pingPort)) return@withContext true

        runCatching {
            InetAddress.getByName(host).isReachable(TIMEOUT_MS)
        }.getOrDefault(false)
    }

    private fun tcpReachable(host: String, port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            true
        }
    }.getOrDefault(false)
}
