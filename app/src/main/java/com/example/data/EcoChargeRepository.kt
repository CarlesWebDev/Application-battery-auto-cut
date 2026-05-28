package com.example.data

import kotlinx.coroutines.flow.Flow

class EcoChargeRepository(private val database: AppDatabase) {
    private val settingDao = database.settingDao()
    private val sessionDao = database.chargingSessionDao()

    val allSessions: Flow<List<ChargingSession>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: ChargingSession): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun getLatestSession(): ChargingSession? {
        return sessionDao.getLatestSession()
    }

    suspend fun clearSessions() {
        sessionDao.clearAllSessions()
    }

    // Typed configuration helpers using the AppSetting table
    suspend fun getStringSetting(key: String, defaultValue: String): String {
        return settingDao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun setStringSetting(key: String, value: String) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getIntSetting(key: String, defaultValue: Int): Int {
        val str = settingDao.getSetting(key)?.value
        return str?.toIntOrNull() ?: defaultValue
    }

    suspend fun setIntSetting(key: String, value: Int) {
        settingDao.insertSetting(AppSetting(key, value.toString()))
    }

    suspend fun getBooleanSetting(key: String, defaultValue: Boolean): Boolean {
        val str = settingDao.getSetting(key)?.value
        return str?.toBooleanStrictOrNull() ?: defaultValue
    }

    suspend fun setBooleanSetting(key: String, value: Boolean) {
        settingDao.insertSetting(AppSetting(key, value.toString()))
    }
}
