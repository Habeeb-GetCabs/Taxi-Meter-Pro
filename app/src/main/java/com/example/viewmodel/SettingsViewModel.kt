package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val baseFare: StateFlow<Double> = settingsRepository.baseFare
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80.00)

    val farePerKm: StateFlow<Double> = settingsRepository.farePerKm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28.00)

    val waitFarePerMin: StateFlow<Double> = settingsRepository.waitFarePerMin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.00)

    val speedThreshold: StateFlow<Double> = settingsRepository.speedThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5.0)

    val audioEnabled: StateFlow<Boolean> = settingsRepository.audioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoStartEnabled: StateFlow<Boolean> = settingsRepository.autoStartEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currency: StateFlow<String> = settingsRepository.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    val outOfCitySurchargePercent: StateFlow<Double> = settingsRepository.outOfCitySurchargePercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25.0)

    fun updateBaseFare(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateBaseFare(value)
        }
    }

    fun updateFarePerKm(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateFarePerKm(value)
        }
    }

    fun updateWaitFarePerMin(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateWaitFarePerMin(value)
        }
    }

    fun updateSpeedThreshold(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateSpeedThreshold(value)
        }
    }

    fun updateAudioEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAudioEnabled(value)
        }
    }

    fun updateAutoStartEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoStartEnabled(value)
        }
    }

    fun updateCurrency(value: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(value)
        }
    }

    fun updateOutOfCitySurchargePercent(value: Double) {
        viewModelScope.launch {
            settingsRepository.updateOutOfCitySurchargePercent(value)
        }
    }
}
