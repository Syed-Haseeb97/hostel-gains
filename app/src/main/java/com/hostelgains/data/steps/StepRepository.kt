package com.hostelgains.data.steps

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.stepDataStore: DataStore<Preferences> by preferencesDataStore(name = "hostel_step_prefs")

/**
 * Repository persisting and reconciling step counts across device reboots and daily midnight resets.
 */
class StepRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.stepDataStore)

    companion object {
        val KEY_DATE = stringPreferencesKey("step_date")
        val KEY_TODAY_STEPS = intPreferencesKey("today_steps")
        val KEY_LAST_RAW = longPreferencesKey("last_raw_steps")
        val KEY_DAY_START_RAW = longPreferencesKey("day_start_raw_steps")
    }

    val todayStepsFlow: Flow<Int> = dataStore.data.map { prefs ->
        val savedDate = prefs[KEY_DATE] ?: ""
        val today = LocalDate.now().toString()
        if (savedDate == today) {
            prefs[KEY_TODAY_STEPS] ?: 0
        } else {
            0
        }
    }

    suspend fun recordRawSteps(rawSteps: Long) {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val savedDate = prefs[KEY_DATE] ?: ""
            val lastRaw = prefs[KEY_LAST_RAW] ?: 0L
            val dayStartRaw = prefs[KEY_DAY_START_RAW] ?: rawSteps
            val currentSteps = prefs[KEY_TODAY_STEPS] ?: 0

            if (savedDate != today) {
                // New day rollover
                prefs[KEY_DATE] = today
                prefs[KEY_DAY_START_RAW] = rawSteps
                prefs[KEY_LAST_RAW] = rawSteps
                prefs[KEY_TODAY_STEPS] = 0
            } else if (rawSteps < lastRaw) {
                // Device reboot occurred (raw counter reset to 0)
                prefs[KEY_DAY_START_RAW] = rawSteps
                prefs[KEY_LAST_RAW] = rawSteps
                // Keep today's accumulated steps intact
            } else {
                // Normal increment
                val delta = (rawSteps - lastRaw).coerceAtLeast(0L).toInt()
                prefs[KEY_LAST_RAW] = rawSteps
                prefs[KEY_TODAY_STEPS] = currentSteps + delta
            }
        }
    }

    suspend fun setManualSteps(steps: Int) {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            prefs[KEY_DATE] = today
            prefs[KEY_TODAY_STEPS] = steps.coerceAtLeast(0)
        }
    }

    suspend fun addSteps(delta: Int) {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val savedDate = prefs[KEY_DATE] ?: ""
            val current = if (savedDate == today) (prefs[KEY_TODAY_STEPS] ?: 0) else 0
            prefs[KEY_DATE] = today
            prefs[KEY_TODAY_STEPS] = (current + delta).coerceAtLeast(0)
        }
    }

    suspend fun resetSteps() {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            prefs[KEY_DATE] = today
            prefs[KEY_TODAY_STEPS] = 0
        }
    }
}
