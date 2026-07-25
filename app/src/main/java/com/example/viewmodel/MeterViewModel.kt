package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.data.model.TripState
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.TripRepository
import com.example.service.LocationTrackingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MeterViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepository: TripRepository
    private val settingsRepository: SettingsRepository

    val tripState: StateFlow<TripState> = LocationTrackingService.tripState

    val allTrips: StateFlow<List<TripEntity>>

    private val _hasActiveSessionBackup = MutableStateFlow(false)
    val hasActiveSessionBackup: StateFlow<Boolean> = _hasActiveSessionBackup.asStateFlow()

    init {
        val database = TripDatabase.getDatabase(application)
        tripRepository = TripRepository(database.tripDao())
        settingsRepository = SettingsRepository(application)

        allTrips = tripRepository.allTrips
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        checkForActiveBackup()
        startMonitoring()
    }

    fun startMonitoring() {
        viewModelScope.launch {
            val baseFareVal = settingsRepository.baseFare.first()
            val farePerKmVal = settingsRepository.farePerKm.first()
            val waitFareVal = settingsRepository.waitFarePerMin.first()
            val currencyVal = settingsRepository.currency.first()
            val thresholdVal = settingsRepository.speedThreshold.first()
            val autoStartVal = settingsRepository.autoStartEnabled.first()

            LocationTrackingService.startMonitoring(
                getApplication(),
                baseFare = baseFareVal,
                farePerKm = farePerKmVal,
                waitFarePerMin = waitFareVal,
                currency = currencyVal,
                speedThreshold = thresholdVal,
                autoStartEnabled = autoStartVal
            )
        }
    }

    fun checkForActiveBackup() {
        viewModelScope.launch {
            val backup = tripRepository.getActiveTrip()
            _hasActiveSessionBackup.value = backup != null
            Log.d("MeterViewModel", "Checked active session backup: ${backup != null}")
        }
    }

    fun startTrip() {
        viewModelScope.launch {
            // Read active settings directly
            val baseFareVal = settingsRepository.baseFare.first()
            val farePerKmVal = settingsRepository.farePerKm.first()
            val waitFareVal = settingsRepository.waitFarePerMin.first()
            val currencyVal = settingsRepository.currency.first()
            val thresholdVal = settingsRepository.speedThreshold.first()

            LocationTrackingService.startTrip(
                getApplication(),
                baseFare = baseFareVal,
                farePerKm = farePerKmVal,
                waitFarePerMin = waitFareVal,
                currency = currencyVal,
                speedThreshold = thresholdVal
            )
            _hasActiveSessionBackup.value = false
        }
    }

    fun pauseTrip() {
        LocationTrackingService.pauseTrip(getApplication())
    }

    fun resumeTrip() {
        LocationTrackingService.resumeTrip(getApplication())
    }

    fun stopTrip() {
        LocationTrackingService.stopTrip(getApplication())
        // Reset backup state check after a short delay
        viewModelScope.launch {
            _hasActiveSessionBackup.value = false
        }
    }

    fun recoverTrip() {
        LocationTrackingService.recoveryTrip(getApplication())
        _hasActiveSessionBackup.value = false
    }

    fun discardBackup() {
        viewModelScope.launch {
            tripRepository.clearActiveTrip()
            _hasActiveSessionBackup.value = false
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        LocationTrackingService.toggleSimulation(getApplication(), enabled)
    }

    fun toggleOutOfCity(enabled: Boolean) {
        viewModelScope.launch {
            val surchargePct = settingsRepository.outOfCitySurchargePercent.first()
            LocationTrackingService.toggleOutOfCity(getApplication(), enabled, surchargePct)
        }
    }

    fun deleteTrip(id: Int) {
        viewModelScope.launch {
            tripRepository.deleteTripById(id)
        }
    }
}
