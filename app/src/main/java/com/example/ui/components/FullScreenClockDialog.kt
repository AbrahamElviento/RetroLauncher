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
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.db.SystemEntity
import com.example.data.model.DisplaySettings
import com.example.ui.components.LocalDisplaySettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
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
fun FullScreenClockDialog(
    system: SystemEntity,
    enableImmersiveMode: Boolean = true,
    showStatusInFullScreen: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showOverlays by remember { mutableStateOf(true) }
    var activityTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    var dialogWindow by remember { mutableStateOf<Window?>(null) }
    val activity = remember(context) { context.findActivity() }

    // Screen brightness state initialized from current window or system setting
    val initialBrightness = remember {
        try {
            val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
            if (windowBrightness in 0.01f..1.0f) {
                windowBrightness
            } else {
                val cur = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
                )
                (cur / 255f).coerceIn(0.05f, 1.0f)
            }
        } catch (e: Exception) {
            0.8f
        }
    }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    val gestureBrightnessEnabled = remember(system.shortName) {
        system.shortName.contains("GESTURE_BRIGHTNESS")
    }

    var showBrightnessIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(currentBrightness) {
        if (gestureBrightnessEnabled) {
            showBrightnessIndicator = true
            delay(1200)
            showBrightnessIndicator = false
        }
    }

    // Apply brightness dynamically to both Dialog window and Activity window
    LaunchedEffect(currentBrightness, dialogWindow, activity) {
        val b = currentBrightness.coerceIn(0.01f, 1.0f)
        dialogWindow?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = b
            win.attributes = lp
        }
        activity?.window?.let { actWin ->
            val lp = actWin.attributes
            lp.screenBrightness = b
            actWin.attributes = lp
        }
    }

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

    // Live Date state for top-right status bar (d-M-yyyy formatted)
    var statusDateStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("d-M-yyyy", Locale.getDefault())
        while (true) {
            statusDateStr = sdf.format(Date())
            delay(1000L)
        }
    }

    // Auto-hide overlays after 4 seconds of inactivity
    LaunchedEffect(activityTrigger) {
        showOverlays = true
        delay(4000L)
        showOverlays = false
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        onDismiss()
    }

    val displaySettings = LocalDisplaySettings.current
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
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
                dialogWindow = window
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
                dialogWindow = null
                val act = context.findActivity()
                act?.window?.let { actWindow ->
                    // Restore default brightness on exit
                    val actLp = actWindow.attributes
                    actLp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    actWindow.attributes = actLp

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

        val containerModifier = if (gestureBrightnessEnabled) {
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    activityTrigger = System.currentTimeMillis()
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            activityTrigger = System.currentTimeMillis()
                        },
                        onDragEnd = {
                            activityTrigger = System.currentTimeMillis()
                        },
                        onDragCancel = {
                            activityTrigger = System.currentTimeMillis()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount.y / 600f
                            currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                            activityTrigger = System.currentTimeMillis()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showOverlays = !showOverlays
                            activityTrigger = System.currentTimeMillis()
                        }
                    )
                }
        } else {
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    activityTrigger = System.currentTimeMillis()
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showOverlays = !showOverlays
                    activityTrigger = System.currentTimeMillis()
                }
        }

        Box(
            modifier = containerModifier
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

            val parseColor = runCatching {
                Color(android.graphics.Color.parseColor(system.colorHex))
            }.getOrDefault(MaterialTheme.colorScheme.primary)

            val alwaysShowStatus = remember(system.shortName) {
                system.shortName.contains("ALWAYS")
            }

            val (showStatusEffective, statusAlignment) = remember(system.shortName, showStatusInFullScreen) {
                val showInSystem = !system.shortName.contains("HIDE_STATUS") && system.shortName != "false"
                val show = showStatusInFullScreen && showInSystem
                val align = when {
                    system.shortName.contains("CENTERED") -> "CENTERED"
                    system.shortName.contains("BOTTOM") -> "BOTTOM"
                    else -> "TOP"
                }
                Pair(show, align)
            }

            val isStatusVisible = showStatusEffective && (alwaysShowStatus || !showOverlays)

            val isClassicClock = system.folderPath.startsWith("CLASSIC")
            val is24Hour = system.folderPath.contains("24H") || system.allowedExtensions == "24h" || system.allowedExtensions == "HH:mm:ss" || system.allowedExtensions == "HH:mm"
            val showSeconds = !system.folderPath.contains("NOSEC") && !system.folderPath.contains("NO_SEC")

            val statusFontFamily = if (isClassicClock) FontFamily.Monospace else FontFamily.Default
            val statusLetterSpacing = if (isClassicClock) 1.5.sp else TextUnit.Unspecified

            val timePattern = remember(system.allowedExtensions) {
                when (system.allowedExtensions) {
                    "24h" -> "HH:mm:ss"
                    "12h", "" -> "hh:mm:ss a"
                    else -> system.allowedExtensions.ifBlank { "hh:mm:ss a" }
                }
            }

            var mainTimeStr by remember { mutableStateOf("") }
            LaunchedEffect(timePattern) {
                while (true) {
                    val now = Calendar.getInstance()
                    val sdf = runCatching { SimpleDateFormat(timePattern, Locale.getDefault()) }
                        .getOrElse { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
                    mainTimeStr = sdf.format(now.time)
                    delay(1000L)
                }
            }

            val batteryIcon = if (isCharging) {
                Icons.Default.BatteryChargingFull
            } else if (batteryPercent <= 15) {
                Icons.Default.BatteryAlert
            } else {
                Icons.Default.BatteryFull
            }

            // Centered Display Container (Clock + optionally Centered Battery & Date)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val availW = (maxWidth - 16.dp).coerceAtLeast(60.dp)
                val availH = (maxHeight - 16.dp).coerceAtLeast(40.dp)
                val density = LocalDensity.current
                val computedDigitHeight = remember(isClassicClock, is24Hour, showSeconds, availW, availH, statusAlignment) {
                    val widthFactor = when {
                        !is24Hour && showSeconds -> 4.85f
                        is24Hour && showSeconds -> 4.05f
                        !is24Hour && !showSeconds -> 3.75f
                        else -> 2.85f
                    }
                    val maxHFromWidth = availW / widthFactor
                    val maxHFromHeight = if (statusAlignment == "CENTERED") availH * 0.55f else availH * 0.72f
                    minOf(maxHFromWidth, maxHFromHeight).coerceAtLeast(32.dp)
                }

                val textMeasurer = rememberTextMeasurer()
                val textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = parseColor,
                    textAlign = TextAlign.Center
                )

                val optimalFontSize = remember(mainTimeStr, availW, availH, statusAlignment, textStyle, density) {
                    if (mainTimeStr.isBlank()) 64.sp else {
                        val targetWidthPx = with(density) { availW.toPx() }.toInt()
                        val targetHeightPx = with(density) { (if (statusAlignment == "CENTERED") availH * 0.55f else availH * 0.72f).toPx() }.toInt()

                        var low = 16
                        var high = 350
                        var best = 32

                        while (low <= high) {
                            val mid = (low + high) / 2
                            val layoutResult = textMeasurer.measure(
                                text = mainTimeStr,
                                style = textStyle.copy(fontSize = mid.sp),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (layoutResult.size.width <= targetWidthPx && layoutResult.size.height <= targetHeightPx) {
                                best = mid
                                low = mid + 1
                            } else {
                                high = mid - 1
                            }
                        }
                        best.sp
                    }
                }

                val clockFontSizeVal = if (isClassicClock) {
                    with(density) { computedDigitHeight.toSp().value }
                } else {
                    optimalFontSize.value
                }

                val batteryFontSize = (clockFontSizeVal * 0.50f).coerceIn(16f, 160f).sp
                val batteryIconSize = with(density) { (clockFontSizeVal * 0.50f).toDp().coerceIn(20.dp, 160.dp) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // CENTERED Alignment: Battery Level Above Clock
                    if (statusAlignment == "CENTERED") {
                        AnimatedVisibility(
                            visible = isStatusVisible,
                            enter = fadeIn(animationSpec = tween(600)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = batteryIcon,
                                        contentDescription = "Battery Level",
                                        tint = parseColor,
                                        modifier = Modifier.size(batteryIconSize)
                                    )
                                    Text(
                                        text = "$batteryPercent%",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = statusFontFamily,
                                            letterSpacing = statusLetterSpacing,
                                            color = parseColor,
                                            fontSize = batteryFontSize
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(28.dp))
                            }
                        }
                    }

                    // Main Clock Display (Classic 7-segment or Standard Formatted text)
                    if (isClassicClock) {
                        SevenSegmentClock(
                            is24Hour = is24Hour,
                            color = parseColor,
                            digitHeight = computedDigitHeight,
                            showSeconds = showSeconds,
                            showContainerBackground = false
                        )
                    } else {
                        Text(
                            text = mainTimeStr,
                            style = textStyle.copy(
                                fontSize = optimalFontSize
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // CENTERED Alignment: Date Under Clock (matching font style and clock width)
                    if (statusAlignment == "CENTERED") {
                        AnimatedVisibility(
                            visible = isStatusVisible,
                            enter = fadeIn(animationSpec = tween(600)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            val dateStyle = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = statusFontFamily,
                                letterSpacing = statusLetterSpacing,
                                color = parseColor,
                                textAlign = TextAlign.Center
                            )

                            val optimalDateFontSize = remember(statusDateStr, availW, availH, dateStyle, density) {
                                if (statusDateStr.isBlank()) 24.sp else {
                                    val targetWidthPx = with(density) { (availW * 0.88f).toPx() }.toInt()
                                    val targetHeightPx = with(density) { (availH * 0.18f).toPx() }.toInt()

                                    var low = 18
                                    var high = 140
                                    var best = 24

                                    while (low <= high) {
                                        val mid = (low + high) / 2
                                        val layoutResult = textMeasurer.measure(
                                            text = statusDateStr,
                                            style = dateStyle.copy(fontSize = mid.sp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (layoutResult.size.width <= targetWidthPx && layoutResult.size.height <= targetHeightPx) {
                                            best = mid
                                            low = mid + 1
                                        } else {
                                            high = mid - 1
                                        }
                                    }
                                    best.sp
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    text = statusDateStr,
                                    style = dateStyle.copy(fontSize = optimalDateFontSize),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // TOP or BOTTOM Status Bar
            if (statusAlignment == "TOP" || statusAlignment == "BOTTOM") {
                AnimatedVisibility(
                    visible = isStatusVisible,
                    enter = fadeIn(animationSpec = tween(600)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(if (statusAlignment == "BOTTOM") Alignment.BottomCenter else Alignment.TopCenter)
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Battery level on left
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = "Battery Level",
                                tint = parseColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "$batteryPercent%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = statusFontFamily,
                                    letterSpacing = statusLetterSpacing,
                                    color = parseColor
                                )
                            )
                        }

                        // Date on right (matching clock font color & style)
                        Text(
                            text = statusDateStr,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = statusFontFamily,
                                letterSpacing = statusLetterSpacing,
                                color = parseColor
                            )
                        )
                    }
                }
            }

            // Brightness Slider (positioned at Bottom by default; moved to Top if status bar is at Bottom)
            AnimatedVisibility(
                visible = showOverlays && !gestureBrightnessEnabled,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(500)),
                modifier = Modifier
                    .align(if (statusAlignment == "BOTTOM") Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(
                        start = if (statusAlignment == "BOTTOM") 20.dp else 24.dp,
                        end = if (statusAlignment == "BOTTOM") 84.dp else 24.dp,
                        top = if (statusAlignment == "BOTTOM") 20.dp else 0.dp,
                        bottom = if (statusAlignment == "BOTTOM") 0.dp else 24.dp
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(if (statusAlignment == "BOTTOM") 0.75f else 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val brightnessIcon = when {
                            currentBrightness < 0.35f -> Icons.Default.BrightnessLow
                            currentBrightness < 0.70f -> Icons.Default.BrightnessMedium
                            else -> Icons.Default.BrightnessHigh
                        }
                        Icon(
                            imageVector = brightnessIcon,
                            contentDescription = "Brightness Control",
                            tint = parseColor,
                            modifier = Modifier.size(22.dp)
                        )

                        Slider(
                            value = currentBrightness,
                            onValueChange = {
                                currentBrightness = it
                                activityTrigger = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                activityTrigger = System.currentTimeMillis()
                            },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = parseColor,
                                activeTrackColor = parseColor,
                                inactiveTrackColor = parseColor.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${(currentBrightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = statusFontFamily,
                                color = parseColor
                            ),
                            modifier = Modifier.width(38.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Close Button in top right corner (auto-hides after inactivity period)
            AnimatedVisibility(
                visible = showOverlays,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(500)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                FilledIconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Full Screen Clock",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Central Brightness Gesture Indicator
            AnimatedVisibility(
                visible = showBrightnessIndicator,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(400)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val brightnessIcon = when {
                            currentBrightness < 0.35f -> Icons.Default.BrightnessLow
                            currentBrightness < 0.70f -> Icons.Default.BrightnessMedium
                            else -> Icons.Default.BrightnessHigh
                        }
                        Icon(
                            imageVector = brightnessIcon,
                            contentDescription = "Brightness",
                            tint = parseColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Brightness: ${(currentBrightness * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

