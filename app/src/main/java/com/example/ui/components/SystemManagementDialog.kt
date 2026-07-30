package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.config.PresetData
import com.example.data.db.SystemEntity
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

                // Action Bar: Add System Button
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add System")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New System")
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

@Composable
fun SystemManagementItemRow(
    system: SystemEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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

                Column {
                    Text(
                        text = system.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (system.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${system.shortName} • ${system.defaultLaunchMode}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reorder Buttons (Move Up / Move Down)
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(32.dp)
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
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
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

    var showIconPicker by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var showMediaDirectoryPicker by remember { mutableStateOf(false) }
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
            "mgba_libretro_android.so" to "Game Boy Advance (mGBA)",
            "gambatte_libretro_android.so" to "Game Boy / GBC (Gambatte)",
            "nestopia_libretro_android.so" to "NES (Nestopia)",
            "fbalpha2012_libretro_android.so" to "Arcade (FB Alpha 2012)",
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

                    ExposedDropdownMenuBox(
                        expanded = expandedCoreDropdown,
                        onExpandedChange = { expandedCoreDropdown = !expandedCoreDropdown }
                    ) {
                        val matchedLabel = popularCores.firstOrNull { it.first == retroArchCore }?.second ?: "Custom Core File Name"

                        OutlinedTextField(
                            value = matchedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Core Preset") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCoreDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCoreDropdown,
                            onDismissRequest = { expandedCoreDropdown = false }
                        ) {
                            popularCores.forEach { (soName, coreTitle) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(coreTitle, fontWeight = FontWeight.Bold)
                                            Text(soName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        retroArchCore = soName
                                        expandedCoreDropdown = false
                                    }
                                )
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
                                    defaultRomIcon = defaultRomIcon
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
}
