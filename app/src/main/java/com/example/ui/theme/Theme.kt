package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.DisplaySettings

fun parseColorHex(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun RetroLauncherTheme(
    displaySettings: DisplaySettings? = null,
    content: @Composable () -> Unit
) {
    val bg = parseColorHex(displaySettings?.backgroundColorHex ?: "", DarkBackground)
    val surface = parseColorHex(displaySettings?.surfaceColorHex ?: "", DarkSurface)
    val primary = parseColorHex(displaySettings?.primaryColorHex ?: "", DarkPrimary)
    val textPrimary = parseColorHex(displaySettings?.textColorHex ?: "", TextPrimary)
    val cardBg = parseColorHex(displaySettings?.cardBackgroundColorHex ?: "", DarkSurfaceVariant)

    val customColorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.25f),
        onPrimaryContainer = primary,
        secondary = primary,
        onSecondary = Color.White,
        secondaryContainer = cardBg,
        onSecondaryContainer = textPrimary,
        tertiary = primary,
        onTertiary = Color.White,
        background = bg,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = cardBg,
        onSurfaceVariant = textPrimary.copy(alpha = 0.7f),
        outline = primary.copy(alpha = 0.4f)
    )

    MaterialTheme(
        colorScheme = customColorScheme,
        typography = Typography,
        content = content
    )
}

