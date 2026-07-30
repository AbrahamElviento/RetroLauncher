package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

val PRESET_COLOR_PALETTE = listOf(
    "#3D5AFE" to "Electric Blue",
    "#E53935" to "NES Red",
    "#7E57C2" to "SNES Purple",
    "#00897B" to "Genesis Teal",
    "#FB8C00" to "N64 Gold / Orange",
    "#43A047" to "Game Boy Green",
    "#D81B60" to "Hot Pink",
    "#00ACC1" to "Neon Cyan",
    "#8E24AA" to "Deep Violet",
    "#FDD835" to "Arcade Yellow",
    "#3949AB" to "PlayStation Blue",
    "#F4511E" to "Crimson Orange",
    "#00E676" to "Lime Green",
    "#181818" to "Charcoal Black",
    "#757575" to "Slate Gray",
    "#FF4081" to "Magenta"
)

fun parseHexColor(hexStr: String, defaultColor: Color = Color(0xFF3D5AFE)): Color {
    val cleanHex = hexStr.trim().removePrefix("#")
    return try {
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            3 -> {
                val expanded = "${cleanHex[0]}${cleanHex[0]}${cleanHex[1]}${cleanHex[1]}${cleanHex[2]}${cleanHex[2]}"
                Color(android.graphics.Color.parseColor("#$expanded"))
            }
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
fun ColorPickerInput(
    colorHex: String,
    onColorHexChange: (String) -> Unit,
    label: String = "Color Hex",
    modifier: Modifier = Modifier
) {
    var showColorDialog by remember { mutableStateOf(false) }
    val currentColor = remember(colorHex) { parseHexColor(colorHex) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color Preview Swatch Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(currentColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .clickable { showColorDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Pick Color",
                tint = if (currentColor.toArgb() == Color.White.toArgb()) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        OutlinedTextField(
            value = colorHex,
            onValueChange = onColorHexChange,
            label = { Text(label) },
            placeholder = { Text("#3D5AFE") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showColorDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Open Color Picker",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            initialColorHex = colorHex,
            onDismiss = { showColorDialog = false },
            onColorSelected = { selectedHex ->
                onColorHexChange(selectedHex)
                showColorDialog = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var hexInput by remember { mutableStateOf(initialColorHex.ifBlank { "#3D5AFE" }) }
    var selectedColor by remember(hexInput) { mutableStateOf(parseHexColor(hexInput)) }

    var red by remember(selectedColor) { mutableFloatStateOf((selectedColor.red * 255f)) }
    var green by remember(selectedColor) { mutableFloatStateOf((selectedColor.green * 255f)) }
    var blue by remember(selectedColor) { mutableFloatStateOf((selectedColor.blue * 255f)) }

    fun updateRgb(r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        val newColor = Color(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
        selectedColor = newColor
        hexInput = colorToHex(newColor)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "CHOOSE COLOR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview & Hex Input Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                                val c = parseHexColor(input)
                                selectedColor = c
                                red = c.red * 255f
                                green = c.green * 255f
                                blue = c.blue * 255f
                            }
                        },
                        label = { Text("Color Hex Code") },
                        placeholder = { Text("#RRGGBB") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preset Palette Grid
                Text(
                    text = "PRESET CONSOLE PALETTES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    items(PRESET_COLOR_PALETTE) { (hex, name) ->
                        val swatchColor = parseHexColor(hex)
                        val isSelected = colorToHex(selectedColor).equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    hexInput = hex
                                    selectedColor = swatchColor
                                    red = swatchColor.red * 255f
                                    green = swatchColor.green * 255f
                                    blue = swatchColor.blue * 255f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RGB Sliders
                Text(
                    text = "CUSTOM RGB SLIDERS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Red Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R: ${red.toInt()}", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Red)
                    Slider(
                        value = red,
                        onValueChange = { updateRgb(it, green, blue) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.7f))
                    )
                }

                // Green Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G: ${green.toInt()}", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Slider(
                        value = green,
                        onValueChange = { updateRgb(red, it, blue) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2E7D32), activeTrackColor = Color(0xFF2E7D32).copy(alpha = 0.7f))
                    )
                }

                // Blue Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B: ${blue.toInt()}", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Slider(
                        value = blue,
                        onValueChange = { updateRgb(red, green, it) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1565C0), activeTrackColor = Color(0xFF1565C0).copy(alpha = 0.7f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(hexInput) }) {
                        Text("Apply Color")
                    }
                }
            }
        }
    }
}
