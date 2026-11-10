package com.hostelgains.data.nutrition

import com.hostelgains.domain.nutrition.FixtureNutritionParser
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Automated unit test suite verifying deterministic offline parsing and Gemini API integration
 * for authentic Indian university hostel mess meals (R2, Acceptance Criteria).
 */
class NutritionParserTest {

    private lateinit var fixtureParser: FixtureNutritionParser

    @Before
    fun setUp() {
        fixtureParser = FixtureNutritionParser()
    }

    // =========================================================================
    // FixtureNutritionParser Tests
    // =========================================================================

    @Test
    fun parseMeal_muttonRotisEggs_returnsAccurateHighProteinAndCarb() = runBlocking {
        val result = fixtureParser.parseMeal("mutton, 4 dry rotis, eggs")
        assertTrue("Parsing mutton, rotis and eggs should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be >= 40g, got ${meal.proteinGrams}", meal.proteinGrams >= 40.0f)
        assertTrue("Carbs should be >= 60g, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb flag must be true", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
        assertEquals("Should parse 3 distinct food items", 3, meal.items.size)
        assertTrue("Calories should be > 600 kcal", meal.calories >= 600)
    }

    @Test
    fun parseMeal_dalAndRice_returnsLowProteinAndHighCarb() = runBlocking {
        val result = fixtureParser.parseMeal("dal and rice")
        assertTrue("Parsing dal and rice should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be < 20g (protein deficit), got ${meal.proteinGrams}", meal.proteinGrams < 20.0f)
        assertTrue("Carbs should be >= 60g (carb surplus), got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb trigger must be true", meal.isHighCarb)
        assertTrue("Protein deficit trigger must be true", meal.isLowProtein)
        assertEquals("Should parse dal and rice as 2 items", 2, meal.items.size)
    }

    @Test
    fun parseMeal_paneerAndParathas_returnsHighCarbModerateProtein() = runBlocking {
        val result = fixtureParser.parseMeal("paneer bhurji and 3 parathas")
        assertTrue("Parsing paneer and parathas should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be >= 25g, got ${meal.proteinGrams}", meal.proteinGrams >= 25.0f)
        assertTrue("Carbs should be >= 60g, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb trigger must be true", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
        assertEquals(2, meal.items.size)
    }

    @Test
    fun parseMeal_eggWhitesAndRotis_returnsBalanced() = runBlocking {
        val result = fixtureParser.parseMeal("4 boiled egg whites and 2 rotis")
        assertTrue("Parsing egg whites and rotis should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be in range 18-22g, got ${meal.proteinGrams}", meal.proteinGrams in 18.0f..22.0f)
        assertTrue("Carbs should be < 60g, got ${meal.carbsGrams}", meal.carbsGrams < 60.0f)
        assertFalse("High carb trigger must be false", meal.isHighCarb)
        assertFalse("Protein deficit flag must be false", meal.isLowProtein)
        assertEquals(2, meal.items.size)
    }

    @Test
    fun parseMeal_chaiAndSamosas_returnsHighCarbLowProtein() = runBlocking {
        val result = fixtureParser.parseMeal("chai and 2 samosas")
        assertTrue("Parsing chai and samosas should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Protein should be < 20g, got ${meal.proteinGrams}", meal.proteinGrams < 20.0f)
        assertTrue("Carbs should be >= 60g, got ${meal.carbsGrams}", meal.carbsGrams >= 60.0f)
        assertTrue("High carb trigger must be true", meal.isHighCarb)
        assertTrue("Protein deficit trigger must be true", meal.isLowProtein)
    }

    // =========================================================================
    // Heuristic Fallback Tokenizer Tests
    // =========================================================================

    @Test
    fun parseMeal_tokenFallback_chickenAndRice() = runBlocking {
        val result = fixtureParser.parseMeal("chicken and rice")
        assertTrue("Should heuristically parse chicken and rice", result.isSuccess)

        val meal = result.getOrThrow()
        assertTrue("Chicken should provide >= 25g protein", meal.proteinGrams >= 25.0f)
        assertTrue("Rice should provide carbs", meal.carbsGrams >= 45.0f)
        assertEquals(2, meal.items.size)
    }

    @Test
    fun parseMeal_tokenFallback_multiItemBreakfast() = runBlocking {
        val result = fixtureParser.parseMeal("2 cups chai and 3 rotis and 1 plate curd")
        assertTrue("Should heuristically parse chai, rotis, and curd", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(3, meal.items.size)
        assertTrue("Should have carbs from rotis and chai", meal.carbsGrams >= 60.0f)
        assertTrue(meal.isHighCarb)
    }

    @Test
    fun parseMeal_emptyOrWhitespace_returnsFailure() = runBlocking {
        val emptyResult = fixtureParser.parseMeal("")
        assertTrue("Empty string must fail", emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is IllegalArgumentException)

        val whitespaceResult = fixtureParser.parseMeal("   \t\n  ")
        assertTrue("Whitespace-only input must fail", whitespaceResult.isFailure)
        assertTrue(whitespaceResult.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun parseMeal_unrecognizedTokens_returnsFailure() = runBlocking {
        val result = fixtureParser.parseMeal("qwertyuiop asdfghjkl zxcvbnm")
        assertTrue("Unrecognized food tokens must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // =========================================================================
    // GeminiNutritionParser Offline Fallback Tests
    // =========================================================================

    @Test
    fun geminiNutritionParser_whenApiKeyMissing_fallsBackToFixtureParser() = runBlocking {
        val parser = GeminiNutritionParser(apiKey = null, fallbackParser = fixtureParser)
        val result = parser.parseMeal("mutton, 4 dry rotis, eggs")

        assertTrue("Fallback parser should execute successfully", result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue(meal.proteinGrams >= 40.0f)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun geminiNutritionParser_whenNetworkFails_gracefullyFallsBackToFixtureParser() = runBlocking {
        // Pointing to unreachable loopback port simulates network connection failure
        val parser = GeminiNutritionParser(
            apiKey = "mock_key_offline",
            fallbackParser = fixtureParser,
            baseUrl = "http://127.0.0.1:9/generateContent"
        )
        val result = parser.parseMeal("dal and rice")

        assertTrue("Network error must gracefully fall back to fixture parser", result.isSuccess)
        val meal = result.getOrThrow()
        assertTrue("Dal and rice should trigger protein deficit", meal.isLowProtein)
        assertTrue("Dal and rice should trigger carb surplus", meal.isHighCarb)
    }

    @Test
    fun geminiNutritionParser_emptyInput_returnsFailure() = runBlocking {
        val parser = GeminiNutritionParser(apiKey = "test_key", fallbackParser = fixtureParser)
        val result = parser.parseMeal("   ")

        assertTrue("Empty input to Gemini parser must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
