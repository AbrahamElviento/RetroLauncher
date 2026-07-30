package com.example.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object AndroidAppIconHelper {

    fun getAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
        return try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    fun getAllInstalledAppsInfo(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfos.map { info ->
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            AppInfo(packageName = pkg, label = label)
        }.sortedBy { it.label.lowercase() }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String
)
