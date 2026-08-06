package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

val EXPANDED_ICON_LIST: List<Pair<String, ImageVector>> = listOf(
    "gamepad" to Icons.Default.SportsEsports,
    "videogame" to Icons.Default.VideogameAsset,
    "tv" to Icons.Default.Tv,
    "smartphone" to Icons.Default.Smartphone,
    "tablet" to Icons.Default.Tablet,
    "desktop" to Icons.Default.DesktopWindows,
    "memory" to Icons.Default.Memory,
    "album" to Icons.Default.Album,
    "casino" to Icons.Default.Casino,
    "extension" to Icons.Default.Extension,
    "sd_card" to Icons.Default.SdStorage,
    "tune" to Icons.Default.Tune,
    "star" to Icons.Default.Star,
    "history" to Icons.Default.History,
    "android" to Icons.Default.Android,
    "settings" to Icons.Default.Settings,
    "folder" to Icons.Default.Folder,
    "apps" to Icons.Default.Apps,
    "computer" to Icons.Default.Computer,
    "games" to Icons.Default.Games,
    "headset" to Icons.Default.Headset,
    "widgets" to Icons.Default.Widgets,
    "palette" to Icons.Default.Palette,
    "clock" to Icons.Default.Schedule,
    "calendar" to Icons.Default.CalendarToday,
    "pencil" to Icons.Default.Edit,
    "brush" to Icons.Default.Brush,
    "music" to Icons.Default.MusicNote,
    "volume" to Icons.Default.VolumeUp,
    "heart" to Icons.Default.Favorite,
    "home" to Icons.Default.Home,
    "bar_chart" to Icons.Default.BarChart,
    "check_box" to Icons.Default.CheckBox,
    "badge" to Icons.Default.Badge
)

@Composable
fun IconPickerInput(
    iconNameOrPath: String,
    onIconSelected: (String) -> Unit,
    label: String = "System Icon",
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var showIconDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon Preview Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.15f))
                .border(1.5.dp, tint, RoundedCornerShape(10.dp))
                .clickable { showIconDialog = true },
            contentAlignment = Alignment.Center
        ) {
            UniversalIconView(
                iconNameOrPath = iconNameOrPath.ifBlank { "gamepad" },
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }

        OutlinedTextField(
            value = iconNameOrPath,
            onValueChange = onIconSelected,
            label = { Text(label) },
            placeholder = { Text("gamepad, tv, or external PNG path") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showIconDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Pick Icon",
                        tint = tint
                    )
                }
            }
        )
    }

    if (showIconDialog) {
        IconPickerDialog(
            currentIcon = iconNameOrPath,
            onDismiss = { showIconDialog = false },
            onSelectIcon = { selected ->
                onIconSelected(selected)
                showIconDialog = false
            }
        )
    }
}

@Composable
fun IconPickerDialog(
    currentIcon: String,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customPathInput by remember { mutableStateOf(currentIcon) }
    var showFilePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val customIconsFolder = remember(context) {
        com.example.data.config.ConfigStorageManager(context).getCustomIconsDir()
    }
    val customIconFiles = remember {
        if (customIconsFolder.exists() && customIconsFolder.isDirectory) {
            customIconsFolder.listFiles { _, fileName ->
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".jpg", ignoreCase = true) ||
                fileName.endsWith(".jpeg", ignoreCase = true) ||
                fileName.endsWith(".webp", ignoreCase = true)
            }?.sortedBy { it.name } ?: emptyList()
        } else emptyList()
    }

    val filteredIcons = remember(searchQuery) {
        if (searchQuery.isBlank()) EXPANDED_ICON_LIST
        else EXPANDED_ICON_LIST.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }

    ScaledDialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT ICON",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filter Icons") },
                    placeholder = { Text("Search gamepad, tv, android, etc.") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Built-in Icon Grid
                Text(
                    text = "BUILT-IN SYSTEM VECTOR ICONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(filteredIcons) { (iconKey, vector) ->
                        val isSelected = currentIcon.equals(iconKey, ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { onSelectIcon(iconKey) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = iconKey,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (customIconFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CUSTOM IMAGE ICONS (${customIconsFolder.name})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(customIconFiles) { file ->
                            val isSelected = currentIcon == file.absolutePath
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectIcon(file.absolutePath) }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                UniversalIconView(
                                    iconNameOrPath = file.absolutePath,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom External Path Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customPathInput,
                        onValueChange = { customPathInput = it },
                        label = { Text("Or enter custom file path") },
                        placeholder = { Text("/storage/emulated/0/icon.png") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { showFilePicker = true }
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse")
                    }
                    IconButton(onClick = { onSelectIcon(customPathInput) }) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm custom path")
                    }
                }

                if (showFilePicker) {
                    FilePickerDialog(
                        initialPath = customPathInput,
                        onDismiss = { showFilePicker = false },
                        onFileSelected = { selectedPath ->
                            customPathInput = selectedPath
                            showFilePicker = false
                            onSelectIcon(selectedPath)
                        }
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
                }
            }
        }
    }
}
