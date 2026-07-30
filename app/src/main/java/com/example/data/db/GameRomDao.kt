package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRomDao {
    @Query("SELECT * FROM game_roms ORDER BY title ASC")
    fun getAllRoms(): Flow<List<GameRomEntity>>

    @Query("SELECT * FROM game_roms WHERE systemId = :systemId ORDER BY title ASC")
    fun getRomsBySystem(systemId: String): Flow<List<GameRomEntity>>

    @Query("SELECT * FROM game_roms WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteRoms(): Flow<List<GameRomEntity>>

    @Query("SELECT * FROM game_roms WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun getRecentlyPlayedRoms(): Flow<List<GameRomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRom(rom: GameRomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoms(roms: List<GameRomEntity>)

    @Update
    suspend fun updateRom(rom: GameRomEntity)

    @Delete
    suspend fun deleteRom(rom: GameRomEntity)

    @Query("DELETE FROM game_roms WHERE systemId = :systemId")
    suspend fun deleteRomsBySystem(systemId: String)

    @Query("DELETE FROM game_roms")
    suspend fun deleteAllRoms()
}
