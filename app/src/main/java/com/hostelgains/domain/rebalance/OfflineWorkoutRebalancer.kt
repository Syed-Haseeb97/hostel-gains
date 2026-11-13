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

    override suspend fun customizeWorkout(
        dayPlan: WorkoutDayPlan,
        userPrompt: String
    ): Result<CustomizedWorkoutResult> {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Workout request cannot be empty"))
        }

        val lower = trimmed.lowercase()
        val currentExercises = dayPlan.exercises.toMutableList()

        // 1. Dumbbell Only / No Barbell
        if (lower.contains("dumbbell only") || lower.contains("only dumbbell") || lower.contains("no barbell") || lower.contains("only db")) {
            val converted = currentExercises.map { ex ->
                when {
                    ex.name.contains("Barbell Deadlift", ignoreCase = true) || ex.name.contains("Deadlift", ignoreCase = true) ->
                        ex.copy(name = "Dumbbell Romanian Deadlifts", formNotes = "Hips back, flat spine, dumbbell travel along shins.")
                    ex.name.contains("Barbell Bench", ignoreCase = true) || (ex.name.contains("Bench", ignoreCase = true) && !ex.name.contains("Dumbbell", ignoreCase = true)) ->
                        ex.copy(name = "Flat Dumbbell Press", formNotes = "45° elbow tuck, full stretch at chest, press together.")
                    ex.name.contains("Barbell Row", ignoreCase = true) || ex.name.contains("T-Bar", ignoreCase = true) ->
                        ex.copy(name = "Single-Arm Dumbbell Rows", formNotes = "Chest braced, pull dumbbell to hip pocket.")
                    (ex.name.contains("Barbell Squat", ignoreCase = true) || ex.name.contains("Squat", ignoreCase = true)) && !ex.name.contains("Dumbbell", ignoreCase = true) && !ex.name.contains("Goblet", ignoreCase = true) ->
                        ex.copy(name = "Dumbbell Goblet Squats", formNotes = "Hold dumbbell vertically at chest, full depth.")
                    ex.name.contains("Barbell Curl", ignoreCase = true) ->
                        ex.copy(name = "Dumbbell Incline Curls", formNotes = "Supinate wrists at top, controlled 2-sec eccentric.")
                    ex.name.contains("Military Press", ignoreCase = true) || ex.name.contains("Overhead Press", ignoreCase = true) ->
                        ex.copy(name = "Seated Dumbbell Shoulder Press", formNotes = "Neutral-to-pronated grip, full extension.")
                    else -> ex
                }
            }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = converted,
                    coachMessage = "Converted today's routine to 100% Dumbbell-only. No barbells or crowded racks required!",
                    reason = "Dumbbell-only conversion",
                    isAiGenerated = false
                )
            )
        }

        // 2. Shorten workout / Quick session
        if (lower.contains("shorten") || lower.contains("quick") || lower.contains("less time") || lower.contains("20 min") || lower.contains("30 min")) {
            val shortened = currentExercises.take(3).map { it.copy(baselineSets = 3) }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = shortened,
                    coachMessage = "Condensed workout to your top 3 primary compound movements. Rest 60s between sets to finish in under 30 minutes!",
                    reason = "Express workout (shortened)",
                    isAiGenerated = false
                )
            )
        }

        // 3. Deload / Light Day
        if (lower.contains("light day") || lower.contains("deload") || lower.contains("easy day") || lower.contains("feeling tired")) {
            val deloaded = currentExercises.map { it.copy(baselineSets = 2, repRange = "12-15 reps") }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = deloaded,
                    coachMessage = "Deload active: reduced working volume to 2 sets of 12-15 reps per exercise to prioritize joint recovery.",
                    reason = "Deload / Light session",
                    isAiGenerated = false
                )
            )
        }

        // 4. Heavy Day / More Volume
        if (lower.contains("heavy day") || lower.contains("more volume") || lower.contains("harder") || lower.contains("extra sets")) {
            val overloaded = currentExercises.map { it.copy(baselineSets = it.baselineSets + 1) }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = overloaded,
                    coachMessage = "Volume boost applied! Added +1 heavy set to every movement today. Stay hydrated and push near failure.",
                    reason = "High-volume overload",
                    isAiGenerated = false
                )
            )
        }

        // 4b. Too Heavy / Lighter Weight / Dumbbells too heavy
        if (lower.contains("too heavy") || lower.contains("heavy") || lower.contains("lighter") || lower.contains("reduce weight")) {
            val adjusted = currentExercises.map { ex ->
                if (ex.name.contains("Dumbbell", ignoreCase = true) || ex.name.contains("DB", ignoreCase = true)) {
                    ex.copy(
                        repRange = "12-15 reps (Light weight / form focus)",
                        formNotes = "Reduced working load: prioritize controlled tempo and smooth contractions over heavy weight."
                    )
                } else if (ex.baselineSets > 3) {
                    ex.copy(baselineSets = ex.baselineSets - 1)
                } else {
                    ex
                }
            }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = adjusted,
                    coachMessage = "Adjusted workout for lighter load. Reduced dumbbell working weights and rep targets so you can focus on clean form without straining.",
                    reason = "Lighter working load applied",
                    isAiGenerated = false
                )
            )
        }

        // 5. Swap / Replace exercise ("swap X for Y", "replace X with Y", "use Y instead of X")
        val isSwapRequest = lower.contains("swap") || lower.contains("replace") || lower.contains("instead of")
        if (isSwapRequest) {
            val targetExercise = currentExercises.find { ex ->
                val exName = ex.name.lowercase()
                when {
                    exName.contains("squat") && lower.contains("squat") -> true
                    exName.contains("lat pulldown") && (lower.contains("lat pulldown") || lower.contains("pulldown")) -> true
                    exName.contains("bench") && lower.contains("bench") -> true
                    exName.contains("incline") && lower.contains("incline") -> true
                    exName.contains("curl") && lower.contains("curl") -> true
                    exName.contains("row") && lower.contains("row") -> true
                    exName.contains("lateral") && lower.contains("lateral") -> true
                    exName.contains("deadlift") && lower.contains("deadlift") -> true
                    exName.contains("press") && lower.contains("press") -> true
                    else -> lower.contains(exName)
                }
            }

            if (targetExercise != null) {
                val replacement = when {
                    lower.contains("leg press") ->
                        Exercise("custom_leg_press", "Leg Press Machine", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, targetExercise.baselineSets, "10-12 reps", 90, "Feet shoulder-width, full depth, drive through midfoot.")
                    lower.contains("pull up") || lower.contains("pullup") || lower.contains("chin up") ->
                        Exercise("custom_pullups", "Bodyweight / Weighted Pull-ups", MuscleGroup.LATS, ExerciseCategory.COMPOUND_PULL, targetExercise.baselineSets, "To failure", 90, "Dead hang start, chest to bar, drive elbows down.")
                    lower.contains("db press") || lower.contains("dumbbell press") ->
                        Exercise("custom_db_press", "Dumbbell Press", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, targetExercise.baselineSets, "8-12 reps", 90, "Tuck elbows 45°, deep pec stretch, press upward.")
                    lower.contains("db row") || lower.contains("dumbbell row") ->
                        Exercise("custom_db_rows", "Single-Arm Dumbbell Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, targetExercise.baselineSets, "8-12 reps", 60, "Brace torso on bench, pull dumbbell to hip pocket.")
                    lower.contains("push up") || lower.contains("pushup") ->
                        Exercise("custom_pushups", "Incline / Standard Push-ups", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, targetExercise.baselineSets, "To failure", 60, "Brace core, chest to floor or bench, explosive press.")
                    lower.contains("hack squat") ->
                        Exercise("custom_hack_squat", "Hack Squat / V-Squat", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, targetExercise.baselineSets, "8-12 reps", 90, "Back flat against pad, controlled descent, explode up.")
                    lower.contains("bulgarian") || lower.contains("split squat") ->
                        Exercise("custom_split_squat", "Bulgarian Split Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, targetExercise.baselineSets, "10-12 reps", 90, "Rear foot on bench, sink hips deep, drive through front heel.")
                    lower.contains("hammer curl") ->
                        Exercise("custom_hammer_curl", "Dumbbell Hammer Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, targetExercise.baselineSets, "10-12 reps", 60, "Neutral grip, elbows locked at sides.")
                    lower.contains("lateral raise") ->
                        Exercise("custom_lat_raise", "Dumbbell Lateral Raises", MuscleGroup.LATERAL_DELTS, ExerciseCategory.ISOLATION_DELTS, targetExercise.baselineSets, "12-15 reps", 60, "Lead with elbows, raise to parallel.")
                    lower.contains("cable fly") || lower.contains("pec fly") ->
                        Exercise("custom_cable_fly", "Standing Cable Chest Flyes", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, targetExercise.baselineSets, "12-15 reps", 60, "Slight elbow bend, bring hands together in arc.")
                    else -> {
                        val candidateName = Regex("(?:for|with|to)\\s+([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)?.trim()
                            ?: "Alternative Movement"
                        val formattedName = candidateName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        Exercise("custom_${candidateName.lowercase().replace(" ", "_")}", formattedName, targetExercise.targetMuscle, targetExercise.category, targetExercise.baselineSets, targetExercise.repRange, targetExercise.restSeconds, "Customized substitution per user request.")
                    }
                }

                val index = currentExercises.indexOfFirst { it.id == targetExercise.id }
                if (index != -1) {
                    currentExercises[index] = replacement
                } else {
                    currentExercises.add(replacement)
                }

                return Result.success(
                    CustomizedWorkoutResult(
                        customizedExercises = currentExercises,
                        coachMessage = "Swapped out ${targetExercise.name} for ${replacement.name}. Volume and intensity preserved for today's workout!",
                        reason = "Swapped ${targetExercise.name} -> ${replacement.name}",
                        isAiGenerated = false
                    )
                )
            }
        }

        // 6. Add exercise ("add 3 sets of hammer curls", "add dips", "more bicep work")
        val isAddRequest = lower.contains("add") || lower.contains("include") || lower.contains("more ")
        if (isAddRequest) {
            val sets = Regex("(\\d+)\\s*sets?", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            val newEx = when {
                lower.contains("hammer curl") || lower.contains("bicep curl") || lower.contains("curl") ->
                    Exercise("custom_add_curls", "Incline Dumbbell Bicep Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, sets, "10-12 reps", 60, "Pin elbows back, maximize bicep peak contraction.")
                lower.contains("dip") || lower.contains("dips") ->
                    Exercise("custom_add_dips", "Parallel Bar Dips", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, sets, "8-12 reps", 75, "Lean torso forward 30° to target pecs and triceps.")
                lower.contains("calf") || lower.contains("calves") ->
                    Exercise("custom_add_calves", "Standing Dumbbell Calf Raises", MuscleGroup.CALVES, ExerciseCategory.ISOLATION_LEGS, sets, "15-20 reps", 45, "Full stretch at bottom, hold 1-sec peak at top.")
                lower.contains("tricep") || lower.contains("pushdown") ->
                    Exercise("custom_add_triceps", "Rope Tricep Pushdowns", MuscleGroup.TRICEPS, ExerciseCategory.ISOLATION_ARMS, sets, "10-12 reps", 60, "Flare rope at bottom, squeeze horseshoe triceps.")
                lower.contains("abs") || lower.contains("core") || lower.contains("leg raise") ->
                    Exercise("custom_add_abs", "Hanging Knee / Leg Raises", MuscleGroup.CORE, ExerciseCategory.CORE, sets, "12-15 reps", 60, "Roll pelvis upward, avoid swinging.")
                lower.contains("face pull") ->
                    Exercise("custom_add_facepull", "Cable Face Pulls", MuscleGroup.REAR_DELTS, ExerciseCategory.ISOLATION_DELTS, sets, "15 reps", 60, "Pull rope to forehead, rotate hands back.")
                else -> {
                    val customName = Regex("add\\s+(?:\\d+\\s+sets?\\s+(?:of\\s+)?)?([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.get(1)?.trim()
                        ?: "Custom Movement"
                    val formattedCustom = customName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    Exercise("custom_add_${customName.lowercase().replace(" ", "_")}", formattedCustom, MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, sets, "10-12 reps", 60, "Custom exercise added per user request.")
                }
            }
            currentExercises.add(newEx)
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = currentExercises,
                    coachMessage = "Added ${newEx.name} ($sets sets) to today's routine. Make sure to log completion!",
                    reason = "Added ${newEx.name}",
                    isAiGenerated = false
                )
            )
        }

        // 7. Remove / Skip exercise ("remove cable rows", "take out crunches", "drop squats")
        val isRemoveRequest = lower.contains("remove") || lower.contains("take out") || lower.contains("drop") || lower.contains("skip")
        if (isRemoveRequest) {
            val toRemove = currentExercises.find { ex ->
                val exName = ex.name.lowercase()
                lower.contains(exName) || (exName.contains("squat") && lower.contains("squat")) ||
                        (exName.contains("row") && lower.contains("row")) ||
                        (exName.contains("pulldown") && lower.contains("pulldown")) ||
                        (exName.contains("curl") && lower.contains("curl")) ||
                        (exName.contains("press") && lower.contains("press"))
            }
            if (toRemove != null) {
                currentExercises.remove(toRemove)
                return Result.success(
                    CustomizedWorkoutResult(
                        customizedExercises = currentExercises,
                        coachMessage = "Removed ${toRemove.name} from today's plan per your request.",
                        reason = "Removed ${toRemove.name}",
                        isAiGenerated = false
                    )
                )
            }
        }

        // 8. Joint pain fallback (e.g. "wrist hurts", "shoulder pain")
        if (lower.contains("wrist") || lower.contains("pain") || lower.contains("hurt") || lower.contains("shoulder")) {
            val replaced = currentExercises.map { ex ->
                if (ex.name.contains("Press", ignoreCase = true) || ex.name.contains("Bench", ignoreCase = true)) {
                    Exercise("custom_pushups_safe", "Incline Push-ups (Joint Friendly)", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, ex.baselineSets, "To failure", 60, "Tuck elbows, neutral hand placement to relieve joint stress.")
                } else if (ex.name.contains("Barbell Curl", ignoreCase = true)) {
                    Exercise("custom_hammer_curl_safe", "Dumbbell Hammer Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, ex.baselineSets, "10-12 reps", 60, "Neutral grip prevents forearm and wrist strain.")
                } else {
                    ex
                }
            }
            return Result.success(
                CustomizedWorkoutResult(
                    customizedExercises = replaced,
                    coachMessage = "Substituted joint-stressing movements with biomechanically friendly alternatives to protect your recovery.",
                    reason = "Joint-friendly modifications",
                    isAiGenerated = false
                )
            )
        }

        // Default fallback
        val rebalanceRes = rebalanceWorkout(dayPlan, dayPlan.exercises.firstOrNull()?.id ?: "", trimmed)
        val coachMsg = rebalanceRes.getOrNull()?.coachMessage ?: "Workout adjusted to match your request."
        return Result.success(
            CustomizedWorkoutResult(
                customizedExercises = currentExercises,
                coachMessage = coachMsg,
                reason = trimmed,
                isAiGenerated = false
            )
        )
    }
}
