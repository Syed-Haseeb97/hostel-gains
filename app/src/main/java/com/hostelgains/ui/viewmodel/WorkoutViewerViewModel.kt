package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CameraPreset(val label: String, val yawDeg: Float, val pitchDeg: Float) {
    FRONT("Front", 0f, 10f),
    THREE_QUARTER("3/4", 35f, 14f),
    SIDE("Side", 90f, 8f),
    BACK("Back", 180f, 12f)
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
    val yawDeg: Float = 35f,
    val pitchDeg: Float = 14f,
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
            ),
            ExerciseViewerModel(
                id = "d1_rear_delt_flyes",
                name = "Machine Rear Delt Flyes",
                targetMuscles = listOf("Rear Delts", "Rhomboids", "Mid Traps"),
                apparatus = "Reverse Pec-Deck Machine",
                formCue = "Lead with elbows, keep pinkies rotated slightly out, pause when arms are in line with shoulders.",
                recommendedSets = "4 sets x 10-15 reps"
            ),
            ExerciseViewerModel(
                id = "d2_tricep_rope_pushdowns",
                name = "Tricep Rope Pushdowns",
                targetMuscles = listOf("Triceps"),
                apparatus = "High Cable Pulley & Rope",
                formCue = "Pin elbows to your sides, flare the rope apart at the bottom and squeeze the horseshoe.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d3_lying_leg_curls",
                name = "Lying Leg Curls",
                targetMuscles = listOf("Hamstrings", "Calves"),
                apparatus = "Lying Leg Curl Machine",
                formCue = "Hips pressed into the pad, dorsiflex toes, 3-second eccentric lowering.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d4_db_rows",
                name = "Chest-Supported DB Rows",
                targetMuscles = listOf("Rhomboids", "Mid Traps", "Lats", "Rear Delts"),
                apparatus = "45° Incline Bench & Dumbbells",
                formCue = "Chest glued to the pad, row elbows back toward hips, full stretch at the bottom.",
                recommendedSets = "4 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d4_neutral_lat_pulldowns",
                name = "Neutral-Grip Lat Pulldowns",
                targetMuscles = listOf("Lower Lats", "Teres Major", "Biceps"),
                apparatus = "Lat Pulldown Machine & V-Handle",
                formCue = "Neutral grip, lean back slightly, drive elbows down to your ribs.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d4_incline_db_curls",
                name = "Incline Dumbbell Curls",
                targetMuscles = listOf("Biceps Long Head", "Brachialis"),
                apparatus = "Incline Bench & Dumbbells",
                formCue = "Arms hang behind the torso for a full stretch, supinate hard at the top.",
                recommendedSets = "3 sets x 10-12 reps"
            ),
            ExerciseViewerModel(
                id = "d4_preacher_curls",
                name = "Preacher Curls",
                targetMuscles = listOf("Biceps Short Head", "Brachialis"),
                apparatus = "Preacher Bench & EZ Bar",
                formCue = "Elbows fixed on the pad, strict isolation, no swinging, stop just short of lockout.",
                recommendedSets = "3 sets x 10-12 reps"
            ),
            ExerciseViewerModel(
                id = "d4_cable_face_pulls",
                name = "Cable Face Pulls",
                targetMuscles = listOf("Rear Delts", "Rotator Cuff", "Rhomboids"),
                apparatus = "High Cable Pulley & Rope",
                formCue = "Rope to the bridge of your nose, externally rotate so knuckles point to the ceiling.",
                recommendedSets = "3 sets x 12-15 reps"
            ),
            ExerciseViewerModel(
                id = "d5_oh_db_press",
                name = "Standing Overhead DB Press",
                targetMuscles = listOf("Front Delts", "Lateral Delts", "Triceps", "Core"),
                apparatus = "Dumbbells",
                formCue = "Brace core and glutes, press vertically, do not hyperextend the lower back.",
                recommendedSets = "4 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d5_cable_lateral_raises",
                name = "Cable Lateral Raises",
                targetMuscles = listOf("Lateral Delts"),
                apparatus = "Low Cable Pulley & D-Handle",
                formCue = "Cable runs behind your back, constant tension, stop at shoulder height.",
                recommendedSets = "5 sets x 12-15 reps"
            ),
            ExerciseViewerModel(
                id = "d5_flat_db_press",
                name = "Flat Dumbbell Press",
                targetMuscles = listOf("Chest", "Front Delts", "Triceps"),
                apparatus = "Flat Bench & Dumbbells",
                formCue = "Shoulder blades pinned, controlled descent, squeeze pecs together at the top.",
                recommendedSets = "3 sets x 8-12 reps"
            ),
            ExerciseViewerModel(
                id = "d5_oh_tricep_ext",
                name = "Overhead Cable Tricep Extension",
                targetMuscles = listOf("Triceps Long Head"),
                apparatus = "Low Cable Pulley & Rope",
                formCue = "Elbows high and stationary, stretch deep behind the head, extend fully.",
                recommendedSets = "3 sets x 10-12 reps"
            ),
            ExerciseViewerModel(
                id = "d5_dips",
                name = "Bodyweight Dips",
                targetMuscles = listOf("Triceps", "Chest", "Front Delts"),
                apparatus = "Parallel Dip Bars",
                formCue = "Stay upright to bias triceps, lower until upper arms are parallel, lock out hard.",
                recommendedSets = "3 sets to failure"
            ),
            ExerciseViewerModel(
                id = "d6_fst7_lat_pulldown",
                name = "Lat Pulldowns",
                targetMuscles = listOf("Latissimus Dorsi", "Teres Major", "Biceps"),
                apparatus = "Lat Pulldown Machine / Wide Bar",
                formCue = "FST-7: strict weight, 30-45 s rest, chase the pump while keeping full range.",
                recommendedSets = "7 sets x 14 reps"
            ),
            ExerciseViewerModel(
                id = "d6_cable_lateral_raises",
                name = "Cable Lateral Raises",
                targetMuscles = listOf("Lateral Delts"),
                apparatus = "Low Cable Pulley & D-Handle",
                formCue = "Continuous tension on the side delts, lead with the elbow for maximum width.",
                recommendedSets = "5 sets x 12 reps"
            ),
            ExerciseViewerModel(
                id = "d7_active_recovery_walk",
                name = "Active Recovery Walking",
                targetMuscles = listOf("Calves", "Glutes", "Core"),
                apparatus = "None",
                formCue = "Easy conversational pace, tall posture, natural arm swing. Hydrate and foam roll after.",
                recommendedSets = "5,000 - 10,000 steps"
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
        val found = ALL_EXERCISES.find { it.id.equals(nameOrId, ignoreCase = true) }
            ?: ALL_EXERCISES.find { it.name.equals(nameOrId, ignoreCase = true) }
            ?: return
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
