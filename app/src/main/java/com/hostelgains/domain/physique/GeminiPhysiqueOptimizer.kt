package com.hostelgains.domain.physique

import com.hostelgains.data.ApiKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PhysiqueOptimizationResult(
    val dailyCalories: Int,
    val proteinGrams: Int,
    val volumeFocus: String,
    val leverageAdvice: String,
    val hostelMessStrategy: String,
    val fullSummary: String,
    val isAiGenerated: Boolean
)

/**
 * Tailors a student's training volume and nutrition to their exact height, weight,
 * age, and chosen physique build pathway using Google Gemini API or sports-science offline engine.
 */
class GeminiPhysiqueOptimizer(
    private val apiKeyRepository: ApiKeyRepository,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun optimizePlan(profile: UserProfile): PhysiqueOptimizationResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyRepository.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext OfflinePhysiqueOptimizer.optimize(profile)
        }

        try {
            val prompt = buildOptimizationPrompt(profile)
            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val cleanKey = apiKey.trim()
            val targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$cleanKey"

            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", cleanKey)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext OfflinePhysiqueOptimizer.optimize(profile)
            }

            val rawBody = response.body?.string() ?: return@withContext OfflinePhysiqueOptimizer.optimize(profile)
            val parsedResult = parseGeminiResponse(rawBody, profile)
            parsedResult ?: OfflinePhysiqueOptimizer.optimize(profile)
        } catch (e: Exception) {
            OfflinePhysiqueOptimizer.optimize(profile)
        }
    }

    private fun buildOptimizationPrompt(profile: UserProfile): String {
        return """
        You are an elite sports science biomechanist and physique coach for university hostel students.
        Analyze this student's anthropometric stats:
        - Height: ${profile.heightCm} cm (${profile.heightImperialFormatted})
        - Weight: ${profile.weightKg} kg (${profile.weightFormatted})
        - Age: ${profile.ageYears} years
        - Calculated BMI: ${profile.bmi} (${profile.bmiCategory})
        - Target Physique Build: ${profile.targetBuild.title} (${profile.targetBuild.archetypeTag})
        - Rep Range Focus: ${profile.targetBuild.repRangeSummary}
        - Build Aesthetic: ${profile.targetBuild.targetAesthetic}
        - Context: Indian university hostel mess (high dal/rice/chapati carbs, inconsistent protein, crowded gym equipment).

        Provide tailored advice in strict JSON format with these exact keys:
        {
          "dailyCalories": <integer, target daily intake>,
          "proteinGrams": <integer, target grams based on bodyweight and goal>,
          "volumeFocus": "<1 sentence on primary muscular focus for this height & weight>",
          "leverageAdvice": "<biomechanical advice based on height/limb lengths e.g. squat depth, press angles>",
          "hostelMessStrategy": "<actionable hostel dining hall hack to hit target macros without spending extra>",
          "fullSummary": "<2-3 sentence motivational coach summary aligning their exact stats to their dream physique>"
        }
        """.trimIndent()
    }

    private fun parseGeminiResponse(rawBody: String, profile: UserProfile): PhysiqueOptimizationResult? {
        return try {
            val root = JSONObject(rawBody)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            val json = JSONObject(text)
            PhysiqueOptimizationResult(
                dailyCalories = json.optInt("dailyCalories", profile.recommendedCalories),
                proteinGrams = json.optInt("proteinGrams", profile.recommendedProteinGrams),
                volumeFocus = json.optString("volumeFocus", "${profile.targetBuild.title} volume calibrated to ${profile.weightKg.toInt()} kg frame."),
                leverageAdvice = json.optString("leverageAdvice", "Maintain 2-second eccentric control on all compound presses and pulls."),
                hostelMessStrategy = json.optString("hostelMessStrategy", "Double up on dal bowls and eggs/paneer during mess dinner."),
                fullSummary = json.optString("fullSummary", "At ${profile.heightImperialFormatted} and ${profile.weightKg.toInt()} kg, your frame is perfectly primed for ${profile.targetBuild.title}."),
                isAiGenerated = true
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Offline rule-based sports science engine ensuring full offline capability.
 */
object OfflinePhysiqueOptimizer {

    fun optimize(profile: UserProfile): PhysiqueOptimizationResult {
        val calories = profile.recommendedCalories
        val protein = profile.recommendedProteinGrams
        val build = profile.targetBuild

        val (leverageAdvice, messStrategy) = when (build) {
            PhysiqueBuild.V_TAPER -> Pair(
                "At ${profile.heightImperialFormatted}, focus on wide-grip lat pulldowns and lateral raises at a 15-degree torso forward lean to maximize upper shoulder-width leverage.",
                "Target thick dal and milk cups at breakfast to bank 30g protein before morning classes."
            )
            PhysiqueBuild.TANK -> Pair(
                "With a solid ${profile.weightKg.toInt()} kg frame, keep feet flat and wide on squats. Prioritize heavy compound rows with 2-minute rests to recruit thick spinal erectors.",
                "Eat an extra 2 chapatis and request dense paneer/egg servings during dinner to sustain CNS strength recovery."
            )
            PhysiqueBuild.ATHLETE -> Pair(
                "Emphasize unilateral Bulgarian split squats and explosive plyo push-ups to balance hip symmetry and build explosive triple-extension.",
                "Keep hydration high and combine peanut butter or boiled eggs with fruit before evening workout sessions."
            )
            PhysiqueBuild.X_FRAME -> Pair(
                "Perform narrow-stance goblet/barbell squats with elevated heels to shift tension directly onto the vastus lateralis outer quad sweep.",
                "Prioritize steady protein throughout all 3 mess meals to support lower body rebuilding."
            )
            PhysiqueBuild.YOKE -> Pair(
                "Incorporate heavy dumbbell shrugs and seated cable rows pulled high into the sternum to thicken the rhomboids and mid-traps.",
                "Take advantage of hearty mess rice and lentils for clean glycogen loading."
            )
            PhysiqueBuild.GOLDEN_ERA -> Pair(
                "Perform incline dumbbell presses at 30 degrees and controlled dumbbell pullovers to expand ribcage breadth and build the upper chest shelf.",
                "Prioritize high-protein breakfast and evening curds/yogurt."
            )
            PhysiqueBuild.SLEEPER -> Pair(
                "Stick to controlled calisthenics, weighted dips, and full-ROM pull-ups in the 10-15 rep range for maximum relative strength and tendon density.",
                "Moderate carb portions during dinner while maximizing boiled pulses and eggs."
            )
        }

        return PhysiqueOptimizationResult(
            dailyCalories = calories,
            proteinGrams = protein,
            volumeFocus = "${build.title} volume calibrated for ${profile.heightImperialFormatted} at ${profile.weightKg.toInt()} kg (${build.targetAesthetic}).",
            leverageAdvice = leverageAdvice,
            hostelMessStrategy = messStrategy,
            fullSummary = "Based on your ${profile.heightImperialFormatted} height, ${profile.weightKg.toInt()} kg frame (BMI ${profile.bmi}), your baseline is calibrated to ${calories} kcal and ${protein}g protein for ${build.title}.",
            isAiGenerated = false
        )
    }
}
