package com.hostelgains.data.nutrition

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.FixtureNutritionParser
import com.hostelgains.domain.nutrition.FoodItemNutrition
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.workout.DefaultCompensatoryEngine
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Empirical Adversarial Stress Test Suite for Milestone M2 (AI Nutrition Parsing & Macro Logging).
 *
 * Authored by Challenger 2 (EMPIRICAL CHALLENGER).
 * Stress-tests:
 * 1. Extreme macro boundaries (massive quantities, large numbers, multi-item scaling).
 * 2. 0-calorie / near-zero calorie queries (water, diet drinks, black coffee, zero-macro trigger propagation).
 * 3. Gemini parser response variations (markdown code fences, scientific notation, malformed payloads,
 *    HTTP errors, schema nulls, trailing commas, custom pure-Kotlin SimpleJsonParser resilience).
 * 4. Cumulative trigger boundary conditions (exactly 200g carbs, 199.9g vs 200.0g, 49.9g vs 50.0g protein,
 *    single-meal vs cumulative precedence, dynamic meal deletion recalculation).
 * 5. High-concurrency repository stress testing (thread safety, race-free StateFlow emission).
 */
class ChallengerM2AdversarialStressTest {

    private lateinit var fixtureParser: FixtureNutritionParser
    private lateinit var repository: DefaultMealRepository
    private lateinit var compensatoryEngine: DefaultCompensatoryEngine
    private lateinit var workoutRepository: VTaperWorkoutRepository
    private val testDate = "2026-11-10"

    @Before
    fun setUp() {
        fixtureParser = FixtureNutritionParser()
        repository = DefaultMealRepository()
        compensatoryEngine = DefaultCompensatoryEngine()
        workoutRepository = VTaperWorkoutRepository()
    }

    private fun createMockGeminiClient(statusCode: Int, responseBody: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(responseBody.toResponseBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            }
            .build()
    }

    private fun wrapGeminiResponse(rawInnerJson: String): String {
        val escaped = rawInnerJson
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "$escaped"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
    }

    // =========================================================================
    // SECTION 1: Extreme Macro Boundaries & Massive Quantities
    // =========================================================================

    @Test
    fun testMassiveQuantities_50Rotis_calculatesAccuratelyWithoutOverflow() = runBlocking {
        val result = fixtureParser.parseMeal("50 dry rotis")
        assertTrue("50 rotis must be parsed successfully", result.isSuccess)

        val meal = result.getOrThrow()
        // 50 rotis * 17g carbs = 850g carbs
        // 50 rotis * 3g protein = 150g protein
        // 50 rotis * 0.5g fat = 25g fats
        // 50 rotis * 80 kcal = 4000 kcal
        assertEquals(850.0f, meal.carbsGrams, 0.1f)
        assertEquals(150.0f, meal.proteinGrams, 0.1f)
        assertEquals(25.0f, meal.fatsGrams, 0.1f)
        assertEquals(4000, meal.calories)
        assertTrue("850g carbs must trigger high carb surplus", meal.isHighCarb)
        assertFalse("150g protein must not trigger protein deficit", meal.isLowProtein)

        // Verify Compensatory Engine adds +1 set to Squats and Lat Pulldowns
        val day1Plan = workoutRepository.getDayPlan(1)
        val adjusted = compensatoryEngine.evaluateWorkout(day1Plan, meal)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Lat pulldowns must increase from 4 to 5 sets on carb surplus", 5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
        assertFalse(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testMassiveQuantities_100BoiledEggs_calculatesHighProtein() = runBlocking {
        val result = fixtureParser.parseMeal("100 boiled eggs")
        assertTrue("100 boiled eggs must be parsed successfully", result.isSuccess)

        val meal = result.getOrThrow()
        // 100 eggs * 6g protein = 600g protein
        // 100 eggs * 0.5g carbs = 50g carbs
        // 100 eggs * 5g fat = 500g fats
        // 100 eggs * 75 kcal = 7500 kcal
        assertEquals(600.0f, meal.proteinGrams, 0.1f)
        assertEquals(50.0f, meal.carbsGrams, 0.1f)
        assertEquals(500.0f, meal.fatsGrams, 0.1f)
        assertEquals(7500, meal.calories)
        assertFalse("50g carbs is below 60g, so isHighCarb must be false", meal.isHighCarb)
        assertFalse("600g protein is well above 20g, so isLowProtein must be false", meal.isLowProtein)
    }

    @Test
    fun testMassiveQuantities_100BoiledEggWhites_calculatesLeanProtein() = runBlocking {
        val result = fixtureParser.parseMeal("100 boiled egg whites")
        assertTrue("100 egg whites must be parsed", result.isSuccess)

        val meal = result.getOrThrow()
        // 100 whites * 3.5g protein = 350g protein
        // 100 whites * 0.25g carbs = 25g carbs
        // 100 whites * 0.05g fat = 5g fat
        // 100 whites * 17 kcal = 1700 kcal
        assertEquals(350.0f, meal.proteinGrams, 0.1f)
        assertEquals(25.0f, meal.carbsGrams, 0.1f)
        assertEquals(5.0f, meal.fatsGrams, 0.1f)
        assertEquals(1700, meal.calories)
        assertFalse(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testMassiveQuantities_10PlatesBiryani_parsesCountAccurately() = runBlocking {
        val result = fixtureParser.parseMeal("10 plates biryani")
        assertTrue("10 plates biryani should parse", result.isSuccess)

        val meal = result.getOrThrow()
        // 10 plates * 10g protein = 100g protein
        // 10 plates * 60g carbs = 600g carbs
        // 10 plates * 8g fat = 80g fats
        // 10 plates * 350 kcal = 3500 kcal
        assertEquals(100.0f, meal.proteinGrams, 0.1f)
        assertEquals(600.0f, meal.carbsGrams, 0.1f)
        assertEquals(80.0f, meal.fatsGrams, 0.1f)
        assertEquals(3500, meal.calories)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testMassiveQuantities_extremeBoundary_1000Rotis_noNumericOverflow() = runBlocking {
        val result = fixtureParser.parseMeal("1000 rotis")
        assertTrue("1000 rotis must be parsed without throwing", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(3000.0f, meal.proteinGrams, 0.1f)
        assertEquals(17000.0f, meal.carbsGrams, 0.1f)
        assertEquals(500.0f, meal.fatsGrams, 0.1f)
        assertEquals(80000, meal.calories)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testMassiveQuantities_wordNumberQuantities_parsesCorrectly() = runBlocking {
        val result = fixtureParser.parseMeal("ten dry rotis and four boiled eggs")
        assertTrue(result.isSuccess)

        val meal = result.getOrThrow()
        // 10 rotis = 30p, 170c, 5f, 800cal
        // 4 eggs = 24p, 2c, 20f, 300cal
        // Total = 54p, 172c, 25f, 1100cal
        assertEquals(54.0f, meal.proteinGrams, 0.1f)
        assertEquals(172.0f, meal.carbsGrams, 0.1f)
        assertEquals(25.0f, meal.fatsGrams, 0.1f)
        assertEquals(1100, meal.calories)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testMassiveQuantities_multiTokenCombination_accumulatesAllStaples() = runBlocking {
        val query = "5 rotis, 4 boiled eggs, 2 katori dal, 1 plate rice"
        val result = fixtureParser.parseMeal(query)
        assertTrue("Multi-staple mess combo must parse successfully", result.isSuccess)

        val meal = result.getOrThrow()
        // 5 rotis: 15p, 85c, 2.5f, 400 cal
        // 4 eggs: 24p, 2c, 20f, 300 cal
        // 2 dal: 11p, 36c, 9f, 270 cal
        // 1 rice: 5.5p, 54c, 0.5f, 245 cal
        // Total protein = 15 + 24 + 11 + 5.5 = 55.5g
        // Total carbs = 85 + 2 + 36 + 54 = 177.0g
        // Total fats = 2.5 + 20 + 9 + 0.5 = 32.0g
        // Total calories = 400 + 300 + 270 + 245 = 1215 kcal
        assertEquals(55.5f, meal.proteinGrams, 0.1f)
        assertEquals(177.0f, meal.carbsGrams, 0.1f)
        assertEquals(32.0f, meal.fatsGrams, 0.1f)
        assertEquals(1215, meal.calories)
        assertEquals(4, meal.items.size)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    // =========================================================================
    // SECTION 2: 0-Calorie & Zero-Macronutrient Queries
    // =========================================================================

    @Test
    fun testZeroCalorieQuery_pureWater_rejectedByFixtureParser() = runBlocking {
        // Pure water does not contain any food macros in offline mess dictionary
        val result = fixtureParser.parseMeal("pure water")
        assertTrue("Pure water query should fail in offline fixture tokenizer", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testZeroCalorieQuery_dietSoda_rejectedByFixtureParser() = runBlocking {
        val result = fixtureParser.parseMeal("diet coke zero calories")
        assertTrue("Diet coke should fail offline tokenizer (no recognized mess items)", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testZeroCalorieMeal_parsedViaGemini_flagsProteinDeficitDueToZeroProtein() = runBlocking {
        // When Gemini parses a 0-calorie drink, it returns 0g protein, 0g carbs, 0g fat, 0 cal
        val zeroCalorieJson = """
            {
              "mealName": "Mineral Water (500ml)",
              "proteinGrams": 0.0,
              "carbsGrams": 0.0,
              "fatsGrams": 0.0,
              "calories": 0,
              "isHighCarb": false,
              "isLowProtein": true,
              "dietaryNotes": "Hydration with zero caloric content",
              "items": [
                {
                  "name": "Water",
                  "quantityDescription": "500 ml",
                  "proteinGrams": 0.0,
                  "carbsGrams": 0.0,
                  "fatsGrams": 0.0,
                  "calories": 0
                }
              ]
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(zeroCalorieJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("500ml mineral water")
        assertTrue("Gemini parser should succeed on zero-calorie JSON", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(0.0f, meal.proteinGrams, 0.01f)
        assertEquals(0.0f, meal.carbsGrams, 0.01f)
        assertEquals(0.0f, meal.fatsGrams, 0.01f)
        assertEquals(0, meal.calories)
        assertFalse(meal.isHighCarb)
        assertTrue("0g protein < 20g must set isLowProtein = true", meal.isLowProtein)

        // Verify propagation to MealRepository
        repository.logMeal(meal, testDate)
        val dailySummary = repository.getDailySummary(testDate).value
        assertEquals(0.0f, dailySummary.totalProtein, 0.01f)
        assertEquals(0, dailySummary.totalCalories)
        assertTrue("Zero protein meal must trigger protein deficit in summary", dailySummary.hasProteinDeficitTrigger)

        // Verify propagation to CompensatoryEngine: all exercises capped at 3 sets
        val day1Plan = workoutRepository.getDayPlan(1)
        val adjustedWorkout = compensatoryEngine.evaluateWorkout(day1Plan, dailySummary)
        val latPulldown = adjustedWorkout.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Protein deficit cap strictly caps 4 baseline sets to 3", 3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testNearZeroCalorie_blackCoffeeHeuristic_preservesHostelChaiFallback() = runBlocking {
        // "black coffee" contains "coffee", so fixture parser heuristically attributes 1 cup coffee/chai
        val result = fixtureParser.parseMeal("1 cup black coffee")
        assertTrue(result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(2.0f, meal.proteinGrams, 0.1f)
        assertEquals(14.0f, meal.carbsGrams, 0.1f)
        assertEquals(90, meal.calories)
        assertFalse(meal.isHighCarb)
        assertTrue("2g protein < 20g triggers protein deficit", meal.isLowProtein)
    }

    // =========================================================================
    // SECTION 3: Gemini JSON Variations & Parser Robustness
    // =========================================================================

    @Test
    fun testGeminiParser_standardJsonCandidate_parsesAccurately() = runBlocking {
        val rawMeal = """
            {
              "mealName": "Chicken Biryani with Raita",
              "proteinGrams": 42.5,
              "carbsGrams": 85.0,
              "fatsGrams": 18.0,
              "calories": 670,
              "isHighCarb": true,
              "isLowProtein": false,
              "dietaryNotes": "Rich in protein and carbs",
              "items": [
                {
                  "name": "Chicken Biryani",
                  "quantityDescription": "1 full plate",
                  "proteinGrams": 38.0,
                  "carbsGrams": 80.0,
                  "fatsGrams": 15.0,
                  "calories": 607
                },
                {
                  "name": "Cucumber Raita",
                  "quantityDescription": "1 bowl",
                  "proteinGrams": 4.5,
                  "carbsGrams": 5.0,
                  "fatsGrams": 3.0,
                  "calories": 63
                }
              ]
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(rawMeal))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("chicken biryani with raita")
        assertTrue(result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals("Chicken Biryani with Raita", meal.mealName)
        assertEquals(42.5f, meal.proteinGrams, 0.01f)
        assertEquals(85.0f, meal.carbsGrams, 0.01f)
        assertEquals(18.0f, meal.fatsGrams, 0.01f)
        assertEquals(670, meal.calories)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
        assertEquals(2, meal.items.size)
    }

    @Test
    fun testGeminiParser_markdownFencedJson_stripsFencesAndParses() = runBlocking {
        // Many LLMs wrap JSON in ```json\n...\n```
        val rawJsonWithFences = "```json\n" + """
            {
              "mealName": "Grilled Fish & Steamed Rice",
              "proteinGrams": 35.0,
              "carbsGrams": 62.0,
              "fatsGrams": 8.0,
              "calories": 460,
              "isHighCarb": true,
              "isLowProtein": false,
              "items": []
            }
        """.trimIndent() + "\n```"

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(rawJsonWithFences))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("grilled fish with steamed rice")
        assertTrue("Markdown fenced JSON must be parsed successfully", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals("Grilled Fish & Steamed Rice", meal.mealName)
        assertEquals(35.0f, meal.proteinGrams, 0.01f)
        assertEquals(62.0f, meal.carbsGrams, 0.01f)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testGeminiParser_markdownFencedWithWhitespace_parsesCleanly() = runBlocking {
        val rawJson = "   ```\n" + """
            {
              "mealName": "Egg Curry and Parathas",
              "proteinGrams": 28.0,
              "carbsGrams": 55.0,
              "fatsGrams": 22.0,
              "calories": 530,
              "isHighCarb": false,
              "isLowProtein": false
            }
        """.trimIndent() + "\n```   "

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(rawJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("egg curry and parathas")
        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals(28.0f, meal.proteinGrams, 0.01f)
        assertEquals(55.0f, meal.carbsGrams, 0.01f)
    }

    @Test
    fun testGeminiParser_scientificNotationNumbers_parsedCorrectly() = runBlocking {
        // Gemini can occasionally return scientific notation e.g. 2.5e1
        val scientificJson = """
            {
              "mealName": "High Calorie Mass Gainer",
              "proteinGrams": 5.0e1,
              "carbsGrams": 1.2e2,
              "fatsGrams": 1.5e1,
              "calories": 815,
              "isHighCarb": true,
              "isLowProtein": false
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(scientificJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("mass gainer shake")
        assertTrue(result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(50.0f, meal.proteinGrams, 0.01f)
        assertEquals(120.0f, meal.carbsGrams, 0.01f)
        assertEquals(15.0f, meal.fatsGrams, 0.01f)
        assertTrue(meal.isHighCarb)
    }

    @Test
    fun testGeminiParser_missingOptionalFields_usesSafeDefaults() = runBlocking {
        // Minimal response without items, dietaryNotes, or booleans
        val minimalJson = """
            {
              "mealName": "Plain Snack",
              "proteinGrams": 15.0,
              "carbsGrams": 65.0,
              "fatsGrams": 5.0,
              "calories": 365
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(minimalJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("plain snack")
        assertTrue(result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(15.0f, meal.proteinGrams, 0.01f)
        assertEquals(65.0f, meal.carbsGrams, 0.01f)
        assertTrue("Carbs >= 60g defaults isHighCarb to true", meal.isHighCarb)
        assertTrue("Protein < 20g defaults isLowProtein to true", meal.isLowProtein)
        assertTrue(meal.items.isEmpty())
        assertEquals("", meal.dietaryNotes)
    }

    @Test
    fun testGeminiParser_allFieldsNull_usesDefaultsWithoutException() = runBlocking {
        val nullFieldsJson = """
            {
              "mealName": null,
              "proteinGrams": null,
              "carbsGrams": null,
              "fatsGrams": null,
              "calories": null,
              "items": null
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(nullFieldsJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("mystery food item")
        assertTrue("Null fields must be safely handled with defaults", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(0.0f, meal.proteinGrams, 0.01f)
        assertEquals(0.0f, meal.carbsGrams, 0.01f)
        assertEquals(0, meal.calories)
        assertFalse(meal.isHighCarb)
        assertTrue(meal.isLowProtein)
    }

    @Test
    fun testGeminiParser_corruptJsonPayload_gracefullyFallsBackToFixtureParser() = runBlocking {
        // Unclosed braces / invalid JSON
        val malformedJson = """
            {
              "mealName": "Mutton Feast",
              "proteinGrams": 48.0,
              "carbsGrams": 74.0,
              "calories":
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(malformedJson))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("mutton, 4 dry rotis, eggs")
        assertTrue("Malformed JSON from Gemini must fall back to fixture parser", result.isSuccess)

        val meal = result.getOrThrow()
        // Fixture parser result
        assertTrue(meal.proteinGrams >= 40.0f)
        assertTrue(meal.isHighCarb)
    }

    @Test
    fun testGeminiParser_missingCandidates_gracefullyFallsBackToFixtureParser() = runBlocking {
        val emptyCandidatesJson = """
            {
              "candidates": []
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, emptyCandidatesJson)
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("dal and rice")
        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue("Fallback to fixture parser for dal and rice", meal.isLowProtein)
    }

    @Test
    fun testGeminiParser_http429RateLimit_gracefullyFallsBackToFixtureParser() = runBlocking {
        val rateLimitErrorJson = """
            {
              "error": {
                "code": 429,
                "message": "Resource has been exhausted (e.g. check quota)."
              }
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(429, rateLimitErrorJson)
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("mutton, 4 dry rotis, eggs")
        assertTrue("HTTP 429 must fall back cleanly without throwing", result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue(meal.proteinGrams >= 40.0f)
    }

    @Test
    fun testGeminiParser_http500ServerError_gracefullyFallsBackToFixtureParser() = runBlocking {
        val mockClient = createMockGeminiClient(500, "Internal Server Error")
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("dal and rice")
        assertTrue("HTTP 500 must fall back cleanly to fixture parser", result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue(meal.isLowProtein)
    }

    @Test
    fun testGeminiParser_emptyResponseBody_gracefullyFallsBackToFixtureParser() = runBlocking {
        val mockClient = createMockGeminiClient(200, "")
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("dal and rice")
        assertTrue("Empty body must fall back cleanly", result.isSuccess)
        assertTrue(result.getOrThrow().isLowProtein)
    }

    @Test
    fun testGeminiParser_trailingCommasInJson_handledResiliently() = runBlocking {
        // Trailing comma before closing brace
        val jsonWithTrailingComma = """
            {
              "mealName": "Hostel Dinner",
              "proteinGrams": 22.0,
              "carbsGrams": 65.0,
              "fatsGrams": 10.0,
              "calories": 438,
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(jsonWithTrailingComma))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("hostel dinner")
        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals(22.0f, meal.proteinGrams, 0.01f)
        assertEquals(65.0f, meal.carbsGrams, 0.01f)
    }

    @Test
    fun testGeminiParser_stringEscapesAndSpecialCharacters() = runBlocking {
        val jsonWithEscapes = """
            {
              "mealName": "Chef\"s \"Special\" Thali \u0026 Salad",
              "proteinGrams": 30.0,
              "carbsGrams": 70.0,
              "fatsGrams": 15.0,
              "calories": 535,
              "dietaryNotes": "Line 1\nLine 2\tTabbed"
            }
        """.trimIndent()

        val mockClient = createMockGeminiClient(200, wrapGeminiResponse(jsonWithEscapes))
        val parser = GeminiNutritionParser(apiKey = "mock_key", client = mockClient, fallbackParser = fixtureParser)

        val result = parser.parseMeal("thali")
        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue(meal.mealName.contains("Special"))
        assertTrue(meal.dietaryNotes.contains("Line 1"))
    }

    // =========================================================================
    // SECTION 4: Cumulative Trigger Boundary Conditions
    // =========================================================================

    @Test
    fun testCumulativeCarbs_exactly200g_triggersHighCarbSurplus() {
        // 4 meals with 50.0g carbs each -> total carbs = 200.0g
        // Each meal has < 60g carbs, so none individually trigger isHighCarb
        val m1 = MealNutrition("m1", "M1", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m3 = MealNutrition("m3", "M3", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m4 = MealNutrition("m4", "M4", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)
        repository.logMeal(m3, testDate)
        assertFalse("150g carbs cumulative does not reach 200g trigger", repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        repository.logMeal(m4, testDate)
        val summary = repository.getDailySummary(testDate).value
        assertEquals(200.0f, summary.totalCarbs, 0.01f)
        assertTrue("Exactly 200.0g carbs must trigger hasHighCarbTrigger = true", summary.hasHighCarbTrigger)

        // CompensatoryEngine integration: should add +1 set
        val day1Plan = workoutRepository.getDayPlan(1)
        val adjusted = compensatoryEngine.evaluateWorkout(day1Plan, summary)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals(5, latPulldown.effectiveSets)
        assertTrue(latPulldown.isCarbBonusApplied)
    }

    @Test
    fun testCumulativeCarbs_199Point9g_doesNotTriggerHighCarbSurplus() {
        // 4 meals: 3 x 50.0g, 1 x 49.9g -> total 199.9g (< 200g)
        val m1 = MealNutrition("m1", "M1", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m3 = MealNutrition("m3", "M3", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m4 = MealNutrition("m4", "M4", 25.0f, 49.9f, 5.0f, 344, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)
        repository.logMeal(m3, testDate)
        repository.logMeal(m4, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertEquals(199.9f, summary.totalCarbs, 0.05f)
        assertFalse("199.9g carbs must NOT trigger hasHighCarbTrigger", summary.hasHighCarbTrigger)
    }

    @Test
    fun testCumulativeCarbs_200Point1g_triggersHighCarbSurplus() {
        val m1 = MealNutrition("m1", "M1", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m3 = MealNutrition("m3", "M3", 25.0f, 50.0f, 5.0f, 345, isHighCarb = false, isLowProtein = false)
        val m4 = MealNutrition("m4", "M4", 25.0f, 50.1f, 5.0f, 345, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)
        repository.logMeal(m3, testDate)
        repository.logMeal(m4, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertTrue("200.1g carbs must trigger hasHighCarbTrigger", summary.hasHighCarbTrigger)
    }

    @Test
    fun testCumulativeCarbs_singleMeal60g_overridesCumulativeRequirement() {
        // Single meal with 60g carbs triggers immediately even if total carbs is only 60g (< 200g)
        val meal = MealNutrition("m", "M", 30.0f, 60.0f, 10.0f, 450, isHighCarb = true, isLowProtein = false)
        repository.logMeal(meal, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertTrue("Single meal with 60g carbs immediately fires high carb trigger", summary.hasHighCarbTrigger)
    }

    @Test
    fun testCumulativeProtein_exactly50g_acrossTwoMeals_noDeficit() {
        // 2 meals: 25.0g protein each -> total protein = 50.0g
        // Both meals are individually >= 20g, and total protein >= 50g -> hasProteinDeficitTrigger = false
        val m1 = MealNutrition("m1", "M1", 25.0f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertEquals(50.0f, summary.totalProtein, 0.01f)
        assertFalse("Exactly 50.0g protein across 2 meals must NOT trigger protein deficit", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testCumulativeProtein_49Point9g_acrossTwoMeals_triggersDeficit() {
        // 2 meals: 24.95g protein each -> total protein = 49.9g (< 50.0g)
        // Both meals >= 20g individually, but 2 meals total < 50g -> hasProteinDeficitTrigger = true
        val m1 = MealNutrition("m1", "M1", 24.95f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 24.95f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        assertFalse("Single meal of 24.95g does not trigger deficit", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        repository.logMeal(m2, testDate)
        val summary = repository.getDailySummary(testDate).value
        assertEquals(49.9f, summary.totalProtein, 0.05f)
        assertTrue("49.9g protein across 2 meals (< 50g) MUST trigger cumulative protein deficit", summary.hasProteinDeficitTrigger)

        // CompensatoryEngine should cap all exercises at 3 sets
        val day1Plan = workoutRepository.getDayPlan(1)
        val adjusted = compensatoryEngine.evaluateWorkout(day1Plan, summary)
        val latPulldown = adjusted.exercises.first { it.baseExercise.id == "d1_lat_pulldown" }
        assertEquals("Sets capped at 3 due to cumulative deficit", 3, latPulldown.effectiveSets)
        assertTrue(latPulldown.isProteinCapApplied)
    }

    @Test
    fun testCumulativeProtein_50Point01g_acrossTwoMeals_noDeficit() {
        val m1 = MealNutrition("m1", "M1", 25.01f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 40.0f, 5.0f, 305, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertFalse("50.01g protein across 2 meals must NOT trigger protein deficit", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testCumulativeProtein_singleMealUnder20g_triggersDeficitRegardlessOfDailyTotal() {
        // Meal 1: 15.0g protein (< 20g) -> isLowProtein = true
        // Meal 2: 70.0g protein
        // Daily total = 85.0g protein (> 50g), but Meal 1 was low protein, so trigger remains active
        val m1 = MealNutrition("m1", "M1", 15.0f, 40.0f, 5.0f, 265, isHighCarb = false, isLowProtein = true)
        val m2 = MealNutrition("m2", "M2", 70.0f, 40.0f, 15.0f, 575, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertTrue("Any meal with protein < 20g preserves protein deficit trigger", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testCumulativeProtein_singleMealUnder50g_doesNotTriggerDeficitWhenOver20g() {
        // 1 single meal of 35g protein.
        // Even though 35g < 50g, mealCount is only 1, and 35g >= 20g -> hasProteinDeficitTrigger = false
        val meal = MealNutrition("m", "M", 35.0f, 45.0f, 10.0f, 410, isHighCarb = false, isLowProtein = false)
        repository.logMeal(meal, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertFalse("Single meal of 35g protein must NOT trigger deficit (requires 2 meals < 50g)", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testCumulativeProtein_threeMeals_recoveryFromTwoMealDeficit() {
        // Meal 1: 22g protein
        // Meal 2: 22g protein -> 44g < 50g (cumulative deficit triggers)
        val m1 = MealNutrition("m1", "M1", 22.0f, 40.0f, 5.0f, 293, isHighCarb = false, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 22.0f, 40.0f, 5.0f, 293, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)
        assertTrue(repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        // Meal 3: 20g protein -> total protein = 64g >= 50g. All 3 meals >= 20g. Deficit clears!
        val m3 = MealNutrition("m3", "M3", 20.0f, 40.0f, 5.0f, 285, isHighCarb = false, isLowProtein = false)
        repository.logMeal(m3, testDate)
        val summary = repository.getDailySummary(testDate).value
        assertEquals(64.0f, summary.totalProtein, 0.01f)
        assertFalse("Total protein 64g >= 50g across 3 balanced meals clears protein deficit", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testCumulativeTriggers_mealDeletionRecalculatesThresholds() {
        // Log two meals triggering carb surplus (120g carbs)
        val m1 = MealNutrition("m1", "M1", 25.0f, 65.0f, 5.0f, 405, isHighCarb = true, isLowProtein = false)
        val m2 = MealNutrition("m2", "M2", 25.0f, 30.0f, 5.0f, 265, isHighCarb = false, isLowProtein = false)

        repository.logMeal(m1, testDate)
        repository.logMeal(m2, testDate)
        assertTrue(repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        // Delete the high carb meal
        val deleted = repository.deleteMeal(m1.id, testDate)
        assertTrue(deleted)

        val summary = repository.getDailySummary(testDate).value
        assertEquals(1, summary.mealCount)
        assertEquals(30.0f, summary.totalCarbs, 0.01f)
        assertFalse("Carb trigger must clear after high-carb meal is deleted", summary.hasHighCarbTrigger)
    }

    // =========================================================================
    // SECTION 5: High-Concurrency Repository Stress Testing
    // =========================================================================

    @Test
    fun testRepositoryConcurrency_100SimultaneousMealLogs_threadSafeStateFlow() = runBlocking {
        // 100 coroutines logging meals concurrently on Dispatchers.Default
        val jobCount = 100
        val deferreds = (1..jobCount).map { i ->
            async(Dispatchers.Default) {
                val meal = MealNutrition(
                    rawQuery = "meal $i",
                    mealName = "Batch Meal $i",
                    proteinGrams = 1.0f,
                    carbsGrams = 2.0f,
                    fatsGrams = 0.5f,
                    calories = 16,
                    isHighCarb = false,
                    isLowProtein = true
                )
                repository.logMeal(meal, testDate)
            }
        }
        deferreds.awaitAll()

        val summary = repository.getDailySummary(testDate).value
        val meals = repository.getMealsForDate(testDate).value

        assertEquals("All 100 meals must be present without lost updates", 100, meals.size)
        assertEquals(100.0f, summary.totalProtein, 0.1f)
        assertEquals(200.0f, summary.totalCarbs, 0.1f)
        assertEquals(50.0f, summary.totalFats, 0.1f)
        assertEquals(1600, summary.totalCalories)
        assertEquals(100, summary.mealCount)
        assertTrue("200g carbs triggers high carb", summary.hasHighCarbTrigger)
    }

    @Test
    fun testRepositoryConcurrency_simultaneousLogAndDelete_noRaceConditions() = runBlocking {
        // First log 50 baseline meals
        val initialMeals = (1..50).map { i ->
            val meal = MealNutrition("init $i", "Meal $i", 10.0f, 10.0f, 2.0f, 98)
            repository.logMeal(meal, testDate)
        }

        // Concurrently delete the first 25 while logging 25 new ones
        val deleteJobs = initialMeals.take(25).map { meal ->
            async(Dispatchers.Default) {
                repository.deleteMeal(meal.id, testDate)
            }
        }
        val logJobs = (51..75).map { i ->
            async(Dispatchers.Default) {
                repository.logMeal(MealNutrition("new $i", "New Meal $i", 10.0f, 10.0f, 2.0f, 98), testDate)
            }
        }

        (deleteJobs + logJobs).awaitAll()

        val meals = repository.getMealsForDate(testDate).value
        val summary = repository.getDailySummary(testDate).value

        // 50 - 25 + 25 = 50 meals
        assertEquals(50, meals.size)
        assertEquals(50, summary.mealCount)
        assertEquals(500.0f, summary.totalProtein, 0.5f)
        assertEquals(500.0f, summary.totalCarbs, 0.5f)
    }
}
