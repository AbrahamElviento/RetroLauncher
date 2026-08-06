package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.GameRomEntity
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity
import com.example.data.launcher.LaunchResult
import com.example.ui.components.*
import com.example.ui.viewmodel.LauncherViewModel

enum class ActiveFocusZone {
    TOP_BAR,
    SYSTEM_SELECTOR,
    ROM_LIST,
    BOTTOM_BAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val systems by viewModel.systems.collectAsStateWithLifecycle()
    val standaloneProfiles by viewModel.standaloneProfiles.collectAsStateWithLifecycle()
    val selectedSystemId by viewModel.selectedSystemId.collectAsStateWithLifecycle()
    val roms by viewModel.currentSystemRoms.collectAsStateWithLifecycle()
    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val allRoms by viewModel.allRoms.collectAsStateWithLifecycle()
    val romListSettings by viewModel.romListSettings.collectAsStateWithLifecycle()
    val gamepadSettings by viewModel.gamepadSettings.collectAsStateWithLifecycle()
    val bottomBarSettings by viewModel.bottomBarSettings.collectAsStateWithLifecycle()
    val hiddenAndroidApps by viewModel.hiddenAndroidApps.collectAsStateWithLifecycle()
    val customIcons by viewModel.customIcons.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var showEmptyFolderAlert by remember { mutableStateOf<String?>(null) }
    var bottomBarNotification by remember { mutableStateOf<String?>(null) }
    var showScanConfirmation by remember { mutableStateOf<SystemEntity?>(null) }

    // Screen sleep prevention during scanning
    val activity = context as? Activity
    DisposableEffect(isScanning) {
        if (isScanning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val showTopBar = displaySettings.showTopBar

    var showSearchDialog by remember { mutableStateOf(false) }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }

    var showSystemManagementDialog by remember { mutableStateOf(false) }
    var showBgmControlDialog by remember { mutableStateOf(false) }
    var showSlideshowControlDialog by remember { mutableStateOf(false) }
    var activeSlideshowSystemName by remember { mutableStateOf("") }
    var showSystemEditDialog by remember { mutableStateOf(false) }
    var editingSystem by remember { mutableStateOf<SystemEntity?>(null) }

    var showXmlEditorDialog by remember { mutableStateOf(false) }
    var editingXmlProfile by remember { mutableStateOf<StandaloneProfileEntity?>(null) }

    var showDisplaySettingsDialog by remember { mutableStateOf(false) }
    var showConfigFileManagerDialog by remember { mutableStateOf(false) }
    var showGamepadSettingsDialog by remember { mutableStateOf(false) }
    var showBottomBarSettingsDialog by remember { mutableStateOf(false) }
    var showRomListStyleDialog by remember { mutableStateOf(false) }

    var showGameDetailsDialog by remember { mutableStateOf<GameRomEntity?>(null) }
    var missingPackageAlert by remember { mutableStateOf<LaunchResult.PackageNotInstalled?>(null) }

    // Gamepad trigger counters
    var pageUpTrigger by remember { mutableLongStateOf(0L) }
    var pageDownTrigger by remember { mutableLongStateOf(0L) }
    var goToTopTrigger by remember { mutableLongStateOf(0L) }
    var goToBottomTrigger by remember { mutableLongStateOf(0L) }
    var selectActionTrigger by remember { mutableLongStateOf(0L) }
    var backActionTrigger by remember { mutableLongStateOf(0L) }
    var dpadUpTrigger by remember { mutableLongStateOf(0L) }
    var dpadDownTrigger by remember { mutableLongStateOf(0L) }
    var dpadLeftTrigger by remember { mutableLongStateOf(0L) }
    var dpadRightTrigger by remember { mutableLongStateOf(0L) }
    var favoriteActionTrigger by remember { mutableLongStateOf(0L) }
    var infoActionTrigger by remember { mutableLongStateOf(0L) }
    var highlightFavoritesTrigger by remember { mutableLongStateOf(0L) }

    var isMainMenuActive by remember { mutableStateOf(true) }
    var activeFocusZone by remember { mutableStateOf(ActiveFocusZone.ROM_LIST) }
    var topBarFocusedIndex by remember { mutableIntStateOf(0) }
    var systemSelectorFocusedIndex by remember { mutableIntStateOf(2) }

    var isTopBarComboKey1Down by remember { mutableStateOf(false) }
    var isTopBarComboKey2Down by remember { mutableStateOf(false) }
    var isTopBarComboTriggered by remember { mutableStateOf(false) }

    val currentSystem = systems.firstOrNull { it.id == selectedSystemId }

    val isAnyDialogOpen = showSearchDialog ||
            showAppVisibilityDialog ||
            showSystemManagementDialog ||
            showSystemEditDialog ||
            showXmlEditorDialog ||
            showDisplaySettingsDialog ||
            showConfigFileManagerDialog ||
            showGamepadSettingsDialog ||
            showBottomBarSettingsDialog ||
            showRomListStyleDialog ||
            showGameDetailsDialog != null ||
            missingPackageAlert != null ||
            showEmptyFolderAlert != null ||
            showScanConfirmation != null

    BackHandler(enabled = !isAnyDialogOpen) {
        if (activeFocusZone != ActiveFocusZone.ROM_LIST) {
            activeFocusZone = ActiveFocusZone.ROM_LIST
        } else if (!isMainMenuActive) {
            backActionTrigger = System.currentTimeMillis()
        } else {
            // On system main menu, pressing back does nothing
        }
    }

    // Keep bottomBarNotification up to date while scanning
    LaunchedEffect(isScanning, currentSystem) {
        if (isScanning) {
            bottomBarNotification = "Scanning Directory : ${currentSystem?.folderPath ?: ""}"
        } else {
            if (bottomBarNotification?.startsWith("Scanning Directory") == true) {
                bottomBarNotification = null
            }
        }
    }

    // Observe launch events
    LaunchedEffect(Unit) {
        viewModel.launchEvent.collect { result ->
            when (result) {
                is LaunchResult.Success -> {
                    if (viewModel.displaySettings.value.showLaunchToast) {
                        Toast.makeText(context, "Launching game...", Toast.LENGTH_SHORT).show()
                    }
                }
                is LaunchResult.PackageNotInstalled -> {
                    missingPackageAlert = result
                }
                is LaunchResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Observe scan events
    LaunchedEffect(Unit) {
        viewModel.scanEvent.collect { event ->
            when (event) {
                is LauncherViewModel.ScanEvent.ScanFinished -> {
                    val isVirtualOrAndroid = event.systemId.startsWith("widget_") || 
                            event.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                            viewModel.systems.value.any { it.id == event.systemId && (it.defaultLaunchMode == "ANDROID_APP" || it.defaultLaunchMode == "VIRTUAL") }

                    if (event.romCount == 0) {
                        if (!isVirtualOrAndroid) {
                            showEmptyFolderAlert = event.folderPath
                        }
                        bottomBarNotification = "Scan completed! No games found."
                    } else if (event.romCount > 0) {
                        bottomBarNotification = "Scan completed! Found ${event.romCount} games."
                    } else {
                        bottomBarNotification = "Scan completed!"
                    }
                    kotlinx.coroutines.delay(5000)
                    if (!isScanning) {
                        bottomBarNotification = null
                    }
                }
            }
        }
    }

    // Background Music (BGM) playback
    LaunchedEffect(displaySettings.enableBgm) {
        com.example.util.SoundManager.updateBgmState(context, displaySettings.enableBgm)
    }
    DisposableEffect(Unit) {
        onDispose {
            com.example.util.SoundManager.stopBgm()
        }
    }

    CompositionLocalProvider(LocalDisplaySettings provides displaySettings) {
        // Outer Canvas Layout Wrapper applying resolution bounds & margins!
    CanvasLayoutWrapper(
        displaySettings = displaySettings,
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var initialY: Float? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val changes = event.changes
                        if (changes.size == 2) {
                            val avgY = (changes[0].position.y + changes[1].position.y) / 2f
                            if (initialY == null) {
                                initialY = avgY
                            } else {
                                val deltaY = avgY - initialY
                                if (deltaY < -150f) { // Swipe up to hide top bar
                                    viewModel.updateDisplaySettings(displaySettings.copy(showTopBar = false))
                                    initialY = avgY
                                } else if (deltaY > 150f) { // Swipe down to show top bar
                                    viewModel.updateDisplaySettings(displaySettings.copy(showTopBar = true))
                                    initialY = avgY
                                }
                            }
                        } else {
                            initialY = null
                        }
                    }
                }
            }
            .onPreviewKeyEvent { keyEvent ->
            val keyCode = keyEvent.nativeKeyEvent.keyCode
            val isActionDown = keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN

            if (isActionDown) {
                val k1 = gamepadSettings.keyToggleTopBarKey1
                val k2 = gamepadSettings.keyToggleTopBarKey2
                if (k1 > 0 && keyCode == k1) isTopBarComboKey1Down = true
                if (k2 > 0 && keyCode == k2) isTopBarComboKey2Down = true

                if (k1 > 0 && k2 > 0 && isTopBarComboKey1Down && isTopBarComboKey2Down) {
                    if (!isTopBarComboTriggered) {
                        viewModel.updateDisplaySettings(displaySettings.copy(showTopBar = !showTopBar))
                        isTopBarComboTriggered = true
                    }
                    return@onPreviewKeyEvent true
                }

                when {
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                            dpadLeftTrigger = System.currentTimeMillis()
                        } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                            systemSelectorFocusedIndex = maxOf(0, systemSelectorFocusedIndex - 1)
                        } else if (activeFocusZone == ActiveFocusZone.TOP_BAR) {
                            topBarFocusedIndex = 0
                        }
                        true
                    }
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                            dpadRightTrigger = System.currentTimeMillis()
                        } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                            systemSelectorFocusedIndex = minOf(4, systemSelectorFocusedIndex + 1)
                        } else if (activeFocusZone == ActiveFocusZone.TOP_BAR) {
                            topBarFocusedIndex = 0
                        }
                        true
                    }
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!displaySettings.swapTopAndBottomBar) {
                            if (activeFocusZone == ActiveFocusZone.BOTTOM_BAR) {
                                activeFocusZone = ActiveFocusZone.ROM_LIST
                            } else if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                                dpadUpTrigger = System.currentTimeMillis()
                            } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                                if (showTopBar) {
                                    activeFocusZone = ActiveFocusZone.TOP_BAR
                                    //topBarFocusedIndex = 0
                                }
                            }
                        } else {
                            if (activeFocusZone == ActiveFocusZone.TOP_BAR) {
                                activeFocusZone = if (isMainMenuActive) ActiveFocusZone.ROM_LIST else ActiveFocusZone.SYSTEM_SELECTOR
                            } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                                activeFocusZone = ActiveFocusZone.ROM_LIST
                            } else if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                                dpadUpTrigger = System.currentTimeMillis()
                            }
                        }
                        true
                    }
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!displaySettings.swapTopAndBottomBar) {
                            if (activeFocusZone == ActiveFocusZone.TOP_BAR) {
                                activeFocusZone = if (isMainMenuActive) ActiveFocusZone.ROM_LIST else ActiveFocusZone.SYSTEM_SELECTOR
                            } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                                activeFocusZone = ActiveFocusZone.ROM_LIST
                            } else if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                                dpadDownTrigger = System.currentTimeMillis()
                            }
                        } else {
                            if (activeFocusZone == ActiveFocusZone.BOTTOM_BAR) {
                                activeFocusZone = if (isMainMenuActive) ActiveFocusZone.ROM_LIST else ActiveFocusZone.SYSTEM_SELECTOR
                            } else if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) {
                                activeFocusZone = ActiveFocusZone.ROM_LIST
                            } else if (activeFocusZone == ActiveFocusZone.ROM_LIST) {
                                dpadDownTrigger = System.currentTimeMillis()
                            }
                        }
                        true
                    }
                    keyCode == gamepadSettings.keyPageUp -> {
                        pageUpTrigger = System.currentTimeMillis()
                        true
                    }
                    keyCode == gamepadSettings.keyPageDown -> {
                        pageDownTrigger = System.currentTimeMillis()
                        true
                    }
                    keyCode == gamepadSettings.keyGoToTop -> {
                        goToTopTrigger = System.currentTimeMillis()
                        true
                    }
                    keyCode == gamepadSettings.keyGoToBottom -> {
                        goToBottomTrigger = System.currentTimeMillis()
                        true
                    }
                    keyCode == gamepadSettings.keyFavoriteAction -> {
                        favoriteActionTrigger = System.currentTimeMillis()
                        true
                    }
                    keyCode == gamepadSettings.keyInfoAction -> {
                        infoActionTrigger = System.currentTimeMillis()
                        true
                    }
                    gamepadSettings.keySystemSettings > 0 && keyCode == gamepadSettings.keySystemSettings -> {
                        true
                    }
                    gamepadSettings.keyRomListSettings > 0 && keyCode == gamepadSettings.keyRomListSettings -> {
                        true
                    }
                    keyCode == gamepadSettings.keySystemManagerAction -> {
                        showSystemManagementDialog = true
                        true
                    }
                    (gamepadSettings.keySelectAction > 0 && keyCode == gamepadSettings.keySelectAction) || keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                        if (keyEvent.nativeKeyEvent.repeatCount > 0) {
                            return@onPreviewKeyEvent true
                        }
                        when (activeFocusZone) {
                            ActiveFocusZone.TOP_BAR -> {
                                showDisplaySettingsDialog = true
                            }
                            ActiveFocusZone.SYSTEM_SELECTOR -> {
                                val enabledSystems = systems.filter { 
                                    it.isEnabled && 
                                    !it.id.startsWith("widget_") && 
                                    !it.defaultLaunchMode.startsWith("WIDGET_") 
                                }
                                val curIdx = if (currentSystem != null) enabledSystems.indexOfFirst { it.id == currentSystem.id }.coerceAtLeast(0) else 0
                                when (systemSelectorFocusedIndex) {
                                    0 -> {
                                        isMainMenuActive = true
                                        viewModel.selectSystem(null)
                                        activeFocusZone = ActiveFocusZone.ROM_LIST
                                    }
                                    1 -> {
                                        if (enabledSystems.isNotEmpty()) {
                                            val prevIdx = if (curIdx <= 0) enabledSystems.size - 1 else curIdx - 1
                                            viewModel.selectSystem(enabledSystems[prevIdx].id)
                                        }
                                    }
                                    2 -> {
                                        showSystemManagementDialog = true
                                    }
                                    3 -> {
                                        if (enabledSystems.isNotEmpty()) {
                                            val nextIdx = (curIdx + 1) % enabledSystems.size
                                            viewModel.selectSystem(enabledSystems[nextIdx].id)
                                        }
                                    }
                                    4 -> {
                                        if (currentSystem != null) {
                                            editingSystem = currentSystem
                                            showSystemEditDialog = true
                                        } else {
                                            showSystemManagementDialog = true
                                        }
                                    }
                                }
                            }
                            ActiveFocusZone.BOTTOM_BAR -> {
                                showBottomBarSettingsDialog = true
                            }
                            ActiveFocusZone.ROM_LIST -> {
                                selectActionTrigger = System.currentTimeMillis()
                            }
                        }
                        true
                    }
                    keyCode == gamepadSettings.keyBackAction || keyCode == android.view.KeyEvent.KEYCODE_BACK -> {
                        if (activeFocusZone != ActiveFocusZone.ROM_LIST) {
                            activeFocusZone = ActiveFocusZone.ROM_LIST
                            true
                        } else if (!isMainMenuActive) {
                            backActionTrigger = System.currentTimeMillis()
                            true
                        } else {
                            // On system main menu, back press does nothing
                            true
                        }
                    }
                    else -> false
                }
            } else if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                val k1 = gamepadSettings.keyToggleTopBarKey1
                val k2 = gamepadSettings.keyToggleTopBarKey2
                if (k1 > 0 && keyCode == k1) isTopBarComboKey1Down = false
                if (k2 > 0 && keyCode == k2) isTopBarComboKey2Down = false

                if (isTopBarComboTriggered) {
                    if (!isTopBarComboKey1Down && !isTopBarComboKey2Down) {
                        isTopBarComboTriggered = false
                    }
                    return@onPreviewKeyEvent true
                }

                if (keyCode == gamepadSettings.keySystemSettings) {
                    if (isMainMenuActive) {
                        showSystemManagementDialog = true
                    } else if (currentSystem != null) {
                        editingSystem = currentSystem
                        showSystemEditDialog = true
                    } else {
                        showSystemManagementDialog = true
                    }
                    true
                } else if (keyCode == gamepadSettings.keyRomListSettings) {
                    if (isMainMenuActive) {
                        showSystemManagementDialog = true
                    } else {
                        showRomListStyleDialog = true
                    }
                    true
                } else false
            } else false
        }
    ) {
        val HeaderBarContent: @Composable () -> Unit = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                UniversalIconView(
                                    iconNameOrPath = displaySettings.launcherIconPath.ifEmpty { "gamepad" },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = displaySettings.launcherTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        val topActions = listOf(
                            Icons.Default.Settings to "Display Canvas Settings"
                        )
                    topActions.forEachIndexed { idx, (icon, desc) ->
                        val isFocused = (activeFocusZone == ActiveFocusZone.TOP_BAR && topBarFocusedIndex == idx)
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .border(
                                    width = if (isFocused) 2.dp else 0.dp,
                                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            IconButton(
                                onClick = {
                                    showDisplaySettingsDialog = true
                                },
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = desc,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (displaySettings.topBarColorHex.isNotBlank()) com.example.ui.components.parseHexColor(displaySettings.topBarColorHex, MaterialTheme.colorScheme.surface) else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        val FooterBarContent: @Composable () -> Unit = {
            BottomStatusBar(
                settings = bottomBarSettings,
                isFocused = (activeFocusZone == ActiveFocusZone.BOTTOM_BAR),
                containerColorHex = displaySettings.bottomBarColorHex,
                notificationText = bottomBarNotification,
                onOpenBarSettings = {
                    activeFocusZone = ActiveFocusZone.BOTTOM_BAR
                    showBottomBarSettingsDialog = true
                }
            )
        }

        Scaffold(
            topBar = {
                if (displaySettings.swapTopAndBottomBar) {
                    FooterBarContent()
                } else {
                    HeaderBarContent()
                }
            },
            bottomBar = {
                if (displaySettings.swapTopAndBottomBar) {
                    HeaderBarContent()
                } else {
                    FooterBarContent()
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                if (isMainMenuActive) {
                    val activeSystemIdFromXml = remember(isMainMenuActive) {
                        viewModel.loadActiveSystemId()
                    }
                    com.example.ui.components.SystemMainMenu(
                        systems = systems.filter { it.isEnabled },
                        selectedSystemId = selectedSystemId ?: activeSystemIdFromXml,
                        onSelectAndEnterSystem = { system ->
                            val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")
                            if (system.defaultLaunchMode == "WIDGET_BGM") {
                                showBgmControlDialog = true
                            } else if (system.defaultLaunchMode == "WIDGET_SLIDESHOW") {
                                activeSlideshowSystemName = system.name
                                showSlideshowControlDialog = true
                            } else if (!isWidget) {
                                viewModel.selectSystem(system.id)
                                isMainMenuActive = false
                                activeFocusZone = ActiveFocusZone.ROM_LIST
                            }
                        },
                        onActiveSystemHighlighted = { systemId ->
                            viewModel.saveActiveSystemId(systemId)
                        },
                        onEditSystem = { system ->
                            editingSystem = system
                            showSystemEditDialog = true
                        },
                        onAddSystem = {
                            showSystemManagementDialog = true
                        },
                        totalGamesCount = allRoms.size,
                        totalFavoritesCount = allRoms.count { it.isFavorite },
                        totalCompletedCount = allRoms.count { it.isCompleted },
                        systemMainMenuTitle = displaySettings.systemMainMenuTitle,
                        systemMainMenuDescription = displaySettings.systemMainMenuDescription,
                        onOpenSystemManager = { showSystemManagementDialog = true },
                        onOpenMainSettings = { showDisplaySettingsDialog = true },
                        enableNavigationSound = displaySettings.enableNavigationSound,
                        selectedSfxFileName = displaySettings.selectedSfxFileName,
                        mainMenuIconGridScalePercent = displaySettings.mainMenuIconGridScalePercent,
                        showMainMenuTitle = displaySettings.showSystemMainMenuTitle,
                        showEditIcon = displaySettings.showSystemMainMenuEditIcon,
                        defaultDisplayStyle = displaySettings.systemMainMenuStyle,
                        displayColumns = displaySettings.systemMenuDisplayColumns,
                        displayRows = displaySettings.systemMenuDisplayRows,
                        actualColumns = displaySettings.systemMenuActualColumns,
                        actualRows = displaySettings.systemMenuActualRows,
                        textSizeSp = displaySettings.systemMenuTextSizeSp,
                        textAlignment = displaySettings.systemMenuTextAlignment,
                        marqueeSpeed = displaySettings.marqueeSpeed,
                        marqueeDelayMillis = displaySettings.marqueeDelayMillis,
                        dpadUpTrigger = dpadUpTrigger,
                        dpadDownTrigger = dpadDownTrigger,
                        dpadLeftTrigger = dpadLeftTrigger,
                        dpadRightTrigger = dpadRightTrigger,
                        pageUpTrigger = pageUpTrigger,
                        pageDownTrigger = pageDownTrigger,
                        goToTopTrigger = goToTopTrigger,
                        goToBottomTrigger = goToBottomTrigger,
                        selectActionTrigger = selectActionTrigger,
                        highlightFavoritesTrigger = highlightFavoritesTrigger,
                        isMenuFocused = (activeFocusZone == ActiveFocusZone.ROM_LIST),
                        onMoveFocusUp = { activeFocusZone = ActiveFocusZone.TOP_BAR },
                        onMoveFocusDown = { activeFocusZone = ActiveFocusZone.BOTTOM_BAR },
                        onToggleDisplayStyle = { newStyle ->
                            viewModel.updateDisplaySettings(displaySettings.copy(systemMainMenuStyle = newStyle))
                        },
                        autoHideScrollbar = displaySettings.autoHideScrollbar,
                        scrollbarShowDurationMs = displaySettings.scrollbarShowDurationMs,
                        systemMainMenuIconPath = displaySettings.systemMainMenuIconPath,
                        tileMarginLeftDp = displaySettings.systemMenuTileMarginLeftDp,
                        tileMarginRightDp = displaySettings.systemMenuTileMarginRightDp,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Ultra-Compact System Selector (Icon + Text Title + Prev/Next + Dedicated System Settings)
                    CompactSystemSelector(
                        systems = systems.filter { 
                            !it.id.startsWith("widget_") && 
                            !it.defaultLaunchMode.startsWith("WIDGET_") 
                        },
                        selectedSystem = currentSystem,
                        isFocused = (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR),
                        focusedItemIndex = if (activeFocusZone == ActiveFocusZone.SYSTEM_SELECTOR) systemSelectorFocusedIndex else -1,
                        showSystemTitle = displaySettings.showSystemTitle,
                        onSystemSelected = {
                            viewModel.selectSystem(it.id)
                            isMainMenuActive = false
                            activeFocusZone = ActiveFocusZone.ROM_LIST
                        },
                        onOpenSystemManager = { showSystemManagementDialog = true },
                        onOpenMainMenu = {
                            com.example.util.SoundManager.playNavSound(displaySettings.enableNavigationSound, context, displaySettings.selectedSfxFileName)
                            isMainMenuActive = true
                            viewModel.selectSystem(null)
                            activeFocusZone = ActiveFocusZone.ROM_LIST
                        }
                    )

                    if (displaySettings.showSystemTitle) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ROMs Browser View
                    GameGrid(
                        currentSystem = currentSystem,
                        roms = roms,
                        listSettings = romListSettings,
                        onUpdateListSettings = { viewModel.updateRomListSettings(it) },
                        enableNavigationSound = displaySettings.enableNavigationSound,
                        selectedSfxFileName = displaySettings.selectedSfxFileName,
                        allSystems = systems,
                        customIcons = customIcons,
                        onGameClick = { game ->
                            if (currentSystem != null) {
                                viewModel.launchGame(currentSystem, game)
                            }
                        },
                        onGameLongClick = { game ->
                            showGameDetailsDialog = game
                        },
                        onFavoriteToggle = { game ->
                            viewModel.toggleFavorite(game)
                        },
                        onCompletedToggle = { game ->
                            viewModel.toggleCompleted(game)
                        },
                        onShowGameInfo = { game ->
                            showGameDetailsDialog = game
                        },
                        onDeleteFromRecent = { game ->
                            viewModel.removeFromRecent(game)
                        },
                        onScanFolderClick = {
                            if (currentSystem != null) {
                                showScanConfirmation = currentSystem
                            }
                        },
                        isScanning = isScanning,
                        onOpenAppVisibilityClick = {
                            showAppVisibilityDialog = true
                        },
                        onOpenSystemSettings = {
                            if (currentSystem != null) {
                                editingSystem = currentSystem
                                showSystemEditDialog = true
                            } else {
                                showSystemManagementDialog = true
                            }
                        },
                        onUpdateSystemFolder = { newPath ->
                            if (currentSystem != null) {
                                viewModel.saveSystem(currentSystem.copy(folderPath = newPath))
                            }
                        },
                        isListFocused = (activeFocusZone == ActiveFocusZone.ROM_LIST),
                        marqueeSpeed = displaySettings.marqueeSpeed,
                        marqueeDelayMillis = displaySettings.marqueeDelayMillis,
                        dpadUpTrigger = dpadUpTrigger,
                        dpadDownTrigger = dpadDownTrigger,
                        dpadLeftTrigger = dpadLeftTrigger,
                        dpadRightTrigger = dpadRightTrigger,
                        onMoveFocusUp = { activeFocusZone = ActiveFocusZone.SYSTEM_SELECTOR },
                        onMoveFocusDown = { activeFocusZone = ActiveFocusZone.BOTTOM_BAR },
                        pageUpTrigger = pageUpTrigger,
                        pageDownTrigger = pageDownTrigger,
                        goToTopTrigger = goToTopTrigger,
                        goToBottomTrigger = goToBottomTrigger,
                        selectActionTrigger = selectActionTrigger,
                        backActionTrigger = backActionTrigger,
                        favoriteActionTrigger = favoriteActionTrigger,
                        infoActionTrigger = infoActionTrigger,
                        onBackToMainMenu = {
                            com.example.util.SoundManager.playNavSound(displaySettings.enableNavigationSound, context, displaySettings.selectedSfxFileName)
                            isMainMenuActive = true
                            viewModel.selectSystem(null)
                            activeFocusZone = ActiveFocusZone.ROM_LIST
                        },
                        onPreviousSystem = {
                            val activeSystems = systems.filter { 
                                it.isEnabled && 
                                !it.id.startsWith("widget_") && 
                                !it.defaultLaunchMode.startsWith("WIDGET_") 
                            }
                            if (activeSystems.isNotEmpty()) {
                                val curIdx = if (currentSystem != null) activeSystems.indexOfFirst { it.id == currentSystem.id }.coerceAtLeast(0) else 0
                                val prevIdx = if (curIdx <= 0) activeSystems.size - 1 else curIdx - 1
                                viewModel.selectSystem(activeSystems[prevIdx].id)
                            }
                        },
                        onNextSystem = {
                            val activeSystems = systems.filter { 
                                it.isEnabled && 
                                !it.id.startsWith("widget_") && 
                                !it.defaultLaunchMode.startsWith("WIDGET_") 
                            }
                            if (activeSystems.isNotEmpty()) {
                                val curIdx = if (currentSystem != null) activeSystems.indexOfFirst { it.id == currentSystem.id }.coerceAtLeast(0) else 0
                                val nextIdx = (curIdx + 1) % activeSystems.size
                                viewModel.selectSystem(activeSystems[nextIdx].id)
                            }
                        },
                        autoHideScrollbar = displaySettings.autoHideScrollbar,
                        scrollbarShowDurationMs = displaySettings.scrollbarShowDurationMs,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                } // Closes Column
            } // Closes Box
        } // Closes Scaffold
    }

    // Dedicated ROM Search Dialog
    if (showSearchDialog) {
        SearchDialog(
            allRoms = allRoms,
            systems = systems,
            currentSystemId = selectedSystemId,
            onDismiss = { showSearchDialog = false },
            onLaunchGame = { gameToLaunch ->
                val sys = systems.firstOrNull { it.id == gameToLaunch.systemId } ?: currentSystem
                if (sys != null) {
                    viewModel.launchGame(sys, gameToLaunch)
                }
            }
        )
    }

    // Android App Visibility Settings Dialog
    if (showAppVisibilityDialog) {
        val appSystemId = currentSystem?.id ?: "android_apps"
        val systemTitleName = currentSystem?.name ?: "Android Apps"
        val initialVisible = remember(appSystemId) {
            viewModel.getVisibleAndroidAppsXml(appSystemId)
        }
        AndroidAppVisibilityDialog(
            systemTitle = systemTitleName,
            initialVisiblePackages = initialVisible,
            hiddenPackages = hiddenAndroidApps,
            onDismiss = { showAppVisibilityDialog = false },
            onSaveVisiblePackages = { visibleSet ->
                viewModel.saveVisibleAndroidApps(appSystemId, visibleSet)
            }
        )
    }

    // Dedicated System Manager Dialog
    if (showSystemManagementDialog) {
        SystemManagementDialog(
            systems = systems,
            onDismiss = { showSystemManagementDialog = false },
            onSaveSystem = { sysToSave ->
                viewModel.saveSystem(sysToSave)
            },
            onDeleteSystem = { sysToDelete ->
                viewModel.deleteSystem(sysToDelete)
            },
            onReorderSystems = { reordered ->
                viewModel.reorderSystems(reordered)
            },
            onOpenAppVisibility = {
                showAppVisibilityDialog = true
            }
        )
    }

    if (showBgmControlDialog) {
        com.example.ui.components.BgmControlDialog(
            displaySettings = displaySettings,
            onUpdateBgm = { enabled ->
                viewModel.updateDisplaySettings(displaySettings.copy(enableBgm = enabled))
            },
            onDismiss = { showBgmControlDialog = false }
        )
    }

    if (showSlideshowControlDialog) {
        SlideshowControlDialog(
            systemName = activeSlideshowSystemName,
            onDismiss = { showSlideshowControlDialog = false }
        )
    }

    // System Edit Dialog
    if (showSystemEditDialog) {
        SystemEditDialog(
            system = editingSystem,
            standaloneProfiles = standaloneProfiles,
            onDismiss = { showSystemEditDialog = false },
            onSave = { updatedSystem ->
                viewModel.saveSystem(updatedSystem)
                showSystemEditDialog = false
            },
            onDelete = { sysToDelete ->
                viewModel.deleteSystem(sysToDelete)
                showSystemEditDialog = false
            },
            onOpenAppVisibility = {
                showAppVisibilityDialog = true
            }
        )
    }

    // Custom XML Profile Editor Dialog
    if (showXmlEditorDialog) {
        StandaloneXmlProfileEditor(
            profile = editingXmlProfile,
            configStorageManager = viewModel.repository.configStorageManager,
            onDismiss = { showXmlEditorDialog = false },
            onSave = { profileToSave ->
                viewModel.saveStandaloneProfile(profileToSave)
                Toast.makeText(context, "Standalone XML Profile '${profileToSave.name}' saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Display Bounds & Margin Settings Dialog
    if (showDisplaySettingsDialog) {
        DisplaySettingsDialog(
            currentSettings = displaySettings,
            bottomBarSettings = bottomBarSettings,
            onDismiss = {
                showDisplaySettingsDialog = false
                activeFocusZone = ActiveFocusZone.ROM_LIST
            },
            onSaveSettings = { updated ->
                viewModel.updateDisplaySettings(updated)
                Toast.makeText(context, "Display bounds updated!", Toast.LENGTH_SHORT).show()
            },
            onSaveBottomBarSettings = { updatedBarSettings ->
                viewModel.updateBottomBarSettings(updatedBarSettings)
            },
            onOpenGamepadSettings = {
                showGamepadSettingsDialog = true
            },
            onOpenConfigFileManager = {
                showConfigFileManagerDialog = true
            }
        )
    }

    // Gamepad Settings Dialog
    if (showGamepadSettingsDialog) {
        GamepadSettingsDialog(
            settings = gamepadSettings,
            onDismiss = {
                showGamepadSettingsDialog = false
                activeFocusZone = ActiveFocusZone.ROM_LIST
            },
            onSaveSettings = { updated ->
                viewModel.updateGamepadSettings(updated)
                Toast.makeText(context, "Gamepad button mappings saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Bottom Status Bar Settings Dialog
    if (showBottomBarSettingsDialog) {
        BottomBarSettingsDialog(
            settings = bottomBarSettings,
            onDismiss = { showBottomBarSettingsDialog = false },
            onSaveSettings = { updated ->
                viewModel.updateBottomBarSettings(updated)
                Toast.makeText(context, "Bottom status bar settings saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ROM List Style Settings Dialog
    if (showRomListStyleDialog) {
        ListStyleSettingsDialog(
            settings = romListSettings,
            onDismiss = { showRomListStyleDialog = false },
            onUpdateSettings = { viewModel.updateRomListSettings(it) },
            onOpenAppVisibility = { showAppVisibilityDialog = true },
            isAndroidAppsSystem = currentSystem?.id == "android_apps"
        )
    }

    // Accessible Config File Manager Dialog
    if (showConfigFileManagerDialog) {
        ConfigFileManagerDialog(
            configStorageManager = viewModel.repository.configStorageManager,
            onDismiss = { showConfigFileManagerDialog = false },
            onImportXmlClick = {
                showConfigFileManagerDialog = false
                editingXmlProfile = null
                showXmlEditorDialog = true
            }
        )
    }

    // Game Details / ROM Info Dialog
    showGameDetailsDialog?.let { game ->
        GameRomInfoDialog(
            game = game,
            customIcon = customIcons[game.filePath] ?: "",
            onCustomIconChange = { newIcon ->
                viewModel.saveCustomIcon(game, newIcon)
            },
            onFavoriteToggle = { g ->
                viewModel.toggleFavorite(g)
                showGameDetailsDialog = g.copy(isFavorite = !g.isFavorite)
            },
            onCompletedToggle = { g ->
                viewModel.toggleCompleted(g)
                showGameDetailsDialog = g.copy(isCompleted = !g.isCompleted)
            },
            onRenameGame = { g, newName ->
                viewModel.renameGame(g, newName)
                showGameDetailsDialog = g.copy(title = newName)
            },
            onDismiss = { showGameDetailsDialog = null }
        )
    }

    // Missing Package Alert Dialog
    missingPackageAlert?.let { alert ->
        ScaledDialog(
            onDismissRequest = { missingPackageAlert = null }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Emulator App Not Installed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "The app '${alert.appName}' (${alert.packageName}) is not installed on this device.\n\nYou can edit the system configuration to launch RetroArch or add a custom XML profile for an installed standalone app!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { missingPackageAlert = null }) {
                            Text("OK")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            missingPackageAlert = null
                            showXmlEditorDialog = true
                        }) {
                            Text("Edit XML Profile")
                        }
                    }
                }
            }
        }
    }

    // Empty Folder Alert Dialog
    showEmptyFolderAlert?.let { folderPath ->
        ScaledDialog(
            onDismissRequest = { showEmptyFolderAlert = null }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Directory is Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "No games or ROM files with allowed extensions were found in the scanned directory:\n\n$folderPath\n\nPlease check the path, put ROMs there, or edit the system's allowed extensions in System settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEmptyFolderAlert = null }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    // Scan Confirmation Dialog
    showScanConfirmation?.let { system ->
        ScaledDialog(
            onDismissRequest = { showScanConfirmation = null }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Scan Directory",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "scanning directory will take longer time depending on how many files in the directory, are you sure want to do it ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bottomBgHex = displaySettings.bottomBarColorHex
                        val textHex = displaySettings.textColorHex
                        val buttonBgColor = if (bottomBgHex.isNotBlank()) {
                            com.example.ui.theme.parseColorHex(bottomBgHex, MaterialTheme.colorScheme.primary)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        val buttonContentColor = if (textHex.isNotBlank()) {
                            com.example.ui.theme.parseColorHex(textHex, MaterialTheme.colorScheme.onPrimary)
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }

                        Button(
                            onClick = { showScanConfirmation = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBgColor,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "No",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Go back",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = buttonContentColor.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                showScanConfirmation = null
                                viewModel.rescanCurrentSystemRoms(system)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonBgColor,
                                contentColor = buttonContentColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Yeah",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Man, I wanna do it",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = buttonContentColor.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}
