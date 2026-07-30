package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

fun Modifier.lazyListScrollbar(
    state: LazyListState,
    autoHide: Boolean = true,
    showDurationMs: Long = 1500L,
    color: Color = Color.White.copy(alpha = 0.6f),
    trackColor: Color = Color.Black.copy(alpha = 0.2f),
    width: Dp = 6.dp
): Modifier = composed {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(state.isScrollInProgress, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, autoHide, showDurationMs) {
        if (!autoHide) {
            isVisible = true
        } else {
            isVisible = true
            if (!state.isScrollInProgress) {
                delay(showDurationMs.coerceAtLeast(100L))
                isVisible = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "ScrollbarAlpha"
    )

    drawWithContent {
        drawContent()
        if (alpha <= 0.01f) return@drawWithContent
        val totalItems = state.layoutInfo.totalItemsCount
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (totalItems > 0 && visibleItems.isNotEmpty() && visibleItems.size < totalItems) {
            val firstVisibleIndex = visibleItems.first().index
            val visibleItemCount = visibleItems.size
            val canvasHeight = size.height
            val trackWidthPx = width.toPx()

            // Draw track
            drawRoundRect(
                color = trackColor.copy(alpha = trackColor.alpha * alpha),
                topLeft = Offset(size.width - trackWidthPx, 0f),
                size = Size(trackWidthPx, canvasHeight),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )

            // Draw thumb
            val thumbHeight = (canvasHeight * (visibleItemCount.toFloat() / totalItems)).coerceAtLeast(36.dp.toPx())
            val thumbOffsetY = ((canvasHeight - thumbHeight) * (firstVisibleIndex.toFloat() / (totalItems - visibleItemCount).coerceAtLeast(1)))

            drawRoundRect(
                color = color.copy(alpha = color.alpha * alpha),
                topLeft = Offset(size.width - trackWidthPx, thumbOffsetY.coerceIn(0f, canvasHeight - thumbHeight)),
                size = Size(trackWidthPx, thumbHeight),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )
        }
    }
}

fun Modifier.lazyGridScrollbar(
    state: LazyGridState,
    autoHide: Boolean = true,
    showDurationMs: Long = 1500L,
    color: Color = Color.White.copy(alpha = 0.6f),
    trackColor: Color = Color.Black.copy(alpha = 0.2f),
    width: Dp = 6.dp
): Modifier = composed {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(state.isScrollInProgress, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, autoHide, showDurationMs) {
        if (!autoHide) {
            isVisible = true
        } else {
            isVisible = true
            if (!state.isScrollInProgress) {
                delay(showDurationMs.coerceAtLeast(100L))
                isVisible = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "GridScrollbarAlpha"
    )

    drawWithContent {
        drawContent()
        if (alpha <= 0.01f) return@drawWithContent
        val totalItems = state.layoutInfo.totalItemsCount
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (totalItems > 0 && visibleItems.isNotEmpty() && visibleItems.size < totalItems) {
            val firstVisibleIndex = visibleItems.first().index
            val visibleItemCount = visibleItems.size
            val canvasHeight = size.height
            val trackWidthPx = width.toPx()

            // Draw track
            drawRoundRect(
                color = trackColor.copy(alpha = trackColor.alpha * alpha),
                topLeft = Offset(size.width - trackWidthPx, 0f),
                size = Size(trackWidthPx, canvasHeight),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )

            // Draw thumb
            val thumbHeight = (canvasHeight * (visibleItemCount.toFloat() / totalItems)).coerceAtLeast(36.dp.toPx())
            val thumbOffsetY = ((canvasHeight - thumbHeight) * (firstVisibleIndex.toFloat() / (totalItems - visibleItemCount).coerceAtLeast(1)))

            drawRoundRect(
                color = color.copy(alpha = color.alpha * alpha),
                topLeft = Offset(size.width - trackWidthPx, thumbOffsetY.coerceIn(0f, canvasHeight - thumbHeight)),
                size = Size(trackWidthPx, thumbHeight),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )
        }
    }
}
