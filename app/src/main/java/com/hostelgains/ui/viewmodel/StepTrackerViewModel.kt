package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelgains.data.steps.StepRepository
import com.hostelgains.data.steps.StepSensorSource
import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import com.hostelgains.domain.steps.StepAlertEvaluator
import com.hostelgains.domain.steps.StepAlertResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val activeMinutes: Int = 32,
    val isHardwareSensorActive: Boolean = false
)

class StepTrackerViewModel(
    private val alertEvaluator: StepAlertEvaluator = DefaultStepAlertEvaluator(),
    private val stepRepository: StepRepository? = null,
    private val sensorSource: StepSensorSource? = null,
    private val externalScope: CoroutineScope? = null,
    initialSteps: Int = 3450,
    initialHour: Int = 18
) : ViewModel() {

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope
        get() = externalScope ?: internalScope

    override fun onCleared() {
        super.onCleared()
        internalScope.cancel()
    }

    private val _uiState = MutableStateFlow(
        createState(initialSteps, initialHour, 0, isHardwareSensorActive = sensorSource?.isSensorAvailable == true)
    )
    val uiState: StateFlow<StepTrackerUiState> = _uiState.asStateFlow()

    init {
        if (stepRepository != null) {
            val now = LocalTime.now()
            _uiState.value = createState(
                steps = _uiState.value.currentSteps,
                hour = now.hour,
                minute = now.minute,
                isHardwareSensorActive = sensorSource?.isSensorAvailable == true
            )

            // 1. Observe persistent steps from repository
            scope.launch {
                stepRepository.todayStepsFlow.collect { realSteps ->
                    val cur = _uiState.value
                    _uiState.value = createState(
                        steps = realSteps,
                        hour = cur.simulatedHour,
                        minute = cur.simulatedMinute,
                        isHardwareSensorActive = sensorSource?.isSensorAvailable == true
                    )
                }
            }

            // 2. Observe real-time hardware sensor updates if available
            if (sensorSource != null && sensorSource.isSensorAvailable) {
                scope.launch {
                    sensorSource.rawStepFlow.collect { rawCount ->
                        stepRepository.recordRawSteps(rawCount)
                    }
                }
            }
        }
    }

    private fun createState(
        steps: Int,
        hour: Int,
        minute: Int,
        isHardwareSensorActive: Boolean = false
    ): StepTrackerUiState {
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
            activeMinutes = activeMin,
            isHardwareSensorActive = isHardwareSensorActive
        )
    }

    fun setSteps(steps: Int) {
        val cur = _uiState.value
        _uiState.value = createState(steps, cur.simulatedHour, cur.simulatedMinute, cur.isHardwareSensorActive)
        if (stepRepository != null) {
            scope.launch {
                stepRepository.setManualSteps(steps)
            }
        }
    }

    fun addSteps(delta: Int) {
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps + delta, cur.simulatedHour, cur.simulatedMinute, cur.isHardwareSensorActive)
        if (stepRepository != null) {
            scope.launch {
                stepRepository.addSteps(delta)
            }
        }
    }

    fun setTime(hour: Int, minute: Int = 0) {
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps, hour.coerceIn(0, 23), minute.coerceIn(0, 59), cur.isHardwareSensorActive)
    }

    fun syncSystemTime() {
        val now = LocalTime.now()
        val cur = _uiState.value
        _uiState.value = createState(cur.currentSteps, now.hour, now.minute, cur.isHardwareSensorActive)
    }

    fun resetSteps() {
        val cur = _uiState.value
        _uiState.value = createState(0, cur.simulatedHour, cur.simulatedMinute, cur.isHardwareSensorActive)
        if (stepRepository != null) {
            scope.launch {
                stepRepository.resetSteps()
            }
        }
    }
}
