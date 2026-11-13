package com.hostelgains.domain.workout

import com.hostelgains.domain.physique.ExperienceLevel
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.domain.physique.UserProfile
import com.hostelgains.domain.util.SimpleJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GeneratedRoutineResult(
    val coachRationale: String,
    val plans: List<WorkoutDayPlan>,
    val targetBuild: PhysiqueBuild,
    val experienceLevel: ExperienceLevel,
    val isAiGenerated: Boolean = true
)

/**
 * Live AI workout routine generator powered by Google Gemini 1.5 Flash.
 * Takes the user's selected [PhysiqueBuild], [ExperienceLevel], and anthropometrics,
 * and generates a customized, scientifically periodized 7-day PPL routine.
 */
class GeminiWorkoutGenerator(
    private val apiKey: String? = null,
    private val apiKeyProvider: (() -> String?)? = null,
    private val client: OkHttpClient = defaultClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    companion object {
        const val DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }

    suspend fun generateRoutine(profile: UserProfile): Result<GeneratedRoutineResult> = withContext(Dispatchers.IO) {
        val key = when {
            apiKey != null -> apiKey.trim()
            apiKeyProvider != null -> apiKeyProvider.invoke()?.trim()
            else -> System.getenv("GEMINI_API_KEY")?.trim()
        }
        if (key.isNullOrEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Go to Settings / API Key to unlock AI routine generation.")
            )
        }

        try {
            val prompt = buildPrompt(profile)
            val escapedPrompt = SimpleJsonParser.escape(prompt)
            val jsonPayload = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": "$escapedPrompt" }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.3,
                    "responseMimeType": "application/json"
                  }
                }
            """.trimIndent()

            val targetUrl = if (baseUrl.contains("key=")) baseUrl else {
                val delimiter = if (baseUrl.contains("?")) "&" else "?"
                "$baseUrl${delimiter}key=$key"
            }

            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", key)
                .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorSnippet = response.body.string().take(200)
                    val msg = when (code) {
                        401, 403 -> "Gemini API Error ($code): Invalid API key or permission denied. Check your key in Settings."
                        429 -> "Gemini API Quota Exceeded (429). Please wait a moment before regenerating."
                        500, 503 -> "Gemini AI Server Error ($code). Please try again shortly."
                        else -> "Gemini API call failed with HTTP $code: $errorSnippet"
                    }
                    return@withContext Result.failure(IOException(msg))
                }

                val responseBody = response.body.string()
                if (responseBody.isBlank()) {
                    return@withContext Result.failure(IOException("Empty response received from Gemini API"))
                }

                val parsed = parseGeminiResponse(responseBody, profile)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.failure(IllegalStateException("Failed to parse Gemini generated workout routine JSON."))
                }
            }
        } catch (e: Exception) {
            val msg = if (e is IOException) {
                "Cannot reach Gemini AI: ${e.message}. Please check your internet connection."
            } else {
                e.message ?: "An unexpected error occurred while generating the routine."
            }
            Result.failure(Exception(msg, e))
        }
    }

    private fun buildPrompt(profile: UserProfile): String {
        val build = profile.targetBuild
        val level = profile.experienceLevel

        return """
        You are an elite sports scientist and strength coach.
        Generate a fully customized 7-Day Push/Pull/Legs (PPL) + Active Recovery workout routine for a university student.
        
        STUDENT PROFILE:
        - Target Physique Build: ${build.title} (${build.archetypeTag}) - ${build.targetAesthetic}
        - Lifting Experience Level: ${level.title} (${level.subtitle})
        - Rep Focus & Periodization: ${level.repFocus}
        - Rest Period Focus: ${level.restFocus}
        - Experience Description: ${level.description}
        - Student Stats: Height ${profile.heightFormatted}, Weight ${profile.weightFormatted}, Age ${profile.ageYears}
        - Facility Setting: University hostel gym (dumbbells up to 30kg, barbells, pull-up bar, basic cable station, flat/incline benches, crowded peak hours).

        EXPERIENCE LEVEL RULES:
        ${if (level == ExperienceLevel.BEGINNER) {
            "- Level: BEGINNER (0-1 yrs). Foundational movement patterns, higher reps (10-15 reps), 3 working sets per exercise.\n" +
            "- Avoid ultra-high-risk movements or complex Olympic lifts. Emphasize machine/dumbbell alternatives, clear joint execution cues, and 90s rests."
        } else if (level == ExperienceLevel.INTERMEDIATE) {
            "- Level: INTERMEDIATE (1-3 yrs). Hypertrophy sweet-spot (8-12 reps), 3-4 working sets per exercise.\n" +
            "- Progressive overload, balanced compound multi-joint movements + targeted isolation for ${build.targetAesthetic}."
        } else {
            "- Level: ADVANCED (3+ yrs). High intensity & density. Heavy compound loading (4-8 reps) + metabolic burnout isolations (12-15 reps).\n" +
            "- Incorporate pause reps, dead-stop mechanics, mechanical dropsets, 4-5 working sets, and weak-point specialization for ${build.targetAesthetic}."
        }}

        Output strictly valid JSON with this exact schema:
        {
          "coachRationale": "<2-3 sentence coaching explanation justifying why this routine was designed specifically for a ${level.title} chasing the ${build.title}>",
          "days": [
            {
              "dayNumber": 1,
              "dayName": "Day 1: Pull A (<Focus>)",
              "focusArea": "<Target muscle groups>",
              "guidanceNotes": "<Coaching execution cue>",
              "isActiveRest": false,
              "exercises": [
                {
                  "id": "ai_d1_ex1",
                  "name": "<Exercise Name>",
                  "targetMuscle": "CHEST|BACK|LATS|REAR_DELTS|LATERAL_DELTS|FRONT_DELTS|BICEPS|TRICEPS|QUADS|HAMSTRINGS|GLUTES|CALVES|CORE",
                  "category": "COMPOUND_PUSH|COMPOUND_PULL|COMPOUND_LEGS|ISOLATION_DELTS|ISOLATION_ARMS|ISOLATION_LEGS|CORE",
                  "baselineSets": 3,
                  "repRange": "8-12 reps",
                  "restSeconds": 90,
                  "formNotes": "<Key execution cue>",
                  "isCompensatoryEligibleCarb": true
                }
              ]
            },
            { "dayNumber": 2, "dayName": "Day 2: Push A ...", ... },
            { "dayNumber": 3, "dayName": "Day 3: Legs A ...", ... },
            { "dayNumber": 4, "dayName": "Day 4: Pull B ...", ... },
            { "dayNumber": 5, "dayName": "Day 5: Push B ...", ... },
            { "dayNumber": 6, "dayName": "Day 6: Legs B ...", ... },
            {
              "dayNumber": 7,
              "dayName": "Day 7: Active Recovery & Mobility",
              "focusArea": "Deload, Campus Walks, Thoracic Mobility",
              "guidanceNotes": "Recharge CNS for next week's progressive overload.",
              "isActiveRest": true,
              "exercises": []
            }
          ]
        }
        """.trimIndent()
    }

    private fun parseGeminiResponse(rawBody: String, profile: UserProfile): GeneratedRoutineResult? {
        return try {
            val rootNode = SimpleJsonParser.parse(rawBody) ?: return null
            val candidates = (rootNode["candidates"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
            if (candidates.isEmpty()) return null
            val firstCandidate = candidates[0] as? SimpleJsonParser.Node.ObjectNode ?: return null
            val content = firstCandidate["content"] as? SimpleJsonParser.Node.ObjectNode ?: return null
            val parts = (content["parts"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
            if (parts.isEmpty()) return null
            val firstPart = parts[0] as? SimpleJsonParser.Node.ObjectNode ?: return null
            val rawText = (firstPart["text"] as? SimpleJsonParser.Node.StringNode)?.value ?: return null

            val cleanedText = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val pair = WorkoutPlanSerializer.fromJson(cleanedText) ?: return null
            GeneratedRoutineResult(
                coachRationale = pair.first.ifBlank { "Customized ${profile.targetBuild.title} routine tailored for ${profile.experienceLevel.title} level." },
                plans = pair.second,
                targetBuild = profile.targetBuild,
                experienceLevel = profile.experienceLevel,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
