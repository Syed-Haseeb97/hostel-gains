package com.hostelgains.domain.rebalance

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.workout.AdjustedExercise
import com.hostelgains.domain.workout.AdjustedWorkoutPlan
import com.hostelgains.domain.workout.Exercise
import com.hostelgains.domain.workout.ExerciseCategory
import com.hostelgains.domain.workout.MuscleGroup
import com.hostelgains.domain.workout.WorkoutDayPlan
import kotlin.math.min

/**
 * Deterministic offline implementation of WorkoutRebalancer.
 * Uses an anatomical synergy redistribution matrix across all 6 PPL training days,
 * ensuring seamless offline fallback when network or AI API keys are unavailable.
 */
class OfflineWorkoutRebalancer : WorkoutRebalancer {

    companion object {
        const val DEFAULT_COMPENSATION_SETS = 2
        const val CARB_SURPLUS_THRESHOLD_GRAMS = 60.0f
        const val PROTEIN_DEFICIT_THRESHOLD_GRAMS = 20.0f
        const val PROTEIN_DEFICIT_MAX_SETS = 3
    }

    override suspend fun rebalanceWorkout(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String
    ): Result<RebalanceResult> {
        return try {
            val resolvedSkipped = resolveSkippedExercise(dayPlan, skippedExerciseId, userPrompt)
            val reason = extractReason(userPrompt, resolvedSkipped?.name ?: skippedExerciseId)

            if (dayPlan.isActiveRest || dayPlan.exercises.isEmpty()) {
                return Result.success(
                    RebalanceResult(
                        skippedExerciseId = resolvedSkipped?.id ?: skippedExerciseId,
                        reason = reason,
                        compensations = emptyList(),
                        coachMessage = "Day ${dayPlan.dayNumber} is Active Recovery: walking protocol only.",
                        isAiGenerated = false
                    )
                )
            }

            val skipped = resolvedSkipped ?: dayPlan.exercises.firstOrNull()
            ?: return Result.failure(IllegalStateException("No exercises found in day plan to rebalance."))

            val compensations = determineCompensations(dayPlan, skipped)
            val targetNames = compensations.mapNotNull { comp ->
                dayPlan.exercises.find { it.id == comp.exerciseId }?.name ?: comp.exerciseId
            }.joinToString(", ")

            val coachMessage = if (compensations.isNotEmpty()) {
                "Swapped out ${skipped.name}. Added ${compensations.sumOf { it.extraSets }} sets to $targetNames to maintain weekly V-taper volume!"
            } else {
                "Noted skip for ${skipped.name}. Baseline volume preserved for remaining exercises."
            }

            Result.success(
                RebalanceResult(
                    skippedExerciseId = skipped.id,
                    reason = reason,
                    compensations = compensations,
                    coachMessage = coachMessage,
                    isAiGenerated = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun harmonizeWithMacros(
        rebalancedPlan: WorkoutDayPlan,
        rebalanceResult: RebalanceResult?,
        dailyMacros: DailyMacroSummary
    ): AdjustedWorkoutPlan {
        if (rebalancedPlan.isActiveRest) {
            return AdjustedWorkoutPlan(
                dayNumber = rebalancedPlan.dayNumber,
                dayName = rebalancedPlan.dayName,
                focusArea = rebalancedPlan.focusArea,
                exercises = rebalancedPlan.exercises.map {
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
        if (rebalanceResult != null) {
            activeTriggers.add("AI Rebalanced: ${rebalanceResult.coachMessage}")
        }
        if (isCarbTriggerActive) {
            activeTriggers.add("High Carbohydrate Surplus (Added +1 set to Lat Pulldowns & Squats)")
        }
        if (isProteinDeficitActive) {
            activeTriggers.add("Protein Deficit Ceiling (Capped all exercises at max 3 sets for recovery)")
        }

        val compensationsMap = rebalanceResult?.compensations?.associateBy { it.exerciseId } ?: emptyMap()
        val skippedId = rebalanceResult?.skippedExerciseId

        val adjustedExercises = rebalancedPlan.exercises.map { exercise ->
            val isSkipped = exercise.id == skippedId

            if (isSkipped) {
                AdjustedExercise(
                    baseExercise = exercise,
                    effectiveSets = 0,
                    effectiveReps = "0 (Skipped)",
                    isCarbBonusApplied = false,
                    isProteinCapApplied = false,
                    adjustmentNotes = listOf("Skipped: ${rebalanceResult.reason}")
                )
            } else {
                val compensation = compensationsMap[exercise.id]
                val extraSets = compensation?.extraSets ?: 0

                val isCarbTarget = exercise.isCompensatoryEligibleCarb ||
                        exercise.name.contains("Lat Pulldown", ignoreCase = true) ||
                        exercise.name.contains("Squat", ignoreCase = true)
                val carbBonus = if (isCarbTriggerActive && isCarbTarget) 1 else 0

                val tentativeSets = exercise.baselineSets + extraSets + carbBonus

                val effectiveSets: Int
                val isProteinCapApplied: Boolean

                if (isProteinDeficitActive) {
                    effectiveSets = min(PROTEIN_DEFICIT_MAX_SETS, tentativeSets)
                    isProteinCapApplied = tentativeSets > PROTEIN_DEFICIT_MAX_SETS ||
                            (exercise.baselineSets + extraSets) > PROTEIN_DEFICIT_MAX_SETS
                } else {
                    effectiveSets = tentativeSets
                    isProteinCapApplied = false
                }

                val isCarbBonusApplied = carbBonus > 0

                val notes = mutableListOf<String>()
                if (compensation != null) {
                    notes.add("+${compensation.extraSets} sets rebalanced (${compensation.rationale})")
                }
                if (isCarbBonusApplied) {
                    notes.add("+1 set (Carb Surplus: ${dailyMacros.totalCarbs}g carbs)")
                }
                if (isProteinCapApplied) {
                    notes.add("Capped at $PROTEIN_DEFICIT_MAX_SETS sets (Protein Deficit: ${dailyMacros.totalProtein}g protein)")
                }
                if (notes.isEmpty()) {
                    notes.add("Baseline sets maintained")
                }

                val repRange = compensation?.repAdjustment?.ifBlank { exercise.repRange } ?: exercise.repRange

                AdjustedExercise(
                    baseExercise = exercise,
                    effectiveSets = effectiveSets,
                    effectiveReps = repRange,
                    isCarbBonusApplied = isCarbBonusApplied,
                    isProteinCapApplied = isProteinCapApplied,
                    adjustmentNotes = notes
                )
            }
        }

        return AdjustedWorkoutPlan(
            dayNumber = rebalancedPlan.dayNumber,
            dayName = rebalancedPlan.dayName,
            focusArea = rebalancedPlan.focusArea,
            exercises = adjustedExercises,
            activeTriggers = activeTriggers
        )
    }

    private fun resolveSkippedExercise(
        dayPlan: WorkoutDayPlan,
        skippedExerciseId: String,
        userPrompt: String
    ): Exercise? {
        val trimmedId = skippedExerciseId.trim()
        if (trimmedId.isNotEmpty()) {
            val byId = dayPlan.exercises.find { it.id.equals(trimmedId, ignoreCase = true) }
            if (byId != null) return byId

            val byName = dayPlan.exercises.find { it.name.contains(trimmedId, ignoreCase = true) }
            if (byName != null) return byName
        }

        val promptLower = userPrompt.lowercase()
        if (promptLower.isEmpty()) return dayPlan.exercises.firstOrNull()

        // Match against exercises in current day plan by keyword
        return dayPlan.exercises.find { ex ->
            val nameLower = ex.name.lowercase()
            val idLower = ex.id.lowercase()
            promptLower.contains(nameLower) ||
                    promptLower.contains(idLower) ||
                    matchExerciseKeywords(promptLower, ex)
        } ?: dayPlan.exercises.firstOrNull()
    }

    private fun matchExerciseKeywords(prompt: String, ex: Exercise): Boolean {
        val name = ex.name.lowercase()
        return when {
            name.contains("lat pulldown") && prompt.contains("lat pulldown") -> true
            name.contains("pulldown") && prompt.contains("pulldown") -> true
            name.contains("row") && prompt.contains("row") -> true
            name.contains("squat") && prompt.contains("squat") -> true
            name.contains("incline") && (prompt.contains("incline") || prompt.contains("db press")) -> true
            name.contains("lateral raise") && (prompt.contains("lateral") || prompt.contains("side delt")) -> true
            name.contains("push-up") && prompt.contains("push") -> true
            name.contains("pushdown") && prompt.contains("pushdown") -> true
            name.contains("curl") && (prompt.contains("curl") || prompt.contains("bicep")) -> true
            name.contains("plank") && prompt.contains("plank") -> true
            name.contains("leg curl") && (prompt.contains("leg curl") || prompt.contains("hamstring")) -> true
            name.contains("rear delt") && prompt.contains("rear delt") -> true
            name.contains("overhead") && (prompt.contains("overhead") || prompt.contains("ohp")) -> true
            name.contains("dip") && prompt.contains("dip") -> true
            else -> false
        }
    }

    private fun extractReason(userPrompt: String, fallbackExerciseName: String): String {
        val p = userPrompt.trim()
        if (p.isEmpty()) return "Equipment unavailable in gym"
        return when {
            p.contains("wrist", ignoreCase = true) || p.contains("hurts", ignoreCase = true) || p.contains("pain", ignoreCase = true) ->
                "Pain/joint discomfort ($p)"
            p.contains("no", ignoreCase = true) || p.contains("machine", ignoreCase = true) || p.contains("busy", ignoreCase = true) || p.contains("taken", ignoreCase = true) ->
                "Equipment unavailable ($p)"
            p.contains("skip", ignoreCase = true) || p.contains("couldn't", ignoreCase = true) ->
                "Exercise skipped ($p)"
            else -> p
        }
    }

    private fun determineCompensations(
        dayPlan: WorkoutDayPlan,
        skipped: Exercise
    ): List<ExerciseCompensation> {
        val candidates = dayPlan.exercises.filter { it.id != skipped.id }

        // Specific Synergistic Redistribution Matrix
        val match = findSynergisticPartner(skipped, candidates)
        if (match != null) {
            return listOf(
                ExerciseCompensation(
                    exerciseId = match.id,
                    extraSets = DEFAULT_COMPENSATION_SETS,
                    repAdjustment = match.repRange,
                    rationale = generateRationale(skipped, match)
                )
            )
        }

        // Generic fallback to same target muscle or same category in the day's routine
        val sameMuscle = candidates.find { it.targetMuscle == skipped.targetMuscle }
        if (sameMuscle != null) {
            return listOf(
                ExerciseCompensation(
                    exerciseId = sameMuscle.id,
                    extraSets = DEFAULT_COMPENSATION_SETS,
                    repAdjustment = sameMuscle.repRange,
                    rationale = "Redistributes ${skipped.targetMuscle.name.lowercase()} volume to retain hypertrophy stimulus."
                )
            )
        }

        val sameCategory = candidates.find { it.category == skipped.category }
        if (sameCategory != null) {
            return listOf(
                ExerciseCompensation(
                    exerciseId = sameCategory.id,
                    extraSets = DEFAULT_COMPENSATION_SETS,
                    repAdjustment = sameCategory.repRange,
                    rationale = "Maintains movement pattern volume via ${sameCategory.name}."
                )
            )
        }

        // If no direct partner, redistribute to the first compound movement
        val firstCompound = candidates.find {
            it.category == ExerciseCategory.COMPOUND_PULL ||
                    it.category == ExerciseCategory.COMPOUND_PUSH ||
                    it.category == ExerciseCategory.COMPOUND_LEGS
        } ?: candidates.firstOrNull()

        return if (firstCompound != null) {
            listOf(
                ExerciseCompensation(
                    exerciseId = firstCompound.id,
                    extraSets = DEFAULT_COMPENSATION_SETS,
                    repAdjustment = firstCompound.repRange,
                    rationale = "Redistributes lost volume to primary daily compound lift."
                )
            )
        } else {
            emptyList()
        }
    }

    private fun findSynergisticPartner(skipped: Exercise, candidates: List<Exercise>): Exercise? {
        val sName = skipped.name.lowercase()
        val sMuscle = skipped.targetMuscle

        return when {
            // Back / Lats: Lat Pulldowns -> Seated Cable Rows or Doorway/Towel Rows
            sMuscle == MuscleGroup.LATS || sName.contains("lat pulldown") -> {
                candidates.find { it.name.contains("Row", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.BACK }
            }
            sMuscle == MuscleGroup.BACK || sName.contains("row") -> {
                candidates.find { it.name.contains("Pulldown", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.LATS }
            }
            // Upper Chest / Shoulders: Incline DB Press -> Incline Push-ups
            sName.contains("incline dumbbell press") || sName.contains("incline db press") -> {
                candidates.find { it.name.contains("Incline Push-ups", ignoreCase = true) }
                    ?: candidates.find { it.category == ExerciseCategory.COMPOUND_PUSH }
            }
            sName.contains("incline push-ups") -> {
                candidates.find { it.name.contains("Incline Dumbbell Press", ignoreCase = true) }
                    ?: candidates.find { it.category == ExerciseCategory.COMPOUND_PUSH }
            }
            sMuscle == MuscleGroup.LATERAL_DELTS -> {
                candidates.find { it.name.contains("Face Pull", ignoreCase = true) }
                    ?: candidates.find { it.name.contains("Push-up", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.FRONT_DELTS }
            }
            sMuscle == MuscleGroup.REAR_DELTS -> {
                candidates.find { it.name.contains("Row", ignoreCase = true) }
                    ?: candidates.find { it.name.contains("Lateral", ignoreCase = true) }
            }
            sMuscle == MuscleGroup.TRICEPS -> {
                candidates.find { it.name.contains("Push-up", ignoreCase = true) }
                    ?: candidates.find { it.name.contains("Dips", ignoreCase = true) }
                    ?: candidates.find { it.category == ExerciseCategory.COMPOUND_PUSH }
            }
            sMuscle == MuscleGroup.BICEPS -> {
                candidates.find { it.targetMuscle == MuscleGroup.BICEPS && it.id != skipped.id }
                    ?: candidates.find { it.category == ExerciseCategory.COMPOUND_PULL }
            }
            // Legs / Quads: Goblet Squats -> Bodyweight Squats
            sName.contains("goblet squat") -> {
                candidates.find { it.name.contains("Bodyweight Squats", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.QUADS }
            }
            sName.contains("bodyweight squat") -> {
                candidates.find { it.name.contains("Goblet Squats", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.QUADS }
            }
            // Hamstrings / Core: Lying Leg Curls / Plank -> bodyweight hinges / plank extension
            sMuscle == MuscleGroup.HAMSTRINGS || sName.contains("leg curl") -> {
                candidates.find { it.name.contains("Plank", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.CORE }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.QUADS }
            }
            sMuscle == MuscleGroup.CORE || sName.contains("plank") -> {
                candidates.find { it.name.contains("Leg Curl", ignoreCase = true) }
                    ?: candidates.find { it.targetMuscle == MuscleGroup.HAMSTRINGS }
            }
            else -> null
        }
    }

    private fun generateRationale(skipped: Exercise, compensated: Exercise): String {
        val sName = skipped.name
        val cName = compensated.name
        return when {
            sName.contains("Lat Pulldown", ignoreCase = true) && cName.contains("Row", ignoreCase = true) ->
                "Redistributes lat and upper-back volume so weekly V-taper growth is maintained."
            sName.contains("Row", ignoreCase = true) && cName.contains("Pulldown", ignoreCase = true) ->
                "Transfers horizontal pulling volume to vertical lat loading."
            sName.contains("Incline", ignoreCase = true) && cName.contains("Push-up", ignoreCase = true) ->
                "Shifts clavicular pec loading to bodyweight incline angles to preserve upper-chest stimulation."
            sName.contains("Goblet Squat", ignoreCase = true) && cName.contains("Bodyweight Squat", ignoreCase = true) ->
                "Redistributes quad volume to explosive high-rep bodyweight squats."
            sName.contains("Leg Curl", ignoreCase = true) ->
                "Transfers hamstring/posterior chain load to hinge and core isometric tension."
            sName.contains("Plank", ignoreCase = true) ->
                "Compensates core anterior stability volume through posterior chain loading."
            else ->
                "Compensates ${skipped.targetMuscle.name.lowercase()} volume via ${compensated.name}."
        }
    }
}
