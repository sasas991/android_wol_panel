package com.example.wolpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wolpanel.ui.WolPanelScreen
import com.example.wolpanel.ui.theme.WolPanelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WolPanelTheme {
                val vm: WolViewModel = viewModel()
                val devices by vm.devices.collectAsStateWithLifecycle()
                val message by vm.message.collectAsStateWithLifecycle()

                WolPanelScreen(
                    devices = devices,
                    message = message,
                    onWake = vm::wake,
                    onAdd = vm::addDevice,
                    onUpdate = vm::updateDevice,
                    onRemove = vm::removeDevice,
                    onRefresh = vm::refreshAll,
                    onMessageShown = vm::consumeMessage,
                )
            }
        }
    }
}
