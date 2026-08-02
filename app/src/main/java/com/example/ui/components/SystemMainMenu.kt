package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SystemEntity
import kotlinx.coroutines.launch

enum class SystemDisplayStyle {
    ICON_GRID,
    GRID_ALT,
    TEXT_LIST
}

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
    showMainMenuTitle: Boolean = true,
    showEditIcon: Boolean = false,
    defaultDisplayStyle: String = "ICON_GRID",
    displayColumns: Int = 4,
    displayRows: Int = 4,
    actualColumns: Int = 4,
    actualRows: Int = 4,
    textSizeSp: Int = 16,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    dpadUpTrigger: Long = 0L,
    dpadDownTrigger: Long = 0L,
    dpadLeftTrigger: Long = 0L,
    dpadRightTrigger: Long = 0L,
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
    modifier: Modifier = Modifier
) {
    var displayStyle by remember(defaultDisplayStyle) {
        mutableStateOf(
            when (defaultDisplayStyle) {
                "GRID_ALT" -> SystemDisplayStyle.GRID_ALT
                "TEXT_LIST" -> SystemDisplayStyle.TEXT_LIST
                else -> SystemDisplayStyle.ICON_GRID
            }
        )
    }

    // Auto-highlight last opened system or default to index 0
    var focusedIndex by remember(systems.size, selectedSystemId) {
        val idx = systems.indexOfFirst { it.id == selectedSystemId }
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

    // Ensure focusedIndex is valid
    val currentFocusedSystem = systems.getOrNull(focusedIndex)

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
                            SystemDisplayStyle.TEXT_LIST -> SystemDisplayStyle.ICON_GRID
                        }
                        val styleStr = when (displayStyle) {
                            SystemDisplayStyle.ICON_GRID -> "ICON_GRID"
                            SystemDisplayStyle.GRID_ALT -> "GRID_ALT"
                            SystemDisplayStyle.TEXT_LIST -> "TEXT_LIST"
                        }
                        onToggleDisplayStyle?.invoke(styleStr)
                    }
                }
            } else if (currentFocusedSystem != null) {
                onSelectAndEnterSystem(currentFocusedSystem)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (showMainMenuTitle) {
            // Main Menu Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                                else MaterialTheme.colorScheme.surfaceVariant
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
                                SystemDisplayStyle.TEXT_LIST -> SystemDisplayStyle.ICON_GRID
                            }
                            val styleStr = when (displayStyle) {
                                SystemDisplayStyle.ICON_GRID -> "ICON_GRID"
                                SystemDisplayStyle.GRID_ALT -> "GRID_ALT"
                                SystemDisplayStyle.TEXT_LIST -> "TEXT_LIST"
                            }
                            onToggleDisplayStyle?.invoke(styleStr)
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
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
                            },
                            contentDescription = "Toggle Grid / List Mode",
                            tint = if (isMenuFocused && isHeaderFocused && headerFocusedIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

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
            var lastHandledPageUp by remember { mutableLongStateOf(pageUpTrigger) }
            var lastHandledPageDown by remember { mutableLongStateOf(pageDownTrigger) }
            var lastHandledGoToTop by remember { mutableLongStateOf(goToTopTrigger) }
            var lastHandledGoToBottom by remember { mutableLongStateOf(goToBottomTrigger) }

            when (displayStyle) {
                SystemDisplayStyle.ICON_GRID, SystemDisplayStyle.GRID_ALT -> {
                    val iconScaleFactor = (mainMenuIconGridScalePercent / 100f).coerceIn(0.5f, 1.5f)
                    val numCols = maxOf(1, (maxWidth / (140.dp * iconScaleFactor)).toInt())
                    val gridState = remember(systems.size, selectedSystemId) {
                        val initialIdx = systems.indexOfFirst { it.id == selectedSystemId }
                        val safeIdx = if (initialIdx >= 0) initialIdx else 0
                        LazyGridState(firstVisibleItemIndex = maxOf(0, safeIdx - numCols))
                    }

                    fun scrollToCenter(index: Int) {
                        coroutineScope.launch {
                            val viewportHeight = gridState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            gridState.animateScrollToItem(index, scrollOffset = offset)
                        }
                    }

                    LaunchedEffect(gridState, focusedIndex) {
                        if (focusedIndex >= 0 && focusedIndex < systems.size) {
                            gridState.scrollToItem(focusedIndex)
                            val viewportHeight = gridState.layoutInfo.viewportSize.height
                            val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                            gridState.animateScrollToItem(focusedIndex, scrollOffset = offset)
                        }
                    }

                    LaunchedEffect(pageUpTrigger) {
                        if (pageUpTrigger > 0L && pageUpTrigger != lastHandledPageUp && systems.isNotEmpty()) {
                            lastHandledPageUp = pageUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = maxOf(0, focusedIndex - (numCols * 2))
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(pageDownTrigger) {
                        if (pageDownTrigger > 0L && pageDownTrigger != lastHandledPageDown && systems.isNotEmpty()) {
                            lastHandledPageDown = pageDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = minOf(systems.size - 1, focusedIndex + (numCols * 2))
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToTopTrigger) {
                        if (goToTopTrigger > 0L && goToTopTrigger != lastHandledGoToTop && systems.isNotEmpty()) {
                            lastHandledGoToTop = goToTopTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = 0
                            scrollToCenter(focusedIndex)
                        }
                    }
                    LaunchedEffect(goToBottomTrigger) {
                        if (goToBottomTrigger > 0L && goToBottomTrigger != lastHandledGoToBottom && systems.isNotEmpty()) {
                            lastHandledGoToBottom = goToBottomTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            focusedIndex = maxOf(0, systems.size - 1)
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
                    LaunchedEffect(dpadRightTrigger) {
                        if (dpadRightTrigger > 0L && dpadRightTrigger != lastHandledRight && systems.isNotEmpty()) {
                            lastHandledRight = dpadRightTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                headerFocusedIndex = minOf(1, headerFocusedIndex + 1)
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
                    LaunchedEffect(dpadUpTrigger) {
                        if (dpadUpTrigger > 0L && dpadUpTrigger != lastHandledUp && systems.isNotEmpty()) {
                            lastHandledUp = dpadUpTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                onMoveFocusUp()
                            } else if (showMainMenuTitle && focusedIndex < numCols) {
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
                    LaunchedEffect(dpadDownTrigger) {
                        if (dpadDownTrigger > 0L && dpadDownTrigger != lastHandledDown && systems.isNotEmpty()) {
                            lastHandledDown = dpadDownTrigger
                            com.example.util.SoundManager.playNavSound(enableNavigationSound, context, selectedSfxFileName)
                            if (isHeaderFocused) {
                                isHeaderFocused = false
                            } else if (focusedIndex + numCols < systems.size) {
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

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(numCols),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .lazyGridScrollbar(gridState, autoHide = autoHideScrollbar, showDurationMs = scrollbarShowDurationMs.toLong())
                    ) {
                        itemsIndexed(systems, key = { _, s -> s.id }) { index, system ->
                            val isFocused = isMenuFocused && !isHeaderFocused && index == focusedIndex
                            if (displayStyle == SystemDisplayStyle.GRID_ALT) {
                                SystemGridCardAlt(
                                    system = system,
                                    isFocused = isFocused,
                                    showEditIcon = showEditIcon,
                                    textSizeSp = textSizeSp,
                                    marqueeSpeed = marqueeSpeed,
                                    marqueeDelayMillis = marqueeDelayMillis,
                                    modifier = Modifier
                                        .padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp)
                                        .fillMaxWidth()
                                        .height(130.dp * iconScaleFactor),
                                    onClick = {
                                        isHeaderFocused = false
                                        focusedIndex = index
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
                                    textAlignment = textAlignment,
                                    marqueeSpeed = marqueeSpeed,
                                    marqueeDelayMillis = marqueeDelayMillis,
                                    modifier = Modifier
                                        .padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp)
                                        .fillMaxWidth()
                                        .height(110.dp * iconScaleFactor),
                                    onClick = {
                                        isHeaderFocused = false
                                        focusedIndex = index
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
                                textAlignment = textAlignment,
                                marqueeSpeed = marqueeSpeed,
                                marqueeDelayMillis = marqueeDelayMillis,
                                modifier = Modifier.padding(start = tileMarginLeftDp.dp, end = tileMarginRightDp.dp),
                                onClick = {
                                    isHeaderFocused = false
                                    focusedIndex = index
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
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
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

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (textSizeSp * 0.75f).sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = parseColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = system.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = parseColor, fontSize = (textSizeSp * 0.7f).sp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemTextRow(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    textAlignment: String = "LEFT",
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit = {},
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

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isFocused) BorderStroke(2.dp, parseColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (textSizeSp * 0.75f).sp),
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = parseColor, fontSize = (textSizeSp * 0.7f).sp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemGridCardAlt(
    system: SystemEntity,
    isFocused: Boolean,
    showEditIcon: Boolean = false,
    textSizeSp: Int = 16,
    marqueeSpeed: Int = 30,
    marqueeDelayMillis: Int = 1200,
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

    Card(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (textSizeSp * 0.75f).sp),
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
                            fontSize = (textSizeSp * 0.7f).sp
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
