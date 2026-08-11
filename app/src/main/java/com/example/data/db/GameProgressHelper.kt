package com.example.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.data.config.ConfigStorageManager
import java.io.File

class GameProgressHelper(private val context: Context) {

    private val configStorageManager = ConfigStorageManager(context)

    fun getDatabaseFile(): File {
        val baseDir = configStorageManager.resolveBaseDir()
        if (!baseDir.exists()) {
            try {
                baseDir.mkdirs()
            } catch (_: Exception) {}
        }
        return File(baseDir, "game_progress.db")
    }

    fun getWritableDatabase(): SQLiteDatabase? {
        val dbFile = getDatabaseFile()
        return try {
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS game_progress (
                    system_id TEXT NOT NULL,
                    game_file_name TEXT NOT NULL,
                    launch_count INTEGER DEFAULT 0,
                    last_played INTEGER DEFAULT 0,
                    PRIMARY KEY (system_id, game_file_name)
                )
                """.trimIndent()
            )
            db
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun incrementLaunchCount(systemId: String, gameFileName: String, timestamp: Long) {
        val db = getWritableDatabase() ?: return
        try {
            db.execSQL(
                """
                INSERT INTO game_progress (system_id, game_file_name, launch_count, last_played)
                VALUES (?, ?, 1, ?)
                ON CONFLICT(system_id, game_file_name) DO UPDATE SET
                    launch_count = launch_count + 1,
                    last_played = ?
                """.trimIndent(),
                arrayOf(systemId, gameFileName, timestamp, timestamp)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                db.close()
            } catch (_: Exception) {}
        }
    }

    fun getProgress(systemId: String, gameFileName: String): ProgressData? {
        val db = getWritableDatabase() ?: return null
        var data: ProgressData? = null
        try {
            val cursor = db.rawQuery(
                "SELECT launch_count, last_played FROM game_progress WHERE system_id = ? AND game_file_name = ?",
                arrayOf(systemId, gameFileName)
            )
            if (cursor.moveToFirst()) {
                val launchCount = cursor.getInt(0)
                val lastPlayed = cursor.getLong(1)
                data = ProgressData(launchCount, lastPlayed)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                db.close()
            } catch (_: Exception) {}
        }
        return data
    }

    data class ProgressData(val launchCount: Int, val lastPlayed: Long)
}
