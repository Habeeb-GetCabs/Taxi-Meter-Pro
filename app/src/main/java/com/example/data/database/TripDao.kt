package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Int): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Int)

    @Query("SELECT * FROM active_trip LIMIT 1")
    suspend fun getActiveTrip(): ActiveTripRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveTrip(activeTrip: ActiveTripRecord)

    @Query("DELETE FROM active_trip")
    suspend fun clearActiveTrip()
}
