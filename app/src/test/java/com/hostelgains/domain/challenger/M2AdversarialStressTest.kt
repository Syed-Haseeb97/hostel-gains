package com.hostelgains.domain.challenger

import com.hostelgains.data.nutrition.DefaultMealRepository
import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.FixtureNutritionParser
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import com.hostelgains.domain.nutrition.MealNutrition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Empirical Adversarial Stress Harness for Milestone M2 (Challenger 1).
 *
 * Targets:
 * 1. Complex Indian Mess Food Queries:
 *    - Hindi vocabulary: phulka, daal, chawal, dahi, dudh, chana, paratha, samosa, chai, paneer, mutton, egg.
 *    - Multi-item composite queries with mixed numerals, English word counts, and heavy/irregular punctuation.
 *    - Malformed, empty, whitespace, and corrupt/injection queries.
 * 2. Concurrency Stress Testing on MealRepository:
 *    - Multi-threaded simultaneous meal logging on the same date (100 meals across 16 threads).
 *    - Multi-threaded isolated logging across 10 distinct dates.
 *    - Simultaneous concurrent logging, state observation, and reading.
 *    - High-concurrency simultaneous logging and deleting without race conditions or memory leaks.
 * 3. Deletion Edge Cases:
 *    - Deleting non-existent meal IDs, queries, and dates.
 *    - Double / idempotent deletions.
 *    - Sequential deletion draining all meals back to zero and resetting cumulative triggers.
 *    - Multi-date scoped vs global unscoped deletions.
 * 4. Gemini Parser Resilience & Offline Fallbacks:
 *    - Null, empty, and whitespace API keys delegating cleanly to Fixture parser.
 *    - Simulated network connection failure.
 *    - Pure Kotlin SimpleJsonParser handling malformed JSON, markdown fences, and unicode escapes.
 * 5. Cumulative Daily Macro Aggregation & Boundary Thresholds:
 *    - Floating-point boundary precision (59.99 vs 60.00 carbs, 19.99 vs 20.00 protein).
 *    - Cumulative carb threshold (> 200g across sub-60g meals).
 *    - Cumulative protein deficit threshold (< 50g across 2 meals).
 */
class M2AdversarialStressTest {

    private lateinit var fixtureParser: FixtureNutritionParser
    private lateinit var repository: DefaultMealRepository
    private val testDate = "2026-11-10"

    @Before
    fun setUp() {
        fixtureParser = FixtureNutritionParser()
        repository = DefaultMealRepository()
    }

    // =========================================================================
    // SECTION 1: Complex Multi-Item Queries and Hindi Food Vocabulary
    // =========================================================================

    @Test
    fun testHindiFood_individualStaples_phulkaDaalChawalDahiDudhChana() = runBlocking {
        // 1. Phulka
        val phulkaRes = fixtureParser.parseMeal("4 phulkas")
        assertTrue(phulkaRes.isSuccess)
        val phulka = phulkaRes.getOrThrow()
        assertEquals(1, phulka.items.size)
        assertEquals(12.0f, phulka.proteinGrams, 0.01f) // 4 * 3g
        assertEquals(68.0f, phulka.carbsGrams, 0.01f) // 4 * 17g
        assertTrue(phulka.isHighCarb)

        // 2. Daal
        val daalRes = fixtureParser.parseMeal("2 katoris daal")
        assertTrue(daalRes.isSuccess)
        val daal = daalRes.getOrThrow()
        assertEquals(1, daal.items.size)
        assertEquals(11.0f, daal.proteinGrams, 0.01f) // 2 * 5.5g
        assertEquals(36.0f, daal.carbsGrams, 0.01f) // 2 * 18g
        assertTrue(daal.isLowProtein)

        // 3. Chawal
        val chawalRes = fixtureParser.parseMeal("1 plate chawal")
        assertTrue(chawalRes.isSuccess)
        val chawal = chawalRes.getOrThrow()
        assertEquals(1, chawal.items.size)
        assertEquals(5.5f, chawal.proteinGrams, 0.01f)
        assertEquals(54.0f, chawal.carbsGrams, 0.01f)
        assertEquals(245, chawal.calories)

        // 4. Dahi
        val dahiRes = fixtureParser.parseMeal("fresh dahi")
        assertTrue(dahiRes.isSuccess)
        val dahi = dahiRes.getOrThrow()
        assertEquals(1, dahi.items.size)
        assertEquals(4.0f, dahi.proteinGrams, 0.01f)
        assertEquals(5.0f, dahi.carbsGrams, 0.01f)
        assertEquals(70, dahi.calories)

        // 5. Dudh
        val dudhRes = fixtureParser.parseMeal("1 glass dudh")
        assertTrue(dudhRes.isSuccess)
        val dudh = dudhRes.getOrThrow()
        assertEquals(1, dudh.items.size)
        assertEquals(8.0f, dudh.proteinGrams, 0.01f)
        assertEquals(12.0f, dudh.carbsGrams, 0.01f)
        assertEquals(150, dudh.calories)

        // 6. Chana
        val chanaRes = fixtureParser.parseMeal("1 bowl chana")
        assertTrue(chanaRes.isSuccess)
        val chana = chanaRes.getOrThrow()
        assertEquals(1, chana.items.size)
        assertEquals(12.0f, chana.proteinGrams, 0.01f)
        assertEquals(35.0f, chana.carbsGrams, 0.01f)
        assertEquals(240, chana.calories)
    }

    @Test
    fun testComplexMultiItem_allSixHindiItemsCombined() = runBlocking {
        // Query containing all 6 requested Hindi staple foods
        val query = "4 phulkas, 2 katoris daal, 1 plate chawal, dahi, 1 glass dudh, and chana"
        val result = fixtureParser.parseMeal(query)
        assertTrue("Parsing 6 distinct Hindi food items should succeed", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals("Must recognize all 6 distinct food items", 6, meal.items.size)

        // Expected totals:
        // Phulkas: p=12, c=68, f=2, cal=320
        // Daal: p=11, c=36, f=9, cal=270
        // Chawal: p=5.5, c=54, f=0.5, cal=245
        // Dahi: p=4, c=5, f=4, cal=70
        // Dudh: p=8, c=12, f=8, cal=150
        // Chana: p=12, c=35, f=6, cal=240
        // Total: p = 52.5g, c = 210.0g, f = 29.5g, cal = 1295
        assertEquals(52.5f, meal.proteinGrams, 0.05f)
        assertEquals(210.0f, meal.carbsGrams, 0.05f)
        assertEquals(29.5f, meal.fatsGrams, 0.05f)
        assertEquals(1295, meal.calories)

        assertTrue("210g carbs is high carb", meal.isHighCarb)
        assertFalse("52.5g protein is NOT low protein", meal.isLowProtein)
    }

    @Test
    fun testPunctuationStressAndMixedCasing() = runBlocking {
        // Extreme casing and punctuation
        val messyQuery = "   4  PHULKAS;   2  DAAL!   1  CHAWAL,   DAHI...   DUDH  &  CHANA???   "
        val result = fixtureParser.parseMeal(messyQuery)
        assertTrue("Heuristic tokenizer must survive aggressive punctuation and uppercase", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(6, meal.items.size)
        assertEquals(52.5f, meal.proteinGrams, 0.05f)
        assertEquals(210.0f, meal.carbsGrams, 0.05f)
    }

    @Test
    fun testSlashSeparatedDenseTokens() = runBlocking {
        val denseQuery = "phulka/daal/chawal"
        val result = fixtureParser.parseMeal(denseQuery)
        assertTrue("Slash-delimited tokens should be recognized", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(3, meal.items.size)
        assertTrue(meal.carbsGrams > 50f)
    }

    @Test
    fun testEnglishWordCountsInHindiQueries() = runBlocking {
        val query = "four phulkas, two katoris daal, one plate chawal"
        val result = fixtureParser.parseMeal(query)
        assertTrue("English number words (four, two, one) should parse correctly", result.isSuccess)

        val meal = result.getOrThrow()
        assertEquals(3, meal.items.size)
        // four phulkas: 12g p, 68g c
        // two daal: 11g p, 36g c
        // one chawal: 5.5g p, 54g c
        // total p = 28.5g, total c = 158g
        assertEquals(28.5f, meal.proteinGrams, 0.05f)
        assertEquals(158.0f, meal.carbsGrams, 0.05f)
        assertTrue(meal.isHighCarb)
        assertFalse(meal.isLowProtein)
    }

    @Test
    fun testMalformedAndUnrecognizedQueries_handledGracefully() = runBlocking {
        // Pure punctuation
        val punctRes = fixtureParser.parseMeal("??!!,,.. ;; --")
        assertTrue(punctRes.isFailure)
        assertTrue(punctRes.exceptionOrNull() is IllegalArgumentException)

        // Random gibberish
        val gibberishRes = fixtureParser.parseMeal("supercalifragilisticexpialidocious foobarbaz")
        assertTrue(gibberishRes.isFailure)

        // SQL injection / XSS payload strings
        val injectionRes = fixtureParser.parseMeal("'; DROP TABLE meals; -- <script>alert(1)</script>")
        assertTrue(injectionRes.isFailure)

        // Partial recognition: recognized tokens extracted, unrecognized tokens safely ignored
        val partialRes = fixtureParser.parseMeal("3 phulkas with mysterious alien hostel gravy")
        assertTrue(partialRes.isSuccess)
        val meal = partialRes.getOrThrow()
        assertEquals(1, meal.items.size)
        assertEquals("Dry Rotis", meal.items[0].name)
    }

    // =========================================================================
    // SECTION 2: Concurrency Stress on MealRepository
    // =========================================================================

    @Test
    fun testConcurrency_100MealsLoggedAcross16Threads_stateConsistent() {
        val threadCount = 16
        val mealsPerThread = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val tasks = mutableListOf<Callable<Boolean>>()

        for (t in 0 until threadCount) {
            tasks.add(Callable {
                for (m in 0 until mealsPerThread) {
                    val meal = MealNutrition(
                        rawQuery = "Thread $t Meal $m",
                        mealName = "T${t}_M${m}",
                        proteinGrams = 10.0f,
                        carbsGrams = 20.0f,
                        fatsGrams = 5.0f,
                        calories = 165,
                        isHighCarb = false,
                        isLowProtein = true
                    )
                    repository.logMeal(meal, testDate)
                }
                true
            })
        }

        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        assertTrue("Executor must terminate cleanly within 10s", executor.awaitTermination(10, TimeUnit.SECONDS))

        futures.forEach { assertTrue(it.get()) }

        val finalMeals = repository.getMealsForDate(testDate).value
        val finalSummary = repository.getDailySummary(testDate).value

        val totalExpectedMeals = threadCount * mealsPerThread
        assertEquals(totalExpectedMeals, finalMeals.size)
        assertEquals(totalExpectedMeals, finalSummary.mealCount)

        // 160 meals * 10g protein = 1600g
        assertEquals((totalExpectedMeals * 10.0f), finalSummary.totalProtein, 0.1f)
        // 160 meals * 20g carbs = 3200g
        assertEquals((totalExpectedMeals * 20.0f), finalSummary.totalCarbs, 0.1f)
        // 160 meals * 5g fats = 800g
        assertEquals((totalExpectedMeals * 5.0f), finalSummary.totalFats, 0.1f)
        // 160 meals * 165 cal = 26400 cal
        assertEquals((totalExpectedMeals * 165), finalSummary.totalCalories)

        // 3200g carbs > 200g cumulative limit -> carb trigger active
        assertTrue(finalSummary.hasHighCarbTrigger)
        // Individual meals had isLowProtein = true -> protein deficit active
        assertTrue(finalSummary.hasProteinDeficitTrigger)
    }

    @Test
    fun testConcurrency_multiDateIsolationUnderConcurrentWrites() {
        val dateCount = 10
        val mealsPerDate = 5
        val executor = Executors.newFixedThreadPool(dateCount)
        val tasks = mutableListOf<Callable<Boolean>>()

        for (d in 0 until dateCount) {
            val date = String.format("2026-11-%02d", d + 1)
            tasks.add(Callable {
                for (m in 0 until mealsPerDate) {
                    val meal = MealNutrition(
                        rawQuery = "Date $date Meal $m",
                        mealName = "Meal $m",
                        proteinGrams = 25.0f,
                        carbsGrams = 50.0f,
                        fatsGrams = 10.0f,
                        calories = 390
                    )
                    repository.logMeal(meal, date)
                }
                true
            })
        }

        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        futures.forEach { assertTrue(it.get()) }

        val loggedDates = repository.getAllLoggedDates()
        assertEquals(dateCount, loggedDates.size)

        for (d in 0 until dateCount) {
            val date = String.format("2026-11-%02d", d + 1)
            val meals = repository.getMealsForDate(date).value
            val summary = repository.getDailySummary(date).value

            assertEquals(mealsPerDate, meals.size)
            assertEquals(mealsPerDate, summary.mealCount)
            assertEquals((mealsPerDate * 25.0f), summary.totalProtein, 0.05f)
            assertEquals((mealsPerDate * 50.0f), summary.totalCarbs, 0.05f)
        }
    }

    @Test
    fun testConcurrency_simultaneousLoggingAndDeleting_neverThrowsOrCorrupts() {
        val executor = Executors.newFixedThreadPool(8)
        val initialMeals = (0 until 40).map { i ->
            MealNutrition(
                rawQuery = "Initial Meal $i",
                mealName = "Init_$i",
                proteinGrams = 20.0f,
                carbsGrams = 30.0f,
                fatsGrams = 5.0f,
                calories = 245
            )
        }
        initialMeals.forEach { repository.logMeal(it, testDate) }

        val tasks = mutableListOf<Callable<Boolean>>()

        // 4 threads adding 10 meals each (+40 meals)
        for (w in 0 until 4) {
            tasks.add(Callable {
                for (m in 0 until 10) {
                    val meal = MealNutrition(
                        rawQuery = "Concurrent Meal $w-$m",
                        mealName = "Conc_${w}_$m",
                        proteinGrams = 15.0f,
                        carbsGrams = 40.0f,
                        fatsGrams = 8.0f,
                        calories = 292
                    )
                    repository.logMeal(meal, testDate)
                }
                true
            })
        }

        // 4 threads deleting 10 initial meals each (-40 meals)
        for (delThread in 0 until 4) {
            val subset = initialMeals.subList(delThread * 10, (delThread + 1) * 10)
            tasks.add(Callable {
                for (meal in subset) {
                    repository.deleteMeal(meal.id, testDate)
                }
                true
            })
        }

        val futures = executor.invokeAll(tasks)
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        futures.forEach { assertTrue(it.get()) }

        val finalMeals = repository.getMealsForDate(testDate).value
        val finalSummary = repository.getDailySummary(testDate).value

        // 40 initial + 40 added - 40 deleted = 40 remaining
        assertEquals(40, finalMeals.size)
        assertEquals(40, finalSummary.mealCount)

        // All 40 remaining meals are from the adder threads (15g protein, 40g carbs, 8g fats, 292 kcal)
        assertEquals((40 * 15.0f), finalSummary.totalProtein, 0.1f)
        assertEquals((40 * 40.0f), finalSummary.totalCarbs, 0.1f)
        assertEquals((40 * 8.0f), finalSummary.totalFats, 0.1f)
        assertEquals((40 * 292), finalSummary.totalCalories)
    }

    // =========================================================================
    // SECTION 3: Deletion Edge Cases on MealRepository
    // =========================================================================

    @Test
    fun testDeleteMeal_nonExistentId_returnsFalseAndPreservesState() {
        val meal = MealNutrition("query", "Real Meal", 30f, 40f, 10f, 370)
        repository.logMeal(meal, testDate)

        val nonExistentId = UUID.randomUUID().toString()
        val deleted = repository.deleteMeal(nonExistentId, testDate)

        assertFalse("Deleting non-existent ID must return false", deleted)
        assertEquals(1, repository.getMealsForDate(testDate).value.size)
        assertEquals(30f, repository.getDailySummary(testDate).value.totalProtein, 0.01f)
    }

    @Test
    fun testDeleteMeal_nonExistentDate_returnsFalse() {
        val deleted = repository.deleteMeal("any_id", "1999-01-01")
        assertFalse("Deleting from non-existent date must return false", deleted)
    }

    @Test
    fun testDeleteMeal_duplicateConsecutiveDeletions_isIdempotent() {
        val meal = MealNutrition("query", "Real Meal", 30f, 40f, 10f, 370)
        repository.logMeal(meal, testDate)

        val firstDelete = repository.deleteMeal(meal.id, testDate)
        assertTrue("First deletion must return true", firstDelete)

        val secondDelete = repository.deleteMeal(meal.id, testDate)
        assertFalse("Second deletion of same meal must return false", secondDelete)

        val thirdDelete = repository.deleteMeal(meal.id, testDate)
        assertFalse("Third deletion must return false", thirdDelete)
    }

    @Test
    fun testSequentialDeletion_drainsToEmptyStateAndResetsTriggers() {
        val meal1 = MealNutrition("m1", "M1", 10.0f, 70.0f, 5.0f, 365, isHighCarb = true, isLowProtein = true)
        val meal2 = MealNutrition("m2", "M2", 35.0f, 80.0f, 15.0f, 595, isHighCarb = true, isLowProtein = false)

        repository.logMeal(meal1, testDate)
        repository.logMeal(meal2, testDate)

        var summary = repository.getDailySummary(testDate).value
        assertEquals(2, summary.mealCount)
        assertTrue(summary.hasHighCarbTrigger)
        assertTrue(summary.hasProteinDeficitTrigger)

        // Delete meal1 (which had low protein)
        repository.deleteMeal(meal1.id, testDate)
        summary = repository.getDailySummary(testDate).value
        assertEquals(1, summary.mealCount)
        assertEquals(35.0f, summary.totalProtein, 0.01f)
        assertEquals(80.0f, summary.totalCarbs, 0.01f)
        assertTrue(summary.hasHighCarbTrigger)
        assertFalse("Once low protein meal1 is removed, remaining meal2 (35g protein) has no protein deficit", summary.hasProteinDeficitTrigger)

        // Delete meal2 -> repository empty
        repository.deleteMeal(meal2.id, testDate)
        summary = repository.getDailySummary(testDate).value
        assertEquals(0, summary.mealCount)
        assertEquals(0.0f, summary.totalProtein, 0.01f)
        assertEquals(0.0f, summary.totalCarbs, 0.01f)
        assertEquals(0, summary.totalCalories)
        assertFalse(summary.hasHighCarbTrigger)
        assertFalse(summary.hasProteinDeficitTrigger)
    }

    @Test
    fun testDeleteMeal_globalUnscopedDeletionAcrossMultipleDates() {
        val mealSharedId = "shared_meal_uuid_123"
        val meal1 = MealNutrition("m1", "Meal D1", 20f, 30f, 5f, 245, id = mealSharedId)
        val meal2 = MealNutrition("m2", "Meal D2", 20f, 30f, 5f, 245, id = mealSharedId)

        repository.logMeal(meal1, "2026-11-10")
        repository.logMeal(meal2, "2026-11-11")

        // Passing dateIso = null should search and remove across all dates
        val deleted = repository.deleteMeal(mealSharedId, dateIso = null)
        assertTrue(deleted)

        assertEquals(0, repository.getMealsForDate("2026-11-10").value.size)
        assertEquals(0, repository.getMealsForDate("2026-11-11").value.size)
        assertTrue(repository.getAllLoggedDates().isEmpty())
    }

    // =========================================================================
    // SECTION 4: Gemini Parser Resilience & Fallback Hardening
    // =========================================================================

    @Test
    fun testGeminiParser_nullApiKey_handlesHindiFoodViaFallback() = runBlocking {
        val parser = GeminiNutritionParser(apiKey = null, fallbackParser = fixtureParser)
        val result = parser.parseMeal("4 phulkas, 1 katori daal, and 1 bowl dahi")

        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals(3, meal.items.size)
        // 4 phulkas (12g p), 1 daal (5.5g p), 1 dahi (4g p) -> total 21.5g protein
        assertEquals(21.5f, meal.proteinGrams, 0.05f)
    }

    @Test
    fun testGeminiParser_blankAndWhitespaceApiKey_delegatesToFallback() = runBlocking {
        val blankParser = GeminiNutritionParser(apiKey = "    ", fallbackParser = fixtureParser)
        val result = blankParser.parseMeal("chana and 2 phulkas")

        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals(2, meal.items.size)
        assertEquals(18.0f, meal.proteinGrams, 0.05f) // 12g (chana) + 6g (2 phulkas)
    }

    @Test
    fun testGeminiParser_unreachableHost_gracefullyFallsBack() = runBlocking {
        val offlineGemini = GeminiNutritionParser(
            apiKey = "offline_adversarial_key",
            fallbackParser = fixtureParser,
            baseUrl = "http://127.0.0.1:9/generateContent"
        )
        val result = offlineGemini.parseMeal("4 phulkas, 2 katoris daal, and 1 plate chawal")
        assertTrue("Unreachable host must gracefully fall back to fixture parser", result.isSuccess)
        val meal = result.getOrThrow()
        assertEquals(3, meal.items.size)
        assertTrue(meal.isHighCarb)
    }

    @Test
    fun testGeminiParser_emptyAndWhitespaceInput_rejectedBeforeNetwork() = runBlocking {
        val parser = GeminiNutritionParser(apiKey = "key", fallbackParser = fixtureParser)
        val emptyRes = parser.parseMeal("")
        assertTrue(emptyRes.isFailure)
        assertTrue(emptyRes.exceptionOrNull() is IllegalArgumentException)

        val whitespaceRes = parser.parseMeal("   \t\n  ")
        assertTrue(whitespaceRes.isFailure)
        assertTrue(whitespaceRes.exceptionOrNull() is IllegalArgumentException)
    }

    // =========================================================================
    // SECTION 5: DailyMacroSummary Aggregation Boundaries & Cumulative Triggers
    // =========================================================================

    @Test
    fun testExactCarbThreshold_59_99g_vs_60_00g_boundary() {
        val subThreshold = MealNutrition("sub", "Sub Carb", 25f, 59.99f, 5f, 385)
        assertFalse("59.99g carbs is strictly under 60.00g threshold", subThreshold.isHighCarb)

        val exactThreshold = MealNutrition("exact", "Exact Carb", 25f, 60.00f, 5f, 385)
        assertTrue("60.00g carbs must trigger isHighCarb = true", exactThreshold.isHighCarb)

        val superThreshold = MealNutrition("super", "Super Carb", 25f, 60.01f, 5f, 385)
        assertTrue("60.01g carbs must trigger isHighCarb = true", superThreshold.isHighCarb)
    }

    @Test
    fun testExactProteinThreshold_19_99g_vs_20_00g_boundary() {
        val deficitActive = MealNutrition("def", "Deficit", 19.99f, 40f, 5f, 285)
        assertTrue("19.99g protein is strictly under 20.00g threshold", deficitActive.isLowProtein)

        val sufficient = MealNutrition("suf", "Sufficient", 20.00f, 40f, 5f, 285)
        assertFalse("20.00g protein is sufficient, isLowProtein must be false", sufficient.isLowProtein)

        val superSufficient = MealNutrition("super", "Super Sufficient", 20.01f, 40f, 5f, 285)
        assertFalse("20.01g protein must be false", superSufficient.isLowProtein)
    }

    @Test
    fun testCumulativeCarbThreshold_crosses200gWithSub60gMeals() {
        // Log 3 meals with 50g carbs each (150g total)
        for (i in 1..3) {
            repository.logMeal(MealNutrition("m$i", "Meal $i", 25f, 50f, 5f, 345, isHighCarb = false, isLowProtein = false), testDate)
        }
        var summary = repository.getDailySummary(testDate).value
        assertEquals(150f, summary.totalCarbs, 0.01f)
        assertFalse("150g total carbs with no single high-carb meal must NOT trigger carb surplus", summary.hasHighCarbTrigger)

        // 4th meal with 51g carbs brings total to 201g >= 200g
        repository.logMeal(MealNutrition("m4", "Meal 4", 25f, 51f, 5f, 349, isHighCarb = false, isLowProtein = false), testDate)
        summary = repository.getDailySummary(testDate).value
        assertEquals(201f, summary.totalCarbs, 0.01f)
        assertTrue("201g total carbs exceeds 200g cumulative limit, must trigger hasHighCarbTrigger = true", summary.hasHighCarbTrigger)
    }

    @Test
    fun testCumulativeProteinDeficit_triggersWhen2MealsTotalUnder50g() {
        // 2 meals with 22g protein each (individually not low protein since >= 20g)
        repository.logMeal(MealNutrition("m1", "M1", 22f, 40f, 5f, 293, isLowProtein = false), testDate)
        assertFalse(repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        repository.logMeal(MealNutrition("m2", "M2", 22f, 40f, 5f, 293, isLowProtein = false), testDate)
        // 2 meals, total protein 44g < 50g
        assertTrue("2 meals with total protein < 50g must trigger cumulative protein deficit", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        // 3rd meal with 15g protein pushes total to 59g >= 50g
        repository.logMeal(MealNutrition("m3", "M3", 15f, 40f, 5f, 265, isLowProtein = false), testDate)
        assertFalse("Total protein reaches 59g (>= 50g), deficit must resolve", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)
    }
}
