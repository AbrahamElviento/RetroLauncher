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
        "palette" to Icons.Default.Palette,
        "clock" to Icons.Default.Schedule,
        "calendar" to Icons.Default.CalendarToday,
        "pencil" to Icons.Default.Edit,
        "brush" to Icons.Default.Brush,
        "music" to Icons.Default.MusicNote,
        "volume" to Icons.Default.VolumeUp,
        "heart" to Icons.Default.Favorite,
        "home" to Icons.Default.Home,
        "bar_chart" to Icons.Default.BarChart,
        "check_box" to Icons.Default.CheckBox,
        "badge" to Icons.Default.Badge
    )

    fun getIconVector(iconName: String): ImageVector {
        val lower = iconName.lowercase()
        return when {
            lower == "star" || lower.contains("fav") || lower.contains("star") -> Icons.Default.Star
            lower == "history" || lower.contains("recent") || lower.contains("history") -> Icons.Default.History
            lower == "android" || lower.contains("android") -> Icons.Default.Android
            lower == "apps" || lower.contains("apps") -> Icons.Default.Apps
            lower == "headset" || lower.contains("headset") -> Icons.Default.Headset
            lower == "clock" || lower.contains("clock") || lower.contains("schedule") || lower.contains("time") -> Icons.Default.Schedule
            lower == "calendar" || lower.contains("calendar") || lower.contains("date") || lower.contains("today") -> Icons.Default.CalendarToday
            lower == "pencil" || lower == "edit" || lower.contains("pencil") || lower.contains("edit") -> Icons.Default.Edit
            lower == "brush" || lower.contains("brush") || lower.contains("draw") -> Icons.Default.Brush
            lower == "music" || lower.contains("music") || lower.contains("audio_note") -> Icons.Default.MusicNote
            lower == "volume" || lower.contains("volume") || lower.contains("audio") || lower.contains("sound") -> Icons.Default.VolumeUp
            lower == "heart" || lower.contains("heart") || lower.contains("fav_heart") || lower.contains("favorite") -> Icons.Default.Favorite
            lower == "home" || lower.contains("home") -> Icons.Default.Home
            lower == "bar_chart" || lower.contains("bar_chart") || lower.contains("chart") -> Icons.Default.BarChart
            lower == "check_box" || lower.contains("check_box") || lower.contains("check") -> Icons.Default.CheckBox
            lower == "badge" || lower.contains("badge") -> Icons.Default.Badge
            lower.contains("ps") || lower.contains("playstation") -> Icons.Default.VideogameAsset
            lower.contains("3ds") || lower.contains("ds") -> Icons.Default.Smartphone
            lower.contains("tv") || lower.contains("console") -> Icons.Default.Tv
            lower.contains("arcade") || lower.contains("mame") -> Icons.Default.Casino
            lower.contains("pc") || lower.contains("desktop") -> Icons.Default.DesktopWindows
            else -> AVAILABLE_ICONS.firstOrNull { it.first == lower }?.second ?: Icons.Default.SportsEsports
        }
    }
}
