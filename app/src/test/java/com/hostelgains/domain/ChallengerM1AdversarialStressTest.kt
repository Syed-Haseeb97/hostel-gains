package com.hostelgains.domain

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Empirical Adversarial Stress Test Suite for Milestone M1.
 *
 * Authored by Challenger 1 (EMPIRICAL CHALLENGER).
 * Stress-tests domain logic, CompensatoryEngine, StepAlertEvaluator, and VTaperWorkoutRepository
 * across boundary conditions, mathematical invariants, and adversarial edge inputs.
 */
class ChallengerM1AdversarialStressTest {

    private lateinit var engine: DefaultCompensatoryEngine
    private lateinit var stepEvaluator: DefaultStepAlertEvaluator
    private lateinit var repository: VTaperWorkoutRepository

    @Before
    fun setUp() {
        engine = DefaultCompensatoryEngine()
        stepEvaluator = DefaultStepAlertEvaluator()
        repository = VTaperWorkoutRepository()
    }

    // =========================================================================
    // SECTION 1: Adversarial Carbohydrate Variations on CompensatoryEngine
    // =========================================================================

    @Test
    fun testCarbs_massive1000g_addsExactlyOneSetWithoutOverflow() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 50.0f,
            totalCarbs = 1000.0f,
            totalFats = 20.0f,
            totalCalories = 4380,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Massive 1000g carbs must add exactly +1 set (4 -> 5)", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)

        val day3 = repository.getDayPlan(3)
        val adjustedDay3 = engine.evaluateWorkout(day3, macros)
        val gobletSquats = adjustedDay3.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        assertEquals("Massive 1000g carbs must add exactly +1 set to Goblet Squats (4 -> 5)", 5, gobletSquats.effectiveSets)
    }

    @Test
    fun testCarbs_extremeOneMillionGrams_doesNotOverflow() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 50.0f,
            totalCarbs = 1_000_000.0f,
            totalFats = 20.0f,
            totalCalories = 4000200,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

        assertEquals(5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCarbs_zero_maintainsExactBaseline() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = 0.0f,
            totalFats = 10.0f,
            totalCalories = 230,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCarbs_negativeValues_maintainsBaselineWithoutCrash() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = -500.0f,
            totalFats = 10.0f,
            totalCalories = 0,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCarbs_floatNaN_maintainsBaselineWithoutCrash() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = Float.NaN,
            totalFats = 10.0f,
            totalCalories = 0,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(4, latPulldown.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCarbs_positiveInfinity_triggersCarbBonus() {
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35.0f,
            totalCarbs = Float.POSITIVE_INFINITY,
            totalFats = 10.0f,
            totalCalories = 0,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCarbs_fineGrainedBoundaryGridSearch() {
        val day1 = repository.getDayPlan(1)

        // Test 100 points between 59.50g and 60.50g in 0.01g increments
        for (i in 0..100) {
            val carbValue = 59.50f + (i * 0.01f)
            val macros = DailyMacroSummary(
                dateIso = "2026-11-10",
                totalProtein = 30.0f,
                totalCarbs = carbValue,
                totalFats = 10.0f,
                totalCalories = 400
            )

            val adjusted = engine.evaluateWorkout(day1, macros)
            val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

            if (carbValue >= 60.0f) {
                assertEquals("Carb >= 60.0f ($carbValue) must trigger +1 set", 5, latPulldown.effectiveSets)
                assertTrue(latPulldown.isCarbBonusApplied)
            } else {
                assertEquals("Carb < 60.0f ($carbValue) must NOT trigger bonus set", 4, latPulldown.effectiveSets)
                assertFalse(latPulldown.isCarbBonusApplied)
            }
        }
    }

    @Test
    fun testCarbs_explicitFlagOverride_triggersBonusRegardlessOfTotalCarbs() {
        // totalCarbs = 10g (<60g), but parser has marked hasHighCarbTrigger = true
        val macros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 10.0f,
            totalFats = 5.0f,
            totalCalories = 205,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, macros)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("hasHighCarbTrigger flag must trigger bonus set", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    // =========================================================================
    // SECTION 2: Adversarial Protein Variations on CompensatoryEngine
    // =========================================================================

    @Test
    fun testProtein_zero_triggersDeficitCapOnAllDays() {
        val zeroProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 0.0f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 250,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = true
        )

        // Verify that across all resistance training days (Days 1 to 6), every exercise is <= 3 sets
        for (day in 1..6) {
            val dayPlan = repository.getDayPlan(day)
            val adjusted = engine.evaluateWorkout(dayPlan, zeroProteinMacros)

            for (adjExercise in adjusted.exercises) {
                assertTrue(
                    "Day $day exercise ${adjExercise.baseExercise.name} effectiveSets (${adjExercise.effectiveSets}) exceeds max 3 under zero protein",
                    adjExercise.effectiveSets <= 3
                )
            }
        }
    }

    @Test
    fun testProtein_negativeValues_triggersDeficitCapSafely() {
        val negativeProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = -50.0f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 0,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false // relying on totalProtein < 20.0f
        )

        val day1 = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1, negativeProteinMacros)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testProtein_massiveSurplus10000g_preservesBaselinesWithoutSpuriousCap() {
        val massiveProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 10000.0f,
            totalCarbs = 40.0f,
            totalFats = 10.0f,
            totalCalories = 40250,
            hasHighCarbTrigger = false,
            hasProteinDeficitTrigger = false
        )

        val day6 = repository.getDayPlan(6)
        val adjusted = engine.evaluateWorkout(day6, massiveProteinMacros)
        val fst7LatPulldown = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        assertEquals("Baseline 7 sets preserved under high protein surplus", 7, fst7LatPulldown.effectiveSets)
        assertFalse(fst7LatPulldown.isProteinCapApplied)
    }

    @Test
    fun testProtein_fineGrainedBoundaryGridSearch() {
        val day1 = repository.getDayPlan(1)

        // Test 100 points between 19.50g and 20.50g in 0.01g increments
        for (i in 0..100) {
            val proteinValue = 19.50f + (i * 0.01f)
            val macros = DailyMacroSummary(
                dateIso = "2026-11-10",
                totalProtein = proteinValue,
                totalCarbs = 40.0f,
                totalFats = 10.0f,
                totalCalories = 300
            )

            val adjusted = engine.evaluateWorkout(day1, macros)
            val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

            if (proteinValue < 20.0f) {
                assertEquals("Protein < 20.0f ($proteinValue) must cap Lat Pulldowns at 3", 3, latPulldown.effectiveSets)
                assertTrue(latPulldown.isProteinCapApplied)
            } else {
                assertEquals("Protein >= 20.0f ($proteinValue) must preserve baseline 4", 4, latPulldown.effectiveSets)
                assertFalse(latPulldown.isProteinCapApplied)
            }
        }
    }

    // =========================================================================
    // SECTION 3: Simultaneous Triggers, Conflict Resolution, & Invariants
    // =========================================================================

    @Test
    fun testSimultaneousTriggers_massiveCarbsAndSevereDeficit_proteinCapStrictPrecedence() {
        // 1,000g carbs (+1 carb bonus) + 2g protein (deficit cap)
        val extremeMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 2.0f,
            totalCarbs = 1000.0f,
            totalFats = 10.0f,
            totalCalories = 4100,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = true
        )

        // Day 1: Lat Pulldowns baseline 4 -> +1 bonus = 5 tentative -> capped at min(3, 5) = 3
        val day1 = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1, extremeMacros)
        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

        assertEquals("Protein deficit ceiling must override carb bonus: min(3, 4+1) = 3", 3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertTrue(latPulldown.isProteinCapApplied)

        // Day 6: FST-7 Lat Pulldowns baseline 7 -> +1 bonus = 8 tentative -> capped at min(3, 8) = 3
        val day6 = repository.getDayPlan(6)
        val adjustedDay6 = engine.evaluateWorkout(day6, extremeMacros)
        val fst7LatPulldown = adjustedDay6.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        assertEquals("FST-7 Lat Pulldowns capped strictly at 3 sets", 3, fst7LatPulldown.effectiveSets)
        assertTrue(fst7LatPulldown.isCarbBonusApplied)
        assertTrue(fst7LatPulldown.isProteinCapApplied)
    }

    @Test
    fun testPropertyBased_randomizedMacroStress_proteinCapInvariantHoldsGlobally() {
        val random = Random(42)

        // 500 randomized macro combinations across all 6 training days
        for (iteration in 1..500) {
            val protein = random.nextFloat() * 120.0f - 20.0f // -20g to 100g
            val carbs = random.nextFloat() * 300.0f - 50.0f   // -50g to 250g
            val dayNumber = random.nextInt(1, 7)            // Days 1 to 6

            val macros = DailyMacroSummary(
                dateIso = "2026-11-10",
                totalProtein = protein,
                totalCarbs = carbs,
                totalFats = 15.0f,
                totalCalories = 500
            )

            val dayPlan = repository.getDayPlan(dayNumber)
            val adjusted = engine.evaluateWorkout(dayPlan, macros)

            val isProteinDeficit = protein < 20.0f

            for (adjExercise in adjusted.exercises) {
                if (isProteinDeficit) {
                    assertTrue(
                        "Iteration $iteration: Invariant violation on Day $dayNumber exercise ${adjExercise.baseExercise.name}: " +
                                "effectiveSets (${adjExercise.effectiveSets}) > 3 under protein $protein g",
                        adjExercise.effectiveSets <= 3
                    )
                }
            }
        }
    }

    @Test
    fun testActiveRestDay_immuneToAllMacroExtremes() {
        val day7 = repository.getDayPlan(7)

        val testCases = listOf(
            DailyMacroSummary("2026-11-10", totalProtein = 0.0f, totalCarbs = 1000.0f, totalFats = 0.0f, totalCalories = 4000),
            DailyMacroSummary("2026-11-10", totalProtein = -50.0f, totalCarbs = -100.0f, totalFats = 0.0f, totalCalories = 0),
            DailyMacroSummary("2026-11-10", totalProtein = Float.NaN, totalCarbs = Float.NaN, totalFats = 0.0f, totalCalories = 0),
            DailyMacroSummary("2026-11-10", totalProtein = 200.0f, totalCarbs = 500.0f, totalFats = 50.0f, totalCalories = 3250)
        )

        for (macros in testCases) {
            val adjusted = engine.evaluateWorkout(day7, macros)
            assertEquals(1, adjusted.exercises.size)
            val walk = adjusted.exercises[0]
            assertEquals("Active Recovery Walking", walk.baseExercise.name)
            assertEquals(1, walk.effectiveSets)
            assertFalse(walk.isCarbBonusApplied)
            assertFalse(walk.isProteinCapApplied)
            assertTrue(adjusted.activeTriggers.isEmpty())
        }
    }

    @Test
    fun testNonEligibleExercises_neverReceiveCarbBonus() {
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 40.0f,
            totalCarbs = 500.0f, // Massive carb surplus
            totalFats = 15.0f,
            totalCalories = 2295,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = false
        )

        // Day 2 (Push A) has NO Lat Pulldowns or Squats
        val day2 = repository.getDayPlan(2)
        val adjustedDay2 = engine.evaluateWorkout(day2, highCarbMacros)

        for (adjExercise in adjustedDay2.exercises) {
            assertFalse(
                "Push A exercise ${adjExercise.baseExercise.name} must never receive carb bonus set",
                adjExercise.isCarbBonusApplied
            )
            assertEquals(
                "Push A exercise ${adjExercise.baseExercise.name} must maintain baseline sets",
                adjExercise.baseExercise.baselineSets,
                adjExercise.effectiveSets
            )
        }

        // Day 5 (Push B) has NO Lat Pulldowns or Squats
        val day5 = repository.getDayPlan(5)
        val adjustedDay5 = engine.evaluateWorkout(day5, highCarbMacros)

        for (adjExercise in adjustedDay5.exercises) {
            assertFalse(
                "Push B exercise ${adjExercise.baseExercise.name} must never receive carb bonus set",
                adjExercise.isCarbBonusApplied
            )
            assertEquals(
                adjExercise.baseExercise.baselineSets,
                adjExercise.effectiveSets
            )
        }
    }

    // =========================================================================
    // SECTION 4: Adversarial StepAlertEvaluator Stress Tests
    // =========================================================================

    @Test
    fun testStepAlert_minuteByMinute24HourGridSearch() {
        // Test all 1,440 minutes in a 24-hour day with currentSteps = 4,999 (sub-threshold)
        var totalAlertsTriggered = 0
        var totalMinutesEvaluated = 0

        for (hour in 0..23) {
            for (minute in 0..59) {
                totalMinutesEvaluated++
                val result = stepEvaluator.evaluate(currentSteps = 4999, hourOfDay = hour, minuteOfHour = minute)

                if (hour < 18) {
                    assertFalse(
                        "Alert must NOT trigger before 18:00 (failed at $hour:$minute)",
                        result.isAlertTriggered
                    )
                    assertNull(result.alertTitle)
                } else {
                    assertTrue(
                        "Alert MUST trigger at/after 18:00 for 4,999 steps (failed at $hour:$minute)",
                        result.isAlertTriggered
                    )
                    assertNotNull(result.alertTitle)
                    totalAlertsTriggered++
                }
            }
        }

        assertEquals(1440, totalMinutesEvaluated)
        // From 18:00 to 23:59 is 6 hours * 60 minutes = 360 minutes
        assertEquals(360, totalAlertsTriggered)
    }

    @Test
    fun testStepAlert_stepBoundaryStepGridAt1800() {
        // Test steps from 4,950 to 5,050 at exactly 18:00
        for (steps in 4950..5050) {
            val result = stepEvaluator.evaluate(currentSteps = steps, hourOfDay = 18, minuteOfHour = 0)

            if (steps < 5000) {
                assertTrue("Steps $steps at 18:00 must trigger alert", result.isAlertTriggered)
                assertNotNull(result.alertTitle)
            } else {
                assertFalse("Steps $steps at 18:00 must suppress alert", result.isAlertTriggered)
                assertNull(result.alertTitle)
            }
        }
    }

    @Test
    fun testStepAlert_negativeSteps_triggersAlertWithoutCrash() {
        val result = stepEvaluator.evaluate(currentSteps = -100, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("Negative steps must evaluate as sub-threshold and trigger alert", result.isAlertTriggered)
        assertEquals(-100, result.currentSteps)
        assertNotNull(result.alertMessage)
    }

    @Test
    fun testStepAlert_extremeSteps_100000_suppressesAlert() {
        val result = stepEvaluator.evaluate(currentSteps = 100000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("100,000 steps must suppress alert", result.isAlertTriggered)
    }

    @Test
    fun testStepAlert_integerMaxAndMinValues() {
        val maxResult = stepEvaluator.evaluate(currentSteps = Int.MAX_VALUE, hourOfDay = 18, minuteOfHour = 0)
        assertFalse(maxResult.isAlertTriggered)

        val minResult = stepEvaluator.evaluate(currentSteps = Int.MIN_VALUE, hourOfDay = 18, minuteOfHour = 0)
        assertTrue(minResult.isAlertTriggered)
    }

    @Test
    fun testStepAlert_exactMinuteTransition_1759to1800() {
        val at1759 = stepEvaluator.evaluate(currentSteps = 4500, hourOfDay = 17, minuteOfHour = 59)
        assertFalse("At 17:59:59 alert must be inactive", at1759.isAlertTriggered)

        val at1800 = stepEvaluator.evaluate(currentSteps = 4500, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("At 18:00:00 alert must become active", at1800.isAlertTriggered)
    }

    // =========================================================================
    // SECTION 5: VTaperWorkoutRepository Adversarial Integrity
    // =========================================================================

    @Test
    fun testRepository_invalidDayNumbers_throwIllegalArgumentException() {
        val invalidDays = listOf(0, 8, -1, 100, Int.MIN_VALUE, Int.MAX_VALUE)

        for (day in invalidDays) {
            try {
                repository.getDayPlan(day)
                fail("Day $day should have thrown IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message!!.contains("Day number must be between 1 and 7"))
            }
        }
    }

    @Test
    fun testRepository_exerciseIds_areGloballyUnique() {
        val allPlans = repository.getAllPlans()
        val seenIds = mutableSetOf<String>()

        for (plan in allPlans) {
            for (exercise in plan.exercises) {
                assertFalse(
                    "Duplicate exercise ID detected: ${exercise.id} in Day ${plan.dayNumber}",
                    seenIds.contains(exercise.id)
                )
                seenIds.add(exercise.id)
            }
        }

        // Must have at least 25 unique exercises across all 7 days
        assertTrue("Expected at least 25 unique exercises, got ${seenIds.size}", seenIds.size >= 25)
    }

    @Test
    fun testRepository_allBaselineSetsAndRestTimers_arePhysiologicallyValid() {
        for (day in 1..7) {
            val plan = repository.getDayPlan(day)
            for (exercise in plan.exercises) {
                assertTrue("Baseline sets must be positive for ${exercise.name}", exercise.baselineSets > 0)
                assertTrue("Rep range must not be blank for ${exercise.name}", exercise.repRange.isNotBlank())
                assertTrue("Rest seconds must be non-negative for ${exercise.name}", exercise.restSeconds >= 0)
                assertTrue("Form notes must not be blank for ${exercise.name}", exercise.formNotes.isNotBlank())
            }
        }
    }

    // =========================================================================
    // SECTION 6: High-Throughput Performance & Stress Harness
    // =========================================================================

    @Test
    fun testHighThroughput_compensatoryEngine_10000Evaluations_subSecond() {
        val day1 = repository.getDayPlan(1)
        val sampleMeal = MealNutrition(
            rawQuery = "dal, rice, and mutton",
            mealName = "Mess Mix",
            proteinGrams = 18.0f,
            carbsGrams = 75.0f,
            fatsGrams = 12.0f,
            calories = 480
        )

        val startTime = System.currentTimeMillis()
        for (i in 1..10000) {
            val adjusted = engine.evaluateWorkout(day1, sampleMeal)
            assertEquals(3, adjusted.exercises[0].effectiveSets)
        }
        val duration = System.currentTimeMillis() - startTime

        assertTrue("10,000 CompensatoryEngine evaluations took $duration ms (must be < 2,000ms)", duration < 2000)
    }

    @Test
    fun testHighThroughput_stepEvaluator_10000Evaluations_subSecond() {
        val startTime = System.currentTimeMillis()
        for (i in 1..10000) {
            val res = stepEvaluator.evaluate(currentSteps = 4500, hourOfDay = 18, minuteOfHour = 30)
            assertTrue(res.isAlertTriggered)
        }
        val duration = System.currentTimeMillis() - startTime

        assertTrue("10,000 StepEvaluator evaluations took $duration ms (must be < 1,000ms)", duration < 1000)
    }
}
