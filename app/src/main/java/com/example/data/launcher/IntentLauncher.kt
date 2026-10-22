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

    private fun isAetherSX2OrNetherSX2(profile: StandaloneProfileEntity, targetPackage: String): Boolean {
        val id = profile.id.lowercase()
        val pkg = targetPackage.lowercase()
        val act = profile.activityName.lowercase()
        return id == "aethersx2" || id == "nethersx2" ||
                pkg == "xyz.aethersx2.android" || pkg == "net.nethersx2.android" || pkg == "net.pcsx2.aethersx2" ||
                pkg.contains("aethersx2") || pkg.contains("nethersx2") ||
                act.contains("aethersx2") || act.contains("nethersx2")
    }

    private fun getPpssppRomPath(filePath: String): Pair<String, String> {
        val absolutePath = filePath.trim()
        if (absolutePath.startsWith("/storage/")) {
            val sub = absolutePath.removePrefix("/storage/")
            val parts = sub.split("/", limit = 2)
            if (parts.size == 2 && parts[0] != "emulated" && parts[0] != "self") {
                val sdCardName = parts[0]
                val relativePath = parts[1].trimStart('/')
                val formattedSdPath = "$sdCardName/$relativePath"
                return Pair(formattedSdPath, absolutePath)
            }
        } else if (absolutePath.startsWith("/mnt/media_rw/")) {
            val sub = absolutePath.removePrefix("/mnt/media_rw/")
            val parts = sub.split("/", limit = 2)
            if (parts.size == 2) {
                val sdCardName = parts[0]
                val relativePath = parts[1].trimStart('/')
                val formattedSdPath = "$sdCardName/$relativePath"
                val fullStoragePath = "/storage/$sdCardName/$relativePath"
                return Pair(formattedSdPath, fullStoragePath)
            }
        }
        return Pair(absolutePath, absolutePath)
    }

    fun getSafDocumentUri(file: File): Uri {
        val absolutePath = file.absolutePath
        if (absolutePath.startsWith("content://")) {
            return Uri.parse(absolutePath)
        }
        val authority = "com.android.externalstorage.documents"

        val (volume, relativePath) = when {
            absolutePath.startsWith("/storage/emulated/0/") -> {
                "primary" to absolutePath.removePrefix("/storage/emulated/0/")
            }
            absolutePath.startsWith("/sdcard/") -> {
                "primary" to absolutePath.removePrefix("/sdcard/")
            }
            absolutePath.startsWith("/mnt/sdcard/") -> {
                "primary" to absolutePath.removePrefix("/mnt/sdcard/")
            }
            absolutePath.startsWith("/storage/") -> {
                val sub = absolutePath.removePrefix("/storage/")
                val parts = sub.split("/", limit = 2)
                if (parts.size == 2) {
                    if (parts[0] == "emulated") {
                        val subParts = parts[1].split("/", limit = 2)
                        if (subParts.size == 2) "primary" to subParts[1] else "primary" to parts[1]
                    } else {
                        parts[0] to parts[1]
                    }
                } else {
                    "primary" to sub
                }
            }
            else -> "primary" to absolutePath.trimStart('/')
        }

        val cleanRelativePath = relativePath.trimStart('/')
        val docId = "$volume:$cleanRelativePath"

        return try {
            android.provider.DocumentsContract.buildDocumentUri(authority, docId)
        } catch (e: Exception) {
            val encodedDocId = Uri.encode(docId, ":/")
            Uri.parse("content://$authority/document/$encodedDocId")
        }
    }

    fun getSafTreeDocumentUri(file: File): Uri {
        val absolutePath = file.absolutePath
        if (absolutePath.startsWith("content://")) {
            return Uri.parse(absolutePath)
        }
        val authority = "com.android.externalstorage.documents"

        val (volume, relativePath) = when {
            absolutePath.startsWith("/storage/emulated/0/") -> {
                "primary" to absolutePath.removePrefix("/storage/emulated/0/")
            }
            absolutePath.startsWith("/sdcard/") -> {
                "primary" to absolutePath.removePrefix("/sdcard/")
            }
            absolutePath.startsWith("/mnt/sdcard/") -> {
                "primary" to absolutePath.removePrefix("/mnt/sdcard/")
            }
            absolutePath.startsWith("/storage/") -> {
                val sub = absolutePath.removePrefix("/storage/")
                val parts = sub.split("/", limit = 2)
                if (parts.size == 2) {
                    if (parts[0] == "emulated") {
                        val subParts = parts[1].split("/", limit = 2)
                        if (subParts.size == 2) "primary" to subParts[1] else "primary" to parts[1]
                    } else {
                        parts[0] to parts[1]
                    }
                } else {
                    "primary" to sub
                }
            }
            else -> "primary" to absolutePath.trimStart('/')
        }

        val cleanRelativePath = relativePath.trimStart('/')
        val docId = "$volume:$cleanRelativePath"
        val parentPath = if (cleanRelativePath.contains("/")) cleanRelativePath.substringBeforeLast('/') else ""
        val treeId = if (parentPath.isNotEmpty()) "$volume:$parentPath" else "$volume:"

        return try {
            val treeUri = android.provider.DocumentsContract.buildTreeDocumentUri(authority, treeId)
            android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        } catch (e: Exception) {
            val encodedTreeId = Uri.encode(treeId, ":/")
            val encodedDocId = Uri.encode(docId, ":/")
            Uri.parse("content://$authority/tree/$encodedTreeId/document/$encodedDocId")
        }
    }

    fun launchStandaloneApp(profile: StandaloneProfileEntity, game: GameRomEntity): LaunchResult {
        var targetPackage = profile.packageName.trim()

        if (!isPackageInstalled(targetPackage)) {
            val idLower = profile.id.lowercase()
            val alternates = when {
                idLower == "aethersx2" || idLower == "nethersx2" -> listOf("xyz.aethersx2.android", "net.nethersx2.android", "net.pcsx2.aethersx2")
                idLower == "ppsspp" -> listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")
                idLower == "ppsspp_gold" -> listOf("org.ppsspp.ppssppgold", "org.ppsspp.ppsspp")
                idLower == "drastic" -> listOf("com.dsemu.drastic")
                idLower == "dolphin" -> listOf("org.dolphinemu.dolphinemu", "org.dolphinemu.mmjr")
                else -> emptyList()
            }
            val installedAlt = alternates.firstOrNull { isPackageInstalled(it) }
            if (installedAlt != null) {
                targetPackage = installedAlt
            } else {
                return LaunchResult.PackageNotInstalled(profile.packageName, profile.name)
            }
        }

        val isAetherOrNether = isAetherSX2OrNetherSX2(profile, targetPackage)

        // Format activity class name
        val rawActivity = profile.activityName.trim()
        val isDrastic = profile.id.lowercase().contains("drastic") ||
                targetPackage == "com.dsemu.drastic" ||
                rawActivity.endsWith(".DraSticActivity")

        val activityClass = when {
            isAetherOrNether -> "$targetPackage.EmulationActivity"
            rawActivity.startsWith(".") -> "$targetPackage$rawActivity"
            rawActivity.startsWith("/") -> "$targetPackage${rawActivity.removePrefix("/")}"
            rawActivity.endsWith(".EmulationActivity") -> "$targetPackage.EmulationActivity"
            profile.id == "ppsspp" || profile.id == "ppsspp_gold" || rawActivity.endsWith(".PpssppActivity") -> "$targetPackage.PpssppActivity"
            isDrastic -> "$targetPackage.DraSticActivity"
            profile.id == "dolphin" || rawActivity.endsWith("ui.main.MainActivity") -> "$targetPackage.ui.main.MainActivity"
            else -> rawActivity
        }

        val isPpsspp = profile.id == "ppsspp" || profile.id == "ppsspp_gold" ||
                targetPackage == "org.ppsspp.ppsspp" || targetPackage == "org.ppsspp.ppssppgold" ||
                targetPackage.contains("ppsspp")

        val actionToUse = when {
            isAetherOrNether -> Intent.ACTION_MAIN
            isPpsspp || isDrastic -> Intent.ACTION_VIEW
            else -> profile.intentAction.ifEmpty { Intent.ACTION_VIEW }
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
            val safUri = getSafDocumentUri(file)
            val safTreeUri = getSafTreeDocumentUri(file)
            val safUriString = safUri.toString()

            val intent = Intent(actionToUse).apply {
                setClassName(targetPackage, activityClass)

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (!isDrastic) {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                val extraKey = profile.romPathExtraKey.ifEmpty {
                    when {
                        isAetherOrNether -> "bootPath"
                        isPpsspp -> "org.ppsspp.ppsspp.Shortcuts"
                        else -> "bootPath"
                    }
                }

                if (isPpsspp) {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    data = safUri
                } else if (isDrastic) {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    val targetUri = when {
                        profile.romPathExtraKey.equals("FILE_URI", ignoreCase = true) ||
                        profile.romPathExtraKey.equals("FILE", ignoreCase = true) -> fileUri
                        profile.romPathExtraKey.equals("FILE_PATH", ignoreCase = true) ||
                        profile.romPathExtraKey.equals("RAW_PATH", ignoreCase = true) -> Uri.parse(game.filePath)
                        else -> safUri
                    }
                    data = targetUri
                    clipData = android.content.ClipData.newRawUri(null, targetUri)

                    // Do NOT pass legacy shortcut extras like "GAMEPATH", "path", "PATH", "bootPath", etc.
                    // Passing those extras causes DraStic's shortcut handler to activate and fail with
                    // the toast "Unable to open shortcut", even when the ROM loads successfully via data URI.
                    if (profile.romPathExtraKey.isNotBlank() &&
                        !profile.romPathExtraKey.equals("DATA", ignoreCase = true) &&
                        !profile.romPathExtraKey.equals("FILE_URI", ignoreCase = true) &&
                        !profile.romPathExtraKey.equals("FILE", ignoreCase = true) &&
                        !profile.romPathExtraKey.equals("FILE_PATH", ignoreCase = true) &&
                        !profile.romPathExtraKey.equals("RAW_PATH", ignoreCase = true) &&
                        !profile.romPathExtraKey.equals("NONE", ignoreCase = true)
                    ) {
                        putExtra(profile.romPathExtraKey, game.filePath)
                    }
                } else if (isAetherOrNether) {
                    putExtra("bootPath", safUriString)
                    if (extraKey.isNotEmpty() && extraKey != "bootPath") {
                        putExtra(extraKey, safUriString)
                    }
                } else {
                    putExtra(extraKey, game.filePath)
                    putExtra("bootPath", game.filePath)
                    putExtra("path", game.filePath)
                    putExtra("ROM", game.filePath)
                    putExtra("gamePath", game.filePath)
                    putExtra("romPath", game.filePath)

                    if (extraKey == "android.intent.extra.STREAM" || extraKey == "CONTENT_URI") {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        setDataAndType(uri, "*/*")
                    } else {
                        setDataAndType(uri, "*/*")
                    }
                }

                if (profile.extraArgsJson.isNotBlank() && profile.extraArgsJson != "{}") {
                    try {
                        val json = org.json.JSONObject(profile.extraArgsJson)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = json.optString(key)
                            if (key.isNotEmpty()) {
                                putExtra(key, value)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("IntentLauncher", "Failed to parse extraArgsJson: ${e.message}")
                    }
                }
            }

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val urisToGrant = if (isAetherOrNether || isPpsspp || isDrastic) {
                listOfNotNull(safUri, safTreeUri, uri, fileUri)
            } else {
                listOfNotNull(uri, fileUri)
            }
            for (u in urisToGrant) {
                try {
                    context.grantUriPermission(targetPackage, u, flags)
                } catch (e: Exception) {
                    Log.w("IntentLauncher", "Failed granting permission for $u to $targetPackage: ${e.message}")
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
            var cls = if (parts.size > 1) parts[1] else ""
            val userSerial = if (parts.size > 2) parts[2].toLongOrNull() else null

            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager

            if (launcherApps != null && userManager != null) {
                val userProfiles = userManager.userProfiles
                var targetUser = if (userSerial != null) {
                    userProfiles.find { userManager.getSerialNumberForUser(it) == userSerial }
                } else {
                    null
                }

                if (targetUser == null) {
                    targetUser = android.os.Process.myUserHandle()
                }

                if (cls.isEmpty()) {
                    val activities = launcherApps.getActivityList(pkg, targetUser)
                    val mainActivity = activities.firstOrNull()
                    if (mainActivity != null) {
                        cls = mainActivity.componentName.className
                    }
                }

                if (cls.isNotEmpty()) {
                    val compName = android.content.ComponentName(pkg, cls)
                    launcherApps.startMainActivity(compName, targetUser, null, null)
                    return LaunchResult.Success
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
