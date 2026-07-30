package com.example.data.repository

import android.content.Context
import com.example.data.config.ConfigStorageManager
import com.example.data.config.PresetData
import com.example.data.db.AppDatabase
import com.example.data.db.GameRomEntity
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity
import com.example.data.model.DisplaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class LauncherRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val systemDao = db.systemDao()
    private val gameRomDao = db.gameRomDao()
    private val standaloneProfileDao = db.standaloneProfileDao()

    val configStorageManager = ConfigStorageManager(context)

    val allSystems: Flow<List<SystemEntity>> = systemDao.getAllSystems()
    val allRoms: Flow<List<GameRomEntity>> = gameRomDao.getAllRoms()
    val favoriteRoms: Flow<List<GameRomEntity>> = gameRomDao.getFavoriteRoms()
    val recentlyPlayedRoms: Flow<List<GameRomEntity>> = gameRomDao.getRecentlyPlayedRoms()
    val allStandaloneProfiles: Flow<List<StandaloneProfileEntity>> = standaloneProfileDao.getAllProfiles()

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        // Load custom XML profiles from shareable config storage folder first
        val folderProfiles = configStorageManager.loadAllProfilesFromFolder()
        if (folderProfiles.isNotEmpty()) {
            standaloneProfileDao.insertProfiles(folderProfiles)
        }

        val currentProfiles = standaloneProfileDao.getAllProfiles().first()
        if (currentProfiles.isEmpty()) {
            standaloneProfileDao.insertProfiles(PresetData.PRESET_STANDALONE_PROFILES)
            PresetData.PRESET_STANDALONE_PROFILES.forEach {
                configStorageManager.saveProfileToFolder(it, overwriteIfExists = false)
            }
        } else {
            val existingIds = currentProfiles.map { it.id }.toSet()
            val missingPresets = PresetData.PRESET_STANDALONE_PROFILES.filter { it.id !in existingIds }
            if (missingPresets.isNotEmpty()) {
                standaloneProfileDao.insertProfiles(missingPresets)
                missingPresets.forEach {
                    configStorageManager.saveProfileToFolder(it, overwriteIfExists = false)
                }
            }
        }

        val baseDir = configStorageManager.getBaseDirPath()
        val defaultSystems = PresetData.getDefaultSystems(baseDir)
        
        // Load systems.xml from shareable config storage folder if it exists
        val xmlSystems = configStorageManager.loadSystemsXml()
        if (xmlSystems != null && xmlSystems.isNotEmpty()) {
            systemDao.insertSystems(xmlSystems)
        }

        val currentSystems = systemDao.getAllSystems().first()
        if (currentSystems.isEmpty()) {
            // Load default systems
            systemDao.insertSystems(defaultSystems)
            configStorageManager.saveSystemsXml(defaultSystems, overwriteIfExists = false)

            // Generate sample demo ROMs so the launcher is immediately playable & testable
            generateSampleDemoRoms(defaultSystems)
            scanAndroidApps("android_apps")
            scanAndroidApps("android_games")
            scanAndroidApps("android_emulators")
        } else {
            val existingIds = currentSystems.map { it.id }.toSet()
            val missingDefaults = defaultSystems.filter { (it.id in listOf("favorites", "recently_played", "android_apps", "android_games", "android_emulators")) && it.id !in existingIds }
            if (missingDefaults.isNotEmpty()) {
                systemDao.insertSystems(missingDefaults)
            }
            scanAndroidApps("android_apps")
            scanAndroidApps("android_games")
            scanAndroidApps("android_emulators")
        }
    }

    suspend fun getRomsForSystem(systemId: String): Flow<List<GameRomEntity>> {
        return gameRomDao.getRomsBySystem(systemId)
    }

    suspend fun insertOrUpdateSystem(system: SystemEntity) = withContext(Dispatchers.IO) {
        val existing = systemDao.getSystemById(system.id)
        val systemToSave = if (existing != null) {
            system.copy(displayOrder = existing.displayOrder)
        } else if (system.displayOrder == 0) {
            val all = systemDao.getAllSystems().first()
            val maxOrder = all.maxOfOrNull { it.displayOrder } ?: 0
            system.copy(displayOrder = maxOrder + 1)
        } else {
            system
        }
        systemDao.insertSystem(systemToSave)
        val all = systemDao.getAllSystems().first()
        configStorageManager.saveSystemsXml(all)
        scanFolderForRoms(systemToSave)
    }

    suspend fun updateSystemsList(systems: List<SystemEntity>) = withContext(Dispatchers.IO) {
        systemDao.insertSystems(systems)
        val all = systemDao.getAllSystems().first()
        configStorageManager.saveSystemsXml(all)
    }

    suspend fun deleteSystem(system: SystemEntity) = withContext(Dispatchers.IO) {
        gameRomDao.deleteRomsBySystem(system.id)
        systemDao.deleteSystem(system)
        val all = systemDao.getAllSystems().first()
        configStorageManager.saveSystemsXml(all)
    }

    suspend fun saveStandaloneProfile(profile: StandaloneProfileEntity) = withContext(Dispatchers.IO) {
        standaloneProfileDao.insertProfile(profile)
        configStorageManager.saveProfileToFolder(profile)
    }

    suspend fun deleteStandaloneProfile(profile: StandaloneProfileEntity) = withContext(Dispatchers.IO) {
        standaloneProfileDao.deleteProfile(profile)
    }

    suspend fun syncUserDataToConfig() = withContext(Dispatchers.IO) {
        val allRomsList = gameRomDao.getAllRoms().first()
        val dataList = allRomsList.map {
            ConfigStorageManager.RomUserData(
                filePath = it.filePath,
                isFavorite = it.isFavorite,
                lastPlayedTimestamp = it.lastPlayedTimestamp,
                playCount = it.playCount
            )
        }
        configStorageManager.saveFavoritesAndRecents(dataList)
    }

    suspend fun applySavedUserDataToDatabase() = withContext(Dispatchers.IO) {
        val savedUserDataMap = configStorageManager.loadFavoritesAndRecents()
        if (savedUserDataMap.isEmpty()) return@withContext
        val allRomsList = gameRomDao.getAllRoms().first()
        for (rom in allRomsList) {
            val savedData = savedUserDataMap[rom.filePath]
            if (savedData != null) {
                if (rom.isFavorite != savedData.isFavorite ||
                    rom.lastPlayedTimestamp != savedData.lastPlayedTimestamp ||
                    rom.playCount != savedData.playCount) {
                    gameRomDao.updateRom(
                        rom.copy(
                            isFavorite = savedData.isFavorite,
                            lastPlayedTimestamp = savedData.lastPlayedTimestamp,
                            playCount = savedData.playCount
                        )
                    )
                }
            }
        }
    }

    suspend fun toggleFavorite(rom: GameRomEntity) = withContext(Dispatchers.IO) {
        gameRomDao.updateRom(rom.copy(isFavorite = !rom.isFavorite))
        syncUserDataToConfig()
    }

    suspend fun renameGame(game: GameRomEntity, newName: String) = withContext(Dispatchers.IO) {
        val updatedGame = game.copy(title = newName)
        gameRomDao.updateRom(updatedGame)
        
        // Save to custom_name.xml in base storage folder
        val customNames = configStorageManager.loadCustomNames().toMutableMap()
        customNames[game.filePath] = newName
        configStorageManager.saveCustomNames(customNames)
    }

    fun loadCustomIcons(): Map<String, String> {
        return configStorageManager.loadCustomIcons()
    }

    fun saveCustomIcon(filePath: String, iconPath: String) {
        val customIcons = configStorageManager.loadCustomIcons().toMutableMap()
        if (iconPath.isBlank()) {
            customIcons.remove(filePath)
        } else {
            customIcons[filePath] = iconPath
        }
        configStorageManager.saveCustomIcons(customIcons)
    }

    suspend fun updateRomPlayed(rom: GameRomEntity) = withContext(Dispatchers.IO) {
        gameRomDao.updateRom(
            rom.copy(
                lastPlayedTimestamp = System.currentTimeMillis(),
                playCount = rom.playCount + 1
            )
        )
        syncUserDataToConfig()
    }

    suspend fun removeRomFromRecent(rom: GameRomEntity) = withContext(Dispatchers.IO) {
        gameRomDao.updateRom(rom.copy(lastPlayedTimestamp = 0L))
        syncUserDataToConfig()
    }

    fun getHiddenAndroidApps(): Set<String> {
        val prefs = context.getSharedPreferences("android_apps_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
    }

    fun getVisibleAndroidAppsXml(systemId: String): Set<String>? {
        return configStorageManager.loadVisibleAppsXml(systemId)
    }

    suspend fun saveVisibleAndroidApps(systemId: String, visibleSet: Set<String>) = withContext(Dispatchers.IO) {
        configStorageManager.saveVisibleAppsXml(systemId, visibleSet)
        scanAndroidApps(systemId)
    }

    suspend fun saveHiddenAndroidApps(hiddenSet: Set<String>) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("android_apps_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("hidden_packages", hiddenSet).apply()
        scanAndroidApps("android_apps")
    }

    suspend fun scanAndroidApps(systemId: String = "android_apps") = withContext(Dispatchers.IO) {
        val existingSys = systemDao.getSystemById(systemId)
        val defaultSys = PresetData.getDefaultSystems(configStorageManager.getBaseDirPath()).find { it.id == systemId }
        if (existingSys == null && defaultSys != null) {
            systemDao.insertSystem(defaultSys)
        }

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

        var xmlVisibleSet = configStorageManager.loadVisibleAppsXml(systemId)
        if (xmlVisibleSet == null) {
            val hiddenApps = if (systemId == "android_apps") getHiddenAndroidApps() else emptySet()
            val defaultVisible = resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName || hiddenApps.contains(pkg)) null
                else pkg
            }.toSet()
            configStorageManager.saveVisibleAppsXml(systemId, defaultVisible)
            xmlVisibleSet = defaultVisible
        }
        val baseDir = configStorageManager.getBaseDirPath()
        val savedUserDataMap = configStorageManager.loadFavoritesAndRecents()
        val customNamesMap = configStorageManager.loadCustomNames()

        val appRoms = resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null

            // Check XML visibility
            if (!xmlVisibleSet.contains(pkg)) return@mapNotNull null

            val customTitle = customNamesMap[pkg]
            val label = customTitle ?: info.loadLabel(pm).toString()

            // Check if user has custom artwork in RetroLauncher/boxart/
            val customArt = File("$baseDir/boxart/${pkg}.png")
            val artPath = if (customArt.exists()) customArt.absolutePath else ""
            val savedData = savedUserDataMap[pkg]

            GameRomEntity(
                systemId = systemId,
                title = label,
                filePath = pkg,
                fileName = pkg,
                extension = "apk",
                coverArtPath = if (artPath.isNotEmpty()) artPath else null,
                isFavorite = savedData?.isFavorite ?: false,
                lastPlayedTimestamp = savedData?.lastPlayedTimestamp ?: 0L,
                playCount = savedData?.playCount ?: 0
            )
        }

        gameRomDao.deleteRomsBySystem(systemId)
        gameRomDao.insertRoms(appRoms)
    }

    suspend fun scanFolderForRoms(system: SystemEntity) = withContext(Dispatchers.IO) {
        if (system.id in listOf("android_apps", "android_games", "android_emulators") || system.defaultLaunchMode == "ANDROID_APP") {
            scanAndroidApps(system.id)
            return@withContext
        }

        val folder = File(system.folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        // Always clear previous ROMs for this system before inserting newly scanned files
        gameRomDao.deleteRomsBySystem(system.id)

        val allowedExts = system.allowedExtensions
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        val foundFiles = mutableListOf<File>()

        try {
            folder.walkTopDown()
                .onEnter { dir ->
                    // Do not enter hidden directories starting with '.'
                    !dir.name.startsWith(".")
                }
                .filter { file ->
                    file.isFile && !file.name.startsWith(".") && allowedExts.any { ext -> file.name.lowercase().endsWith(ext) }
                }
                .forEach { foundFiles.add(it) }
        } catch (e: Exception) {
            android.util.Log.e("LauncherRepository", "Error scanning folder ${system.folderPath} for system ${system.name}", e)
        }

        val savedUserDataMap = configStorageManager.loadFavoritesAndRecents()
        val displaySettings = configStorageManager.loadDisplaySettings()
        val baseDir = configStorageManager.getBaseDirPath()
        val customNamesMap = configStorageManager.loadCustomNames()

        val romEntities = foundFiles.map { file ->
            val ext = file.extension.lowercase()
            val customTitle = customNamesMap[file.absolutePath]
            val titleName = if (customTitle != null) {
                customTitle
            } else if (system.isArcade) {
                com.example.data.util.ArcadeDatabase.getArcadeTitle(file.nameWithoutExtension, displaySettings.customArcadeDbPath)
            } else {
                file.nameWithoutExtension
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
            }
            val savedData = savedUserDataMap[file.absolutePath]
            val coverPath = findRomArtwork(file, system, baseDir)

            GameRomEntity(
                systemId = system.id,
                title = titleName,
                filePath = file.absolutePath,
                fileName = file.name,
                extension = ext,
                coverArtPath = coverPath,
                isFavorite = savedData?.isFavorite ?: false,
                lastPlayedTimestamp = savedData?.lastPlayedTimestamp ?: 0L,
                playCount = savedData?.playCount ?: 0
            )
        }

        if (romEntities.isNotEmpty()) {
            gameRomDao.insertRoms(romEntities)
        }
    }

    private fun findRomArtwork(file: File, system: SystemEntity, baseDir: String): String? {
        val stem = file.nameWithoutExtension
        val exts = listOf(".png", ".jpg", ".jpeg", ".webp", ".PNG", ".JPG", ".JPEG", ".WEBP")
        val mediaSubdirs = listOf("icons", "media", "artworks", "covers", "boxart", "images")

        // 1. Check system.boxartFolderPath if configured
        if (system.boxartFolderPath.isNotBlank()) {
            val mediaDir = File(system.boxartFolderPath)
            if (mediaDir.exists() && mediaDir.isDirectory) {
                // Check subfolders inside boxartFolderPath (e.g. boxartFolderPath/icons/zelda.png)
                for (subdir in mediaSubdirs) {
                    val subFolder = File(mediaDir, subdir)
                    if (subFolder.exists() && subFolder.isDirectory) {
                        for (ext in exts) {
                            val candidate = File(subFolder, "$stem$ext")
                            if (candidate.exists() && candidate.isFile) {
                                return candidate.absolutePath
                            }
                        }
                    }
                }
                // Check direct children inside boxartFolderPath
                for (ext in exts) {
                    val candidate = File(mediaDir, "$stem$ext")
                    if (candidate.exists() && candidate.isFile) {
                        return candidate.absolutePath
                    }
                }
            }
        }

        // 2. Check system.folderPath (system root ROM folder)
        if (system.folderPath.isNotBlank()) {
            val sysRoot = File(system.folderPath)
            if (sysRoot.exists() && sysRoot.isDirectory) {
                // Check subfolders inside system root (e.g. system.folderPath/icons/zelda.png)
                for (subdir in mediaSubdirs) {
                    val subFolder = File(sysRoot, subdir)
                    if (subFolder.exists() && subFolder.isDirectory) {
                        for (ext in exts) {
                            val candidate = File(subFolder, "$stem$ext")
                            if (candidate.exists() && candidate.isFile) {
                                return candidate.absolutePath
                            }
                        }
                    }
                }
                // Check direct children inside system root
                for (ext in exts) {
                    val candidate = File(sysRoot, "$stem$ext")
                    if (candidate.exists() && candidate.isFile && candidate.absolutePath != file.absolutePath) {
                        return candidate.absolutePath
                    }
                }
            }
        }

        // 3. Check file.parentFile (if the ROM is in a subfolder like system.folderPath/z/zelda.zip)
        val romParent = file.parentFile
        if (romParent != null && romParent.exists() && romParent.absolutePath != system.folderPath) {
            for (subdir in mediaSubdirs) {
                val subFolder = File(romParent, subdir)
                if (subFolder.exists() && subFolder.isDirectory) {
                    for (ext in exts) {
                        val candidate = File(subFolder, "$stem$ext")
                        if (candidate.exists() && candidate.isFile) {
                            return candidate.absolutePath
                        }
                    }
                }
            }
            for (ext in exts) {
                val candidate = File(romParent, "$stem$ext")
                if (candidate.exists() && candidate.isFile && candidate.absolutePath != file.absolutePath) {
                    return candidate.absolutePath
                }
            }
        }

        // 4. Check Central application storage ($baseDir)
        val centralSubdirs = listOf("icons", "boxart", "media", "covers", "artworks")
        for (subdir in centralSubdirs) {
            val centralDir = File("$baseDir/$subdir/${system.id}")
            if (centralDir.exists() && centralDir.isDirectory) {
                for (ext in exts) {
                    val candidate = File(centralDir, "$stem$ext")
                    if (candidate.exists() && candidate.isFile) {
                        return candidate.absolutePath
                    }
                }
            }
        }

        return null
    }

    suspend fun scanAllSystemFolders() = withContext(Dispatchers.IO) {
        val systems = systemDao.getAllSystems().first()
        for (sys in systems) {
            scanFolderForRoms(sys)
        }
    }

    private suspend fun generateSampleDemoRoms(systems: List<SystemEntity>) {
        for (sys in systems) {
            val folder = File(sys.folderPath)
            if (!folder.exists()) folder.mkdirs()
            createDemoRomInFolder(sys, folder)
        }
    }

    private suspend fun createDemoRomInFolder(sys: SystemEntity, folder: File) {
        val ext = sys.allowedExtensions.split(",").firstOrNull()?.trim() ?: ".zip"
        val sampleTitles = when (sys.id) {
            "snes" -> listOf("Super Mario World", "The Legend of Zelda - A Link to the Past", "Chrono Trigger")
            "ps2" -> listOf("Grand Theft Auto - San Andreas", "Kingdom Hearts II", "Shadow of the Colossus")
            "3ds" -> listOf("Pokemon Sun and Moon", "Super Mario 3D Land", "Fire Emblem Awakening")
            "psp" -> listOf("God of War - Chains of Olympus", "Crisis Core - Final Fantasy VII", "Tekken 6")
            "n64" -> listOf("Super Mario 64", "The Legend of Zelda - Ocarina of Time", "GoldenEye 007")
            "gba" -> listOf("Pokemon Emerald Version", "Metroid Fusion", "Castlevania - Aria of Sorrow")
            "ps1" -> listOf("Castlevania - Symphony of the Night", "Final Fantasy VII", "Metal Gear Solid")
            "gc" -> listOf("Super Smash Bros. Melee", "The Legend of Zelda - The Wind Waker", "Super Mario Sunshine")
            else -> listOf("Demo Game 1", "Demo Game 2")
        }

        val roms = mutableListOf<GameRomEntity>()
        sampleTitles.forEach { title ->
            val fileName = "${title.replace(" ", "_")}$ext"
            val file = File(folder, fileName)
            if (!file.exists()) {
                try {
                    file.writeText("# Sample ROM File for $title ($sys.name)\nCreated by Retro Launcher")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            roms.add(
                GameRomEntity(
                    systemId = sys.id,
                    title = title,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    extension = ext.removePrefix(".")
                )
            )
        }
        gameRomDao.insertRoms(roms)
    }

    fun saveDisplaySettings(settings: DisplaySettings): Boolean {
        return configStorageManager.saveDisplaySettings(settings)
    }

    fun loadDisplaySettings(): DisplaySettings {
        return configStorageManager.loadDisplaySettings()
    }

    fun saveRomListSettings(settings: com.example.data.model.RomListSettings): Boolean {
        return configStorageManager.saveRomListSettings(settings)
    }

    fun loadRomListSettings(): com.example.data.model.RomListSettings {
        return configStorageManager.loadRomListSettings()
    }

    fun saveGamepadSettings(settings: com.example.data.model.GamepadSettings): Boolean {
        return configStorageManager.saveGamepadSettings(settings)
    }

    fun loadGamepadSettings(): com.example.data.model.GamepadSettings {
        return configStorageManager.loadGamepadSettings()
    }

    fun saveBottomBarSettings(settings: com.example.data.model.BottomBarSettings): Boolean {
        return configStorageManager.saveBottomBarSettings(settings)
    }

    fun loadBottomBarSettings(): com.example.data.model.BottomBarSettings {
        return configStorageManager.loadBottomBarSettings()
    }
}
