package com.example.data.config

import android.content.Context
import android.os.Environment
import android.util.Xml
import com.example.data.db.GameRomEntity
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity
import com.example.data.model.BottomBarSettings
import com.example.data.model.DisplayMode
import com.example.data.model.DisplaySettings
import com.example.data.model.GamepadSettings
import com.example.data.model.RomListSettings
import com.example.data.model.RomListStyle
import com.example.data.model.TextAlignmentOption
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.io.StringWriter

class ConfigStorageManager(private val context: Context) {

    fun resolveBaseDir(): File {
        val prefs = context.getSharedPreferences("config_storage_prefs", Context.MODE_PRIVATE)
        val customPath = prefs.getString("custom_base_dir_path", null)
        if (!customPath.isNullOrBlank()) {
            val customDir = File(customPath)
            if (!customDir.exists()) {
                try { customDir.mkdirs() } catch (_: Exception) {}
            }
            if (customDir.exists()) return customDir
        }

        val docsDir = File(
            Environment.getExternalStorageDirectory(),
            "RetroLauncher"
        )
        if (!docsDir.exists()) {
            try {
                docsDir.mkdirs()
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
        return if (docsDir.exists() && docsDir.canWrite()) {
            docsDir
        } else {
            val appDir = File(context.getExternalFilesDir(null), "RetroLauncher")
            if (!appDir.exists()) appDir.mkdirs()
            appDir
        }
    }

    private val baseDir: File get() = resolveBaseDir()

    private fun createSerializer(writer: java.io.Writer): XmlSerializer {
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        try {
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        } catch (_: Exception) {}
        try {
            serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "    ")
        } catch (_: Exception) {}
        try {
            serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n")
        } catch (_: Exception) {}
        return serializer
    }

    fun saveCachedRomList(systemId: String, roms: List<GameRomEntity>): Boolean {
        var db: SQLiteDatabase? = null
        return try {
            val dataDir = File(baseDir, "data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            // Delete old XML cache file if it exists
            val oldXmlFile = File(dataDir, systemId)
            if (oldXmlFile.exists() && !oldXmlFile.isDirectory) {
                oldXmlFile.delete()
            }

            val dbFile = File(dataDir, "rom_cache.db")
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_roms (
                    id INTEGER,
                    systemId TEXT,
                    title TEXT,
                    filePath TEXT,
                    fileName TEXT,
                    extension TEXT,
                    coverArtPath TEXT,
                    customEmulatorOverride TEXT,
                    isFavorite INTEGER,
                    isCompleted INTEGER,
                    lastPlayedTimestamp INTEGER,
                    playCount INTEGER,
                    PRIMARY KEY (systemId, filePath)
                )
            """.trimIndent())

            db.beginTransaction()
            try {
                // Delete previous entries for this system
                db.delete("cached_roms", "systemId = ?", arrayOf(systemId))

                // Insert new ones
                for (rom in roms) {
                    val values = ContentValues().apply {
                        put("id", rom.id)
                        put("systemId", rom.systemId)
                        put("title", rom.title)
                        put("filePath", rom.filePath)
                        put("fileName", rom.fileName)
                        put("extension", rom.extension)
                        put("coverArtPath", rom.coverArtPath)
                        put("customEmulatorOverride", rom.customEmulatorOverride)
                        put("isFavorite", if (rom.isFavorite) 1 else 0)
                        put("isCompleted", if (rom.isCompleted) 1 else 0)
                        put("lastPlayedTimestamp", rom.lastPlayedTimestamp)
                        put("playCount", rom.playCount)
                    }
                    db.insertWithOnConflict("cached_roms", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db?.close()
        }
    }

    fun loadCachedRomList(systemId: String): List<GameRomEntity> {
        val list = mutableListOf<GameRomEntity>()
        var db: SQLiteDatabase? = null
        try {
            val dataDir = File(baseDir, "data")
            val dbFile = File(dataDir, "rom_cache.db")
            if (!dbFile.exists()) return emptyList()

            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            
            // Check if table exists before querying
            val tableCheckCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='cached_roms'",
                null
            )
            val tableExists = tableCheckCursor.use { it.count > 0 }
            if (!tableExists) return emptyList()

            val cursor = db.rawQuery(
                "SELECT * FROM cached_roms WHERE systemId = ? ORDER BY title ASC",
                arrayOf(systemId)
            )
            cursor.use { c ->
                val idCol = c.getColumnIndex("id")
                val systemIdCol = c.getColumnIndex("systemId")
                val titleCol = c.getColumnIndex("title")
                val filePathCol = c.getColumnIndex("filePath")
                val fileNameCol = c.getColumnIndex("fileName")
                val extensionCol = c.getColumnIndex("extension")
                val coverArtPathCol = c.getColumnIndex("coverArtPath")
                val customEmulatorOverrideCol = c.getColumnIndex("customEmulatorOverride")
                val isFavoriteCol = c.getColumnIndex("isFavorite")
                val isCompletedCol = c.getColumnIndex("isCompleted")
                val lastPlayedTimestampCol = c.getColumnIndex("lastPlayedTimestamp")
                val playCountCol = c.getColumnIndex("playCount")

                while (c.moveToNext()) {
                    list.add(
                        GameRomEntity(
                            id = if (idCol != -1) c.getLong(idCol) else 0L,
                            systemId = if (systemIdCol != -1) c.getString(systemIdCol) else systemId,
                            title = if (titleCol != -1) c.getString(titleCol) else "",
                            filePath = if (filePathCol != -1) c.getString(filePathCol) else "",
                            fileName = if (fileNameCol != -1) c.getString(fileNameCol) else "",
                            extension = if (extensionCol != -1) c.getString(extensionCol) else "",
                            coverArtPath = if (coverArtPathCol != -1) c.getString(coverArtPathCol) else null,
                            customEmulatorOverride = if (customEmulatorOverrideCol != -1) c.getString(customEmulatorOverrideCol) else null,
                            isFavorite = if (isFavoriteCol != -1) c.getInt(isFavoriteCol) == 1 else false,
                            isCompleted = if (isCompletedCol != -1) c.getInt(isCompletedCol) == 1 else false,
                            lastPlayedTimestamp = if (lastPlayedTimestampCol != -1) c.getLong(lastPlayedTimestampCol) else 0L,
                            playCount = if (playCountCol != -1) c.getInt(playCountCol) else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return list
    }

    fun setBaseDirPath(newPath: String, moveCurrentFiles: Boolean): Boolean {
        return try {
            val oldDir = resolveBaseDir()
            val newDir = File(newPath)
            if (!newDir.exists()) {
                newDir.mkdirs()
            }
            if (moveCurrentFiles && oldDir.exists() && oldDir.absolutePath != newDir.absolutePath) {
                oldDir.listFiles()?.forEach { file ->
                    val destFile = File(newDir, file.name)
                    if (file.isDirectory) {
                        file.copyRecursively(destFile, overwrite = true)
                    } else {
                        file.copyTo(destFile, overwrite = true)
                    }
                }
            }
            val prefs = context.getSharedPreferences("config_storage_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("custom_base_dir_path", newDir.absolutePath).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private val customEmulatorsDir: File by lazy {
        val dir = File(baseDir, "custom_emulators")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val _customIconsDir: File by lazy {
        val dir = File(baseDir, "custom_icons")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    fun getBaseDirPath(): String = baseDir.absolutePath

    fun getCustomIconsDir(): File {
        if (!_customIconsDir.exists()) _customIconsDir.mkdirs()
        return _customIconsDir
    }

    fun getCustomIconsDirPath(): String = getCustomIconsDir().absolutePath

    fun getCustomIconFiles(): List<File> {
        val dir = getCustomIconsDir()
        return dir.listFiles { _, name ->
            name.endsWith(".png", ignoreCase = true) ||
            name.endsWith(".jpg", ignoreCase = true) ||
            name.endsWith(".jpeg", ignoreCase = true) ||
            name.endsWith(".webp", ignoreCase = true)
        }?.sortedBy { it.name } ?: emptyList()
    }

    fun getSfxDir(): File {
        val dir = File(baseDir, "sfx")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBgmDir(): File {
        val dir = File(baseDir, "bgm")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // --- ACTIVE SYSTEM ID XML ---

    fun saveActiveSystemId(systemId: String): Boolean {
        return try {
            val file = File(baseDir, "active_system.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "ActiveSystem")
            serializer.startTag("", "systemId").text(systemId).endTag("", "systemId")
            serializer.endTag("", "ActiveSystem")
            serializer.endDocument()

            FileOutputStream(file).use { out ->
                out.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadActiveSystemId(): String? {
        return try {
            val file = File(baseDir, "active_system.xml")
            if (!file.exists()) return null
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")
            var eventType = parser.eventType
            var systemId: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "systemId") {
                        systemId = parser.nextText()
                    }
                }
                eventType = parser.next()
            }
            systemId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- CUSTOM NAMES XML ---

    fun saveCustomNames(customNames: Map<String, String>): Boolean {
        return try {
            val file = File(baseDir, "custom_name.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "CustomNames")

            for ((filePath, customName) in customNames) {
                serializer.startTag("", "Game")
                serializer.startTag("", "filePath").text(filePath).endTag("", "filePath")
                serializer.startTag("", "customName").text(customName).endTag("", "customName")
                serializer.endTag("", "Game")
            }

            serializer.endTag("", "CustomNames")
            serializer.endDocument()

            FileOutputStream(file).use { out ->
                out.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadCustomNames(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val file = File(baseDir, "custom_name.xml")
            if (!file.exists()) return result
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")
            var eventType = parser.eventType
            var currentFilePath: String? = null
            var currentCustomName: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "filePath") {
                            currentFilePath = parser.nextText()
                        } else if (tagName == "customName") {
                            currentCustomName = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "Game") {
                            if (currentFilePath != null && currentCustomName != null) {
                                result[currentFilePath] = currentCustomName
                            }
                            currentFilePath = null
                            currentCustomName = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // --- CUSTOM ICONS XML ---

    fun saveCustomIcons(customIcons: Map<String, String>): Boolean {
        return try {
            val file = File(baseDir, "custom_icon.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "CustomIcons")

            for ((filePath, customIcon) in customIcons) {
                serializer.startTag("", "Game")
                serializer.startTag("", "filePath").text(filePath).endTag("", "filePath")
                serializer.startTag("", "customIcon").text(customIcon).endTag("", "customIcon")
                serializer.endTag("", "Game")
            }

            serializer.endTag("", "CustomIcons")
            serializer.endDocument()

            FileOutputStream(file).use { out ->
                out.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadCustomIcons(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val file = File(baseDir, "custom_icon.xml")
            if (!file.exists()) return result
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")
            var eventType = parser.eventType
            var currentFilePath: String? = null
            var currentCustomIcon: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "filePath") {
                            currentFilePath = parser.nextText()
                        } else if (tagName == "customIcon") {
                            currentCustomIcon = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "Game") {
                            if (currentFilePath != null && currentCustomIcon != null) {
                                result[currentFilePath] = currentCustomIcon
                            }
                            currentFilePath = null
                            currentCustomIcon = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    // --- DISPLAY SETTINGS XML ---

    fun saveDisplaySettings(settings: DisplaySettings): Boolean {
        return try {
            val file = File(baseDir, "display_settings.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "DisplaySettings")

            serializer.startTag("", "mode").text(settings.mode.name).endTag("", "mode")
            serializer.startTag("", "customWidthPx").text(settings.customWidthPx.toString()).endTag("", "customWidthPx")
            serializer.startTag("", "customHeightPx").text(settings.customHeightPx.toString()).endTag("", "customHeightPx")
            serializer.startTag("", "marginTopDp").text(settings.marginTopDp.toString()).endTag("", "marginTopDp")
            serializer.startTag("", "marginBottomDp").text(settings.marginBottomDp.toString()).endTag("", "marginBottomDp")
            serializer.startTag("", "marginLeftDp").text(settings.marginLeftDp.toString()).endTag("", "marginLeftDp")
            serializer.startTag("", "marginRightDp").text(settings.marginRightDp.toString()).endTag("", "marginRightDp")
            serializer.startTag("", "aspectRatio").text(settings.aspectRatio).endTag("", "aspectRatio")
            serializer.startTag("", "uiScale").text(settings.uiScale.toString()).endTag("", "uiScale")
            serializer.startTag("", "launcherTitle").text(settings.launcherTitle).endTag("", "launcherTitle")
            serializer.startTag("", "launcherIconPath").text(settings.launcherIconPath).endTag("", "launcherIconPath")
            serializer.startTag("", "showSystemMainMenuTitle").text(settings.showSystemMainMenuTitle.toString()).endTag("", "showSystemMainMenuTitle")
            serializer.startTag("", "bottomSystemMainMenuTitle").text(settings.bottomSystemMainMenuTitle.toString()).endTag("", "bottomSystemMainMenuTitle")
            serializer.startTag("", "showSystemMainMenuEditIcon").text(settings.showSystemMainMenuEditIcon.toString()).endTag("", "showSystemMainMenuEditIcon")
            serializer.startTag("", "systemMainMenuStyle").text(settings.systemMainMenuStyle).endTag("", "systemMainMenuStyle")
            serializer.startTag("", "systemMainMenuGridStyle").text(settings.systemMainMenuGridStyle).endTag("", "systemMainMenuGridStyle")
            serializer.startTag("", "centeredLastGridItem").text(settings.centeredLastGridItem.toString()).endTag("", "centeredLastGridItem")
            serializer.startTag("", "systemMenuDisplayColumns").text(settings.systemMenuDisplayColumns.toString()).endTag("", "systemMenuDisplayColumns")
            serializer.startTag("", "systemMenuDisplayRows").text(settings.systemMenuDisplayRows.toString()).endTag("", "systemMenuDisplayRows")
            serializer.startTag("", "systemMenuActualColumns").text(settings.systemMenuActualColumns.toString()).endTag("", "systemMenuActualColumns")
            serializer.startTag("", "systemMenuActualRows").text(settings.systemMenuActualRows.toString()).endTag("", "systemMenuActualRows")
            serializer.startTag("", "showSystemTitle").text(settings.showSystemTitle.toString()).endTag("", "showSystemTitle")
            serializer.startTag("", "bottomSystemTitle").text(settings.bottomSystemTitle.toString()).endTag("", "bottomSystemTitle")
            serializer.startTag("", "showSubSystemTitle").text(settings.showSubSystemTitle.toString()).endTag("", "showSubSystemTitle")
            serializer.startTag("", "swapTopAndBottomBar").text(settings.swapTopAndBottomBar.toString()).endTag("", "swapTopAndBottomBar")
            serializer.startTag("", "systemMenuTextSizeSp").text(settings.systemMenuTextSizeSp.toString()).endTag("", "systemMenuTextSizeSp")
            serializer.startTag("", "systemMenuTextAlignment").text(settings.systemMenuTextAlignment).endTag("", "systemMenuTextAlignment")
            serializer.startTag("", "backgroundColorHex").text(settings.backgroundColorHex).endTag("", "backgroundColorHex")
            serializer.startTag("", "surfaceColorHex").text(settings.surfaceColorHex).endTag("", "surfaceColorHex")
            serializer.startTag("", "primaryColorHex").text(settings.primaryColorHex).endTag("", "primaryColorHex")
            serializer.startTag("", "textColorHex").text(settings.textColorHex).endTag("", "textColorHex")
            serializer.startTag("", "cardBackgroundColorHex").text(settings.cardBackgroundColorHex).endTag("", "cardBackgroundColorHex")
            serializer.startTag("", "marqueeSpeed").text(settings.marqueeSpeed.toString()).endTag("", "marqueeSpeed")
            serializer.startTag("", "marqueeDelayMillis").text(settings.marqueeDelayMillis.toString()).endTag("", "marqueeDelayMillis")
            serializer.startTag("", "systemMainMenuTitle").text(settings.systemMainMenuTitle).endTag("", "systemMainMenuTitle")
            serializer.startTag("", "systemMainMenuDescription").text(settings.systemMainMenuDescription).endTag("", "systemMainMenuDescription")
            serializer.startTag("", "enableNavigationSound").text(settings.enableNavigationSound.toString()).endTag("", "enableNavigationSound")
            serializer.startTag("", "enableBgm").text(settings.enableBgm.toString()).endTag("", "enableBgm")
            serializer.startTag("", "mainMenuIconGridScalePercent").text(settings.mainMenuIconGridScalePercent.toString()).endTag("", "mainMenuIconGridScalePercent")
            serializer.startTag("", "selectedSfxFileName").text(settings.selectedSfxFileName).endTag("", "selectedSfxFileName")
            serializer.startTag("", "maxRecentCount").text(settings.maxRecentCount.toString()).endTag("", "maxRecentCount")
            serializer.startTag("", "customArcadeDbPath").text(settings.customArcadeDbPath).endTag("", "customArcadeDbPath")
            serializer.startTag("", "autoHideScrollbar").text(settings.autoHideScrollbar.toString()).endTag("", "autoHideScrollbar")
            serializer.startTag("", "scrollbarShowDurationMs").text(settings.scrollbarShowDurationMs.toString()).endTag("", "scrollbarShowDurationMs")
            serializer.startTag("", "systemMenuTileMarginLeftDp").text(settings.systemMenuTileMarginLeftDp.toString()).endTag("", "systemMenuTileMarginLeftDp")
            serializer.startTag("", "systemMenuTileMarginRightDp").text(settings.systemMenuTileMarginRightDp.toString()).endTag("", "systemMenuTileMarginRightDp")
            serializer.startTag("", "systemMainMenuIconPath").text(settings.systemMainMenuIconPath).endTag("", "systemMainMenuIconPath")
            serializer.startTag("", "topBarColorHex").text(settings.topBarColorHex).endTag("", "topBarColorHex")
            serializer.startTag("", "bottomBarColorHex").text(settings.bottomBarColorHex).endTag("", "bottomBarColorHex")
            serializer.startTag("", "removeCharsFromGameNames").text(settings.removeCharsFromGameNames).endTag("", "removeCharsFromGameNames")
            serializer.startTag("", "enableRomIconPopUp").text(settings.enableRomIconPopUp.toString()).endTag("", "enableRomIconPopUp")
            serializer.startTag("", "romIconPopUpShowNds").text(settings.romIconPopUpShowNds.toString()).endTag("", "romIconPopUpShowNds")
            serializer.startTag("", "romIconPopUpShowPsp").text(settings.romIconPopUpShowPsp.toString()).endTag("", "romIconPopUpShowPsp")
            serializer.startTag("", "romIconPopUpShowAndroid").text(settings.romIconPopUpShowAndroid.toString()).endTag("", "romIconPopUpShowAndroid")
            serializer.startTag("", "romIconPopUpShowDefault").text(settings.romIconPopUpShowDefault.toString()).endTag("", "romIconPopUpShowDefault")
            serializer.startTag("", "romIconPopUpShowInGridStyle").text(settings.romIconPopUpShowInGridStyle.toString()).endTag("", "romIconPopUpShowInGridStyle")
            serializer.startTag("", "romIconPopUpTimeoutMs").text(settings.romIconPopUpTimeoutMs.toString()).endTag("", "romIconPopUpTimeoutMs")
            serializer.startTag("", "romIconPopUpAlignment").text(settings.romIconPopUpAlignment).endTag("", "romIconPopUpAlignment")
            serializer.startTag("", "romIconPopUpWidthPercent").text(settings.romIconPopUpWidthPercent.toString()).endTag("", "romIconPopUpWidthPercent")
            serializer.startTag("", "sleepTimeoutMode").text(settings.sleepTimeoutMode).endTag("", "sleepTimeoutMode")
            serializer.startTag("", "sleepTimeoutSeconds").text(settings.sleepTimeoutSeconds.toString()).endTag("", "sleepTimeoutSeconds")
            serializer.startTag("", "showLaunchToast").text(settings.showLaunchToast.toString()).endTag("", "showLaunchToast")
            serializer.startTag("", "enableImmersiveMode").text(settings.enableImmersiveMode.toString()).endTag("", "enableImmersiveMode")
            serializer.startTag("", "showFirstLastReorderButtons").text(settings.showFirstLastReorderButtons.toString()).endTag("", "showFirstLastReorderButtons")
            serializer.startTag("", "showRomDetailsButton").text(settings.showRomDetailsButton.toString()).endTag("", "showRomDetailsButton")
            serializer.startTag("", "showRomDetailsInGridStyle").text(settings.showRomDetailsInGridStyle.toString()).endTag("", "showRomDetailsInGridStyle")
            serializer.startTag("", "showRomFavoriteButton").text(settings.showRomFavoriteButton.toString()).endTag("", "showRomFavoriteButton")
            serializer.startTag("", "showRomFavoriteInGridStyle").text(settings.showRomFavoriteInGridStyle.toString()).endTag("", "showRomFavoriteInGridStyle")
            serializer.startTag("", "showRomCompleteButton").text(settings.showRomCompleteButton.toString()).endTag("", "showRomCompleteButton")
            serializer.startTag("", "showRomCompleteInGridStyle").text(settings.showRomCompleteInGridStyle.toString()).endTag("", "showRomCompleteInGridStyle")
            serializer.startTag("", "showTopBar").text(settings.showTopBar.toString()).endTag("", "showTopBar")
            serializer.startTag("", "showTopBarSettingsIcon").text(settings.showTopBarSettingsIcon.toString()).endTag("", "showTopBarSettingsIcon")
            serializer.startTag("", "showTopBarTitleIcon").text(settings.showTopBarTitleIcon.toString()).endTag("", "showTopBarTitleIcon")
            serializer.startTag("", "topBarTitleAlignment").text(settings.topBarTitleAlignment).endTag("", "topBarTitleAlignment")
            serializer.startTag("", "enableSwipeSystemNavigation").text(settings.enableSwipeSystemNavigation.toString()).endTag("", "enableSwipeSystemNavigation")
            serializer.startTag("", "ignoreSystemAnimationScale").text(settings.ignoreSystemAnimationScale.toString()).endTag("", "ignoreSystemAnimationScale")

            serializer.endTag("", "DisplaySettings")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadDisplaySettings(): DisplaySettings {
        val file = File(baseDir, "display_settings.xml")
        if (!file.exists()) return DisplaySettings()

        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var mode = DisplayMode.FULL_SCREEN
            var customWidthPx = 1080
            var customHeightPx = 1920
            var marginTopDp = 0
            var marginBottomDp = 0
            var marginLeftDp = 0
            var marginRightDp = 0
            var aspectRatio = "16:9"
            var uiScale = 1.0f
            var launcherTitle = "RetroLauncher"
            var launcherIconPath = "gamepad"
            var showSystemMainMenuTitle = true
            var bottomSystemMainMenuTitle = false
            var showSystemMainMenuEditIcon = false
            var systemMainMenuStyle = "ICON_GRID"
            var systemMainMenuGridStyle = "ICON_GRID"
            var centeredLastGridItem = false
            var systemMenuDisplayColumns = 4
            var systemMenuDisplayRows = 4
            var systemMenuActualColumns = 4
            var systemMenuActualRows = 4
            var showSystemTitle = true
            var bottomSystemTitle = false
            var showSubSystemTitle = true
            var swapTopAndBottomBar = false
            var systemMenuTextSizeSp = 16
            var systemMenuTextAlignment = "LEFT"
            var backgroundColorHex = "#121212"
            var surfaceColorHex = "#1E1E1E"
            var primaryColorHex = "#3D5AFE"
            var textColorHex = "#FFFFFF"
            var cardBackgroundColorHex = "#2A2A2A"
            var marqueeSpeed = 30
            var marqueeDelayMillis = 1200
            var systemMainMenuTitle = "SYSTEM MAIN MENU"
            var systemMainMenuDescription = "Select a console / system to launch games"
            var enableNavigationSound = true
            var enableBgm = true
            var mainMenuIconGridScalePercent = 100
            var selectedSfxFileName = ""
            var maxRecentCount = 30
            var customArcadeDbPath = ""
            var autoHideScrollbar = true
            var scrollbarShowDurationMs = 1500
            var systemMenuTileMarginLeftDp = 0
            var systemMenuTileMarginRightDp = 0
            var systemMainMenuIconPath = "gamepad"
            var topBarColorHex = ""
            var bottomBarColorHex = ""
            var removeCharsFromGameNames = ""
            var enableRomIconPopUp = false
            var romIconPopUpShowNds = true
            var romIconPopUpShowPsp = true
            var romIconPopUpShowAndroid = true
            var romIconPopUpShowDefault = true
            var romIconPopUpShowInGridStyle = true
            var romIconPopUpTimeoutMs = 1000
            var romIconPopUpAlignment = "middle_center"
            var romIconPopUpWidthPercent = 30
            var sleepTimeoutMode = "DEVICE"
            var sleepTimeoutSeconds = 30
            var showLaunchToast = true
            var enableImmersiveMode = true
            var showFirstLastReorderButtons = true
            var showRomDetailsButton = true
            var showRomDetailsInGridStyle = true
            var showRomFavoriteButton = true
            var showRomFavoriteInGridStyle = true
            var showRomCompleteButton = true
            var showRomCompleteInGridStyle = true
            var showTopBar = true
            var showTopBarSettingsIcon = true
            var showTopBarTitleIcon = true
            var topBarTitleAlignment = "LEFT"
            var enableSwipeSystemNavigation = false
            var ignoreSystemAnimationScale = false

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "mode" -> mode = runCatching { DisplayMode.valueOf(text) }.getOrDefault(DisplayMode.FULL_SCREEN)
                                "customWidthPx" -> customWidthPx = text.toIntOrNull() ?: 1080
                                "customHeightPx" -> customHeightPx = text.toIntOrNull() ?: 1920
                                "marginTopDp" -> marginTopDp = text.toIntOrNull() ?: 0
                                "marginBottomDp" -> marginBottomDp = text.toIntOrNull() ?: 0
                                "marginLeftDp" -> marginLeftDp = text.toIntOrNull() ?: 0
                                "marginRightDp" -> marginRightDp = text.toIntOrNull() ?: 0
                                "aspectRatio" -> aspectRatio = text
                                "uiScale" -> uiScale = text.toFloatOrNull() ?: 1.0f
                                "launcherTitle" -> launcherTitle = text
                                "launcherIconPath" -> launcherIconPath = text
                                "showSystemMainMenuTitle" -> showSystemMainMenuTitle = text.toBooleanStrictOrNull() ?: true
                                "bottomSystemMainMenuTitle" -> bottomSystemMainMenuTitle = text.toBooleanStrictOrNull() ?: false
                                "showSystemMainMenuEditIcon" -> showSystemMainMenuEditIcon = text.toBooleanStrictOrNull() ?: false
                                "systemMainMenuStyle" -> systemMainMenuStyle = text
                                "systemMainMenuGridStyle" -> systemMainMenuGridStyle = text
                                "centeredLastGridItem" -> centeredLastGridItem = text.toBooleanStrictOrNull() ?: false
                                "systemMenuDisplayColumns" -> systemMenuDisplayColumns = text.toIntOrNull() ?: 4
                                "systemMenuDisplayRows" -> systemMenuDisplayRows = text.toIntOrNull() ?: 4
                                "systemMenuActualColumns" -> systemMenuActualColumns = text.toIntOrNull() ?: 4
                                "systemMenuActualRows" -> systemMenuActualRows = text.toIntOrNull() ?: 4
                                "showSystemTitle" -> showSystemTitle = text.toBooleanStrictOrNull() ?: true
                                "bottomSystemTitle" -> bottomSystemTitle = text.toBooleanStrictOrNull() ?: false
                                "showSubSystemTitle" -> showSubSystemTitle = text.toBooleanStrictOrNull() ?: true
                                "swapTopAndBottomBar" -> swapTopAndBottomBar = text.toBooleanStrictOrNull() ?: false
                                "systemMenuTextSizeSp" -> systemMenuTextSizeSp = text.toIntOrNull() ?: 16
                                "systemMenuTextAlignment" -> systemMenuTextAlignment = text
                                "backgroundColorHex" -> backgroundColorHex = text
                                "surfaceColorHex" -> surfaceColorHex = text
                                "primaryColorHex" -> primaryColorHex = text
                                "textColorHex" -> textColorHex = text
                                "cardBackgroundColorHex" -> cardBackgroundColorHex = text
                                "marqueeSpeed" -> marqueeSpeed = text.toIntOrNull() ?: 30
                                "marqueeDelayMillis" -> marqueeDelayMillis = text.toIntOrNull() ?: 1200
                                "systemMainMenuTitle" -> systemMainMenuTitle = text
                                "systemMainMenuDescription" -> systemMainMenuDescription = text
                                "enableNavigationSound" -> enableNavigationSound = text.toBooleanStrictOrNull() ?: true
                                "enableBgm" -> enableBgm = text.toBooleanStrictOrNull() ?: true
                                "mainMenuIconGridScalePercent" -> mainMenuIconGridScalePercent = text.toIntOrNull() ?: 100
                                "selectedSfxFileName" -> selectedSfxFileName = text
                                "maxRecentCount" -> maxRecentCount = text.toIntOrNull() ?: 30
                                "customArcadeDbPath" -> customArcadeDbPath = text
                                "autoHideScrollbar" -> autoHideScrollbar = text.toBooleanStrictOrNull() ?: true
                                "scrollbarShowDurationMs" -> scrollbarShowDurationMs = text.toIntOrNull() ?: 1500
                                "systemMenuTileMarginLeftDp" -> systemMenuTileMarginLeftDp = text.toIntOrNull() ?: 0
                                "systemMenuTileMarginRightDp" -> systemMenuTileMarginRightDp = text.toIntOrNull() ?: 0
                                "systemMainMenuIconPath" -> systemMainMenuIconPath = text
                                "topBarColorHex" -> topBarColorHex = text
                                "bottomBarColorHex" -> bottomBarColorHex = text
                                "removeCharsFromGameNames" -> removeCharsFromGameNames = text
                                "enableRomIconPopUp" -> enableRomIconPopUp = text.toBooleanStrictOrNull() ?: false
                                "romIconPopUpShowNds" -> romIconPopUpShowNds = text.toBooleanStrictOrNull() ?: true
                                "romIconPopUpShowPsp" -> romIconPopUpShowPsp = text.toBooleanStrictOrNull() ?: true
                                "romIconPopUpShowAndroid" -> romIconPopUpShowAndroid = text.toBooleanStrictOrNull() ?: true
                                "romIconPopUpShowDefault" -> romIconPopUpShowDefault = text.toBooleanStrictOrNull() ?: true
                                "romIconPopUpShowInGridStyle" -> romIconPopUpShowInGridStyle = text.toBooleanStrictOrNull() ?: true
                                "romIconPopUpTimeoutMs" -> romIconPopUpTimeoutMs = text.toIntOrNull() ?: 1000
                                "romIconPopUpAlignment" -> romIconPopUpAlignment = text
                                "romIconPopUpWidthPercent" -> romIconPopUpWidthPercent = text.toIntOrNull() ?: 30
                                "sleepTimeoutMode" -> sleepTimeoutMode = text
                                "sleepTimeoutSeconds" -> sleepTimeoutSeconds = text.toIntOrNull() ?: 30
                                "showLaunchToast" -> showLaunchToast = text.toBooleanStrictOrNull() ?: true
                                "enableImmersiveMode" -> enableImmersiveMode = text.toBooleanStrictOrNull() ?: true
                                "showFirstLastReorderButtons" -> showFirstLastReorderButtons = text.toBooleanStrictOrNull() ?: true
                                "showRomDetailsButton" -> showRomDetailsButton = text.toBooleanStrictOrNull() ?: true
                                "showRomDetailsInGridStyle" -> showRomDetailsInGridStyle = text.toBooleanStrictOrNull() ?: true
                                "showRomFavoriteButton" -> showRomFavoriteButton = text.toBooleanStrictOrNull() ?: true
                                "showRomFavoriteInGridStyle" -> showRomFavoriteInGridStyle = text.toBooleanStrictOrNull() ?: true
                                "showRomCompleteButton" -> showRomCompleteButton = text.toBooleanStrictOrNull() ?: true
                                "showRomCompleteInGridStyle" -> showRomCompleteInGridStyle = text.toBooleanStrictOrNull() ?: true
                                "showTopBar" -> showTopBar = text.toBooleanStrictOrNull() ?: true
                                "showTopBarSettingsIcon" -> showTopBarSettingsIcon = text.toBooleanStrictOrNull() ?: true
                                "showTopBarTitleIcon" -> showTopBarTitleIcon = text.toBooleanStrictOrNull() ?: true
                                "topBarTitleAlignment" -> topBarTitleAlignment = text
                                "enableSwipeSystemNavigation" -> enableSwipeSystemNavigation = text.toBooleanStrictOrNull() ?: false
                                "ignoreSystemAnimationScale" -> ignoreSystemAnimationScale = text.toBooleanStrictOrNull() ?: false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            DisplaySettings(
                mode = mode,
                customWidthPx = customWidthPx,
                customHeightPx = customHeightPx,
                marginTopDp = marginTopDp,
                marginBottomDp = marginBottomDp,
                marginLeftDp = marginLeftDp,
                marginRightDp = marginRightDp,
                aspectRatio = aspectRatio,
                uiScale = uiScale,
                launcherTitle = launcherTitle,
                launcherIconPath = launcherIconPath,
                showSystemMainMenuTitle = showSystemMainMenuTitle,
                bottomSystemMainMenuTitle = bottomSystemMainMenuTitle,
                showSystemMainMenuEditIcon = showSystemMainMenuEditIcon,
                systemMainMenuStyle = systemMainMenuStyle,
                systemMainMenuGridStyle = systemMainMenuGridStyle,
                systemMenuDisplayColumns = systemMenuDisplayColumns,
                systemMenuDisplayRows = systemMenuDisplayRows,
                systemMenuActualColumns = systemMenuActualColumns,
                systemMenuActualRows = systemMenuActualRows,
                showSystemTitle = showSystemTitle,
                bottomSystemTitle = bottomSystemTitle,
                showSubSystemTitle = showSubSystemTitle,
                swapTopAndBottomBar = swapTopAndBottomBar,
                systemMenuTextSizeSp = systemMenuTextSizeSp,
                systemMenuTextAlignment = systemMenuTextAlignment,
                backgroundColorHex = backgroundColorHex,
                surfaceColorHex = surfaceColorHex,
                primaryColorHex = primaryColorHex,
                textColorHex = textColorHex,
                cardBackgroundColorHex = cardBackgroundColorHex,
                marqueeSpeed = marqueeSpeed,
                marqueeDelayMillis = marqueeDelayMillis,
                systemMainMenuTitle = systemMainMenuTitle,
                systemMainMenuDescription = systemMainMenuDescription,
                enableNavigationSound = enableNavigationSound,
                enableBgm = enableBgm,
                mainMenuIconGridScalePercent = mainMenuIconGridScalePercent,
                selectedSfxFileName = selectedSfxFileName,
                maxRecentCount = maxRecentCount,
                customArcadeDbPath = customArcadeDbPath,
                autoHideScrollbar = autoHideScrollbar,
                scrollbarShowDurationMs = scrollbarShowDurationMs,
                systemMenuTileMarginLeftDp = systemMenuTileMarginLeftDp,
                systemMenuTileMarginRightDp = systemMenuTileMarginRightDp,
                systemMainMenuIconPath = systemMainMenuIconPath,
                topBarColorHex = topBarColorHex,
                bottomBarColorHex = bottomBarColorHex,
                removeCharsFromGameNames = removeCharsFromGameNames,
                enableRomIconPopUp = enableRomIconPopUp,
                romIconPopUpShowNds = romIconPopUpShowNds,
                romIconPopUpShowPsp = romIconPopUpShowPsp,
                romIconPopUpShowAndroid = romIconPopUpShowAndroid,
                romIconPopUpShowDefault = romIconPopUpShowDefault,
                romIconPopUpShowInGridStyle = romIconPopUpShowInGridStyle,
                romIconPopUpTimeoutMs = romIconPopUpTimeoutMs,
                romIconPopUpAlignment = romIconPopUpAlignment,
                romIconPopUpWidthPercent = romIconPopUpWidthPercent,
                sleepTimeoutMode = sleepTimeoutMode,
                sleepTimeoutSeconds = sleepTimeoutSeconds,
                showLaunchToast = showLaunchToast,
                enableImmersiveMode = enableImmersiveMode,
                showFirstLastReorderButtons = showFirstLastReorderButtons,
                showRomDetailsButton = showRomDetailsButton,
                showRomDetailsInGridStyle = showRomDetailsInGridStyle,
                showRomFavoriteButton = showRomFavoriteButton,
                showRomFavoriteInGridStyle = showRomFavoriteInGridStyle,
                showRomCompleteButton = showRomCompleteButton,
                showRomCompleteInGridStyle = showRomCompleteInGridStyle,
                showTopBar = showTopBar,
                showTopBarSettingsIcon = showTopBarSettingsIcon,
                showTopBarTitleIcon = showTopBarTitleIcon,
                topBarTitleAlignment = topBarTitleAlignment,
                enableSwipeSystemNavigation = enableSwipeSystemNavigation,
                ignoreSystemAnimationScale = ignoreSystemAnimationScale,
                centeredLastGridItem = centeredLastGridItem
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DisplaySettings()
        }
    }

    // --- ROM LIST SETTINGS XML ---

    fun saveRomListSettings(settings: RomListSettings): Boolean {
        return try {
            val file = File(baseDir, "rom_list_settings.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "RomListSettings")

            serializer.startTag("", "listStyle").text(settings.listStyle.name).endTag("", "listStyle")
            serializer.startTag("", "textSizeSp").text(settings.textSizeSp.toString()).endTag("", "textSizeSp")
            serializer.startTag("", "marginDp").text(settings.marginDp.toString()).endTag("", "marginDp")
            serializer.startTag("", "textAlignment").text(settings.textAlignment.name).endTag("", "textAlignment")
            serializer.startTag("", "showArtworkInTextOnly").text(settings.showArtworkInTextOnly.toString()).endTag("", "showArtworkInTextOnly")
            serializer.startTag("", "gridScalePercent").text(settings.gridScalePercent.toString()).endTag("", "gridScalePercent")
            serializer.startTag("", "showDetails").text(settings.showDetails.toString()).endTag("", "showDetails")

            serializer.endTag("", "RomListSettings")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadRomListSettings(): RomListSettings {
        val file = File(baseDir, "rom_list_settings.xml")
        if (!file.exists()) return RomListSettings()

        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var listStyle = RomListStyle.GRID
            var textSizeSp = 16
            var marginDp = 8
            var textAlignment = TextAlignmentOption.START
            var showArtworkInTextOnly = true
            var gridScalePercent = 100
            var showDetails = true

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "listStyle" -> listStyle = runCatching { RomListStyle.valueOf(text) }.getOrDefault(RomListStyle.GRID)
                                "textSizeSp" -> textSizeSp = text.toIntOrNull() ?: 16
                                "marginDp" -> marginDp = text.toIntOrNull() ?: 8
                                "textAlignment" -> textAlignment = runCatching { TextAlignmentOption.valueOf(text) }.getOrDefault(TextAlignmentOption.START)
                                "showArtworkInTextOnly" -> showArtworkInTextOnly = text.toBooleanStrictOrNull() ?: true
                                "gridScalePercent" -> gridScalePercent = text.toIntOrNull() ?: 100
                                "showDetails" -> showDetails = text.toBooleanStrictOrNull() ?: true
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            RomListSettings(
                listStyle = listStyle,
                textSizeSp = textSizeSp,
                marginDp = marginDp,
                textAlignment = textAlignment,
                showArtworkInTextOnly = showArtworkInTextOnly,
                gridScalePercent = gridScalePercent,
                showDetails = showDetails
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RomListSettings()
        }
    }

    // --- PER-SYSTEM ROM LIST SETTINGS XML ---

    fun saveSystemRomListSettingsMap(map: Map<String, RomListSettings>): Boolean {
        return try {
            val file = File(baseDir, "system_rom_list_settings.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "SystemRomListSettingsMap")

            for ((systemId, settings) in map) {
                serializer.startTag("", "SystemEntry")
                serializer.attribute("", "id", systemId)
                serializer.startTag("", "listStyle").text(settings.listStyle.name).endTag("", "listStyle")
                serializer.startTag("", "textSizeSp").text(settings.textSizeSp.toString()).endTag("", "textSizeSp")
                serializer.startTag("", "marginDp").text(settings.marginDp.toString()).endTag("", "marginDp")
                serializer.startTag("", "textAlignment").text(settings.textAlignment.name).endTag("", "textAlignment")
                serializer.startTag("", "showArtworkInTextOnly").text(settings.showArtworkInTextOnly.toString()).endTag("", "showArtworkInTextOnly")
                serializer.startTag("", "gridScalePercent").text(settings.gridScalePercent.toString()).endTag("", "gridScalePercent")
                serializer.startTag("", "showDetails").text(settings.showDetails.toString()).endTag("", "showDetails")
                serializer.endTag("", "SystemEntry")
            }

            serializer.endTag("", "SystemRomListSettingsMap")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadSystemRomListSettingsMap(): Map<String, RomListSettings> {
        val file = File(baseDir, "system_rom_list_settings.xml")
        if (!file.exists()) return emptyMap()

        val resultMap = mutableMapOf<String, RomListSettings>()
        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var currentSystemId = ""
            var listStyle = RomListStyle.GRID
            var textSizeSp = 16
            var marginDp = 8
            var textAlignment = TextAlignmentOption.START
            var showArtworkInTextOnly = true
            var gridScalePercent = 100
            var showDetails = true

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "SystemEntry") {
                            currentSystemId = parser.getAttributeValue("", "id") ?: ""
                            listStyle = RomListStyle.GRID
                            textSizeSp = 16
                            marginDp = 8
                            textAlignment = TextAlignmentOption.START
                            showArtworkInTextOnly = true
                            gridScalePercent = 100
                            showDetails = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && currentSystemId.isNotEmpty()) {
                            when (currentTag) {
                                "listStyle" -> listStyle = runCatching { RomListStyle.valueOf(text) }.getOrDefault(RomListStyle.GRID)
                                "textSizeSp" -> textSizeSp = text.toIntOrNull() ?: 16
                                "marginDp" -> marginDp = text.toIntOrNull() ?: 8
                                "textAlignment" -> textAlignment = runCatching { TextAlignmentOption.valueOf(text) }.getOrDefault(TextAlignmentOption.START)
                                "showArtworkInTextOnly" -> showArtworkInTextOnly = text.toBooleanStrictOrNull() ?: true
                                "gridScalePercent" -> gridScalePercent = text.toIntOrNull() ?: 100
                                "showDetails" -> showDetails = text.toBooleanStrictOrNull() ?: true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "SystemEntry" && currentSystemId.isNotEmpty()) {
                            resultMap[currentSystemId] = RomListSettings(
                                listStyle = listStyle,
                                textSizeSp = textSizeSp,
                                marginDp = marginDp,
                                textAlignment = textAlignment,
                                showArtworkInTextOnly = showArtworkInTextOnly,
                                gridScalePercent = gridScalePercent,
                                showDetails = showDetails
                            )
                            currentSystemId = ""
                        }
                    }
                }
                eventType = parser.next()
            }
            resultMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    // --- GAMEPAD SETTINGS XML ---

    fun saveGamepadSettings(settings: GamepadSettings): Boolean {
        return try {
            val file = File(baseDir, "gamepad_settings.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "GamepadSettings")

            serializer.startTag("", "keyPageUp").text(settings.keyPageUp.toString()).endTag("", "keyPageUp")
            serializer.startTag("", "keyPageDown").text(settings.keyPageDown.toString()).endTag("", "keyPageDown")
            serializer.startTag("", "keyGoToTop").text(settings.keyGoToTop.toString()).endTag("", "keyGoToTop")
            serializer.startTag("", "keyGoToBottom").text(settings.keyGoToBottom.toString()).endTag("", "keyGoToBottom")
            serializer.startTag("", "keySystemSettings").text(settings.keySystemSettings.toString()).endTag("", "keySystemSettings")
            serializer.startTag("", "keyRomListSettings").text(settings.keyRomListSettings.toString()).endTag("", "keyRomListSettings")
            serializer.startTag("", "keySelectAction").text(settings.keySelectAction.toString()).endTag("", "keySelectAction")
            serializer.startTag("", "keyBackAction").text(settings.keyBackAction.toString()).endTag("", "keyBackAction")
            serializer.startTag("", "keyFavoriteAction").text(settings.keyFavoriteAction.toString()).endTag("", "keyFavoriteAction")
            serializer.startTag("", "keyInfoAction").text(settings.keyInfoAction.toString()).endTag("", "keyInfoAction")
            serializer.startTag("", "keyOpenSearch").text(settings.keyOpenSearch.toString()).endTag("", "keyOpenSearch")
            serializer.startTag("", "keySystemManagerAction").text(settings.keySystemManagerAction.toString()).endTag("", "keySystemManagerAction")
            serializer.startTag("", "keyToggleTopBarKey1").text(settings.keyToggleTopBarKey1.toString()).endTag("", "keyToggleTopBarKey1")
            serializer.startTag("", "keyToggleTopBarKey2").text(settings.keyToggleTopBarKey2.toString()).endTag("", "keyToggleTopBarKey2")

            serializer.endTag("", "GamepadSettings")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadGamepadSettings(): GamepadSettings {
        val file = File(baseDir, "gamepad_settings.xml")
        if (!file.exists()) return GamepadSettings()

        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var defaults = GamepadSettings()
            var keyPageUp = defaults.keyPageUp
            var keyPageDown = defaults.keyPageDown
            var keyGoToTop = defaults.keyGoToTop
            var keyGoToBottom = defaults.keyGoToBottom
            var keySystemSettings = defaults.keySystemSettings
            var keyRomListSettings = defaults.keyRomListSettings
            var keySelectAction = defaults.keySelectAction
            var keyBackAction = defaults.keyBackAction
            var keyFavoriteAction = defaults.keyFavoriteAction
            var keyInfoAction = defaults.keyInfoAction
            var keyOpenSearch = defaults.keyOpenSearch
            var keySystemManagerAction = defaults.keySystemManagerAction
            var keyToggleTopBarKey1 = defaults.keyToggleTopBarKey1
            var keyToggleTopBarKey2 = defaults.keyToggleTopBarKey2

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "keyPageUp" -> keyPageUp = text.toIntOrNull() ?: defaults.keyPageUp
                                "keyPageDown" -> keyPageDown = text.toIntOrNull() ?: defaults.keyPageDown
                                "keyGoToTop" -> keyGoToTop = text.toIntOrNull() ?: defaults.keyGoToTop
                                "keyGoToBottom" -> keyGoToBottom = text.toIntOrNull() ?: defaults.keyGoToBottom
                                "keySystemSettings" -> keySystemSettings = text.toIntOrNull() ?: defaults.keySystemSettings
                                "keyRomListSettings" -> keyRomListSettings = text.toIntOrNull() ?: defaults.keyRomListSettings
                                "keySelectAction" -> keySelectAction = text.toIntOrNull() ?: defaults.keySelectAction
                                "keyBackAction" -> keyBackAction = text.toIntOrNull() ?: defaults.keyBackAction
                                "keyFavoriteAction" -> keyFavoriteAction = text.toIntOrNull() ?: defaults.keyFavoriteAction
                                "keyInfoAction" -> keyInfoAction = text.toIntOrNull() ?: defaults.keyInfoAction
                                "keyOpenSearch" -> keyOpenSearch = text.toIntOrNull() ?: defaults.keyOpenSearch
                                "keySystemManagerAction" -> keySystemManagerAction = text.toIntOrNull() ?: defaults.keySystemManagerAction
                                "keyToggleTopBarKey1" -> keyToggleTopBarKey1 = text.toIntOrNull() ?: defaults.keyToggleTopBarKey1
                                "keyToggleTopBarKey2" -> keyToggleTopBarKey2 = text.toIntOrNull() ?: defaults.keyToggleTopBarKey2
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            GamepadSettings(
                keyPageUp = keyPageUp,
                keyPageDown = keyPageDown,
                keyGoToTop = keyGoToTop,
                keyGoToBottom = keyGoToBottom,
                keySystemSettings = keySystemSettings,
                keyRomListSettings = keyRomListSettings,
                keySelectAction = keySelectAction,
                keyBackAction = keyBackAction,
                keyFavoriteAction = keyFavoriteAction,
                keyInfoAction = keyInfoAction,
                keyOpenSearch = keyOpenSearch,
                keySystemManagerAction = keySystemManagerAction,
                keyToggleTopBarKey1 = keyToggleTopBarKey1,
                keyToggleTopBarKey2 = keyToggleTopBarKey2
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GamepadSettings()
        }
    }

    // --- VISIBLE ANDROID APPS XML PER SYSTEM ---

    fun saveVisibleAppsXml(systemId: String, visiblePackages: Set<String>): Boolean {
        return try {
            val fileName = when (systemId) {
                "android_apps" -> "android_apps.xml"
                "android_games" -> "android_games.xml"
                "android_emulators" -> "android_emulators.xml"
                else -> "${systemId}.xml"
            }
            val file = File(baseDir, fileName)
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "VisibleApps")
            serializer.attribute("", "systemId", systemId)

            for (pkg in visiblePackages) {
                serializer.startTag("", "package").text(pkg).endTag("", "package")
            }

            serializer.endTag("", "VisibleApps")
            serializer.endDocument()

            FileOutputStream(file).use { fos ->
                fos.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadVisibleAppsXml(systemId: String): Set<String>? {
        val fileName = when (systemId) {
            "android_apps" -> "android_apps.xml"
            "android_games" -> "android_games.xml"
            "android_emulators" -> "android_emulators.xml"
            else -> "${systemId}.xml"
        }
        val file = File(baseDir, fileName)
        if (!file.exists()) return null

        val visibleSet = mutableSetOf<String>()
        try {
            val parser = Xml.newPullParser()
            FileInputStream(file).use { fis ->
                parser.setInput(fis, "UTF-8")
                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "package") {
                        val pkg = parser.nextText()
                        if (pkg.isNotEmpty()) {
                            visibleSet.add(pkg)
                        }
                    }
                    eventType = parser.next()
                }
            }
            return visibleSet
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // --- BOTTOM BAR SETTINGS XML ---

    fun saveBottomBarSettings(settings: BottomBarSettings): Boolean {
        return try {
            val file = File(baseDir, "bottom_bar_settings.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "BottomBarSettings")

            serializer.startTag("", "showBottomBar").text(settings.showBottomBar.toString()).endTag("", "showBottomBar")
            serializer.startTag("", "heightDp").text(settings.heightDp.toString()).endTag("", "heightDp")
            serializer.startTag("", "iconSizeDp").text(settings.iconSizeDp.toString()).endTag("", "iconSizeDp")
            serializer.startTag("", "showTime").text(settings.showTime.toString()).endTag("", "showTime")
            serializer.startTag("", "showBattery").text(settings.showBattery.toString()).endTag("", "showBattery")
            serializer.startTag("", "showWifi").text(settings.showWifi.toString()).endTag("", "showWifi")
            serializer.startTag("", "showBluetooth").text(settings.showBluetooth.toString()).endTag("", "showBluetooth")
            serializer.startTag("", "showDate").text(settings.showDate.toString()).endTag("", "showDate")
            serializer.startTag("", "showSettingsIcon").text(settings.showSettingsIcon.toString()).endTag("", "showSettingsIcon")
            serializer.startTag("", "itemsOrderAndAlign").text(settings.itemsOrderAndAlign).endTag("", "itemsOrderAndAlign")
            serializer.startTag("", "dateFormat").text(settings.dateFormat).endTag("", "dateFormat")
            serializer.startTag("", "timeFormat").text(settings.timeFormat).endTag("", "timeFormat")
            serializer.startTag("", "showClockIcon").text(settings.showClockIcon.toString()).endTag("", "showClockIcon")
            serializer.startTag("", "showDateIcon").text(settings.showDateIcon.toString()).endTag("", "showDateIcon")
            serializer.startTag("", "showBatteryIcon").text(settings.showBatteryIcon.toString()).endTag("", "showBatteryIcon")

            serializer.endTag("", "BottomBarSettings")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadBottomBarSettings(): BottomBarSettings {
        val file = File(baseDir, "bottom_bar_settings.xml")
        if (!file.exists()) return BottomBarSettings()

        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var eventType = parser.eventType
            var showBottomBar = true
            var heightDp = 36
            var iconSizeDp = 18
            var showTime = true
            var showBattery = true
            var showWifi = true
            var showBluetooth = true
            var showDate = true
            var showSettingsIcon = true
            var itemsOrderAndAlign = "bluetooth:left,wifi:left,battery:left,date:right,time:right"
            var dateFormat = "EEE, MMM d"
            var timeFormat = "HH:mm"
            var showClockIcon = true
            var showDateIcon = true
            var showBatteryIcon = true

            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "showBottomBar" -> showBottomBar = text.toBooleanStrictOrNull() ?: true
                                "heightDp" -> heightDp = text.toIntOrNull() ?: 36
                                "iconSizeDp" -> iconSizeDp = text.toIntOrNull() ?: 18
                                "showTime" -> showTime = text.toBooleanStrictOrNull() ?: true
                                "showBattery" -> showBattery = text.toBooleanStrictOrNull() ?: true
                                "showWifi" -> showWifi = text.toBooleanStrictOrNull() ?: true
                                "showBluetooth" -> showBluetooth = text.toBooleanStrictOrNull() ?: true
                                "showDate" -> showDate = text.toBooleanStrictOrNull() ?: true
                                "showSettingsIcon" -> showSettingsIcon = text.toBooleanStrictOrNull() ?: true
                                "itemsOrderAndAlign" -> itemsOrderAndAlign = text
                                "dateFormat" -> dateFormat = text
                                "timeFormat" -> timeFormat = text
                                "showClockIcon" -> showClockIcon = text.toBooleanStrictOrNull() ?: true
                                "showDateIcon" -> showDateIcon = text.toBooleanStrictOrNull() ?: true
                                "showBatteryIcon" -> showBatteryIcon = text.toBooleanStrictOrNull() ?: true
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            BottomBarSettings(
                showBottomBar = showBottomBar,
                heightDp = heightDp,
                iconSizeDp = iconSizeDp,
                showTime = showTime,
                showBattery = showBattery,
                showWifi = showWifi,
                showBluetooth = showBluetooth,
                showDate = showDate,
                showSettingsIcon = showSettingsIcon,
                itemsOrderAndAlign = itemsOrderAndAlign,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                showClockIcon = showClockIcon,
                showDateIcon = showDateIcon,
                showBatteryIcon = showBatteryIcon
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BottomBarSettings()
        }
    }

    // --- SYSTEM MAPPINGS XML ---

    fun saveSystemsXml(systems: List<SystemEntity>, overwriteIfExists: Boolean = true): Boolean {
        return try {
            val file = File(baseDir, "systems.xml")
            if (!overwriteIfExists && file.exists()) {
                return true
            }
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "systems")

            systems.forEach { sys ->
                serializer.startTag("", "system")
                serializer.attribute("", "id", sys.id)
                serializer.startTag("", "name").text(sys.name).endTag("", "name")
                serializer.startTag("", "shortName").text(sys.shortName).endTag("", "shortName")
                serializer.startTag("", "folderPath").text(sys.folderPath).endTag("", "folderPath")
                serializer.startTag("", "boxartFolderPath").text(sys.boxartFolderPath).endTag("", "boxartFolderPath")
                serializer.startTag("", "allowedExtensions").text(sys.allowedExtensions).endTag("", "allowedExtensions")
                serializer.startTag("", "defaultLaunchMode").text(sys.defaultLaunchMode).endTag("", "defaultLaunchMode")
                serializer.startTag("", "retroArchCore").text(sys.retroArchCore).endTag("", "retroArchCore")
                serializer.startTag("", "customXmlProfileId").text(sys.customXmlProfileId).endTag("", "customXmlProfileId")
                serializer.startTag("", "retroArchPackage").text(sys.retroArchPackage).endTag("", "retroArchPackage")
                serializer.startTag("", "colorHex").text(sys.colorHex).endTag("", "colorHex")
                serializer.startTag("", "iconName").text(sys.iconName).endTag("", "iconName")
                serializer.startTag("", "isArcade").text(sys.isArcade.toString()).endTag("", "isArcade")
                serializer.startTag("", "defaultRomIcon").text(sys.defaultRomIcon).endTag("", "defaultRomIcon")
                serializer.startTag("", "retroarchSaveDir").text(sys.retroarchSaveDir).endTag("", "retroarchSaveDir")
                serializer.startTag("", "saveAspectRatio").text(sys.saveAspectRatio).endTag("", "saveAspectRatio")
                serializer.startTag("", "manufacturer").text(sys.manufacturer).endTag("", "manufacturer")
                serializer.startTag("", "releaseYear").text(sys.releaseYear).endTag("", "releaseYear")
                serializer.startTag("", "displayOrder").text(sys.displayOrder.toString()).endTag("", "displayOrder")
                serializer.startTag("", "isEnabled").text(sys.isEnabled.toString()).endTag("", "isEnabled")
                serializer.endTag("", "system")
            }

            serializer.endTag("", "systems")
            serializer.endDocument()

            file.writeText(writer.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadSystemsXml(): List<SystemEntity>? {
        val file = File(baseDir, "systems.xml")
        if (!file.exists()) return null
        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(StringReader(file.readText()))
            val list = mutableListOf<SystemEntity>()
            var eventType = parser.eventType

            var id = ""
            var name = ""
            var shortName = ""
            var folderPath = ""
            var boxartFolderPath = ""
            var allowedExtensions = ""
            var defaultLaunchMode = "RETROARCH"
            var retroArchCore = ""
            var retroArchPackage = ""
            var customXmlProfileId = ""
            var colorHex = "#2196F3"
            var iconName = "gamepad"
            var isArcade = false
            var defaultRomIcon = ""
            var retroarchSaveDir = ""
            var saveAspectRatio = "Auto"
            var manufacturer = ""
            var releaseYear = ""
            var displayOrder = 0
            var isEnabled = true
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "system") {
                            id = parser.getAttributeValue("", "id") ?: ""
                            name = ""
                            shortName = ""
                            folderPath = ""
                            boxartFolderPath = ""
                            allowedExtensions = ""
                            defaultLaunchMode = "RETROARCH"
                            retroArchCore = ""
                            retroArchPackage = ""
                            customXmlProfileId = ""
                            colorHex = "#2196F3"
                            iconName = "gamepad"
                            isArcade = false
                            defaultRomIcon = ""
                            retroarchSaveDir = ""
                            saveAspectRatio = "Auto"
                            manufacturer = ""
                            releaseYear = ""
                            displayOrder = list.size
                            isEnabled = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "name" -> name = text
                                "shortName" -> shortName = text
                                "folderPath" -> folderPath = text
                                "boxartFolderPath" -> boxartFolderPath = text
                                "allowedExtensions" -> allowedExtensions = text
                                "defaultLaunchMode" -> defaultLaunchMode = text
                                "retroArchCore" -> retroArchCore = text
                                "retroArchPackage" -> retroArchPackage = text
                                "customXmlProfileId" -> customXmlProfileId = text
                                "colorHex" -> colorHex = text
                                "iconName" -> iconName = text
                                "isArcade" -> isArcade = text.toBooleanStrictOrNull() ?: false
                                "defaultRomIcon" -> defaultRomIcon = text
                                "retroarchSaveDir" -> retroarchSaveDir = text
                                "saveAspectRatio" -> saveAspectRatio = text
                                "manufacturer" -> manufacturer = text
                                "releaseYear" -> releaseYear = text
                                "displayOrder" -> displayOrder = text.toIntOrNull() ?: list.size
                                "isEnabled" -> isEnabled = text.toBooleanStrictOrNull() ?: true
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "system" && id.isNotBlank()) {
                            list.add(
                                SystemEntity(
                                    id = id,
                                    name = name.ifBlank { id },
                                    shortName = shortName,
                                    folderPath = folderPath,
                                    boxartFolderPath = boxartFolderPath,
                                    allowedExtensions = allowedExtensions,
                                    defaultLaunchMode = defaultLaunchMode,
                                    retroArchCore = retroArchCore,
                                    retroArchPackage = retroArchPackage,
                                    customXmlProfileId = customXmlProfileId,
                                    colorHex = colorHex,
                                    iconName = iconName,
                                    isArcade = isArcade,
                                    defaultRomIcon = defaultRomIcon,
                                    retroarchSaveDir = retroarchSaveDir,
                                    saveAspectRatio = saveAspectRatio,
                                    manufacturer = manufacturer,
                                    releaseYear = releaseYear,
                                    displayOrder = displayOrder,
                                    isEnabled = isEnabled
                                )
                            )
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- STANDALONE EMULATOR XML PROFILES ---

    fun generateProfileXmlString(profile: StandaloneProfileEntity): String {
        return try {
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)

            serializer.startTag("", "emulator")
            serializer.attribute("", "id", profile.id)
            serializer.attribute("", "name", profile.name)

            serializer.startTag("", "package").text(profile.packageName).endTag("", "package")
            serializer.startTag("", "activity").text(profile.activityName).endTag("", "activity")
            serializer.startTag("", "action").text(profile.intentAction).endTag("", "action")
            serializer.startTag("", "romPathExtraKey").text(profile.romPathExtraKey).endTag("", "romPathExtraKey")
            serializer.startTag("", "extraArgsJson").text(profile.extraArgsJson).endTag("", "extraArgsJson")

            serializer.endTag("", "emulator")
            serializer.endDocument()

            writer.toString()
        } catch (e: Exception) {
            "<emulator id=\"${profile.id}\" name=\"${profile.name}\">\n" +
                    "  <package>${profile.packageName}</package>\n" +
                    "  <activity>${profile.activityName}</activity>\n" +
                    "  <action>${profile.intentAction}</action>\n" +
                    "  <romPathExtraKey>${profile.romPathExtraKey}</romPathExtraKey>\n" +
                    "  <extraArgsJson>${profile.extraArgsJson}</extraArgsJson>\n" +
                    "</emulator>"
        }
    }

    fun parseProfileXmlString(xmlContent: String): StandaloneProfileEntity? {
        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var id = ""
            var name = ""
            var packageName = ""
            var activityName = ""
            var action = "android.intent.action.MAIN"
            var romPathExtraKey = "bootPath"
            var extraArgsJson = "{}"
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "emulator") {
                            id = parser.getAttributeValue("", "id") ?: ""
                            name = parser.getAttributeValue("", "name") ?: ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "package", "packageName" -> packageName = text
                                "activity", "activityName" -> activityName = text
                                "action", "intentAction" -> action = text
                                "romPathExtraKey", "extraKey" -> romPathExtraKey = text
                                "extraArgsJson" -> extraArgsJson = text
                                "id" -> if (id.isEmpty()) id = text
                                "name" -> if (name.isEmpty()) name = text
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (id.isEmpty()) id = packageName.ifEmpty { "custom_${System.currentTimeMillis()}" }
            if (name.isEmpty()) name = id

            StandaloneProfileEntity(
                id = id,
                name = name,
                packageName = packageName,
                activityName = activityName,
                intentAction = action,
                romPathExtraKey = romPathExtraKey,
                extraArgsJson = extraArgsJson,
                isCustomXml = true,
                rawXmlContent = xmlContent
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveProfileToFolder(profile: StandaloneProfileEntity, overwriteIfExists: Boolean = true): Boolean {
        return try {
            val file = File(customEmulatorsDir, "${profile.id}.xml")
            if (!overwriteIfExists && file.exists()) {
                return true
            }
            val xmlStr = if (profile.rawXmlContent.isNotBlank()) profile.rawXmlContent else generateProfileXmlString(profile)
            file.writeText(xmlStr)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadAllProfilesFromFolder(): List<StandaloneProfileEntity> {
        val list = mutableListOf<StandaloneProfileEntity>()
        val files = customEmulatorsDir.listFiles { _, name -> name.endsWith(".xml") } ?: return list
        for (f in files) {
            try {
                val parsed = parseProfileXmlString(f.readText())
                if (parsed != null) {
                    list.add(parsed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun getAllFilesList(): List<File> {
        val result = mutableListOf<File>()
        baseDir.listFiles()?.let { result.addAll(it) }
        return result
    }

    // --- FAVORITES & RECENTLY PLAYED PERSISTENCE ---

    data class RomUserData(
        val filePath: String,
        val isFavorite: Boolean,
        val isCompleted: Boolean = false,
        val lastPlayedTimestamp: Long,
        val playCount: Int
    )

    fun saveFavoritesAndRecents(userDataList: List<RomUserData>): Boolean {
        val favSuccess = saveFavorites(userDataList)
        val recSuccess = saveRecents(userDataList)
        return favSuccess && recSuccess
    }

    fun saveFavorites(userDataList: List<RomUserData>): Boolean {
        return try {
            val file = File(baseDir, "favorites.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "FavoritesList")

            for (data in userDataList) {
                if (data.isFavorite || data.isCompleted) {
                    serializer.startTag("", "RomUserData")
                    serializer.startTag("", "filePath").text(data.filePath).endTag("", "filePath")
                    serializer.startTag("", "isFavorite").text(data.isFavorite.toString()).endTag("", "isFavorite")
                    serializer.startTag("", "isCompleted").text(data.isCompleted.toString()).endTag("", "isCompleted")
                    serializer.startTag("", "lastPlayedTimestamp").text(data.lastPlayedTimestamp.toString()).endTag("", "lastPlayedTimestamp")
                    serializer.startTag("", "playCount").text(data.playCount.toString()).endTag("", "playCount")
                    serializer.endTag("", "RomUserData")
                }
            }

            serializer.endTag("", "FavoritesList")
            serializer.endDocument()

            FileOutputStream(file).use { fos ->
                fos.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveRecents(userDataList: List<RomUserData>): Boolean {
        return try {
            val file = File(baseDir, "recents.xml")
            val writer = StringWriter()
            val serializer = createSerializer(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "RecentsList")

            for (data in userDataList) {
                if (data.lastPlayedTimestamp > 0 || data.playCount > 0) {
                    serializer.startTag("", "RomUserData")
                    serializer.startTag("", "filePath").text(data.filePath).endTag("", "filePath")
                    serializer.startTag("", "isFavorite").text(data.isFavorite.toString()).endTag("", "isFavorite")
                    serializer.startTag("", "isCompleted").text(data.isCompleted.toString()).endTag("", "isCompleted")
                    serializer.startTag("", "lastPlayedTimestamp").text(data.lastPlayedTimestamp.toString()).endTag("", "lastPlayedTimestamp")
                    serializer.startTag("", "playCount").text(data.playCount.toString()).endTag("", "playCount")
                    serializer.endTag("", "RomUserData")
                }
            }

            serializer.endTag("", "RecentsList")
            serializer.endDocument()

            FileOutputStream(file).use { fos ->
                fos.write(writer.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadFavoritesAndRecents(): Map<String, RomUserData> {
        val resultMap = mutableMapOf<String, RomUserData>()
        val fileFav = File(baseDir, "favorites.xml")
        val fileRec = File(baseDir, "recents.xml")
        val fileOld = File(baseDir, "favorites_and_recents.xml")

        if (!fileFav.exists() && !fileRec.exists() && fileOld.exists()) {
            val oldData = loadFromXmlFile(fileOld, "UserRomDataList")
            saveFavorites(oldData.values.toList())
            saveRecents(oldData.values.toList())
            return oldData
        }

        val favData = if (fileFav.exists()) loadFromXmlFile(fileFav, "FavoritesList") else emptyMap()
        val recData = if (fileRec.exists()) loadFromXmlFile(fileRec, "RecentsList") else emptyMap()

        val allKeys = favData.keys + recData.keys
        for (key in allKeys) {
            val f = favData[key]
            val r = recData[key]
            resultMap[key] = RomUserData(
                filePath = key,
                isFavorite = f?.isFavorite ?: r?.isFavorite ?: false,
                isCompleted = f?.isCompleted ?: r?.isCompleted ?: false,
                lastPlayedTimestamp = maxOf(f?.lastPlayedTimestamp ?: 0L, r?.lastPlayedTimestamp ?: 0L),
                playCount = maxOf(f?.playCount ?: 0, r?.playCount ?: 0)
            )
        }
        return resultMap
    }

    private fun loadFromXmlFile(file: File, rootTag: String): Map<String, RomUserData> {
        val resultMap = mutableMapOf<String, RomUserData>()
        try {
            val parser = Xml.newPullParser()
            FileInputStream(file).use { fis ->
                parser.setInput(fis, "UTF-8")
                var eventType = parser.eventType

                var currentFilePath = ""
                var currentIsFav = false
                var currentIsCompleted = false
                var currentTimestamp = 0L
                var currentPlayCount = 0

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = parser.name
                        when (tagName) {
                            "RomUserData" -> {
                                currentFilePath = ""
                                currentIsFav = false
                                currentIsCompleted = false
                                currentTimestamp = 0L
                                currentPlayCount = 0
                            }
                            "filePath" -> currentFilePath = parser.nextText()
                            "isFavorite" -> currentIsFav = parser.nextText().toBoolean()
                            "isCompleted" -> currentIsCompleted = parser.nextText().toBoolean()
                            "lastPlayedTimestamp" -> currentTimestamp = parser.nextText().toLongOrNull() ?: 0L
                            "playCount" -> currentPlayCount = parser.nextText().toIntOrNull() ?: 0
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.name == "RomUserData" && currentFilePath.isNotEmpty()) {
                            resultMap[currentFilePath] = RomUserData(
                                filePath = currentFilePath,
                                isFavorite = currentIsFav,
                                isCompleted = currentIsCompleted,
                                lastPlayedTimestamp = currentTimestamp,
                                playCount = currentPlayCount
                            )
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultMap
    }
}
