package com.hostelgains.domain.challenger

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.ExerciseCategory
import com.hostelgains.domain.workout.MuscleGroup
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Empirical Adversarial Stress Harness for Milestone M1 (Challenger 2).
 *
 * Targets:
 * 1. Day 6 Weak-Point Pump: FST-7 Lat Pulldowns (7 sets x 14 reps, 45s rest) under normal,
 *    carb surplus, and protein deficit conditions.
 * 2. Day 7 Active Rest: Step target (5,000 to 10,000 steps) with zero resistance exercises
 *    even under extreme carb surplus or protein deficit.
 * 3. Day 1-5 set mutations across all exercises under all 4 macro quadrants.
 * 4. Boundary floating-point thresholds (59.99 vs 60.00 carbs, 19.99 vs 20.00 protein).
 * 5. Step Counter Sync boundary conditions (17:59 vs 18:00, 4,999 vs 5,000 steps).
 * 6. High-concurrency thread safety and baseline plan immutability.
 */
class M1AdversarialStressTest {

    private lateinit var engine: DefaultCompensatoryEngine
    private lateinit var repository: VTaperWorkoutRepository
    private lateinit var stepEvaluator: DefaultStepAlertEvaluator

    @Before
    fun setUp() {
        engine = DefaultCompensatoryEngine()
        repository = VTaperWorkoutRepository()
        stepEvaluator = DefaultStepAlertEvaluator()
    }

    // =========================================================================
    // SECTION 1: Day 6 Weak-Point Pump (FST-7 Protocol Stress-Testing)
    // =========================================================================

    @Test
    fun testDay6_FST7_latPulldown_baselineIntegrity() {
        val day6Plan = repository.getDayPlan(6)
        assertEquals(6, day6Plan.dayNumber)
        assertEquals("Day 6: Weak-Point Pump", day6Plan.dayName)
        assertFalse("Day 6 must not be active rest", day6Plan.isActiveRest)
        assertEquals(3, day6Plan.exercises.size)

        val fst7LatPulldown = day6Plan.exercises.first { it.id == "d6_fst7_lat_pulldown" }
        assertEquals("Lat Pulldowns", fst7LatPulldown.name)
        assertEquals("Baseline sets must be exactly 7 for FST-7", 7, fst7LatPulldown.baselineSets)
        assertEquals("Rep range must be exactly 14 reps for FST-7", "14 reps", fst7LatPulldown.repRange)
        assertEquals("Rest timer must be exactly 45s for FST-7 pump", 45, fst7LatPulldown.restSeconds)
        assertEquals(MuscleGroup.LATS, fst7LatPulldown.targetMuscle)
        assertEquals(ExerciseCategory.COMPOUND_PULL, fst7LatPulldown.category)
        assertTrue(fst7LatPulldown.isCompensatoryEligibleCarb)
        assertTrue(fst7LatPulldown.formNotes.contains("FST-7", ignoreCase = true))
    }

    @Test
    fun testDay6_FST7_latPulldown_carbSurplus_expandsTo8Sets() {
        val highCarbMeal = MealNutrition(
            rawQuery = "mutton biryani with extra rice",
            mealName = "Biryani Surplus",
            proteinGrams = 40.0f,
            carbsGrams = 110.0f,
            fatsGrams = 22.0f,
            calories = 780,
            isHighCarb = true,
            isLowProtein = false
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(6), highCarbMeal)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        // FST-7 Lat Pulldowns expands from 7 to 8 sets
        assertEquals(8, latPulldown.effectiveSets)
        assertEquals("14 reps", latPulldown.effectiveReps)
        assertEquals(45, latPulldown.baseExercise.restSeconds)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)

        // Other Day 6 exercises must NOT receive carb bonus
        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d6_cable_lateral_raises" }
        assertEquals(5, lateralRaises.effectiveSets)
        assertFalse(lateralRaises.isCarbBonusApplied)

        val facePulls = adjusted.exercises.first { it.baseExercise.id == "d6_face_pulls" }
        assertEquals(3, facePulls.effectiveSets)
        assertFalse(facePulls.isCarbBonusApplied)
    }

    @Test
    fun testDay6_FST7_latPulldown_lowProtein_capsAt3Sets() {
        val lowProteinMeal = MealNutrition(
            rawQuery = "plain veg noodles",
            mealName = "Low Protein Mess Dinner",
            proteinGrams = 9.0f,
            carbsGrams = 55.0f,
            fatsGrams = 8.0f,
            calories = 330,
            isHighCarb = false,
            isLowProtein = true
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(6), lowProteinMeal)

        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d6_cable_lateral_raises" }
        val facePulls = adjusted.exercises.first { it.baseExercise.id == "d6_face_pulls" }

        // FST-7 Lat Pulldowns capped from 7 sets down to 3 sets
        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
        assertFalse(latPulldown.isCarbBonusApplied)

        // Lateral raises capped from 5 sets down to 3 sets
        assertEquals(3, lateralRaises.effectiveSets)
        assertTrue(lateralRaises.isProteinCapApplied)

        // Face pulls maintained at 3 sets
        assertEquals(3, facePulls.effectiveSets)

        // All exercises in Day 6 must NOT exceed 3 sets under protein deficit
        adjusted.exercises.forEach {
            assertTrue("Exercise ${it.baseExercise.name} sets (${it.effectiveSets}) exceeded 3", it.effectiveSets <= 3)
        }
    }

    @Test
    fun testDay6_FST7_combinedCarbSurplusAndProteinDeficit_strictCeilingEnforced() {
        // High carb (80g) + low protein (14g)
        val dalRiceMeal = MealNutrition(
            rawQuery = "double rice and dal",
            mealName = "Mess Dal Rice Carb Feast",
            proteinGrams = 14.0f,
            carbsGrams = 80.0f,
            fatsGrams = 6.0f,
            calories = 430,
            isHighCarb = true,
            isLowProtein = true
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(6), dalRiceMeal)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }

        // Tentative sets = 7 + 1 = 8. Effective sets = min(3, 8) = 3!
        assertEquals(3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertTrue(latPulldown.isProteinCapApplied)
        assertEquals("14 reps", latPulldown.effectiveReps)
        assertEquals(45, latPulldown.baseExercise.restSeconds)
    }

    @Test
    fun testDay6_FST7_normalCondition_preservesBaseline() {
        val normalMeal = MealNutrition(
            rawQuery = "4 eggs, 2 rotis",
            mealName = "Balanced Egg Meal",
            proteinGrams = 28.0f,
            carbsGrams = 40.0f,
            fatsGrams = 14.0f,
            calories = 400,
            isHighCarb = false,
            isLowProtein = false
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(6), normalMeal)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d6_fst7_lat_pulldown" }
        val lateralRaises = adjusted.exercises.first { it.baseExercise.id == "d6_cable_lateral_raises" }
        val facePulls = adjusted.exercises.first { it.baseExercise.id == "d6_face_pulls" }

        assertEquals(7, latPulldown.effectiveSets)
        assertEquals(5, lateralRaises.effectiveSets)
        assertEquals(3, facePulls.effectiveSets)
        assertFalse(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)
        assertTrue(adjusted.activeTriggers.isEmpty())
    }

    // =========================================================================
    // SECTION 2: Day 7 Active Rest Stress-Testing
    // =========================================================================

    @Test
    fun testDay7_activeRest_baselineIntegrity() {
        val day7Plan = repository.getDayPlan(7)
        assertEquals(7, day7Plan.dayNumber)
        assertEquals("Day 7: Active Rest", day7Plan.dayName)
        assertTrue("Day 7 must be marked as active rest", day7Plan.isActiveRest)
        assertEquals(1, day7Plan.exercises.size)

        val walk = day7Plan.exercises[0]
        assertEquals("Active Recovery Walking", walk.name)
        assertEquals(MuscleGroup.ACTIVE_RECOVERY, walk.targetMuscle)
        assertEquals(ExerciseCategory.ACTIVE_RECOVERY, walk.category)
        assertEquals(1, walk.baselineSets)
        assertEquals("5,000 - 10,000 steps", walk.repRange)
        assertEquals(0, walk.restSeconds)
        assertFalse(walk.isCompensatoryEligibleCarb)
        assertEquals("5,000 - 10,000 steps only.", day7Plan.guidanceNotes)
    }

    @Test
    fun testDay7_activeRest_extremeCarbSurplus_noResistanceSetsAdded() {
        val massiveCarbMeal = MealNutrition(
            rawQuery = "sweets, halwa, 6 rotis, 2 plates rice",
            mealName = "Extreme Carb Bomb",
            proteinGrams = 60.0f,
            carbsGrams = 350.0f,
            fatsGrams = 40.0f,
            calories = 2000,
            isHighCarb = true,
            isLowProtein = false
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(7), massiveCarbMeal)

        // Must strictly preserve active recovery walking only
        assertEquals(1, adjusted.exercises.size)
        val walk = adjusted.exercises[0]
        assertEquals("Active Recovery Walking", walk.baseExercise.name)
        assertEquals("Walking sets must remain 1 even under massive carb load", 1, walk.effectiveSets)
        assertEquals("5,000 - 10,000 steps", walk.effectiveReps)
        assertFalse("Carb bonus must NEVER be applied on active rest day", walk.isCarbBonusApplied)
        assertFalse(walk.isProteinCapApplied)
        assertTrue("Active rest plan must have zero active triggers that add resistance", adjusted.activeTriggers.isEmpty())
    }

    @Test
    fun testDay7_activeRest_severeProteinDeficit_noDeleteriousCap() {
        val zeroProteinMeal = MealNutrition(
            rawQuery = "sugar water and glucose biscuits",
            mealName = "Zero Protein Snack",
            proteinGrams = 1.0f,
            carbsGrams = 45.0f,
            fatsGrams = 2.0f,
            calories = 200,
            isHighCarb = false,
            isLowProtein = true
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(7), zeroProteinMeal)
        assertEquals(1, adjusted.exercises.size)
        val walk = adjusted.exercises[0]
        assertEquals(1, walk.effectiveSets)
        assertFalse(walk.isProteinCapApplied)
        assertEquals("5,000 - 10,000 steps", walk.effectiveReps)
    }

    @Test
    fun testDay7_activeRest_combinedExtremeMacros_zeroResistanceExercisesLeaked() {
        val extremeMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 5.0f,
            totalCarbs = 500.0f,
            totalFats = 50.0f,
            totalCalories = 2470,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = true,
            mealCount = 3
        )

        val adjusted = engine.evaluateWorkout(repository.getDayPlan(7), extremeMacros)
        assertEquals(1, adjusted.exercises.size)
        val walk = adjusted.exercises[0]
        assertEquals(1, walk.effectiveSets)
        assertEquals("Active Recovery Walking", walk.baseExercise.name)
        assertEquals(MuscleGroup.ACTIVE_RECOVERY, walk.baseExercise.targetMuscle)
        assertEquals(ExerciseCategory.ACTIVE_RECOVERY, walk.baseExercise.category)

        // Verify zero resistance exercises exist
        val resistanceExercises = adjusted.exercises.filter {
            it.baseExercise.targetMuscle != MuscleGroup.ACTIVE_RECOVERY
        }
        assertTrue("Zero resistance exercises must exist in Day 7", resistanceExercises.isEmpty())
    }

    // =========================================================================
    // SECTION 3: Days 1 to 5 Set Mutation Matrix Across All 4 Macro Quadrants
    // =========================================================================

    @Test
    fun testDay1_pullA_mutationsAcrossAllFourQuadrants() {
        val day1 = repository.getDayPlan(1)

        // Q1: Balanced (carbs=40, protein=30)
        val q1 = engine.evaluateWorkout(day1, DailyMacroSummary("2026-11-10", 30f, 40f, 10f, 370))
        assertEquals(listOf(4, 3, 4, 3), q1.exercises.map { it.effectiveSets })

        // Q2: High Carb Surplus (carbs=75, protein=30) -> Lat Pulldowns +1
        val q2 = engine.evaluateWorkout(day1, DailyMacroSummary("2026-11-10", 30f, 75f, 10f, 510))
        assertEquals(listOf(5, 3, 4, 3), q2.exercises.map { it.effectiveSets })
        assertTrue(q2.exercises[0].isCarbBonusApplied)

        // Q3: Low Protein Deficit (carbs=40, protein=12) -> All capped at 3
        val q3 = engine.evaluateWorkout(day1, DailyMacroSummary("2026-11-10", 12f, 40f, 10f, 298))
        assertEquals(listOf(3, 3, 3, 3), q3.exercises.map { it.effectiveSets })
        assertTrue(q3.exercises[0].isProteinCapApplied)
        assertTrue(q3.exercises[2].isProteinCapApplied)

        // Q4: High Carb + Low Protein (carbs=80, protein=12) -> min(3, 4+1) = 3
        val q4 = engine.evaluateWorkout(day1, DailyMacroSummary("2026-11-10", 12f, 80f, 10f, 458))
        assertEquals(listOf(3, 3, 3, 3), q4.exercises.map { it.effectiveSets })
        assertTrue(q4.exercises[0].isCarbBonusApplied)
        assertTrue(q4.exercises[0].isProteinCapApplied)
    }

    @Test
    fun testDay2_pushA_mutationsAcrossAllFourQuadrants() {
        val day2 = repository.getDayPlan(2)

        // Q1: Balanced
        val q1 = engine.evaluateWorkout(day2, DailyMacroSummary("2026-11-10", 30f, 40f, 10f, 370))
        assertEquals(listOf(4, 5, 3, 3), q1.exercises.map { it.effectiveSets })

        // Q2: High Carb -> Push A exercises are NOT Lat Pulldowns or Squats, so sets unchanged
        val q2 = engine.evaluateWorkout(day2, DailyMacroSummary("2026-11-10", 30f, 80f, 10f, 530))
        assertEquals(listOf(4, 5, 3, 3), q2.exercises.map { it.effectiveSets })
        assertFalse(q2.exercises.any { it.isCarbBonusApplied })

        // Q3: Low Protein -> 4 and 5 capped at 3
        val q3 = engine.evaluateWorkout(day2, DailyMacroSummary("2026-11-10", 15f, 40f, 10f, 310))
        assertEquals(listOf(3, 3, 3, 3), q3.exercises.map { it.effectiveSets })
        assertTrue(q3.exercises[0].isProteinCapApplied)
        assertTrue(q3.exercises[1].isProteinCapApplied)

        // Q4: High Carb + Low Protein
        val q4 = engine.evaluateWorkout(day2, DailyMacroSummary("2026-11-10", 15f, 80f, 10f, 470))
        assertEquals(listOf(3, 3, 3, 3), q4.exercises.map { it.effectiveSets })
    }

    @Test
    fun testDay3_legsAndCore_mutationsAcrossAllFourQuadrants() {
        val day3 = repository.getDayPlan(3)
        // Baseline: Goblet Squats (4), Bodyweight Squats (2), Lying Leg Curls (3), Plank (3)

        // Q1: Balanced
        val q1 = engine.evaluateWorkout(day3, DailyMacroSummary("2026-11-10", 30f, 40f, 10f, 370))
        assertEquals(listOf(4, 2, 3, 3), q1.exercises.map { it.effectiveSets })

        // Q2: High Carb -> Goblet Squats (4->5), BW Squats (2->3)
        val q2 = engine.evaluateWorkout(day3, DailyMacroSummary("2026-11-10", 30f, 85f, 10f, 550))
        assertEquals(listOf(5, 3, 3, 3), q2.exercises.map { it.effectiveSets })
        assertTrue(q2.exercises[0].isCarbBonusApplied)
        assertTrue(q2.exercises[1].isCarbBonusApplied)
        assertFalse(q2.exercises[2].isCarbBonusApplied)
        assertFalse(q2.exercises[3].isCarbBonusApplied)

        // Q3: Low Protein -> Goblet Squats capped to 3, BW Squats remains 2 (baseline 2 <= 3)
        val q3 = engine.evaluateWorkout(day3, DailyMacroSummary("2026-11-10", 10f, 40f, 10f, 290))
        assertEquals(listOf(3, 2, 3, 3), q3.exercises.map { it.effectiveSets })
        assertTrue(q3.exercises[0].isProteinCapApplied)
        assertFalse(q3.exercises[1].isProteinCapApplied) // 2 does not exceed 3

        // Q4: High Carb + Low Protein -> Goblet Squats min(3, 4+1) = 3; BW Squats min(3, 2+1) = 3
        val q4 = engine.evaluateWorkout(day3, DailyMacroSummary("2026-11-10", 10f, 85f, 10f, 470))
        assertEquals(listOf(3, 3, 3, 3), q4.exercises.map { it.effectiveSets })
        assertTrue(q4.exercises[0].isCarbBonusApplied)
        assertTrue(q4.exercises[0].isProteinCapApplied)
        assertTrue(q4.exercises[1].isCarbBonusApplied)
    }

    @Test
    fun testDay4_pullB_mutationsAcrossAllFourQuadrants() {
        val day4 = repository.getDayPlan(4)
        // Exercises: Chest-Supported DB Rows (4), Neutral-Grip Lat Pulldowns (3), Incline Curls (3), Preacher Curls (3), Face Pulls (3)

        // Q1: Balanced
        val q1 = engine.evaluateWorkout(day4, DailyMacroSummary("2026-11-10", 30f, 40f, 10f, 370))
        assertEquals(listOf(4, 3, 3, 3, 3), q1.exercises.map { it.effectiveSets })

        // Q2: High Carb -> Neutral Lat Pulldowns gets +1 (3->4)
        val q2 = engine.evaluateWorkout(day4, DailyMacroSummary("2026-11-10", 30f, 90f, 10f, 570))
        assertEquals(listOf(4, 4, 3, 3, 3), q2.exercises.map { it.effectiveSets })
        assertTrue(q2.exercises[1].isCarbBonusApplied)

        // Q3: Low Protein -> DB Rows capped at 3
        val q3 = engine.evaluateWorkout(day4, DailyMacroSummary("2026-11-10", 15f, 40f, 10f, 310))
        assertEquals(listOf(3, 3, 3, 3, 3), q3.exercises.map { it.effectiveSets })
        assertTrue(q3.exercises[0].isProteinCapApplied)

        // Q4: High Carb + Low Protein -> Neutral Lat Pulldown min(3, 3+1) = 3
        val q4 = engine.evaluateWorkout(day4, DailyMacroSummary("2026-11-10", 15f, 90f, 10f, 510))
        assertEquals(listOf(3, 3, 3, 3, 3), q4.exercises.map { it.effectiveSets })
        assertTrue(q4.exercises[1].isCarbBonusApplied)
        assertTrue(q4.exercises[1].isProteinCapApplied)
    }

    @Test
    fun testDay5_pushB_mutationsAcrossAllFourQuadrants() {
        val day5 = repository.getDayPlan(5)
        // Exercises: Standing Overhead DB Press (4), Cable Lateral Raises (5), Flat DB Press (3), OH Tricep Ext (3), Dips (3)

        // Q1: Balanced
        val q1 = engine.evaluateWorkout(day5, DailyMacroSummary("2026-11-10", 30f, 40f, 10f, 370))
        assertEquals(listOf(4, 5, 3, 3, 3), q1.exercises.map { it.effectiveSets })

        // Q2: High Carb -> None eligible, sets unchanged
        val q2 = engine.evaluateWorkout(day5, DailyMacroSummary("2026-11-10", 30f, 100f, 10f, 610))
        assertEquals(listOf(4, 5, 3, 3, 3), q2.exercises.map { it.effectiveSets })

        // Q3: Low Protein -> 4 and 5 capped at 3
        val q3 = engine.evaluateWorkout(day5, DailyMacroSummary("2026-11-10", 14f, 40f, 10f, 306))
        assertEquals(listOf(3, 3, 3, 3, 3), q3.exercises.map { it.effectiveSets })
        assertTrue(q3.exercises[0].isProteinCapApplied)
        assertTrue(q3.exercises[1].isProteinCapApplied)

        // Q4: High Carb + Low Protein
        val q4 = engine.evaluateWorkout(day5, DailyMacroSummary("2026-11-10", 14f, 100f, 10f, 546))
        assertEquals(listOf(3, 3, 3, 3, 3), q4.exercises.map { it.effectiveSets })
    }

    // =========================================================================
    // SECTION 4: Boundary Floating-Point Thresholds & Extreme Value Hardening
    // =========================================================================

    @Test
    fun testCarbThreshold_boundaryPrecision_59_99g_vs_60_00g() {
        val day1 = repository.getDayPlan(1)

        // 59.99g carbs -> INACTIVE (must NOT trigger carb bonus)
        val subThreshold = DailyMacroSummary("2026-11-10", 25.0f, 59.99f, 10f, 430)
        assertFalse(subThreshold.hasHighCarbTrigger)
        val adjustedSub = engine.evaluateWorkout(day1, subThreshold)
        assertEquals(4, adjustedSub.exercises[0].effectiveSets)
        assertFalse(adjustedSub.exercises[0].isCarbBonusApplied)

        // 60.00g carbs -> ACTIVE (MUST trigger carb bonus)
        val exactThreshold = DailyMacroSummary("2026-11-10", 25.0f, 60.00f, 10f, 430)
        assertTrue(exactThreshold.hasHighCarbTrigger)
        val adjustedExact = engine.evaluateWorkout(day1, exactThreshold)
        assertEquals(5, adjustedExact.exercises[0].effectiveSets)
        assertTrue(adjustedExact.exercises[0].isCarbBonusApplied)

        // 60.01g carbs -> ACTIVE
        val superThreshold = DailyMacroSummary("2026-11-10", 25.0f, 60.01f, 10f, 430)
        assertTrue(superThreshold.hasHighCarbTrigger)
        val adjustedSuper = engine.evaluateWorkout(day1, superThreshold)
        assertEquals(5, adjustedSuper.exercises[0].effectiveSets)
        assertTrue(adjustedSuper.exercises[0].isCarbBonusApplied)
    }

    @Test
    fun testProteinThreshold_boundaryPrecision_19_99g_vs_20_00g() {
        val day1 = repository.getDayPlan(1)

        // 19.99g protein -> DEFICIT ACTIVE (must cap at 3 sets)
        val deficitActive = DailyMacroSummary("2026-11-10", 19.99f, 40.0f, 10f, 330)
        assertTrue(deficitActive.hasProteinDeficitTrigger)
        val adjustedDeficit = engine.evaluateWorkout(day1, deficitActive)
        assertEquals(3, adjustedDeficit.exercises[0].effectiveSets)
        assertTrue(adjustedDeficit.exercises[0].isProteinCapApplied)

        // 20.00g protein -> SUFFICIENT (must NOT cap at 3 sets)
        val sufficient = DailyMacroSummary("2026-11-10", 20.00f, 40.0f, 10f, 330)
        assertFalse(sufficient.hasProteinDeficitTrigger)
        val adjustedSufficient = engine.evaluateWorkout(day1, sufficient)
        assertEquals(4, adjustedSufficient.exercises[0].effectiveSets)
        assertFalse(adjustedSufficient.exercises[0].isProteinCapApplied)

        // 20.01g protein -> SUFFICIENT
        val superSufficient = DailyMacroSummary("2026-11-10", 20.01f, 40.0f, 10f, 330)
        assertFalse(superSufficient.hasProteinDeficitTrigger)
        val adjustedSuper = engine.evaluateWorkout(day1, superSufficient)
        assertEquals(4, adjustedSuper.exercises[0].effectiveSets)
        assertFalse(adjustedSuper.exercises[0].isProteinCapApplied)
    }

    @Test
    fun testZeroAndNegativeMacros_handledGracefully() {
        val day1 = repository.getDayPlan(1)

        // 0g carbs, 0g protein -> protein deficit active, no crashes
        val zeroMacros = DailyMacroSummary("2026-11-10", 0.0f, 0.0f, 0.0f, 0)
        val adjustedZero = engine.evaluateWorkout(day1, zeroMacros)
        assertTrue(adjustedZero.exercises.all { it.effectiveSets <= 3 })

        // Negative values (adultered / corrupt input)
        val negativeMacros = DailyMacroSummary("2026-11-10", -15.0f, -60.0f, -5.0f, 0)
        val adjustedNegative = engine.evaluateWorkout(day1, negativeMacros)
        assertTrue(adjustedNegative.exercises.all { it.effectiveSets <= 3 })
        assertTrue("Effective sets must remain positive", adjustedNegative.exercises.all { it.effectiveSets > 0 })
    }

    @Test
    fun testAstronomicalMacros_handledGracefully() {
        val day1 = repository.getDayPlan(1)
        val astronomical = DailyMacroSummary("2026-11-10", 50000.0f, 100000.0f, 10000.0f, 700000)
        val adjusted = engine.evaluateWorkout(day1, astronomical)
        assertEquals(5, adjusted.exercises[0].effectiveSets) // Lat pulldowns +1
        assertFalse(adjusted.exercises[0].isProteinCapApplied)
    }

    // =========================================================================
    // SECTION 5: Step Counter Sync Boundary & Alert Timing Stress Tests
    // =========================================================================

    @Test
    fun testStepAlert_timeCutoffBoundary_1759_vs_1800() {
        // 17:59 (before 6:00 PM cutoff) -> alert must NEVER trigger even if steps = 0
        val res1759 = stepEvaluator.evaluate(currentSteps = 0, hourOfDay = 17, minuteOfHour = 59)
        assertFalse("Alert must not trigger before 18:00", res1759.isAlertTriggered)

        // 18:00 (exact 6:00 PM cutoff) with steps < 5000 -> alert MUST trigger
        val res1800 = stepEvaluator.evaluate(currentSteps = 4999, hourOfDay = 18, minuteOfHour = 0)
        assertTrue("Alert must trigger at 18:00 with 4,999 steps", res1800.isAlertTriggered)
        assertNotNull(res1800.alertTitle)
        assertNotNull(res1800.alertMessage)
        assertTrue(res1800.alertMessage!!.contains("10,000-step walk", ignoreCase = true))

        // 18:01 -> alert triggers
        val res1801 = stepEvaluator.evaluate(currentSteps = 1200, hourOfDay = 18, minuteOfHour = 1)
        assertTrue(res1801.isAlertTriggered)
    }

    @Test
    fun testStepAlert_stepCountBoundary_4999_vs_5000() {
        // 4,999 steps at 18:00 -> alert TRIGGERED (under 5,000)
        val res4999 = stepEvaluator.evaluate(currentSteps = 4999, hourOfDay = 18, minuteOfHour = 0)
        assertTrue(res4999.isAlertTriggered)

        // 5,000 steps at 18:00 -> alert NOT TRIGGERED (meets 5,000 threshold)
        val res5000 = stepEvaluator.evaluate(currentSteps = 5000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse("Alert must not trigger if steps equal 5,000", res5000.isAlertTriggered)

        // 5,001 steps at 18:00 -> alert NOT TRIGGERED
        val res5001 = stepEvaluator.evaluate(currentSteps = 5001, hourOfDay = 18, minuteOfHour = 0)
        assertFalse(res5001.isAlertTriggered)
    }

    @Test
    fun testStepAlert_lateNightAndExtremeSteps() {
        // 23:59 with 2500 steps -> alert triggered
        val res2359 = stepEvaluator.evaluate(currentSteps = 2500, hourOfDay = 23, minuteOfHour = 59)
        assertTrue(res2359.isAlertTriggered)

        // 18:30 with 0 steps -> alert triggered
        val resZero = stepEvaluator.evaluate(currentSteps = 0, hourOfDay = 18, minuteOfHour = 30)
        assertTrue(resZero.isAlertTriggered)

        // 19:00 with negative steps (corrupt sensor) -> alert triggered
        val resNeg = stepEvaluator.evaluate(currentSteps = -500, hourOfDay = 19, minuteOfHour = 0)
        assertTrue(resNeg.isAlertTriggered)

        // 18:00 with 25,000 steps -> alert false
        val resHigh = stepEvaluator.evaluate(currentSteps = 25000, hourOfDay = 18, minuteOfHour = 0)
        assertFalse(resHigh.isAlertTriggered)
    }

    // =========================================================================
    // SECTION 6: High-Concurrency Thread Safety & Immutability Verification
    // =========================================================================

    @Test
    fun testRepository_immutabilityAcrossRepeatedEvaluations() {
        val originalDay1 = repository.getDayPlan(1)
        val originalDay6 = repository.getDayPlan(6)
        val originalDay7 = repository.getDayPlan(7)

        // Run severe set modifications
        val lowProteinMeal = MealNutrition("low protein", "test", 5f, 40f, 5f, 225, isLowProtein = true)
        val highCarbMeal = MealNutrition("high carb", "test", 35f, 150f, 10f, 830, isHighCarb = true)

        engine.evaluateWorkout(originalDay1, lowProteinMeal)
        engine.evaluateWorkout(originalDay6, lowProteinMeal)
        engine.evaluateWorkout(originalDay6, highCarbMeal)
        engine.evaluateWorkout(originalDay7, highCarbMeal)

        // Re-fetch from repository and verify original baseline sets are completely unchanged
        val freshDay1 = repository.getDayPlan(1)
        assertEquals(4, freshDay1.exercises[0].baselineSets)

        val freshDay6 = repository.getDayPlan(6)
        assertEquals(7, freshDay6.exercises[0].baselineSets)
        assertEquals(5, freshDay6.exercises[1].baselineSets)
        assertEquals(3, freshDay6.exercises[2].baselineSets)

        val freshDay7 = repository.getDayPlan(7)
        assertEquals(1, freshDay7.exercises[0].baselineSets)
        assertEquals("5,000 - 10,000 steps", freshDay7.exercises[0].repRange)
    }

    @Test
    fun testCompensatoryEngine_multiThreadedEvaluation_threadSafety() {
        val executor = Executors.newFixedThreadPool(16)
        val tasks = mutableListOf<Callable<Boolean>>()

        for (i in 0 until 100) {
            val dayNum = (i % 7) + 1
            val carbs = (30 + (i % 80)).toFloat()
            val protein = (10 + (i % 40)).toFloat()

            tasks.add(Callable {
                val plan = repository.getDayPlan(dayNum)
                val summary = DailyMacroSummary("2026-11-10", protein, carbs, 15f, 400)
                val adjusted = engine.evaluateWorkout(plan, summary)

                // Invariants to verify per result
                if (plan.isActiveRest) {
                    adjusted.exercises.size == 1 && adjusted.exercises[0].effectiveSets == 1
                } else if (summary.hasProteinDeficitTrigger) {
                    adjusted.exercises.all { it.effectiveSets <= 3 }
                } else {
                    adjusted.exercises.all { it.effectiveSets >= 2 }
                }
            })
        }

        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        assertTrue("Executor must terminate cleanly within 10 seconds", executor.awaitTermination(10, TimeUnit.SECONDS))

        for (future in futures) {
            assertTrue("Concurrent evaluation must satisfy all domain invariants", future.get())
        }
    }
}
