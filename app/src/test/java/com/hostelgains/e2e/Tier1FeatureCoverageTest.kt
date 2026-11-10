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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tier 1: Comprehensive Feature Coverage Test Suite.
 *
 * Verifies all 5 core features with >= 5 tests per feature (total: 25 tests):
 * - Feature 1: Natural Language Nutrition Parsing via NutritionParser (5 tests)
 * - Feature 2: V-Taper PPL Framework Database (5 tests)
 * - Feature 3: Dynamic Carb Bonus Trigger (5 tests)
 * - Feature 4: Dynamic Protein Deficit Cap (5 tests)
 * - Feature 5: Step Counter Sync & 6:00 PM Alert Engine (5 tests)
 */
class Tier1FeatureCoverageTest {

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

    // =========================================================================
    // Feature 1: Nutrition Parsing (5 Tests)
    // =========================================================================

    @Test
    fun testTier1_01_parseMuttonRotisEggs_returnsHighProteinAndCarb() = runBlocking {
        val result = parser.parseMeal("mutton, 4 dry rotis, eggs")
        assertTrue("Parsing should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be >= 40g for mutton feast, got ${meal.proteinGrams}", meal.proteinGrams >= 40.0f)
        assertTrue("Carbs should be >= 60g for 4 rotis, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb flag must be true", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
        assertEquals(3, meal.items.size)
    }

    @Test
    fun testTier1_02_parseDalAndRice_returnsLowProteinAndHighCarb() = runBlocking {
        val result = parser.parseMeal("dal and rice")
        assertTrue("Parsing should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be < 20g for standard dal-rice, got ${meal.proteinGrams}", meal.proteinGrams < 20.0f)
        assertTrue("Carbs should be >= 60g for rice + dal, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb flag must be true", meal.isHighCarb)
        assertTrue("Protein deficit flag must be true", meal.isLowProtein)
    }

    @Test
    fun testTier1_03_parsePaneerBhurjiAndParathas_returnsHighProteinAndCarb() = runBlocking {
        val result = parser.parseMeal("paneer bhurji and 3 parathas")
        assertTrue("Parsing should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be >= 25g, got ${meal.proteinGrams}", meal.proteinGrams >= 25.0f)
        assertTrue("Carbs should be >= 60g, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb flag must be true", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
    }

    @Test
    fun testTier1_04_parseBoiledEggWhitesAndRotis_returnsLeanMacros() = runBlocking {
        val result = parser.parseMeal("4 boiled egg whites and 2 rotis")
        assertTrue("Parsing should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be >= 18g, got ${meal.proteinGrams}", meal.proteinGrams >= 18.0f)
        assertTrue("Carbs should be < 60g for 2 rotis, got ${meal.carbsGrams}", meal.carbsGrams < 60.0f)
        assertFalse("High carb flag must be false", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
    }

    @Test
    fun testTier1_05_parseChaiAndSamosas_returnsSnackDeficit() = runBlocking {
        val result = parser.parseMeal("chai and 2 samosas")
        assertTrue("Parsing should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be < 15g, got ${meal.proteinGrams}", meal.proteinGrams < 15.0f)
        assertTrue("Carbs should be >= 60g, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb flag must be true", meal.isHighCarb)
        assertTrue("Protein deficit flag must be true", meal.isLowProtein)
    }

    // =========================================================================
    // Feature 2: V-Taper PPL Framework Database (5 Tests)
    // =========================================================================

    @Test
    fun testTier1_06_day1PullASchedule_baselineSetsAndReps() {
        val day1 = repository.getDayPlan(1)
        assertEquals("Day 1: Pull A", day1.dayName)
        assertEquals(4, day1.exercises.size)

        val latPulldown = day1.exercises[0]
        assertEquals("Wide-Grip Lat Pulldowns", latPulldown.name)
        assertEquals(4, latPulldown.baselineSets)
        assertEquals("8-12 reps", latPulldown.repRange)
        assertTrue(latPulldown.isCompensatoryEligibleCarb)

        val cableRows = day1.exercises[1]
        assertEquals("Seated Cable Rows", cableRows.name)
        assertEquals(3, cableRows.baselineSets)

        val rearDeltFlyes = day1.exercises[2]
        assertEquals("Machine Rear Delt Flyes", rearDeltFlyes.name)
        assertEquals(4, rearDeltFlyes.baselineSets)

        val hammerCurls = day1.exercises[3]
        assertEquals("Dumbbell Hammer Curls", hammerCurls.name)
        assertEquals(3, hammerCurls.baselineSets)
    }

    @Test
    fun testTier1_07_day2PushASchedule_baselineSetsAndReps() {
        val day2 = repository.getDayPlan(2)
        assertEquals("Day 2: Push A", day2.dayName)
        assertEquals(4, day2.exercises.size)

        assertEquals("Incline Dumbbell Press", day2.exercises[0].name)
        assertEquals(4, day2.exercises[0].baselineSets)

        assertEquals("Dumbbell Lateral Raises", day2.exercises[1].name)
        assertEquals(5, day2.exercises[1].baselineSets)

        assertEquals("Incline Push-ups", day2.exercises[2].name)
        assertEquals(3, day2.exercises[2].baselineSets)

        assertEquals("Tricep Rope Pushdowns", day2.exercises[3].name)
        assertEquals(3, day2.exercises[3].baselineSets)
    }

    @Test
    fun testTier1_08_day3LegsAndCoreSchedule_baselineSetsAndReps() {
        val day3 = repository.getDayPlan(3)
        assertEquals("Day 3: Legs & Core", day3.dayName)
        assertEquals(4, day3.exercises.size)

        val gobletSquats = day3.exercises[0]
        assertEquals("Goblet Squats", gobletSquats.name)
        assertEquals(4, gobletSquats.baselineSets)
        assertTrue(gobletSquats.isCompensatoryEligibleCarb)

        val bwSquats = day3.exercises[1]
        assertEquals("Bodyweight Squats", bwSquats.name)
        assertEquals(2, bwSquats.baselineSets)
        assertTrue(bwSquats.isCompensatoryEligibleCarb)

        val legCurls = day3.exercises[2]
        assertEquals("Lying Leg Curls", legCurls.name)
        assertEquals(3, legCurls.baselineSets)

        val plank = day3.exercises[3]
        assertEquals("Plank", plank.name)
        assertEquals(3, plank.baselineSets)
    }

    @Test
    fun testTier1_09_day6WeakPointPump_fst7VolumeAndRest() {
        val day6 = repository.getDayPlan(6)
        assertEquals("Day 6: Weak-Point Pump", day6.dayName)
        assertEquals(3, day6.exercises.size)

        val fst7LatPulldowns = day6.exercises[0]
        assertEquals("Lat Pulldowns", fst7LatPulldowns.name)
        assertEquals(7, fst7LatPulldowns.baselineSets)
        assertEquals("14 reps", fst7LatPulldowns.repRange)
        assertEquals(45, fst7LatPulldowns.restSeconds)
        assertTrue(fst7LatPulldowns.isCompensatoryEligibleCarb)

        val lateralRaises = day6.exercises[1]
        assertEquals("Cable Lateral Raises", lateralRaises.name)
        assertEquals(5, lateralRaises.baselineSets)
        assertEquals(45, lateralRaises.restSeconds)

        val facePulls = day6.exercises[2]
        assertEquals("Face Pulls", facePulls.name)
        assertEquals(3, facePulls.baselineSets)
        assertEquals(60, facePulls.restSeconds)
    }

    @Test
    fun testTier1_10_day7ActiveRest_stepTargetOnly() {
        val day7 = repository.getDayPlan(7)
        assertEquals("Day 7: Active Rest", day7.dayName)
        assertTrue(day7.isActiveRest)
        assertEquals(1, day7.exercises.size)
        assertEquals("Active Recovery Walking", day7.exercises[0].name)
        assertEquals("5,000 - 10,000 steps", day7.exercises[0].repRange)
    }

    // =========================================================================
    // Feature 3: Dynamic Carb Bonus Trigger (5 Tests)
    // =========================================================================

    @Test
    fun testTier1_11_carbBonus_addsOneSetToLatPulldownsOnPullA() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = 70.0f,
            totalFats = 15.0f,
            totalCalories = 555,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, highCarbMacros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Lat Pulldowns must receive +1 set (4 -> 5)", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier1_12_carbBonus_doesNotAddSetsToOtherPullExercises() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = 75.0f,
            totalFats = 15.0f,
            totalCalories = 575,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, highCarbMacros)

        val cableRows = adjusted.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val rearDeltFlyes = adjusted.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val hammerCurls = adjusted.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        assertEquals(3, cableRows.effectiveSets)
        assertFalse(cableRows.isCarbBonusApplied)
        assertEquals(4, rearDeltFlyes.effectiveSets)
        assertFalse(rearDeltFlyes.isCarbBonusApplied)
        assertEquals(3, hammerCurls.effectiveSets)
        assertFalse(hammerCurls.isCarbBonusApplied)
    }

    @Test
    fun testTier1_13_carbBonus_addsOneSetToGobletSquatsAndBwSquats() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 80.0f,
            totalFats = 12.0f,
            totalCalories = 548,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day3 = repository.getDayPlan(3)
        val adjusted = engine.evaluateWorkout(day3, highCarbMacros)

        val gobletSquats = adjusted.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        val bwSquats = adjusted.exercises.first { it.baseExercise.id == "d3_bw_squats" }
        val legCurls = adjusted.exercises.first { it.baseExercise.id == "d3_lying_leg_curls" }

        assertEquals("Goblet Squats should increase from 4 to 5", 5, gobletSquats.effectiveSets)
        assertTrue(gobletSquats.isCarbBonusApplied)

        assertEquals("Bodyweight Squats should increase from 2 to 3", 3, bwSquats.effectiveSets)
        assertTrue(bwSquats.isCarbBonusApplied)

        assertEquals("Leg curls should remain baseline 3", 3, legCurls.effectiveSets)
        assertFalse(legCurls.isCarbBonusApplied)
    }

    @Test
    fun testTier1_14_carbBonus_addsOneSetToDay6Fst7LatPulldowns() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 40.0f,
            totalCarbs = 65.0f,
            totalFats = 15.0f,
            totalCalories = 555,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day6 = repository.getDayPlan(6)
        val adjusted = engine.evaluateWorkout(day6, highCarbMacros)

        val fst7LatPulldowns = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        assertEquals("FST-7 Lat Pulldowns should increase from 7 to 8", 8, fst7LatPulldowns.effectiveSets)
        assertTrue(fst7LatPulldowns.isCarbBonusApplied)
    }

    @Test
    fun testTier1_15_carbBonus_onPushDayDoesNotAlterPushMovements() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = 90.0f,
            totalFats = 15.0f,
            totalCalories = 635,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day2 = repository.getDayPlan(2)
        val adjusted = engine.evaluateWorkout(day2, highCarbMacros)

        for (exercise in adjusted.exercises) {
            assertEquals("Push day exercise ${exercise.baseExercise.name} sets unchanged",
                exercise.baseExercise.baselineSets, exercise.effectiveSets)
            assertFalse(exercise.isCarbBonusApplied)
        }
    }

    // =========================================================================
    // Feature 4: Dynamic Protein Deficit Cap (5 Tests)
    // =========================================================================

    @Test
    fun testTier1_16_proteinCap_restrictsFourSetExercisesToThree() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 12.0f,
            totalCarbs = 40.0f,
            totalFats = 8.0f,
            totalCalories = 280,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, lowProteinMacros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val rearDeltFlyes = adjusted.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }

        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)

        assertEquals(3, rearDeltFlyes.effectiveSets)
        assertTrue(rearDeltFlyes.isProteinCapApplied)
    }

    @Test
    fun testTier1_17_proteinCap_restrictsFiveSetLateralRaisesToThree() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 14.0f,
            totalCarbs = 45.0f,
            totalFats = 10.0f,
            totalCalories = 326,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day2 = repository.getDayPlan(2)
        val adjusted = engine.evaluateWorkout(day2, lowProteinMacros)

        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d2_db_lateral_raises" }
        assertEquals("5-set lateral raises must be capped at 3", 3, lateralRaises.effectiveSets)
        assertTrue(lateralRaises.isProteinCapApplied)
    }

    @Test
    fun testTier1_18_proteinCap_restrictsSevenSetFst7ToThree() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 10.0f,
            totalCarbs = 35.0f,
            totalFats = 5.0f,
            totalCalories = 225,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day6 = repository.getDayPlan(6)
        val adjusted = engine.evaluateWorkout(day6, lowProteinMacros)

        val fst7LatPulldowns = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        assertEquals("7-set FST-7 must be capped strictly at 3 sets", 3, fst7LatPulldowns.effectiveSets)
        assertTrue(fst7LatPulldowns.isProteinCapApplied)
    }

    @Test
    fun testTier1_19_proteinCap_preservesThreeSetExercisesAtThree() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 15.0f,
            totalCarbs = 40.0f,
            totalFats = 8.0f,
            totalCalories = 292,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, lowProteinMacros)

        val cableRows = adjusted.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val hammerCurls = adjusted.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        assertEquals(3, cableRows.effectiveSets)
        assertEquals(3, hammerCurls.effectiveSets)
    }

    @Test
    fun testTier1_20_proteinCap_preservesTwoSetExercisesAtTwo() {
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 12.0f,
            totalCarbs = 40.0f,
            totalFats = 8.0f,
            totalCalories = 280,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day3 = repository.getDayPlan(3)
        val adjusted = engine.evaluateWorkout(day3, lowProteinMacros)

        val bwSquats = adjusted.exercises.first { it.baseExercise.id == "d3_bw_squats" }
        assertEquals("2-set exercise remains 2 sets under protein cap", 2, bwSquats.effectiveSets)
    }

    // =========================================================================
    // Feature 5: Step Counter Sync & 6 PM Alert Engine (5 Tests)
    // =========================================================================

    @Test
    fun testTier1_21_stepAlert_sub5kStepsAt1800_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 4500, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("Sub-5,000 steps at 18:00 must trigger alert", result.isAlertTriggered)
        assertNotNull(result.alertTitle)
        assertNotNull(result.alertMessage)
    }

    @Test
    fun testTier1_22_stepAlert_5kStepsAt1800_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 5000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("5,000 steps at 18:00 must suppress alert", result.isAlertTriggered)
        assertNull(result.alertTitle)
        assertNull(result.alertMessage)
    }

    @Test
    fun testTier1_23_stepAlert_sub5kStepsAt1759_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 3000, hourOfDay = 17, minuteOfHour = 59)
        assertFalse("Sub-5,000 steps prior to 18:00 must suppress alert", result.isAlertTriggered)
        assertNull(result.alertTitle)
    }

    @Test
    fun testTier1_24_stepAlert_sub5kStepsAt2030_maintainsAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 3800, hourOfDay = 20, minuteOfHour = 30)
        assertTrue("Sub-5,000 steps in late evening must maintain alert", result.isAlertTriggered)
        assertTrue(result.alertMessage!!.contains("3800"))
    }

    @Test
    fun testTier1_25_stepAlert_alertContentSpecifies10kWalkRecommendation() {
        val result = stepEvaluator.evaluate(currentSteps = 2200, hourOfDay = 19, minuteOfHour = 15)
        assertTrue(result.isAlertTriggered)
        assertTrue("Alert must recommend 10,000 steps after dinner",
            result.alertMessage!!.contains("10,000-step"))
        assertEquals(10000, result.targetSteps)
    }
}
