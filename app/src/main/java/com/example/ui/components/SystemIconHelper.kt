package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object SystemIconHelper {

    val AVAILABLE_ICONS = listOf(
        "gamepad" to Icons.Default.SportsEsports,
        "videogame" to Icons.Default.VideogameAsset,
        "tv" to Icons.Default.Tv,
        "smartphone" to Icons.Default.Smartphone,
        "tablet" to Icons.Default.Tablet,
        "desktop" to Icons.Default.DesktopWindows,
        "memory" to Icons.Default.Memory,
        "album" to Icons.Default.Album,
        "casino" to Icons.Default.Casino,
        "extension" to Icons.Default.Extension,
        "sd_card" to Icons.Default.SdStorage,
        "tune" to Icons.Default.Tune,
        "star" to Icons.Default.Star,
        "history" to Icons.Default.History,
        "android" to Icons.Default.Android,
        "settings" to Icons.Default.Settings,
        "folder" to Icons.Default.Folder,
        "apps" to Icons.Default.Apps,
        "computer" to Icons.Default.Computer,
        "games" to Icons.Default.Games,
        "headset" to Icons.Default.Headset,
        "widgets" to Icons.Default.Widgets,
        "palette" to Icons.Default.Palette
    )

    fun getIconVector(iconName: String): ImageVector {
        val lower = iconName.lowercase()
        return when {
            lower == "star" || lower.contains("fav") || lower.contains("star") -> Icons.Default.Star
            lower == "history" || lower.contains("recent") || lower.contains("history") -> Icons.Default.History
            lower == "android" || lower.contains("android") -> Icons.Default.Android
            lower == "apps" || lower.contains("apps") -> Icons.Default.Apps
            lower == "headset" || lower.contains("headset") -> Icons.Default.Headset
            lower.contains("ps") || lower.contains("playstation") -> Icons.Default.VideogameAsset
            lower.contains("3ds") || lower.contains("ds") -> Icons.Default.Smartphone
            lower.contains("tv") || lower.contains("console") -> Icons.Default.Tv
            lower.contains("arcade") || lower.contains("mame") -> Icons.Default.Casino
            lower.contains("pc") || lower.contains("desktop") -> Icons.Default.DesktopWindows
            else -> AVAILABLE_ICONS.firstOrNull { it.first == lower }?.second ?: Icons.Default.SportsEsports
        }
    }
}
