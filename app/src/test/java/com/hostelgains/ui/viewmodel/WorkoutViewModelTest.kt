package com.hostelgains.ui.viewmodel

import com.hostelgains.data.nutrition.DefaultMealRepository
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutViewModelTest {

    private lateinit var mealRepository: DefaultMealRepository
    private lateinit var workoutRepository: VTaperWorkoutRepository
    private lateinit var compensatoryEngine: DefaultCompensatoryEngine
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        mealRepository = DefaultMealRepository()
        mealRepository.clearAll()
        workoutRepository = VTaperWorkoutRepository()
        compensatoryEngine = DefaultCompensatoryEngine()
        viewModel = WorkoutViewModel(
            workoutRepository = workoutRepository,
            compensatoryEngine = compensatoryEngine,
            mealRepository = mealRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialState_loadsDay1PullA() {
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedDayIndex)
        assertEquals("Day 1: Pull A", state.basePlan.dayName)
        assertEquals(4, state.adjustedPlan.exercises.size)
        assertFalse(state.rebalanceState.isRebalanced)
    }

    @Test
    fun testSelectDay_updatesDayPlanAcrossWeek() {
        // Day 2 (Index 1) Push A
        viewModel.selectDay(1)
        var state = viewModel.uiState.value
        assertEquals(1, state.selectedDayIndex)
        assertEquals("Day 2: Push A", state.basePlan.dayName)

        // Day 3 (Index 2) Legs & Core
        viewModel.selectDay(2)
        state = viewModel.uiState.value
        assertEquals(2, state.selectedDayIndex)
        assertEquals("Day 3: Legs & Core", state.basePlan.dayName)

        // Day 7 (Index 6) Active Rest
        viewModel.selectDay(6)
        state = viewModel.uiState.value
        assertEquals(6, state.selectedDayIndex)
        assertTrue(state.basePlan.isActiveRest)
    }

    @Test
    fun testCarbTrigger_addsOneSetToLatPulldowns() {
        // Log high carb meal (100g carbs)
        mealRepository.logMeal(
            MealNutrition(
                rawQuery = "4 rotis and rice",
                mealName = "Carb Meal",
                proteinGrams = 25f,
                carbsGrams = 100f,
                fatsGrams = 5f,
                calories = 550,
                isHighCarb = true,
                isLowProtein = false
            )
        )
        Thread.sleep(50)

        viewModel.selectDay(0) // Day 1 Pull A
        val state = viewModel.uiState.value
        val latPulldown = state.adjustedPlan.exercises.find { it.baseExercise.name.contains("Lat Pulldown") }
        assertNotNull(latPulldown)
        // Baseline is 4, +1 carb bonus = 5 sets
        assertEquals(5, latPulldown!!.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testProteinDeficitTrigger_capsExercisesAtThreeSets() {
        // Log low protein meal (8g protein)
        mealRepository.logMeal(
            MealNutrition(
                rawQuery = "dal and small rice",
                mealName = "Vegetarian Mess Lunch",
                proteinGrams = 8f,
                carbsGrams = 40f,
                fatsGrams = 5f,
                calories = 240,
                isHighCarb = false,
                isLowProtein = true
            )
        )
        Thread.sleep(50)

        viewModel.selectDay(0) // Day 1
        val state = viewModel.uiState.value
        val latPulldown = state.adjustedPlan.exercises.find { it.baseExercise.name.contains("Lat Pulldown") }
        assertNotNull(latPulldown)
        // Baseline 4 capped at 3
        assertEquals(3, latPulldown!!.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testPrecedence_proteinDeficitCeilingOverridesCarbBonus() {
        // High carb (80g) AND Low protein (10g)
        mealRepository.logMeal(
            MealNutrition(
                rawQuery = "Rice and sweets",
                mealName = "Carb heavy low protein",
                proteinGrams = 10f,
                carbsGrams = 80f,
                fatsGrams = 5f,
                calories = 410,
                isHighCarb = true,
                isLowProtein = true
            )
        )
        Thread.sleep(50)

        viewModel.selectDay(0)
        val state = viewModel.uiState.value
        val latPulldown = state.adjustedPlan.exercises.find { it.baseExercise.name.contains("Lat Pulldown") }
        assertNotNull(latPulldown)
        // Baseline 4 + 1 carb = 5, strictly capped at 3 by protein deficit!
        assertEquals(3, latPulldown!!.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testInlineSkipExercise_redistributesVolumeAndGeneratesCoachCard() {
        viewModel.selectDay(0) // Day 1
        viewModel.skipExercise("d1_lat_pulldown", "Equipment busy in gym")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.rebalanceState.isRebalanced)
        assertEquals("d1_lat_pulldown", state.rebalanceState.skippedExerciseId)
        assertTrue(state.rebalanceState.coachMessage.isNotEmpty())

        val cableRows = state.adjustedPlan.exercises.find { it.baseExercise.id == "d1_cable_rows" }
        assertNotNull(cableRows)
        // Baseline 3 + 2 rebalanced sets = 5 sets (when no protein deficit active)
        assertEquals(5, cableRows!!.effectiveSets)
        assertTrue(cableRows.adjustmentNotes.any { it.contains("AI Rebalanced") })
    }

    @Test
    fun testOneTapResetAction_restoresDefaultRoutine() {
        viewModel.selectDay(0)
        viewModel.skipExercise("d1_lat_pulldown")
        Thread.sleep(50)
        assertTrue(viewModel.uiState.value.rebalanceState.isRebalanced)

        viewModel.resetRebalance()
        Thread.sleep(50)
        val state = viewModel.uiState.value
        assertFalse(state.rebalanceState.isRebalanced)
        val cableRows = state.adjustedPlan.exercises.find { it.baseExercise.id == "d1_cable_rows" }
        assertEquals(3, cableRows!!.effectiveSets)
    }

    @Test
    fun testRebalanceFromPrompt_parsesNaturalLanguageStatements() {
        viewModel.selectDay(1) // Day 2: Push A
        viewModel.rebalanceFromPrompt("Wrist hurts, had to skip Incline DB press")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.rebalanceState.isRebalanced)
        assertEquals("d2_incline_db_press", state.rebalanceState.skippedExerciseId)

        // Seeks Incline Push-ups for compensation (+2 sets)
        val pushups = state.adjustedPlan.exercises.find { it.baseExercise.id == "d2_incline_pushups" }
        assertNotNull(pushups)
        // Baseline 3 + 2 = 5 sets
        assertEquals(5, pushups!!.effectiveSets)
    }

    @Test
    fun testCustomizeWorkout_swapSquatsForLegPress() {
        viewModel.selectDay(2) // Day 3: Legs & Core
        viewModel.rebalanceFromPrompt("swap squats for leg press")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.rebalanceState.isRebalanced)
        assertTrue(state.rebalanceState.isCustomized)

        val legPress = state.adjustedPlan.exercises.find { it.baseExercise.name.contains("Leg Press") }
        assertNotNull("Leg Press should replace squats", legPress)

        val squats = state.adjustedPlan.exercises.find { it.baseExercise.name.contains("Goblet Squats") || it.baseExercise.name.contains("Barbell Squats") }
        assertNull("Squats should be swapped out", squats)
    }

    @Test
    fun testCustomizeWorkout_addHammerCurls() {
        viewModel.selectDay(0) // Day 1: Pull A
        val initialCount = viewModel.uiState.value.adjustedPlan.exercises.size
        viewModel.rebalanceFromPrompt("add 3 sets of hammer curls")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.rebalanceState.isCustomized)
        assertEquals(initialCount + 1, state.adjustedPlan.exercises.size)
        assertTrue(state.adjustedPlan.exercises.any { it.baseExercise.name.contains("Hammer Curls") || it.baseExercise.name.contains("Curls") })
    }

    @Test
    fun testCustomizeWorkout_shortenWorkout() {
        viewModel.selectDay(0) // Day 1
        viewModel.rebalanceFromPrompt("shorten workout to 30 mins")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.rebalanceState.isCustomized)
        assertTrue("Shortened workout should have <= 3 exercises", state.adjustedPlan.exercises.size <= 3)
    }

    @Test
    fun testPromptDumbbellsTooHeavy_doesNotRemoveLatPulldown() {
        viewModel.selectDay(0) // Day 1: Pull A (includes Lat Pulldown)
        viewModel.rebalanceFromPrompt("bro dumbells are too heavy")
        Thread.sleep(50)

        val state = viewModel.uiState.value
        // Lat Pulldowns must still be present and not skipped!
        val latPulldown = state.adjustedPlan.exercises.find { it.baseExercise.id == "d1_lat_pulldown" }
        assertNotNull("Lat pulldown must remain in the workout when user complains about dumbbells", latPulldown)
        assertFalse("Lat pulldown should not be skipped", state.rebalanceState.skippedExerciseId == "d1_lat_pulldown")
        assertTrue("Workout customization must be marked active", state.rebalanceState.isCustomized)
    }

    @Test
    fun testToggleExerciseCompleted_tracksDoneStatus() {
        assertFalse(viewModel.uiState.value.completedExerciseIds.contains("d1_lat_pulldown"))
        viewModel.toggleExerciseCompleted("d1_lat_pulldown")
        assertTrue(viewModel.uiState.value.completedExerciseIds.contains("d1_lat_pulldown"))
        viewModel.toggleExerciseCompleted("d1_lat_pulldown")
        assertFalse(viewModel.uiState.value.completedExerciseIds.contains("d1_lat_pulldown"))
    }
}
