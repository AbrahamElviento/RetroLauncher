package com.example.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object AndroidAppIconHelper {

    fun getAppIconBitmap(context: Context, compoundId: String): ImageBitmap? {
        if (compoundId.isBlank()) return null
        val parts = compoundId.split("|")
        val packageName = parts[0]
        val activityName = if (parts.size > 1) parts[1] else ""
        val userSerial = if (parts.size > 2) parts[2].toLongOrNull() else null

        return try {
            if (activityName.isNotEmpty() && userSerial != null) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                if (launcherApps != null && userManager != null) {
                    val userProfiles = userManager.userProfiles
                    val user = userProfiles.find { userManager.getSerialNumberForUser(it) == userSerial }
                    if (user != null) {
                        val compName = android.content.ComponentName(packageName, activityName)
                        val activityInfo = launcherApps.resolveActivity(android.content.Intent().setComponent(compName), user)
                        if (activityInfo != null) {
                            val drawable = activityInfo.getBadgedIcon(0)
                            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
                            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
                            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            return bmp.asImageBitmap()
                        }
                    }
                }
            }

            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            try {
                val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getAllInstalledAppsInfo(context: Context): List<AppInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

        if (launcherApps != null && userManager != null) {
            try {
                val appList = mutableListOf<AppInfo>()
                val profiles = userManager.userProfiles
                for (profile in profiles) {
                    val serial = userManager.getSerialNumberForUser(profile)
                    val activities = launcherApps.getActivityList(null, profile)
                    for (activity in activities) {
                        val pkg = activity.applicationInfo.packageName
                        if (pkg == context.packageName) continue
                        val cls = activity.name
                        val label = activity.label?.toString()?.trim() ?: pkg
                        val compoundId = "$pkg|$cls|$serial"
                        appList.add(AppInfo(packageName = compoundId, label = label))
                    }
                }
                if (appList.isNotEmpty()) {
                    return appList.distinctBy { it.packageName }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val pm = context.packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return resolveInfos.mapNotNull { info ->
            try {
                val actInfo = info.activityInfo ?: return@mapNotNull null
                val pkg = actInfo.packageName ?: return@mapNotNull null
                if (pkg.isBlank()) return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val cls = actInfo.name ?: ""

                val label = try {
                    val loadedLabel = info.loadLabel(pm)?.toString()?.trim()
                    if (loadedLabel.isNullOrBlank()) {
                        pkg.split(".").lastOrNull()?.trim() ?: "Android App"
                    } else {
                        loadedLabel
                    }
                } catch (e: Exception) {
                    pkg.split(".").lastOrNull()?.trim() ?: "Android App"
                }

                val compoundId = if (cls.isNotEmpty()) "$pkg|$cls|0" else "$pkg"
                AppInfo(packageName = compoundId, label = label)
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }
         .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

data class AppInfo(
    val packageName: String,
    val label: String
)
