package com.hostelgains.domain.physique

import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.data.network.SharedHttpClient
import com.hostelgains.domain.util.SimpleJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
 *
 * Security & resource hardening:
 * - Closes OkHttp [okhttp3.Response] bodies deterministically via `.use { ... }`.
 * - Sends the API key strictly via the `x-goog-api-key` header (never in URL query parameters).
 * - Uses [SimpleJsonParser] instead of Android `org.json` stubs for local JVM test compatibility.
 */
class GeminiPhysiqueOptimizer(
    private val apiKeyRepository: ApiKeyRepository,
    private val client: OkHttpClient = SharedHttpClient.instance.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    companion object {
        const val DEFAULT_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    }

    suspend fun optimizePlan(profile: UserProfile): PhysiqueOptimizationResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyRepository.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext OfflinePhysiqueOptimizer.optimize(profile)
        }

        try {
            val prompt = buildOptimizationPrompt(profile)
            val payload = SimpleJsonParser.buildGeminiRequestJson(
                userText = prompt,
                systemInstruction = "You are an elite sports science biomechanist and physique coach for university hostel students. Provide tailored advice in strict JSON format.",
                temperature = 0.2
            )

            val cleanKey = apiKey.trim()
            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", cleanKey)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext OfflinePhysiqueOptimizer.optimize(profile)
                }

                val rawBody = response.body.string()
                if (rawBody.isBlank()) {
                    return@withContext OfflinePhysiqueOptimizer.optimize(profile)
                }
                parseGeminiResponse(rawBody, profile) ?: OfflinePhysiqueOptimizer.optimize(profile)
            }
        } catch (e: Exception) {
            OfflinePhysiqueOptimizer.optimize(profile)
        }
    }

    private fun buildOptimizationPrompt(profile: UserProfile): String {
        return """
        Analyze this student's anthropometric stats:
        - Height: ${profile.heightCm} cm (${profile.heightImperialFormatted})
        - Weight: ${profile.weightKg} kg (${profile.weightFormatted})
        - Age: ${profile.ageYears} years
        - Calculated BMI: ${profile.bmi} (${profile.bmiCategory})
        - Target Physique Build: ${profile.targetBuild.title} (${profile.targetBuild.archetypeTag})
        - Experience Level: ${profile.experienceLevel.title} (${profile.experienceLevel.subtitle})
        - Rep Range & Volume Focus: ${profile.experienceLevel.repFocus}
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

    internal fun parseGeminiResponse(rawBody: String, profile: UserProfile): PhysiqueOptimizationResult? {
        return try {
            val cleanedText = SimpleJsonParser.extractGeminiCandidateText(rawBody) ?: return null
            val json = SimpleJsonParser.parse(cleanedText) as? SimpleJsonParser.Node.ObjectNode ?: return null

            PhysiqueOptimizationResult(
                dailyCalories = json.optInt("dailyCalories", profile.recommendedCalories).coerceIn(1200, 6000),
                proteinGrams = json.optInt("proteinGrams", profile.recommendedProteinGrams).coerceIn(40, 400),
                volumeFocus = json.optString(
                    "volumeFocus",
                    "${profile.targetBuild.title} volume calibrated to ${profile.weightKg.toInt()} kg frame."
                ).take(250),
                leverageAdvice = json.optString(
                    "leverageAdvice",
                    "Maintain 2-second eccentric control on all compound presses and pulls."
                ).take(300),
                hostelMessStrategy = json.optString(
                    "hostelMessStrategy",
                    "Double up on dal bowls and eggs/paneer during mess dinner."
                ).take(300),
                fullSummary = json.optString(
                    "fullSummary",
                    "At ${profile.heightImperialFormatted} and ${profile.weightKg.toInt()} kg, your frame is primed for ${profile.targetBuild.title}."
                ).take(400),
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
