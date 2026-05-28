package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSession): Long

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): ChargingSession?

    @Query("DELETE FROM charging_sessions")
    suspend fun clearAllSessions()
}
