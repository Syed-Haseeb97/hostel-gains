package com.hostelgains.e2e

import com.hostelgains.domain.nutrition.DailyMacroSummary
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
 * Tier 4: Real-World Application Scenarios Test Suite.
 *
 * Validates authentic university hostel student journeys from meal logging to dynamic workout
 * adaptation and step sync:
 * - Scenario 1: Indian Hostel Vegetarian Day (Dal-rice lunch -> evening Push A workout volume ceiling)
 * - Scenario 2: Sunday Hostel Feast (Mutton curry + 4 dry rotis + eggs -> carb bonus +1 set, high protein)
 * - Scenario 3: Exam Crunch Desk Day (Sedentary student with 2,350 steps at 18:30 -> alert triggered)
 * - Scenario 4: High-Volume V-Taper Specialization Day (FST-7 Lat Pulldowns: optimal vs deficit parity)
 * - Scenario 5: Day 7 Active Rest Recovery Cycle (Evening deficit alert -> walk fulfillment -> goal clear)
 */
class Tier4HostelScenariosTest {

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
    fun testTier4_01_indianHostelVegetarianDay_dalRiceCatabolicRisk() = runBlocking {
        // Step 1: Student logs hostel mess lunch of "dal and rice"
        val meal = parser.parseMeal("dal and rice").getOrThrow()
        assertEquals(8.5f, meal.proteinGrams, 0.1f)
        assertEquals(60.0f, meal.carbsGrams, 0.1f)
        assertTrue("Dal and rice is high carb", meal.isHighCarb)
        assertTrue("Dal and rice creates protein deficit", meal.isLowProtein)

        // Step 2: Evening Push A workout (Day 2) is scheduled
        val day2Base = repository.getDayPlan(2)
        assertEquals(4, day2Base.exercises.size)

        // Step 3: Compensatory Engine dynamically adapts workout to protect against catabolism
        val adjusted = engine.evaluateWorkout(day2Base, meal)

        val inclineDbPress = adjusted.exercises.first { it.baseExercise.id == "d2_incline_db_press" }
        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d2_db_lateral_raises" }
        val inclinePushups = adjusted.exercises.first { it.baseExercise.id == "d2_incline_pushups" }
        val tricepPushdowns = adjusted.exercises.first { it.baseExercise.id == "d2_tricep_rope_pushdowns" }

        // All movements capped at max 3 sets
        assertEquals("Incline DB Press capped at 3 sets (from 4)", 3, inclineDbPress.effectiveSets)
        assertTrue(inclineDbPress.isProteinCapApplied)

        assertEquals("DB Lateral Raises capped at 3 sets (from 5)", 3, lateralRaises.effectiveSets)
        assertTrue(lateralRaises.isProteinCapApplied)

        assertEquals("Incline Push-ups maintained at 3 sets", 3, inclinePushups.effectiveSets)
        assertEquals("Tricep Rope Pushdowns maintained at 3 sets", 3, tricepPushdowns.effectiveSets)

        // Verify audit note explains the rationale to the student
        assertTrue(inclineDbPress.adjustmentNotes.any { it.contains("Protein Deficit") })
    }

    @Test
    fun testTier4_02_hostelSundayFeast_muttonHypertrophySurplus() = runBlocking {
        // Step 1: Student logs Sunday special meal: "mutton, 4 dry rotis, eggs"
        val feast = parser.parseMeal("mutton, 4 dry rotis, eggs").getOrThrow()
        assertEquals(48.0f, feast.proteinGrams, 0.1f)
        assertEquals(74.0f, feast.carbsGrams, 0.1f)
        assertTrue(feast.isHighCarb)
        assertFalse(feast.isLowProtein)

        // Step 2: Evening Pull A session (Day 1)
        val day1Base = repository.getDayPlan(1)
        val adjusted = engine.evaluateWorkout(day1Base, feast)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val cableRows = adjusted.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val rearDeltFlyes = adjusted.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val hammerCurls = adjusted.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        // Carb bonus expands Lat Pulldowns to 5 sets for hypertrophy; no cap applied
        assertEquals("Lat Pulldowns receives +1 set (4 -> 5)", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)

        // Baseline volume intact across all other exercises
        assertEquals(3, cableRows.effectiveSets)
        assertEquals(4, rearDeltFlyes.effectiveSets)
        assertEquals(3, hammerCurls.effectiveSets)
    }

    @Test
    fun testTier4_03_examCrunchSedentaryDay_stepAlertTriggered() {
        // Midterm exam day: Student seated at library desk all day
        val examStepsAt630Pm = 2350
        val alert = stepEvaluator.evaluate(currentSteps = examStepsAt630Pm, hourOfDay = 18, minuteOfHour = 30)

        // UI Alert triggers with warning and 10,000-step evening prompt
        assertTrue("Alert must trigger for sedentary exam day", alert.isAlertTriggered)
        assertEquals(2350, alert.currentSteps)
        assertEquals(10000, alert.targetSteps)
        assertEquals("Step Deficit Alert (Sub-5,000 at 6:00 PM)", alert.alertTitle)
        assertTrue(alert.alertMessage!!.contains("10,000-step"))
    }

    @Test
    fun testTier4_04_fst7WeakPointPumpSpecialization_optimalVsDeficitParity() = runBlocking {
        val day6Base = repository.getDayPlan(6)

        // Condition A: Optimal vegetarian power day (Paneer Bhurji & Parathas: 27g protein, 68g carbs)
        val optimalMeal = parser.parseMeal("paneer bhurji and 3 parathas").getOrThrow()
        val adjustedOptimal = engine.evaluateWorkout(day6Base, optimalMeal)
        val optimalLatPulldowns = adjustedOptimal.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        // FST-7 Lat Pulldowns gets carb bonus: 7 -> 8 sets!
        assertEquals("Optimal nutrition unlocks 8 sets on FST-7", 8, optimalLatPulldowns.effectiveSets)
        assertTrue(optimalLatPulldowns.isCarbBonusApplied)
        assertFalse(optimalLatPulldowns.isProteinCapApplied)

        // Condition B: Deficit day (Dal and Rice: 8.5g protein, 60g carbs)
        val deficitMeal = parser.parseMeal("dal and rice").getOrThrow()
        val adjustedDeficit = engine.evaluateWorkout(day6Base, deficitMeal)
        val deficitLatPulldowns = adjustedDeficit.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        // FST-7 Lat Pulldowns strictly capped at 3 sets for safety
        assertEquals("Protein deficit collapses FST-7 to 3 sets", 3, deficitLatPulldowns.effectiveSets)
        assertTrue(deficitLatPulldowns.isProteinCapApplied)
    }

    @Test
    fun testTier4_05_recompositionDay7ActiveRest_walkTargetSync() {
        // Day 7 Active Rest
        val day7Base = repository.getDayPlan(7)
        assertTrue(day7Base.isActiveRest)

        // At 18:00 (6:00 PM), student has logged 4,200 steps
        val alertAt6Pm = stepEvaluator.evaluate(currentSteps = 4200, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("Sub-5,000 steps triggers alert at 6 PM", alertAt6Pm.isAlertTriggered)

        // Student takes post-dinner walk adding 6,100 steps -> total 10,300 steps
        val stepsAt930Pm = 4200 + 6100
        val alertAt930Pm = stepEvaluator.evaluate(currentSteps = stepsAt930Pm, hourOfDay = 21, minuteOfHour = 30)

        // Alert is cleared, daily 10k target achieved
        assertFalse("Alert clears once 10,000 steps achieved", alertAt930Pm.isAlertTriggered)
        assertEquals(10300, alertAt930Pm.currentSteps)

        // Workout engine confirms zero weight training for recovery
        val adjustedDay7 = engine.evaluateWorkout(day7Base, DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 30.0f,
            totalCarbs = 80.0f,
            totalFats = 15.0f,
            totalCalories = 575
        ))
        assertEquals(1, adjustedDay7.exercises.size)
        assertEquals("Active Recovery Walking", adjustedDay7.exercises[0].baseExercise.name)
    }
}
