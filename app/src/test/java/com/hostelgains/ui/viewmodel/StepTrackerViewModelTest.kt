package com.hostelgains.ui.viewmodel

import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StepTrackerViewModelTest {

    private lateinit var evaluator: DefaultStepAlertEvaluator
    private lateinit var viewModel: StepTrackerViewModel

    @Before
    fun setUp() {
        evaluator = DefaultStepAlertEvaluator()
        viewModel = StepTrackerViewModel(
            alertEvaluator = evaluator,
            initialSteps = 3450,
            initialHour = 18
        )
    }

    @Test
    fun testInitialState_triggers6PMDeficitAlertWhenSub5000() {
        val state = viewModel.uiState.value
        assertEquals(3450, state.currentSteps)
        assertEquals(18, state.simulatedHour)
        assertTrue(state.alertResult.isAlertTriggered)
        assertNotNull(state.alertResult.alertTitle)
        assertTrue(state.alertResult.alertMessage?.contains("10,000-step walk") == true)
    }

    @Test
    fun testNoAlert_whenTimeIsBefore6PM() {
        // Set time to 17:59 (5:59 PM) with 3450 steps
        viewModel.setTime(17, 59)
        val state = viewModel.uiState.value
        assertFalse(state.alertResult.isAlertTriggered)

        // Set time to 12:00 PM with 3450 steps
        viewModel.setTime(12, 0)
        assertFalse(viewModel.uiState.value.alertResult.isAlertTriggered)
    }

    @Test
    fun testNoAlert_whenStepsAre5000OrGreaterAt6PM() {
        viewModel.setTime(18, 0)
        viewModel.setSteps(5000)
        val state = viewModel.uiState.value
        assertFalse(state.alertResult.isAlertTriggered)

        viewModel.setSteps(10200)
        assertFalse(viewModel.uiState.value.alertResult.isAlertTriggered)
    }

    @Test
    fun testAddSteps_updatesMetricsAccurately() {
        viewModel.setSteps(0)
        assertEquals(0, viewModel.uiState.value.currentSteps)
        assertEquals(0f, viewModel.uiState.value.distanceKm, 0.01f)
        assertEquals(0, viewModel.uiState.value.caloriesBurned)

        viewModel.addSteps(4000)
        val state = viewModel.uiState.value
        assertEquals(4000, state.currentSteps)
        assertTrue(state.distanceKm > 2.0f)
        assertTrue(state.caloriesBurned > 100)
    }

    @Test
    fun testResetSteps_clearsStepCount() {
        viewModel.setSteps(7500)
        assertEquals(7500, viewModel.uiState.value.currentSteps)

        viewModel.resetSteps()
        assertEquals(0, viewModel.uiState.value.currentSteps)
    }
}
