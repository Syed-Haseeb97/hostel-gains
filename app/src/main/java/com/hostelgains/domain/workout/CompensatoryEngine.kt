package com.hostelgains.domain.workout

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import kotlin.math.min

/**
 * Interface for the dynamic compensatory workout adjustment engine.
 */
interface CompensatoryEngine {
    fun evaluateWorkout(
        basePlan: WorkoutDayPlan,
        dailyMacros: DailyMacroSummary
    ): AdjustedWorkoutPlan

    fun evaluateWorkout(
        basePlan: WorkoutDayPlan,
        meal: MealNutrition
    ): AdjustedWorkoutPlan
}

/**
 * Genuine implementation of the Dynamic Compensatory Workout Engine.
 *
 * Rules:
 * 1. Carb Trigger: When meal/daily carbs >= 60g (or isHighCarb = true), dynamically adds 1 extra set
 *    to Lat Pulldowns and Squats to exploit glycogen surplus for hypertrophy.
 * 2. Protein Deficit Trigger: When meal/daily protein < 20g (or isLowProtein = true), automatically
 *    caps ALL exercises at a maximum of 3 sets to prevent catabolic overreaching.
 * 3. Precedence & Conflict Resolution: Protein deficit safety cap strictly acts as an absolute
 *    ceiling: effectiveSets = min(3, baselineSets + carbBonus).
 */
class DefaultCompensatoryEngine : CompensatoryEngine {

    companion object {
        const val CARB_SURPLUS_THRESHOLD_GRAMS = 60.0f
        const val PROTEIN_DEFICIT_THRESHOLD_GRAMS = 20.0f
        const val PROTEIN_DEFICIT_MAX_SETS = 3
    }

    override fun evaluateWorkout(
        basePlan: WorkoutDayPlan,
        meal: MealNutrition
    ): AdjustedWorkoutPlan {
        val summary = DailyMacroSummary.fromSingleMeal("today", meal)
        return evaluateWorkout(basePlan, summary)
    }

    override fun evaluateWorkout(
        basePlan: WorkoutDayPlan,
        dailyMacros: DailyMacroSummary
    ): AdjustedWorkoutPlan {
        // Active rest day requires no set modifications
        if (basePlan.isActiveRest) {
            return AdjustedWorkoutPlan(
                dayNumber = basePlan.dayNumber,
                dayName = basePlan.dayName,
                focusArea = basePlan.focusArea,
                exercises = basePlan.exercises.map {
                    AdjustedExercise(
                        baseExercise = it,
                        effectiveSets = it.baselineSets,
                        effectiveReps = it.repRange,
                        isCarbBonusApplied = false,
                        isProteinCapApplied = false,
                        adjustmentNotes = listOf("Active recovery protocol - steps only")
                    )
                },
                activeTriggers = emptyList()
            )
        }

        val isCarbTriggerActive = dailyMacros.hasHighCarbTrigger ||
                dailyMacros.totalCarbs >= CARB_SURPLUS_THRESHOLD_GRAMS

        val isProteinDeficitActive = dailyMacros.hasProteinDeficitTrigger ||
                dailyMacros.totalProtein < PROTEIN_DEFICIT_THRESHOLD_GRAMS

        val activeTriggers = mutableListOf<String>()
        if (isCarbTriggerActive) {
            activeTriggers.add("High Carbohydrate Surplus (Added +1 set to Lat Pulldowns & Squats)")
        }
        if (isProteinDeficitActive) {
            activeTriggers.add("Protein Deficit Ceiling (Capped all exercises at max 3 sets for recovery)")
        }

        val adjustedExercises = basePlan.exercises.map { exercise ->
            val isCarbTargetExercise = exercise.isCompensatoryEligibleCarb ||
                    exercise.name.contains("Lat Pulldown", ignoreCase = true) ||
                    exercise.name.contains("Squat", ignoreCase = true)

            val carbBonus = if (isCarbTriggerActive && isCarbTargetExercise) 1 else 0
            val tentativeSets = exercise.baselineSets + carbBonus

            val effectiveSets: Int
            val isProteinCapApplied: Boolean

            if (isProteinDeficitActive) {
                effectiveSets = min(PROTEIN_DEFICIT_MAX_SETS, tentativeSets)
                // Cap is considered applied if tentative sets exceeded max sets (3)
                // or if the exercise baseline exceeded max sets
                isProteinCapApplied = tentativeSets > PROTEIN_DEFICIT_MAX_SETS || exercise.baselineSets > PROTEIN_DEFICIT_MAX_SETS
            } else {
                effectiveSets = tentativeSets
                isProteinCapApplied = false
            }

            val isCarbBonusApplied = carbBonus > 0

            val notes = mutableListOf<String>()
            if (isCarbBonusApplied) {
                notes.add("+1 set (Carb Surplus: ${dailyMacros.totalCarbs}g carbs)")
            }
            if (isProteinCapApplied) {
                notes.add("Capped at $PROTEIN_DEFICIT_MAX_SETS sets (Protein Deficit: ${dailyMacros.totalProtein}g protein)")
            }
            if (notes.isEmpty()) {
                notes.add("Baseline sets maintained")
            }

            AdjustedExercise(
                baseExercise = exercise,
                effectiveSets = effectiveSets,
                effectiveReps = exercise.repRange,
                isCarbBonusApplied = isCarbBonusApplied,
                isProteinCapApplied = isProteinCapApplied,
                adjustmentNotes = notes
            )
        }

        return AdjustedWorkoutPlan(
            dayNumber = basePlan.dayNumber,
            dayName = basePlan.dayName,
            focusArea = basePlan.focusArea,
            exercises = adjustedExercises,
            activeTriggers = activeTriggers
        )
    }
}
