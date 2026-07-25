package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taxi_meter_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_BASE_FARE = doublePreferencesKey("base_fare")
        val KEY_FARE_PER_KM = doublePreferencesKey("fare_per_km")
        val KEY_WAIT_FARE_PER_MIN = doublePreferencesKey("wait_fare_per_min")
        val KEY_SPEED_THRESHOLD = doublePreferencesKey("speed_threshold") // in km/h
        val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val KEY_AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        val KEY_CURRENCY = stringPreferencesKey("currency")
    }

    // Default configuration values
    val baseFare: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_BASE_FARE] ?: 80.00
    }

    val farePerKm: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_FARE_PER_KM] ?: 28.00
    }

    val waitFarePerMin: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_WAIT_FARE_PER_MIN] ?: 2.00
    }

    val speedThreshold: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_SPEED_THRESHOLD] ?: 5.0
    }

    val audioEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUDIO_ENABLED] ?: true
    }

    val autoStartEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_START_ENABLED] ?: false
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY] ?: "₹"
    }

    // Modern setter functions
    suspend fun updateBaseFare(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_FARE] = value
        }
    }

    suspend fun updateFarePerKm(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FARE_PER_KM] = value
        }
    }

    suspend fun updateWaitFarePerMin(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WAIT_FARE_PER_MIN] = value
        }
    }

    suspend fun updateSpeedThreshold(value: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEED_THRESHOLD] = value
        }
    }

    suspend fun updateAudioEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUDIO_ENABLED] = value
        }
    }

    suspend fun updateAutoStartEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_ENABLED] = value
        }
    }

    suspend fun updateCurrency(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY] = value
        }
    }
}
