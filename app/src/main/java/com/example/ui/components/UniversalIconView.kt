package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File

@Composable
fun UniversalIconView(
    iconNameOrPath: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val isExternalFile = remember(iconNameOrPath) {
        if (iconNameOrPath.isBlank()) false
        else {
            val file = File(iconNameOrPath)
            file.exists() || iconNameOrPath.startsWith("/") || iconNameOrPath.startsWith("http") ||
                    iconNameOrPath.endsWith(".png", ignoreCase = true) ||
                    iconNameOrPath.endsWith(".jpg", ignoreCase = true) ||
                    iconNameOrPath.endsWith(".webp", ignoreCase = true)
        }
    }

    if (isExternalFile) {
        AsyncImage(
            model = iconNameOrPath,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = SystemIconHelper.getIconVector(iconNameOrPath),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
        )
    }
}
