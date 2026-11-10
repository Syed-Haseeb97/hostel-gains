package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CameraPreset(val label: String, val yawDeg: Float, val pitchDeg: Float) {
    FRONT("Front", 90f, 10f),
    SIDE("Side", 0f, 10f),
    THREE_QUARTER("3/4 View", -35f, 15f)
}

data class ExerciseViewerModel(
    val id: String,
    val name: String,
    val targetMuscles: List<String>,
    val apparatus: String,
    val formCue: String,
    val recommendedSets: String = "4 x 8-12 reps"
)

data class WorkoutViewerUiState(
    val availableExercises: List<ExerciseViewerModel>,
    val selectedExercise: ExerciseViewerModel,
    val isPlaying: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val yawDeg: Float = -35f,
    val pitchDeg: Float = 15f,
    val isAutoOrbit: Boolean = false
)

class WorkoutViewerViewModel : ViewModel() {

    companion object {
        val ALL_EXERCISES = listOf(
            ExerciseViewerModel(
                id = "d1_lat_pulldown",
                name = "Wide-Grip Lat Pulldowns",
                targetMuscles = listOf("Latissimus Dorsi", "Teres Major", "Biceps"),
                apparatus = "Lat Pulldown Machine / Wide Cable Bar",
                formCue = "Pull bar to upper chest, drive elbows down into back pockets, control 2s eccentric.",
                recommendedSets = "4 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d2_incline_db_press",
                name = "Incline Dumbbell Press",
                targetMuscles = listOf("Clavicular Upper Chest", "Anterior Deltoids", "Triceps"),
                apparatus = "30° Incline Bench & Dumbbells",
                formCue = "30-degree incline, deep pec stretch at bottom, tuck elbows at 45 degrees.",
                recommendedSets = "4 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d3_goblet_squats",
                name = "Goblet Squats",
                targetMuscles = listOf("Quadriceps", "Glutes", "Core"),
                apparatus = "Heavy Dumbbell / Kettlebell",
                formCue = "Hold dumbbell tight against sternum, break at hips and knees, drive through midfoot.",
                recommendedSets = "4 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d1_cable_rows",
                name = "Seated Cable Rows",
                targetMuscles = listOf("Rhomboids", "Mid Traps", "Lats"),
                apparatus = "Low Cable Row Station & V-Grip",
                formCue = "Chest upright, pull handles into belly button, squeeze shoulder blades together.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d2_db_lateral_raises",
                name = "Dumbbell Lateral Raises",
                targetMuscles = listOf("Lateral Deltoids"),
                apparatus = "Dumbbells",
                formCue = "Slight torso lean, raise in scapular plane, lead with elbows for 3D shoulder cap.",
                recommendedSets = "5 sets x 10-15 reps"
            ),
            ExerciseViewerModel(
                id = "d2_incline_pushups",
                name = "Incline Push-ups",
                targetMuscles = listOf("Chest", "Anterior Deltoids", "Triceps"),
                apparatus = "Elevated Bench / Bar",
                formCue = "Hands on bench, brace abdominal wall, touch lower sternum to edge, press explosively.",
                recommendedSets = "3 sets to failure"
            ),
            ExerciseViewerModel(
                id = "d1_hammer_curls",
                name = "Dumbbell Hammer Curls",
                targetMuscles = listOf("Brachialis", "Brachioradialis", "Biceps"),
                apparatus = "Dumbbells",
                formCue = "Neutral thumbs-up grip, pin elbows tight to ribs, no swinging.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d3_bw_squats",
                name = "Bodyweight Squats",
                targetMuscles = listOf("Quadriceps", "Glutes"),
                apparatus = "Bodyweight",
                formCue = "Continuous rhythm, break parallel, explosive quad burnout.",
                recommendedSets = "2 sets to failure"
            ),
            ExerciseViewerModel(
                id = "d6_face_pulls",
                name = "Face Pulls",
                targetMuscles = listOf("Rear Delts", "Rotator Cuff", "Rhomboids"),
                apparatus = "High Cable Pulley & Rope Attachment",
                formCue = "Pull rope to bridge of nose, externally rotate forearms backwards at finish.",
                recommendedSets = "3 sets x 15 reps"
            ),
            ExerciseViewerModel(
                id = "d3_plank",
                name = "Plank",
                targetMuscles = listOf("Transverse Abdominis", "Core Stability"),
                apparatus = "Floor Mat",
                formCue = "Hollow-body position, squeeze glutes, pull elbows toward toes isometrically.",
                recommendedSets = "3 sets x 60 sec"
            )
        )
    }

    private val _uiState = MutableStateFlow(
        WorkoutViewerUiState(
            availableExercises = ALL_EXERCISES,
            selectedExercise = ALL_EXERCISES[0]
        )
    )
    val uiState: StateFlow<WorkoutViewerUiState> = _uiState.asStateFlow()

    fun selectExercise(nameOrId: String) {
        val found = ALL_EXERCISES.find {
            it.name.equals(nameOrId, ignoreCase = true) || it.id.equals(nameOrId, ignoreCase = true)
        } ?: return
        _uiState.value = _uiState.value.copy(selectedExercise = found)
    }

    fun togglePlayPause() {
        val cur = _uiState.value
        _uiState.value = cur.copy(isPlaying = !cur.isPlaying)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun toggleAutoOrbit() {
        val cur = _uiState.value
        _uiState.value = cur.copy(isAutoOrbit = !cur.isAutoOrbit)
    }

    fun setCameraPreset(preset: CameraPreset) {
        _uiState.value = _uiState.value.copy(
            yawDeg = preset.yawDeg,
            pitchDeg = preset.pitchDeg,
            isAutoOrbit = false
        )
    }

    fun onYawPitchChanged(yaw: Float, pitch: Float) {
        _uiState.value = _uiState.value.copy(yawDeg = yaw, pitchDeg = pitch, isAutoOrbit = false)
    }
}
