package com.example.data.model

enum class TripStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TripState(
    val status: TripStatus = TripStatus.IDLE,
    val startTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val waitingSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val currentFare: Double = 0.0,
    val speedKmH: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val baseFare: Double = 80.0,
    val farePerKm: Double = 28.0,
    val waitFarePerMin: Double = 2.0,
    val currency: String = "₹",
    val speedThreshold: Double = 5.0, // km/h below which waits accumulate
    val isMoving: Boolean = false,
    val isWaitingPaused: Boolean = false,
    val autoStartEnabled: Boolean = false,
    val isSimulationEnabled: Boolean = false,
    val isOutOfCity: Boolean = false,
    val outOfCitySurchargePercent: Double = 25.0,
    val outOfCitySurchargeAmount: Double = 0.0,
    val pickupAddress: String = "",
    val dropAddress: String = "",
    val routePoints: List<Pair<Double, Double>> = emptyList()
)
