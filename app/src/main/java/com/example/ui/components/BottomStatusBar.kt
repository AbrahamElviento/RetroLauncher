package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomBarSettings
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomStatusBar(
    settings: BottomBarSettings,
    onOpenBarSettings: () -> Unit,
    isFocused: Boolean = false,
    containerColorHex: String = "",
    notificationText: String? = null,
    onDoubleTapClock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!settings.showBottomBar) return

    val context = LocalContext.current
    val attributionContext = context

    // Live Time state
    var currentTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(settings.timeFormat) {
        val formatter = try {
            SimpleDateFormat(settings.timeFormat, Locale.getDefault())
        } catch (e: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
        while (true) {
            currentTimeStr = formatter.format(Date())
            delay(1000L)
        }
    }

    // Live Date state
    var currentDateStr by remember { mutableStateOf("") }
    LaunchedEffect(settings.dateFormat) {
        val formatter = try {
            SimpleDateFormat(settings.dateFormat, Locale.getDefault())
        } catch (e: Exception) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        }
        while (true) {
            currentDateStr = formatter.format(Date())
            delay(10000L)
        }
    }

    // Live Battery state
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(attributionContext) {
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
        attributionContext.registerReceiver(receiver, filter)

        onDispose {
            try {
                attributionContext.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if unregister fails
            }
        }
    }

    // Wifi state check
    var isWifiConnected by remember { mutableStateOf(false) }
    LaunchedEffect(attributionContext) {
        val cm = attributionContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        while (true) {
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            delay(3000L)
        }
    }

    // Bluetooth state check
    var isBluetoothEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(attributionContext) {
        val bluetoothManager = attributionContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val btAdapter = bluetoothManager?.adapter ?: android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        while (true) {
            isBluetoothEnabled = btAdapter?.isEnabled == true
            delay(5000L)
        }
    }

    val orderedItems = remember(settings.itemsOrderAndAlign) {
        getNormalizedItems(settings.itemsOrderAndAlign)
    }

    val leftItems = remember(orderedItems) { orderedItems.filter { it.second == "left" } }
    val rightItems = remember(orderedItems) { orderedItems.filter { it.second == "right" } }

    @Composable
    fun RenderItem(key: String) {
        when (key) {
            "wifi" -> {
                if (settings.showWifi) {
                    Icon(
                        imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Wi-Fi",
                        tint = if (isWifiConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(settings.iconSizeDp.dp)
                    )
                }
            }
            "bluetooth" -> {
                if (settings.showBluetooth) {
                    Icon(
                        imageVector = if (isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = "Bluetooth",
                        tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(settings.iconSizeDp.dp)
                    )
                }
            }
            "battery" -> {
                if (settings.showBattery) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (settings.showBatteryIcon) {
                            Icon(
                                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (batteryPercent > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                contentDescription = "Battery",
                                tint = if (isCharging || batteryPercent > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(settings.iconSizeDp.dp)
                            )
                        }
                        Text(
                            text = "$batteryPercent%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (settings.iconSizeDp * 0.75f).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            "date" -> {
                if (settings.showDate) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (settings.showDateIcon) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date",
                                modifier = Modifier.size((settings.iconSizeDp * 0.8f).dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = currentDateStr,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (settings.iconSizeDp * 0.8f).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
            "time" -> {
                if (settings.showTime) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onDoubleTapClock?.invoke()
                                }
                            )
                        }
                    ) {
                        if (settings.showClockIcon) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size((settings.iconSizeDp * 0.85f).dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = currentTimeStr,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (settings.iconSizeDp * 0.8f).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(settings.heightDp.dp)
            .clickable { onOpenBarSettings() },
        color = if (containerColorHex.isNotBlank()) {
            parseHexColor(containerColorHex)
        } else if (isFocused) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        },
        contentColor = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isFocused) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Left-aligned items
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leftItems.forEach { (key, _) ->
                    RenderItem(key = key)
                }
            }

            // Center Area: Notification with Marquee if text is too long
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!notificationText.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (notificationText.contains("Scanning", ignoreCase = true)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = notificationText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (notificationText.contains("completed", ignoreCase = true) || notificationText.contains("found", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1200,
                                velocity = 30.dp
                            )
                        )
                    }
                }
            }

            // Right Side: Right-aligned items & optionally Settings Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rightItems.forEach { (key, _) ->
                    RenderItem(key = key)
                }

                if (settings.showSettingsIcon) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Bar Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size((settings.iconSizeDp * 0.8f).dp)
                    )
                }
            }
        }
    }
}

private fun parseItemsOrderAndAlign(input: String): List<Pair<String, String>> {
    if (input.isBlank()) {
        return listOf(
            "bluetooth" to "left",
            "wifi" to "left",
            "battery" to "left",
            "date" to "right",
            "time" to "right"
        )
    }
    return input.split(",").mapNotNull {
        val parts = it.split(":")
        if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            null
        }
    }
}

private fun getNormalizedItems(input: String): List<Pair<String, String>> {
    val parsed = parseItemsOrderAndAlign(input).toMutableList()
    val allKeys = listOf("bluetooth", "wifi", "battery", "date", "time")
    val missingKeys = allKeys.filter { key -> parsed.none { it.first == key } }
    for (key in missingKeys) {
        val defaultAlign = when (key) {
            "date", "time" -> "right"
            else -> "left"
        }
        parsed.add(key to defaultAlign)
    }
    return parsed
}
