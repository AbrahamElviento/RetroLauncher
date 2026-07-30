package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StandaloneProfileDao {
    @Query("SELECT * FROM standalone_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<StandaloneProfileEntity>>

    @Query("SELECT * FROM standalone_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): StandaloneProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StandaloneProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<StandaloneProfileEntity>)

    @Delete
    suspend fun deleteProfile(profile: StandaloneProfileEntity)
}
