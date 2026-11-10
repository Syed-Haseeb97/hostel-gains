package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.hostelgains.data.nutrition.MealRepository
import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.workout.AdjustedExercise
import com.hostelgains.domain.workout.AdjustedWorkoutPlan
import com.hostelgains.domain.workout.CompensatoryEngine
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.Exercise
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import com.hostelgains.domain.workout.WorkoutDayPlan
import com.hostelgains.ui.components.CompensationItemUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

data class RebalanceState(
    val isRebalanced: Boolean = false,
    val skippedExerciseId: String? = null,
    val skippedExerciseName: String? = null,
    val reason: String = "",
    val coachMessage: String = "",
    val compensations: List<CompensationItemUi> = emptyList(),
    val isCardExpanded: Boolean = true
)

data class WorkoutUiState(
    val selectedDayIndex: Int = 0, // 0..6 for Day 1..Day 7
    val basePlan: WorkoutDayPlan,
    val adjustedPlan: AdjustedWorkoutPlan,
    val completedExerciseIds: Set<String> = emptySet(),
    val rebalanceState: RebalanceState = RebalanceState(),
    val dailyMacros: DailyMacroSummary
)

class WorkoutViewModel(
    private val workoutRepository: VTaperWorkoutRepository = VTaperWorkoutRepository(),
    private val compensatoryEngine: CompensatoryEngine = DefaultCompensatoryEngine(),
    private val mealRepository: MealRepository = AppRepositories.mealRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: CoroutineScope(Dispatchers.Default)

    private var _selectedDayIndex: Int = 0
    private var _completedExercises: Set<String> = emptySet()
    private var _rebalanceState: RebalanceState = RebalanceState()

    private val _uiState = MutableStateFlow(buildInitialUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private fun buildInitialUiState(): WorkoutUiState {
        val base = workoutRepository.getDayPlan(1)
        val initialSummary = mealRepository.getDailySummary().value
        val adj = computeAdjustedPlan(base, initialSummary, _rebalanceState)
        return WorkoutUiState(
            selectedDayIndex = 0,
            basePlan = base,
            adjustedPlan = adj,
            completedExerciseIds = emptySet(),
            rebalanceState = _rebalanceState,
            dailyMacros = initialSummary
        )
    }

    init {
        updateState()
        scope.launch {
            mealRepository.getDailySummary().collect { updatedSummary ->
                updateState(summary = updatedSummary)
            }
        }
    }

    private fun updateState(
        dayIndex: Int = _selectedDayIndex,
        completed: Set<String> = _completedExercises,
        rebalance: RebalanceState = _rebalanceState,
        summary: DailyMacroSummary = mealRepository.getDailySummary().value
    ) {
        _selectedDayIndex = dayIndex
        _completedExercises = completed
        _rebalanceState = rebalance
        val basePlan = workoutRepository.getDayPlan(dayIndex + 1)
        val adjusted = computeAdjustedPlan(basePlan, summary, rebalance)
        _uiState.value = WorkoutUiState(
            selectedDayIndex = dayIndex,
            basePlan = basePlan,
            adjustedPlan = adjusted,
            completedExerciseIds = completed,
            rebalanceState = rebalance,
            dailyMacros = summary
        )
    }

    fun selectDay(dayIndex: Int) {
        if (dayIndex in 0..6) {
            var rebalance = _rebalanceState
            if (rebalance.isRebalanced) {
                val currentDayPlan = workoutRepository.getDayPlan(dayIndex + 1)
                if (currentDayPlan.exercises.none { it.id == rebalance.skippedExerciseId }) {
                    rebalance = RebalanceState()
                }
            }
            updateState(dayIndex = dayIndex, rebalance = rebalance)
        }
    }

    fun toggleExerciseCompleted(exerciseId: String) {
        val current = _completedExercises
        val updated = if (current.contains(exerciseId)) {
            current - exerciseId
        } else {
            current + exerciseId
        }
        updateState(completed = updated)
    }

    fun toggleCoachCardExpanded() {
        val cur = _rebalanceState
        updateState(rebalance = cur.copy(isCardExpanded = !cur.isCardExpanded))
    }

    fun skipExercise(exerciseId: String, reason: String = "Equipment unavailable / skipped") {
        val dayPlan = workoutRepository.getDayPlan(_selectedDayIndex + 1)
        val targetExercise = dayPlan.exercises.find { it.id == exerciseId } ?: return

        val compensationResult = computeDeterministicCompensation(dayPlan, targetExercise, reason)
        val newRebalance = RebalanceState(
            isRebalanced = true,
            skippedExerciseId = exerciseId,
            skippedExerciseName = targetExercise.name,
            reason = reason,
            coachMessage = compensationResult.coachMessage,
            compensations = compensationResult.compensations,
            isCardExpanded = true
        )
        updateState(rebalance = newRebalance)
    }

    fun rebalanceFromPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val dayPlan = workoutRepository.getDayPlan(_selectedDayIndex + 1)
        // Find which exercise in today's plan matches the prompt
        val matchedExercise = dayPlan.exercises.firstOrNull { ex ->
            val lowerPrompt = trimmed.lowercase()
            val lowerName = ex.name.lowercase()
            when {
                lowerName.contains("lat pulldown") && (lowerPrompt.contains("lat pulldown") || lowerPrompt.contains("pulldown") || lowerPrompt.contains("machine")) -> true
                lowerName.contains("incline dumbbell press") && (lowerPrompt.contains("incline db") || lowerPrompt.contains("incline press") || lowerPrompt.contains("wrist") || lowerPrompt.contains("press")) -> true
                lowerName.contains("squat") && (lowerPrompt.contains("squat") || lowerPrompt.contains("leg")) -> true
                lowerName.contains("curl") && lowerPrompt.contains("curl") -> true
                lowerName.contains("row") && lowerPrompt.contains("row") -> true
                lowerName.contains("lateral raise") && lowerPrompt.contains("lateral") -> true
                else -> lowerPrompt.contains(lowerName)
            }
        } ?: dayPlan.exercises.firstOrNull() ?: return

        val reason = when {
            trimmed.contains("wrist", ignoreCase = true) || trimmed.contains("hurt", ignoreCase = true) || trimmed.contains("pain", ignoreCase = true) -> "Joint/wrist strain reported"
            trimmed.contains("machine", ignoreCase = true) || trimmed.contains("busy", ignoreCase = true) || trimmed.contains("occupied", ignoreCase = true) -> "Equipment unavailable in gym"
            else -> trimmed
        }

        skipExercise(matchedExercise.id, reason)
    }

    fun resetRebalance() {
        updateState(rebalance = RebalanceState(isRebalanced = false))
    }

    /**
     * Deterministic synergistic fallback compensation matrix.
     */
    private fun computeDeterministicCompensation(
        dayPlan: WorkoutDayPlan,
        skippedEx: Exercise,
        reason: String
    ): CompensationOutcome {
        val compensations = mutableListOf<CompensationItemUi>()
        val coachMsg: String

        when {
            // Back / Lat Pulldown -> Seated Cable Rows or other Row
            skippedEx.name.contains("Lat Pulldown", ignoreCase = true) -> {
                val rowEx = dayPlan.exercises.find { it.name.contains("Row", ignoreCase = true) }
                    ?: dayPlan.exercises.find { it.id != skippedEx.id }
                val targetName = rowEx?.name ?: "Doorway / Towel Rows"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "8-12 reps",
                        rationale = "Redistributes lat and upper-back volume so weekly V-taper growth is maintained."
                    )
                )
                coachMsg = "Swapped out ${skippedEx.name}. Added 2 sets to $targetName to hit your back threshold!"
            }

            // Incline DB Press / Upper Chest -> Incline Push-ups
            skippedEx.name.contains("Press", ignoreCase = true) || skippedEx.name.contains("Chest", ignoreCase = true) -> {
                val pushupEx = dayPlan.exercises.find { it.name.contains("Push-up", ignoreCase = true) || it.name.contains("Dips", ignoreCase = true) }
                    ?: dayPlan.exercises.find { it.id != skippedEx.id }
                val targetName = pushupEx?.name ?: "Incline Push-ups"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "To failure",
                        rationale = "Transfers clavicular upper-pec overload to bodyweight incline press without joint strain."
                    )
                )
                coachMsg = "Swapped out ${skippedEx.name}. Added 2 sets to $targetName to protect shoulders and maintain pec volume!"
            }

            // Squats / Quads -> Bodyweight Squats
            skippedEx.name.contains("Squat", ignoreCase = true) -> {
                val bwSquatEx = dayPlan.exercises.find { it.name.contains("Bodyweight Squat", ignoreCase = true) }
                    ?: dayPlan.exercises.find { it.id != skippedEx.id }
                val targetName = bwSquatEx?.name ?: "Bodyweight Squats"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "To failure",
                        rationale = "Maintains quad sweep hypertrophy via high-cadence bodyweight squat burnout."
                    )
                )
                coachMsg = "Swapped out ${skippedEx.name}. Added 2 sets to $targetName for quad overload without spinal loading!"
            }

            // Other exercises (Hamstring / Core / Delts / Arms)
            else -> {
                val altEx = dayPlan.exercises.firstOrNull { it.id != skippedEx.id }
                val targetName = altEx?.name ?: "Synergistic Movement"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = altEx?.repRange ?: "8-12 reps",
                        rationale = "Redistributes volume to synergistic stabilizer muscles to maintain weekly training load."
                    )
                )
                coachMsg = "Adjusted workout routine. Transferred 2 volume sets to $targetName."
            }
        }

        return CompensationOutcome(coachMsg, compensations)
    }

    /**
     * Layers rebalancing with CompensatoryEngine rules and enforces strict 3-set protein deficit cap precedence.
     */
    private fun computeAdjustedPlan(
        basePlan: WorkoutDayPlan,
        dailyMacros: DailyMacroSummary,
        rebalance: RebalanceState
    ): AdjustedWorkoutPlan {
        if (basePlan.isActiveRest) {
            return compensatoryEngine.evaluateWorkout(basePlan, dailyMacros)
        }

        val isCarbTriggerActive = dailyMacros.hasHighCarbTrigger || (dailyMacros.mealCount > 0 && dailyMacros.totalCarbs >= 60.0f)
        val isProteinDeficitActive = dailyMacros.hasProteinDeficitTrigger || (dailyMacros.mealCount > 0 && dailyMacros.totalProtein < 20.0f)

        val activeTriggers = mutableListOf<String>()
        if (isCarbTriggerActive) {
            activeTriggers.add("High-Carb Active (+1 Set to Lats & Squats)")
        }
        if (isProteinDeficitActive) {
            activeTriggers.add("Protein Deficit Active (Capped at 3 Sets)")
        }
        if (rebalance.isRebalanced) {
            activeTriggers.add("AI Rebalanced (Volume Redistributed)")
        }

        val adjustedExercises = basePlan.exercises.map { exercise ->
            if (rebalance.isRebalanced && exercise.id == rebalance.skippedExerciseId) {
                // Exercise was skipped
                AdjustedExercise(
                    baseExercise = exercise,
                    effectiveSets = 0,
                    effectiveReps = "Skipped",
                    isCarbBonusApplied = false,
                    isProteinCapApplied = false,
                    adjustmentNotes = listOf("Skipped: ${rebalance.reason}")
                )
            } else {
                // Check if this exercise received rebalance compensation
                val comp = if (rebalance.isRebalanced) {
                    rebalance.compensations.find { it.exerciseName.equals(exercise.name, ignoreCase = true) }
                } else null
                val rebalanceExtraSets = comp?.extraSets ?: 0

                // Carb bonus
                val isCarbTargetExercise = exercise.isCompensatoryEligibleCarb ||
                        exercise.name.contains("Lat Pulldown", ignoreCase = true) ||
                        exercise.name.contains("Squat", ignoreCase = true)
                val carbBonus = if (isCarbTriggerActive && isCarbTargetExercise) 1 else 0

                val tentativeSets = exercise.baselineSets + rebalanceExtraSets + carbBonus

                val effectiveSets: Int
                val isProteinCapApplied: Boolean

                if (isProteinDeficitActive) {
                    effectiveSets = min(3, tentativeSets)
                    isProteinCapApplied = tentativeSets > 3 || exercise.baselineSets > 3
                } else {
                    effectiveSets = tentativeSets
                    isProteinCapApplied = false
                }

                val notes = mutableListOf<String>()
                if (rebalanceExtraSets > 0) {
                    notes.add("+$rebalanceExtraSets sets (AI Rebalanced)")
                }
                if (carbBonus > 0) {
                    notes.add("+1 set (Carb Surplus: ${dailyMacros.totalCarbs}g)")
                }
                if (isProteinCapApplied) {
                    notes.add("Capped at 3 sets (Protein Deficit: ${dailyMacros.totalProtein}g)")
                }
                if (notes.isEmpty()) {
                    notes.add("Baseline sets maintained")
                }

                AdjustedExercise(
                    baseExercise = exercise,
                    effectiveSets = effectiveSets,
                    effectiveReps = exercise.repRange,
                    isCarbBonusApplied = carbBonus > 0,
                    isProteinCapApplied = isProteinCapApplied,
                    adjustmentNotes = notes
                )
            }
        }

        return AdjustedWorkoutPlan(
            dayNumber = basePlan.dayNumber,
            dayName = basePlan.dayName,
            focusArea = basePlan.focusArea,
            exercises = adjustedExercises,
            activeTriggers = activeTriggers
        )
    }

    private data class CompensationOutcome(
        val coachMessage: String,
        val compensations: List<CompensationItemUi>
    )
}
