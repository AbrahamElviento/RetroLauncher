package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DisplayMode
import com.example.data.model.DisplaySettings

// CompositionLocal to distribute DisplaySettings to all nested dialogs and pickers
val LocalDisplaySettings = staticCompositionLocalOf { DisplaySettings() }

@Composable
fun ScaledDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable BoxScope.() -> Unit
) {
    val displaySettings = LocalDisplaySettings.current
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        // Ensure CompositionLocal propagates inside the Dialog's sub-window
        CompositionLocalProvider(LocalDisplaySettings provides displaySettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
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
                            contentAlignment = Alignment.Center,
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
                                    ),
                                contentAlignment = Alignment.Center,
                                content = content
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    DisplayMode.LOWER_HALF -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth())
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(
                                        top = displaySettings.marginTopDp.dp,
                                        bottom = displaySettings.marginBottomDp.dp,
                                        start = displaySettings.marginLeftDp.dp,
                                        end = displaySettings.marginRightDp.dp
                                    ),
                                contentAlignment = Alignment.Center,
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
                                contentAlignment = Alignment.Center,
                                content = content
                            )
                        }
                    }
                }
            }
        }
    }
}
