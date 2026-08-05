package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import android.view.KeyEvent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.util.SlideshowManager

@Composable
fun SlideshowControlDialog(
    systemName: String,
    onDismiss: () -> Unit
) {
    val isPlaying by SlideshowManager.isPlaying.collectAsState()
    val currentImageIndex by SlideshowManager.currentIndex.collectAsState()
    val imageFiles by SlideshowManager.imageFiles.collectAsState()

    var isFullScreen by remember { mutableStateOf(false) }
    var showOverlays by remember { mutableStateOf(true) }
    var activityTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            showOverlays = true
            activityTrigger = System.currentTimeMillis()
        }
    }

    LaunchedEffect(activityTrigger, isFullScreen) {
        if (isFullScreen) {
            showOverlays = true
            kotlinx.coroutines.delay(4000L) // 4 seconds of inactivity
            showOverlays = false
        }
    }

    val currentFile = if (imageFiles.isNotEmpty()) {
        imageFiles.getOrNull(currentImageIndex) ?: imageFiles.first()
    } else {
        null
    }

    if (isFullScreen && currentFile != null) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        activityTrigger = System.currentTimeMillis()
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val keyCode = keyEvent.nativeKeyEvent.keyCode
                            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                SlideshowManager.prev()
                                true
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                SlideshowManager.next()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .clickable {
                        if (!showOverlays) {
                            activityTrigger = System.currentTimeMillis()
                        } else {
                            isFullScreen = false
                        }
                    }
            ) {
                AsyncImage(
                    model = currentFile,
                    contentDescription = "Full Screen Slideshow Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Close Button in top right
                AnimatedVisibility(
                    visible = showOverlays,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Full Screen",
                            tint = Color.White
                        )
                    }
                }

                // Prev/Next semi-transparent overlay buttons on the sides
                AnimatedVisibility(
                    visible = showOverlays,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            activityTrigger = System.currentTimeMillis()
                            SlideshowManager.prev()
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Image",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showOverlays,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            activityTrigger = System.currentTimeMillis()
                            SlideshowManager.next()
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Image",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Slideshow Controller",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog"
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Image Preview Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentFile != null) {
                            Crossfade(
                                targetState = currentFile,
                                animationSpec = tween(500),
                                label = "dialog_slideshow"
                            ) { file ->
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Slideshow Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "No Images",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No images found in folder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Track / Image info
                    if (imageFiles.isNotEmpty()) {
                        Text(
                            text = "Image ${currentImageIndex + 1} of ${imageFiles.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Playback and View Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Previous Button
                        IconButton(
                            onClick = { SlideshowManager.prev() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                            enabled = imageFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Image",
                                tint = if (imageFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play / Pause Button
                        IconButton(
                            onClick = { SlideshowManager.togglePlayPause() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp)),
                            enabled = imageFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Slideshow" else "Play Slideshow",
                                tint = if (imageFiles.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next Button
                        IconButton(
                            onClick = { SlideshowManager.next() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)),
                            enabled = imageFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Image",
                                tint = if (imageFiles.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // View Full Screen Button
                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(24.dp)),
                            enabled = imageFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "View Full Screen",
                                tint = if (imageFiles.isNotEmpty()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
