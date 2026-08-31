package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SystemEntity
import com.example.data.model.TotalGamesWidgetConfig
import com.example.data.model.WidgetSubItem
import kotlinx.coroutines.launch
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.platform.LocalContext

enum class SystemDisplayStyle {
    ICON_GRID,
    GRID_ALT,
    TEXT_LIST,
    CAROUSEL
}

data class SystemGridItem(
    val system: SystemEntity?,
    val originalIndex: Int,
    val span: Int
)

@Composable
fun SystemMainMenu(
    systems: List<SystemEntity>,
    selectedSystemId: String?,
    onSelectAndEnterSystem: (SystemEntity) -> Unit,
    onActiveSystemHighlighted: (String) -> Unit = {},
    onEditSystem: (SystemEntity) -> Unit,
    onAddSystem: () -> Unit,
    systemMainMenuTitle: String = "SYSTEM MAIN MENU",
    systemMainMenuDescription: String = "Select a console / system to launch games",
    onOpenSystemManager: () -> Unit = {},
    onOpenMainSettings: () -> Unit = {},
    enableNavigationSound: Boolean = true,
    selectedSfxFileName: String = "",
    mainMenuIconGridScalePercent: Int = 100,
    mainMenuCarouselIconSizePercent: Int = 100,
    mainMenuCarouselAnimation: Boolean = true,
    mainMenuCarouselUnlimited: Boolean = false,
    showMainMenuTitle: Boolean = true,
    showEditIcon: Boolean = false,
    defaultDisplayStyle: String = "ICON_GRID",
    systemMainMenuGridStyle: String = "TOP_LEFT",
    centeredLastGridItem: Boolean = false,
    displayColumns: Int = 4,
    displayRows: Int = 4,
    actualColumns: Int = 4,
    actualRows: Int = 4,
    textSizeSp: Int = 16,
    descTextSizeSp: Int = 12,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    dpadUpTrigger: Long = 0L,
    dpadDownTrigger: Long = 0L,
    dpadLeftTrigger: Long = 0L,
    dpadRightTrigger: Long = 0L,
    crownUpTrigger: Long = 0L,
    crownDownTrigger: Long = 0L,
    pageUpTrigger: Long = 0L,
    pageDownTrigger: Long = 0L,
    goToTopTrigger: Long = 0L,
    goToBottomTrigger: Long = 0L,
    selectActionTrigger: Long = 0L,
    highlightFavoritesTrigger: Long = 0L,
    isMenuFocused: Boolean = true,
    onMoveFocusUp: () -> Unit = {},
    onMoveFocusDown: () -> Unit = {},
    onToggleDisplayStyle: ((String) -> Unit)? = null,
    autoHideScrollbar: Boolean = true,
    scrollbarShowDurationMs: Int = 1500,
    systemMainMenuIconPath: String = "gamepad",
    tileMarginLeftDp: Int = 0,
    tileMarginRightDp: Int = 0,
    totalGamesCount: Int = 0,
    totalFavoritesCount: Int = 0,
    totalCompletedCount: Int = 0,
    onDoubleTapClock: (() -> Unit)? = null,
    onToggleHeader: (() -> Unit)? = null,
    onOpenFullScreenClock: ((SystemEntity) -> Unit)? = null,
    bottomSystemMainMenuTitle: Boolean = false,
    onCarouselSwipeUpDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var displayStyle by remember(defaultDisplayStyle) {
        mutableStateOf(
            when (defaultDisplayStyle) {
                "GRID_ALT" -> SystemDisplayStyle.GRID_ALT
                "TEXT_LIST" -> SystemDisplayStyle.TEXT_LIST
                "CAROUSEL" -> SystemDisplayStyle.CAROUSEL
                else -> SystemDisplayStyle.ICON_GRID
            }
        )
    }

    // Auto-highlight last opened system or default to index 0
    var focusedIndex by remember(systems.size) {
        val idx = if (!selectedSystemId.isNullOrEmpty()) systems.indexOfFirst { it.id == selectedSystemId } else -1
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    LaunchedEffect(selectedSystemId, systems) {
        if (!selectedSystemId.isNullOrEmpty()) {
            val idx = systems.indexOfFirst { it.id == selectedSystemId }
            if (idx >= 0) {
                focusedIndex = idx
            }
        }
    }

    LaunchedEffect(focusedIndex, systems) {
        val currentSystem = systems.getOrNull(focusedIndex)
        if (currentSystem != null) {
            onActiveSystemHighlighted(currentSystem.id)
        }
    }

    // Header Focus State (0 = System Manager, 1 = Display Style Toggle, 2 = Main Settings)
    var isHeaderFocused by remember { mutableStateOf(false) }
    var headerFocusedIndex by remember { mutableIntStateOf(0) }

    var lastHandledHighlightFavorites by remember { mutableLongStateOf(highlightFavoritesTrigger) }
    LaunchedEffect(highlightFavoritesTrigger) {
        if (highlightFavoritesTrigger > 0L && highlightFavoritesTrigger != lastHandledHighlightFavorites) {
            lastHandledHighlightFavorites = highlightFavoritesTrigger
            val favIdx = systems.indexOfFirst { it.id == "favorites" }
            if (favIdx >= 0) {
                focusedIndex = favIdx
                isHeaderFocused = false
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val handleWidgetDoubleTap: (SystemEntity) -> Unit = { system ->
        val action = system.retroarchSaveDir.ifBlank { "NONE" }
        when (action) {
            "NONE" -> {
                // Do nothing
            }
            "FULLSCREEN_CLOCK" -> {
                onOpenFullScreenClock?.invoke(system)
            }
            "HIDE_SHOW_TOP_BAR" -> {
                onDoubleTapClock?.invoke()
            }
            "HIDE_SHOW_HEADER" -> {
                onToggleHeader?.invoke()
            }
            "OPEN_MAIN_SETTING" -> {
                onOpenMainSettings()
            }
            "OPEN_SYSTEM_MANAGER" -> {
                onOpenSystemManager()
            }
            "CYCLE_DISPLAY_STYLE" -> {
                val styles = listOf("ICON_GRID", "GRID_ALT", "TEXT_LIST", "CAROUSEL")
                val currentKey = when (displayStyle) {
                    SystemDisplayStyle.GRID_ALT -> "GRID_ALT"
                    SystemDisplayStyle.TEXT_LIST -> "TEXT_LIST"
                    SystemDisplayStyle.CAROUSEL -> "CAROUSEL"
                    else -> "ICON_GRID"
                }
                val idx = styles.indexOf(currentKey)
                val nextStyle = styles[(idx + 1) % styles.size]
                displayStyle = when (nextStyle) {
                    "GRID_ALT" -> SystemDisplayStyle.GRID_ALT
                    "TEXT_LIST" -> SystemDisplayStyle.TEXT_LIST
                    "CAROUSEL" -> SystemDisplayStyle.CAROUSEL
                    else -> SystemDisplayStyle.ICON_GRID
                }
                onToggleDisplayStyle?.invoke(nextStyle)
            }
            "EXPAND_NOTIFICATIONS" -> {
                if (!com.example.service.LauncherAccessibilityService.performNotifications()) {
                    try {
                        val statusBarService = context.getSystemService("statusbar")
                        val statusBarManager = Class.forName("android.app.StatusBarManager")
                        val showNotificationMethod = statusBarManager.getMethod("expandNotificationsPanel")
                        showNotificationMethod.isAccessible = true
                        showNotificationMethod.invoke(statusBarService)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "TOGGLE_WIFI" -> {
                try {
                    val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val panelIntent = android.content.Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(panelIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        if (wifiManager != null) {
                            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                        } else {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }
                    }
                } catch (e: Exception) {
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            "TOGGLE_BLUETOOTH" -> {
                try {
                    val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                    val btAdapter = bluetoothManager?.adapter ?: android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    if (btAdapter != null) {
                        if (btAdapter.isEnabled) {
                            @Suppress("DEPRECATION")
                            val success = btAdapter.disable()
                            if (!success) {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val success = btAdapter.enable()
                            if (!success) {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        }
                    } else {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                } catch (e: Exception) {
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            "SHOW_VOLUME_PANEL" -> {
                try {
                    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                    audioManager?.adjustSuggestedStreamVolume(
                        android.media.AudioManager.ADJUST_SAME,
                        android.media.AudioManager.USE_DEFAULT_STREAM_TYPE,
                        android.media.AudioManager.FLAG_SHOW_UI
                    ) ?: audioManager?.adjustVolume(
                        android.media.AudioManager.ADJUST_SAME,
                        android.media.AudioManager.FLAG_SHOW_UI
                    )
                } catch (e: Exception) {
                    try {
                        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                        audioManager?.adjustVolume(
                            android.media.AudioManager.ADJUST_SAME,
                            android.media.AudioManager.FLAG_SHOW_UI
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            "SHOW_RECENT_APPS" -> {
                com.example.service.LauncherAccessibilityService.openRecentApps(context)
            }
            "SCREEN_LOCK" -> {
                com.example.service.LauncherAccessibilityService.lockScreen(context)
            }
            else -> {
                if (action == "LAUNCH_APP" || action.startsWith("LAUNCH_APP:")) {
                    val pkg = if (action.startsWith("LAUNCH_APP:")) action.removePrefix("LAUNCH_APP:") else ""
                    if (pkg.isNotBlank()) {
                        com.example.data.launcher.IntentLauncher(context).launchAndroidApp(pkg)
                    } else if (system.defaultLaunchMode == "WIDGET_APP_LAUNCH" && system.folderPath.isNotBlank()) {
                        com.example.data.launcher.IntentLauncher(context).launchAndroidApp(system.folderPath)
                    }
                }
            }
        }
    }

    // Ensure focusedIndex is valid
    val currentFocusedSystem = systems.getOrNull(focusedIndex)

    val triggerWidgetActionIfSingleTap: (SystemEntity) -> Unit = { system ->
        val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")
        val isSingleTapEnabled = isWidget && (system.saveAspectRatio == "SINGLE_TAP" || system.saveAspectRatio == "true")
        if (isSingleTapEnabled) {
            handleWidgetDoubleTap(system)
        }
    }

    var lastHandledSelectTrigger by remember { mutableLongStateOf(selectActionTrigger) }

    LaunchedEffect(selectActionTrigger) {
        if (selectActionTrigger > 0L && selectActionTrigger != lastHandledSelectTrigger) {
            lastHandledSelectTrigger = selectActionTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            if (isHeaderFocused) {
                when (headerFocusedIndex) {
                    0 -> onOpenSystemManager()
                    1 -> {
                        displayStyle = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> SystemDisplayStyle.GRID_ALT
                            SystemDisplayStyle.GRID_ALT -> SystemDisplayStyle.TEXT_LIST
                            SystemDisplayStyle.TEXT_LIST -> SystemDisplayStyle.CAROUSEL
                            SystemDisplayStyle.CAROUSEL -> SystemDisplayStyle.ICON_GRID
                        }
                        val styleStr = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> "ICON_GRID"
                            SystemDisplayStyle.GRID_ALT -> "GRID_ALT"
                            SystemDisplayStyle.TEXT_LIST -> "TEXT_LIST"
                            SystemDisplayStyle.CAROUSEL -> "CAROUSEL"
                        }
                        onToggleDisplayStyle?.invoke(styleStr)
                    }
                }
            } else if (currentFocusedSystem != null) {
                triggerWidgetActionIfSingleTap(currentFocusedSystem)
                onSelectAndEnterSystem(currentFocusedSystem)
            }
        }
    }

    val headerContent = @Composable { paddingModifier: Modifier ->
        Row(
            modifier = paddingModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                UniversalIconView(
                    iconNameOrPath = systemMainMenuIconPath.ifEmpty { "gamepad" },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = systemMainMenuTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = systemMainMenuDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Selectable Header Buttons with increased margin
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                // 0. Emulator System Manager Button
                IconButton(
                    onClick = {
                        isHeaderFocused = true
                        headerFocusedIndex = 0
                        com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                        onOpenSystemManager()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 0) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 0) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Emulator System Manager",
                        tint = if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 1. Grid / List Layout Switcher
                IconButton(
                    onClick = {
                        isHeaderFocused = true
                        headerFocusedIndex = 1
                        displayStyle = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> SystemDisplayStyle.GRID_ALT
                            SystemDisplayStyle.GRID_ALT -> SystemDisplayStyle.TEXT_LIST
                            SystemDisplayStyle.TEXT_LIST -> SystemDisplayStyle.CAROUSEL
                            SystemDisplayStyle.CAROUSEL -> SystemDisplayStyle.ICON_GRID
                        }
                        val styleStr = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> "ICON_GRID"
                            SystemDisplayStyle.GRID_ALT -> "GRID_ALT"
                            SystemDisplayStyle.TEXT_LIST -> "TEXT_LIST"
                            SystemDisplayStyle.CAROUSEL -> "CAROUSEL"
                        }
                        onToggleDisplayStyle?.invoke(styleStr)
                        com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 1) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 1) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> Icons.Default.GridView
                            SystemDisplayStyle.GRID_ALT -> Icons.Default.Apps
                            SystemDisplayStyle.TEXT_LIST -> Icons.Default.ViewList
                            SystemDisplayStyle.CAROUSEL -> Icons.Default.ViewCarousel
                        },
                        contentDescription = "Toggle Grid / List / Carousel Mode",
                        tint = if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (showMainMenuTitle && !bottomSystemMainMenuTitle) {
            headerContent(Modifier.fillMaxWidth().padding(bottom = 12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // System Display Content based on chosen style
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val spacing = 12.dp

            var lastHandledLeft by remember { mutableLongStateOf(dpadLeftTrigger) }
            var lastHandledRight by remember { mutableLongStateOf(dpadRightTrigger) }
            var lastHandledUp by remember { mutableLongStateOf(dpadUpTrigger) }
            var lastHandledDown by remember { mutableLongStateOf(dpadDownTrigger) }
            var lastHandledCrownUp by remember { mutableLongStateOf(crownUpTrigger) }
            var lastHandledCrownDown by remember { mutableLongStateOf(crownDownTrigger) }
            var lastHandledPageUp by remember { mutableLongStateOf(pageUpTrigger) }
            var lastHandledPageDown by remember { mutableLongStateOf(pageDownTrigger) }
            var lastHandledGoToTop by remember { mutableLongStateOf(goToTopTrigger) }
            var lastHandledGoToBottom by remember { mutableLongStateOf(goToBottomTrigger) }

            when (displayStyle) {
                SystemDisplayStyle.ICON_GRID, SystemDisplayStyle.GRID_ALT -> {
                    val iconScaleFactor = (mainMenuIconGridScalePercent / 100f).coerceIn(0.5f, 1.5f)
                    val numCols = maxOf(1, (maxWidth / (140.dp * iconScaleFactor)).toInt())
                    val gridItems = remember(systems, numCols, centeredLastGridItem) {
                        val result = mutableListOf<SystemGridItem>()
                        val chunked = systems.mapIndexed { index, system -> Pair(system, index) }.chunked(numCols)
                        chunked.forEachIndexed { rowIndex, rowItems ->
                            val isLastRow = rowIndex == chunked.lastIndex
                            if (isLastRow && centeredLastGridItem && rowItems.size < numCols) {
                                val dummySpan = numCols - rowItems.size
                                if (dummySpan > 0) {
                                    result.add(SystemGridItem(null, -1, dummySpan))
                                }
                                rowItems.forEach { (system, index) ->
                                    result.add(SystemGridItem(system, index, 2))
                                }
                                if (dummySpan > 0) {
                                    result.add(SystemGridItem(null, -1, dummySpan))
                                }
                            } else {
                                rowItems.forEach { (system, index) ->
                                    result.add(SystemGridItem(system, index, 2))
                                }
                            }
                        }
                        result
                    }

                    fun getGridIndex(origIdx: Int): Int {
                        val gIdx = gridItems.indexOfFirst { it.originalIndex == origIdx }
                        return if (gIdx >= 0) gIdx else origIdx
                    }

                    val gridState = remember(systems.size, selectedSystemId) {
                        val initialIdx = systems.indexOfFirst { it.id == selectedSystemId }
                        val safeIdx = if (initialIdx >= 0) initialIdx else 0
                        val gridIdx = gridItems.indexOfFirst { it.originalIndex == safeIdx }.coerceAtLeast(0)
                        LazyGridState(firstVisibleItemIndex = maxOf(0, gridIdx - numCols))
                    }

                    fun scrollToCenter(index: Int) {
                        coroutineScope.launch {
                            val gridIndex = getGridIndex(index)
                            val viewportHeight = gridState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            gridState.animateScrollToItem(gridIndex, scrollOffset = offset)
                        }
                    }

                    LaunchedEffect(gridState, focusedIndex) {
                        if (focusedIndex >= 0 && focusedIndex < systems.size) {
                            val gridIndex = getGridIndex(focusedIndex)
                            gridState.scrollToItem(gridIndex)
                            val viewportHeight = gridState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            gridState.animateScrollToItem(gridIndex, scrollOffset = offset)
                        }
                    }

                    val isBottomStart = systemMainMenuGridStyle == "BOTTOM_LEFT" || systemMainMenuGridStyle == "BOTTOM_RIGHT"
                    val isRtl = systemMainMenuGridStyle == "TOP_RIGHT" || systemMainMenuGridStyle == "BOTTOM_RIGHT"

                    LaunchedEffect(pageUpTrigger) {
                        if (pageUpTrigger > 0L && pageUpTrigger != lastHandledPageUp && systems.isNotEmpty()) {
                            lastHandledPageUp = pageUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = if (isBottomStart) {
                                minOf(systems.size - 1, focusedIndex + (numCols * 2))
                            } else {
                                maxOf(0, focusedIndex - (numCols * 2))
                            }
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(pageDownTrigger) {
                        if (pageDownTrigger > 0L && pageDownTrigger != lastHandledPageDown && systems.isNotEmpty()) {
                            lastHandledPageDown = pageDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = if (isBottomStart) {
                                maxOf(0, focusedIndex - (numCols * 2))
                            } else {
                                minOf(systems.size - 1, focusedIndex + (numCols * 2))
                            }
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToTopTrigger) {
                        if (goToTopTrigger > 0L && goToTopTrigger != lastHandledGoToTop && systems.isNotEmpty()) {
                            lastHandledGoToTop = goToTopTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = if (isBottomStart) systems.size - 1 else 0
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToBottomTrigger) {
                        if (goToBottomTrigger > 0L && goToBottomTrigger != lastHandledGoToBottom && systems.isNotEmpty()) {
                            lastHandledGoToBottom = goToBottomTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = if (isBottomStart) 0 else maxOf(0, systems.size - 1)
                            scrollToCenter(focusedIndex)
                        }
                    }

                    LaunchedEffect(dpadLeftTrigger) {
                        if (dpadLeftTrigger > 0L && dpadLeftTrigger != lastHandledLeft && systems.isNotEmpty()) {
                            lastHandledLeft = dpadLeftTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = maxOf(0, headerFocusedIndex - 1)
                            } else {
                                if (isRtl) {
                                    if (focusedIndex + 1 < systems.size) {
                                        focusedIndex += 1
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        focusedIndex = 0
                                        scrollToCenter(focusedIndex)
                                    }
                                } else {
                                    if (focusedIndex > 0) {
                                        focusedIndex -= 1
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        focusedIndex = systems.size - 1
                                        scrollToCenter(focusedIndex)
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(dpadRightTrigger) {
                        if (dpadRightTrigger > 0L && dpadRightTrigger != lastHandledRight && systems.isNotEmpty()) {
                            lastHandledRight = dpadRightTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = minOf(1, headerFocusedIndex + 1)
                            } else {
                                if (isRtl) {
                                    if (focusedIndex > 0) {
                                        focusedIndex -= 1
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        focusedIndex = systems.size - 1
                                        scrollToCenter(focusedIndex)
                                    }
                                } else {
                                    if (focusedIndex + 1 < systems.size) {
                                        focusedIndex += 1
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        focusedIndex = 0
                                        scrollToCenter(focusedIndex)
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(dpadUpTrigger) {
                        if (dpadUpTrigger > 0L && dpadUpTrigger != lastHandledUp && systems.isNotEmpty()) {
                            lastHandledUp = dpadUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                onMoveFocusUp()
                            } else {
                                if (isBottomStart) {
                                    if (focusedIndex + numCols < systems.size) {
                                        focusedIndex += numCols
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        if (showMainMenuTitle) {
                                            isHeaderFocused = true
                                            headerFocusedIndex = 0
                                        } else {
                                            onMoveFocusUp()
                                        }
                                    }
                                } else {
                                    if (showMainMenuTitle && focusedIndex < numCols) {
                                        isHeaderFocused = true
                                        headerFocusedIndex = 0
                                    } else if (!showMainMenuTitle && focusedIndex < numCols) {
                                        onMoveFocusUp()
                                    } else if (focusedIndex >= numCols) {
                                        focusedIndex -= numCols
                                        scrollToCenter(focusedIndex)
                                    }
                                }
                            }
                        }
                    }
                    LaunchedEffect(dpadDownTrigger) {
                        if (dpadDownTrigger > 0L && dpadDownTrigger != lastHandledDown && systems.isNotEmpty()) {
                            lastHandledDown = dpadDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                if (isBottomStart) {
                                    isHeaderFocused = false
                                    focusedIndex = systems.size - 1
                                    scrollToCenter(focusedIndex)
                                } else {
                                    isHeaderFocused = false
                                }
                            } else {
                                if (isBottomStart) {
                                    if (focusedIndex >= numCols) {
                                        focusedIndex -= numCols
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        onMoveFocusDown()
                                    }
                                } else {
                                    if (focusedIndex + numCols < systems.size) {
                                        focusedIndex += numCols
                                        scrollToCenter(focusedIndex)
                                    } else if (focusedIndex < systems.size - 1) {
                                        focusedIndex = systems.size - 1
                                        scrollToCenter(focusedIndex)
                                    } else {
                                        onMoveFocusDown()
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(crownUpTrigger) {
                        if (crownUpTrigger > 0L && crownUpTrigger != lastHandledCrownUp && systems.isNotEmpty()) {
                            lastHandledCrownUp = crownUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (isBottomStart) {
                                if (focusedIndex + numCols < systems.size) {
                                    focusedIndex += numCols
                                    scrollToCenter(focusedIndex)
                                } else if (focusedIndex < systems.size - 1) {
                                    focusedIndex = systems.size - 1
                                    scrollToCenter(focusedIndex)
                                }
                            } else {
                                if (focusedIndex >= numCols) {
                                    focusedIndex -= numCols
                                    scrollToCenter(focusedIndex)
                                } else if (focusedIndex > 0) {
                                    focusedIndex = 0
                                    scrollToCenter(focusedIndex)
                                }
                            }
                        }
                    }

                    LaunchedEffect(crownDownTrigger) {
                        if (crownDownTrigger > 0L && crownDownTrigger != lastHandledCrownDown && systems.isNotEmpty()) {
                            lastHandledCrownDown = crownDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (isBottomStart) {
                                if (focusedIndex >= numCols) {
                                    focusedIndex -= numCols
                                    scrollToCenter(focusedIndex)
                                } else if (focusedIndex > 0) {
                                    focusedIndex = 0
                                    scrollToCenter(focusedIndex)
                                }
                            } else {
                                if (focusedIndex + numCols < systems.size) {
                                    focusedIndex += numCols
                                    scrollToCenter(focusedIndex)
                                } else if (focusedIndex < systems.size - 1) {
                                    focusedIndex = systems.size - 1
                                    scrollToCenter(focusedIndex)
                                }
                            }
                        }
                    }

                    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    val totalColumns = numCols * 2

                    CompositionLocalProvider(
                        LocalLayoutDirection provides layoutDirection
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(totalColumns),
                            state = gridState,
                            reverseLayout = isBottomStart,
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = if (isBottomStart) {
                                Arrangement.spacedBy(spacing, Alignment.Bottom)
                            } else {
                                Arrangement.spacedBy(spacing, Alignment.Top)
                            },
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .lazyGridScrollbar(gridState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                        ) {
                            itemsIndexed(
                                gridItems,
                                key = { gIndex, item -> item.system?.id ?: "dummy_$gIndex" },
                                span = { _, item -> GridItemSpan(item.span) }
                            ) { gIndex, item ->
                                val system = item.system
                                val index = item.originalIndex
                                if (system != null && index >= 0) {
                                    val isFocused = isMenuFocused && !isHeaderFocused && index == focusedIndex
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides LayoutDirection.Ltr
                                    ) {
                                        if (displayStyle == SystemDisplayStyle.GRID_ALT) {
                                            SystemGridCardAlt(
                                                system = system,
                                                isFocused = isFocused,
                                                showEditIcon = showEditIcon,
                                                textSizeSp = textSizeSp,
                                                descTextSizeSp = descTextSizeSp,
                                                marqueeSpeed = marqueeSpeed,
                                                marqueeDelayMillis = marqueeDelayMillis,
                                                totalGamesCount = totalGamesCount,
                                                totalFavoritesCount = totalFavoritesCount,
                                                totalCompletedCount = totalCompletedCount,
                                                onWidgetDoubleTap = { handleWidgetDoubleTap(system) },
                                                modifier = Modifier
                                                    .padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp)
                                                    .fillMaxWidth()
                                                    .height(130.dp * iconScaleFactor),
                                                onClick = {
                                                    isHeaderFocused = false
                                                    focusedIndex = index
                                                    triggerWidgetActionIfSingleTap(system)
                                                    onSelectAndEnterSystem(system)
                                                },
                                                onLongClick = {
                                                    isHeaderFocused = false
                                                    focusedIndex = index
                                                },
                                                onEdit = { onEditSystem(system) }
                                            )
                                        } else {
                                            SystemGridCard(
                                                system = system,
                                                isFocused = isFocused,
                                                showEditIcon = showEditIcon,
                                                textSizeSp = textSizeSp,
                                                descTextSizeSp = descTextSizeSp,
                                                textAlignment = textAlignment,
                                                marqueeSpeed = marqueeSpeed,
                                                marqueeDelayMillis = marqueeDelayMillis,
                                                totalGamesCount = totalGamesCount,
                                                totalFavoritesCount = totalFavoritesCount,
                                                totalCompletedCount = totalCompletedCount,
                                                onWidgetDoubleTap = { handleWidgetDoubleTap(system) },
                                                modifier = Modifier
                                                    .padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp)
                                                    .fillMaxWidth()
                                                    .height(110.dp * iconScaleFactor),
                                                onClick = {
                                                    isHeaderFocused = false
                                                    focusedIndex = index
                                                    triggerWidgetActionIfSingleTap(system)
                                                    onSelectAndEnterSystem(system)
                                                },
                                                onLongClick = {
                                                    isHeaderFocused = false
                                                    focusedIndex = index
                                                },
                                                onEdit = { onEditSystem(system) }
                                            )
                                        }
                                    }
                                } else {
                                    val dummyHeight = if (displayStyle == SystemDisplayStyle.GRID_ALT) {
                                        130.dp * iconScaleFactor
                                    } else {
                                        110.dp * iconScaleFactor
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(dummyHeight))
                                }
                            }
                        }
                    }
                }
                SystemDisplayStyle.TEXT_LIST -> {
                    val listState = remember(systems.size, selectedSystemId) {
                        val initialIdx = systems.indexOfFirst { it.id == selectedSystemId }
                        val safeIdx = if (initialIdx >= 0) initialIdx else 0
                        LazyListState(firstVisibleItemIndex = maxOf(0, safeIdx - 2))
                    }

                    fun scrollListToCenter(index: Int) {
                        coroutineScope.launch {
                            val viewportHeight = listState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            listState.animateScrollToItem(index, scrollOffset = offset)
                        }
                    }

                    LaunchedEffect(listState, focusedIndex) {
                        if (focusedIndex >= 0 && focusedIndex < systems.size) {
                            listState.scrollToItem(focusedIndex)
                            val viewportHeight = listState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            listState.animateScrollToItem(focusedIndex, scrollOffset = offset)
                        }
                    }

                    LaunchedEffect(pageUpTrigger) {
                        if (pageUpTrigger > 0L && pageUpTrigger != lastHandledPageUp && systems.isNotEmpty()) {
                            lastHandledPageUp = pageUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = maxOf(0, focusedIndex - 5)
                            scrollListToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(pageDownTrigger) {
                        if (pageDownTrigger > 0L && pageDownTrigger != lastHandledPageDown && systems.isNotEmpty()) {
                            lastHandledPageDown = pageDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = minOf(systems.size - 1, focusedIndex + 5)
                            scrollListToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToTopTrigger) {
                        if (goToTopTrigger > 0L && goToTopTrigger != lastHandledGoToTop && systems.isNotEmpty()) {
                            lastHandledGoToTop = goToTopTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = 0
                            scrollListToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToBottomTrigger) {
                        if (goToBottomTrigger > 0L && goToBottomTrigger != lastHandledGoToBottom && systems.isNotEmpty()) {
                            lastHandledGoToBottom = goToBottomTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = maxOf(0, systems.size - 1)
                            scrollListToCenter(focusedIndex)
                        }
                    }

                    LaunchedEffect(dpadLeftTrigger) {
                        if (dpadLeftTrigger > 0L && dpadLeftTrigger != lastHandledLeft && systems.isNotEmpty()) {
                            lastHandledLeft = dpadLeftTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = maxOf(0, headerFocusedIndex - 1)
                            } else {
                                if (focusedIndex > 0) {
                                    focusedIndex -= 1
                                    scrollListToCenter(focusedIndex)
                                } else {
                                    focusedIndex = systems.size - 1
                                    scrollListToCenter(focusedIndex)
                                }
                            }
                        }
                    }
                    LaunchedEffect(dpadRightTrigger) {
                        if (dpadRightTrigger > 0L && dpadRightTrigger != lastHandledRight && systems.isNotEmpty()) {
                            lastHandledRight = dpadRightTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = minOf(1, headerFocusedIndex + 1)
                            } else {
                                if (focusedIndex + 1 < systems.size) {
                                    focusedIndex += 1
                                    scrollListToCenter(focusedIndex)
                                } else {
                                    focusedIndex = 0
                                    scrollListToCenter(focusedIndex)
                                }
                            }
                        }
                    }
                    LaunchedEffect(dpadUpTrigger) {
                        if (dpadUpTrigger > 0L && dpadUpTrigger != lastHandledUp && systems.isNotEmpty()) {
                            lastHandledUp = dpadUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                onMoveFocusUp()
                            } else if (showMainMenuTitle && focusedIndex == 0) {
                                isHeaderFocused = true
                                headerFocusedIndex = 0
                            } else if (!showMainMenuTitle && focusedIndex == 0) {
                                onMoveFocusUp()
                            } else if (focusedIndex > 0) {
                                focusedIndex -= 1
                                scrollListToCenter(focusedIndex)
                            }
                        }
                    }
                    LaunchedEffect(dpadDownTrigger) {
                        if (dpadDownTrigger > 0L && dpadDownTrigger != lastHandledDown && systems.isNotEmpty()) {
                            lastHandledDown = dpadDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                isHeaderFocused = false
                            } else if (focusedIndex + 1 < systems.size) {
                                focusedIndex += 1
                                scrollListToCenter(focusedIndex)
                            } else {
                                onMoveFocusDown()
                            }
                        }
                    }

                    LaunchedEffect(crownUpTrigger) {
                        if (crownUpTrigger > 0L && crownUpTrigger != lastHandledCrownUp && systems.isNotEmpty()) {
                            lastHandledCrownUp = crownUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (focusedIndex > 0) {
                                focusedIndex -= 1
                                scrollListToCenter(focusedIndex)
                            }
                        }
                    }

                    LaunchedEffect(crownDownTrigger) {
                        if (crownDownTrigger > 0L && crownDownTrigger != lastHandledCrownDown && systems.isNotEmpty()) {
                            lastHandledCrownDown = crownDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (focusedIndex + 1 < systems.size) {
                                focusedIndex += 1
                                scrollListToCenter(focusedIndex)
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyListScrollbar(listState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                    ) {
                        itemsIndexed(systems, key = { _, s -> s.id }) { index, system ->
                            val isFocused = isMenuFocused && !isHeaderFocused && index == focusedIndex
                            SystemTextRow(
                                system = system,
                                isFocused = isFocused,
                                showEditIcon = showEditIcon,
                                textSizeSp = textSizeSp,
                                descTextSizeSp = descTextSizeSp,
                                textAlignment = textAlignment,
                                marqueeSpeed = marqueeSpeed,
                                marqueeDelayMillis = marqueeDelayMillis,
                                totalGamesCount = totalGamesCount,
                                totalFavoritesCount = totalFavoritesCount,
                                totalCompletedCount = totalCompletedCount,
                                modifier = Modifier.padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp),
                                onWidgetDoubleTap = { handleWidgetDoubleTap(system) },
                                onClick = {
                                    isHeaderFocused = false
                                    focusedIndex = index
                                    triggerWidgetActionIfSingleTap(system)
                                    onSelectAndEnterSystem(system)
                                },
                                onLongClick = {
                                    isHeaderFocused = false
                                    focusedIndex = index
                                },
                                onEdit = { onEditSystem(system) }
                            )
                        }
                    }
                }
                SystemDisplayStyle.CAROUSEL -> {
                    fun getRealIndex(virtualIdx: Int): Int {
                        if (systems.isEmpty()) return 0
                        return if (mainMenuCarouselUnlimited) {
                            val rem = virtualIdx % systems.size
                            if (rem < 0) rem + systems.size else rem
                        } else {
                            virtualIdx.coerceIn(systems.indices)
                        }
                    }

                    val carouselState = remember(systems.size, selectedSystemId, mainMenuCarouselUnlimited) {
                        val initialIdx = systems.indexOfFirst { it.id == selectedSystemId }
                        val safeIdx = if (initialIdx >= 0) initialIdx else 0
                        val startIdx = if (mainMenuCarouselUnlimited && systems.isNotEmpty()) 50_000 - (50_000 % systems.size) + safeIdx else safeIdx
                        LazyListState(firstVisibleItemIndex = startIdx, firstVisibleItemScrollOffset = 0)
                    }

                    val initialIdx = systems.indexOfFirst { it.id == selectedSystemId }
                    val safeIdx = if (initialIdx >= 0) initialIdx else 0
                    val startIdx = if (mainMenuCarouselUnlimited && systems.isNotEmpty()) 50_000 - (50_000 % systems.size) + safeIdx else safeIdx

                    var virtualFocusedIndex by remember(systems.size, selectedSystemId, mainMenuCarouselUnlimited) {
                        mutableIntStateOf(startIdx)
                    }

                    var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                    fun scrollCarouselToCenter(index: Int) {
                        if (systems.isNotEmpty()) {
                            val maxCount = if (mainMenuCarouselUnlimited) 100_000 else systems.size
                            if (index in 0 until maxCount) {
                                scrollJob?.cancel()
                                scrollJob = coroutineScope.launch {
                                    carouselState.animateScrollToItem(index, scrollOffset = 0)
                                }
                            }
                        }
                    }

                    // Auto-select center item when scrolling (touch / fling)
                    val isScrollInProgress = carouselState.isScrollInProgress
                    LaunchedEffect(isScrollInProgress) {
                        if (!isScrollInProgress && systems.isNotEmpty() && !isHeaderFocused) {
                            val viewportCenter = (carouselState.layoutInfo.viewportStartOffset + carouselState.layoutInfo.viewportEndOffset) / 2
                            val centerItem = carouselState.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                                val itemCenter = item.offset + item.size / 2
                                kotlin.math.abs(itemCenter - viewportCenter)
                            }
                            if (centerItem != null) {
                                val maxVirtualCount = if (mainMenuCarouselUnlimited) 100_000 else systems.size
                                if (centerItem.index in 0 until maxVirtualCount && virtualFocusedIndex != centerItem.index) {
                                    virtualFocusedIndex = centerItem.index
                                    focusedIndex = getRealIndex(centerItem.index)
                                }
                            }
                        }
                    }

                    // When focusedIndex changes via D-Pad or logic, ensure it's centered
                    LaunchedEffect(focusedIndex) {
                        if (systems.isNotEmpty()) {
                            val currentReal = getRealIndex(virtualFocusedIndex)
                            if (currentReal != focusedIndex) {
                                // Adjust virtual index to match the new focusedIndex
                                if (mainMenuCarouselUnlimited) {
                                    val diff = focusedIndex - currentReal
                                    val halfSize = systems.size / 2
                                    var adjustedDiff = diff
                                    if (adjustedDiff > halfSize) {
                                        adjustedDiff -= systems.size
                                    } else if (adjustedDiff < -halfSize) {
                                        adjustedDiff += systems.size
                                    }
                                    virtualFocusedIndex = (virtualFocusedIndex + adjustedDiff).coerceIn(0, 100_000 - 1)
                                } else {
                                    virtualFocusedIndex = focusedIndex
                                }
                            }
                            if (!isScrollInProgress) {
                                scrollCarouselToCenter(virtualFocusedIndex)
                            }
                        }
                    }

                    LaunchedEffect(goToTopTrigger) {
                        if (goToTopTrigger > 0L && goToTopTrigger != lastHandledGoToTop && systems.isNotEmpty()) {
                            lastHandledGoToTop = goToTopTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            virtualFocusedIndex = if (mainMenuCarouselUnlimited) {
                                50_000 - (50_000 % systems.size)
                            } else {
                                0
                            }
                            focusedIndex = 0
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    LaunchedEffect(goToBottomTrigger) {
                        if (goToBottomTrigger > 0L && goToBottomTrigger != lastHandledGoToBottom && systems.isNotEmpty()) {
                            lastHandledGoToBottom = goToBottomTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            virtualFocusedIndex = if (mainMenuCarouselUnlimited) {
                                50_000 - (50_000 % systems.size) + (systems.size - 1)
                            } else {
                                systems.size - 1
                            }
                            focusedIndex = systems.size - 1
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    LaunchedEffect(pageUpTrigger) {
                        if (pageUpTrigger > 0L && pageUpTrigger != lastHandledPageUp && systems.isNotEmpty()) {
                            lastHandledPageUp = pageUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            val step = 5
                            if (mainMenuCarouselUnlimited) {
                                virtualFocusedIndex = (virtualFocusedIndex - step + 100_000) % 100_000
                            } else {
                                virtualFocusedIndex = maxOf(0, virtualFocusedIndex - step)
                            }
                            focusedIndex = getRealIndex(virtualFocusedIndex)
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    LaunchedEffect(pageDownTrigger) {
                        if (pageDownTrigger > 0L && pageDownTrigger != lastHandledPageDown && systems.isNotEmpty()) {
                            lastHandledPageDown = pageDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            val step = 5
                            if (mainMenuCarouselUnlimited) {
                                virtualFocusedIndex = (virtualFocusedIndex + step) % 100_000
                            } else {
                                virtualFocusedIndex = minOf(systems.size - 1, virtualFocusedIndex + step)
                            }
                            focusedIndex = getRealIndex(virtualFocusedIndex)
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    LaunchedEffect(dpadLeftTrigger) {
                        if (dpadLeftTrigger > 0L && dpadLeftTrigger != lastHandledLeft && systems.isNotEmpty()) {
                            lastHandledLeft = dpadLeftTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = maxOf(0, headerFocusedIndex - 1)
                            } else {
                                if (mainMenuCarouselUnlimited) {
                                    virtualFocusedIndex = (virtualFocusedIndex - 1 + 100_000) % 100_000
                                } else {
                                    if (virtualFocusedIndex > 0) {
                                        virtualFocusedIndex -= 1
                                    } else {
                                        virtualFocusedIndex = systems.size - 1
                                    }
                                }
                                focusedIndex = getRealIndex(virtualFocusedIndex)
                                scrollCarouselToCenter(virtualFocusedIndex)
                            }
                        }
                    }
                    LaunchedEffect(dpadRightTrigger) {
                        if (dpadRightTrigger > 0L && dpadRightTrigger != lastHandledRight && systems.isNotEmpty()) {
                            lastHandledRight = dpadRightTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = minOf(1, headerFocusedIndex + 1)
                            } else {
                                if (mainMenuCarouselUnlimited) {
                                    virtualFocusedIndex = (virtualFocusedIndex + 1) % 100_000
                                } else {
                                    if (virtualFocusedIndex + 1 < systems.size) {
                                        virtualFocusedIndex += 1
                                    } else {
                                        virtualFocusedIndex = 0
                                    }
                                }
                                focusedIndex = getRealIndex(virtualFocusedIndex)
                                scrollCarouselToCenter(virtualFocusedIndex)
                            }
                        }
                    }
                    LaunchedEffect(dpadUpTrigger) {
                        if (dpadUpTrigger > 0L && dpadUpTrigger != lastHandledUp && systems.isNotEmpty()) {
                            lastHandledUp = dpadUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                onMoveFocusUp()
                            } else if (showMainMenuTitle) {
                                isHeaderFocused = true
                                headerFocusedIndex = 0
                            } else {
                                onMoveFocusUp()
                            }
                        }
                    }
                    LaunchedEffect(dpadDownTrigger) {
                        if (dpadDownTrigger > 0L && dpadDownTrigger != lastHandledDown && systems.isNotEmpty()) {
                            lastHandledDown = dpadDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                isHeaderFocused = false
                            } else {
                                onMoveFocusDown()
                            }
                        }
                    }

                    LaunchedEffect(crownUpTrigger) {
                        if (crownUpTrigger > 0L && crownUpTrigger != lastHandledCrownUp && systems.isNotEmpty()) {
                            lastHandledCrownUp = crownUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (mainMenuCarouselUnlimited) {
                                virtualFocusedIndex = (virtualFocusedIndex - 1 + 100_000) % 100_000
                            } else {
                                if (virtualFocusedIndex > 0) {
                                    virtualFocusedIndex -= 1
                                } else {
                                    virtualFocusedIndex = systems.size - 1
                                }
                            }
                            focusedIndex = getRealIndex(virtualFocusedIndex)
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    LaunchedEffect(crownDownTrigger) {
                        if (crownDownTrigger > 0L && crownDownTrigger != lastHandledCrownDown && systems.isNotEmpty()) {
                            lastHandledCrownDown = crownDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            isHeaderFocused = false
                            if (mainMenuCarouselUnlimited) {
                                virtualFocusedIndex = (virtualFocusedIndex + 1) % 100_000
                            } else {
                                if (virtualFocusedIndex + 1 < systems.size) {
                                    virtualFocusedIndex += 1
                                } else {
                                    virtualFocusedIndex = 0
                                }
                            }
                            focusedIndex = getRealIndex(virtualFocusedIndex)
                            scrollCarouselToCenter(virtualFocusedIndex)
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                var totalDragY = 0f
                                var hasTriggered = false
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        totalDragY = 0f
                                        hasTriggered = false
                                    },
                                    onDragEnd = {},
                                    onDragCancel = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragY += dragAmount
                                        if (!hasTriggered && kotlin.math.abs(totalDragY) > 50f) {
                                            hasTriggered = true
                                            if (systems.isNotEmpty()) {
                                                if (mainMenuCarouselUnlimited) {
                                                    val currentReal = getRealIndex(virtualFocusedIndex)
                                                    val diff = 0 - currentReal
                                                    val halfSize = systems.size / 2
                                                    var adjustedDiff = diff
                                                    if (adjustedDiff > halfSize) {
                                                        adjustedDiff -= systems.size
                                                    } else if (adjustedDiff < -halfSize) {
                                                        adjustedDiff += systems.size
                                                    }
                                                    virtualFocusedIndex = (virtualFocusedIndex + adjustedDiff).coerceIn(0, 100_000 - 1)
                                                } else {
                                                    virtualFocusedIndex = 0
                                                }
                                                focusedIndex = 0
                                                isHeaderFocused = false
                                                scrollCarouselToCenter(virtualFocusedIndex)
                                                onCarouselSwipeUpDown()
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val screenWidth = maxWidth
                        val itemWidth = 220.dp
                        val centerHorizontalPadding = if (screenWidth > itemWidth) (screenWidth - itemWidth) / 2 else 24.dp

                        val itemCount = if (mainMenuCarouselUnlimited && systems.isNotEmpty()) 100_000 else systems.size

                        LazyRow(
                            state = carouselState,
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = centerHorizontalPadding, vertical = 24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(itemCount, key = { index -> index }) { index ->
                                val realIndex = getRealIndex(index)
                                val system = systems[realIndex]
                                val isFocused = isMenuFocused && !isHeaderFocused && index == virtualFocusedIndex
                                val cardScale by animateFloatAsState(
                                    targetValue = if (isFocused && mainMenuCarouselAnimation) 1.20f else if (mainMenuCarouselAnimation) 0.88f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                    label = "carouselScale"
                                )
                                val cardAlpha by animateFloatAsState(
                                    targetValue = if (isFocused || !mainMenuCarouselAnimation) 1.0f else 0.70f,
                                    label = "carouselAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = cardScale
                                            scaleY = cardScale
                                            alpha = cardAlpha
                                        }
                                        .width(220.dp)
                                        .height(180.dp)
                                ) {
                                    SystemCarouselCard(
                                        system = system,
                                        isFocused = isFocused,
                                        showEditIcon = showEditIcon,
                                        textSizeSp = textSizeSp,
                                        descTextSizeSp = descTextSizeSp,
                                        iconScalePercent = mainMenuCarouselIconSizePercent,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        totalGamesCount = totalGamesCount,
                                        totalFavoritesCount = totalFavoritesCount,
                                        totalCompletedCount = totalCompletedCount,
                                        onWidgetDoubleTap = { handleWidgetDoubleTap(system) },
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = {
                                            isHeaderFocused = false
                                            if (virtualFocusedIndex == index) {
                                                triggerWidgetActionIfSingleTap(system)
                                                onSelectAndEnterSystem(system)
                                            } else {
                                                virtualFocusedIndex = index
                                                focusedIndex = realIndex
                                                scrollCarouselToCenter(index)
                                            }
                                        },
                                        onLongClick = {
                                            isHeaderFocused = false
                                            virtualFocusedIndex = index
                                            focusedIndex = realIndex
                                            scrollCarouselToCenter(index)
                                        },
                                        onEdit = { onEditSystem(system) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showMainMenuTitle && bottomSystemMainMenuTitle) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            headerContent(Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    }
}

@Composable
fun ClockWidgetContent(
    system: SystemEntity,
    isFocused: Boolean,
    style: String,
    onWidgetDoubleTap: (() -> Unit)? = null,
    descTextSizeSp: Int = 12,
    textSizeSp: Int = 16,
    iconScalePercent: Int = 100,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val isClassicClock = system.folderPath.startsWith("CLASSIC")
    val is24Hour = system.folderPath.contains("24H") || system.allowedExtensions == "24h" || system.allowedExtensions == "HH:mm:ss" || system.allowedExtensions == "HH:mm"
    val showSeconds = !system.folderPath.contains("NOSEC") && !system.folderPath.contains("NO_SEC")
    val isSingleTapEnabled = system.saveAspectRatio == "SINGLE_TAP" || system.saveAspectRatio == "true"

    if (isClassicClock) {
        val clockModifier = Modifier
            .fillMaxSize()
            .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                detectTapGestures(
                    onTap = {
                        if (isSingleTapEnabled) {
                            onWidgetDoubleTap?.invoke()
                        }
                    },
                    onDoubleTap = {
                        onWidgetDoubleTap?.invoke()
                    }
                )
            }

        when (style) {
            "GRID" -> {
                Box(
                    modifier = clockModifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SevenSegmentClock(
                        is24Hour = is24Hour,
                        color = parseColor,
                        digitHeight = (textSizeSp * 1.8f).coerceIn(24f, 48f).dp,
                        showSeconds = showSeconds
                    )
                }
            }
            "GRID_ALT" -> {
                Box(
                    modifier = clockModifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SevenSegmentClock(
                        is24Hour = is24Hour,
                        color = parseColor,
                        digitHeight = (textSizeSp * 1.8f).coerceIn(24f, 48f).dp,
                        showSeconds = showSeconds
                    )
                }
            }
            "CAROUSEL" -> {
                val baseDigitHeight = if (isFocused) 46.dp else 34.dp
                val scaledHeight = baseDigitHeight * (iconScalePercent / 100f)
                Box(
                    modifier = clockModifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SevenSegmentClock(
                        is24Hour = is24Hour,
                        color = parseColor,
                        digitHeight = scaledHeight,
                        showSeconds = showSeconds
                    )
                }
            }
            else -> {
                Box(
                    modifier = clockModifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SevenSegmentClock(
                        is24Hour = is24Hour,
                        color = parseColor,
                        digitHeight = (textSizeSp * 1.6f).coerceIn(20f, 40f).dp,
                        showSeconds = showSeconds
                    )
                }
            }
        }
        return
    }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    val timePattern = remember(system.allowedExtensions) {
        when (system.allowedExtensions) {
            "24h" -> "HH:mm:ss"
            "12h", "" -> "hh:mm:ss a"
            else -> system.allowedExtensions.ifBlank { "hh:mm:ss a" }
        }
    }

    val datePattern = remember(system.retroArchCore) {
        system.retroArchCore.ifBlank { "EEE, MMM dd, yyyy" }
    }

    LaunchedEffect(timePattern, datePattern) {
        while (true) {
            val now = java.util.Calendar.getInstance()
            val sdfTime = runCatching { java.text.SimpleDateFormat(timePattern, java.util.Locale.getDefault()) }
                .getOrElse { java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()) }
            val sdfDate = runCatching { java.text.SimpleDateFormat(datePattern, java.util.Locale.getDefault()) }
                .getOrElse { java.text.SimpleDateFormat("EEE, MMM dd, yyyy", java.util.Locale.getDefault()) }
            currentTime = sdfTime.format(now.time)
            currentDate = sdfDate.format(now.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    when (style) {
        "GRID" -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "clock" },
                    tint = parseColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            color = parseColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = descTextSizeSp.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        "GRID_ALT" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "clock" },
                    tint = parseColor,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        "CAROUSEL" -> {
            val baseIconSize = if (isFocused) 64.dp else 48.dp
            val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName.ifBlank { "clock" },
                        tint = parseColor,
                        modifier = Modifier.size(scaledIconSize)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = parseColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = alignHoriz,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp, color = parseColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = descTextSizeSp.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BatteryWidgetContent(
    system: SystemEntity,
    isFocused: Boolean,
    style: String,
    onWidgetDoubleTap: (() -> Unit)? = null,
    descTextSizeSp: Int = 12,
    textSizeSp: Int = 16,
    iconScalePercent: Int = 100,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200
) {
    val context = LocalContext.current
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    var batteryPercent by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryPercent = (level * 100) / scale
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val currentStatus = context.registerReceiver(receiver, filter)
        if (currentStatus != null) {
            val level = currentStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = currentStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPercent = (level * 100) / scale
            }
            val status = currentStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if unregister fails
            }
        }
    }

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val isSingleTapEnabled = system.saveAspectRatio == "SINGLE_TAP" || system.saveAspectRatio == "true"

    val batteryIcon = if (isCharging) {
        Icons.Default.BatteryChargingFull
    } else if (batteryPercent <= 15) {
        Icons.Default.BatteryAlert
    } else {
        Icons.Default.BatteryFull
    }

    when (style) {
        "GRID" -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = batteryIcon,
                    contentDescription = "Battery Status",
                    tint = parseColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            color = parseColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCharging) "Charging" else "Discharging",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        "GRID_ALT" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = batteryIcon,
                    contentDescription = "Battery Status",
                    tint = parseColor,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$batteryPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = descTextSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        "CAROUSEL" -> {
            val baseIconSize = if (isFocused) 64.dp else 48.dp
            val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery Status",
                        tint = parseColor,
                        modifier = Modifier.size(scaledIconSize)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$batteryPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = parseColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = alignHoriz,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$batteryPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp, color = parseColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isCharging) "Charging" else "Discharging",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = descTextSizeSp.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AppLaunchWidgetContent(
    system: SystemEntity,
    isFocused: Boolean,
    style: String,
    descTextSizeSp: Int = 12,
    textSizeSp: Int = 16,
    iconScalePercent: Int = 100,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200
) {
    val context = LocalContext.current
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val useAppIcon = system.allowedExtensions == "true"
    val bitmap = if (useAppIcon && system.folderPath.isNotEmpty()) {
        remember(system.folderPath) {
            AndroidAppIconHelper.getAppIconBitmap(context, system.folderPath)
        }
    } else {
        null
    }

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val showLaunchText = system.retroArchCore != "false"
    val subtitleText = if (showLaunchText) "Launch App" else ""

    when (style) {
        "GRID" -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    UniversalIconView(
                        iconNameOrPath = system.iconName.ifBlank { "android" },
                        tint = parseColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz,
                    verticalArrangement = Arrangement.Center
                ) {
                    MarqueeText(
                        text = system.name,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            color = parseColor
                        )
                    )
                    if (subtitleText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        "GRID_ALT" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    UniversalIconView(
                        iconNameOrPath = system.iconName.ifBlank { "android" },
                        tint = parseColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                MarqueeText(
                    text = system.name,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                if (subtitleText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = descTextSizeSp.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        "CAROUSEL" -> {
            val baseIconSize = if (isFocused) 64.dp else 48.dp
            val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(scaledIconSize)
                        )
                    } else {
                        UniversalIconView(
                            iconNameOrPath = system.iconName.ifBlank { "android" },
                            tint = parseColor,
                            modifier = Modifier.size(scaledIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                MarqueeText(
                    text = system.name,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = alignHoriz,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp, color = parseColor),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitleText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BgmWidgetContent(
    system: SystemEntity,
    isFocused: Boolean,
    style: String,
    descTextSizeSp: Int = 12,
    textSizeSp: Int = 16,
    iconScalePercent: Int = 100,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val currentTrack by com.example.util.SoundManager.currentTrackName.collectAsState()
    val isPlaying by com.example.util.SoundManager.isPlayingState.collectAsState()
    val hasBgmFiles by com.example.util.SoundManager.hasBgmFiles.collectAsState()
    val displaySettings = LocalDisplaySettings.current

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val trackTitle = if (!displaySettings.enableBgm) {
        "BGM stopped"
    } else if (hasBgmFiles) {
        currentTrack ?: "BGM Active"
    } else {
        "No BGM files"
    }

    val actionText = system.allowedExtensions.ifBlank { "Tap to view" }
    val isActuallyPlaying = isPlaying && displaySettings.enableBgm
    val playStateText = if (isActuallyPlaying) "Playing • $actionText" else "Stopped • $actionText"

    when (style) {
        "GRID" -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "music_note" },
                    tint = parseColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz,
                    verticalArrangement = Arrangement.Center
                ) {
                    MarqueeText(
                        text = trackTitle,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            color = parseColor
                        )
                    )
                    Text(
                        text = playStateText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        "GRID_ALT" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "music_note" },
                    tint = parseColor,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                MarqueeText(
                    text = trackTitle,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Text(
                    text = playStateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = descTextSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        "CAROUSEL" -> {
            val baseIconSize = if (isFocused) 64.dp else 48.dp
            val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName.ifBlank { "music_note" },
                        tint = parseColor,
                        modifier = Modifier.size(scaledIconSize)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                MarqueeText(
                    text = trackTitle,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = playStateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = alignHoriz,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playStateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TotalGamesWidgetContent(
    system: SystemEntity, 
    totalGamesCount: Int, 
    totalFavoritesCount: Int,
    totalCompletedCount: Int,
    isFocused: Boolean, 
    style: String
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val widgetItems = remember(system.folderPath, system.colorHex, system.iconName) {
        TotalGamesWidgetConfig.parse(system.folderPath, system.colorHex, system.iconName)
            .filter { it.enabled }
    }

    if (widgetItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Counters Enabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        return
    }

    val heroItem = widgetItems.first()
    val secondaryItems = widgetItems.drop(1)

    fun getCountFor(key: String): Int {
        return when (key) {
            "games" -> totalGamesCount
            "favorites" -> totalFavoritesCount
            "completed" -> totalCompletedCount
            else -> 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val heroCount = getCountFor(heroItem.key)
        val heroColor = runCatching { Color(android.graphics.Color.parseColor(heroItem.color)) }.getOrDefault(parseColor)
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            UniversalIconView(
                iconNameOrPath = heroItem.icon,
                tint = heroColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$heroCount",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = heroColor),
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = heroItem.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (secondaryItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                secondaryItems.forEach { item ->
                    val count = getCountFor(item.key)
                    val itemColor = runCatching { Color(android.graphics.Color.parseColor(item.color)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UniversalIconView(
                                iconNameOrPath = item.icon,
                                tint = itemColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTextWidgetContent(
    system: SystemEntity,
    isFocused: Boolean,
    style: String,
    descTextSizeSp: Int = 12,
    textSizeSp: Int = 16,
    iconScalePercent: Int = 100,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onWidgetDoubleTap: (() -> Unit)? = null
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val customText = system.retroArchCore.ifBlank { "Custom text" }
    val titleText = system.name.ifBlank { "Custom Text" }

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val isSingleTapEnabled = system.saveAspectRatio == "SINGLE_TAP" || system.saveAspectRatio == "true"

    when (style) {
        "GRID" -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "pencil" },
                    tint = parseColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz,
                    verticalArrangement = Arrangement.Center
                ) {
                    MarqueeText(
                        text = titleText,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            color = parseColor
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = descTextSizeSp.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        "GRID_ALT" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "pencil" },
                    tint = parseColor,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                MarqueeText(
                    text = titleText,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = customText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = descTextSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        "CAROUSEL" -> {
            val baseIconSize = if (isFocused) 64.dp else 48.dp
            val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName.ifBlank { "pencil" },
                        tint = parseColor,
                        modifier = Modifier.size(scaledIconSize)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                MarqueeText(
                    text = titleText,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = customText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp,
                        color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(isSingleTapEnabled, onWidgetDoubleTap) {
                        detectTapGestures(
                            onTap = {
                                if (isSingleTapEnabled) {
                                    onWidgetDoubleTap?.invoke()
                                }
                            },
                            onDoubleTap = {
                                onWidgetDoubleTap?.invoke()
                            }
                        )
                    },
                horizontalAlignment = alignHoriz,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = textSizeSp.sp,
                        color = parseColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = customText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = descTextSizeSp.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SlideshowWidgetContent(system: SystemEntity, isFocused: Boolean, style: String) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val delaySeconds = remember(system.allowedExtensions) {
        system.allowedExtensions.toIntOrNull() ?: 5
    }

    val isPlaying by com.example.util.SlideshowManager.isPlaying.collectAsState()
    val currentImageIndex by com.example.util.SlideshowManager.currentIndex.collectAsState()
    val imageFiles by com.example.util.SlideshowManager.imageFiles.collectAsState()

    LaunchedEffect(system.folderPath) {
        com.example.util.SlideshowManager.setFolderPath(system.folderPath)
    }

    LaunchedEffect(isPlaying, imageFiles, delaySeconds) {
        if (isPlaying && imageFiles.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(delaySeconds * 1000L)
                com.example.util.SlideshowManager.next()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (imageFiles.isNotEmpty()) {
            val currentFile = imageFiles.getOrNull(currentImageIndex) ?: imageFiles.first()
            Crossfade(
                targetState = currentFile,
                animationSpec = tween(1000),
                label = "slideshow"
            ) { file ->
                AsyncImage(
                    model = file,
                    contentDescription = "Slideshow Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )
            Text(
                text = system.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName.ifBlank { "palette" },
                    tint = parseColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "No images in folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemGridCard(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    descTextSizeSp: Int = 12,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    totalGamesCount: Int = 0,
    totalFavoritesCount: Int = 0,
    totalCompletedCount: Int = 0,
    onWidgetDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) parseColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "border"
    )

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "scale")

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onDoubleClick = if (isWidget && onWidgetDoubleTap != null) onWidgetDoubleTap else null,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                parseColor.copy(alpha = if (isFocused) 0.25f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isWidget) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (system.defaultLaunchMode) {
                        "WIDGET_CLOCK" -> ClockWidgetContent(system = system, isFocused = isFocused, style = "GRID", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, textAlignment = textAlignment, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_TOTAL_GAMES" -> TotalGamesWidgetContent(system = system, totalGamesCount = totalGamesCount, totalFavoritesCount = totalFavoritesCount, totalCompletedCount = totalCompletedCount, isFocused = isFocused, style = "GRID")
                        "WIDGET_CUSTOM_TEXT" -> CustomTextWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, textAlignment = textAlignment, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis, onWidgetDoubleTap = onWidgetDoubleTap)
                        "WIDGET_SLIDESHOW" -> SlideshowWidgetContent(system = system, isFocused = isFocused, style = "GRID")
                        "WIDGET_BGM" -> BgmWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, textAlignment = textAlignment, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_BATTERY" -> BatteryWidgetContent(system = system, isFocused = isFocused, style = "GRID", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, textAlignment = textAlignment, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_APP_LAUNCH" -> AppLaunchWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, textAlignment = textAlignment, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        else -> {}
                    }

                    if (showEditIcon) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Widget",
                                    tint = parseColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName,
                        tint = parseColor,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = alignHoriz,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MarqueeText(
                            text = system.name,
                            isFocused = isFocused,
                            marqueeSpeed = marqueeSpeed,
                            marqueeDelayMillis = marqueeDelayMillis,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp)
                        )
                        Text(
                            text = system.manufacturer,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = parseColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = system.shortName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = parseColor, fontSize = descTextSizeSp.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (showEditIcon) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit System",
                                tint = parseColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemTextRow(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    descTextSizeSp: Int = 12,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    totalGamesCount: Int = 0,
    totalFavoritesCount: Int = 0,
    totalCompletedCount: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onWidgetDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val backgroundColor = if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface

    val alignHoriz = when (textAlignment) {
        "CENTER" -> Alignment.CenterHorizontally
        "RIGHT" -> Alignment.End
        else -> Alignment.Start
    }

    val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isFocused) BorderStroke(2.dp, parseColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onDoubleClick = if (isWidget && onWidgetDoubleTap != null) onWidgetDoubleTap else null,
                onLongClick = onLongClick
            )
    ) {
        if (isWidget) {
            val isClassicClock = (system.defaultLaunchMode == "WIDGET_CLOCK" || system.id.startsWith("widget_clock")) && system.folderPath.startsWith("CLASSIC")
            val is24Hour = system.folderPath.contains("24H") || system.allowedExtensions == "24h" || system.allowedExtensions == "HH:mm:ss" || system.allowedExtensions == "HH:mm"
            val showSeconds = !system.folderPath.contains("NOSEC") && !system.folderPath.contains("NO_SEC")

            if (isClassicClock) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        SevenSegmentClock(
                            is24Hour = is24Hour,
                            color = parseColor,
                            digitHeight = (textSizeSp * 1.5f).coerceIn(20f, 36f).dp,
                            showSeconds = showSeconds
                        )
                    }

                    if (showEditIcon) {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Widget",
                                tint = parseColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                val context = LocalContext.current
                val isAppLaunch = system.defaultLaunchMode == "WIDGET_APP_LAUNCH"
                val useAppIcon = isAppLaunch && system.allowedExtensions == "true"
                val bitmap = remember(system.folderPath, useAppIcon) {
                    if (useAppIcon && system.folderPath.isNotEmpty()) {
                        AndroidAppIconHelper.getAppIconBitmap(context, system.folderPath)
                    } else {
                        null
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    UniversalIconView(
                        iconNameOrPath = system.iconName,
                        tint = parseColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = system.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (system.defaultLaunchMode) {
                            "WIDGET_CLOCK" -> "Clock Widget"
                            "WIDGET_TOTAL_GAMES" -> "Games Count Widget"
                            "WIDGET_CUSTOM_TEXT" -> system.retroArchCore.ifBlank { "Custom text" }
                            "WIDGET_SLIDESHOW" -> "Image Slideshow Widget"
                            "WIDGET_BGM" -> "BGM Music Widget"
                            "WIDGET_BATTERY" -> "Battery Status Widget"
                            "WIDGET_APP_LAUNCH" -> "App Launcher Widget"
                            else -> "Widget"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                when (system.defaultLaunchMode) {
                    "WIDGET_CLOCK" -> {
                        var currentTime by remember { mutableStateOf("") }
                        val timePattern = remember(system.allowedExtensions) {
                            when (system.allowedExtensions) {
                                "24h" -> "HH:mm"
                                "12h", "" -> "hh:mm a"
                                else -> {
                                    // Strip seconds if present in custom format to keep the compact list row neat
                                    system.allowedExtensions.replace(":ss", "").ifBlank { "hh:mm a" }
                                }
                            }
                        }
                        LaunchedEffect(timePattern) {
                            while (true) {
                                val now = java.util.Calendar.getInstance()
                                val sdfTime = runCatching { java.text.SimpleDateFormat(timePattern, java.util.Locale.getDefault()) }
                                    .getOrElse { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
                                currentTime = sdfTime.format(now.time)
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = parseColor)
                        )
                    }
                    "WIDGET_TOTAL_GAMES" -> {
                        Text(
                            text = "$totalGamesCount (★$totalFavoritesCount, ✔$totalCompletedCount)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = parseColor)
                        )
                    }
                    "WIDGET_CUSTOM_TEXT" -> {
                        Text(
                            text = system.retroArchCore,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                    "WIDGET_SLIDESHOW" -> {
                        val imageFiles = remember(system.folderPath) {
                            val dir = File(system.folderPath)
                            if (dir.exists() && dir.isDirectory) {
                                dir.listFiles { _, name ->
                                    val lower = name.lowercase()
                                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                                }?.toList() ?: emptyList()
                            } else {
                                emptyList()
                            }
                        }
                        Text(
                            text = "${imageFiles.size} images",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = parseColor)
                        )
                    }
                    "WIDGET_BGM" -> {
                        val currentTrack by com.example.util.SoundManager.currentTrackName.collectAsState()
                        val isPlaying by com.example.util.SoundManager.isPlayingState.collectAsState()
                        Text(
                            text = currentTrack ?: if (isPlaying) "Playing" else "Stopped",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = parseColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp).basicMarquee()
                        )
                    }
                    "WIDGET_BATTERY" -> {
                        val context = LocalContext.current
                        var batteryPercent by remember { mutableStateOf(100) }
                        var isCharging by remember { mutableStateOf(false) }

                        DisposableEffect(context) {
                            val receiver = object : BroadcastReceiver() {
                                override fun onReceive(ctx: Context?, intent: Intent?) {
                                    intent?.let {
                                        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                                        if (level >= 0 && scale > 0) {
                                            batteryPercent = (level * 100) / scale
                                        }
                                        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                                status == BatteryManager.BATTERY_STATUS_FULL
                                    }
                                }
                            }
                            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                            val currentStatus = context.registerReceiver(receiver, filter)
                            if (currentStatus != null) {
                                val level = currentStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                val scale = currentStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                                if (level >= 0 && scale > 0) {
                                    batteryPercent = (level * 100) / scale
                                }
                                val status = currentStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                        status == BatteryManager.BATTERY_STATUS_FULL
                            }

                            onDispose {
                                try {
                                    context.unregisterReceiver(receiver)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }

                        val expandNotifications = {
                            try {
                                val statusBarService = context.getSystemService("statusbar")
                                val statusBarManager = Class.forName("android.app.StatusBarManager")
                                val showNotificationMethod = statusBarManager.getMethod("expandNotificationsPanel")
                                showNotificationMethod.isAccessible = true
                                showNotificationMethod.invoke(statusBarService)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val batteryIcon = if (isCharging) {
                            Icons.Default.BatteryChargingFull
                        } else if (batteryPercent <= 15) {
                            Icons.Default.BatteryAlert
                        } else {
                            Icons.Default.BatteryFull
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = "Battery Status",
                                tint = parseColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$batteryPercent%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = parseColor)
                            )
                        }
                    }
                    "WIDGET_APP_LAUNCH" -> {
                        val showLaunchText = system.retroArchCore != "false"
                        if (showLaunchText) {
                            Text(
                                text = "Launch App",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = parseColor, fontSize = descTextSizeSp.sp)
                            )
                        }
                    }
                }

                if (showEditIcon) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Widget",
                            tint = parseColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UniversalIconView(
                    iconNameOrPath = system.iconName,
                    tint = parseColor,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = alignHoriz
                ) {
                    MarqueeText(
                        text = system.name,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = textSizeSp.sp)
                    )
                    Text(
                        text = system.folderPath,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = parseColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = system.shortName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = parseColor, fontSize = descTextSizeSp.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (showEditIcon) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit System",
                            tint = parseColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemGridCardAlt(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    descTextSizeSp: Int = 12,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    totalGamesCount: Int = 0,
    totalFavoritesCount: Int = 0,
    totalCompletedCount: Int = 0,
    onWidgetDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) parseColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "border"
    )

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "scale")

    val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onDoubleClick = if (isWidget && onWidgetDoubleTap != null) onWidgetDoubleTap else null,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                parseColor.copy(alpha = if (isFocused) 0.25f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isWidget) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (system.defaultLaunchMode) {
                        "WIDGET_CLOCK" -> ClockWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_TOTAL_GAMES" -> TotalGamesWidgetContent(system = system, totalGamesCount = totalGamesCount, totalFavoritesCount = totalFavoritesCount, totalCompletedCount = totalCompletedCount, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_CUSTOM_TEXT" -> CustomTextWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis, onWidgetDoubleTap = onWidgetDoubleTap)
                        "WIDGET_SLIDESHOW" -> SlideshowWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_BGM" -> BgmWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_BATTERY" -> BatteryWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_APP_LAUNCH" -> AppLaunchWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        else -> {}
                    }

                    if (showEditIcon) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Widget",
                                    tint = parseColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    UniversalIconView(
                        iconNameOrPath = system.iconName,
                        tint = parseColor,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MarqueeText(
                        text = system.name,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = textSizeSp.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )

                    if (system.manufacturer.isNotEmpty()) {
                        Text(
                            text = system.manufacturer,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = parseColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = system.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = parseColor,
                                fontSize = descTextSizeSp.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (showEditIcon) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit System",
                            tint = parseColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemCarouselCard(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    descTextSizeSp: Int = 12,
    iconScalePercent: Int = 100,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    totalGamesCount: Int = 0,
    totalFavoritesCount: Int = 0,
    totalCompletedCount: Int = 0,
    onWidgetDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit
) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) parseColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        label = "carouselBorder"
    )

    val isWidget = system.id.startsWith("widget_") || system.defaultLaunchMode.startsWith("WIDGET_")

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onDoubleClick = if (isWidget && onWidgetDoubleTap != null) onWidgetDoubleTap else null,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                parseColor.copy(alpha = if (isFocused) 0.3f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isWidget) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (system.defaultLaunchMode) {
                        "WIDGET_CLOCK" -> ClockWidgetContent(system = system, isFocused = isFocused, style = "CAROUSEL", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, iconScalePercent = iconScalePercent, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_TOTAL_GAMES" -> TotalGamesWidgetContent(system = system, totalGamesCount = totalGamesCount, totalFavoritesCount = totalFavoritesCount, totalCompletedCount = totalCompletedCount, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_CUSTOM_TEXT" -> CustomTextWidgetContent(system = system, isFocused = isFocused, style = "CAROUSEL", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, iconScalePercent = iconScalePercent, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis, onWidgetDoubleTap = onWidgetDoubleTap)
                        "WIDGET_SLIDESHOW" -> SlideshowWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_BGM" -> BgmWidgetContent(system = system, isFocused = isFocused, style = "CAROUSEL", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, iconScalePercent = iconScalePercent, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_BATTERY" -> BatteryWidgetContent(system = system, isFocused = isFocused, style = "CAROUSEL", onWidgetDoubleTap = onWidgetDoubleTap, descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, iconScalePercent = iconScalePercent, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        "WIDGET_APP_LAUNCH" -> AppLaunchWidgetContent(system = system, isFocused = isFocused, style = "CAROUSEL", descTextSizeSp = descTextSizeSp, textSizeSp = textSizeSp, iconScalePercent = iconScalePercent, marqueeSpeed = marqueeSpeed, marqueeDelayMillis = marqueeDelayMillis)
                        else -> {}
                    }

                    if (showEditIcon) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Widget",
                                    tint = parseColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Main Carousel Item: Large Icon centered on top, then Title below it with marquee
                val baseIconSize = if (isFocused) 64.dp else 48.dp
                val scaledIconSize = baseIconSize * (iconScalePercent / 100f)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        UniversalIconView(
                            iconNameOrPath = system.iconName,
                            tint = parseColor,
                            modifier = Modifier.size(scaledIconSize)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    MarqueeText(
                        text = system.name,
                        isFocused = isFocused,
                        marqueeSpeed = marqueeSpeed,
                        marqueeDelayMillis = marqueeDelayMillis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = (if (isFocused) textSizeSp * 1.05f else textSizeSp.toFloat()).sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (showEditIcon) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit System",
                            tint = parseColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
