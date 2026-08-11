package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DisplayMode
import com.example.data.model.DisplaySettings

@Composable
fun CanvasLayoutWrapper(
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (displaySettings.mode) {
            DisplayMode.FULL_SCREEN -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
            }
            DisplayMode.TOP_HALF -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f),
                        content = content
                    )
                    // Secondary area indicator - total black
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(androidx.compose.ui.graphics.Color.Black)
                    )
                }
            }
            DisplayMode.LOWER_HALF -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top area secondary placeholder - total black
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(androidx.compose.ui.graphics.Color.Black)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        content = content
                    )
                }
            }
            DisplayMode.CUSTOM_SIZE -> {
                val widthDp = with(density) { displaySettings.customWidthPx.toDp() }
                val heightDp = with(density) { displaySettings.customHeightPx.toDp() }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = widthDp)
                            .heightIn(max = heightDp)
                            .padding(
                                top = displaySettings.marginTopDp.dp,
                                bottom = displaySettings.marginBottomDp.dp,
                                start = displaySettings.marginLeftDp.dp,
                                end = displaySettings.marginRightDp.dp
                            ),
                        content = content
                    )
                }
            }
        }
    }
}
