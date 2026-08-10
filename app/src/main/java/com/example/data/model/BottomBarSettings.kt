package com.example.data.model

data class BottomBarSettings(
    val showBottomBar: Boolean = true,
    val heightDp: Int = 36,
    val iconSizeDp: Int = 18,
    val showTime: Boolean = true,
    val showBattery: Boolean = true,
    val showWifi: Boolean = true,
    val showBluetooth: Boolean = true,
    val showDate: Boolean = true,
    val showSettingsIcon: Boolean = true,
    val itemsOrderAndAlign: String = "bluetooth:left,wifi:left,battery:left,date:right,time:right",
    val dateFormat: String = "EEE, MMM d",
    val timeFormat: String = "HH:mm",
    val showClockIcon: Boolean = true,
    val showDateIcon: Boolean = true,
    val showBatteryIcon: Boolean = true
)
