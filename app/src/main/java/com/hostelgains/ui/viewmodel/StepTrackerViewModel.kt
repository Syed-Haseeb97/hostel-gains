package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import com.hostelgains.domain.steps.StepAlertEvaluator
import com.hostelgains.domain.steps.StepAlertResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class StepTrackerUiState(
    val currentSteps: Int = 3450,
    val targetSteps: Int = 10000,
    val simulatedHour: Int = 18, // 6:00 PM default to demonstrate deficit trigger
    val simulatedMinute: Int = 0,
    val alertResult: StepAlertResult = StepAlertResult(
        isAlertTriggered = true,
        currentSteps = 3450,
        targetSteps = 10000,
        alertTitle = "Step Deficit Alert (Sub-5,000 at 6:00 PM)",
        alertMessage = "You have logged only 3450 steps today. To stay on track for body recomposition and insulin sensitivity, take a 10,000-step walk after your hostel dinner."
    ),
    val distanceKm: Float = 2.69f,
    val caloriesBurned: Int = 145,
    val activeMinutes: Int = 32
)

class StepTrackerViewModel(
    private val alertEvaluator: StepAlertEvaluator = DefaultStepAlertEvaluator(),
    initialSteps: Int = 3450,
    initialHour: Int = 18
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        createState(initialSteps, initialHour, 0)
    )
    val uiState: StateFlow<StepTrackerUiState> = _uiState.asStateFlow()

    private fun createState(steps: Int, hour: Int, minute: Int): StepTrackerUiState {
        val safeSteps = steps.coerceAtLeast(0)
        val alert = alertEvaluator.evaluate(safeSteps, hour, minute)
        val dist = (safeSteps * 0.00078f * 100).toInt() / 100f
        val kcal = (safeSteps * 0.042f).toInt()
        val activeMin = (safeSteps / 110).coerceAtLeast(0)

        return StepTrackerUiState(
            currentSteps = safeSteps,
            targetSteps = 10000,
            simulatedHour = hour,
            simulatedMinute = minute,
            alertResult = alert,
            distanceKm = dist,
            caloriesBurned = kcal,
            activeMinutes = activeMin
        )
    }

    fun setSteps(steps: Int) {
        val cur = _uiState.value
        _uiState.value = createState(steps, cur.simulatedHour, cur.simulatedMinute)
    }

    fun addSteps(delta: Int) {
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps + delta, cur.simulatedHour, cur.simulatedMinute)
    }

    fun setTime(hour: Int, minute: Int = 0) {
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun syncSystemTime() {
        val now = LocalTime.now()
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps, now.hour, now.minute)
    }

    fun resetSteps() {
        val cur = _uiState.value
        _uiState.value = createState(0, cur.simulatedHour, cur.simulatedMinute)
    }
}
