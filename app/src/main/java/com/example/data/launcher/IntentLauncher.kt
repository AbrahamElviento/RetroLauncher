package com.example.data.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.GameRomEntity
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity
import com.example.data.model.LaunchMode
import java.io.File

sealed class LaunchResult {
    data object Success : LaunchResult()
    data class PackageNotInstalled(val packageName: String, val appName: String) : LaunchResult()
    data class Error(val message: String) : LaunchResult()
}

class IntentLauncher(private val context: Context) {

    fun launchGame(
        system: SystemEntity,
        game: GameRomEntity,
        profile: StandaloneProfileEntity? = null
    ): LaunchResult {
        if (system.id == "android_apps" || system.defaultLaunchMode == "ANDROID_APP" || game.systemId == "android_apps") {
            return launchAndroidApp(game.filePath)
        }

        val file = File(game.filePath)
        val fileExists = file.exists()

        val mode = if (system.defaultLaunchMode == "STANDALONE_XML") LaunchMode.STANDALONE_XML else LaunchMode.RETROARCH

        return when (mode) {
            LaunchMode.RETROARCH -> launchRetroArch(system, game)
            LaunchMode.STANDALONE_XML -> {
                if (profile != null) {
                    launchStandaloneApp(profile, game)
                } else {
                    launchRetroArch(system, game)
                }
            }
            LaunchMode.AUTO_DETECT -> {
                if (profile != null && isPackageInstalled(profile.packageName)) {
                    launchStandaloneApp(profile, game)
                } else {
                    launchRetroArch(system, game)
                }
            }
        }
    }

    fun launchRetroArch(system: SystemEntity, game: GameRomEntity): LaunchResult {
        var targetPackage: String? = null

        val specifiedPkg = system.retroArchPackage.trim()
        if (specifiedPkg.isNotBlank() && specifiedPkg != "AUTO") {
            targetPackage = specifiedPkg
        } else {
            val retroArchPackages = listOf("com.retroarch.aarch64", "com.retroarch.ra32", "com.retroarch", "com.retroarch.ra")
            for (pkg in retroArchPackages) {
                if (isPackageInstalled(pkg)) {
                    targetPackage = pkg
                    break
                }
            }
            if (targetPackage == null) {
                targetPackage = "com.retroarch.aarch64"
            }
        }

        Log.i("IntentLauncher", "Attempting RetroArch launch for game '${game.title}' with package: $targetPackage")

        if (!isPackageInstalled(targetPackage)) {
            Log.e("IntentLauncher", "Package $targetPackage is not installed on device.")
            return LaunchResult.PackageNotInstalled(targetPackage, "RetroArch ($targetPackage)")
        }

        var corePath = system.retroArchCore.trim()
        if (corePath.isNotEmpty() && !corePath.contains("/")) {
            corePath = "/data/data/$targetPackage/cores/$corePath"
        }

        val candidateClasses = listOf(
            "com.retroarch.browser.retroactivity.RetroActivityFuture",
            "com.retroarch.browser.retroactivity.RetroActivity",
            "$targetPackage.browser.retroactivity.RetroActivityFuture",
            "$targetPackage.browser.retroactivity.RetroActivity"
        )

        var lastError: Exception? = null

        val internalConfig = "/data/data/$targetPackage/files/retroarch.cfg"
        val externalConfig = "/storage/emulated/0/Android/data/$targetPackage/files/retroarch.cfg"

        for (cls in candidateClasses) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setClassName(targetPackage, cls)
                    putExtra("ROM", game.filePath)
                    if (corePath.isNotEmpty()) {
                        putExtra("LIBRETRO", corePath)
                    }
                    putExtra("CONFIGFILE", externalConfig)
                    putExtra("CONFIG", internalConfig)
                    putExtra("CONFIG_FILE", externalConfig)
                    putExtra("IME", "$targetPackage/.IME")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                Log.i("IntentLauncher", "Successfully launched RetroArch with activity class: $cls for package: $targetPackage")
                return LaunchResult.Success
            } catch (e: Exception) {
                Log.w("IntentLauncher", "Failed RetroArch launch with class '$cls': ${e.message}")
                lastError = e
            }
        }

        // Fallback: Launch via package launcher intent with extras
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.apply {
                    action = Intent.ACTION_VIEW
                    putExtra("ROM", game.filePath)
                    if (corePath.isNotEmpty()) {
                        putExtra("LIBRETRO", corePath)
                    }
                    putExtra("CONFIGFILE", externalConfig)
                    putExtra("CONFIG", internalConfig)
                    putExtra("CONFIG_FILE", externalConfig)
                    putExtra("IME", "$targetPackage/.IME")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
                Log.i("IntentLauncher", "Successfully launched RetroArch using package launcher intent")
                return LaunchResult.Success
            }
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Fallback launcher intent failed for $targetPackage: ${e.message}", e)
            lastError = e
        }

        Log.e("IntentLauncher", "All RetroArch launch attempts failed for $targetPackage", lastError)
        return LaunchResult.Error("RetroArch error: ${lastError?.localizedMessage ?: "Unable to launch RetroArch activity"}")
    }

    fun launchStandaloneApp(profile: StandaloneProfileEntity, game: GameRomEntity): LaunchResult {
        var targetPackage = profile.packageName.trim()

        if (!isPackageInstalled(targetPackage)) {
            val alternates = when (profile.id) {
                "aethersx2" -> listOf("xyz.aethersx2.android", "net.pcsx2.aethersx2", "net.nethersx2.android")
                "ppsspp" -> listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")
                "ppsspp_gold" -> listOf("org.ppsspp.ppssppgold", "org.ppsspp.ppsspp")
                "drastic" -> listOf("com.dsemu.drastic")
                "dolphin" -> listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")
                else -> emptyList()
            }
            val installedAlt = alternates.firstOrNull { isPackageInstalled(it) }
            if (installedAlt != null) {
                targetPackage = installedAlt
            } else {
                return LaunchResult.PackageNotInstalled(profile.packageName, profile.name)
            }
        }

        // Format activity class name
        val rawActivity = profile.activityName.trim()
        val activityClass = when {
            rawActivity.startsWith(".") -> "$targetPackage$rawActivity"
            rawActivity.startsWith("/") -> "$targetPackage${rawActivity.removePrefix("/")}"
            profile.id == "aethersx2" || rawActivity.endsWith(".EmulationActivity") -> "$targetPackage.EmulationActivity"
            profile.id == "ppsspp" || profile.id == "ppsspp_gold" || rawActivity.endsWith(".PpssppActivity") -> "$targetPackage.PpssppActivity"
            profile.id == "drastic" || rawActivity.endsWith(".DraSticActivity") -> "$targetPackage.DraSticActivity"
            profile.id == "dolphin" || rawActivity.endsWith("ui.main.MainActivity") -> "$targetPackage.ui.main.MainActivity"
            else -> rawActivity
        }

        val actionToUse = if (profile.id == "aethersx2") {
            Intent.ACTION_MAIN
        } else {
            profile.intentAction.ifEmpty { Intent.ACTION_VIEW }
        }

        return try {
            try {
                val builder = android.os.StrictMode.VmPolicy.Builder()
                android.os.StrictMode.setVmPolicy(builder.build())
            } catch (e: Exception) {
                // Ignore
            }

            val file = File(game.filePath)
            val uri = getUriForFile(file)
            val fileUri = Uri.fromFile(file)

            try {
                context.grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (e: Exception) {
                // Ignore
            }

            val intent = Intent(actionToUse).apply {
                setClassName(targetPackage, activityClass)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                val extraKey = profile.romPathExtraKey.ifEmpty { "bootPath" }

                // Populate primary extra key and all known emulator keys (DraStic, AetherSX2, PPSSPP, etc.)
                putExtra(extraKey, game.filePath)
                putExtra("bootPath", game.filePath)
                putExtra("path", game.filePath)
                putExtra("ROM", game.filePath)
                putExtra("gamePath", game.filePath)
                putExtra("romPath", game.filePath)

                if (extraKey == "android.intent.extra.STREAM" || extraKey == "CONTENT_URI") {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    setDataAndType(uri, "*/*")
                } else if (profile.id == "drastic" || targetPackage == "com.dsemu.drastic") {
                    // DraStic specifically expects file Uri or direct bootPath extra
                    setDataAndType(fileUri, "*/*")
                } else {
                    setDataAndType(uri, "*/*")
                }
            }

            context.startActivity(intent)
            LaunchResult.Success
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Failed launching ${profile.name} with package $targetPackage and activity $activityClass", e)
            LaunchResult.Error("Launch failed: ${e.localizedMessage}")
        }
    }

    fun launchAndroidApp(packageName: String): LaunchResult {
        return try {
            val parts = packageName.split("|")
            val pkg = parts[0]
            val cls = if (parts.size > 1) parts[1] else ""
            val userSerial = if (parts.size > 2) parts[2].toLongOrNull() else null

            if (cls.isNotEmpty() && userSerial != null) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
                if (launcherApps != null && userManager != null) {
                    val userProfiles = userManager.userProfiles
                    val user = userProfiles.find { userManager.getSerialNumberForUser(it) == userSerial }
                    if (user != null) {
                        val compName = android.content.ComponentName(pkg, cls)
                        launcherApps.startMainActivity(compName, user, null, null)
                        return LaunchResult.Success
                    }
                }
            }

            // Fallback 1: Split with "/" if any
            val launchIntent = if (packageName.contains("/")) {
                val slashParts = packageName.split("/")
                val slashPkg = slashParts[0]
                val slashCls = slashParts[1]
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClassName(slashPkg, slashCls)
                intent
            } else if (cls.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClassName(pkg, cls)
                intent
            } else {
                context.packageManager.getLaunchIntentForPackage(pkg)
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                LaunchResult.Success
            } else {
                LaunchResult.PackageNotInstalled(packageName, packageName)
            }
        } catch (e: Exception) {
            Log.e("IntentLauncher", "Failed to launch Android app $packageName", e)
            LaunchResult.Error("Failed to launch app: ${e.localizedMessage}")
        }
    }

    private fun getUriForFile(file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val pkg = packageName.split("|")[0]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkg, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
