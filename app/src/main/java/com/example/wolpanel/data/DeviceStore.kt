package com.example.wolpanel.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the device list as a JSON blob in SharedPreferences. Kept deliberately simple
 * (no Room / annotation processors) since the data set is small and flat.
 */
class DeviceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<Device> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it).toDevice() }
        }.getOrDefault(emptyList())
    }

    fun save(devices: List<Device>) {
        val arr = JSONArray()
        devices.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_DEVICES, arr.toString()).apply()
    }

    private fun Device.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("mac", mac)
        put("host", host)
        put("broadcast", broadcast)
        put("port", port)
        put("pingPort", pingPort)
        put("sshHost", sshHost)
    }

    private fun JSONObject.toDevice() = Device(
        id = optString("id"),
        name = optString("name"),
        mac = optString("mac"),
        host = optString("host"),
        broadcast = optString("broadcast", "255.255.255.255"),
        port = optInt("port", 9),
        pingPort = optInt("pingPort", 0),
        sshHost = optString("sshHost", ""),
    )

    private companion object {
        const val PREFS = "wol_panel_prefs"
        const val KEY_DEVICES = "devices"
    }
}
