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
        "history" to Icons.Default.History
    )

    fun getIconVector(iconName: String): ImageVector {
        val lower = iconName.lowercase()
        return when {
            lower == "star" || lower.contains("fav") || lower.contains("star") -> Icons.Default.Star
            lower == "history" || lower.contains("recent") || lower.contains("history") -> Icons.Default.History
            lower.contains("ps") || lower.contains("playstation") -> Icons.Default.VideogameAsset
            lower.contains("3ds") || lower.contains("ds") -> Icons.Default.Smartphone
            lower.contains("tv") || lower.contains("console") -> Icons.Default.Tv
            lower.contains("arcade") || lower.contains("mame") -> Icons.Default.Casino
            lower.contains("pc") || lower.contains("desktop") -> Icons.Default.DesktopWindows
            else -> AVAILABLE_ICONS.firstOrNull { it.first == lower }?.second ?: Icons.Default.SportsEsports
        }
    }
}
