package com.example.data.model

data class BottomBarSettings(
    val showBottomBar: Boolean = true,
    val heightDp: Int = 36,
    val iconSizeDp: Int = 18,
    val showTime: Boolean = true,
    val showBattery: Boolean = true,
    val showWifi: Boolean = true,
    val showBluetooth: Boolean = true
)
