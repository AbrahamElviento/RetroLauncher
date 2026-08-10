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
import androidx.compose.foundation.lazy.LazyListState
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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.platform.LocalContext

enum class SystemDisplayStyle {
    ICON_GRID,
    GRID_ALT,
    TEXT_LIST
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
    bottomSystemMainMenuTitle: Boolean = false,
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
                        },
                        contentDescription = "Toggle Grid / List Mode",
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
                                                onDoubleTapClock = onDoubleTapClock,
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
                                                descTextSizeSp = descTextSizeSp,
                                                textAlignment = textAlignment,
                                                marqueeSpeed = marqueeSpeed,
                                                marqueeDelayMillis = marqueeDelayMillis,
                                                totalGamesCount = totalGamesCount,
                                                totalFavoritesCount = totalFavoritesCount,
                                                totalCompletedCount = totalCompletedCount,
                                                onDoubleTapClock = onDoubleTapClock,
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
                                onDoubleTapClock = onDoubleTapClock,
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

        if (showMainMenuTitle && bottomSystemMainMenuTitle) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            headerContent(Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    }
}

@Composable
fun ClockWidgetContent(system: SystemEntity, isFocused: Boolean, style: String, onDoubleTapClock: (() -> Unit)? = null, descTextSizeSp: Int = 12) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onDoubleTapClock?.invoke()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UniversalIconView(
            iconNameOrPath = system.iconName.ifBlank { "clock" },
            tint = parseColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentTime,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = parseColor),
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
        if (system.name != "Clock Widget" && system.name.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = system.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = parseColor),
                maxLines = 1
            )
        }
    }
}

@Composable
fun BatteryWidgetContent(system: SystemEntity, isFocused: Boolean, style: String, descTextSizeSp: Int = 12) {
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

    // Function to expand android status bar notifications panel on double tap
    fun expandNotifications() {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        expandNotifications()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Battery dynamic icon
        val batteryIcon = if (isCharging) {
            Icons.Default.BatteryChargingFull
        } else if (batteryPercent <= 15) {
            Icons.Default.BatteryAlert
        } else {
            Icons.Default.BatteryFull
        }

        Icon(
            imageVector = batteryIcon,
            contentDescription = "Battery Status",
            tint = parseColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$batteryPercent%",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = parseColor),
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

        if (system.name != "Battery Widget" && system.name.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = system.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = parseColor),
                maxLines = 1
            )
        }
    }
}

@Composable
fun AppLaunchWidgetContent(system: SystemEntity, isFocused: Boolean, style: String, descTextSizeSp: Int = 12) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "App Icon",
                modifier = Modifier.size(if (style == "GRID_ALT") 32.dp else 44.dp)
            )
        } else {
            UniversalIconView(
                iconNameOrPath = system.iconName.ifBlank { "android" },
                tint = parseColor,
                modifier = Modifier.size(if (style == "GRID_ALT") 32.dp else 44.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = system.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = parseColor),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val appName = if (system.folderPath.isNotEmpty()) {
            system.folderPath.split("|").first().split(".").lastOrNull() ?: ""
        } else {
            ""
        }
        val showLaunchText = system.retroArchCore != "false"
        if (appName.isNotEmpty() && showLaunchText) {
            Text(
                text = "Launch",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = descTextSizeSp.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BgmWidgetContent(system: SystemEntity, isFocused: Boolean, style: String, descTextSizeSp: Int = 12) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val currentTrack by com.example.util.SoundManager.currentTrackName.collectAsState()
    val isPlaying by com.example.util.SoundManager.isPlayingState.collectAsState()
    val hasBgmFiles by com.example.util.SoundManager.hasBgmFiles.collectAsState()
    val displaySettings = LocalDisplaySettings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UniversalIconView(
            iconNameOrPath = system.iconName.ifBlank { "music_note" },
            tint = parseColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val trackTitle = if (!displaySettings.enableBgm) {
            "BGM stopped"
        } else if (hasBgmFiles) {
            currentTrack ?: "BGM Active"
        } else {
            "No BGM files"
        }

        Text(
            text = trackTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = parseColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        val actionText = system.allowedExtensions.ifBlank { "Tap to view" }
        val isActuallyPlaying = isPlaying && displaySettings.enableBgm
        Text(
            text = if (isActuallyPlaying) "Playing • $actionText" else "Stopped • $actionText",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = descTextSizeSp.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
fun CustomTextWidgetContent(system: SystemEntity, isFocused: Boolean, style: String) {
    val parseColor = runCatching {
        Color(android.graphics.Color.parseColor(system.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val customText = system.retroArchCore.ifBlank { "No text configured." }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UniversalIconView(
            iconNameOrPath = system.iconName.ifBlank { "pencil" },
            tint = parseColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = customText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (system.name.isNotBlank() && system.name != "Custom Text") {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = system.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = parseColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    onDoubleTapClock: (() -> Unit)? = null,
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
                        "WIDGET_CLOCK" -> ClockWidgetContent(system = system, isFocused = isFocused, style = "GRID", onDoubleTapClock = onDoubleTapClock, descTextSizeSp = descTextSizeSp)
                        "WIDGET_TOTAL_GAMES" -> TotalGamesWidgetContent(system = system, totalGamesCount = totalGamesCount, totalFavoritesCount = totalFavoritesCount, totalCompletedCount = totalCompletedCount, isFocused = isFocused, style = "GRID")
                        "WIDGET_CUSTOM_TEXT" -> CustomTextWidgetContent(system = system, isFocused = isFocused, style = "GRID")
                        "WIDGET_SLIDESHOW" -> SlideshowWidgetContent(system = system, isFocused = isFocused, style = "GRID")
                        "WIDGET_BGM" -> BgmWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp)
                        "WIDGET_BATTERY" -> BatteryWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp)
                        "WIDGET_APP_LAUNCH" -> AppLaunchWidgetContent(system = system, isFocused = isFocused, style = "GRID", descTextSizeSp = descTextSizeSp)
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
    onDoubleTapClock: (() -> Unit)? = null,
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
                onLongClick = onLongClick
            )
    ) {
        if (isWidget) {
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
                            "WIDGET_CUSTOM_TEXT" -> "Custom Text Widget"
                            "WIDGET_SLIDESHOW" -> "Image Slideshow Widget"
                            "WIDGET_BGM" -> "BGM Music Widget"
                            "WIDGET_BATTERY" -> "Battery Status Widget"
                            "WIDGET_APP_LAUNCH" -> "App Launcher Widget"
                            else -> "Widget"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = descTextSizeSp.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = parseColor),
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        onDoubleTapClock?.invoke()
                                    }
                                )
                            }
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
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        expandNotifications()
                                    }
                                )
                            }
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
                        val context = LocalContext.current
                        val useAppIcon = system.allowedExtensions == "true"
                        val bitmap = if (useAppIcon && system.folderPath.isNotEmpty()) {
                            remember(system.folderPath) {
                                AndroidAppIconHelper.getAppIconBitmap(context, system.folderPath)
                            }
                        } else {
                            null
                        }

                        val showLaunchText = system.retroArchCore != "false"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap,
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                UniversalIconView(
                                    iconNameOrPath = system.iconName.ifEmpty { "android" },
                                    tint = parseColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            if (showLaunchText) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Launch App",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = parseColor, fontSize = descTextSizeSp.sp)
                                )
                            }
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
    onDoubleTapClock: (() -> Unit)? = null,
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
                        "WIDGET_CLOCK" -> ClockWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", onDoubleTapClock = onDoubleTapClock, descTextSizeSp = descTextSizeSp)
                        "WIDGET_TOTAL_GAMES" -> TotalGamesWidgetContent(system = system, totalGamesCount = totalGamesCount, totalFavoritesCount = totalFavoritesCount, totalCompletedCount = totalCompletedCount, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_CUSTOM_TEXT" -> CustomTextWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_SLIDESHOW" -> SlideshowWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT")
                        "WIDGET_BGM" -> BgmWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp)
                        "WIDGET_BATTERY" -> BatteryWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp)
                        "WIDGET_APP_LAUNCH" -> AppLaunchWidgetContent(system = system, isFocused = isFocused, style = "GRID_ALT", descTextSizeSp = descTextSizeSp)
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
}
