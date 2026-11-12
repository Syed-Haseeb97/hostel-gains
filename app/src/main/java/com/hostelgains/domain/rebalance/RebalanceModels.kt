package com.hostelgains.domain.rebalance

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.workout.AdjustedWorkoutPlan
import com.hostelgains.domain.workout.Exercise
import com.hostelgains.domain.workout.WorkoutDayPlan

/**
 * Volume compensation directed to a specific synergistic exercise.
 */
data class ExerciseCompensation(
    val exerciseId: String,
    val extraSets: Int,
    val repAdjustment: String,
    val rationale: String
)

/**
 * Result of dynamic exercise rebalancing following equipment unavailability or user skip.
 */
data class RebalanceResult(
    val skippedExerciseId: String,
    val reason: String,
    val compensations: List<ExerciseCompensation>,
    val coachMessage: String,
    val isAiGenerated: Boolean
)

/**
 * Result of customizing today's workout plan (swapping, adding, removing, or modifying exercises).
 */
data class CustomizedWorkoutResult(
    val customizedExercises: List<Exercise>,
    val coachMessage: String,
    val reason: String = "",
    val isAiGenerated: Boolean = false
)

/**
 * Dynamic Exercise Rebalancer Subsystem contract.
 * Redistributes volume from skipped or unavailable exercises and harmonizes
 * rebalanced volume with nutrition-driven compensatory triggers.
 */
interface WorkoutRebalancer {
    /**
     * Rebalances a daily workout plan when an exercise is skipped or unavailable.
     *
     * @param dayPlan Baseline workout plan for the day.
     * @param skippedExerciseId Identifier or name of the skipped exercise.
     * @param userPrompt Natural language description of reason/circumstance (e.g. "No Lat Pulldown machine today").
     */
    suspend fun rebalanceWorkout(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String = ""
    ): Result<RebalanceResult>

    /**
     * Harmonizes rebalanced exercise volume with macronutrient compensatory triggers.
     * High-Carb adds sets to eligible movements, while the Protein-Deficit safety cap (max 3 sets)
     * strictly supersedes rebalanced volume to prevent catabolic overtraining.
     */
    fun harmonizeWithMacros(
        rebalancedPlan: WorkoutDayPlan,
        rebalanceResult: RebalanceResult?,
        dailyMacros: DailyMacroSummary
    ): AdjustedWorkoutPlan

    /**
     * Dynamically customizes, swaps, adds, removes, or modifies exercises in [dayPlan]
     * based on user's natural language request (e.g. "swap squats for leg press", "add 3 sets of curls", "dumbbell only today").
     */
    suspend fun customizeWorkout(
        dayPlan: WorkoutDayPlan,
        userPrompt: String
    ): Result<CustomizedWorkoutResult> {
        return Result.failure(UnsupportedOperationException("Workout customization not implemented"))
    }
}
