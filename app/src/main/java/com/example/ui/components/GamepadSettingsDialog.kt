package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GamepadSettings

@Composable
fun GamepadSettingsDialog(
    settings: GamepadSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (GamepadSettings) -> Unit
) {
    var keyPageUp by remember { mutableIntStateOf(settings.keyPageUp) }
    var keyPageDown by remember { mutableIntStateOf(settings.keyPageDown) }
    var keyGoToTop by remember { mutableIntStateOf(settings.keyGoToTop) }
    var keyGoToBottom by remember { mutableIntStateOf(settings.keyGoToBottom) }
    var keySystemSettings by remember { mutableIntStateOf(settings.keySystemSettings) }
    var keyRomListSettings by remember { mutableIntStateOf(settings.keyRomListSettings) }
    var keySelectAction by remember { mutableIntStateOf(settings.keySelectAction) }
    var keyBackAction by remember { mutableIntStateOf(settings.keyBackAction) }
    var keyFavoriteAction by remember { mutableIntStateOf(settings.keyFavoriteAction) }
    var keyInfoAction by remember { mutableIntStateOf(settings.keyInfoAction) }
    var keyOpenSearch by remember { mutableIntStateOf(settings.keyOpenSearch) }
    var keySystemManagerAction by remember { mutableIntStateOf(settings.keySystemManagerAction) }

    var capturingActionName by remember { mutableStateOf<String?>(null) }

    fun getKeyLabel(keyCode: Int): String {
        if (keyCode <= 0) return "Unset"
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L1, 102 -> "L1 / L Shoulder"
            KeyEvent.KEYCODE_BUTTON_R1, 103 -> "R1 / R Shoulder"
            KeyEvent.KEYCODE_BUTTON_L2, 104 -> "L2 / L Trigger"
            KeyEvent.KEYCODE_BUTTON_R2, 105 -> "R2 / R Trigger"
            KeyEvent.KEYCODE_BUTTON_THUMBL, 106 -> "L Thumbstick Press"
            KeyEvent.KEYCODE_BUTTON_THUMBR, 107 -> "R Thumbstick Press"
            KeyEvent.KEYCODE_BUTTON_START, 108 -> "START Button"
            KeyEvent.KEYCODE_BUTTON_SELECT, 109 -> "SELECT Button"
            KeyEvent.KEYCODE_BUTTON_A, 96 -> "A Button / Cross"
            KeyEvent.KEYCODE_BUTTON_B, 97 -> "B Button / Circle"
            KeyEvent.KEYCODE_BUTTON_X, 99 -> "X Button / Square"
            KeyEvent.KEYCODE_BUTTON_Y, 100 -> "Y Button / Triangle"
            KeyEvent.KEYCODE_PAGE_UP -> "Page Up"
            KeyEvent.KEYCODE_PAGE_DOWN -> "Page Down"
            KeyEvent.KEYCODE_DPAD_CENTER -> "D-Pad Center"
            else -> if (keyCode > 0) KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_") else "Key Code $keyCode"
        }
    }

    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .onPreviewKeyEvent { keyEvent ->
                    if (capturingActionName != null && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        var code = keyEvent.nativeKeyEvent.keyCode
                        if (code == 0 || code == KeyEvent.KEYCODE_UNKNOWN) {
                            val scan = keyEvent.nativeKeyEvent.scanCode
                            if (scan > 0) code = scan
                        }
                        if (code != 0) {
                            when (capturingActionName) {
                                "page_up" -> keyPageUp = code
                                "page_down" -> keyPageDown = code
                                "go_top" -> keyGoToTop = code
                                "go_bottom" -> keyGoToBottom = code
                                "sys_settings" -> keySystemSettings = code
                                "rom_settings" -> keyRomListSettings = code
                                "select_action" -> keySelectAction = code
                                "back_action" -> keyBackAction = code
                                "fav_action" -> keyFavoriteAction = code
                                "info_action" -> keyInfoAction = code
                                "search_action" -> keyOpenSearch = code
                                "sys_manager" -> keySystemManagerAction = code
                            }
                            capturingActionName = null
                            true
                        } else false
                    } else false
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gamepad Button Mapping",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Tap any field, then press a key on your connected controller to remap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mapping Items
                    KeyMappingRow("Page Up List (L / L1)", getKeyLabel(keyPageUp), capturingActionName == "page_up", onClick = { capturingActionName = "page_up" }, onUnset = { keyPageUp = 0 })
                    KeyMappingRow("Page Down List (R / R1)", getKeyLabel(keyPageDown), capturingActionName == "page_down", onClick = { capturingActionName = "page_down" }, onUnset = { keyPageDown = 0 })
                    KeyMappingRow("Jump to First Item (L2)", getKeyLabel(keyGoToTop), capturingActionName == "go_top", onClick = { capturingActionName = "go_top" }, onUnset = { keyGoToTop = 0 })
                    KeyMappingRow("Jump to Last Item (R2)", getKeyLabel(keyGoToBottom), capturingActionName == "go_bottom", onClick = { capturingActionName = "go_bottom" }, onUnset = { keyGoToBottom = 0 })
                    KeyMappingRow("Open System Settings (START in ROM list)", getKeyLabel(keySystemSettings), capturingActionName == "sys_settings", onClick = { capturingActionName = "sys_settings" }, onUnset = { keySystemSettings = 0 })
                    KeyMappingRow("Open ROM List Settings (SELECT in ROM list)", getKeyLabel(keyRomListSettings), capturingActionName == "rom_settings", onClick = { capturingActionName = "rom_settings" }, onUnset = { keyRomListSettings = 0 })
                    KeyMappingRow("Open Emulator System Manager", getKeyLabel(keySystemManagerAction), capturingActionName == "sys_manager", onClick = { capturingActionName = "sys_manager" }, onUnset = { keySystemManagerAction = 0 })
                    KeyMappingRow("A: Open Folder / Launch ROM", getKeyLabel(keySelectAction), capturingActionName == "select_action", onClick = { capturingActionName = "select_action" }, onUnset = { keySelectAction = 0 })
                    KeyMappingRow("B: Go Back / Favorites (Main Menu)", getKeyLabel(keyBackAction), capturingActionName == "back_action", onClick = { capturingActionName = "back_action" }, onUnset = { keyBackAction = 0 })
                    KeyMappingRow("Y: Toggle Favorite (in ROM list)", getKeyLabel(keyFavoriteAction), capturingActionName == "fav_action", onClick = { capturingActionName = "fav_action" }, onUnset = { keyFavoriteAction = 0 })
                    KeyMappingRow("X: Show ROM Info / Stats (in ROM list)", getKeyLabel(keyInfoAction), capturingActionName == "info_action", onClick = { capturingActionName = "info_action" }, onUnset = { keyInfoAction = 0 })
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val def = GamepadSettings()
                            keyPageUp = def.keyPageUp
                            keyPageDown = def.keyPageDown
                            keyGoToTop = def.keyGoToTop
                            keyGoToBottom = def.keyGoToBottom
                            keySystemSettings = def.keySystemSettings
                            keyRomListSettings = def.keyRomListSettings
                            keySelectAction = def.keySelectAction
                            keyBackAction = def.keyBackAction
                            keyFavoriteAction = def.keyFavoriteAction
                            keyInfoAction = def.keyInfoAction
                            keyOpenSearch = def.keyOpenSearch
                            keySystemManagerAction = def.keySystemManagerAction
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 12.sp)
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            onSaveSettings(
                                GamepadSettings(
                                    keyPageUp = keyPageUp,
                                    keyPageDown = keyPageDown,
                                    keyGoToTop = keyGoToTop,
                                    keyGoToBottom = keyGoToBottom,
                                    keySystemSettings = keySystemSettings,
                                    keyRomListSettings = keyRomListSettings,
                                    keySelectAction = keySelectAction,
                                    keyBackAction = keyBackAction,
                                    keyFavoriteAction = keyFavoriteAction,
                                    keyInfoAction = keyInfoAction,
                                    keyOpenSearch = keyOpenSearch,
                                    keySystemManagerAction = keySystemManagerAction
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
}

@Composable
private fun KeyMappingRow(
    actionLabel: String,
    keyLabel: String,
    isCapturing: Boolean,
    onClick: () -> Unit,
    onUnset: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCapturing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (isCapturing) 2.dp else 1.dp,
                color = if (isCapturing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (isCapturing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (isCapturing) "Press Key..." else keyLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCapturing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (onUnset != null && keyLabel != "Unset") {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onUnset,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unset Mapped Key",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
