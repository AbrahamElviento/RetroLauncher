package com.example.data.util

import com.example.data.db.GameRomEntity
import com.example.data.db.SystemEntity
import java.io.File

object GameIconResolver {
    fun resolveRomIcon(
        game: GameRomEntity,
        system: SystemEntity?,
        customIcons: Map<String, String>,
        allSystems: List<SystemEntity>
    ): String {
        // 1. Priority 1: custom name.xml (customIcons map)
        val customIcon = customIcons[game.filePath]
        if (!customIcon.isNullOrBlank()) {
            return customIcon
        }

        // 2. Priority 2: retroarch save (.state.auto.png in retroarchSaveDir)
        val actualSystem = if (system?.id == "favorites" || system?.id == "recently_played") {
            allSystems.firstOrNull { it.id == game.systemId }
        } else {
            system
        }

        if (actualSystem != null && actualSystem.retroarchSaveDir.isNotBlank()) {
            val saveDir = File(actualSystem.retroarchSaveDir)
            if (saveDir.exists() && saveDir.isDirectory) {
                val romFile = File(game.filePath)
                val stem = romFile.nameWithoutExtension
                val stateAutoPng = File(saveDir, "$stem.state.auto.png")
                if (stateAutoPng.exists() && stateAutoPng.isFile) {
                    return stateAutoPng.absolutePath
                }
            }
        }

        // Fallback: scanned coverArtPath
        if (!game.coverArtPath.isNullOrEmpty() && File(game.coverArtPath).exists()) {
            return game.coverArtPath
        }

        // 3. Priority 3: the default icon that has been set on the SystemEditDetailDialog
        if (actualSystem != null && !actualSystem.defaultRomIcon.isNullOrBlank()) {
            return actualSystem.defaultRomIcon
        }

        // Fallback 2: the system icon name or "gamepad"
        if (actualSystem != null && !actualSystem.iconName.isNullOrBlank()) {
            return actualSystem.iconName
        }

        return "gamepad"
    }
}
