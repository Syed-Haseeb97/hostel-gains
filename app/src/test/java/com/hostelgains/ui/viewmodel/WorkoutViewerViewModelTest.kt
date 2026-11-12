package com.hostelgains.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutViewerViewModelTest {

    private lateinit var viewModel: WorkoutViewerViewModel

    @Before
    fun setUp() {
        viewModel = WorkoutViewerViewModel()
    }

    @Test
    fun testInitialState_defaultsToLatPulldownsAndPlaying() {
        val state = viewModel.uiState.value
        assertEquals("Wide-Grip Lat Pulldowns", state.selectedExercise.name)
        assertTrue(state.isPlaying)
        assertEquals(1.0f, state.playbackSpeed, 0.01f)
        assertFalse(state.isAutoOrbit)
    }

    @Test
    fun testSelectExercise_updatesActiveExerciseDetails() {
        viewModel.selectExercise("Incline Dumbbell Press")
        var state = viewModel.uiState.value
        assertEquals("Incline Dumbbell Press", state.selectedExercise.name)
        assertTrue(state.selectedExercise.targetMuscles.any { it.contains("Chest") })

        viewModel.selectExercise("Goblet Squats")
        state = viewModel.uiState.value
        assertEquals("Goblet Squats", state.selectedExercise.name)
        assertTrue(state.selectedExercise.targetMuscles.any { it.contains("Quad") })
    }

    @Test
    fun testPlaybackControls_togglePlayPauseAndSpeed() {
        viewModel.togglePlayPause()
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.setSpeed(1.5f)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed, 0.01f)

        viewModel.setSpeed(0.5f)
        assertEquals(0.5f, viewModel.uiState.value.playbackSpeed, 0.01f)
    }

    @Test
    fun testCameraPresets_updatesYawAndPitch() {
        viewModel.setCameraPreset(CameraPreset.FRONT)
        var state = viewModel.uiState.value
        assertEquals(CameraPreset.FRONT.yawDeg, state.yawDeg, 0.01f)
        assertFalse(state.isAutoOrbit)

        viewModel.setCameraPreset(CameraPreset.SIDE)
        state = viewModel.uiState.value
        assertEquals(CameraPreset.SIDE.yawDeg, state.yawDeg, 0.01f)

        viewModel.setCameraPreset(CameraPreset.THREE_QUARTER)
        state = viewModel.uiState.value
        assertEquals(CameraPreset.THREE_QUARTER.yawDeg, state.yawDeg, 0.01f)
    }

    @Test
    fun testAutoOrbit_togglesCorrectly() {
        viewModel.toggleAutoOrbit()
        assertTrue(viewModel.uiState.value.isAutoOrbit)

        viewModel.toggleAutoOrbit()
        assertFalse(viewModel.uiState.value.isAutoOrbit)
    }
}
