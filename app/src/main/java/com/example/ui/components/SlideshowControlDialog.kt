package com.example.ui.components

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.data.model.DisplaySettings
import com.example.ui.components.LocalDisplaySettings
import com.example.util.SlideshowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun SlideshowControlDialog(
    systemName: String,
    enableImmersiveMode: Boolean = true,
    showStatusInFullScreen: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by SlideshowManager.isPlaying.collectAsState()
    val currentImageIndex by SlideshowManager.currentIndex.collectAsState()
    val imageFiles by SlideshowManager.imageFiles.collectAsState()

    var isFullScreen by remember { mutableStateOf(false) }
    var showOverlays by remember { mutableStateOf(true) }
    var activityTrigger by remember { mutableStateOf(0L) }

    // Live Battery state
    var batteryPercent by remember { mutableIntStateOf(100) }
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
            } catch (_: Exception) {}
        }
    }

    // Live Clock state for full screen
    var currentTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(isFullScreen) {
        if (isFullScreen) {
            val is24Hr = android.text.format.DateFormat.is24HourFormat(context)
            val pattern = if (is24Hr) "HH:mm" else "hh:mm a"
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            while (true) {
                currentTimeStr = sdf.format(Date())
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

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

        val displaySettings = LocalDisplaySettings.current
        val density = LocalDensity.current

        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                decorFitsSystemWindows = false
            )
        ) {
            val view = LocalView.current
            DisposableEffect(view, displaySettings) {
                var parent = view.parent
                while (parent != null && parent !is DialogWindowProvider) {
                    parent = parent.parent
                }
                (parent as? DialogWindowProvider)?.window?.let { window ->
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    window.setBackgroundDrawableResource(android.R.color.black)
                    window.setDimAmount(0f)
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    WindowCompat.setDecorFitsSystemWindows(window, false)

                    // Clear any window margins/insets or background padding on the dialog decor (absolute 0 margin)
                    window.decorView.setPadding(0, 0, 0, 0)
                    window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    window.addFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                    )
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
                }

                // Temporarily hide system UI on host activity window
                val activity = context.findActivity()
                activity?.window?.let { actWindow ->
                    WindowCompat.setDecorFitsSystemWindows(actWindow, false)
                    val actController = WindowInsetsControllerCompat(actWindow, actWindow.decorView)
                    actController.hide(WindowInsetsCompat.Type.systemBars())
                    actController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    @Suppress("DEPRECATION")
                    actWindow.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
                }

                onDispose {
                    // Remember and restore host activity's immersive state accurately
                    val act = context.findActivity()
                    act?.window?.let { actWindow ->
                        val actController = WindowInsetsControllerCompat(actWindow, actWindow.decorView)
                        if (enableImmersiveMode) {
                            WindowCompat.setDecorFitsSystemWindows(actWindow, false)
                            actController.hide(WindowInsetsCompat.Type.systemBars())
                            actController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            WindowCompat.setDecorFitsSystemWindows(actWindow, true)
                            actController.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                // Extra 10% top & 10% bottom pure black background overlay (120% height layout)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layout { measurable, constraints ->
                            val extraHeight = (constraints.maxHeight * 0.20f).toInt()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minHeight = constraints.maxHeight + extraHeight,
                                    maxHeight = constraints.maxHeight + extraHeight
                                )
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeRelative(0, -(extraHeight / 2))
                            }
                        }
                        .background(Color.Black)
                )
                AsyncImage(
                    model = currentFile,
                    contentDescription = "Full Screen Slideshow Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Status Overlay in Top Bar (Battery on top-left, Clock on top-right when controls are hidden)
                AnimatedVisibility(
                    visible = !showOverlays && showStatusInFullScreen,
                    enter = fadeIn(animationSpec = tween(600)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Battery level on the top left
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val batteryIcon = if (isCharging) {
                                Icons.Default.BatteryChargingFull
                            } else if (batteryPercent <= 15) {
                                Icons.Default.BatteryAlert
                            } else {
                                Icons.Default.BatteryFull
                            }
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = "Battery Level",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$batteryPercent%",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Clock on the top right
                        Text(
                            text = currentTimeStr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

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
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
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
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
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
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
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
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
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
                        var isPrevFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .onFocusChanged { isPrevFocused = it.isFocused }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPrevFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isPrevFocused) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = imageFiles.isNotEmpty()) {
                                    SlideshowManager.prev()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Image",
                                tint = if (isPrevFocused) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (imageFiles.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play / Pause Button
                        var isPlayFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .onFocusChanged { isPlayFocused = it.isFocused }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlayFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isPlayFocused) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = imageFiles.isNotEmpty()) {
                                    SlideshowManager.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Slideshow" else "Play Slideshow",
                                tint = if (isPlayFocused) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (imageFiles.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next Button
                        var isNextFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .onFocusChanged { isNextFocused = it.isFocused }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isNextFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isNextFocused) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = imageFiles.isNotEmpty()) {
                                    SlideshowManager.next()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Image",
                                tint = if (isNextFocused) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (imageFiles.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // View Full Screen Button
                        var isFullFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .onFocusChanged { isFullFocused = it.isFocused }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFullFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isFullFocused) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = imageFiles.isNotEmpty()) {
                                    isFullScreen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "View Full Screen",
                                tint = if (isFullFocused) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (imageFiles.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
