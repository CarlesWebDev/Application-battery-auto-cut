package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val startBatteryLevel: Int,
    val endBatteryLevel: Int,
    val peakTemperature: Double,
    val isAutocutTriggered: Boolean = false,
    val energySavedWh: Double = 0.0
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
