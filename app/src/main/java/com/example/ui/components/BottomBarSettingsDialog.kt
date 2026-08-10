package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showDate by remember { mutableStateOf(settings.showDate) }
    var showSettingsIcon by remember { mutableStateOf(settings.showSettingsIcon) }
    var dateFormat by remember { mutableStateOf(settings.dateFormat) }
    var timeFormat by remember { mutableStateOf(settings.timeFormat) }
    var showClockIcon by remember { mutableStateOf(settings.showClockIcon) }
    var showDateIcon by remember { mutableStateOf(settings.showDateIcon) }
    var showBatteryIcon by remember { mutableStateOf(settings.showBatteryIcon) }

    var orderedItems by remember {
        mutableStateOf(getNormalizedItemsInDialog(settings.itemsOrderAndAlign))
    }

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
                    if (showTime) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Clock Icon", style = MaterialTheme.typography.labelMedium)
                                Switch(
                                    checked = showClockIcon,
                                    onCheckedChange = { showClockIcon = it }
                                )
                            }
                            OutlinedTextField(
                                value = timeFormat,
                                onValueChange = { timeFormat = it },
                                label = { Text("Time Format Pattern", style = MaterialTheme.typography.labelSmall) },
                                placeholder = { Text("e.g. HH:mm or hh:mm a") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            // Live Preview / Helpers
                            val previewText = remember(timeFormat) {
                                try {
                                    java.text.SimpleDateFormat(timeFormat, java.util.Locale.getDefault()).format(java.util.Date())
                                } catch (e: Exception) {
                                    "Invalid format pattern"
                                }
                            }
                            Text(
                                text = "Preview: $previewText",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (previewText == "Invalid format pattern") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            // Suggestion row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("HH:mm", "hh:mm a", "HH:mm:ss").forEach { option ->
                                    SuggestionChip(
                                        onClick = { timeFormat = option },
                                        label = { Text(option, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Date Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Date Display", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showDate,
                            onCheckedChange = { showDate = it }
                        )
                    }
                    if (showDate) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Date Icon", style = MaterialTheme.typography.labelMedium)
                                Switch(
                                    checked = showDateIcon,
                                    onCheckedChange = { showDateIcon = it }
                                )
                            }
                            OutlinedTextField(
                                value = dateFormat,
                                onValueChange = { dateFormat = it },
                                label = { Text("Date Format Pattern", style = MaterialTheme.typography.labelSmall) },
                                placeholder = { Text("e.g. EEE, MMM d") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            // Live Preview / Helpers
                            val previewText = remember(dateFormat) {
                                try {
                                    java.text.SimpleDateFormat(dateFormat, java.util.Locale.getDefault()).format(java.util.Date())
                                } catch (e: Exception) {
                                    "Invalid format pattern"
                                }
                            }
                            Text(
                                text = "Preview: $previewText",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (previewText == "Invalid format pattern") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            // Suggestion row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("EEE, MMM d", "yyyy-MM-dd", "MM/dd/yyyy").forEach { option ->
                                    SuggestionChip(
                                        onClick = { dateFormat = option },
                                        label = { Text(option, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
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
                    if (showBattery) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Battery Icon", style = MaterialTheme.typography.labelMedium)
                                Switch(
                                    checked = showBatteryIcon,
                                    onCheckedChange = { showBatteryIcon = it }
                                )
                            }
                        }
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

                    // Settings Icon Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Settings Menu Icon", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showSettingsIcon,
                            onCheckedChange = { showSettingsIcon = it }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text("Order & Alignment:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    orderedItems.forEachIndexed { index, item ->
                        val (key, align) = item
                        val displayName = when (key) {
                            "wifi" -> "Wi-Fi Indicator"
                            "bluetooth" -> "Bluetooth Indicator"
                            "battery" -> "Battery Status"
                            "date" -> "Date Display"
                            "time" -> "Time Clock"
                            else -> key.replaceFirstChar { it.uppercase() }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Item Name and Reorder Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Up Arrow
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val newList = orderedItems.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index - 1]
                                                newList[index - 1] = temp
                                                orderedItems = newList
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Down Arrow
                                    IconButton(
                                        onClick = {
                                            if (index < orderedItems.size - 1) {
                                                val newList = orderedItems.toMutableList()
                                                val temp = newList[index]
                                                newList[index] = newList[index + 1]
                                                newList[index + 1] = temp
                                                orderedItems = newList
                                            }
                                        },
                                        enabled = index < orderedItems.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Alignment Switcher
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isLeft = align == "left"
                                    FilledTonalButton(
                                        onClick = {
                                            val newList = orderedItems.toMutableList()
                                            newList[index] = key to "left"
                                            orderedItems = newList
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isLeft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isLeft) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Left", style = MaterialTheme.typography.labelSmall)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val newList = orderedItems.toMutableList()
                                            newList[index] = key to "right"
                                            orderedItems = newList
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (!isLeft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            contentColor = if (!isLeft) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Right", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
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
                        val itemsOrderAndAlignStr = orderedItems.joinToString(",") { "${it.first}:${it.second}" }
                        onSaveSettings(
                            BottomBarSettings(
                                showBottomBar = showBar,
                                heightDp = height.toInt(),
                                iconSizeDp = iconSize.toInt(),
                                showTime = showTime,
                                showBattery = showBattery,
                                showWifi = showWifi,
                                showBluetooth = showBluetooth,
                                showDate = showDate,
                                showSettingsIcon = showSettingsIcon,
                                itemsOrderAndAlign = itemsOrderAndAlignStr,
                                dateFormat = dateFormat,
                                timeFormat = timeFormat,
                                showClockIcon = showClockIcon,
                                showDateIcon = showDateIcon,
                                showBatteryIcon = showBatteryIcon
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

private fun getNormalizedItemsInDialog(input: String): List<Pair<String, String>> {
    val defaultList = listOf(
        "bluetooth" to "left",
        "wifi" to "left",
        "battery" to "left",
        "date" to "right",
        "time" to "right"
    )
    if (input.isBlank()) return defaultList
    val parsed = input.split(",").mapNotNull {
        val parts = it.split(":")
        if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else null
    }.toMutableList()

    val allKeys = listOf("bluetooth", "wifi", "battery", "date", "time")
    for (key in allKeys) {
        if (parsed.none { it.first == key }) {
            val defaultAlign = if (key == "date" || key == "time") "right" else "left"
            parsed.add(key to defaultAlign)
        }
    }
    return parsed
}
