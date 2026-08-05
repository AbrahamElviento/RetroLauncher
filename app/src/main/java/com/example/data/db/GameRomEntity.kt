package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_roms")
data class GameRomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemId: String,
    val title: String,
    val filePath: String,
    val fileName: String,
    val extension: String,
    val coverArtPath: String? = null,
    val customEmulatorOverride: String? = null,
    val isFavorite: Boolean = false,
    val isCompleted: Boolean = false,
    val lastPlayedTimestamp: Long = 0,
    val playCount: Int = 0
)
