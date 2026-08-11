package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.GameRomEntity
import com.example.data.db.SystemEntity
import com.example.data.util.GameIconResolver
import com.example.data.model.DisplaySettings
import com.example.data.model.RomListSettings
import com.example.data.model.RomListStyle
import com.example.data.model.TextAlignmentOption
import kotlinx.coroutines.launch
import java.io.File

private val topMostSelectedPaths = mutableMapOf<String, String>()

private fun getItemUniqueKey(item: ListDisplayItem): String {
    return when (item) {
        is ListDisplayItem.ParentFolderItem -> "__parent__"
        is ListDisplayItem.FolderItem -> "folder:${item.fullPath}"
        is ListDisplayItem.GameItem -> "game:${item.game.filePath}"
    }
}

sealed class ListDisplayItem {
    object ParentFolderItem : ListDisplayItem()
    data class FolderItem(val name: String, val fullPath: String) : ListDisplayItem()
    data class GameItem(val game: GameRomEntity) : ListDisplayItem()
}

@Composable
fun GameGrid(
    currentSystem: SystemEntity?,
    roms: List<GameRomEntity>,
    listSettings: RomListSettings,
    onUpdateListSettings: (RomListSettings) -> Unit,
    onGameClick: (GameRomEntity) -> Unit,
    onGameLongClick: (GameRomEntity) -> Unit,
    onFavoriteToggle: (GameRomEntity) -> Unit,
    onCompletedToggle: (GameRomEntity) -> Unit = {},
    onScanFolderClick: () -> Unit,
    isScanning: Boolean = false,
    onOpenAppVisibilityClick: (() -> Unit)? = null,
    onOpenSystemSettings: (() -> Unit)? = null,
    onUpdateSystemFolder: ((String) -> Unit)? = null,
    isListFocused: Boolean = true,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    dpadUpTrigger: Long = 0L,
    dpadDownTrigger: Long = 0L,
    dpadLeftTrigger: Long = 0L,
    dpadRightTrigger: Long = 0L,
    onMoveFocusUp: () -> Unit = {},
    onMoveFocusDown: () -> Unit = {},
    pageUpTrigger: Long = 0L,
    pageDownTrigger: Long = 0L,
    goToTopTrigger: Long = 0L,
    goToBottomTrigger: Long = 0L,
    selectActionTrigger: Long = 0L,
    backActionTrigger: Long = 0L,
    favoriteActionTrigger: Long = 0L,
    infoActionTrigger: Long = 0L,
    onShowGameInfo: ((GameRomEntity) -> Unit)? = null,
    onDeleteFromRecent: ((GameRomEntity) -> Unit)? = null,
    onBackToMainMenu: () -> Unit = {},
    onPreviousSystem: () -> Unit = {},
    onNextSystem: () -> Unit = {},
    autoHideScrollbar: Boolean = true,
    scrollbarShowDurationMs: Int = 1500,
    enableNavigationSound: Boolean = true,
    selectedSfxFileName: String = "",
    allSystems: List<SystemEntity> = emptyList(),
    customIcons: Map<String, String> = emptyMap(),
    isScrolling: Boolean = false,
    displaySettings: DisplaySettings = DisplaySettings(),
    onApplyToActiveSystem: ((RomListSettings) -> Unit)? = null,
    onApplyToAllSystems: ((RomListSettings) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val gridCardScale = (listSettings.gridScalePercent / 100f).coerceIn(0.5f, 2.0f)
    val minTileWidth = (160.dp * gridCardScale).coerceAtLeast(80.dp)
    val gridNumCols = maxOf(1, (screenWidthDp / minTileWidth).toInt())

    var showStyleDialog by remember { mutableStateOf(false) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var currentSubfolderPath by remember(currentSystem?.id) { mutableStateOf("") }
    val openedFolderHistory = remember(currentSystem?.id) { mutableStateMapOf<String, String>() }

    var isHeaderFocused by remember(currentSystem?.id) { mutableStateOf(false) }
    var headerFocusedIndex by remember { mutableIntStateOf(0) } // 0: Folder, 1: Refresh, 2: Style

    val coroutineScope = rememberCoroutineScope()

    var lastHandledSelectTrigger by remember { mutableLongStateOf(selectActionTrigger) }
    var lastHandledBackTrigger by remember { mutableLongStateOf(backActionTrigger) }
    var lastHandledFavoriteTrigger by remember { mutableLongStateOf(favoriteActionTrigger) }
    var lastHandledInfoTrigger by remember { mutableLongStateOf(infoActionTrigger) }
    var lastHandledDpadLeft by remember { mutableLongStateOf(dpadLeftTrigger) }
    var lastHandledDpadRight by remember { mutableLongStateOf(dpadRightTrigger) }
    var lastHandledDpadUp by remember { mutableLongStateOf(dpadUpTrigger) }
    var lastHandledDpadDown by remember { mutableLongStateOf(dpadDownTrigger) }
    var lastHandledPageUpTrigger by remember { mutableLongStateOf(pageUpTrigger) }
    var lastHandledPageDownTrigger by remember { mutableLongStateOf(pageDownTrigger) }
    var lastHandledGoToTopTrigger by remember { mutableLongStateOf(goToTopTrigger) }
    var lastHandledGoToBottomTrigger by remember { mutableLongStateOf(goToBottomTrigger) }

    val isAndroidSystem = currentSystem?.id in listOf("android_apps", "android_games", "android_emulators") || currentSystem?.defaultLaunchMode == "ANDROID_APP"
    val isVirtualCollection = currentSystem?.id == "favorites" || currentSystem?.id == "recently_played" || isAndroidSystem

    // Determine subfolders and items in current directory
    val displayItems = remember(currentSystem?.id, currentSystem?.folderPath, currentSubfolderPath, roms) {
        val list = mutableListOf<ListDisplayItem>()

        if (currentSubfolderPath.isNotEmpty()) {
            list.add(ListDisplayItem.ParentFolderItem)
        }

        if (currentSystem != null && !isVirtualCollection) {
            val baseFolder = File(currentSystem.folderPath)
            val currentFolder = if (currentSubfolderPath.isEmpty()) baseFolder else File(baseFolder, currentSubfolderPath)

            if (currentFolder.exists() && currentFolder.isDirectory) {
                // List subdirectories
                currentFolder.listFiles { f -> 
                    f.isDirectory && 
                    !f.name.startsWith(".") && 
                    !f.name.startsWith("_") && 
                    !f.name.equals("noload", ignoreCase = true) 
                }
                    ?.sortedBy { it.name.lowercase() }
                    ?.forEach { dir ->
                        list.add(ListDisplayItem.FolderItem(dir.name, dir.absolutePath))
                    }
            }
        }

        // Add matching ROMs
        val matchingRoms = if (isVirtualCollection) {
            roms
        } else {
            val baseFolder = File(currentSystem?.folderPath ?: "")
            val targetFolder = if (currentSubfolderPath.isEmpty()) baseFolder else File(baseFolder, currentSubfolderPath)
            val targetAbsolutePath = targetFolder.absolutePath

            roms.filter { rom ->
                val parentPath = File(rom.filePath).parent
                parentPath == targetAbsolutePath
            }
        }

        matchingRoms.forEach { game ->
            list.add(ListDisplayItem.GameItem(game))
        }

        list
    }

    val validHeaderIndices = remember(isAndroidSystem, isVirtualCollection) {
        when {
            isAndroidSystem -> listOf(0, 2)
            isVirtualCollection -> listOf(2)
            else -> listOf(0, 1, 2)
        }
    }

    LaunchedEffect(validHeaderIndices) {
        if (headerFocusedIndex !in validHeaderIndices) {
            headerFocusedIndex = validHeaderIndices.firstOrNull() ?: 2
        }
    }

    var selectedIndex by remember(currentSystem?.id) {
        val sysId = currentSystem?.id ?: ""
        val savedKey = topMostSelectedPaths[sysId]
        val initialIdx = if (savedKey != null && currentSubfolderPath.isEmpty()) {
            val idx = displayItems.indexOfFirst { getItemUniqueKey(it) == savedKey }
            if (idx >= 0) idx else 0
        } else {
            0
        }
        mutableIntStateOf(initialIdx)
    }

    // Whenever selectedIndex changes in top-most directory, remember it
    LaunchedEffect(selectedIndex, currentSubfolderPath, currentSystem?.id) {
        if (currentSubfolderPath.isEmpty() && currentSystem != null && selectedIndex in displayItems.indices) {
            val item = displayItems[selectedIndex]
            topMostSelectedPaths[currentSystem.id] = getItemUniqueKey(item)
        }
    }

    var lastSubfolderPath by remember(currentSystem?.id) { mutableStateOf("") }
    var targetHighlightFolder by remember(currentSystem?.id) { mutableStateOf<String?>(null) }

    val displaySettings = LocalDisplaySettings.current
    var showRomIconPopUp by remember { mutableStateOf(false) }

    LaunchedEffect(
        selectedIndex,
        displayItems,
        isListFocused,
        isHeaderFocused,
        displaySettings.enableRomIconPopUp,
        displaySettings.romIconPopUpTimeoutMs
    ) {
        showRomIconPopUp = false
        val currentSelectedItem = displayItems.getOrNull(selectedIndex)
        if (displaySettings.enableRomIconPopUp && isListFocused && !isHeaderFocused && currentSelectedItem is ListDisplayItem.GameItem) {
            val delayMs = displaySettings.romIconPopUpTimeoutMs.toLong().coerceAtLeast(0L)
            kotlinx.coroutines.delay(delayMs)
            showRomIconPopUp = true
        }
    }

    if (lastSubfolderPath != currentSubfolderPath) {
        val targetFolder = targetHighlightFolder
        if (targetFolder != null) {
            val idx = displayItems.indexOfFirst {
                it is ListDisplayItem.FolderItem && it.name == targetFolder
            }
            selectedIndex = if (idx >= 0) idx else 0
            targetHighlightFolder = null
        } else {
            selectedIndex = 0
        }
        lastSubfolderPath = currentSubfolderPath
    }

    val gridState = remember(currentSystem?.id, currentSubfolderPath) {
        LazyGridState(firstVisibleItemIndex = maxOf(0, selectedIndex - 2))
    }
    val listState = remember(currentSystem?.id, currentSubfolderPath) {
        LazyListState(firstVisibleItemIndex = maxOf(0, selectedIndex - 2))
    }

    fun scrollToCenter(index: Int) {
        coroutineScope.launch {
            if (listSettings.listStyle == RomListStyle.GRID) {
                val vh = gridState.layoutInfo.viewportSize.height
                val offset = if (vh > 0) -(vh / 3) else 0
                gridState.animateScrollToItem(index, scrollOffset = offset)
            } else {
                val vh = listState.layoutInfo.viewportSize.height
                val offset = if (vh > 0) -(vh / 3) else 0
                listState.animateScrollToItem(index, scrollOffset = offset)
            }
        }
    }

    fun openSubfolder(folderName: String) {
        com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
        val parentPath = currentSubfolderPath
        openedFolderHistory[parentPath] = folderName
        currentSubfolderPath = if (currentSubfolderPath.isEmpty()) folderName else "$currentSubfolderPath/$folderName"
        selectedIndex = 0
    }

    fun navigateUpToParent() {
        if (currentSubfolderPath.isNotEmpty()) {
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            val oldSubfolderPath = currentSubfolderPath
            val parentPath = if (oldSubfolderPath.contains("/")) {
                oldSubfolderPath.substringBeforeLast("/")
            } else {
                ""
            }
            val lastOpenedFolder = openedFolderHistory[parentPath]
            targetHighlightFolder = lastOpenedFolder
            currentSubfolderPath = parentPath
        } else {
            onBackToMainMenu()
        }
    }



    // Reset header focus when system or subfolder changes
    LaunchedEffect(currentSystem?.id, currentSubfolderPath) {
        isHeaderFocused = false
    }

    // React to Gamepad Triggers
    LaunchedEffect(dpadUpTrigger) {
        if (dpadUpTrigger > 0L && dpadUpTrigger != lastHandledDpadUp) {
            lastHandledDpadUp = dpadUpTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            if (isHeaderFocused) {
                isHeaderFocused = false
                onMoveFocusUp()
            } else {
                val step = if (listSettings.listStyle == RomListStyle.GRID) gridNumCols else 1
                if (selectedIndex >= step) {
                    selectedIndex -= step
                    scrollToCenter(selectedIndex)
                } else {
                    if (displaySettings.showSubSystemTitle) {
                        isHeaderFocused = true
                        headerFocusedIndex = validHeaderIndices.firstOrNull() ?: 2
                    } else {
                        onMoveFocusUp()
                    }
                }
            }
        }
    }

    LaunchedEffect(dpadDownTrigger) {
        if (dpadDownTrigger > 0L && dpadDownTrigger != lastHandledDpadDown) {
            lastHandledDpadDown = dpadDownTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            if (isHeaderFocused) {
                isHeaderFocused = false
                selectedIndex = 0
                scrollToCenter(selectedIndex)
            } else {
                val step = if (listSettings.listStyle == RomListStyle.GRID) gridNumCols else 1
                if (displayItems.isNotEmpty() && selectedIndex + step < displayItems.size) {
                    selectedIndex += step
                    scrollToCenter(selectedIndex)
                } else if (selectedIndex < displayItems.size - 1) {
                    selectedIndex = displayItems.size - 1
                    scrollToCenter(selectedIndex)
                } else {
                    onMoveFocusDown()
                }
            }
        }
    }

    LaunchedEffect(dpadLeftTrigger) {
        if (dpadLeftTrigger > 0L && dpadLeftTrigger != lastHandledDpadLeft) {
            lastHandledDpadLeft = dpadLeftTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            if (isHeaderFocused) {
                val currentIndexInValid = validHeaderIndices.indexOf(headerFocusedIndex)
                if (currentIndexInValid > 0) {
                    headerFocusedIndex = validHeaderIndices[currentIndexInValid - 1]
                }
            } else if (listSettings.listStyle == RomListStyle.GRID) {
                if (selectedIndex > 0) {
                    selectedIndex -= 1
                    scrollToCenter(selectedIndex)
                } else {
                    onPreviousSystem()
                }
            } else {
                onPreviousSystem()
            }
        }
    }

    LaunchedEffect(dpadRightTrigger) {
        if (dpadRightTrigger > 0L && dpadRightTrigger != lastHandledDpadRight) {
            lastHandledDpadRight = dpadRightTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            if (isHeaderFocused) {
                val currentIndexInValid = validHeaderIndices.indexOf(headerFocusedIndex)
                if (currentIndexInValid >= 0 && currentIndexInValid < validHeaderIndices.size - 1) {
                    headerFocusedIndex = validHeaderIndices[currentIndexInValid + 1]
                }
            } else if (listSettings.listStyle == RomListStyle.GRID) {
                if (selectedIndex + 1 < displayItems.size) {
                    selectedIndex += 1
                    scrollToCenter(selectedIndex)
                } else {
                    onNextSystem()
                }
            } else {
                onNextSystem()
            }
        }
    }

    LaunchedEffect(pageUpTrigger) {
        if (pageUpTrigger > 0L && pageUpTrigger != lastHandledPageUpTrigger && displayItems.isNotEmpty()) {
            lastHandledPageUpTrigger = pageUpTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            selectedIndex = maxOf(0, selectedIndex - 8)
            scrollToCenter(selectedIndex)
        }
    }

    LaunchedEffect(pageDownTrigger) {
        if (pageDownTrigger > 0L && pageDownTrigger != lastHandledPageDownTrigger && displayItems.isNotEmpty()) {
            lastHandledPageDownTrigger = pageDownTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            selectedIndex = minOf(displayItems.size - 1, selectedIndex + 8)
            scrollToCenter(selectedIndex)
        }
    }

    LaunchedEffect(goToTopTrigger) {
        if (goToTopTrigger > 0L && goToTopTrigger != lastHandledGoToTopTrigger && displayItems.isNotEmpty()) {
            lastHandledGoToTopTrigger = goToTopTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            selectedIndex = 0
            scrollToCenter(selectedIndex)
        }
    }

    LaunchedEffect(goToBottomTrigger) {
        if (goToBottomTrigger > 0L && goToBottomTrigger != lastHandledGoToBottomTrigger && displayItems.isNotEmpty()) {
            lastHandledGoToBottomTrigger = goToBottomTrigger
            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
            selectedIndex = maxOf(0, displayItems.size - 1)
            scrollToCenter(selectedIndex)
        }
    }

    // A Button (Select Action): Launch game or open subfolder
    LaunchedEffect(selectActionTrigger) {
        if (selectActionTrigger > 0L && selectActionTrigger != lastHandledSelectTrigger) {
            lastHandledSelectTrigger = selectActionTrigger
            if (isScrolling) {
                // Ignore button press while swipe animation is running on the list
                return@LaunchedEffect
            }
            if (isHeaderFocused) {
                when (headerFocusedIndex) {
                    0 -> {
                        if (isAndroidSystem) {
                            onOpenAppVisibilityClick?.invoke()
                        } else if (!isVirtualCollection) {
                            showDirectoryPicker = true
                        }
                    }
                    1 -> {
                        if (!isAndroidSystem && !isVirtualCollection) {
                            onScanFolderClick()
                        }
                    }
                    2 -> showStyleDialog = true
                }
            } else if (displayItems.isNotEmpty()) {
                val item = displayItems.getOrNull(selectedIndex)
                if (item is ListDisplayItem.ParentFolderItem) {
                    navigateUpToParent()
                } else if (item is ListDisplayItem.FolderItem) {
                    openSubfolder(item.name)
                } else if (item is ListDisplayItem.GameItem) {
                    onGameClick(item.game)
                }
            }
        }
    }

    // B Button / Back Action: Go to parent subfolder if inside a subfolder, else return to system main menu
    LaunchedEffect(backActionTrigger) {
        if (backActionTrigger > 0L && backActionTrigger != lastHandledBackTrigger) {
            lastHandledBackTrigger = backActionTrigger
            if (currentSubfolderPath.isNotEmpty()) {
                navigateUpToParent()
            } else {
                onBackToMainMenu()
            }
        }
    }

    // Y Button: Toggle Favorite on selected game item
    LaunchedEffect(favoriteActionTrigger) {
        if (favoriteActionTrigger > 0L && favoriteActionTrigger != lastHandledFavoriteTrigger) {
            lastHandledFavoriteTrigger = favoriteActionTrigger
            if (!isHeaderFocused && displayItems.isNotEmpty()) {
                val item = displayItems.getOrNull(selectedIndex)
                if (item is ListDisplayItem.GameItem) {
                    onFavoriteToggle(item.game)
                }
            }
        }
    }

    // X Button: Show ROM Details / Stats Popup
    LaunchedEffect(infoActionTrigger) {
        if (infoActionTrigger > 0L && infoActionTrigger != lastHandledInfoTrigger) {
            lastHandledInfoTrigger = infoActionTrigger
            if (!isHeaderFocused && displayItems.isNotEmpty()) {
                val item = displayItems.getOrNull(selectedIndex)
                if (item is ListDisplayItem.GameItem) {
                    onShowGameInfo?.invoke(item.game)
                }
            }
        }
    }

    val currentOnNextSystem by rememberUpdatedState(onNextSystem)
    val currentOnPreviousSystem by rememberUpdatedState(onPreviousSystem)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentSystem?.id) {
                var totalOffsetX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalOffsetX = 0f },
                    onDragEnd = {
                        val density = context.resources.displayMetrics.density
                        val thresholdPx = 60f * density
                        if (totalOffsetX < -thresholdPx) {
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            currentOnNextSystem()
                        } else if (totalOffsetX > thresholdPx) {
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            currentOnPreviousSystem()
                        }
                        totalOffsetX = 0f
                    },
                    onDragCancel = { totalOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalOffsetX += dragAmount
                    }
                )
            }
    ) {
        val subSystemHeaderBar: @Composable () -> Unit = {
            if (displaySettings.showSubSystemTitle) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSystem?.name ?: "ALL GAMES",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${displayItems.size} Items • ${if (currentSubfolderPath.isEmpty()) currentSystem?.folderPath ?: "All" else "${currentSystem?.folderPath}/$currentSubfolderPath"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAndroidSystem) {
                                    val isAppVisFocused = isListFocused && isHeaderFocused && headerFocusedIndex == 0
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAppVisFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isAppVisFocused) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        IconButton(
                                            onClick = {
                                                isHeaderFocused = true
                                                headerFocusedIndex = 0
                                                onOpenAppVisibilityClick?.invoke()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Smartphone,
                                                contentDescription = "Choose Visible Android Apps",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (!isVirtualCollection) {
                                    // Directory Selector button
                                    val isFolderFocused = isListFocused && isHeaderFocused && headerFocusedIndex == 0
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isFolderFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isFolderFocused) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        IconButton(
                                            onClick = {
                                                isHeaderFocused = true
                                                headerFocusedIndex = 0
                                                showDirectoryPicker = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = "Select Folder",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Refresh / Rescan button
                                    val isRefreshFocused = isListFocused && isHeaderFocused && headerFocusedIndex == 1
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isRefreshFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isRefreshFocused) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        IconButton(
                                            onClick = {
                                                isHeaderFocused = true
                                                headerFocusedIndex = 1
                                                onScanFolderClick()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Scan Folder",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // List Style Customizer Settings Button
                                val isStyleFocused = isListFocused && isHeaderFocused && headerFocusedIndex == 2
                                Surface(
                                    shape = CircleShape,
                                    color = if (isStyleFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isStyleFocused) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    IconButton(
                                        onClick = {
                                            isHeaderFocused = true
                                            headerFocusedIndex = 2
                                            showStyleDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (listSettings.listStyle) {
                                                RomListStyle.GRID -> Icons.Default.GridView
                                                RomListStyle.LIST -> Icons.Default.ViewList
                                                RomListStyle.TEXT_ONLY -> Icons.Default.FormatListBulleted
                                            },
                                            contentDescription = "ROM List Style Settings",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            subSystemHeaderBar()

        // Subfolder Breadcrumb Navigation Bar (if in subfolder)
        if (currentSubfolderPath.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "/$currentSubfolderPath",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val selectionAreaWidth = maxWidth
            val selectionAreaHeight = maxHeight

            if (displayItems.isEmpty()) {
                if (isScrolling) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (currentSystem?.id) {
                                    "favorites" -> Icons.Default.Star
                                    "recently_played" -> Icons.Default.History
                                    else -> Icons.Default.FolderOpen
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (currentSystem?.id) {
                                    "favorites" -> "No favorite games yet"
                                    "recently_played" -> "No recently played games yet"
                                    else -> "No items or ROM files in folder"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (currentSystem?.id) {
                                    "favorites" -> "Tap the star icon on any game to add it to your Favorites!"
                                    "recently_played" -> "Launch any game to see your recent history here!"
                                    else -> "Folder: ${currentSystem?.folderPath}\nAllowed exts: ${currentSystem?.allowedExtensions}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (currentSystem?.id != "favorites" && currentSystem?.id != "recently_played") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showDirectoryPicker = true }
                                    ) {
                                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Directory")
                                    }

                                    Button(
                                        onClick = onScanFolderClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Re-scan")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            when (listSettings.listStyle) {
                RomListStyle.GRID -> {
                    val gridCardScale = (listSettings.gridScalePercent / 100f).coerceIn(0.5f, 2.0f)
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridNumCols),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyGridScrollbar(gridState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                    ) {
                        itemsIndexed(displayItems, key = { index, item ->
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> "parent_dir"
                                is ListDisplayItem.FolderItem -> "dir_${item.fullPath}"
                                is ListDisplayItem.GameItem -> "game_${item.game.id}"
                            }
                        }) { index, item ->
                            val isFocused = isListFocused && !isHeaderFocused && (index == selectedIndex)
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> {
                                    FolderGridCardItem(
                                        folderName = ".. (Up to Parent)",
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            navigateUpToParent()
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.FolderItem -> {
                                    FolderGridCardItem(
                                        folderName = item.name,
                                        folderFullPath = item.fullPath,
                                        system = currentSystem,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            openSubfolder(item.name)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.GameItem -> {
                                    GameGridCardItem(
                                        game = item.game,
                                        system = currentSystem,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        gridScalePercent = listSettings.gridScalePercent,
                                        showDetails = listSettings.showDetails,
                                        onClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameClick(item.game)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameLongClick(item.game)
                                        },
                                        onFavoriteClick = { onFavoriteToggle(item.game) },
                                        onCompletedClick = { onCompletedToggle(item.game) },
                                        onInfoClick = { onShowGameInfo?.invoke(item.game) },
                                        onDeleteClick = if (currentSystem?.id == "recently_played") { { onDeleteFromRecent?.invoke(item.game) } } else null,
                                        allSystems = allSystems,
                                        customIcons = customIcons
                                    )
                                }
                            }
                        }
                    }
                }
                RomListStyle.LIST -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyListScrollbar(listState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                    ) {
                        itemsIndexed(displayItems, key = { index, item ->
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> "parent_dir"
                                is ListDisplayItem.FolderItem -> "dir_${item.fullPath}"
                                is ListDisplayItem.GameItem -> "game_${item.game.id}"
                            }
                        }) { index, item ->
                            val isFocused = isListFocused && !isHeaderFocused && (index == selectedIndex)
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> {
                                    FolderListRowItem(
                                        folderName = ".. (Up to Parent)",
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            navigateUpToParent()
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.FolderItem -> {
                                    FolderListRowItem(
                                        folderName = item.name,
                                        folderFullPath = item.fullPath,
                                        system = currentSystem,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            openSubfolder(item.name)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.GameItem -> {
                                    GameListRowItem(
                                        game = item.game,
                                        system = currentSystem,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        showDetails = listSettings.showDetails,
                                        onClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameClick(item.game)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameLongClick(item.game)
                                        },
                                        onFavoriteClick = { onFavoriteToggle(item.game) },
                                        onCompletedClick = { onCompletedToggle(item.game) },
                                        onInfoClick = { onShowGameInfo?.invoke(item.game) },
                                        onDeleteClick = if (currentSystem?.id == "recently_played") { { onDeleteFromRecent?.invoke(item.game) } } else null,
                                        allSystems = allSystems,
                                        customIcons = customIcons
                                    )
                                }
                            }
                        }
                    }
                }
                RomListStyle.TEXT_ONLY -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(listSettings.marginDp.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyListScrollbar(listState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                    ) {
                        itemsIndexed(displayItems, key = { index, item ->
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> "parent_dir"
                                is ListDisplayItem.FolderItem -> "dir_${item.fullPath}"
                                is ListDisplayItem.GameItem -> "game_${item.game.id}"
                            }
                        }) { index, item ->
                            val isFocused = isListFocused && !isHeaderFocused && (index == selectedIndex)
                            when (item) {
                                is ListDisplayItem.ParentFolderItem -> {
                                    FolderTextOnlyItem(
                                        folderName = ".. (Up to Parent)",
                                        settings = listSettings,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            navigateUpToParent()
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.FolderItem -> {
                                    FolderTextOnlyItem(
                                        folderName = item.name,
                                        settings = listSettings,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            isHeaderFocused = false
                                            openSubfolder(item.name)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                        }
                                    )
                                }
                                is ListDisplayItem.GameItem -> {
                                    GameTextOnlyItem(
                                        game = item.game,
                                        system = currentSystem,
                                        settings = listSettings,
                                        isFocused = isFocused,
                                        marqueeSpeed = marqueeSpeed,
                                        marqueeDelayMillis = marqueeDelayMillis,
                                        onClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameClick(item.game)
                                        },
                                        onLongClick = {
                                            selectedIndex = index
                                            isHeaderFocused = false
                                            onGameLongClick(item.game)
                                        },
                                        onFavoriteClick = { onFavoriteToggle(item.game) },
                                        onCompletedClick = { onCompletedToggle(item.game) },
                                        onInfoClick = { onShowGameInfo?.invoke(item.game) },
                                        onDeleteClick = if (currentSystem?.id == "recently_played") { { onDeleteFromRecent?.invoke(item.game) } } else null,
                                        allSystems = allSystems,
                                        customIcons = customIcons
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Auto Pop-up Icon Preview Overlay
            if (showRomIconPopUp) {
                val currentSelectedItem = displayItems.getOrNull(selectedIndex)
                if (currentSelectedItem is ListDisplayItem.GameItem) {
                    val game = currentSelectedItem.game
                    val actualSystem = remember(game, currentSystem, allSystems) {
                        if (currentSystem?.id == "favorites" || currentSystem?.id == "recently_played") {
                            allSystems.firstOrNull { it.id == game.systemId }
                        } else {
                            currentSystem
                        }
                    }
                    val saveFileImageExists = remember(game, actualSystem) {
                        if (actualSystem != null && actualSystem.retroarchSaveDir.isNotBlank()) {
                            val saveDir = File(actualSystem.retroarchSaveDir)
                            if (saveDir.exists() && saveDir.isDirectory) {
                                val romFile = File(game.filePath)
                                val stem = romFile.nameWithoutExtension
                                val stateAutoPng = File(saveDir, "$stem.state.auto.png")
                                stateAutoPng.exists() && stateAutoPng.isFile
                            } else false
                        } else false
                    }
                    val popUpAspectRatio = if (saveFileImageExists) {
                        val ratioStr = actualSystem?.saveAspectRatio ?: "Auto"
                        when (ratioStr) {
                            "4:3" -> 4f / 3f
                            "3:2" -> 3f / 2f
                            "16:9" -> 16f / 9f
                            "10:9" -> 10f / 9f
                            "1:1" -> 1f
                            else -> 1f
                        }
                    } else {
                        1f
                    }

                    val popUpAlignment = when (displaySettings.romIconPopUpAlignment) {
                        "top_left" -> Alignment.TopStart
                        "top_center" -> Alignment.TopCenter
                        "top_right" -> Alignment.TopEnd
                        "middle_left" -> Alignment.CenterStart
                        "middle_center" -> Alignment.Center
                        "middle_right" -> Alignment.CenterEnd
                        "bottom_left" -> Alignment.BottomStart
                        "bottom_center" -> Alignment.BottomCenter
                        "bottom_right" -> Alignment.BottomEnd
                        else -> Alignment.Center
                    }

                    val popUpWidthPercent = (displaySettings.romIconPopUpWidthPercent / 100f).coerceIn(0.05f, 1.0f)
                    val popUpWidth = selectionAreaWidth * popUpWidthPercent

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = popUpAlignment
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                            tonalElevation = 8.dp,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .width(popUpWidth)
                                .aspectRatio(popUpAspectRatio)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val resolvedIcon = remember(game, currentSystem, customIcons) {
                                    GameIconResolver.resolveRomIcon(game, currentSystem, customIcons, allSystems)
                                }
                                val isAndroidApp = game.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                                        actualSystem?.defaultLaunchMode == "ANDROID_APP" ||
                                        game.systemId.startsWith("android_")

                                if (isAndroidApp) {
                                    val customIcon = customIcons[game.filePath]
                                    if (!customIcon.isNullOrBlank() && (File(customIcon).exists() || customIcon.startsWith("/") || customIcon.startsWith("http"))) {
                                        UniversalIconView(
                                            iconNameOrPath = customIcon,
                                            contentDescription = game.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        val appIcon = remember(game.filePath) {
                                            AndroidAppIconHelper.getAppIconBitmap(context, game.filePath)
                                        }
                                        if (appIcon != null) {
                                            Image(
                                                bitmap = appIcon,
                                                contentDescription = game.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            UniversalIconView(
                                                iconNameOrPath = "android",
                                                contentDescription = game.title,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                } else {
                                    UniversalIconView(
                                        iconNameOrPath = resolvedIcon,
                                        contentDescription = game.title,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = if (saveFileImageExists && actualSystem?.saveAspectRatio != "Auto") {
                                            androidx.compose.ui.layout.ContentScale.FillBounds
                                        } else {
                                            androidx.compose.ui.layout.ContentScale.Fit
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showStyleDialog) {
            ListStyleSettingsDialog(
                settings = listSettings,
                onDismiss = { showStyleDialog = false },
                onUpdateSettings = onUpdateListSettings,
                onApplyToActiveSystem = onApplyToActiveSystem,
                onApplyToAllSystems = onApplyToAllSystems,
                activeSystemName = currentSystem?.name,
                onOpenAppVisibility = onOpenAppVisibilityClick,
                isAndroidAppsSystem = currentSystem?.id == "android_apps"
            )
        }

        if (showDirectoryPicker) {
            DirectoryPickerDialog(
                initialPath = currentSystem?.folderPath ?: "/storage/emulated/0",
                onDismiss = { showDirectoryPicker = false },
                onDirectorySelected = { selectedPath ->
                    if (onUpdateSystemFolder != null) {
                        onUpdateSystemFolder(selectedPath)
                    }
                }
            )
        }
    }
}
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridCardItem(
    folderName: String,
    folderFullPath: String? = null,
    system: SystemEntity? = null,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val folderArtPath = remember(folderName, folderFullPath, system?.boxartFolderPath) {
        val exts = listOf(".png", ".jpg", ".jpeg", ".webp", ".PNG", ".JPG", ".JPEG", ".WEBP")
        if (!system?.boxartFolderPath.isNullOrBlank()) {
            val mediaDir = File(system!!.boxartFolderPath)
            if (mediaDir.exists() && mediaDir.isDirectory) {
                for (ext in exts) {
                    val candidate = File(mediaDir, "$folderName$ext")
                    if (candidate.exists() && candidate.isFile) return@remember candidate.absolutePath
                }
            }
        }
        if (!folderFullPath.isNullOrBlank()) {
            val dir = File(folderFullPath)
            if (dir.exists() && dir.isDirectory) {
                val names = listOf(folderName, "folder", "cover", "icon", "artwork")
                for (name in names) {
                    for (ext in exts) {
                        val candidate = File(dir, "$name$ext")
                        if (candidate.exists() && candidate.isFile) return@remember candidate.absolutePath
                    }
                }
            }
        }
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (folderArtPath != null) {
                AsyncImage(
                    model = folderArtPath,
                    contentDescription = folderName,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            MarqueeText(
                text = folderName,
                isFocused = isFocused,
                marqueeSpeed = marqueeSpeed,
                marqueeDelayMillis = marqueeDelayMillis,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Subfolder",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderListRowItem(
    folderName: String,
    folderFullPath: String? = null,
    system: SystemEntity? = null,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val folderArtPath = remember(folderName, folderFullPath, system?.boxartFolderPath) {
        val exts = listOf(".png", ".jpg", ".jpeg", ".webp", ".PNG", ".JPG", ".JPEG", ".WEBP")
        if (!system?.boxartFolderPath.isNullOrBlank()) {
            val mediaDir = File(system!!.boxartFolderPath)
            if (mediaDir.exists() && mediaDir.isDirectory) {
                for (ext in exts) {
                    val candidate = File(mediaDir, "$folderName$ext")
                    if (candidate.exists() && candidate.isFile) return@remember candidate.absolutePath
                }
            }
        }
        if (!folderFullPath.isNullOrBlank()) {
            val dir = File(folderFullPath)
            if (dir.exists() && dir.isDirectory) {
                val names = listOf(folderName, "folder", "cover", "icon", "artwork")
                for (name in names) {
                    for (ext in exts) {
                        val candidate = File(dir, "$name$ext")
                        if (candidate.exists() && candidate.isFile) return@remember candidate.absolutePath
                    }
                }
            }
        }
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (folderArtPath != null) {
                AsyncImage(
                    model = folderArtPath,
                    contentDescription = folderName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = folderName,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Subfolder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderTextOnlyItem(
    folderName: String,
    settings: RomListSettings,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = settings.marginDp.dp, vertical = (settings.marginDp / 2).dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showArtworkInTextOnly) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((settings.textSizeSp * 1.2f).dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            MarqueeText(
                text = folderName,
                isFocused = isFocused,
                marqueeSpeed = marqueeSpeed,
                marqueeDelayMillis = marqueeDelayMillis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = settings.textSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameGridCardItem(
    game: GameRomEntity,
    system: SystemEntity?,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    gridScalePercent: Int = 100,
    showDetails: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCompletedClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    allSystems: List<SystemEntity> = emptyList(),
    customIcons: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    val displaySettings = LocalDisplaySettings.current
    val cardHeightScale = (gridScalePercent / 100f).coerceIn(0.5f, 2.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp * cardHeightScale)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val resolvedIcon = remember(game, system, customIcons) {
                GameIconResolver.resolveRomIcon(game, system, customIcons, allSystems)
            }
            val actualSystem = remember(game, system, allSystems) {
                if (system?.id == "favorites" || system?.id == "recently_played") {
                    allSystems.firstOrNull { it.id == game.systemId }
                } else {
                    system
                }
            }
            val isAndroidApp = game.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                    actualSystem?.defaultLaunchMode == "ANDROID_APP" ||
                    game.systemId.startsWith("android_")

            if (isAndroidApp) {
                val customIcon = customIcons[game.filePath]
                if (!customIcon.isNullOrBlank() && (File(customIcon).exists() || customIcon.startsWith("/") || customIcon.startsWith("http"))) {
                    UniversalIconView(
                        iconNameOrPath = customIcon,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val appIcon = remember(game.filePath) {
                        AndroidAppIconHelper.getAppIconBitmap(context, game.filePath)
                    }
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = game.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } else {
                        UniversalIconView(
                            iconNameOrPath = "android",
                            contentDescription = game.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    }
                }
            } else {
                val isExternal = remember(resolvedIcon) {
                    resolvedIcon.startsWith("/") || resolvedIcon.startsWith("http") || File(resolvedIcon).exists()
                }
                UniversalIconView(
                    iconNameOrPath = resolvedIcon,
                    contentDescription = game.title,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = if (isExternal) Modifier.fillMaxSize() else Modifier.align(Alignment.Center).size(48.dp)
                )
            }

            // Info Button (Top Left corner)
            if (displaySettings.showRomDetailsButton) {
                IconButton(
                    onClick = { onInfoClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Top Right Action Buttons (Favorite / Complete / Delete)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from Recent",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (displaySettings.showRomFavoriteButton) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (game.isFavorite) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (displaySettings.showRomCompleteButton) {
                    IconButton(
                        onClick = onCompletedClick,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (game.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete",
                            tint = if (game.isCompleted) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            val cleanedTitle = getCleanedTitle(game.title, displaySettings.removeCharsFromGameNames)
            MarqueeText(
                text = cleanedTitle,
                isFocused = isFocused,
                marqueeSpeed = marqueeSpeed,
                marqueeDelayMillis = marqueeDelayMillis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (showDetails) {
                val subtitleText = if ((system?.id == "favorites" || system?.id == "recently_played") && game.systemId != "android_apps") {
                    "${game.systemId.uppercase()} • ${game.fileName}"
                } else {
                    game.fileName
                }
                Text(
                    text = subtitleText,
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
fun GameListRowItem(
    game: GameRomEntity,
    system: SystemEntity?,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    showDetails: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCompletedClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    allSystems: List<SystemEntity> = emptyList(),
    customIcons: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    val displaySettings = LocalDisplaySettings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val resolvedIcon = remember(game, system, customIcons) {
                    GameIconResolver.resolveRomIcon(game, system, customIcons, allSystems)
                }
                val actualSystem = remember(game, system, allSystems) {
                    if (system?.id == "favorites" || system?.id == "recently_played") {
                        allSystems.firstOrNull { it.id == game.systemId }
                    } else {
                        system
                    }
                }
                val isAndroidApp = game.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                        actualSystem?.defaultLaunchMode == "ANDROID_APP" ||
                        game.systemId.startsWith("android_")

                if (isAndroidApp) {
                    val customIcon = customIcons[game.filePath]
                    if (!customIcon.isNullOrBlank() && (File(customIcon).exists() || customIcon.startsWith("/") || customIcon.startsWith("http"))) {
                        UniversalIconView(
                            iconNameOrPath = customIcon,
                            contentDescription = game.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val appIcon = remember(game.filePath) {
                            AndroidAppIconHelper.getAppIconBitmap(context, game.filePath)
                        }
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = game.title,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    UniversalIconView(
                        iconNameOrPath = resolvedIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val cleanedTitle = getCleanedTitle(game.title, displaySettings.removeCharsFromGameNames)
                MarqueeText(
                    text = cleanedTitle,
                    isFocused = isFocused,
                    marqueeSpeed = marqueeSpeed,
                    marqueeDelayMillis = marqueeDelayMillis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (showDetails) {
                    val subtitleText = if ((system?.id == "favorites" || system?.id == "recently_played") && game.systemId != "android_apps") {
                        "${game.systemId.uppercase()} • ${game.fileName}"
                    } else {
                        game.fileName
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onInfoClick != null && displaySettings.showRomDetailsButton) {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from Recent",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (displaySettings.showRomFavoriteButton) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (game.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (displaySettings.showRomCompleteButton) {
                IconButton(onClick = onCompletedClick) {
                    Icon(
                        imageVector = if (game.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Complete",
                        tint = if (game.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameTextOnlyItem(
    game: GameRomEntity,
    system: SystemEntity?,
    settings: RomListSettings,
    isFocused: Boolean = false,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCompletedClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    allSystems: List<SystemEntity> = emptyList(),
    customIcons: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    val displaySettings = LocalDisplaySettings.current

    val textAlign = when (settings.textAlignment) {
        TextAlignmentOption.START -> TextAlign.Start
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.END -> TextAlign.End
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = settings.marginDp.dp, vertical = (settings.marginDp / 2).dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settings.showArtworkInTextOnly) {
                val resolvedIcon = remember(game, system, customIcons) {
                    GameIconResolver.resolveRomIcon(game, system, customIcons, allSystems)
                }
                val actualSystem = remember(game, system, allSystems) {
                    if (system?.id == "favorites" || system?.id == "recently_played") {
                        allSystems.firstOrNull { it.id == game.systemId }
                    } else {
                        system
                    }
                }
                val isAndroidApp = game.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                        actualSystem?.defaultLaunchMode == "ANDROID_APP" ||
                        game.systemId.startsWith("android_")

                if (isAndroidApp) {
                    val customIcon = customIcons[game.filePath]
                    if (!customIcon.isNullOrBlank() && (File(customIcon).exists() || customIcon.startsWith("/") || customIcon.startsWith("http"))) {
                        UniversalIconView(
                            iconNameOrPath = customIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size((settings.textSizeSp * 1.2f).dp)
                        )
                    } else {
                        val appIcon = remember(game.filePath) {
                            AndroidAppIconHelper.getAppIconBitmap(context, game.filePath)
                        }
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size((settings.textSizeSp * 1.2f).dp)
                            )
                        } else {
                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size((settings.textSizeSp * 1.2f).dp))
                        }
                    }
                } else {
                    UniversalIconView(
                        iconNameOrPath = resolvedIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size((settings.textSizeSp * 1.2f).dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            val displaySettings = LocalDisplaySettings.current
            val cleanedTitle = getCleanedTitle(game.title, displaySettings.removeCharsFromGameNames)
            MarqueeText(
                text = cleanedTitle,
                isFocused = isFocused,
                marqueeSpeed = marqueeSpeed,
                marqueeDelayMillis = marqueeDelayMillis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = settings.textSizeSp.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = textAlign,
                modifier = Modifier.weight(1f)
            )

            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size((settings.textSizeSp * 1.2f).dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from Recent",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = android.graphics.Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun getCleanedTitle(title: String, removeChars: String): String {
    if (removeChars.isEmpty()) return title
    var cleaned = title
    for (char in removeChars) {
        cleaned = cleaned.replace(char.toString(), "")
    }
    return cleaned
}
