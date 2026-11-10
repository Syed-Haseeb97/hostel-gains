package com.hostelgains.domain

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Genuine automated unit tests verifying the Dynamic Compensatory Workout Engine rules.
 */
class CompensatoryEngineTest {

    private lateinit var engine: DefaultCompensatoryEngine
    private lateinit var repository: VTaperWorkoutRepository

    @Before
    fun setUp() {
        engine = DefaultCompensatoryEngine()
        repository = VTaperWorkoutRepository()
    }

    @Test
    fun evaluateWorkout_highCarbTrigger_addsOneSetToLatPulldownsAndSquats() {
        // High carb meal (75g carbs, 35g protein) -> Carb Surplus Active, Protein Deficit Inactive
        val highCarbMeal = MealNutrition(
            rawQuery = "mutton, 4 rotis, sweet curd",
            mealName = "High Carb Mutton Feast",
            proteinGrams = 35.0f,
            carbsGrams = 75.0f,
            fatsGrams = 18.0f,
            calories = 600,
            isHighCarb = true,
            isLowProtein = false
        )

        // Day 1 (Pull A)
        val day1Plan = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1Plan, highCarbMeal)

        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val cableRows = adjustedDay1.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val rearDeltFlyes = adjustedDay1.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val hammerCurls = adjustedDay1.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        // Lat Pulldowns must increase from baseline 4 to 5 sets
        assertEquals(5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)

        // Non-eligible exercises must maintain baseline sets
        assertEquals(3, cableRows.effectiveSets)
        assertFalse(cableRows.isCarbBonusApplied)
        assertEquals(4, rearDeltFlyes.effectiveSets)
        assertFalse(rearDeltFlyes.isCarbBonusApplied)
        assertEquals(3, hammerCurls.effectiveSets)
        assertFalse(hammerCurls.isCarbBonusApplied)

        // Day 3 (Legs & Core)
        val day3Plan = repository.getDayPlan(3)
        val adjustedDay3 = engine.evaluateWorkout(day3Plan, highCarbMeal)

        val gobletSquats = adjustedDay3.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        val bwSquats = adjustedDay3.exercises.first { it.baseExercise.id == "d3_bw_squats" }
        val legCurls = adjustedDay3.exercises.first { it.baseExercise.id == "d3_lying_leg_curls" }
        val plank = adjustedDay3.exercises.first { it.baseExercise.id == "d3_plank" }

        // Squats must receive +1 set
        assertEquals(5, gobletSquats.effectiveSets) // 4 -> 5
        assertTrue(gobletSquats.isCarbBonusApplied)
        assertEquals(3, bwSquats.effectiveSets)     // 2 -> 3
        assertTrue(bwSquats.isCarbBonusApplied)

        // Other leg/core movements remain baseline
        assertEquals(3, legCurls.effectiveSets)
        assertEquals(3, plank.effectiveSets)
    }

    @Test
    fun evaluateWorkout_proteinDeficitTrigger_capsAllExercisesAtThreeSets() {
        // Low protein meal (12g protein, 45g carbs) -> Protein Deficit Active, Carb Surplus Inactive
        val lowProteinMeal = MealNutrition(
            rawQuery = "small dal and salad",
            mealName = "Vegetarian Low Protein Snack",
            proteinGrams = 12.0f,
            carbsGrams = 45.0f,
            fatsGrams = 5.0f,
            calories = 270,
            isHighCarb = false,
            isLowProtein = true
        )

        // Day 1 (Pull A)
        val day1Plan = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1Plan, lowProteinMeal)

        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val rearDeltFlyes = adjustedDay1.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val cableRows = adjustedDay1.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val hammerCurls = adjustedDay1.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        // Exercises with baseline 4 sets capped at 3
        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
        assertFalse(latPulldown.isCarbBonusApplied)

        assertEquals(3, rearDeltFlyes.effectiveSets)
        assertTrue(rearDeltFlyes.isProteinCapApplied)

        // Exercises with baseline 3 remain 3
        assertEquals(3, cableRows.effectiveSets)
        assertEquals(3, hammerCurls.effectiveSets)

        // Day 6 (Weak-Point Pump with FST-7 Lat Pulldowns)
        val day6Plan = repository.getDayPlan(6)
        val adjustedDay6 = engine.evaluateWorkout(day6Plan, lowProteinMeal)

        val fst7LatPulldown = adjustedDay6.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        val cableLateralRaises = adjustedDay6.exercises.first { it.baseExercise.id == "d6_cable_lateral_raises" }
        val facePulls = adjustedDay6.exercises.first { it.baseExercise.id == "d6_face_pulls" }

        // Baseline 7 sets capped strictly at 3
        assertEquals(3, fst7LatPulldown.effectiveSets)
        assertTrue(fst7LatPulldown.isProteinCapApplied)

        // Baseline 5 sets capped strictly at 3
        assertEquals(3, cableLateralRaises.effectiveSets)
        assertTrue(cableLateralRaises.isProteinCapApplied)

        // Baseline 3 sets remains 3
        assertEquals(3, facePulls.effectiveSets)
    }

    @Test
    fun evaluateWorkout_combinedCarbAndProteinDeficit_enforcesThreeSetCap() {
        // Typical vegetarian hostel dal & rice: 10g protein (<20g deficit) AND 70g carbs (>=60g surplus)
        val dalAndRiceMeal = MealNutrition(
            rawQuery = "dal and rice",
            mealName = "Hostel Dal and White Rice",
            proteinGrams = 10.0f,
            carbsGrams = 70.0f,
            fatsGrams = 8.0f,
            calories = 390,
            isHighCarb = true,
            isLowProtein = true
        )

        val day1Plan = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1Plan, dalAndRiceMeal)

        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }

        // Safety Precedence: Protein cap restricts sets to max 3, overriding the carb addition: min(3, 4 + 1) = 3
        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertTrue(latPulldown.isProteinCapApplied)

        // Day 3 Squats
        val day3Plan = repository.getDayPlan(3)
        val adjustedDay3 = engine.evaluateWorkout(day3Plan, dalAndRiceMeal)

        val gobletSquats = adjustedDay3.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        val bwSquats = adjustedDay3.exercises.first { it.baseExercise.id == "d3_bw_squats" }

        // Goblet squats: 4 + 1 = 5 -> capped at 3
        assertEquals(3, gobletSquats.effectiveSets)
        assertTrue(gobletSquats.isCarbBonusApplied)
        assertTrue(gobletSquats.isProteinCapApplied)

        // Bodyweight squats: 2 + 1 = 3 -> capped at 3
        assertEquals(3, bwSquats.effectiveSets)
        assertTrue(bwSquats.isCarbBonusApplied)
    }

    @Test
    fun evaluateWorkout_normalBalancedMeal_preservesBaselineSets() {
        // Balanced meal (30g protein, 45g carbs)
        val balancedMeal = MealNutrition(
            rawQuery = "chicken breast, roti and salad",
            mealName = "Balanced High Protein Meal",
            proteinGrams = 30.0f,
            carbsGrams = 45.0f,
            fatsGrams = 10.0f,
            calories = 390,
            isHighCarb = false,
            isLowProtein = false
        )

        val day1Plan = repository.getDayPlan(1)
        val adjustedDay1 = engine.evaluateWorkout(day1Plan, balancedMeal)

        val latPulldown = adjustedDay1.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        val cableRows = adjustedDay1.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        val rearDeltFlyes = adjustedDay1.exercises.first { it.baseExercise.id == "d1_rear_delt_flyes" }
        val hammerCurls = adjustedDay1.exercises.first { it.baseExercise.id == "d1_hammer_curls" }

        // Exact baseline preservation
        assertEquals(4, latPulldown.effectiveSets)
        assertEquals(3, cableRows.effectiveSets)
        assertEquals(4, rearDeltFlyes.effectiveSets)
        assertEquals(3, hammerCurls.effectiveSets)

        assertFalse(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)
        assertTrue(adjustedDay1.activeTriggers.isEmpty())
    }

    @Test
    fun evaluateWorkout_activeRestDay_preservesActiveRestPlan() {
        val highCarbMeal = MealNutrition(
            rawQuery = "chole bhature",
            mealName = "Sunday Bhature Treat",
            proteinGrams = 15.0f,
            carbsGrams = 90.0f,
            fatsGrams = 25.0f,
            calories = 645,
            isHighCarb = true,
            isLowProtein = true
        )

        val day7Plan = repository.getDayPlan(7)
        val adjustedDay7 = engine.evaluateWorkout(day7Plan, highCarbMeal)

        assertEquals(1, adjustedDay7.exercises.size)
        assertEquals("Active Recovery Walking", adjustedDay7.exercises[0].baseExercise.name)
        assertEquals(1, adjustedDay7.exercises[0].effectiveSets)
        assertEquals("5,000 - 10,000 steps", adjustedDay7.exercises[0].effectiveReps)
    }
}
