package com.example.wolpanel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.wolpanel.R
import com.example.wolpanel.data.Device
import com.example.wolpanel.data.DeviceStatus
import com.example.wolpanel.data.DeviceUiState
import com.example.wolpanel.data.WakePhase
import com.example.wolpanel.data.WakeSignal

private const val GITHUB_PROFILE_URL = "https://github.com/sasas991"

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
        bottomBar = { AuthorFooter() },
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

/** Watermark plus a link out to the author's GitHub profile. */
@Composable
private fun AuthorFooter() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "made by sasas991",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Outlined so the icon reads as a button rather than decoration.
        OutlinedIconButton(
            onClick = { uriHandler.openUri(GITHUB_PROFILE_URL) },
            modifier = Modifier.padding(start = 8.dp).size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "Open sasas991 on GitHub",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
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
    var lastWake by remember { mutableStateOf<WakeSignal?>(null) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Button(onClick = onWake) {
                    Icon(Icons.Filled.Power, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Wake", modifier = Modifier.padding(start = 6.dp))
                }
                DeviceOverflowMenu(
                    deviceName = ui.device.name,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${ui.device.host}  ·  ${ui.device.mac}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(
                visible = ui.wake != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                // Keep showing the last signal while animating out, so it doesn't blank mid-collapse.
                lastWake = ui.wake ?: lastWake
                lastWake?.let {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        WakeBanner(it)
                    }
                }
            }
        }
    }
}

/** Inline banner reporting the progress of the latest wake: packet sent, waking, awake, or failed. */
@Composable
private fun WakeBanner(signal: WakeSignal) {
    val text = when (signal.phase) {
        WakePhase.SENDING -> "Sending magic packet…"
        WakePhase.WAKING -> "Magic packet sent · waking…"
        WakePhase.AWAKE -> "Awake — the wake worked"
        WakePhase.NO_REPLY -> "Packet sent · no reply yet"
        WakePhase.FAILED -> "Couldn't send packet: ${signal.detail}"
    }
    val color = when (signal.phase) {
        WakePhase.AWAKE -> Color(0xFF2E7D32)
        WakePhase.NO_REPLY -> Color(0xFFF9A825)
        WakePhase.FAILED -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (signal.phase) {
            WakePhase.SENDING, WakePhase.WAKING ->
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = color,
                )
            WakePhase.AWAKE ->
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            WakePhase.NO_REPLY ->
                Icon(Icons.Filled.Send, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            WakePhase.FAILED ->
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun DeviceOverflowMenu(
    deviceName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options for $deviceName")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Change") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
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
