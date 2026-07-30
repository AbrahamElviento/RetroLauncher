package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {
    @Query("SELECT * FROM emulator_systems ORDER BY displayOrder ASC")
    fun getAllSystems(): Flow<List<SystemEntity>>

    @Query("SELECT * FROM emulator_systems WHERE id = :id")
    suspend fun getSystemById(id: String): SystemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystem(system: SystemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystems(systems: List<SystemEntity>)

    @Update
    suspend fun updateSystem(system: SystemEntity)

    @Delete
    suspend fun deleteSystem(system: SystemEntity)

    @Query("DELETE FROM emulator_systems WHERE id = :id")
    suspend fun deleteSystemById(id: String)
}
