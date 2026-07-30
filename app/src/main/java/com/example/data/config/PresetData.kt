package com.example.data.config

import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity

object PresetData {

    val PRESET_STANDALONE_PROFILES = listOf(
        StandaloneProfileEntity(
            id = "aethersx2",
            name = "AetherSX2 / NetherSX2 (PS2)",
            packageName = "xyz.aethersx2.android",
            activityName = "xyz.aethersx2.android.EmulationActivity",
            intentAction = "android.intent.action.MAIN",
            romPathExtraKey = "bootPath",
            extraArgsJson = "{\"IME\":\"true\"}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="aethersx2" name="AetherSX2 / NetherSX2 (PS2)">
                  <package>xyz.aethersx2.android</package>
                  <activity>xyz.aethersx2.android.EmulationActivity</activity>
                  <action>android.intent.action.MAIN</action>
                  <romPathExtraKey>bootPath</romPathExtraKey>
                  <extraArgsJson>{"IME":"true"}</extraArgsJson>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "ppsspp",
            name = "PPSSPP (PSP)",
            packageName = "org.ppsspp.ppsspp",
            activityName = "org.ppsspp.ppsspp.PpssppActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "PATH",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="ppsspp" name="PPSSPP (PSP)">
                  <package>org.ppsspp.ppsspp</package>
                  <activity>org.ppsspp.ppsspp.PpssppActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>PATH</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "ppsspp_gold",
            name = "PPSSPP Gold (PSP)",
            packageName = "org.ppsspp.ppssppgold",
            activityName = "org.ppsspp.ppssppgold.PpssppActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "PATH",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="ppsspp_gold" name="PPSSPP Gold (PSP)">
                  <package>org.ppsspp.ppssppgold</package>
                  <activity>org.ppsspp.ppssppgold.PpssppActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>PATH</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "drastic",
            name = "DraStic (NDS)",
            packageName = "com.dsemu.drastic",
            activityName = "com.dsemu.drastic.DraSticActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "PATH",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="drastic" name="DraStic (NDS)">
                  <package>com.dsemu.drastic</package>
                  <activity>com.dsemu.drastic.DraSticActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>PATH</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "dolphin",
            name = "Dolphin (Wii / GameCube)",
            packageName = "org.dolphinemu.dolphinemu",
            activityName = "org.dolphinemu.dolphinemu.ui.main.MainActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "AutoStartFile",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="dolphin" name="Dolphin (Wii / GameCube)">
                  <package>org.dolphinemu.dolphinemu</package>
                  <activity>org.dolphinemu.dolphinemu.ui.main.MainActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>AutoStartFile</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "citra_azahar",
            name = "Azahar++ / Citra (3DS)",
            packageName = "io.github.azahar.emulator",
            activityName = "org.citra.citra_emu.ui.main.MainActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "bootPath",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="citra_azahar" name="Azahar++ / Citra (3DS)">
                  <package>io.github.azahar.emulator</package>
                  <activity>org.citra.citra_emu.ui.main.MainActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>bootPath</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "duckstation",
            name = "DuckStation (PS1)",
            packageName = "com.github.stenzek.duckstation",
            activityName = "com.github.stenzek.duckstation.MainActivity",
            intentAction = "android.intent.action.MAIN",
            romPathExtraKey = "bootPath",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="duckstation" name="DuckStation (PS1)">
                  <package>com.github.stenzek.duckstation</package>
                  <activity>com.github.stenzek.duckstation.MainActivity</activity>
                  <action>android.intent.action.MAIN</action>
                  <romPathExtraKey>bootPath</romPathExtraKey>
                </emulator>
            """.trimIndent()
        ),
        StandaloneProfileEntity(
            id = "mupen64plus",
            name = "Mupen64Plus FZ (N64)",
            packageName = "paulscode.android.mupen64plusae",
            activityName = "paulscode.android.mupen64plusae.MainActivity",
            intentAction = "android.intent.action.VIEW",
            romPathExtraKey = "ROM",
            extraArgsJson = "{}",
            isCustomXml = false,
            rawXmlContent = """
                <emulator id="mupen64plus" name="Mupen64Plus FZ (N64)">
                  <package>paulscode.android.mupen64plusae</package>
                  <activity>paulscode.android.mupen64plusae.MainActivity</activity>
                  <action>android.intent.action.VIEW</action>
                  <romPathExtraKey>ROM</romPathExtraKey>
                </emulator>
            """.trimIndent()
        )
    )

    fun getDefaultSystems(baseFolder: String): List<SystemEntity> {
        return listOf(
            SystemEntity(
                id = "favorites",
                name = "Favorites",
                shortName = "FAV",
                folderPath = "Collection/Favorites",
                allowedExtensions = "",
                defaultLaunchMode = "VIRTUAL",
                retroArchCore = "",
                customXmlProfileId = "",
                iconName = "star",
                colorHex = "#FFD700",
                manufacturer = "Collection",
                releaseYear = "",
                displayOrder = -2,
                isEnabled = true
            ),
            SystemEntity(
                id = "recently_played",
                name = "Recently Played",
                shortName = "RECENT",
                folderPath = "Collection/Recent",
                allowedExtensions = "",
                defaultLaunchMode = "VIRTUAL",
                retroArchCore = "",
                customXmlProfileId = "",
                iconName = "history",
                colorHex = "#FF9800",
                manufacturer = "Collection",
                releaseYear = "",
                displayOrder = -1,
                isEnabled = true
            ),
            SystemEntity(
                id = "android_apps",
                name = "Android Apps",
                shortName = "APPS",
                folderPath = "Internal/Apps",
                allowedExtensions = "apk",
                defaultLaunchMode = "ANDROID_APP",
                retroArchCore = "",
                customXmlProfileId = "",
                iconName = "smartphone",
                colorHex = "#4CAF50",
                manufacturer = "Google",
                releaseYear = "2008",
                displayOrder = 0,
                isEnabled = true
            ),
            SystemEntity(
                id = "android_games",
                name = "Android Games",
                shortName = "GAMES",
                folderPath = "Internal/Games",
                allowedExtensions = "apk",
                defaultLaunchMode = "ANDROID_APP",
                retroArchCore = "",
                customXmlProfileId = "",
                iconName = "gamepad",
                colorHex = "#2196F3",
                manufacturer = "Android",
                releaseYear = "2008",
                displayOrder = 0,
                isEnabled = true
            ),
            SystemEntity(
                id = "android_emulators",
                name = "Android Emulators",
                shortName = "EMUS",
                folderPath = "Internal/Emulators",
                allowedExtensions = "apk",
                defaultLaunchMode = "ANDROID_APP",
                retroArchCore = "",
                customXmlProfileId = "",
                iconName = "sports_esports",
                colorHex = "#9C27B0",
                manufacturer = "Android",
                releaseYear = "2008",
                displayOrder = 0,
                isEnabled = true
            ),
            SystemEntity(
                id = "snes",
                name = "Super Nintendo",
                shortName = "SNES",
                folderPath = "$baseFolder/snes",
                allowedExtensions = ".sfc,.smc,.zip",
                defaultLaunchMode = "RETROARCH",
                retroArchCore = "snes9x_libretro_android.so",
                customXmlProfileId = "",
                iconName = "snes",
                colorHex = "#D0BCFF",
                manufacturer = "Nintendo",
                releaseYear = "1990",
                displayOrder = 1,
                isEnabled = true
            ),
            SystemEntity(
                id = "ps2",
                name = "PlayStation 2",
                shortName = "PS2",
                folderPath = "$baseFolder/ps2",
                allowedExtensions = ".iso,.chd,.bin",
                defaultLaunchMode = "STANDALONE_XML",
                retroArchCore = "",
                customXmlProfileId = "aethersx2",
                iconName = "playstation",
                colorHex = "#90CAF9",
                manufacturer = "Sony",
                releaseYear = "2000",
                displayOrder = 2,
                isEnabled = true
            ),
            SystemEntity(
                id = "3ds",
                name = "Nintendo 3DS",
                shortName = "3DS",
                folderPath = "$baseFolder/3ds",
                allowedExtensions = ".3ds,.cia,.app,.zip",
                defaultLaunchMode = "STANDALONE_XML",
                retroArchCore = "",
                customXmlProfileId = "citra_azahar",
                iconName = "3ds",
                colorHex = "#F48FB1",
                manufacturer = "Nintendo",
                releaseYear = "2011",
                displayOrder = 3,
                isEnabled = true
            ),
            SystemEntity(
                id = "psp",
                name = "PlayStation Portable",
                shortName = "PSP",
                folderPath = "$baseFolder/psp",
                allowedExtensions = ".iso,.cso,.pbp",
                defaultLaunchMode = "STANDALONE_XML",
                retroArchCore = "ppsspp_libretro_android.so",
                customXmlProfileId = "ppsspp",
                iconName = "psp",
                colorHex = "#CE93D8",
                manufacturer = "Sony",
                releaseYear = "2004",
                displayOrder = 3,
                isEnabled = true
            ),
            SystemEntity(
                id = "n64",
                name = "Nintendo 64",
                shortName = "N64",
                folderPath = "$baseFolder/n64",
                allowedExtensions = ".n64,.z64,.v64,.zip",
                defaultLaunchMode = "RETROARCH",
                retroArchCore = "mupen64plus_next_libretro_android.so",
                customXmlProfileId = "mupen64plus",
                iconName = "n64",
                colorHex = "#A5D6A7",
                manufacturer = "Nintendo",
                releaseYear = "1996",
                displayOrder = 4,
                isEnabled = true
            ),
            SystemEntity(
                id = "gba",
                name = "Game Boy Advance",
                shortName = "GBA",
                folderPath = "$baseFolder/gba",
                allowedExtensions = ".gba,.zip",
                defaultLaunchMode = "RETROARCH",
                retroArchCore = "mgba_libretro_android.so",
                customXmlProfileId = "",
                iconName = "gba",
                colorHex = "#FFCC80",
                manufacturer = "Nintendo",
                releaseYear = "2001",
                displayOrder = 5,
                isEnabled = true
            ),
            SystemEntity(
                id = "ps1",
                name = "PlayStation 1",
                shortName = "PS1",
                folderPath = "$baseFolder/ps1",
                allowedExtensions = ".chd,.cue,.bin,.pbp",
                defaultLaunchMode = "RETROARCH",
                retroArchCore = "pcsx_rearmed_libretro_android.so",
                customXmlProfileId = "duckstation",
                iconName = "ps1",
                colorHex = "#80DEEA",
                manufacturer = "Sony",
                releaseYear = "1994",
                displayOrder = 6,
                isEnabled = true
            ),
            SystemEntity(
                id = "gc",
                name = "GameCube & Wii",
                shortName = "GC / Wii",
                folderPath = "$baseFolder/gc_wii",
                allowedExtensions = ".iso,.rvz,.gcm",
                defaultLaunchMode = "STANDALONE_XML",
                retroArchCore = "",
                customXmlProfileId = "dolphin",
                iconName = "gc",
                colorHex = "#9FA8DA",
                manufacturer = "Nintendo",
                releaseYear = "2001",
                displayOrder = 7,
                isEnabled = true
            ),
            SystemEntity(
                id = "arcade",
                name = "Arcade",
                shortName = "ARCADE",
                folderPath = "$baseFolder/arcade",
                allowedExtensions = ".zip,.7z",
                defaultLaunchMode = "RETROARCH",
                retroArchCore = "fbneo_libretro_android.so",
                customXmlProfileId = "",
                iconName = "casino",
                colorHex = "#FF5722",
                manufacturer = "Arcade",
                releaseYear = "1980",
                displayOrder = 8,
                isEnabled = true,
                isArcade = true
            )
        )
    }
}
