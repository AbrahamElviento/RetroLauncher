package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomBarSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BottomStatusBar(
    settings: BottomBarSettings,
    onOpenBarSettings: () -> Unit,
    isFocused: Boolean = false,
    containerColorHex: String = "",
    modifier: Modifier = Modifier
) {
    if (!settings.showBottomBar) return

    val context = LocalContext.current

    // Live Time state
    var currentTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeStr = formatter.format(Date())
            delay(1000L)
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
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if unregister fails
            }
        }
    }

    // Wifi state check
    var isWifiConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        while (true) {
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            delay(3000L)
        }
    }

    // Bluetooth state check
    var isBluetoothEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        while (true) {
            isBluetoothEnabled = btAdapter?.isEnabled == true
            delay(5000L)
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
            // Left Side: Status Indicators (Wifi, Bluetooth, Battery)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Wi-Fi
                if (settings.showWifi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Wi-Fi",
                            tint = if (isWifiConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(settings.iconSizeDp.dp)
                        )
                    }
                }

                // Bluetooth
                if (settings.showBluetooth) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                            contentDescription = "Bluetooth",
                            tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(settings.iconSizeDp.dp)
                        )
                    }
                }

                // Battery
                if (settings.showBattery) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else if (batteryPercent > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                            contentDescription = "Battery",
                            tint = if (isCharging || batteryPercent > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(settings.iconSizeDp.dp)
                        )
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

            // Right Side: Time & Bar Settings Config Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (settings.showTime) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.size((settings.iconSizeDp * 0.85f).dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
