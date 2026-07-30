package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RomListSettings
import com.example.data.model.RomListStyle
import com.example.data.model.TextAlignmentOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListStyleSettingsDialog(
    settings: RomListSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (RomListSettings) -> Unit,
    onOpenAppVisibility: (() -> Unit)? = null,
    isAndroidAppsSystem: Boolean = false
) {
    var style by remember { mutableStateOf(settings.listStyle) }
    var textSize by remember { mutableFloatStateOf(settings.textSizeSp.toFloat()) }
    var margin by remember { mutableFloatStateOf(settings.marginDp.toFloat()) }
    var alignment by remember { mutableStateOf(settings.textAlignment) }
    var showArtwork by remember { mutableStateOf(settings.showArtworkInTextOnly) }
    var gridScalePercent by remember { mutableFloatStateOf(settings.gridScalePercent.toFloat()) }

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
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROM List Style Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isAndroidAppsSystem && onOpenAppVisibility != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenAppVisibility()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Visible Android Apps")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }

                // Choose Style Mode
                Text("Select Display Layout:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = style == RomListStyle.GRID,
                        onClick = {
                            style = RomListStyle.GRID
                            onUpdateSettings(settings.copy(listStyle = RomListStyle.GRID))
                        },
                        label = { Text("Grid") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = style == RomListStyle.LIST,
                        onClick = {
                            style = RomListStyle.LIST
                            onUpdateSettings(settings.copy(listStyle = RomListStyle.LIST))
                        },
                        label = { Text("Card List") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = style == RomListStyle.TEXT_ONLY,
                        onClick = {
                            style = RomListStyle.TEXT_ONLY
                            onUpdateSettings(settings.copy(listStyle = RomListStyle.TEXT_ONLY))
                        },
                        label = { Text("Text Only") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Grid / Icon Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Icon Grid Size (%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("${gridScalePercent.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = gridScalePercent,
                        onValueChange = {
                            gridScalePercent = it
                            onUpdateSettings(settings.copy(listStyle = style, gridScalePercent = it.toInt()))
                        },
                        valueRange = 50f..150f,
                        steps = 19
                    )
                }

                if (style == RomListStyle.TEXT_ONLY) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text("Text Only Customization:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Text Size Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Text Size", style = MaterialTheme.typography.bodySmall)
                            Text("${textSize.toInt()} sp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = textSize,
                            onValueChange = {
                                textSize = it
                                onUpdateSettings(settings.copy(listStyle = style, textSizeSp = it.toInt()))
                            },
                            valueRange = 12f..28f,
                            steps = 15
                        )
                    }

                    // Margin Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item Margin / Padding", style = MaterialTheme.typography.bodySmall)
                            Text("${margin.toInt()} dp", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = margin,
                            onValueChange = {
                                margin = it
                                onUpdateSettings(settings.copy(listStyle = style, marginDp = it.toInt()))
                            },
                            valueRange = 2f..24f,
                            steps = 21
                        )
                    }

                    // Alignment
                    Text("Text Alignment:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = alignment == TextAlignmentOption.START,
                            onClick = {
                                alignment = TextAlignmentOption.START
                                onUpdateSettings(settings.copy(listStyle = style, textAlignment = TextAlignmentOption.START))
                            },
                            label = { Text("Left") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = alignment == TextAlignmentOption.CENTER,
                            onClick = {
                                alignment = TextAlignmentOption.CENTER
                                onUpdateSettings(settings.copy(listStyle = style, textAlignment = TextAlignmentOption.CENTER))
                            },
                            label = { Text("Center") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = alignment == TextAlignmentOption.END,
                            onClick = {
                                alignment = TextAlignmentOption.END
                                onUpdateSettings(settings.copy(listStyle = style, textAlignment = TextAlignmentOption.END))
                            },
                            label = { Text("Right") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Show Artwork in Text Only toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Artwork / App Icon", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = showArtwork,
                            onCheckedChange = {
                                showArtwork = it
                                onUpdateSettings(settings.copy(listStyle = style, showArtworkInTextOnly = it))
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
