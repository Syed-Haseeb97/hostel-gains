package com.hostelgains.e2e

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import com.hostelgains.e2e.fixtures.FixtureNutritionParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tier 3: Cross-Feature Combinations & Conflict Resolution Test Suite.
 *
 * Verifies multi-system interactions, trigger precedence, and complex cumulative state flows:
 * - High-Carb + Low-Protein Simultaneous Triggers (Safety Precedence Ceiling)
 * - Multi-Meal Daily Accumulation & State Transitions
 * - Step Deficit + Carb Surplus Evening Synergy
 * - Extreme Volume Protocol (FST-7) under Severe Nutritional Restriction
 * - Active Rest Day Invariant under High Carb Ingestion
 */
class Tier3CombinationsTest {

    private lateinit var parser: FixtureNutritionParser
    private lateinit var engine: DefaultCompensatoryEngine
    private lateinit var repository: VTaperWorkoutRepository
    private lateinit var stepEvaluator: DefaultStepAlertEvaluator

    @Before
    fun setUp() {
        parser = FixtureNutritionParser()
        engine = DefaultCompensatoryEngine()
        repository = VTaperWorkoutRepository()
        stepEvaluator = DefaultStepAlertEvaluator()
    }

    @Test
    fun testTier3_01_highCarbLowProteinSimultaneous_proteinCapPrecedenceEnforcedOnPullA() = runBlocking {
        // High carb (60g carbs) AND low protein (8.5g protein) from Indian mess dal and rice
        val parseResult = parser.parseMeal("dal and rice")
        assertTrue(parseResult.isSuccess)
        val dalRiceMeal = parseResult.getOrThrow()

        assertTrue(dalRiceMeal.isHighCarb)
        assertTrue(dalRiceMeal.isLowProtein)

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, dalRiceMeal)

        // Both triggers must be active in plan audit
        assertEquals(2, adjusted.activeTriggers.size)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val cableRows = adjusted.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val rearDeltFlyes = adjusted.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val hammerCurls = adjusted.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        // Physiological Precedence: effectiveSets = min(3, 4 + 1) = 3 sets
        assertEquals("Lat Pulldowns must be capped at 3 despite carb bonus", 3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertTrue(latPulldown.isProteinCapApplied)

        // Other exercises capped or maintained at 3
        assertEquals(3, cableRows.effectiveSets)
        assertEquals(3, rearDeltFlyes.effectiveSets)
        assertTrue(rearDeltFlyes.isProteinCapApplied)
        assertEquals(3, hammerCurls.effectiveSets)
    }

    @Test
    fun testTier3_02_highCarbLowProteinSimultaneous_squatsCappedOnLegDay() = runBlocking {
        val parseResult = parser.parseMeal("dal and rice")
        val dalRiceMeal = parseResult.getOrThrow()

        val day3 = repository.getDayPlan(3)
        val adjusted = engine.evaluateWorkout(day3, dalRiceMeal)

        val gobletSquats = adjusted.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        val bwSquats = adjusted.exercises.first { it.baseExercise.id == "d3_bw_squats" }

        // Goblet squats: min(3, 4 + 1) = 3 sets
        assertEquals("Goblet Squats capped at 3", 3, gobletSquats.effectiveSets)
        assertTrue(gobletSquats.isCarbBonusApplied)
        assertTrue(gobletSquats.isProteinCapApplied)

        // Bodyweight squats: min(3, 2 + 1) = 3 sets
        assertEquals("BW Squats increases from 2 to 3 sets", 3, bwSquats.effectiveSets)
        assertTrue(bwSquats.isCarbBonusApplied)
    }

    @Test
    fun testTier3_03_day6WeakPointPumpUnderProteinDeficit_drasticVolumeReduction() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 15.0f,
            totalCarbs = 65.0f, // Both triggers active
            totalFats = 10.0f,
            totalCalories = 410,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = true
        )

        val day6 = repository.getDayPlan(6)
        val adjusted = engine.evaluateWorkout(day6, lowProteinMacros)

        val fst7LatPulldowns = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d6_cable_lateral_raises" }
        val facePulls = adjusted.exercises.first { it.baseExercise.id == "d6_face_pulls" }

        // FST-7 normally 7 sets -> min(3, 7 + 1) = 3 sets
        assertEquals("FST-7 volume drastically reduced from 7 to 3 sets", 3, fst7LatPulldowns.effectiveSets)
        assertTrue(fst7LatPulldowns.isProteinCapApplied)

        // Lateral raises normally 5 sets -> min(3, 5) = 3 sets
        assertEquals("Lateral raises capped at 3 sets", 3, lateralRaises.effectiveSets)
        assertTrue(lateralRaises.isProteinCapApplied)

        // Face pulls normally 3 sets -> 3 sets
        assertEquals(3, facePulls.effectiveSets)
    }

    @Test
    fun testTier3_04_multiMealAccumulation_updatesDailyTotalsAndCumulativeTriggers() = runBlocking {
        // Meal 1: Breakfast (Chai & 2 Samosas)
        val meal1 = parser.parseMeal("chai and 2 samosas").getOrThrow()
        var dailyP = meal1.proteinGrams
        var dailyC = meal1.carbsGrams
        var dailyF = meal1.fatsGrams
        var dailyCal = meal1.calories

        var summary = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = dailyP,
            totalCarbs = dailyC,
            totalFats = dailyF,
            totalCalories = dailyCal,
            hasHighCarbTrigger = dailyC >= 60.0f,
            hasProteinDeficitTrigger = dailyP < 20.0f,
            mealCount = 1
        )
        // After breakfast: Protein is low (8g < 20g), carbs are high (60g >= 60g)
        assertTrue("Meal 1 triggers protein deficit", summary.hasProteinDeficitTrigger)
        assertTrue("Meal 1 triggers high carb", summary.hasHighCarbTrigger)

        // Meal 2: Lunch (Dal and Rice)
        val meal2 = parser.parseMeal("dal and rice").getOrThrow()
        dailyP += meal2.proteinGrams
        dailyC += meal2.carbsGrams
        dailyF += meal2.fatsGrams
        dailyCal += meal2.calories

        summary = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = dailyP,
            totalCarbs = dailyC,
            totalFats = dailyF,
            totalCalories = dailyCal,
            hasHighCarbTrigger = dailyC >= 60.0f,
            hasProteinDeficitTrigger = dailyP < 20.0f,
            mealCount = 2
        )
        // After lunch: dailyP = 8 + 8.5 = 16.5g (< 20g -> still deficit), dailyC = 120g
        assertTrue("Still in protein deficit after lunch", summary.hasProteinDeficitTrigger)
        assertEquals(120.0f, summary.totalCarbs, 0.1f)

        // Meal 3: Evening High-Protein Snack (4 egg whites & 2 rotis)
        val meal3 = parser.parseMeal("4 boiled egg whites and 2 rotis").getOrThrow()
        dailyP += meal3.proteinGrams
        dailyC += meal3.carbsGrams
        dailyF += meal3.fatsGrams
        dailyCal += meal3.calories

        summary = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = dailyP,
            totalCarbs = dailyC,
            totalFats = dailyF,
            totalCalories = dailyCal,
            hasHighCarbTrigger = dailyC >= 60.0f,
            hasProteinDeficitTrigger = dailyP < 20.0f,
            mealCount = 3
        )
        // After evening eggs: dailyP = 16.5 + 20 = 36.5g (Deficit clears!)
        assertFalse("Protein deficit successfully cleared after evening egg whites", summary.hasProteinDeficitTrigger)
        assertTrue("High carb remains active", summary.hasHighCarbTrigger)

        // Evaluate evening Pull A workout with cleared protein deficit and active carb surplus
        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, summary)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

        // Now Lat Pulldowns gets full 5 sets (4 + 1 bonus without cap!)
        assertEquals("Lat Pulldowns receives full 5 sets once protein deficit clears", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier3_05_stepDeficitPlusCarbSurplus_simultaneousEveningSynergy() = runBlocking {
        // High carb Sunday mess dinner
        val muttonMeal = parser.parseMeal("mutton, 4 dry rotis, eggs").getOrThrow()
        val day1 = repository.getDayPlan(1)
        val adjustedWorkout = engine.evaluateWorkout(day1, muttonMeal)

        // Workout receives carb surplus volume
        val latPulldown = adjustedWorkout.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(5, latPulldown.effectiveSets)

        // Concurrently, student was studying and has only 3,200 steps at 18:30
        val alertResult = stepEvaluator.evaluate(currentSteps = 3200, hourOfDay = 18, minuteOfHour = 30)

        // Alert is active recommending a 10,000-step post-dinner walk
        assertTrue("Step alert must trigger concurrently", alertResult.isAlertTriggered)
        assertTrue(alertResult.alertMessage!!.contains("10,000-step"))
    }

    @Test
    fun testTier3_06_day7ActiveRestWithCarbSurplus_noPhantomResistanceSetsAdded() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = 110.0f,
            totalFats = 20.0f,
            totalCalories = 760,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day7 = repository.getDayPlan(7)
        val adjusted = engine.evaluateWorkout(day7, highCarbMacros)

        // Must remain exactly 1 recovery activity with 0 resistance sets
        assertEquals(1, adjusted.exercises.size)
        val walking = adjusted.exercises[0]
        assertEquals("Active Recovery Walking", walking.baseExercise.name)
        assertEquals(1, walking.effectiveSets)
        assertFalse(walking.isCarbBonusApplied)
        assertFalse(walking.isProteinCapApplied)
    }
}
