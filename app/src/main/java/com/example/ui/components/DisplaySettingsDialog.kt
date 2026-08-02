package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BottomBarSettings
import com.example.data.model.DisplayMode
import com.example.data.model.DisplaySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsDialog(
    currentSettings: DisplaySettings,
    bottomBarSettings: BottomBarSettings? = null,
    onDismiss: () -> Unit,
    onSaveSettings: (DisplaySettings) -> Unit,
    onSaveBottomBarSettings: ((BottomBarSettings) -> Unit)? = null,
    onOpenGamepadSettings: (() -> Unit)? = null,
    onOpenConfigFileManager: (() -> Unit)? = null
) {
    // Accordion Expansion States
    var expandedScreen by remember { mutableStateOf(true) }
    var expandedTopBottomBar by remember { mutableStateOf(false) }
    var expandedSystemMainMenu by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedOther by remember { mutableStateOf(false) }

    // Screen Settings States
    val context = androidx.compose.ui.platform.LocalContext.current
    val storageManager = remember(context) { com.example.data.config.ConfigStorageManager(context) }
    val baseDirName = remember(storageManager) {
        try {
            val base = storageManager.resolveBaseDir()
            if (base.name == "0" || base.name == "emulated") "RetroLauncher" else base.name
        } catch (_: Exception) {
            "RetroLauncher"
        }
    }
    val sfxFileList = remember(context) { com.example.util.SoundManager.getSfxFileList(context) }

    var mode by remember { mutableStateOf(currentSettings.mode) }
    var expandedModeDropdown by remember { mutableStateOf(false) }
    var widthPxText by remember { mutableStateOf(currentSettings.customWidthPx.toString()) }
    var heightPxText by remember { mutableStateOf(currentSettings.customHeightPx.toString()) }
    var topMarginText by remember { mutableStateOf(currentSettings.marginTopDp.toString()) }
    var bottomMarginText by remember { mutableStateOf(currentSettings.marginBottomDp.toString()) }
    var leftMarginText by remember { mutableStateOf(currentSettings.marginLeftDp.toString()) }
    var rightMarginText by remember { mutableStateOf(currentSettings.marginRightDp.toString()) }
    var launcherTitleText by remember { mutableStateOf(currentSettings.launcherTitle) }
    var launcherIconPathText by remember { mutableStateOf(currentSettings.launcherIconPath) }

    // Top & Bottom Bar States
    var swapTopAndBottomBar by remember { mutableStateOf(currentSettings.swapTopAndBottomBar) }
    var showBottomBar by remember { mutableStateOf(bottomBarSettings?.showBottomBar ?: true) }
    var marqueeSpeedText by remember { mutableStateOf(currentSettings.marqueeSpeed.toString()) }
    var marqueeDelayMillisText by remember { mutableStateOf(currentSettings.marqueeDelayMillis.toString()) }
    var maxRecentCountText by remember { mutableStateOf(currentSettings.maxRecentCount.toString()) }

    // System Main Menu States
    var showSystemMainMenuTitle by remember { mutableStateOf(currentSettings.showSystemMainMenuTitle) }
    var systemMainMenuTitleText by remember { mutableStateOf(currentSettings.systemMainMenuTitle) }
    var systemMainMenuDescriptionText by remember { mutableStateOf(currentSettings.systemMainMenuDescription) }
    var systemMainMenuIconPathText by remember { mutableStateOf(currentSettings.systemMainMenuIconPath) }
    var systemMenuTileMarginLeftText by remember { mutableStateOf(currentSettings.systemMenuTileMarginLeftDp.toString()) }
    var systemMenuTileMarginRightText by remember { mutableStateOf(currentSettings.systemMenuTileMarginRightDp.toString()) }
    var showSystemMainMenuEditIcon by remember { mutableStateOf(currentSettings.showSystemMainMenuEditIcon) }
    var systemMainMenuStyle by remember { mutableStateOf(if (currentSettings.systemMainMenuStyle == "CAROUSEL") "ICON_GRID" else currentSettings.systemMainMenuStyle) }
    var systemMainMenuGridStyle by remember { mutableStateOf(currentSettings.systemMainMenuGridStyle) }
    var showSystemTitle by remember { mutableStateOf(currentSettings.showSystemTitle) }
    var expandedMainMenuStyleDropdown by remember { mutableStateOf(false) }
    var expandedGridStyleDropdown by remember { mutableStateOf(false) }
    var systemMenuTextSize by remember { mutableFloatStateOf(currentSettings.systemMenuTextSizeSp.toFloat()) }
    var systemMenuTextAlignment by remember { mutableStateOf(currentSettings.systemMenuTextAlignment) }

    // Sound / Audio States
    var expandedAudio by remember { mutableStateOf(false) }
    var enableNavigationSound by remember { mutableStateOf(currentSettings.enableNavigationSound) }
    var enableBgm by remember { mutableStateOf(currentSettings.enableBgm) }
    var mainMenuIconGridScalePercent by remember { mutableFloatStateOf(currentSettings.mainMenuIconGridScalePercent.toFloat()) }
    var selectedSfxFileName by remember { mutableStateOf(currentSettings.selectedSfxFileName) }

    // Theme States
    var backgroundColorHex by remember { mutableStateOf(currentSettings.backgroundColorHex) }
    var surfaceColorHex by remember { mutableStateOf(currentSettings.surfaceColorHex) }
    var primaryColorHex by remember { mutableStateOf(currentSettings.primaryColorHex) }
    var textColorHex by remember { mutableStateOf(currentSettings.textColorHex) }
    var cardBackgroundColorHex by remember { mutableStateOf(currentSettings.cardBackgroundColorHex) }
    var topBarColorHex by remember { mutableStateOf(currentSettings.topBarColorHex) }
    var bottomBarColorHex by remember { mutableStateOf(currentSettings.bottomBarColorHex) }

    // Other States
    var customArcadeDbPathText by remember { mutableStateOf(currentSettings.customArcadeDbPath) }
    var showCustomArcadeDbFilePicker by remember { mutableStateOf(false) }
    var autoHideScrollbar by remember { mutableStateOf(currentSettings.autoHideScrollbar) }
    var scrollbarShowDurationMsText by remember { mutableStateOf(currentSettings.scrollbarShowDurationMs.toString()) }
    var removeCharsFromGameNamesText by remember { mutableStateOf(currentSettings.removeCharsFromGameNames) }

    // Auto Pop-up Icon States
    var enableRomIconPopUp by remember { mutableStateOf(currentSettings.enableRomIconPopUp) }
    var romIconPopUpTimeoutMsText by remember { mutableStateOf(currentSettings.romIconPopUpTimeoutMs.toString()) }
    var romIconPopUpAlignment by remember { mutableStateOf(currentSettings.romIconPopUpAlignment) }
    var romIconPopUpWidthPercentText by remember { mutableStateOf(currentSettings.romIconPopUpWidthPercent.toString()) }
    var expandedAlignmentDropdown by remember { mutableStateOf(false) }

    // Sleep Timeout States
    var sleepTimeoutMode by remember { mutableStateOf(currentSettings.sleepTimeoutMode) }
    var sleepTimeoutSecondsText by remember { mutableStateOf(currentSettings.sleepTimeoutSeconds.toString()) }
    var expandedSleepModeDropdown by remember { mutableStateOf(false) }

    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MAIN SETTINGS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Accordion List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. SCREEN ACCORDION
                    AccordionCard(
                        title = "Screen Settings",
                        icon = Icons.Default.AspectRatio,
                        isExpanded = expandedScreen,
                        onToggle = { expandedScreen = !expandedScreen }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CANVAS LAYOUT MODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val displayModes = remember {
                                listOf(
                                    DisplayMode.FULL_SCREEN to ("Full Screen (100% Height)" to "Standard launcher full display view"),
                                    DisplayMode.TOP_HALF to ("Top Half Screen (50% Height)" to "Top screen canvas (3DS style / dual-screen)"),
                                    DisplayMode.LOWER_HALF to ("Lower Half Screen (50% Height)" to "Bottom screen canvas (Multitasking)"),
                                    DisplayMode.CUSTOM_SIZE to ("Custom Resolution & Margins" to "Specify width, height and margins")
                                )
                            }

                            val selectedModeInfo = displayModes.firstOrNull { it.first == mode }?.second ?: ("Full Screen" to "")

                            ExposedDropdownMenuBox(
                                expanded = expandedModeDropdown,
                                onExpandedChange = { expandedModeDropdown = !expandedModeDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedModeInfo.first,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Layout Mode") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModeDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedModeDropdown,
                                    onDismissRequest = { expandedModeDropdown = false }
                                ) {
                                    displayModes.forEach { (dispMode, info) ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(info.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                    Text(
                                                        info.second,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                mode = dispMode
                                                expandedModeDropdown = false
                                            },
                                            leadingIcon = {
                                                if (mode == dispMode) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (mode == DisplayMode.CUSTOM_SIZE) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "CUSTOM PIXEL SIZE & MARGINS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = widthPxText,
                                        onValueChange = { widthPxText = it },
                                        label = { Text("Width (px)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = heightPxText,
                                        onValueChange = { heightPxText = it },
                                        label = { Text("Height (px)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = topMarginText,
                                        onValueChange = { topMarginText = it },
                                        label = { Text("Top") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = bottomMarginText,
                                        onValueChange = { bottomMarginText = it },
                                        label = { Text("Bottom") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = leftMarginText,
                                        onValueChange = { leftMarginText = it },
                                        label = { Text("Left") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = rightMarginText,
                                        onValueChange = { rightMarginText = it },
                                        label = { Text("Right") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "TOP BAR TITLE & ICON",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = launcherTitleText,
                                onValueChange = { launcherTitleText = it },
                                label = { Text("Top Bar Title Text") },
                                placeholder = { Text("RetroLauncher") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            IconPickerInput(
                                iconNameOrPath = launcherIconPathText,
                                onIconSelected = { launcherIconPathText = it },
                                label = "Title Bar Icon"
                            )
                        }
                    }

                    // 2. TOP & BOTTOM BAR ACCORDION
                    AccordionCard(
                        title = "Top & Bottom Bar",
                        icon = Icons.Default.SwapVert,
                        isExpanded = expandedTopBottomBar,
                        onToggle = { expandedTopBottomBar = !expandedTopBottomBar }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            text = "Switch Top & Bottom Bar",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Swap positions of header and bottom status bar",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = swapTopAndBottomBar,
                                        onCheckedChange = { swapTopAndBottomBar = it }
                                    )
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
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Show Bottom Status Bar",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Toggle bottom status bar visibility",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = showBottomBar,
                                        onCheckedChange = { showBottomBar = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // 3. SYSTEM MAIN MENU ACCORDION
                    AccordionCard(
                        title = "System Main Menu",
                        icon = Icons.Default.GridView,
                        isExpanded = expandedSystemMainMenu,
                        onToggle = { expandedSystemMainMenu = !expandedSystemMainMenu }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // System Main Menu Title Customization
                            OutlinedTextField(
                                value = systemMainMenuTitleText,
                                onValueChange = { systemMainMenuTitleText = it },
                                label = { Text("System Main Menu Title") },
                                placeholder = { Text("e.g. SYSTEM MAIN MENU or CONSOLES") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // System Main Menu Description Customization
                            OutlinedTextField(
                                value = systemMainMenuDescriptionText,
                                onValueChange = { systemMainMenuDescriptionText = it },
                                label = { Text("System Main Menu Description") },
                                placeholder = { Text("Select a console / system to launch games") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // System Main Menu Select Icon
                            Text(
                                text = "System Main Menu Icon",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Icon displayed on system main menu header",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            IconPickerInput(
                                iconNameOrPath = systemMainMenuIconPathText,
                                onIconSelected = { systemMainMenuIconPathText = it },
                                label = "System Main Menu Icon"
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Tile Margin Left and Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = systemMenuTileMarginLeftText,
                                    onValueChange = { systemMenuTileMarginLeftText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tile Margin Left (dp)") },
                                    placeholder = { Text("e.g. 0") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = systemMenuTileMarginRightText,
                                    onValueChange = { systemMenuTileMarginRightText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tile Margin Right (dp)") },
                                    placeholder = { Text("e.g. 0") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "DISPLAY MODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val menuStyles = remember {
                                listOf(
                                    "ICON_GRID" to "Standard Grid Mode",
                                    "GRID_ALT" to "Grid Alternative Mode",
                                    "TEXT_LIST" to "Text List Mode"
                                )
                            }
                            val selectedMenuStyleLabel = menuStyles.firstOrNull { it.first == systemMainMenuStyle }?.second ?: "Standard Grid Mode"

                            ExposedDropdownMenuBox(
                                expanded = expandedMainMenuStyleDropdown,
                                onExpandedChange = { expandedMainMenuStyleDropdown = !expandedMainMenuStyleDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedMenuStyleLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Main Menu Active Layout Style") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMainMenuStyleDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedMainMenuStyleDropdown,
                                    onDismissRequest = { expandedMainMenuStyleDropdown = false }
                                ) {
                                    menuStyles.forEach { (styleKey, styleLabel) ->
                                        DropdownMenuItem(
                                            text = { Text(styleLabel, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                systemMainMenuStyle = styleKey
                                                if (styleKey == "ICON_GRID" || styleKey == "GRID_ALT") {
                                                    systemMainMenuGridStyle = styleKey
                                                }
                                                expandedMainMenuStyleDropdown = false
                                            },
                                            leadingIcon = {
                                                if (systemMainMenuStyle == styleKey) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    Text(
                                        text = "Show Header Bar",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Switch(
                                        checked = showSystemMainMenuTitle,
                                        onCheckedChange = { showSystemMainMenuTitle = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

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
                                    Text(
                                        text = "Show Edit Icon on System Cards",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Switch(
                                        checked = showSystemMainMenuEditIcon,
                                        onCheckedChange = { showSystemMainMenuEditIcon = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

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
                                    Text(
                                        text = "Show System Selector Title Bar",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Switch(
                                        checked = showSystemTitle,
                                        onCheckedChange = { showSystemTitle = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ICON GRID SIZE PERCENTAGE
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Icon Grid Size (%)", style = MaterialTheme.typography.bodyMedium)
                                    Text("${mainMenuIconGridScalePercent.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = mainMenuIconGridScalePercent,
                                    onValueChange = { mainMenuIconGridScalePercent = it },
                                    valueRange = 50f..150f,
                                    steps = 19
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // TEXT DISPLAY STYLE SETTING (DEDICATED)
                            Text(
                                text = "TEXT DISPLAY STYLE SETTINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Text Size", style = MaterialTheme.typography.bodyMedium)
                                    Text("${systemMenuTextSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = systemMenuTextSize,
                                    onValueChange = { systemMenuTextSize = it },
                                    valueRange = 12f..28f,
                                    steps = 15
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Text Alignment", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = systemMenuTextAlignment == "LEFT",
                                    onClick = { systemMenuTextAlignment = "LEFT" },
                                    label = { Text("Left") },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = systemMenuTextAlignment == "CENTER",
                                    onClick = { systemMenuTextAlignment = "CENTER" },
                                    label = { Text("Center") },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignCenter, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = systemMenuTextAlignment == "RIGHT",
                                    onClick = { systemMenuTextAlignment = "RIGHT" },
                                    label = { Text("Right") },
                                    leadingIcon = { Icon(Icons.Default.FormatAlignRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. AUDIO & SOUND SETTINGS ACCORDION
                    AccordionCard(
                        title = "Audio & Sound",
                        icon = Icons.Default.VolumeUp,
                        isExpanded = expandedAudio,
                        onToggle = { expandedAudio = !expandedAudio }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            text = "Navigation Sound Effects",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Play click sound on D-pad navigation and button actions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = enableNavigationSound,
                                        onCheckedChange = { enableNavigationSound = it }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // SFX File Picker
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Navigation SFX File",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Place audio files (.wav, .mp3) in ${baseDirName}/sfx",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    var showSfxDropdown by remember { mutableStateOf(false) }

                                    Box {
                                        OutlinedButton(
                                            onClick = { showSfxDropdown = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (selectedSfxFileName.isBlank() || selectedSfxFileName == "Default") "Default (Synthesized Click)" else selectedSfxFileName)
                                        }
                                        DropdownMenu(
                                            expanded = showSfxDropdown,
                                            onDismissRequest = { showSfxDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Default (Synthesized Click)") },
                                                onClick = {
                                                    selectedSfxFileName = "Default"
                                                    showSfxDropdown = false
                                                    com.example.util.SoundManager.playNavSound(true, context, "Default")
                                                }
                                            )
                                            sfxFileList.forEach { sfx ->
                                                DropdownMenuItem(
                                                    text = { Text(sfx) },
                                                    onClick = {
                                                        selectedSfxFileName = sfx
                                                        showSfxDropdown = false
                                                        com.example.util.SoundManager.playNavSound(true, context, sfx)
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
                                            text = "Background Music (BGM)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Play music files from ${baseDirName}/bgm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = enableBgm,
                                        onCheckedChange = { enableBgm = it }
                                    )
                                }
                            }
                        }
                    }

                    // 5. THEME ACCORDION
                    AccordionCard(
                        title = "Theme & Colors",
                        icon = Icons.Default.Palette,
                        isExpanded = expandedTheme,
                        onToggle = { expandedTheme = !expandedTheme }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "COLOR SCHEME PRESETS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = primaryColorHex == "#3D5AFE" && backgroundColorHex == "#121212",
                                    onClick = {
                                        backgroundColorHex = "#121212"
                                        surfaceColorHex = "#1E1E1E"
                                        primaryColorHex = "#3D5AFE"
                                        textColorHex = "#FFFFFF"
                                        cardBackgroundColorHex = "#2A2A2A"
                                    },
                                    label = { Text("Default", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = primaryColorHex == "#00E5FF" && backgroundColorHex == "#000000",
                                    onClick = {
                                        backgroundColorHex = "#000000"
                                        surfaceColorHex = "#0A0A0A"
                                        primaryColorHex = "#00E5FF"
                                        textColorHex = "#FFFFFF"
                                        cardBackgroundColorHex = "#141414"
                                    },
                                    label = { Text("OLED", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = primaryColorHex == "#D0D0D0",
                                    onClick = {
                                        backgroundColorHex = "#181818"
                                        surfaceColorHex = "#282828"
                                        primaryColorHex = "#D0D0D0"
                                        textColorHex = "#EEEEEE"
                                        cardBackgroundColorHex = "#333333"
                                    },
                                    label = { Text("Grayscale", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ColorPickerInput(
                                    colorHex = backgroundColorHex,
                                    onColorHexChange = { backgroundColorHex = it },
                                    label = "Background Color"
                                )
                                ColorPickerInput(
                                    colorHex = surfaceColorHex,
                                    onColorHexChange = { surfaceColorHex = it },
                                    label = "Surface Color"
                                )
                                ColorPickerInput(
                                    colorHex = primaryColorHex,
                                    onColorHexChange = { primaryColorHex = it },
                                    label = "Highlight / Primary Color"
                                )
                                ColorPickerInput(
                                    colorHex = textColorHex,
                                    onColorHexChange = { textColorHex = it },
                                    label = "Text Color"
                                )
                                ColorPickerInput(
                                    colorHex = topBarColorHex,
                                    onColorHexChange = { topBarColorHex = it },
                                    label = "Top Bar Color"
                                )
                                ColorPickerInput(
                                    colorHex = bottomBarColorHex,
                                    onColorHexChange = { bottomBarColorHex = it },
                                    label = "Bottom Bar Color"
                                )
                            }
                        }
                    }

                    // 6. OTHER ACCORDION
                    AccordionCard(
                        title = "Other",
                        icon = Icons.Default.MoreHoriz,
                        isExpanded = expandedOther,
                        onToggle = { expandedOther = !expandedOther }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = maxRecentCountText,
                                onValueChange = { maxRecentCountText = it.filter { c -> c.isDigit() } },
                                label = { Text("Max Recent Games Count") },
                                placeholder = { Text("e.g. 30") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Column {
                                Text(
                                    text = "Custom Arcade Database",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Supports DAT, Libretro RDB XML, or key-value title maps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customArcadeDbPathText,
                                        onValueChange = { customArcadeDbPathText = it },
                                        label = { Text("Database File Path (.xml/.dat/.rdb)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = { showCustomArcadeDbFilePicker = true }
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Browse")
                                    }
                                }
                            }

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
                                            text = "Auto-Hide Scrollbar",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Automatically hide scrollbar when not scrolling",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoHideScrollbar,
                                        onCheckedChange = { autoHideScrollbar = it }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = scrollbarShowDurationMsText,
                                onValueChange = { scrollbarShowDurationMsText = it.filter { c -> c.isDigit() } },
                                label = { Text("Scrollbar Show Duration (ms)") },
                                placeholder = { Text("e.g. 1500") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = removeCharsFromGameNamesText,
                                onValueChange = { removeCharsFromGameNamesText = it },
                                label = { Text("Remove Characters from Game Names") },
                                placeholder = { Text("Characters to delete, e.g. -/\\") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "AUTO POP-UP ICON SETTINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

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
                                            text = "Auto Pop-up ROM/Game Icon",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Shows a larger preview of the selected game icon",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = enableRomIconPopUp,
                                        onCheckedChange = { enableRomIconPopUp = it }
                                    )
                                }
                            }

                            if (enableRomIconPopUp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = romIconPopUpTimeoutMsText,
                                        onValueChange = { romIconPopUpTimeoutMsText = it.filter { c -> c.isDigit() } },
                                        label = { Text("Pop-up Delay (ms)") },
                                        placeholder = { Text("e.g. 1000") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = romIconPopUpWidthPercentText,
                                        onValueChange = { romIconPopUpWidthPercentText = it.filter { c -> c.isDigit() } },
                                        label = { Text("Width Size (%)") },
                                        placeholder = { Text("e.g. 30") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                val alignmentOptions = listOf(
                                    "top_left" to "Top Left",
                                    "top_center" to "Top Center",
                                    "top_right" to "Top Right",
                                    "middle_left" to "Middle Left",
                                    "middle_center" to "Middle Center",
                                    "middle_right" to "Middle Right",
                                    "bottom_left" to "Bottom Left",
                                    "bottom_center" to "Bottom Center",
                                    "bottom_right" to "Bottom Right"
                                )
                                val selectedAlignmentLabel = alignmentOptions.firstOrNull { it.first == romIconPopUpAlignment }?.second ?: "Middle Center"

                                ExposedDropdownMenuBox(
                                    expanded = expandedAlignmentDropdown,
                                    onExpandedChange = { expandedAlignmentDropdown = !expandedAlignmentDropdown }
                                ) {
                                    OutlinedTextField(
                                        value = selectedAlignmentLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pop-up Position") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAlignmentDropdown) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedAlignmentDropdown,
                                        onDismissRequest = { expandedAlignmentDropdown = false }
                                    ) {
                                        alignmentOptions.forEach { (alignVal, alignLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(alignLabel, style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    romIconPopUpAlignment = alignVal
                                                    expandedAlignmentDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "TEXT MARQUEE SETTINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = marqueeSpeedText,
                                    onValueChange = { marqueeSpeedText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Speed (dp/s)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = marqueeDelayMillisText,
                                    onValueChange = { marqueeDelayMillisText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Delay (ms)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "APPLICATION SLEEP TIMEOUT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val sleepOptions = listOf(
                                "DEVICE" to "Use device sleep timeout",
                                "ALWAYS_ON" to "Always on",
                                "CUSTOM" to "Override in typed seconds"
                            )
                            val selectedSleepLabel = sleepOptions.firstOrNull { it.first == sleepTimeoutMode }?.second ?: "Use device sleep timeout"

                            ExposedDropdownMenuBox(
                                expanded = expandedSleepModeDropdown,
                                onExpandedChange = { expandedSleepModeDropdown = !expandedSleepModeDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedSleepLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sleep Timeout Option") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSleepModeDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedSleepModeDropdown,
                                    onDismissRequest = { expandedSleepModeDropdown = false }
                                ) {
                                    sleepOptions.forEach { (sleepVal, sleepLabel) ->
                                        DropdownMenuItem(
                                            text = { Text(sleepLabel, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                sleepTimeoutMode = sleepVal
                                                expandedSleepModeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (sleepTimeoutMode == "CUSTOM") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = sleepTimeoutSecondsText,
                                    onValueChange = { sleepTimeoutSecondsText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Sleep Timeout (seconds)") },
                                    placeholder = { Text("e.g. 30") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                                val adminComponent = remember { android.content.ComponentName(context, com.example.receiver.AdminReceiver::class.java) }
                                val isAdminActive = remember(sleepTimeoutMode) { dpm?.isAdminActive(adminComponent) == true }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAdminActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, if (isAdminActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isAdminActive) "✅ Lock Permission Active" else "⚠️ Lock Permission Required",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isAdminActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = if (isAdminActive) 
                                                "RetroLauncher has permission to turn off the screen directly." 
                                                else "To turn off the screen directly on sleep timeout, RetroLauncher needs Device Administrator permission.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!isAdminActive) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = {
                                                    val intent = android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                                        putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                                        putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to turn off the screen directly on sleep timeout.")
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.align(Alignment.End),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                )
                                            ) {
                                                Text("Grant Permission", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "SYSTEM TOOLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenGamepadSettings?.invoke()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Gamepad, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gamepad Mapping")
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenConfigFileManager?.invoke()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Configs")
                                }
                            }
                        }
                    }
                }

                if (showCustomArcadeDbFilePicker) {
                    FilePickerDialog(
                        initialPath = customArcadeDbPathText,
                        onDismiss = { showCustomArcadeDbFilePicker = false },
                        onFileSelected = { selectedPath ->
                            customArcadeDbPathText = selectedPath
                            showCustomArcadeDbFilePicker = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val newSettings = DisplaySettings(
                                mode = mode,
                                customWidthPx = widthPxText.toIntOrNull() ?: 1080,
                                customHeightPx = heightPxText.toIntOrNull() ?: 1920,
                                marginTopDp = topMarginText.toIntOrNull() ?: 0,
                                marginBottomDp = bottomMarginText.toIntOrNull() ?: 0,
                                marginLeftDp = leftMarginText.toIntOrNull() ?: 0,
                                marginRightDp = rightMarginText.toIntOrNull() ?: 0,
                                launcherTitle = launcherTitleText.ifBlank { "RetroLauncher" },
                                launcherIconPath = launcherIconPathText.ifBlank { "gamepad" },
                                backgroundColorHex = backgroundColorHex,
                                surfaceColorHex = surfaceColorHex,
                                primaryColorHex = primaryColorHex,
                                textColorHex = textColorHex,
                                cardBackgroundColorHex = cardBackgroundColorHex,
                                showSystemMainMenuTitle = showSystemMainMenuTitle,
                                showSystemMainMenuEditIcon = showSystemMainMenuEditIcon,
                                systemMainMenuStyle = systemMainMenuStyle,
                                systemMainMenuGridStyle = systemMainMenuGridStyle,
                                showSystemTitle = showSystemTitle,
                                swapTopAndBottomBar = swapTopAndBottomBar,
                                systemMenuTextSizeSp = systemMenuTextSize.toInt(),
                                systemMenuTextAlignment = systemMenuTextAlignment,
                                marqueeSpeed = marqueeSpeedText.toIntOrNull() ?: 30,
                                marqueeDelayMillis = marqueeDelayMillisText.toIntOrNull() ?: 1200,
                                systemMainMenuTitle = systemMainMenuTitleText.ifBlank { "SYSTEM MAIN MENU" },
                                systemMainMenuDescription = systemMainMenuDescriptionText.ifBlank { "Select a console / system to launch games" },
                                enableNavigationSound = enableNavigationSound,
                                enableBgm = enableBgm,
                                mainMenuIconGridScalePercent = mainMenuIconGridScalePercent.toInt(),
                                selectedSfxFileName = selectedSfxFileName,
                                maxRecentCount = maxRecentCountText.toIntOrNull() ?: 30,
                                customArcadeDbPath = customArcadeDbPathText,
                                autoHideScrollbar = autoHideScrollbar,
                                scrollbarShowDurationMs = scrollbarShowDurationMsText.toIntOrNull() ?: 1500,
                                systemMenuTileMarginLeftDp = systemMenuTileMarginLeftText.toIntOrNull() ?: 0,
                                systemMenuTileMarginRightDp = systemMenuTileMarginRightText.toIntOrNull() ?: 0,
                                systemMainMenuIconPath = systemMainMenuIconPathText.ifBlank { "gamepad" },
                                topBarColorHex = topBarColorHex,
                                bottomBarColorHex = bottomBarColorHex,
                                removeCharsFromGameNames = removeCharsFromGameNamesText,
                                enableRomIconPopUp = enableRomIconPopUp,
                                romIconPopUpTimeoutMs = romIconPopUpTimeoutMsText.toIntOrNull() ?: 1000,
                                romIconPopUpAlignment = romIconPopUpAlignment,
                                romIconPopUpWidthPercent = romIconPopUpWidthPercentText.toIntOrNull() ?: 30,
                                sleepTimeoutMode = sleepTimeoutMode,
                                sleepTimeoutSeconds = sleepTimeoutSecondsText.toIntOrNull() ?: 30
                            )
                            onSaveSettings(newSettings)
                            if (bottomBarSettings != null && onSaveBottomBarSettings != null) {
                                onSaveBottomBarSettings(bottomBarSettings.copy(showBottomBar = showBottomBar))
                            }
                            onDismiss()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccordionCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
