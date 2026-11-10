package com.hostelgains.domain.workout

/**
 * Anatomical muscle groups targeted in the V-Taper PPL framework.
 */
enum class MuscleGroup {
    CHEST,
    BACK,
    LATS,
    REAR_DELTS,
    LATERAL_DELTS,
    FRONT_DELTS,
    BICEPS,
    TRICEPS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE,
    ACTIVE_RECOVERY
}

/**
 * Functional movement categories for training optimization.
 */
enum class ExerciseCategory {
    COMPOUND_PUSH,
    COMPOUND_PULL,
    COMPOUND_LEGS,
    ISOLATION_DELTS,
    ISOLATION_ARMS,
    ISOLATION_LEGS,
    CORE,
    ACTIVE_RECOVERY
}

/**
 * Model representing a physical exercise in the V-Taper framework.
 */
data class Exercise(
    val id: String,
    val name: String,
    val targetMuscle: MuscleGroup,
    val category: ExerciseCategory,
    val baselineSets: Int,
    val repRange: String,
    val restSeconds: Int,
    val formNotes: String,
    val isCompensatoryEligibleCarb: Boolean = false
)

/**
 * 7-Day V-Taper PPL program daily workout plan.
 */
data class WorkoutDayPlan(
    val dayNumber: Int,
    val dayName: String,
    val focusArea: String,
    val exercises: List<Exercise>,
    val isActiveRest: Boolean = false,
    val guidanceNotes: String = ""
)

/**
 * Exercise modified dynamically by the Compensatory Workout Engine based on nutrition.
 */
data class AdjustedExercise(
    val baseExercise: Exercise,
    val effectiveSets: Int,
    val effectiveReps: String,
    val isCarbBonusApplied: Boolean,
    val isProteinCapApplied: Boolean,
    val adjustmentNotes: List<String>
)

/**
 * Full daily workout plan reflecting all compensatory rules and volume adjustments.
 */
data class AdjustedWorkoutPlan(
    val dayNumber: Int,
    val dayName: String,
    val focusArea: String,
    val exercises: List<AdjustedExercise>,
    val activeTriggers: List<String>
)
