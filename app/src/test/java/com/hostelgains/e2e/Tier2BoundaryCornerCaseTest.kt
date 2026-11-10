package com.hostelgains.e2e

import com.hostelgains.domain.nutrition.DailyMacroSummary
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
 * Tier 2: Boundary & Corner Cases Test Suite.
 *
 * Exercises extreme boundary limits and edge conditions with >= 5 tests per dimension (total: 25 tests):
 * - Dimension A: Carbohydrate Boundary (59.0g vs 60.0g) (5 tests)
 * - Dimension B: Protein Deficit Boundary (19.9g vs 20.0g) (5 tests)
 * - Dimension C: Evaluation Time Cutoff (17:59 vs 18:00) (5 tests)
 * - Dimension D: Step Threshold Boundary (4,999 vs 5,000 steps) (5 tests)
 * - Dimension E: Meal Description Corner & Malformed Inputs (5 tests)
 */
class Tier2BoundaryCornerCaseTest {

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
    // Dimension A: Carbohydrate Boundary (59.0g vs 60.0g) (5 Tests)
    // =========================================================================

    @Test
    fun testTier2_01_carbsExactly59_doesNotTriggerBonus() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 59.0f,
            totalFats = 10.0f,
            totalCalories = 446,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("59.0g carbs must not trigger bonus set", 4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testTier2_02_carbs59Point9_doesNotTriggerBonus() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 59.9f,
            totalFats = 10.0f,
            totalCalories = 450,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("59.9g carbs must not trigger bonus set", 4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testTier2_03_carbsExactly60_triggersBonus() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 60.0f,
            totalFats = 10.0f,
            totalCalories = 450,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Exactly 60.0g carbs must trigger +1 set (4 -> 5)", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testTier2_04_carbs60Point1_triggersBonus() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 60.1f,
            totalFats = 10.0f,
            totalCalories = 450,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("60.1g carbs must trigger +1 set (4 -> 5)", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testTier2_05_carbsZero_doesNotCrashAndMaintainsBaseline() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 0.0f,
            totalFats = 10.0f,
            totalCalories = 210,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    // =========================================================================
    // Dimension B: Protein Deficit Boundary (19.9g vs 20.0g) (5 Tests)
    // =========================================================================

    @Test
    fun testTier2_06_protein19Point9_triggersDeficitCap() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 19.9f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 330,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("19.9g protein must trigger low-protein cap (capped at 3)", 3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier2_07_proteinExactly20_doesNotTriggerCap() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 20.0f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 330,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Exactly 20.0g protein satisfies threshold; baseline 4 preserved", 4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier2_08_protein20Point1_doesNotTriggerCap() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 20.1f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 330,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier2_09_proteinZero_triggersCapSafely() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 0.0f,
            totalCarbs = 40.0f,
            totalFats = 5.0f,
            totalCalories = 205,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testTier2_10_proteinLargeSurplus100g_doesNotTriggerCap() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 100.0f,
            totalCarbs = 40.0f,
            totalFats = 15.0f,
            totalCalories = 695,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day2 = repository.getDayPlan(2)
        val adjusted = engine.evaluateWorkout(day2, macros)

        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d2_db_lateral_raises" }
        assertEquals("5 sets preserved under high protein", 5, lateralRaises.effectiveSets)
        assertFalse(lateralRaises.isProteinCapApplied)
    }

    // =========================================================================
    // Dimension C: Time Boundary (17:59 vs 18:00) (5 Tests)
    // =========================================================================

    @Test
    fun testTier2_11_time1759_sub5kSteps_noAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 3000, hourOfDay = 17, minuteOfHour = 59)
        assertFalse("At 17:59, cutoff has not arrived; alert must be false", result.isAlertTriggered)
        assertNull(result.alertTitle)
    }

    @Test
    fun testTier2_12_time1800_sub5kSteps_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 3000, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("At 18:00 exactly, alert must trigger", result.isAlertTriggered)
        assertNotNull(result.alertTitle)
    }

    @Test
    fun testTier2_13_time1801_sub5kSteps_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 3000, hourOfDay = 18, minuteOfHour = 1)
        assertTrue("At 18:01, alert must trigger", result.isAlertTriggered)
    }

    @Test
    fun testTier2_14_time2359_sub5kSteps_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 4200, hourOfDay = 23, minuteOfHour = 59)
        assertTrue("At 23:59 late night, alert must remain active if still under 5,000 steps", result.isAlertTriggered)
    }

    @Test
    fun testTier2_15_time0000_sub5kSteps_noAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 500, hourOfDay = 0, minuteOfHour = 0)
        assertFalse("At midnight 00:00 start of new day, 6:00 PM cutoff has not arrived yet", result.isAlertTriggered)
    }

    // =========================================================================
    // Dimension D: Step Count Boundary (4,999 vs 5,000 steps) (5 Tests)
    // =========================================================================

    @Test
    fun testTier2_16_steps4999_at1800_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 4999, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("4,999 steps is 1 step under threshold; alert must trigger", result.isAlertTriggered)
    }

    @Test
    fun testTier2_17_steps5000_at1800_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 5000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("Exactly 5,000 steps satisfies threshold; alert must be suppressed", result.isAlertTriggered)
    }

    @Test
    fun testTier2_18_steps5001_at1800_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 5001, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("5,001 steps exceeds threshold; alert must be suppressed", result.isAlertTriggered)
    }

    @Test
    fun testTier2_19_stepsZero_at1800_triggersAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 0, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("0 steps at 18:00 must trigger alert safely", result.isAlertTriggered)
        assertEquals(0, result.currentSteps)
    }

    @Test
    fun testTier2_20_stepsLargeSurplus25000_at1800_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 25000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("25,000 steps suppresses alert completely", result.isAlertTriggered)
    }

    // =========================================================================
    // Dimension E: Meal Description Corner Cases (5 Tests)
    // =========================================================================

    @Test
    fun testTier2_21_mealTextEmpty_returnsFailure() = runBlocking {
        val result = parser.parseMeal("")
        assertTrue("Empty string must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testTier2_22_mealTextWhitespaceOnly_returnsFailure() = runBlocking {
        val result = parser.parseMeal("   \t\n  ")
        assertTrue("Whitespace-only input must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testTier2_23_mealTextSpecialCharsOnly_returnsFailure() = runBlocking {
        val result = parser.parseMeal("!@#$%^&*()_+=-{}[]:;\"'<>?,./")
        assertTrue("Special characters without food items must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testTier2_24_mealTextExtremelyLongString_parsesWithoutBufferOverflow() = runBlocking {
        val longPrefix = "hostel mess food was served today in dining hall room B ".repeat(100)
        val query = "$longPrefix with 4 dry rotis and mutton and 2 eggs"

        val result = parser.parseMeal(query)
        assertTrue("Extremely long query containing recognized items must parse safely", result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue("Should detect rotis/mutton/eggs", meal.proteinGrams >= 40.0f)
    }

    @Test
    fun testTier2_25_mealTextGibberishTokens_returnsFailure() = runBlocking {
        val result = parser.parseMeal("xyzabc randomnonsense nonfoodwords12345")
        assertTrue("Unrecognized gibberish food tokens must return failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
