package com.hostelgains.domain.rebalance

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Dynamic Exercise Rebalancer & Equipment Fallback Subsystem.
 * Covers:
 * 1. Gemini AI JSON parsing and edge-case recovery.
 * 2. Deterministic offline fallback matrix across all 6 PPL workout days.
 * 3. Natural language skip prompt matching.
 * 4. Precedence harmonization with CompensatoryEngine (High-Carb surplus and strict Protein-Deficit 3-set cap).
 */
class WorkoutRebalancerTest {

    private lateinit var repository: VTaperWorkoutRepository
    private lateinit var offlineRebalancer: OfflineWorkoutRebalancer
    private lateinit var geminiRebalancer: GeminiWorkoutRebalancer

    @Before
    fun setUp() {
        repository = VTaperWorkoutRepository()
        offlineRebalancer = OfflineWorkoutRebalancer()
        geminiRebalancer = GeminiWorkoutRebalancer(apiKey = null, fallback = offlineRebalancer)
    }

    // =========================================================================
    // 1. JSON Parsing Tests
    // =========================================================================

    @Test
    fun testJsonParsing_validDirectJson() {
        val rawJson = """
            {
              "skippedExerciseId": "d1_lat_pulldown",
              "reason": "Machine busy during peak hour",
              "compensations": [
                {
                  "exerciseId": "d1_cable_rows",
                  "extraSets": 2,
                  "repAdjustment": "8-12 reps",
                  "rationale": "Redistributes lat and upper-back volume so weekly V-taper growth is maintained."
                }
              ],
              "coachMessage": "Swapped out Lat Pulldowns. Added 2 sets to Seated Cable Rows to hit your back threshold!"
            }
        """.trimIndent()

        val result = geminiRebalancer.parseRebalanceJsonResponse(rawJson)
        assertNotNull("Must successfully parse valid JSON", result)
        assertEquals("d1_lat_pulldown", result?.skippedExerciseId)
        assertEquals("Machine busy during peak hour", result?.reason)
        assertEquals(1, result?.compensations?.size)

        val comp = result?.compensations?.first()
        assertEquals("d1_cable_rows", comp?.exerciseId)
        assertEquals(2, comp?.extraSets)
        assertEquals("8-12 reps", comp?.repAdjustment)
        assertTrue(result?.isAiGenerated == true)
    }

    @Test
    fun testJsonParsing_markdownFencedJson() {
        val fencedJson = """
            ```json
            {
              "skippedExerciseId": "d2_incline_db_press",
              "reason": "Wrist discomfort on heavy dumbbells",
              "compensations": [
                {
                  "exerciseId": "d2_incline_pushups",
                  "extraSets": 2,
                  "repAdjustment": "To failure",
                  "rationale": "Shifts clavicular pec loading to bodyweight incline push-ups."
                }
              ],
              "coachMessage": "Protected your wrist while keeping upper-chest tension high!"
            }
            ```
        """.trimIndent()

        val result = geminiRebalancer.parseRebalanceJsonResponse(fencedJson)
        assertNotNull("Must strip markdown code fences and parse JSON", result)
        assertEquals("d2_incline_db_press", result?.skippedExerciseId)
        assertEquals("d2_incline_pushups", result?.compensations?.first()?.exerciseId)
    }

    @Test
    fun testJsonParsing_geminiApiEnvelope() {
        val envelopeJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"skippedExerciseId\": \"d3_goblet_squats\",\n  \"reason\": \"Knee stiffness\",\n  \"compensations\": [\n    {\n      \"exerciseId\": \"d3_bw_squats\",\n      \"extraSets\": 2,\n      \"repAdjustment\": \"To failure\",\n      \"rationale\": \"Redistributes quad volume to explosive high-rep bodyweight squats.\"\n    }\n  ],\n  \"coachMessage\": \"Swapped to bodyweight squats for quad burn!\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = geminiRebalancer.parseRebalanceJsonResponse(envelopeJson)
        assertNotNull("Must unpack candidate parts and parse nested JSON", result)
        assertEquals("d3_goblet_squats", result?.skippedExerciseId)
        assertEquals("d3_bw_squats", result?.compensations?.first()?.exerciseId)
    }

    @Test
    fun testJsonParsing_malformedJsonReturnsNull() {
        assertNull(geminiRebalancer.parseRebalanceJsonResponse(""))
        assertNull(geminiRebalancer.parseRebalanceJsonResponse("Not a JSON response"))
        assertNull(geminiRebalancer.parseRebalanceJsonResponse("{ malformed: true }"))
    }

    // =========================================================================
    // 2. Offline Fallback Matrix Across All 6 PPL Workout Days
    // =========================================================================

    @Test
    fun testOfflineRebalancer_day1PullA_latPulldownToCableRows() = runBlocking {
        val day1 = repository.getDayPlan(1)
        val result = offlineRebalancer.rebalanceWorkout(day1, "d1_lat_pulldown").getOrThrow()

        assertEquals("d1_lat_pulldown", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertEquals("d1_cable_rows", comp.exerciseId)
        assertEquals(2, comp.extraSets)
        assertTrue(comp.rationale.contains("lat", ignoreCase = true) || comp.rationale.contains("back", ignoreCase = true))
    }

    @Test
    fun testOfflineRebalancer_day2PushA_inclineDbPressToPushups() = runBlocking {
        val day2 = repository.getDayPlan(2)
        val result = offlineRebalancer.rebalanceWorkout(day2, "d2_incline_db_press").getOrThrow()

        assertEquals("d2_incline_db_press", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertEquals("d2_incline_pushups", comp.exerciseId)
        assertEquals(2, comp.extraSets)
    }

    @Test
    fun testOfflineRebalancer_day3LegsAndCore_gobletSquatsToBwSquats() = runBlocking {
        val day3 = repository.getDayPlan(3)
        val result = offlineRebalancer.rebalanceWorkout(day3, "d3_goblet_squats").getOrThrow()

        assertEquals("d3_goblet_squats", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertEquals("d3_bw_squats", comp.exerciseId)
        assertEquals(2, comp.extraSets)
    }

    @Test
    fun testOfflineRebalancer_day3LegsAndCore_lyingLegCurlsToPlank() = runBlocking {
        val day3 = repository.getDayPlan(3)
        val result = offlineRebalancer.rebalanceWorkout(day3, "d3_lying_leg_curls").getOrThrow()

        assertEquals("d3_lying_leg_curls", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertEquals("d3_plank", comp.exerciseId)
        assertEquals(2, comp.extraSets)
    }

    @Test
    fun testOfflineRebalancer_day4PullB_horizontalRowsToLatPulldowns() = runBlocking {
        val day4 = repository.getDayPlan(4)
        val result = offlineRebalancer.rebalanceWorkout(day4, "d4_db_rows").getOrThrow()

        assertEquals("d4_db_rows", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertEquals("d4_neutral_lat_pulldowns", comp.exerciseId)
        assertEquals(2, comp.extraSets)
    }

    @Test
    fun testOfflineRebalancer_day5PushB_overheadPressToFlatDbPress() = runBlocking {
        val day5 = repository.getDayPlan(5)
        val result = offlineRebalancer.rebalanceWorkout(day5, "d5_oh_db_press").getOrThrow()

        assertEquals("d5_oh_db_press", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        val comp = result.compensations.first()
        assertTrue("Compensated exercise must be in day 5 routine", day5.exercises.any { it.id == comp.exerciseId })
        assertEquals(2, comp.extraSets)
    }

    @Test
    fun testOfflineRebalancer_day6WeakPointPump_fst7LatPulldown() = runBlocking {
        val day6 = repository.getDayPlan(6)
        val result = offlineRebalancer.rebalanceWorkout(day6, "d6_fst7_lat_pulldown").getOrThrow()

        assertEquals("d6_fst7_lat_pulldown", result.skippedExerciseId)
        assertEquals(1, result.compensations.size)
        assertEquals(2, result.compensations.first().extraSets)
    }

    @Test
    fun testOfflineRebalancer_day7ActiveRest_noCompensationRequired() = runBlocking {
        val day7 = repository.getDayPlan(7)
        val result = offlineRebalancer.rebalanceWorkout(day7, "d7_active_recovery_walk").getOrThrow()

        assertTrue("Active rest day produces no compensatory sets", result.compensations.isEmpty())
    }

    // =========================================================================
    // 3. Natural Language Skip Prompt Matching
    // =========================================================================

    @Test
    fun testPromptMatching_noLatPulldownMachine() = runBlocking {
        val day1 = repository.getDayPlan(1)
        val result = offlineRebalancer.rebalanceWorkout(
            dayPlan = day1,
            skippedExerciseId = "",
            userPrompt = "No Lat Pulldown machine today, only dumbbells and floor"
        ).getOrThrow()

        assertEquals("d1_lat_pulldown", result.skippedExerciseId)
        assertEquals("d1_cable_rows", result.compensations.first().exerciseId)
    }

    @Test
    fun testPromptMatching_wristHurtsInclineDbPress() = runBlocking {
        val day2 = repository.getDayPlan(2)
        val result = offlineRebalancer.rebalanceWorkout(
            dayPlan = day2,
            skippedExerciseId = "",
            userPrompt = "Wrist hurts, had to skip Incline DB press"
        ).getOrThrow()

        assertEquals("d2_incline_db_press", result.skippedExerciseId)
        assertEquals("d2_incline_pushups", result.compensations.first().exerciseId)
    }

    @Test
    fun testPromptMatching_couldntDoSquats() = runBlocking {
        val day3 = repository.getDayPlan(3)
        val result = offlineRebalancer.rebalanceWorkout(
            dayPlan = day3,
            skippedExerciseId = "",
            userPrompt = "Couldn't do squats today, rack occupied"
        ).getOrThrow()

        assertEquals("d3_goblet_squats", result.skippedExerciseId)
        assertEquals("d3_bw_squats", result.compensations.first().exerciseId)
    }

    // =========================================================================
    // 4. Harmonization with Macros & Precedence Rules
    // =========================================================================

    @Test
    fun testHarmonizeWithMacros_normalNutrition_addsRebalancedSets() = runBlocking {
        val day1 = repository.getDayPlan(1)
        val rebalanceResult = offlineRebalancer.rebalanceWorkout(day1, "d1_lat_pulldown").getOrThrow()

        // Normal nutrition: 35g protein, 45g carbs (neither high-carb nor protein-deficit)
        val normalMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 35f,
            totalCarbs = 45f,
            totalFats = 15f,
            totalCalories = 455
        )

        val adjustedPlan = offlineRebalancer.harmonizeWithMacros(day1, rebalanceResult, normalMacros)

        // Skipped exercise must have effectiveSets = 0
        val skippedEx = adjustedPlan.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(0, skippedEx.effectiveSets)
        assertTrue(skippedEx.adjustmentNotes.any { it.contains("Skipped", ignoreCase = true) })

        // Compensated exercise: baseline 3 sets + 2 extra sets = 5 sets
        val compEx = adjustedPlan.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        assertEquals(3 + 2, compEx.effectiveSets)
        assertFalse("Protein cap should NOT be applied on normal nutrition", compEx.isProteinCapApplied)
        assertFalse("Carb bonus should NOT be applied on normal nutrition", compEx.isCarbBonusApplied)
    }

    @Test
    fun testHarmonizeWithMacros_highCarbSurplus_addsCarbBonusToEligibleMoves() = runBlocking {
        val day3 = repository.getDayPlan(3)
        // Skip lying leg curls -> compensated to plank
        val rebalanceResult = offlineRebalancer.rebalanceWorkout(day3, "d3_lying_leg_curls").getOrThrow()

        // High carb: 80g carbs, 40g protein
        val highCarbMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 40f,
            totalCarbs = 80f,
            totalFats = 15f,
            totalCalories = 615,
            hasHighCarbTrigger = true
        )

        val adjustedPlan = offlineRebalancer.harmonizeWithMacros(day3, rebalanceResult, highCarbMacros)

        // Goblet Squats is eligible for carb bonus: baseline 4 + 1 carb = 5 sets
        val squatEx = adjustedPlan.exercises.first { it.baseExercise.id == "d3_goblet_squats" }
        assertEquals(4 + 1, squatEx.effectiveSets)
        assertTrue(squatEx.isCarbBonusApplied)

        // Plank compensated: baseline 3 + 2 extra sets = 5 sets
        val plankEx = adjustedPlan.exercises.first { it.baseExercise.id == "d3_plank" }
        assertEquals(3 + 2, plankEx.effectiveSets)
    }

    @Test
    fun testHarmonizeWithMacros_proteinDeficitCap_strictlyOverrulesRebalancedSets() = runBlocking {
        val day1 = repository.getDayPlan(1)
        val rebalanceResult = offlineRebalancer.rebalanceWorkout(day1, "d1_lat_pulldown").getOrThrow()

        // Protein deficit: vegetarian dal and rice meal with 12g protein (< 20g)
        val lowProteinMacros = DailyMacroSummary(
            dateIso = "2026-11-10",
            totalProtein = 12f,
            totalCarbs = 70f, // High carb also present!
            totalFats = 10f,
            totalCalories = 418,
            hasHighCarbTrigger = true,
            hasProteinDeficitTrigger = true
        )

        val adjustedPlan = offlineRebalancer.harmonizeWithMacros(day1, rebalanceResult, lowProteinMacros)

        // Seated Cable Rows: baseline 3 + 2 extra = 5 sets.
        // Under protein deficit safety ceiling, it MUST BE CAPPED AT MAX 3 SETS!
        val compEx = adjustedPlan.exercises.first { it.baseExercise.id == "d1_cable_rows" }
        assertEquals("Protein deficit ceiling must cap rebalanced sets at 3", 3, compEx.effectiveSets)
        assertTrue("isProteinCapApplied must be true", compEx.isProteinCapApplied)

        // All non-skipped exercises must be <= 3 sets
        adjustedPlan.exercises.filter { it.effectiveSets > 0 }.forEach { ex ->
            assertTrue(
                "Exercise ${ex.baseExercise.name} with sets ${ex.effectiveSets} must not exceed 3 sets under protein deficit",
                ex.effectiveSets <= 3
            )
        }

        // Skipped exercise remains 0 sets
        val skippedEx = adjustedPlan.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(0, skippedEx.effectiveSets)
    }
}
