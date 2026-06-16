package com.example.wolpanel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wolpanel.data.Device
import com.example.wolpanel.data.DeviceStatus
import com.example.wolpanel.data.DeviceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WolPanelScreen(
    devices: List<DeviceUiState>,
    message: String?,
    onWake: (Device) -> Unit,
    onSsh: (Device) -> Unit,
    onAdd: (Device) -> Unit,
    onUpdate: (Device) -> Unit,
    onRemove: (String) -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Device?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Wake-on-LAN Panel") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh status")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add device")
            }
        },
    ) { padding ->
        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No devices yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(devices, key = { it.device.id }) { ui ->
                    DeviceCard(
                        ui = ui,
                        onWake = { onWake(ui.device) },
                        onSsh = { onSsh(ui.device) },
                        onEdit = { editing = ui.device; showEditor = true },
                        onDelete = { onRemove(ui.device.id) },
                    )
                }
            }
        }
    }

    if (showEditor) {
        DeviceEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { device ->
                if (editing == null) onAdd(device) else onUpdate(device)
                showEditor = false
            },
        )
    }
}

@Composable
private fun DeviceCard(
    ui: DeviceUiState,
    onWake: () -> Unit,
    onSsh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(ui.status)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    ui.device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${ui.device.host}  ·  ${ui.device.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    statusLabel(ui.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(ui.status),
                )
            }
            if (ui.device.sshHost.isNotBlank()) {
                IconButton(onClick = onSsh) {
                    Icon(Icons.Filled.Terminal, contentDescription = "SSH to ${ui.device.name}")
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${ui.device.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${ui.device.name}")
            }
            Button(onClick = onWake, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Filled.Power, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Wake", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun StatusDot(status: DeviceStatus) {
    if (status == DeviceStatus.CHECKING) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
    } else {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(statusColor(status)),
        )
    }
}

private fun statusLabel(status: DeviceStatus): String = when (status) {
    DeviceStatus.UNKNOWN -> "Unknown"
    DeviceStatus.CHECKING -> "Checking…"
    DeviceStatus.ONLINE -> "Online"
    DeviceStatus.OFFLINE -> "Offline"
}

@Composable
private fun statusColor(status: DeviceStatus): Color = when (status) {
    DeviceStatus.ONLINE -> Color(0xFF2E7D32)
    DeviceStatus.OFFLINE -> Color(0xFFC62828)
    DeviceStatus.CHECKING -> MaterialTheme.colorScheme.primary
    DeviceStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
}
