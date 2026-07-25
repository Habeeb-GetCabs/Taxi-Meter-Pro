package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.ActiveTripRecord
import com.example.data.database.TripDatabase
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.data.repository.TripRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LocationTrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationManager: android.location.LocationManager? = null
    private var locationListener: android.location.LocationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private lateinit var repository: TripRepository
    
    private var lastLocation: Location? = null
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "TaxiMeterService"
        private const val NOTIFICATION_ID = 9911
        private const val CHANNEL_ID = "taxi_meter_channel"

        private val _tripState = MutableStateFlow(TripState())
        val tripState: StateFlow<TripState> = _tripState.asStateFlow()

        // Commands to manage the service easily from outside
        fun startTrip(
            context: Context, 
            baseFare: Double, 
            farePerKm: Double, 
            waitFarePerMin: Double, 
            currency: String, 
            speedThreshold: Double
        ) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "START"
                putExtra("baseFare", baseFare)
                putExtra("farePerKm", farePerKm)
                putExtra("waitFarePerMin", waitFarePerMin)
                putExtra("currency", currency)
                putExtra("speedThreshold", speedThreshold)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startMonitoring(
            context: Context,
            baseFare: Double,
            farePerKm: Double,
            waitFarePerMin: Double,
            currency: String,
            speedThreshold: Double,
            autoStartEnabled: Boolean
        ) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "MONITOR"
                putExtra("baseFare", baseFare)
                putExtra("farePerKm", farePerKm)
                putExtra("waitFarePerMin", waitFarePerMin)
                putExtra("currency", currency)
                putExtra("speedThreshold", speedThreshold)
                putExtra("autoStartEnabled", autoStartEnabled)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "startService failed in startMonitoring", e)
            }
        }

        fun pauseTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "PAUSE"
            }
            context.startService(intent)
        }

        fun resumeTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "RESUME"
            }
            context.startService(intent)
        }

        fun stopTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
        }

        fun recoveryTrip(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "RECOVER"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun toggleSimulation(context: Context, enabled: Boolean) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "TOGGLE_SIMULATION"
                putExtra("enabled", enabled)
            }
            context.startService(intent)
        }

        fun toggleOutOfCity(context: Context, enabled: Boolean, surchargePct: Double = 25.0) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = "TOGGLE_OUT_OF_CITY"
                putExtra("enabled", enabled)
                putExtra("surchargePct", surchargePct)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing tracking Service")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        
        val database = TripDatabase.getDatabase(this)
        repository = TripRepository(database.tripDao())

        // Setup notification channel
        createNotificationChannel()

        // Setup WakeLock
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaxiMeter::TrackingLock")

        // Setup TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsInitialized = true
                Log.d(TAG, "TTS Initialized successfully")
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun safeStartForeground(id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasLocationPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasLocationPerm) {
                    startForeground(id, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(id, notification)
                }
            } else {
                startForeground(id, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "safeStartForeground caught exception safely", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand Action: $action")

        when (action) {
            "START" -> {
                val base = intent.getDoubleExtra("baseFare", 80.0)
                val rate = intent.getDoubleExtra("farePerKm", 28.0)
                val wait = intent.getDoubleExtra("waitFarePerMin", 2.0)
                val curr = intent.getStringExtra("currency") ?: "₹"
                val speedLim = intent.getDoubleExtra("speedThreshold", 5.0)

                initiateNewTrip(base, rate, wait, curr, speedLim)
            }
            "MONITOR" -> {
                val base = intent.getDoubleExtra("baseFare", 80.0)
                val rate = intent.getDoubleExtra("farePerKm", 28.0)
                val wait = intent.getDoubleExtra("waitFarePerMin", 2.0)
                val curr = intent.getStringExtra("currency") ?: "₹"
                val speedLim = intent.getDoubleExtra("speedThreshold", 5.0)
                val autoStart = intent.getBooleanExtra("autoStartEnabled", true)

                _tripState.value = _tripState.value.copy(
                    baseFare = base,
                    farePerKm = rate,
                    waitFarePerMin = wait,
                    currency = curr,
                    speedThreshold = speedLim,
                    autoStartEnabled = autoStart
                )

                recalculateFare()
                safeStartForeground(NOTIFICATION_ID, buildNotification())
                startTrackingLocation()
            }
            "PAUSE" -> {
                pauseActiveTrip()
            }
            "RESUME" -> {
                resumeActiveTrip()
            }
            "STOP" -> {
                stopAndSaveTrip()
            }
            "RECOVER" -> {
                recoverSavedTrip()
            }
            "TOGGLE_SIMULATION" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                _tripState.value = _tripState.value.copy(isSimulationEnabled = enabled)
                if (enabled) {
                    announceVoice("Simulation demo mode enabled.")
                } else {
                    announceVoice("GPS tracking restored.")
                }
            }
            "TOGGLE_OUT_OF_CITY" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                val surchargePct = intent.getDoubleExtra("surchargePct", 25.0)
                _tripState.value = _tripState.value.copy(
                    isOutOfCity = enabled,
                    outOfCitySurchargePercent = surchargePct
                )
                recalculateFare()
                if (enabled) {
                    announceVoice("Out of city outstation tariff activated.")
                } else {
                    announceVoice("Standard city tariff restored.")
                }
            }
        }

        return START_STICKY
    }

    private fun announceVoice(message: String) {
        if (isTtsInitialized && _tripState.value.status != TripStatus.IDLE) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun resolveAddress(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "GPS Location"
        return try {
            val geocoder = android.location.Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val line = addr.getAddressLine(0)
                if (!line.isNullOrBlank()) {
                    line
                } else {
                    val subLoc = addr.subLocality ?: addr.locality ?: ""
                    val city = addr.adminArea ?: ""
                    if (subLoc.isNotEmpty()) "$subLoc, $city" else String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
                }
            } else {
                String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "Lat: %.4f, Lng: %.4f", lat, lng)
        }
    }

    private fun initiateNewTrip(base: Double, rate: Double, wait: Double, curr: String, speedLim: Double) {
        acquireWakeLock()
        
        val initialPoints = if (_tripState.value.latitude != null && _tripState.value.longitude != null) {
            listOf(Pair(_tripState.value.latitude!!, _tripState.value.longitude!!))
        } else {
            emptyList()
        }

        val startLat = _tripState.value.latitude ?: 0.0
        val startLng = _tripState.value.longitude ?: 0.0
        val pickupAddr = resolveAddress(startLat, startLng)

        _tripState.value = TripState(
            status = TripStatus.RUNNING,
            startTime = System.currentTimeMillis(),
            baseFare = base,
            farePerKm = rate,
            waitFarePerMin = wait,
            currency = curr,
            speedThreshold = speedLim,
            currentFare = base,
            latitude = _tripState.value.latitude,
            longitude = _tripState.value.longitude,
            isOutOfCity = _tripState.value.isOutOfCity,
            outOfCitySurchargePercent = _tripState.value.outOfCitySurchargePercent,
            pickupAddress = pickupAddr,
            routePoints = initialPoints
        )

        recalculateFare()
        safeStartForeground(NOTIFICATION_ID, buildNotification())

        lastLocation = null
        startTrackingLocation()
        startTimers()

        announceVoice("Ride started. Base fare of $curr$base applied.")
        
        saveSnapshotToDatabase()
    }

    private fun pauseActiveTrip() {
        if (_tripState.value.status == TripStatus.RUNNING) {
            _tripState.value = _tripState.value.copy(
                status = TripStatus.PAUSED,
                speedKmH = 0.0
            )
            stopTrackingLocation()
            announceVoice("Ride paused.")
            updateNotification()
            saveSnapshotToDatabase()
        }
    }

    private fun resumeActiveTrip() {
        if (_tripState.value.status == TripStatus.PAUSED) {
            _tripState.value = _tripState.value.copy(
                status = TripStatus.RUNNING
            )
            safeStartForeground(NOTIFICATION_ID, buildNotification())
            lastLocation = null
            startTrackingLocation()
            announceVoice("Ride resumed.")
            updateNotification()
            saveSnapshotToDatabase()
        }
    }

    private fun recoverSavedTrip() {
        serviceScope.launch {
            val record = repository.getActiveTrip()
            if (record != null) {
                // Recover details from last snapshot
                _tripState.value = TripState(
                    status = if (record.isPaused) TripStatus.PAUSED else TripStatus.RUNNING,
                    startTime = record.startTime,
                    durationSeconds = record.elapsedSeconds,
                    waitingSeconds = record.accumulatedWaitingSeconds,
                    distanceKm = record.accumulatedDistanceKm,
                    latitude = if (record.lastLatitude != 0.0) record.lastLatitude else null,
                    longitude = if (record.lastLongitude != 0.0) record.lastLongitude else null,
                    currentFare = 0.0 // will be calculated below
                )
                
                // Let's load preferences or default configurations
                // Just do calculation based on our state
                recalculateFare()

                acquireWakeLock()
                
                if (_tripState.value.status == TripStatus.RUNNING) {
                    startTrackingLocation()
                }
                
                startTimers()

                safeStartForeground(NOTIFICATION_ID, buildNotification())
                announceVoice("Cab tracking recovered.")
            } else {
                stopSelf()
            }
        }
    }

    private fun stopAndSaveTrip() {
        val currentState = _tripState.value
        if (currentState.status != TripStatus.IDLE) {
            val startLat = currentState.routePoints.firstOrNull()?.first ?: (currentState.latitude ?: 0.0)
            val startLng = currentState.routePoints.firstOrNull()?.second ?: (currentState.longitude ?: 0.0)
            val endLat = currentState.latitude ?: 0.0
            val endLng = currentState.longitude ?: 0.0

            val pAddress = if (currentState.pickupAddress.isNotBlank() && currentState.pickupAddress != "GPS Location") {
                currentState.pickupAddress
            } else {
                resolveAddress(startLat, startLng)
            }
            val dAddress = resolveAddress(endLat, endLng)

            _tripState.value = currentState.copy(
                status = TripStatus.FINISHED,
                pickupAddress = pAddress,
                dropAddress = dAddress
            )
            
            announceVoice("Ride completed. Receipts ready. Total fare ${currentState.currency}${String.format(Locale.US, "%.2f", currentState.currentFare)}")
            
            // Clean trackers
            stopTrackingLocation()
            stopTimers()
            releaseWakeLock()

            // Save completed trip to database in background
            serviceScope.launch {
                val dbId = repository.insertTrip(
                    com.example.data.database.TripEntity(
                        startTime = currentState.startTime,
                        endTime = System.currentTimeMillis(),
                        distanceKm = currentState.distanceKm,
                        durationSeconds = currentState.durationSeconds,
                        waitingSeconds = currentState.waitingSeconds,
                        totalFare = currentState.currentFare,
                        startLatitude = startLat,
                        startLongitude = startLng,
                        endLatitude = endLat,
                        endLongitude = endLng,
                        pickupAddress = pAddress,
                        dropAddress = dAddress,
                        isOutOfCity = currentState.isOutOfCity,
                        outOfCitySurcharge = currentState.outOfCitySurchargeAmount,
                        baseFare = currentState.baseFare,
                        farePerKm = currentState.farePerKm,
                        waitFarePerMin = currentState.waitFarePerMin,
                        currency = currentState.currency
                    )
                )
                repository.clearActiveTrip()
                
                // Keep finished state so UI can retrieve last ride ID
                Log.d(TAG, "Completed trip saved in Room with ID $dbId")
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            // Stop service after saving completion
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingLocation() {
        val hasFineLoc = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarseLoc = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFineLoc && !hasCoarseLoc) {
            Log.w(TAG, "Location permissions not granted yet, skipping location tracking request")
            return
        }

        // Clean up previous callbacks before creating a new one
        stopTrackingLocation()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).apply {
            setMinUpdateIntervalMillis(1000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processIncomingLocation(location)
                }
            }
        }

        var requestedSuccessfully = false
        if (fusedLocationClient != null) {
            try {
                fusedLocationClient?.requestLocationUpdates(
                    request,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
                requestedSuccessfully = true
                Log.d(TAG, "Fused location updates requested successfully")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed requesting Fused GPS updates", e)
            }
        }

        if (!requestedSuccessfully && locationManager != null) {
            // Direct android.location.LocationManager Registration for fallback
            try {
                locationListener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        processIncomingLocation(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ->
                        android.location.LocationManager.GPS_PROVIDER
                    locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true ->
                        android.location.LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider != null) {
                    locationManager?.requestLocationUpdates(
                        provider,
                        1000L,
                        1.0f,
                        locationListener!!,
                        Looper.getMainLooper()
                    )
                    Log.d(TAG, "LocationManager $provider registered")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed requesting LocationManager updates", e)
            }
        }
    }

    private fun stopTrackingLocation() {
        try {
            locationCallback?.let {
                fusedLocationClient?.removeLocationUpdates(it)
                locationCallback = null
            }
            locationListener?.let {
                locationManager?.removeUpdates(it)
                locationListener = null
            }
            Log.d(TAG, "Stopped all GPS location updates")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun processIncomingLocation(location: Location) {
        if (_tripState.value.isSimulationEnabled) {
            return // Skip hardware GPS when demo simulation mode is running
        }

        // Discard duplicate or backwards-in-time updates from dual providers
        if (lastLocation != null && location.time <= lastLocation!!.time) {
            return
        }

        // Allow up to 150.0m accuracy tolerance for real mobile reception
        if (location.hasAccuracy() && location.accuracy > 150.0f) {
            Log.d(TAG, "Discarded inaccurate coordinate: ${location.accuracy}m")
            return
        }

        val currentState = _tripState.value
        val speedMps = if (location.hasSpeed()) location.speed else 0.0f
        var speedKmh = speedMps * 3.6 // m/s to km/h

        var deltaDistanceM = 0.0
        if (lastLocation != null) {
            deltaDistanceM = lastLocation!!.distanceTo(location).toDouble()
            val timeDeltaS = (location.time - lastLocation!!.time) / 1000.0
            if (speedKmh <= 0.0 && timeDeltaS > 0.1) {
                speedKmh = (deltaDistanceM / timeDeltaS) * 3.6
            }
            if (timeDeltaS > 0.1 && (deltaDistanceM / timeDeltaS) * 3.6 > 250.0) {
                Log.d(TAG, "Discarded GPS jump anomaly")
                lastLocation = location
                return
            }
        }

        // Determine if vehicle is moving based on speed or location displacement
        val isMoving = speedKmh >= currentState.speedThreshold || deltaDistanceM >= 2.0

        // AUTO-START ON SPEED THRESHOLD
        if (currentState.autoStartEnabled) {
            if (currentState.status == TripStatus.IDLE && isMoving) {
                Log.d(TAG, "Speed $speedKmh km/h exceeded threshold ${currentState.speedThreshold} km/h. Auto-starting meter!")
                initiateNewTrip(
                    base = currentState.baseFare,
                    rate = currentState.farePerKm,
                    wait = currentState.waitFarePerMin,
                    curr = currentState.currency,
                    speedLim = currentState.speedThreshold
                )
                announceVoice("Vehicle speed detected. Taxi meter auto-started.")
                return
            } else if (currentState.status == TripStatus.PAUSED && isMoving) {
                Log.d(TAG, "Vehicle movement detected ($speedKmh km/h). Auto-resuming trip!")
                resumeActiveTrip()
                announceVoice("Vehicle movement detected. Ride auto-resumed.")
                return
            }
        }

        if (currentState.status != TripStatus.RUNNING) {
            _tripState.value = currentState.copy(
                speedKmH = speedKmh,
                isMoving = isMoving,
                latitude = location.latitude,
                longitude = location.longitude
            )
            lastLocation = location
            updateNotification()
            return
        }

        // ALWAYS ACCUMULATE DISTANCE WHEN PHYSICAL POSITION CHANGES (>= 0.5 meters)
        var newDistance = currentState.distanceKm
        if (lastLocation != null && deltaDistanceM >= 0.5) {
            newDistance += deltaDistanceM / 1000.0 // Convert meters to KM
        }

        lastLocation = location

        val currentPoint = Pair(location.latitude, location.longitude)
        val updatedRoutePoints = if (currentState.routePoints.isEmpty()) {
            listOf(currentPoint)
        } else {
            val lastPt = currentState.routePoints.last()
            val results = FloatArray(1)
            Location.distanceBetween(lastPt.first, lastPt.second, location.latitude, location.longitude, results)
            if (results[0] >= 2.5) { // filter out jitter under 2.5 meters
                currentState.routePoints + currentPoint
            } else {
                currentState.routePoints
            }
        }

        _tripState.value = currentState.copy(
            distanceKm = newDistance,
            speedKmH = speedKmh,
            isMoving = isMoving,
            isWaitingPaused = isMoving, // PAUSE WAITING CHARGES WHILE MOVING ABOVE THRESHOLD
            latitude = location.latitude,
            longitude = location.longitude,
            routePoints = updatedRoutePoints
        )

        recalculateFare()
        updateNotification()
    }

    private fun recalculateFare() {
        val state = _tripState.value
        val waitMinutes = state.waitingSeconds / 60.0
        val base = state.baseFare
        val distCharge = state.distanceKm * state.farePerKm
        val waitCharge = waitMinutes * state.waitFarePerMin
        val subtotal = base + distCharge + waitCharge
        
        val surcharge = if (state.isOutOfCity) {
            subtotal * (state.outOfCitySurchargePercent / 100.0)
        } else {
            0.0
        }
        val total = subtotal + surcharge

        _tripState.value = state.copy(
            currentFare = total,
            outOfCitySurchargeAmount = surcharge
        )
    }

    private fun startTimers() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val state = _tripState.value
                if (state.status == TripStatus.RUNNING) {
                    val nextElapsed = state.durationSeconds + 1

                    if (state.isSimulationEnabled) {
                        // SIMULATION MODE: Simulate ~36 km/h drive (+0.01 km = 10 meters per second)
                        val nextDist = state.distanceKm + 0.010
                        _tripState.value = state.copy(
                            durationSeconds = nextElapsed,
                            distanceKm = nextDist,
                            speedKmH = 36.0,
                            isMoving = true,
                            isWaitingPaused = true
                        )
                    } else {
                        // NORMAL GPS MODE
                        var nextWait = state.waitingSeconds
                        val isMoving = state.isMoving || state.speedKmH >= state.speedThreshold
                        if (!isMoving) {
                            nextWait += 1
                        }

                        _tripState.value = state.copy(
                            durationSeconds = nextElapsed,
                            waitingSeconds = nextWait,
                            isWaitingPaused = isMoving
                        )
                    }

                    recalculateFare()
                    updateNotification()

                    // Periodically snapshot state to support graceful recovery
                    if (nextElapsed % 5 == 0L) {
                        saveSnapshotToDatabase()
                    }
                }
            }
        }
    }

    private fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun saveSnapshotToDatabase() {
        val state = _tripState.value
        if (state.status == TripStatus.RUNNING || state.status == TripStatus.PAUSED) {
            serviceScope.launch(Dispatchers.IO) {
                repository.saveActiveTrip(
                    ActiveTripRecord(
                        startTime = state.startTime,
                        isPaused = state.status == TripStatus.PAUSED,
                        accumulatedDistanceKm = state.distanceKm,
                        accumulatedWaitingSeconds = state.waitingSeconds,
                        elapsedSeconds = state.durationSeconds,
                        lastLatitude = state.latitude ?: 0.0,
                        lastLongitude = state.longitude ?: 0.0,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Taxi Meter Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live fare metering in background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val state = _tripState.value
        val formattedFare = "${state.currency}${String.format(Locale.US, "%.2f", state.currentFare)}"
        val formattedDist = "${String.format(Locale.US, "%.2f", state.distanceKm)} km"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (state.status == TripStatus.IDLE) {
            "Taxi Meter: Auto-Start Ready"
        } else {
            "Active Meter: $formattedFare"
        }

        val waitStateStr = if (state.isWaitingPaused) "Wait: Paused" else "Wait: Accruing"
        val text = if (state.status == TripStatus.IDLE) {
            "Speed: ${String.format(Locale.US, "%.1f", state.speedKmH)} km/h (Threshold: ${state.speedThreshold} km/h)"
        } else {
            "Dist: $formattedDist | Speed: ${String.format(Locale.US, "%.1f", state.speedKmH)} km/h | $waitStateStr"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        try {
            val hasWakeLockPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WAKE_LOCK
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasWakeLockPerm && _tripState.value.status == TripStatus.RUNNING && wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours limit max safety timeout
                Log.d(TAG, "WakeLock acquired successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Shutting down tracking service")
        stopTrackingLocation()
        stopTimers()
        releaseWakeLock()
        
        tts?.let {
            it.stop()
            it.shutdown()
        }

        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
