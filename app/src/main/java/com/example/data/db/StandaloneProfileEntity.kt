package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "standalone_profiles")
data class StandaloneProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val activityName: String,
    val intentAction: String = "android.intent.action.MAIN",
    val romPathExtraKey: String = "bootPath",
    val extraArgsJson: String = "{}",
    val isCustomXml: Boolean = true,
    val rawXmlContent: String = ""
)
