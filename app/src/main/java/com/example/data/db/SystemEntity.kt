package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emulator_systems")
data class SystemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val folderPath: String,
    val allowedExtensions: String,
    val boxartFolderPath: String = "",
    val defaultLaunchMode: String = "RETROARCH",
    val retroArchCore: String = "",
    val retroArchPackage: String = "AUTO",
    val customXmlProfileId: String = "",
    val iconName: String = "gamepad",
    val colorHex: String = "#3D5AFE",
    val manufacturer: String = "Nintendo",
    val releaseYear: String = "1990",
    val displayOrder: Int = 0,
    val isEnabled: Boolean = true,
    val isArcade: Boolean = false,
    val defaultRomIcon: String = ""
)

