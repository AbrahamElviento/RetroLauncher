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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = displaySettings.marginTopDp.dp,
                            bottom = displaySettings.marginBottomDp.dp,
                            start = displaySettings.marginLeftDp.dp,
                            end = displaySettings.marginRightDp.dp
                        ),
                    content = content
                )
            }
            DisplayMode.TOP_HALF -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .padding(
                                top = displaySettings.marginTopDp.dp,
                                bottom = displaySettings.marginBottomDp.dp,
                                start = displaySettings.marginLeftDp.dp,
                                end = displaySettings.marginRightDp.dp
                            )
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        content = content
                    )
                    // Secondary area indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TOP HALF CANVAS ACTIVE\n(Lower screen available for secondary apps/stats)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
            DisplayMode.LOWER_HALF -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top area secondary placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOWER HALF CANVAS ACTIVE\n(Top screen available for primary view/video)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(
                                top = displaySettings.marginTopDp.dp,
                                bottom = displaySettings.marginBottomDp.dp,
                                start = displaySettings.marginLeftDp.dp,
                                end = displaySettings.marginRightDp.dp
                            )
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                        content = content
                    )
                }
            }
        }
    }
}
