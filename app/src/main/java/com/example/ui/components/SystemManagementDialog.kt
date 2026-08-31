package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.config.PresetData
import com.example.data.db.SystemEntity
import com.example.data.model.TotalGamesWidgetConfig
import com.example.data.model.WidgetSubItem
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemManagementDialog(
    systems: List<SystemEntity>,
    onDismiss: () -> Unit,
    onSaveSystem: (SystemEntity) -> Unit,
    onDeleteSystem: (SystemEntity) -> Unit,
    onReorderSystems: (List<SystemEntity>) -> Unit,
    onOpenAppVisibility: (() -> Unit)? = null
) {
    var systemList by remember(systems) { mutableStateOf(systems.sortedBy { it.displayOrder }) }
    var editingSystem by remember { mutableStateOf<SystemEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    ScaledDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Manage Systems",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Emulator System Manager",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enable, reorder, edit, or add systems",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Action Bar: Add System & Add Widget Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isAddingNew = true
                            editingSystem = SystemEntity(
                                id = "sys_" + UUID.randomUUID().toString().take(8),
                                name = "New Emulator System",
                                shortName = "NEW",
                                folderPath = "/storage/emulated/0/RetroRoms/new_system",
                                allowedExtensions = ".zip,.iso",
                                iconName = "gamepad",
                                displayOrder = systemList.size,
                                isEnabled = true
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add System")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add System")
                    }

                    var showAddWidgetMenu by remember { mutableStateOf(false) }

                    val hasClockWidget = remember(systemList) {
                        systemList.any { it.defaultLaunchMode == "WIDGET_CLOCK" || it.id.startsWith("widget_clock") }
                    }
                    val hasTotalGamesWidget = remember(systemList) {
                        systemList.any { it.defaultLaunchMode == "WIDGET_TOTAL_GAMES" || it.id.startsWith("widget_total_games") }
                    }
                    val hasSlideshowWidget = remember(systemList) {
                        systemList.any { it.defaultLaunchMode == "WIDGET_SLIDESHOW" || it.id.startsWith("widget_slideshow") }
                    }
                    val hasBatteryWidget = remember(systemList) {
                        systemList.any { it.defaultLaunchMode == "WIDGET_BATTERY" || it.id.startsWith("widget_battery") }
                    }
                    val hasBgmWidget = remember(systemList) {
                        systemList.any { it.defaultLaunchMode == "WIDGET_BGM" || it.id.startsWith("widget_bgm") }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showAddWidgetMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Widgets, contentDescription = "Add Widget")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Widget")
                        }

                        DropdownMenu(
                            expanded = showAddWidgetMenu,
                            onDismissRequest = { showAddWidgetMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (hasClockWidget) "Clock Widget (Already Added)" else "Clock Widget") },
                                enabled = !hasClockWidget,
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_clock_" + UUID.randomUUID().toString().take(8),
                                        name = "Clock Widget",
                                        shortName = "CLOCK",
                                        folderPath = "",
                                        allowedExtensions = "12h",
                                         iconName = "date_range",
                                        defaultLaunchMode = "WIDGET_CLOCK",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (hasTotalGamesWidget) "Total Games Count (Already Added)" else "Total Games Count") },
                                enabled = !hasTotalGamesWidget,
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_total_games_" + UUID.randomUUID().toString().take(8),
                                        name = "Games Count",
                                        shortName = "COUNT",
                                        folderPath = "",
                                        allowedExtensions = "",
                                        iconName = "info",
                                        defaultLaunchMode = "WIDGET_TOTAL_GAMES",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Custom Text") },
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_custom_text_" + UUID.randomUUID().toString().take(8),
                                        name = "Custom Text",
                                        shortName = "TEXT",
                                        folderPath = "",
                                        allowedExtensions = "",
                                        iconName = "edit",
                                        defaultLaunchMode = "WIDGET_CUSTOM_TEXT",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroArchCore = "Your custom text goes here!",
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (hasSlideshowWidget) "Image Slideshow (Already Added)" else "Image Slideshow") },
                                enabled = !hasSlideshowWidget,
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_slideshow_" + UUID.randomUUID().toString().take(8),
                                        name = "Slideshow",
                                        shortName = "SLIDE",
                                        folderPath = "/storage/emulated/0/Pictures",
                                        allowedExtensions = "5",
                                        retroArchCore = "SHOW_STATUS",
                                        iconName = "play_arrow",
                                        defaultLaunchMode = "WIDGET_SLIDESHOW",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                             DropdownMenuItem(
                                text = { Text(if (hasBgmWidget) "BGM Widget (Already Added)" else "BGM Widget") },
                                enabled = !hasBgmWidget,
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_bgm_" + UUID.randomUUID().toString().take(8),
                                        name = "BGM Widget",
                                        shortName = "BGM",
                                        folderPath = "",
                                        allowedExtensions = "Tap to view",
                                        iconName = "music_note",
                                        defaultLaunchMode = "WIDGET_BGM",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (hasBatteryWidget) "Battery Widget (Already Added)" else "Battery Widget") },
                                enabled = !hasBatteryWidget,
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_battery_" + UUID.randomUUID().toString().take(8),
                                        name = "Battery Widget",
                                        shortName = "BATTERY",
                                        folderPath = "",
                                        allowedExtensions = "",
                                        iconName = "battery_full",
                                        defaultLaunchMode = "WIDGET_BATTERY",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("App Launch Widget") },
                                onClick = {
                                    showAddWidgetMenu = false
                                    isAddingNew = true
                                    editingSystem = SystemEntity(
                                        id = "widget_app_launch_" + UUID.randomUUID().toString().take(8),
                                        name = "App Launch Widget",
                                        shortName = "APP",
                                        folderPath = "",
                                        allowedExtensions = "true",
                                        iconName = "android",
                                        defaultLaunchMode = "WIDGET_APP_LAUNCH",
                                        displayOrder = systemList.size,
                                        isEnabled = true,
                                        retroarchSaveDir = "NONE"
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Systems
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(systemList, key = { _, item -> item.id }) { index, sys ->
                        SystemManagementItemRow(
                            system = sys,
                            isFirst = index == 0,
                            isLast = index == systemList.size - 1,
                            onToggleEnabled = { isEnabled ->
                                val updated = sys.copy(isEnabled = isEnabled)
                                onSaveSystem(updated)
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val newList = systemList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    // Update display orders
                                    val reordered = newList.mapIndexed { i, s -> s.copy(displayOrder = i) }
                                    systemList = reordered
                                    onReorderSystems(reordered)
                                }
                            },
                            onMoveDown = {
                                if (index < systemList.size - 1) {
                                    val newList = systemList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    val reordered = newList.mapIndexed { i, s -> s.copy(displayOrder = i) }
                                    systemList = reordered
                                    onReorderSystems(reordered)
                                }
                            },
                            onMoveToFirst = {
                                if (index > 0) {
                                    val newList = systemList.toMutableList()
                                    val item = newList.removeAt(index)
                                    newList.add(0, item)
                                    val reordered = newList.mapIndexed { i, s -> s.copy(displayOrder = i) }
                                    systemList = reordered
                                    onReorderSystems(reordered)
                                }
                            },
                            onMoveToLast = {
                                if (index < systemList.size - 1) {
                                    val newList = systemList.toMutableList()
                                    val item = newList.removeAt(index)
                                    newList.add(item)
                                    val reordered = newList.mapIndexed { i, s -> s.copy(displayOrder = i) }
                                    systemList = reordered
                                    onReorderSystems(reordered)
                                }
                            },
                            onEdit = {
                                isAddingNew = false
                                editingSystem = sys
                            },
                            onDelete = {
                                onDeleteSystem(sys)
                            },
                            onOpenAppVisibility = onOpenAppVisibility
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }

    // Edit or Add System Dialog
    editingSystem?.let { sys ->
        val isWidget = sys.id.startsWith("widget_") || sys.defaultLaunchMode.startsWith("WIDGET_")
        if (isWidget) {
            WidgetEditDetailDialog(
                system = sys,
                isNew = isAddingNew,
                onDismiss = { editingSystem = null },
                onSave = { updatedSys ->
                    onSaveSystem(updatedSys)
                    editingSystem = null
                }
            )
        } else {
            SystemEditDetailDialog(
                system = sys,
                isNew = isAddingNew,
                onDismiss = { editingSystem = null },
                onSave = { updatedSys ->
                    onSaveSystem(updatedSys)
                    editingSystem = null
                },
                onOpenAppVisibility = onOpenAppVisibility
            )
        }
    }
}

@Composable
fun SystemManagementItemRow(
    system: SystemEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToFirst: () -> Unit,
    onMoveToLast: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenAppVisibility: (() -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Enable/Disable Switch
            Switch(
                checked = system.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // System Icon & Name
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName,
                        contentDescription = system.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = system.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (system.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = "${system.shortName} • ${system.defaultLaunchMode}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            val showFirstLast = LocalDisplaySettings.current.showFirstLastReorderButtons

            // Reorder Buttons (Move to Top / Move Up / Move Down / Move to Bottom)
            if (showFirstLast) {
                IconButton(
                    onClick = onMoveToFirst,
                    enabled = !isFirst,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignTop,
                        contentDescription = "Move to Top",
                        tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }

            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }

            if (showFirstLast) {
                IconButton(
                    onClick = onMoveToLast,
                    enabled = !isLast,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignBottom,
                        contentDescription = "Move to Bottom",
                        tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }

            // App Visibility Filter Button for Android Apps / Games / Emulators
            if (system.id in listOf("android_apps", "android_games", "android_emulators") && onOpenAppVisibility != null) {
                IconButton(
                    onClick = onOpenAppVisibility,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Manage Android App Visibility",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Edit Button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit System",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete Button (Hidden for built-in/special systems and android apps/games/emulators)
            if (system.id !in listOf("android_apps", "android_games", "android_emulators", "favorites", "recently_played")) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete System",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete System") },
            text = { Text("Are you sure you want to remove '${system.name}'? This will not delete ROM files on disk.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemEditDetailDialog(
    system: SystemEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (SystemEntity) -> Unit,
    onOpenAppVisibility: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(system.name) }
    var shortName by remember { mutableStateOf(system.shortName) }
    var colorHex by remember { mutableStateOf(system.colorHex) }
    var folderPath by remember { mutableStateOf(system.folderPath) }
    var allowedExtensions by remember { mutableStateOf(system.allowedExtensions) }
    var boxartFolderPath by remember { mutableStateOf(system.boxartFolderPath) }
    var defaultLaunchMode by remember { mutableStateOf(system.defaultLaunchMode) }
    var retroArchCore by remember { mutableStateOf(system.retroArchCore) }
    var retroArchPackage by remember { mutableStateOf(system.retroArchPackage) }
    var customXmlProfileId by remember { mutableStateOf(system.customXmlProfileId) }
    var iconName by remember { mutableStateOf(system.iconName) }
    var isArcade by remember { mutableStateOf(system.isArcade) }
    var defaultRomIcon by remember { mutableStateOf(system.defaultRomIcon) }
    var manufacturer by remember { mutableStateOf(system.manufacturer) }
    var releaseYear by remember { mutableStateOf(system.releaseYear) }
    var retroarchSaveDir by remember { mutableStateOf(system.retroarchSaveDir) }
    var saveAspectRatio by remember { mutableStateOf(system.saveAspectRatio) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var showMediaDirectoryPicker by remember { mutableStateOf(false) }
    var showRetroarchSaveDirPicker by remember { mutableStateOf(false) }
    var expandedCoreDropdown by remember { mutableStateOf(false) }
    var expandedXmlDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val customIconsFolder = remember(context) {
        com.example.data.config.ConfigStorageManager(context).getCustomIconsDir()
    }
    val customIconFiles = remember(showIconPicker) {
        if (customIconsFolder.exists() && customIconsFolder.isDirectory) {
            customIconsFolder.listFiles { _, fileName ->
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".jpg", ignoreCase = true) ||
                fileName.endsWith(".jpeg", ignoreCase = true) ||
                fileName.endsWith(".webp", ignoreCase = true)
            }?.sortedBy { it.name } ?: emptyList()
        } else emptyList()
    }

    val popularCores = remember {
        listOf(
            "snes9x_libretro_android.so" to "Super Nintendo (Snes9x)",
            "genesis_plus_gx_libretro_android.so" to "Sega Genesis / MD (Genesis Plus GX)",
            "pcsx_rearmed_libretro_android.so" to "PlayStation (PCSX ReARMed)",
            "mupen64plus_next_libretro_android.so" to "Nintendo 64 (Mupen64Plus)",
            "mupen64plus_next_gles3_libretro_android.so" to "Nintendo 64 GLES3 (Mupen64Plus GLES3)",
            "mgba_libretro_android.so" to "Game Boy Advance (mGBA)",
            "gambatte_libretro_android.so" to "Game Boy / GBC (Gambatte)",
            "nestopia_libretro_android.so" to "NES (Nestopia)",
            "fceumm_libretro_android.so" to "NES / FDS (FCEUmm)",
            "fbalpha2012_libretro_android.so" to "Arcade (FB Alpha 2012)",
            "fbneo_libretro_android.so" to "Neo Geo / Arcade (FBNeo)",
            "neocd_libretro_android.so" to "Neo Geo CD (NeoCD)",
            "mednafen_ngp_libretro_android.so" to "Neo Geo Pocket (Mednafen NGP)",
            "mednafen_pce_fast_libretro_android.so" to "NEC PCE / PCE CD (Mednafen PCE Fast)",
            "mednafen_wswan_libretro_android.so" to "Bandai WonderSwan (Mednafen WSwan)",
            "picodrive_libretro_android.so" to "Sega 32X / Genesis (PicoDrive)",
            "desmume_libretro_android.so" to "Nintendo DS (DeSmuME)",
            "melonds_libretro_android.so" to "Nintendo DS (melonDS)",
            "ppsspp_libretro_android.so" to "PSP (PPSSPP)",
            "flycast_libretro_android.so" to "Dreamcast (Flycast)",
            "citra_libretro_android.so" to "3DS (Citra)",
            "dolphin_libretro_android.so" to "GameCube / Wii (Dolphin)"
        )
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isNew) "Add New System" else "Edit System Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if ((system.id in listOf("android_apps", "android_games", "android_emulators") || defaultLaunchMode == "ANDROID_APP") && onOpenAppVisibility != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenAppVisibility()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filter / Hide Installed Android Apps")
                    }
                }

                val parsedColor = parseHexColor(colorHex)

                ColorPickerInput(
                    colorHex = colorHex,
                    onColorHexChange = { colorHex = it },
                    label = "System Color Hex"
                )

                IconPickerInput(
                    iconNameOrPath = iconName,
                    onIconSelected = { iconName = it },
                    label = "System Icon",
                    tint = parsedColor
                )

                IconPickerInput(
                    iconNameOrPath = defaultRomIcon,
                    onIconSelected = { defaultRomIcon = it },
                    label = "Default ROM Icon",
                    tint = parsedColor
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("System Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Short Name (e.g. SNES)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manufacturer,
                        onValueChange = { manufacturer = it },
                        label = { Text("Manufacturer") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = releaseYear,
                        onValueChange = { releaseYear = it },
                        label = { Text("Release Year") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = folderPath,
                    onValueChange = { folderPath = it },
                    label = { Text("ROM Folder Path") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showDirectoryPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Browse Directory",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = retroarchSaveDir,
                    onValueChange = { retroarchSaveDir = it },
                    label = { Text("RetroArch Save Directory") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showRetroarchSaveDirPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Browse RetroArch Save Directory",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true
                )

                var expandedAspectRatioDropdown by remember { mutableStateOf(false) }
                val aspectRatios = listOf("Auto", "4:3", "3:2", "16:9", "10:9", "1:1")

                ExposedDropdownMenuBox(
                    expanded = expandedAspectRatioDropdown,
                    onExpandedChange = { expandedAspectRatioDropdown = it }
                ) {
                    OutlinedTextField(
                        value = when (saveAspectRatio) {
                            "Auto" -> "Auto (default)"
                            else -> saveAspectRatio
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aspect Ratio Thumbnail") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAspectRatioDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedAspectRatioDropdown,
                        onDismissRequest = { expandedAspectRatioDropdown = false }
                    ) {
                        aspectRatios.forEach { ratio ->
                            DropdownMenuItem(
                                text = { Text(if (ratio == "Auto") "Auto (default)" else ratio) },
                                onClick = {
                                    saveAspectRatio = ratio
                                    expandedAspectRatioDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = allowedExtensions,
                    onValueChange = { allowedExtensions = it },
                    label = { Text("Allowed Extensions (e.g. .sfc,.zip)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Arcade System Mark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Arcade System:", style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = isArcade,
                        onClick = { isArcade = !isArcade },
                        label = { Text(if (isArcade) "Yes (Arcade DB Enabled)" else "No") },
                        leadingIcon = if (isArcade) {
                            { Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // Launch Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Launch Mode:", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        FilterChip(
                            selected = defaultLaunchMode == "RETROARCH",
                            onClick = { defaultLaunchMode = "RETROARCH" },
                            label = { Text("RetroArch") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = defaultLaunchMode == "STANDALONE_XML",
                            onClick = { defaultLaunchMode = "STANDALONE_XML" },
                            label = { Text("Standalone") }
                        )
                    }
                }

                if (defaultLaunchMode == "RETROARCH") {
                    Text(
                        text = "RetroArch Package Version:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = retroArchPackage == "com.retroarch.aarch64",
                            onClick = { retroArchPackage = "com.retroarch.aarch64" },
                            label = { Text("aarch64", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = retroArchPackage == "com.retroarch.ra32",
                            onClick = { retroArchPackage = "com.retroarch.ra32" },
                            label = { Text("ra32", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = retroArchPackage == "AUTO" || retroArchPackage == "com.retroarch",
                            onClick = { retroArchPackage = "AUTO" },
                            label = { Text("Auto", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    var coreFilterText by remember { mutableStateOf("") }
                    val filteredCores = remember(coreFilterText) {
                        if (coreFilterText.isBlank()) {
                            popularCores
                        } else {
                            popularCores.filter { (soName, coreTitle) ->
                                coreTitle.contains(coreFilterText, ignoreCase = true) ||
                                soName.contains(coreFilterText, ignoreCase = true) ||
                                (coreFilterText.contains("muppen", ignoreCase = true) && soName.contains("mupen", ignoreCase = true))
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedCoreDropdown,
                        onExpandedChange = {
                            expandedCoreDropdown = it
                            if (it) {
                                coreFilterText = ""
                            }
                        }
                    ) {
                        val matchedLabel = popularCores.firstOrNull { it.first == retroArchCore }?.second ?: "Custom Core File Name"

                        OutlinedTextField(
                            value = if (expandedCoreDropdown) coreFilterText else matchedLabel,
                            onValueChange = {
                                coreFilterText = it
                                expandedCoreDropdown = true
                            },
                            readOnly = false,
                            label = { Text("Select Core Preset (Type to Filter)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCoreDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCoreDropdown,
                            onDismissRequest = { expandedCoreDropdown = false }
                        ) {
                            if (filteredCores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No matching cores found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = {},
                                    enabled = false
                                )
                            } else {
                                filteredCores.forEach { (soName, coreTitle) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(coreTitle, fontWeight = FontWeight.Bold)
                                                Text(soName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            retroArchCore = soName
                                            coreFilterText = ""
                                            expandedCoreDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = retroArchCore,
                        onValueChange = { retroArchCore = it },
                        label = { Text("RetroArch Core (.so file name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedXmlDropdown,
                        onExpandedChange = { expandedXmlDropdown = !expandedXmlDropdown }
                    ) {
                        val currentProfile = PresetData.PRESET_STANDALONE_PROFILES.firstOrNull { it.id == customXmlProfileId }
                        val currentProfileName = currentProfile?.name ?: customXmlProfileId

                        OutlinedTextField(
                            value = currentProfileName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Standalone App Profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedXmlDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedXmlDropdown,
                            onDismissRequest = { expandedXmlDropdown = false }
                        ) {
                            PresetData.PRESET_STANDALONE_PROFILES.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${profile.packageName} (${profile.activityName.substringAfterLast('.')})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        customXmlProfileId = profile.id
                                        expandedXmlDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                system.copy(
                                    name = name,
                                    shortName = shortName,
                                    colorHex = colorHex,
                                    folderPath = folderPath,
                                    allowedExtensions = allowedExtensions,
                                    boxartFolderPath = boxartFolderPath,
                                    defaultLaunchMode = defaultLaunchMode,
                                    retroArchCore = retroArchCore,
                                    retroArchPackage = retroArchPackage,
                                    customXmlProfileId = customXmlProfileId,
                                    iconName = iconName,
                                    isArcade = isArcade,
                                    defaultRomIcon = defaultRomIcon,
                                    manufacturer = manufacturer,
                                    releaseYear = releaseYear,
                                    retroarchSaveDir = retroarchSaveDir,
                                    saveAspectRatio = saveAspectRatio
                                )
                            )
                            onDismiss()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showDirectoryPicker) {
        DirectoryPickerDialog(
            initialPath = folderPath,
            onDismiss = { showDirectoryPicker = false },
            onDirectorySelected = { selectedPath ->
                folderPath = selectedPath
            }
        )
    }

    if (showMediaDirectoryPicker) {
        DirectoryPickerDialog(
            initialPath = if (boxartFolderPath.isNotBlank()) boxartFolderPath else folderPath,
            onDismiss = { showMediaDirectoryPicker = false },
            onDirectorySelected = { selectedPath ->
                boxartFolderPath = selectedPath
            }
        )
    }

    if (showRetroarchSaveDirPicker) {
        DirectoryPickerDialog(
            initialPath = if (retroarchSaveDir.isNotBlank()) retroarchSaveDir else folderPath,
            onDismiss = { showRetroarchSaveDirPicker = false },
            onDirectorySelected = { selectedPath ->
                retroarchSaveDir = selectedPath
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetEditDetailDialog(
    system: SystemEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (SystemEntity) -> Unit
) {
    var name by remember { mutableStateOf(system.name) }
    var colorHex by remember { mutableStateOf(system.colorHex) }
    var folderPath by remember { mutableStateOf(system.folderPath) }
    var allowedExtensions by remember { mutableStateOf(system.allowedExtensions) }
    var retroArchCore by remember { mutableStateOf(system.retroArchCore) }
    var iconName by remember { mutableStateOf(system.iconName) }

    var useAppIcon by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH") {
                system.allowedExtensions != "false"
            } else {
                true
            }
        )
    }

    var showLaunchText by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH") {
                system.retroArchCore != "false"
            } else {
                true
            }
        )
    }

    var showStatusInFullScreen by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_SLIDESHOW") {
                system.retroArchCore.isBlank() || system.retroArchCore.startsWith("SHOW_STATUS") || system.retroArchCore == "true"
            } else if (system.defaultLaunchMode == "WIDGET_CLOCK" || system.id.startsWith("widget_clock")) {
                system.shortName.isBlank() || system.shortName == "CLOCK" || system.shortName.startsWith("SHOW_STATUS") || system.shortName == "true"
            } else {
                true
            }
        )
    }

    var clockStatusBarAlignment by remember {
        mutableStateOf(
            when {
                system.shortName.contains("CENTERED") -> "CENTERED"
                system.shortName.contains("BOTTOM") -> "BOTTOM"
                else -> "TOP"
            }
        )
    }

    var isClassicClock by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_CLOCK") {
                system.folderPath.startsWith("CLASSIC")
            } else {
                false
            }
        )
    }

    var classicClock24Hour by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_CLOCK") {
                system.folderPath.contains("24H") || system.allowedExtensions == "24h" || system.allowedExtensions == "HH:mm:ss" || system.allowedExtensions == "HH:mm"
            } else {
                false
            }
        )
    }

    var classicClockShowSeconds by remember {
        mutableStateOf(
            if (system.defaultLaunchMode == "WIDGET_CLOCK") {
                !system.folderPath.contains("NOSEC") && !system.folderPath.contains("NO_SEC")
            } else {
                true
            }
        )
    }

    var subItemsList by remember {
        mutableStateOf(
            TotalGamesWidgetConfig.parse(system.folderPath, system.colorHex, system.iconName)
        )
    }

    var doubleTapAction by remember {
        mutableStateOf(
            system.retroarchSaveDir.ifBlank { "NONE" }
        )
    }

    var singleTapTrigger by remember {
        mutableStateOf(
            system.saveAspectRatio == "SINGLE_TAP" || system.saveAspectRatio == "true"
        )
    }

    var showDirectoryPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val appList = remember { AndroidAppIconHelper.getAllInstalledAppsInfo(context) }

    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isNew) "Add Widget" else "Edit Widget Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                val typeName = when (system.defaultLaunchMode) {
                    "WIDGET_CLOCK" -> "Clock (Time & Date)"
                    "WIDGET_TOTAL_GAMES" -> "Total Games Count"
                    "WIDGET_CUSTOM_TEXT" -> "Custom Text Display"
                    "WIDGET_SLIDESHOW" -> "Custom Image Slideshow"
                    "WIDGET_BGM" -> "BGM (Background Music)"
                    "WIDGET_BATTERY" -> "Battery Status"
                    "WIDGET_APP_LAUNCH" -> "App Launcher"
                    else -> "Widget"
                }
                Text(
                    text = "Type: $typeName",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Widget Display Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Widget Theme & Style",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useAppIcon,
                            onCheckedChange = { useAppIcon = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use App Icon (ignores manual icon choice)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showLaunchText,
                            onCheckedChange = { showLaunchText = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Show Launch/Launch App Text",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val parsedColor = parseHexColor(colorHex)

                ColorPickerInput(
                    colorHex = colorHex,
                    onColorHexChange = { colorHex = it },
                    label = "Widget Theme Color"
                )

                if (system.defaultLaunchMode != "WIDGET_BATTERY" &&
                    !(system.defaultLaunchMode == "WIDGET_APP_LAUNCH" && useAppIcon)
                ) {
                    IconPickerInput(
                        iconNameOrPath = iconName,
                        onIconSelected = { iconName = it },
                        label = "Widget Icon",
                        tint = parsedColor
                    )
                }

                if (system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Double Tap Action",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    var expandedDoubleTapDropdown by remember { mutableStateOf(false) }
                    var doubleTapAppSearchQuery by remember { mutableStateOf("") }
                    var doubleTapAppExpanded by remember { mutableStateOf(false) }

                    val isClockWidget = system.defaultLaunchMode == "WIDGET_CLOCK" || system.id.startsWith("widget_clock")
                    val doubleTapActionOptions = buildList {
                        add("NONE" to "None")
                        if (isClockWidget) {
                            add("FULLSCREEN_CLOCK" to "Full Screen Clock")
                        }
                        add("HIDE_SHOW_TOP_BAR" to "Hide / Show Top Bar")
                        add("HIDE_SHOW_HEADER" to "Hide / Show Main Menu Header Bar")
                        add("OPEN_MAIN_SETTING" to "Open Main Setting Dialog")
                        add("OPEN_SYSTEM_MANAGER" to "Open Emulator System Manager")
                        add("CYCLE_DISPLAY_STYLE" to "Cycle Main Menu Display Style")
                        add("EXPAND_NOTIFICATIONS" to "Expand Notifications")
                        add("TOGGLE_WIFI" to "Toggle Wi-Fi")
                        add("TOGGLE_BLUETOOTH" to "Toggle Bluetooth")
                        add("SHOW_VOLUME_PANEL" to "Show Volume Panel")
                        add("SHOW_RECENT_APPS" to "Show Recent Apps")
                        add("SCREEN_LOCK" to "Screen Lock")
                        add("LAUNCH_APP" to "Launch App")
                    }

                    val currentActionKey = when {
                        doubleTapAction == "LAUNCH_APP" || doubleTapAction.startsWith("LAUNCH_APP:") -> "LAUNCH_APP"
                        else -> doubleTapAction
                    }
                    val currentDoubleTapLabel = doubleTapActionOptions.firstOrNull { it.first == currentActionKey }?.second ?: "None"

                    ExposedDropdownMenuBox(
                        expanded = expandedDoubleTapDropdown,
                        onExpandedChange = { expandedDoubleTapDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = currentDoubleTapLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Double Tap Action") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDoubleTapDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDoubleTapDropdown,
                            onDismissRequest = { expandedDoubleTapDropdown = false }
                        ) {
                            doubleTapActionOptions.forEach { (actionKey, actionLabel) ->
                                DropdownMenuItem(
                                    text = { Text(actionLabel, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        if (actionKey == "LAUNCH_APP") {
                                            if (!doubleTapAction.startsWith("LAUNCH_APP:")) {
                                                doubleTapAction = "LAUNCH_APP"
                                            }
                                        } else {
                                            doubleTapAction = actionKey
                                        }
                                        expandedDoubleTapDropdown = false
                                    },
                                    leadingIcon = {
                                        if (currentActionKey == actionKey) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (isClockWidget && currentActionKey == "FULLSCREEN_CLOCK") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showStatusInFullScreen = !showStatusInFullScreen },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Status in Full Screen",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Show battery and date information when full screen clock controls are hidden",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = showStatusInFullScreen,
                                        onCheckedChange = { showStatusInFullScreen = it }
                                    )
                                }

                                if (showStatusInFullScreen) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Status Bar Alignment",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val alignmentOptions = listOf(
                                            "TOP" to "Top",
                                            "BOTTOM" to "Bottom",
                                            "CENTERED" to "Centered"
                                        )
                                        alignmentOptions.forEach { (key, label) ->
                                            FilterChip(
                                                selected = clockStatusBarAlignment == key,
                                                onClick = { clockStatusBarAlignment = key },
                                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    val alignDesc = when (clockStatusBarAlignment) {
                                        "BOTTOM" -> "Battery on bottom-left, date on bottom-right"
                                        "CENTERED" -> "Battery on top of centered clock, date under clock matching clock width"
                                        else -> "Battery on top-left, date on top-right"
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = alignDesc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (currentActionKey == "LAUNCH_APP") {
                        Spacer(modifier = Modifier.height(8.dp))
                        val selectedPackage = if (doubleTapAction.startsWith("LAUNCH_APP:")) {
                            doubleTapAction.removePrefix("LAUNCH_APP:")
                        } else ""
                        val selectedAppInfo = appList.find { it.packageName == selectedPackage }
                        val appSelectorLabel = selectedAppInfo?.label ?: if (selectedPackage.isNotBlank()) selectedPackage else "Select an app to launch..."

                        val filteredDoubleTapAppList = remember(doubleTapAppSearchQuery, appList) {
                            if (doubleTapAppSearchQuery.isBlank()) {
                                appList
                            } else {
                                appList.filter {
                                    it.label.contains(doubleTapAppSearchQuery, ignoreCase = true) ||
                                    it.packageName.contains(doubleTapAppSearchQuery, ignoreCase = true)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select App to Launch on Double Tap",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { doubleTapAppExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            if (selectedPackage.isNotBlank()) {
                                                val imageBitmap = remember(selectedPackage) {
                                                    AndroidAppIconHelper.getAppIconBitmap(context, selectedPackage)
                                                }
                                                if (imageBitmap != null) {
                                                    Image(
                                                        bitmap = imageBitmap,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                            }
                                            Text(
                                                text = appSelectorLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = doubleTapAppExpanded,
                                    onDismissRequest = {
                                        doubleTapAppExpanded = false
                                        doubleTapAppSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .heightIn(max = 350.dp)
                                ) {
                                    OutlinedTextField(
                                        value = doubleTapAppSearchQuery,
                                        onValueChange = { doubleTapAppSearchQuery = it },
                                        label = { Text("Search App...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                    )
                                    filteredDoubleTapAppList.forEach { app ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val imageBitmap = remember(app.packageName) {
                                                        AndroidAppIconHelper.getAppIconBitmap(context, app.packageName)
                                                    }
                                                    if (imageBitmap != null) {
                                                        Image(
                                                            bitmap = imageBitmap,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Column {
                                                        Text(app.label, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            app.packageName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                doubleTapAction = "LAUNCH_APP:${app.packageName}"
                                                doubleTapAppExpanded = false
                                                doubleTapAppSearchQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trigger on Single Tap",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Execute the action above on single tap as well",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = singleTapTrigger,
                                onCheckedChange = { singleTapTrigger = it }
                            )
                        }
                    }
                }

                when (system.defaultLaunchMode) {
                    "WIDGET_CLOCK" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Classic Style",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Mimic classic digital watch with seven-segment display",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isClassicClock,
                                    onCheckedChange = { isClassicClock = it }
                                )
                            }
                        }

                        if (isClassicClock) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (classicClock24Hour) "24-Hour Format" else "12-Hour Format",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = if (classicClock24Hour) "24-hour military digital watch format" else "12-hour digital watch format with AM/PM",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = classicClock24Hour,
                                            onCheckedChange = { classicClock24Hour = it }
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Show Seconds",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = if (classicClockShowSeconds) "Display digital seconds counter" else "Hide seconds counter on clock",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = classicClockShowSeconds,
                                            onCheckedChange = { classicClockShowSeconds = it }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Classic Digital Display Preview",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val parseColor = runCatching {
                                Color(android.graphics.Color.parseColor(colorHex))
                            }.getOrDefault(MaterialTheme.colorScheme.primary)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF101412))
                                    .border(1.dp, parseColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SevenSegmentClock(
                                    is24Hour = classicClock24Hour,
                                    color = parseColor,
                                    digitHeight = 36.dp,
                                    showSeconds = classicClockShowSeconds,
                                    showContainerBackground = false
                                )
                            }
                        } else {
                            Text(
                                text = "Time Format Pattern",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val timePresets = listOf(
                                "hh:mm:ss a" to "12h with Sec",
                                "hh:mm a" to "12h",
                                "HH:mm:ss" to "24h with Sec",
                                "HH:mm" to "24h"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                timePresets.forEach { (pattern, label) ->
                                    FilterChip(
                                        selected = allowedExtensions == pattern,
                                        onClick = { allowedExtensions = pattern },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = allowedExtensions,
                                onValueChange = { allowedExtensions = it },
                                label = { Text("Time Pattern (e.g. hh:mm:ss a, HH:mm)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Date Format Pattern",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val datePresets = listOf(
                                "yyyy-MM-dd" to "Y-m-d",
                                "EEE dd-MM-yyyy" to "e d-m-Y",
                                "dd/MM/yyyy" to "d/m/Y",
                                "EEE, MMM dd, yyyy" to "Default"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                datePresets.forEach { (pattern, label) ->
                                    FilterChip(
                                        selected = retroArchCore == pattern,
                                        onClick = { retroArchCore = pattern },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = retroArchCore,
                                onValueChange = { retroArchCore = it },
                                label = { Text("Date Pattern (e.g. yyyy-MM-dd, EEE dd-MM-yyyy)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showStatusInFullScreen = !showStatusInFullScreen },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Status in Full Screen",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Show battery and date information when full screen clock controls are hidden",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = showStatusInFullScreen,
                                            onCheckedChange = { showStatusInFullScreen = it }
                                        )
                                    }

                                    if (showStatusInFullScreen) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Status Bar Alignment",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val alignmentOptions = listOf(
                                                "TOP" to "Top",
                                                "BOTTOM" to "Bottom",
                                                "CENTERED" to "Centered"
                                            )
                                            alignmentOptions.forEach { (key, label) ->
                                                FilterChip(
                                                    selected = clockStatusBarAlignment == key,
                                                    onClick = { clockStatusBarAlignment = key },
                                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        val alignDesc = when (clockStatusBarAlignment) {
                                            "BOTTOM" -> "Battery on bottom-left, date on bottom-right"
                                            "CENTERED" -> "Battery on top of centered clock, date under clock matching clock width"
                                            else -> "Battery on top-left, date on top-right"
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = alignDesc,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "WIDGET_CUSTOM_TEXT" -> {
                        OutlinedTextField(
                            value = retroArchCore,
                            onValueChange = { retroArchCore = it },
                            label = { Text("Custom Text Content") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "WIDGET_SLIDESHOW" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = folderPath,
                                onValueChange = { folderPath = it },
                                label = { Text("Slideshow Image Folder") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showDirectoryPicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Select Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = allowedExtensions,
                            onValueChange = { allowedExtensions = it.filter { c -> c.isDigit() } },
                            label = { Text("Transition Interval (Seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStatusInFullScreen = !showStatusInFullScreen }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Status in Full Screen",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Show battery level on top left and clock on top right when full screen controls are hidden",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = showStatusInFullScreen,
                                    onCheckedChange = { showStatusInFullScreen = it }
                                )
                            }
                        }
                    }
                    "WIDGET_BGM" -> {
                        Text(
                            text = "Background Music Widget. Tapping or selecting this widget using gamepad A button in the main menu will bring up a dedicated playback controller dialog.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = allowedExtensions,
                            onValueChange = { allowedExtensions = it },
                            label = { Text("Custom Action Label (replaces 'Tap to view')") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "WIDGET_APP_LAUNCH" -> {
                        Text(
                            text = "Select App to Launch",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        var expandedAppMenu by remember { mutableStateOf(false) }
                        var appSearchQuery by remember { mutableStateOf("") }
                        val currentApp = appList.find { it.packageName == folderPath }
                        val appLabel = currentApp?.label ?: if (folderPath.isNotEmpty()) folderPath.split("|").first() else "Select an app..."

                        val filteredAppList = remember(appSearchQuery, appList) {
                            if (appSearchQuery.isBlank()) {
                                appList
                            } else {
                                appList.filter {
                                    it.label.contains(appSearchQuery, ignoreCase = true) ||
                                    it.packageName.contains(appSearchQuery, ignoreCase = true)
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedAppMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedAppMenu,
                                onDismissRequest = { 
                                    expandedAppMenu = false 
                                    appSearchQuery = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .heightIn(max = 400.dp)
                            ) {
                                OutlinedTextField(
                                    value = appSearchQuery,
                                    onValueChange = { appSearchQuery = it },
                                    label = { Text("Search App...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        if (appSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { appSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    }
                                )
                                
                                if (filteredAppList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {},
                                        enabled = false
                                    )
                                } else {
                                    filteredAppList.forEach { app ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val bitmap = remember(app.packageName) {
                                                        AndroidAppIconHelper.getAppIconBitmap(context, app.packageName)
                                                    }
                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    Text(app.label)
                                                }
                                            },
                                            onClick = {
                                                folderPath = app.packageName
                                                if (name.isBlank() || name == "App Launch Widget") {
                                                    name = app.label
                                                }
                                                appSearchQuery = ""
                                                expandedAppMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "WIDGET_TOTAL_GAMES" -> {
                        Text(
                            text = "Counter Configurations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Customize, order, enable/disable, change color and icon of the counters shown in the widget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        subItemsList.forEach { item ->
                            val parsedItemColor = runCatching { Color(android.graphics.Color.parseColor(item.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = item.enabled,
                                                onCheckedChange = { isChecked ->
                                                    subItemsList = subItemsList.map {
                                                        if (it.key == item.key) it.copy(enabled = isChecked) else it
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val idx = subItemsList.indexOf(item)
                                                    if (idx > 0) {
                                                        val newList = subItemsList.toMutableList()
                                                        val temp = newList[idx]
                                                        newList[idx] = newList[idx - 1]
                                                        newList[idx - 1] = temp
                                                        subItemsList = newList.mapIndexed { index, subItem -> subItem.copy(order = index) }
                                                    }
                                                },
                                                enabled = subItemsList.indexOf(item) > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val idx = subItemsList.indexOf(item)
                                                    if (idx < subItemsList.size - 1) {
                                                        val newList = subItemsList.toMutableList()
                                                        val temp = newList[idx]
                                                        newList[idx] = newList[idx + 1]
                                                        newList[idx + 1] = temp
                                                        subItemsList = newList.mapIndexed { index, subItem -> subItem.copy(order = index) }
                                                    }
                                                },
                                                enabled = subItemsList.indexOf(item) < subItemsList.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (item.enabled) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = item.label,
                                            onValueChange = { newLabel ->
                                                subItemsList = subItemsList.map {
                                                    if (it.key == item.key) it.copy(label = newLabel) else it
                                                }
                                            },
                                            label = { Text("Display Name") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ColorPickerInput(
                                            colorHex = item.color,
                                            onColorHexChange = { newColor ->
                                                subItemsList = subItemsList.map {
                                                    if (it.key == item.key) it.copy(color = newColor) else it
                                                }
                                            },
                                            label = "Color"
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        IconPickerInput(
                                            iconNameOrPath = item.icon,
                                            onIconSelected = { newIcon ->
                                                subItemsList = subItemsList.map {
                                                    if (it.key == item.key) it.copy(icon = newIcon) else it
                                                }
                                            },
                                            label = "Icon",
                                            tint = parsedItemColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val savedExt = if (system.defaultLaunchMode == "WIDGET_SLIDESHOW") {
                                if (allowedExtensions.isBlank()) "5" else allowedExtensions
                            } else if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH") {
                                useAppIcon.toString()
                            } else if (system.defaultLaunchMode == "WIDGET_CLOCK") {
                                if (isClassicClock) {
                                    if (classicClock24Hour) "24h" else "12h"
                                } else {
                                    if (allowedExtensions.isBlank()) "12h" else allowedExtensions
                                }
                            } else {
                                allowedExtensions
                            }
                            val finalRetroArchCore = if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH") {
                                showLaunchText.toString()
                            } else if (system.defaultLaunchMode == "WIDGET_SLIDESHOW") {
                                if (showStatusInFullScreen) "SHOW_STATUS" else "HIDE_STATUS"
                            } else {
                                retroArchCore
                            }
                            val finalFolderPath = if (system.defaultLaunchMode == "WIDGET_TOTAL_GAMES") {
                                TotalGamesWidgetConfig.serialize(subItemsList)
                            } else if (system.defaultLaunchMode == "WIDGET_CLOCK") {
                                if (isClassicClock) {
                                    val hStr = if (classicClock24Hour) "24H" else "12H"
                                    val sStr = if (classicClockShowSeconds) "SEC" else "NOSEC"
                                    "CLASSIC_${hStr}_${sStr}"
                                } else {
                                    ""
                                }
                            } else {
                                folderPath
                            }
                            val finalRetroarchSaveDir = if (system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")) {
                                doubleTapAction
                            } else {
                                system.retroarchSaveDir
                            }
                            val finalSaveAspectRatio = if (system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")) {
                                if (singleTapTrigger) {
                                    "SINGLE_TAP"
                                } else {
                                    "Auto"
                                }
                            } else {
                                system.saveAspectRatio
                            }
                            val finalShortName = if (system.defaultLaunchMode == "WIDGET_CLOCK" || system.id.startsWith("widget_clock")) {
                                val statusPrefix = if (showStatusInFullScreen) "SHOW_STATUS" else "HIDE_STATUS"
                                "${statusPrefix}:${clockStatusBarAlignment}"
                            } else {
                                system.shortName
                            }
                            onSave(
                                system.copy(
                                    name = name,
                                    shortName = finalShortName,
                                    colorHex = colorHex,
                                    folderPath = finalFolderPath,
                                    allowedExtensions = savedExt,
                                    retroArchCore = finalRetroArchCore,
                                    iconName = iconName,
                                    retroarchSaveDir = finalRetroarchSaveDir,
                                    saveAspectRatio = finalSaveAspectRatio
                                )
                            )
                            onDismiss()
                        }
                    ) {
                        Text("Save Widget")
                    }
                }
            }
        }
    }

    if (showDirectoryPicker) {
        DirectoryPickerDialog(
            initialPath = folderPath,
            onDismiss = { showDirectoryPicker = false },
            onDirectorySelected = { selectedPath ->
                folderPath = selectedPath
            }
        )
    }
}
