package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.hostelgains.data.nutrition.MealRepository
import com.hostelgains.data.profile.UserProfileRepository
import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.domain.physique.UserProfile
import com.hostelgains.domain.rebalance.GeminiWorkoutRebalancer
import com.hostelgains.domain.rebalance.WorkoutRebalancer
import com.hostelgains.domain.workout.AdjustedExercise
import com.hostelgains.domain.workout.AdjustedWorkoutPlan
import com.hostelgains.domain.workout.CompensatoryEngine
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.DynamicWorkoutRepository
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
    val isCardExpanded: Boolean = true,
    val isCustomized: Boolean = false,
    val customizedExercises: List<Exercise>? = null
)

data class WorkoutUiState(
    val selectedDayIndex: Int = 0, // 0..6 for Day 1..Day 7
    val targetBuild: PhysiqueBuild = PhysiqueBuild.V_TAPER,
    val basePlan: WorkoutDayPlan,
    val adjustedPlan: AdjustedWorkoutPlan,
    val completedExerciseIds: Set<String> = emptySet(),
    val rebalanceState: RebalanceState = RebalanceState(),
    val dailyMacros: DailyMacroSummary,
    val userProfile: UserProfile? = null
)

class WorkoutViewModel(
    private val dynamicWorkoutRepository: DynamicWorkoutRepository = DynamicWorkoutRepository(),
    private val compensatoryEngine: CompensatoryEngine = DefaultCompensatoryEngine(),
    private val mealRepository: MealRepository = AppRepositories.mealRepository,
    private val profileRepository: UserProfileRepository? = null,
    private val workoutRebalancer: WorkoutRebalancer = GeminiWorkoutRebalancer(
        apiKeyProvider = { AppRepositories.apiKeyRepository?.getApiKey() }
    ),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    // Secondary constructor for backward compatibility with tests passing VTaperWorkoutRepository
    constructor(
        workoutRepository: VTaperWorkoutRepository,
        compensatoryEngine: CompensatoryEngine = DefaultCompensatoryEngine(),
        mealRepository: MealRepository = AppRepositories.mealRepository,
        workoutRebalancer: WorkoutRebalancer = GeminiWorkoutRebalancer(
            apiKeyProvider = { AppRepositories.apiKeyRepository?.getApiKey() }
        ),
        externalScope: CoroutineScope? = null
    ) : this(
        dynamicWorkoutRepository = DynamicWorkoutRepository(PhysiqueBuild.V_TAPER),
        compensatoryEngine = compensatoryEngine,
        mealRepository = mealRepository,
        profileRepository = null,
        workoutRebalancer = workoutRebalancer,
        externalScope = externalScope
    )

    private val scope: CoroutineScope
        get() = externalScope ?: CoroutineScope(Dispatchers.Default)

    private var _selectedDayIndex: Int = 0
    private var _completedExercises: Set<String> = emptySet()
    private var _rebalanceState: RebalanceState = RebalanceState()
    private var _activeBuild: PhysiqueBuild = profileRepository?.userProfile?.value?.targetBuild ?: PhysiqueBuild.V_TAPER

    private val _uiState = MutableStateFlow(buildInitialUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private fun buildInitialUiState(): WorkoutUiState {
        dynamicWorkoutRepository.setTargetBuild(_activeBuild)
        val base = dynamicWorkoutRepository.getDayPlan(1, _activeBuild)
        val initialSummary = mealRepository.getDailySummary().value
        val adj = computeAdjustedPlan(base, initialSummary, _rebalanceState)
        return WorkoutUiState(
            selectedDayIndex = 0,
            targetBuild = _activeBuild,
            basePlan = base,
            adjustedPlan = adj,
            completedExerciseIds = emptySet(),
            rebalanceState = _rebalanceState,
            dailyMacros = initialSummary,
            userProfile = profileRepository?.userProfile?.value
        )
    }

    init {
        updateState()
        scope.launch {
            mealRepository.getDailySummary().collect { updatedSummary ->
                updateState(summary = updatedSummary)
            }
        }
        if (profileRepository != null) {
            scope.launch {
                profileRepository.userProfile.collect { updatedProfile ->
                    if (updatedProfile.targetBuild != _activeBuild) {
                        _activeBuild = updatedProfile.targetBuild
                        dynamicWorkoutRepository.setTargetBuild(_activeBuild)
                        updateState()
                    }
                }
            }
        }
    }

    private fun updateState(
        dayIndex: Int = _selectedDayIndex,
        completed: Set<String> = _completedExercises,
        rebalance: RebalanceState = _rebalanceState,
        summary: DailyMacroSummary = mealRepository.getDailySummary().value,
        build: PhysiqueBuild = _activeBuild
    ) {
        _selectedDayIndex = dayIndex
        _completedExercises = completed
        _rebalanceState = rebalance
        _activeBuild = build
        dynamicWorkoutRepository.setTargetBuild(build)
        val basePlan = dynamicWorkoutRepository.getDayPlan(dayIndex + 1, build)
        val adjusted = computeAdjustedPlan(basePlan, summary, rebalance)
        _uiState.value = WorkoutUiState(
            selectedDayIndex = dayIndex,
            targetBuild = build,
            basePlan = basePlan,
            adjustedPlan = adjusted,
            completedExerciseIds = completed,
            rebalanceState = rebalance,
            dailyMacros = summary,
            userProfile = profileRepository?.userProfile?.value
        )
    }

    fun selectPhysiqueBuild(build: PhysiqueBuild) {
        _activeBuild = build
        dynamicWorkoutRepository.setTargetBuild(build)
        profileRepository?.updateTargetBuild(build)
        // Reset rebalance when switching builds to prevent cross-routine mismatches
        _rebalanceState = RebalanceState()
        updateState(build = build, rebalance = _rebalanceState)
    }

    fun getAllPlans(): List<WorkoutDayPlan> {
        return dynamicWorkoutRepository.getAllPlans(_activeBuild)
    }

    fun selectDay(dayIndex: Int) {
        if (dayIndex in 0..6) {
            var rebalance = _rebalanceState
            if (rebalance.isRebalanced) {
                val currentDayPlan = dynamicWorkoutRepository.getDayPlan(dayIndex + 1, _activeBuild)
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
        val dayPlan = dynamicWorkoutRepository.getDayPlan(_selectedDayIndex + 1, _activeBuild)
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

    fun undoRebalance() {
        updateState(rebalance = RebalanceState())
    }

    fun resetRebalance() = undoRebalance()

    fun customizeWorkout(prompt: String) = rebalanceFromPrompt(prompt)

    fun rebalanceFromPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val dayPlan = dynamicWorkoutRepository.getDayPlan(_selectedDayIndex + 1, _activeBuild)
        val lowerPrompt = trimmed.lowercase()

        val isCustomization = lowerPrompt.contains("swap") || lowerPrompt.contains("replace") ||
                lowerPrompt.contains("add ") || lowerPrompt.startsWith("add") ||
                lowerPrompt.contains("remove ") || lowerPrompt.startsWith("remove") ||
                lowerPrompt.contains("drop ") || lowerPrompt.startsWith("drop") ||
                lowerPrompt.contains("dumbbell only") || lowerPrompt.contains("only db") ||
                lowerPrompt.contains("shorten") || lowerPrompt.contains("deload") ||
                lowerPrompt.contains("light day") || lowerPrompt.contains("heavy day") ||
                lowerPrompt.contains("instead of")

        if (!isCustomization) {
            val matchedExercise = dayPlan.exercises.firstOrNull { ex ->
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

            // Apply immediate deterministic compensation for instant UI response and test stability
            skipExercise(matchedExercise.id, reason)

            // If Gemini AI is connected, enrich the coach message and rationale asynchronously
            val apiKey = AppRepositories.apiKeyRepository?.getApiKey()
            if (!apiKey.isNullOrBlank()) {
                scope.launch {
                    val result = workoutRebalancer.rebalanceWorkout(dayPlan, matchedExercise.id, trimmed)
                    result.onSuccess { rebalanceResult ->
                        if (rebalanceResult.isAiGenerated) {
                            val cur = _rebalanceState
                            if (cur.isRebalanced && cur.skippedExerciseId == matchedExercise.id) {
                                val newRebalance = cur.copy(
                                    coachMessage = rebalanceResult.coachMessage
                                )
                                updateState(rebalance = newRebalance)
                            }
                        }
                    }
                }
            }
        } else {
            // Apply immediate deterministic customization
            scope.launch {
                val offlineCustomizer = com.hostelgains.domain.rebalance.OfflineWorkoutRebalancer()
                val offlineResult = offlineCustomizer.customizeWorkout(dayPlan, trimmed)
                offlineResult.onSuccess { res ->
                    val newRebalance = RebalanceState(
                        isRebalanced = true,
                        isCustomized = true,
                        reason = res.reason.ifEmpty { trimmed },
                        coachMessage = res.coachMessage,
                        customizedExercises = res.customizedExercises,
                        isCardExpanded = true
                    )
                    updateState(rebalance = newRebalance)
                }

                // If Gemini AI is connected, invoke dynamic AI customization asynchronously
                val apiKey = AppRepositories.apiKeyRepository?.getApiKey()
                if (!apiKey.isNullOrBlank()) {
                    val geminiResult = workoutRebalancer.customizeWorkout(dayPlan, trimmed)
                    geminiResult.onSuccess { res ->
                        if (res.isAiGenerated) {
                            val newRebalance = RebalanceState(
                                isRebalanced = true,
                                isCustomized = true,
                                reason = res.reason.ifEmpty { trimmed },
                                coachMessage = res.coachMessage,
                                customizedExercises = res.customizedExercises,
                                isCardExpanded = true
                            )
                            updateState(rebalance = newRebalance)
                        }
                    }
                }
            }
        }
    }

    private fun computeAdjustedPlan(
        basePlan: WorkoutDayPlan,
        summary: DailyMacroSummary,
        rebalance: RebalanceState
    ): AdjustedWorkoutPlan {
        val effectivePlan = if (rebalance.isCustomized && rebalance.customizedExercises != null) {
            basePlan.copy(exercises = rebalance.customizedExercises)
        } else {
            basePlan
        }

        val normalAdjusted = compensatoryEngine.evaluateWorkout(effectivePlan, summary)
        if (!rebalance.isRebalanced) {
            return normalAdjusted
        }

        if (rebalance.isCustomized) {
            return normalAdjusted.copy(
                activeTriggers = normalAdjusted.activeTriggers + "AI Customized: ${rebalance.reason}"
            )
        }

        if (rebalance.skippedExerciseId == null) {
            return normalAdjusted
        }

        val updatedExercises = normalAdjusted.exercises.map { adjEx ->
            if (adjEx.baseExercise.id == rebalance.skippedExerciseId) {
                adjEx.copy(
                    effectiveSets = 0,
                    effectiveReps = "0 (Skipped)",
                    adjustmentNotes = adjEx.adjustmentNotes + "Skipped: ${rebalance.reason}"
                )
            } else {
                val comp = rebalance.compensations.find { it.exerciseName == adjEx.baseExercise.name }
                if (comp != null) {
                    adjEx.copy(
                        effectiveSets = adjEx.effectiveSets + comp.extraSets,
                        effectiveReps = if (comp.repAdjustment.isNotEmpty()) comp.repAdjustment else adjEx.effectiveReps,
                        adjustmentNotes = adjEx.adjustmentNotes + "AI Rebalanced: +${comp.extraSets} sets (${comp.rationale})"
                    )
                } else {
                    adjEx
                }
            }
        }

        val updatedTriggers = normalAdjusted.activeTriggers +
                "Rebalanced: ${rebalance.skippedExerciseName} (${rebalance.reason})"

        return normalAdjusted.copy(
            exercises = updatedExercises,
            activeTriggers = updatedTriggers
        )
    }

    private data class CompensationCalculation(
        val coachMessage: String,
        val compensations: List<CompensationItemUi>
    )

    private fun computeDeterministicCompensation(
        dayPlan: WorkoutDayPlan,
        skipped: Exercise,
        reason: String
    ): CompensationCalculation {
        val compensations = mutableListOf<CompensationItemUi>()
        val coachMsg: String

        when {
            // Back / Lat Pulldown -> Seated Cable Rows or other Row
            skipped.name.contains("Lat Pulldown", ignoreCase = true) || skipped.targetMuscle == com.hostelgains.domain.workout.MuscleGroup.LATS -> {
                val rowEx = dayPlan.exercises.find { it.name.contains("Row", ignoreCase = true) }
                    ?: dayPlan.exercises.find { it.id != skipped.id }
                val targetName = rowEx?.name ?: "Seated Cable Rows"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "8-12 reps",
                        rationale = "Redistributes lat and upper-back volume so weekly V-taper growth is maintained."
                    )
                )
                coachMsg = "Swapped out ${skipped.name}. Added 2 sets to $targetName to hit your back threshold!"
            }

            // Incline DB Press / Upper Chest -> Incline Push-ups
            skipped.name.contains("Press", ignoreCase = true) || skipped.name.contains("Chest", ignoreCase = true) || skipped.targetMuscle == com.hostelgains.domain.workout.MuscleGroup.CHEST -> {
                val pushupEx = dayPlan.exercises.find { it.name.contains("Push-up", ignoreCase = true) || it.name.contains("Dips", ignoreCase = true) }
                    ?: dayPlan.exercises.find { it.id != skipped.id }
                val targetName = pushupEx?.name ?: "Incline Push-ups"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "To failure",
                        rationale = "Transfers clavicular upper-pec overload to bodyweight incline press without joint strain."
                    )
                )
                coachMsg = "Swapped out ${skipped.name}. Added 2 sets to $targetName to protect shoulders and maintain pec volume!"
            }

            // Squats / Quads -> Bodyweight Squats
            skipped.name.contains("Squat", ignoreCase = true) || skipped.targetMuscle == com.hostelgains.domain.workout.MuscleGroup.QUADS -> {
                val bwSquatEx = dayPlan.exercises.find { it.name.contains("Squat", ignoreCase = true) && it.id != skipped.id }
                    ?: dayPlan.exercises.find { it.id != skipped.id }
                val targetName = bwSquatEx?.name ?: "Bodyweight Squats"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "To failure",
                        rationale = "High-rep bodyweight overload compensates for skipped axial squat loading."
                    )
                )
                coachMsg = "Squat station unavailable. Redirected quad volume into $targetName with metabolic burnout sets!"
            }

            else -> {
                val alt = dayPlan.exercises.firstOrNull { it.id != skipped.id }
                val targetName = alt?.name ?: "Alternative compound movement"
                compensations.add(
                    CompensationItemUi(
                        exerciseName = targetName,
                        extraSets = 2,
                        repAdjustment = "8-12 reps",
                        rationale = "Rebalances total working volume for the session."
                    )
                )
                coachMsg = "Swapped out ${skipped.name}. Rebalanced volume into $targetName."
            }
        }

        return CompensationCalculation(
            coachMessage = coachMsg,
            compensations = compensations
        )
    }
}
