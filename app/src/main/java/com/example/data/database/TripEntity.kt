package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val durationSeconds: Long,
    val waitingSeconds: Long,
    val totalFare: Double,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val passengerNotes: String = ""
)

@Entity(tableName = "active_trip")
data class ActiveTripRecord(
    @PrimaryKey val id: Int = 1, // Only ever 1 active trip
    val startTime: Long,
    val isPaused: Boolean,
    val accumulatedDistanceKm: Double,
    val accumulatedWaitingSeconds: Long,
    val elapsedSeconds: Long,
    val lastLatitude: Double,
    val lastLongitude: Double,
    val lastUpdateTime: Long
)
