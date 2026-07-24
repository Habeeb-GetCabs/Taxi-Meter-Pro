package com.example.data.repository

import com.example.data.database.ActiveTripRecord
import com.example.data.database.TripDao
import com.example.data.database.TripEntity
import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    suspend fun getTripById(id: Int): TripEntity? {
        return tripDao.getTripById(id)
    }

    suspend fun insertTrip(trip: TripEntity): Long {
        return tripDao.insertTrip(trip)
    }

    suspend fun deleteTripById(id: Int) {
        tripDao.deleteTripById(id)
    }

    suspend fun getActiveTrip(): ActiveTripRecord? {
        return tripDao.getActiveTrip()
    }

    suspend fun saveActiveTrip(activeTrip: ActiveTripRecord) {
        tripDao.saveActiveTrip(activeTrip)
    }

    suspend fun clearActiveTrip() {
        tripDao.clearActiveTrip()
    }
}
