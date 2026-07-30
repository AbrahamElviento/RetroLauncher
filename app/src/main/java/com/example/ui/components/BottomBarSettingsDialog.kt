package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BottomBarSettings

@Composable
fun BottomBarSettingsDialog(
    settings: BottomBarSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (BottomBarSettings) -> Unit
) {
    var showBar by remember { mutableStateOf(settings.showBottomBar) }
    var height by remember { mutableFloatStateOf(settings.heightDp.toFloat()) }
    var iconSize by remember { mutableFloatStateOf(settings.iconSizeDp.toFloat()) }
    var showTime by remember { mutableStateOf(settings.showTime) }
    var showBattery by remember { mutableStateOf(settings.showBattery) }
    var showWifi by remember { mutableStateOf(settings.showWifi) }
    var showBluetooth by remember { mutableStateOf(settings.showBluetooth) }

    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bottom Status Bar Customization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Bottom Status Bar", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = showBar,
                        onCheckedChange = { showBar = it }
                    )
                }

                if (showBar) {
                    // Height Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bar Height", style = MaterialTheme.typography.bodySmall)
                            Text("${height.toInt()} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = height,
                            onValueChange = { height = it },
                            valueRange = 24f..64f,
                            steps = 39
                        )
                    }

                    // Icon Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Icon / Font Size", style = MaterialTheme.typography.bodySmall)
                            Text("${iconSize.toInt()} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = iconSize,
                            onValueChange = { iconSize = it },
                            valueRange = 12f..32f,
                            steps = 19
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text("Element Visibility:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    // Time Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Time Clock", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showTime,
                            onCheckedChange = { showTime = it }
                        )
                    }

                    // Battery Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Battery Status", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showBattery,
                            onCheckedChange = { showBattery = it }
                        )
                    }

                    // Wifi Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Wi-Fi State", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showWifi,
                            onCheckedChange = { showWifi = it }
                        )
                    }

                    // Bluetooth Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Bluetooth State", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showBluetooth,
                            onCheckedChange = { showBluetooth = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onSaveSettings(
                            BottomBarSettings(
                                showBottomBar = showBar,
                                heightDp = height.toInt(),
                                iconSizeDp = iconSize.toInt(),
                                showTime = showTime,
                                showBattery = showBattery,
                                showWifi = showWifi,
                                showBluetooth = showBluetooth
                            )
                        )
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
