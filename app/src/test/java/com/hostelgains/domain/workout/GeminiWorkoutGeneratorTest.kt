package com.hostelgains.domain.workout

import com.hostelgains.domain.physique.ExperienceLevel
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.domain.physique.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiWorkoutGeneratorTest {

    private fun createMockClient(statusCode: Int, responseBody: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    @Test
    fun testExperienceLevels_haveCorrectProperties() {
        assertEquals(3, ExperienceLevel.ALL_LEVELS.size)

        val beg = ExperienceLevel.fromId("beginner")
        assertEquals(ExperienceLevel.BEGINNER, beg)
        assertTrue(beg.repFocus.contains("10-15"))

        val inter = ExperienceLevel.fromId("intermediate")
        assertEquals(ExperienceLevel.INTERMEDIATE, inter)
        assertTrue(inter.repFocus.contains("8-12"))

        val adv = ExperienceLevel.fromId("advanced")
        assertEquals(ExperienceLevel.ADVANCED, adv)
        assertTrue(adv.repFocus.contains("4-8"))
    }

    @Test
    fun testWorkoutPlanSerializer_roundtrip() {
        val exercises = listOf(
            Exercise(
                id = "ai_test_1",
                name = "Cable Lat Pulldown",
                targetMuscle = MuscleGroup.LATS,
                category = ExerciseCategory.COMPOUND_PULL,
                baselineSets = 3,
                repRange = "10-12 reps",
                restSeconds = 90,
                formNotes = "Focus on lat sweep",
                isCompensatoryEligibleCarb = true
            )
        )
        val plan = WorkoutDayPlan(
            dayNumber = 1,
            dayName = "Day 1: Pull A",
            focusArea = "Lat Hypertrophy",
            exercises = exercises,
            isActiveRest = false,
            guidanceNotes = "Keep strict form"
        )

        val json = WorkoutPlanSerializer.toJson(listOf(plan), "Custom beginner periodization")
        assertTrue(json.contains("Cable Lat Pulldown"))
        assertTrue(json.contains("Custom beginner periodization"))

        val parsed = WorkoutPlanSerializer.fromJson(json)
        assertNotNull(parsed)
        assertEquals("Custom beginner periodization", parsed?.first)
        assertEquals(1, parsed?.second?.size)

        val parsedDay = parsed?.second?.get(0)!!
        assertEquals("Day 1: Pull A", parsedDay.dayName)
        assertEquals(1, parsedDay.exercises.size)
        assertEquals("Cable Lat Pulldown", parsedDay.exercises[0].name)
        assertEquals(MuscleGroup.LATS, parsedDay.exercises[0].targetMuscle)
    }

    @Test
    fun testGeminiWorkoutGenerator_whenApiKeyMissing_returnsFailure() = runBlocking {
        val generator = GeminiWorkoutGenerator(apiKey = "")
        val profile = UserProfile(
            targetBuild = PhysiqueBuild.V_TAPER,
            experienceLevel = ExperienceLevel.BEGINNER
        )

        val result = generator.generateRoutine(profile)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key is not configured") == true)
    }

    @Test
    fun testGeminiWorkoutGenerator_whenApiReturns403_returnsExplicitKeyError() = runBlocking {
        val mockClient = createMockClient(403, """{"error":{"message":"The provided API key is invalid."}}""")
        val generator = GeminiWorkoutGenerator(apiKey = "invalid_key", client = mockClient)
        val profile = UserProfile(targetBuild = PhysiqueBuild.TANK, experienceLevel = ExperienceLevel.ADVANCED)

        val result = generator.generateRoutine(profile)
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue("Error must mention 403 or Invalid API key, got: $message", message.contains("403") || message.contains("Invalid API key"))
    }

    @Test
    fun testGeminiWorkoutGenerator_successResponse_parsesPlansAndRationale() = runBlocking {
        val mockGeminiJson = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\n  \"coachRationale\": \"Beginner tank routine focusing on foundational spinal bracing.\",\n  \"days\": [\n    {\n      \"dayNumber\": 1,\n      \"dayName\": \"Day 1: Pull A (Foundational Deadlift)\",\n      \"focusArea\": \"Heavy Back & Traps\",\n      \"guidanceNotes\": \"Master the hip hinge before adding bumper plates.\",\n      \"isActiveRest\": false,\n      \"exercises\": [\n        {\n          \"id\": \"ai_d1_1\",\n          \"name\": \"Conventional Deadlift\",\n          \"targetMuscle\": \"BACK\",\n          \"category\": \"COMPOUND_PULL\",\n          \"baselineSets\": 3,\n          \"repRange\": \"8-10 reps\",\n          \"restSeconds\": 120,\n          \"formNotes\": \"Brace abs hard.\",\n          \"isCompensatoryEligibleCarb\": true\n        }\n      ]\n    },\n    {\n      \"dayNumber\": 7,\n      \"dayName\": \"Day 7: Active Recovery\",\n      \"focusArea\": \"Mobility & Foam Rolling\",\n      \"guidanceNotes\": \"Rest CNS\",\n      \"isActiveRest\": true,\n      \"exercises\": []\n    }\n  ]\n}"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val mockClient = createMockClient(200, mockGeminiJson)
        val generator = GeminiWorkoutGenerator(apiKey = "valid_key", client = mockClient)
        val profile = UserProfile(targetBuild = PhysiqueBuild.TANK, experienceLevel = ExperienceLevel.BEGINNER)

        val result = generator.generateRoutine(profile)
        assertTrue("Gemini generator should parse valid JSON", result.isSuccess)
        val routine = result.getOrThrow()
        assertEquals(PhysiqueBuild.TANK, routine.targetBuild)
        assertEquals(ExperienceLevel.BEGINNER, routine.experienceLevel)
        assertTrue(routine.coachRationale.contains("Beginner tank routine"))
        assertEquals(2, routine.plans.size)

        val day1 = routine.plans[0]
        assertEquals("Conventional Deadlift", day1.exercises[0].name)
        assertEquals(3, day1.exercises[0].baselineSets)
        assertEquals("8-10 reps", day1.exercises[0].repRange)
    }

    @Test
    fun testDynamicWorkoutRepository_customAiPlans_takePrecedence() {
        val repo = DynamicWorkoutRepository(PhysiqueBuild.V_TAPER)
        assertFalse(repo.hasCustomAiPlans())

        val customPlan = WorkoutDayPlan(
            dayNumber = 1,
            dayName = "AI Custom Day 1",
            focusArea = "Gemini Hypertrophy",
            exercises = listOf(
                Exercise("custom_ex", "AI Incline Dumbbell Flyes", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "12-15 reps", 60, "Deep stretch")
            )
        )
        repo.setCustomAiPlans(listOf(customPlan), "Periodized for student")
        assertTrue(repo.hasCustomAiPlans())
        assertEquals("Periodized for student", repo.getCoachRationale())

        val day1 = repo.getDayPlan(1)
        assertEquals("AI Custom Day 1", day1.dayName)
        assertEquals("AI Incline Dumbbell Flyes", day1.exercises[0].name)

        // Switching builds clears custom plan
        repo.setTargetBuild(PhysiqueBuild.TANK)
        assertFalse(repo.hasCustomAiPlans())
    }
}
