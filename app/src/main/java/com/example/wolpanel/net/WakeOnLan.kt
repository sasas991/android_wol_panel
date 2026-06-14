package com.example.wolpanel.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Builds and sends Wake-on-LAN "magic packets".
 *
 * A magic packet is 6 bytes of 0xFF followed by the target MAC repeated 16 times,
 * broadcast over UDP. The NIC's WoL firmware wakes the host when it sees this.
 */
object WakeOnLan {

    /** Sends a magic packet for [mac] to [broadcast]:[port]. Throws on malformed MAC or I/O error. */
    suspend fun send(mac: String, broadcast: String, port: Int) = withContext(Dispatchers.IO) {
        val macBytes = parseMac(mac)
        val payload = ByteArray(6 + 16 * macBytes.size)
        for (i in 0 until 6) payload[i] = 0xFF.toByte()
        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, payload, 6 + i * macBytes.size, macBytes.size)
        }

        val address = InetAddress.getByName(broadcast)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(DatagramPacket(payload, payload.size, address, port))
        }
    }

    /** Parses "AA:BB:CC:DD:EE:FF" / "AA-BB-..." / "AABBCC..." into 6 bytes. */
    fun parseMac(mac: String): ByteArray {
        val hex = mac.trim().replace(":", "").replace("-", "").replace(".", "")
        require(hex.length == 12 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "Invalid MAC address: $mac"
        }
        return ByteArray(6) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
