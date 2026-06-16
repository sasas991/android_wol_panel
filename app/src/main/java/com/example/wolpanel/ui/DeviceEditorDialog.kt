package com.example.wolpanel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.wolpanel.data.Device
import com.example.wolpanel.net.WakeOnLan

/**
 * Dialog to add a new device ([initial] == null) or edit an existing one.
 * Validates that name, MAC and host are present and the MAC parses.
 */
@Composable
fun DeviceEditorDialog(
    initial: Device?,
    onDismiss: () -> Unit,
    onSave: (Device) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var mac by remember { mutableStateOf(initial?.mac ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var broadcast by remember { mutableStateOf(initial?.broadcast ?: "255.255.255.255") }
    var port by remember { mutableStateOf((initial?.port ?: 9).toString()) }
    var pingPort by remember { mutableStateOf((initial?.pingPort ?: 0).toString()) }
    var sshHost by remember { mutableStateOf(initial?.sshHost ?: "") }

    val macValid = runCatching { WakeOnLan.parseMac(mac) }.isSuccess
    val canSave = name.isNotBlank() && host.isNotBlank() && macValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add device" else "Edit device") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mac, onValueChange = { mac = it },
                    label = { Text("MAC address") },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    isError = mac.isNotBlank() && !macValid,
                    supportingText = if (mac.isNotBlank() && !macValid) {
                        { Text("Enter 6 hex byte pairs") }
                    } else null,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = host, onValueChange = { host = it },
                    label = { Text("Host / IP (for status check)") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = broadcast, onValueChange = { broadcast = it },
                    label = { Text("Broadcast address") },
                    placeholder = { Text("192.168.1.255") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port, onValueChange = { port = it.filter(Char::isDigit) },
                        label = { Text("WoL port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = pingPort, onValueChange = { pingPort = it.filter(Char::isDigit) },
                        label = { Text("Ping TCP port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = sshHost, onValueChange = { sshHost = it },
                    label = { Text("SSH host (optional)") },
                    placeholder = { Text("user@192.168.1.50") },
                    supportingText = { Text("Opens Termux and runs `ssh <host>`") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        Device(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            mac = mac.trim(),
                            host = host.trim(),
                            broadcast = broadcast.trim().ifBlank { "255.255.255.255" },
                            port = port.toIntOrNull() ?: 9,
                            pingPort = pingPort.toIntOrNull() ?: 0,
                            sshHost = sshHost.trim(),
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
