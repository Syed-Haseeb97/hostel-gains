package com.hostelgains.data.nutrition

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [MealRepository] reactive state aggregation, cumulative volume triggers,
 * and multi-meal dynamic updates (R2, Acceptance Criteria).
 */
class DailyNutritionAggregationTest {

    private lateinit var repository: DefaultMealRepository
    private val testDate = "2026-11-10"

    @Before
    fun setUp() {
        repository = DefaultMealRepository()
    }

    @Test
    fun initialState_emptyRepository_returnsZeroTotalsAndFalseTriggers() {
        val summary = repository.getDailySummary(testDate).value
        val meals = repository.getMealsForDate(testDate).value

        assertEquals(0f, summary.totalProtein, 0.01f)
        assertEquals(0f, summary.totalCarbs, 0.01f)
        assertEquals(0f, summary.totalFats, 0.01f)
        assertEquals(0, summary.totalCalories)
        assertEquals(0, summary.mealCount)
        assertFalse(summary.hasHighCarbTrigger)
        assertFalse(summary.hasProteinDeficitTrigger)
        assertTrue(meals.isEmpty())
    }

    @Test
    fun logSingleMeal_updatesTotalsAndStateFlow() {
        val meal = MealNutrition(
            rawQuery = "mutton, 4 dry rotis, eggs",
            mealName = "Hostel Sunday Special: Mutton, Rotis & Eggs",
            proteinGrams = 48.0f,
            carbsGrams = 68.0f,
            fatsGrams = 23.0f,
            calories = 670,
            isHighCarb = true,
            isLowProtein = false
        )

        repository.logMeal(meal, testDate)

        val summary = repository.getDailySummary(testDate).value
        val meals = repository.getMealsForDate(testDate).value

        assertEquals(1, meals.size)
        assertEquals("Hostel Sunday Special: Mutton, Rotis & Eggs", meals[0].mealName)
        assertEquals(48.0f, summary.totalProtein, 0.01f)
        assertEquals(68.0f, summary.totalCarbs, 0.01f)
        assertEquals(23.0f, summary.totalFats, 0.01f)
        assertEquals(670, summary.totalCalories)
        assertEquals(1, summary.mealCount)
        assertTrue("Carbs >= 60g must set hasHighCarbTrigger = true", summary.hasHighCarbTrigger)
        assertFalse("Protein >= 20g must set hasProteinDeficitTrigger = false", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun logMultipleMeals_accumulatesMacrosDynamically() {
        val breakfast = MealNutrition(
            rawQuery = "4 boiled egg whites and 2 rotis",
            mealName = "Breakfast",
            proteinGrams = 25.0f,
            carbsGrams = 34.0f,
            fatsGrams = 2.0f,
            calories = 254,
            isHighCarb = false,
            isLowProtein = false
        )
        val lunch = MealNutrition(
            rawQuery = "paneer bhurji and 3 parathas",
            mealName = "Lunch",
            proteinGrams = 30.0f,
            carbsGrams = 65.0f,
            fatsGrams = 26.0f,
            calories = 614,
            isHighCarb = true,
            isLowProtein = false
        )

        repository.logMeal(breakfast, testDate)
        repository.logMeal(lunch, testDate)

        val summary = repository.getDailySummary(testDate).value
        val meals = repository.getMealsForDate(testDate).value

        assertEquals(2, meals.size)
        assertEquals(55.0f, summary.totalProtein, 0.01f)
        assertEquals(99.0f, summary.totalCarbs, 0.01f)
        assertEquals(28.0f, summary.totalFats, 0.01f)
        assertEquals(868, summary.totalCalories)
        assertEquals(2, summary.mealCount)
        assertTrue("Lunch was high carb, so hasHighCarbTrigger = true", summary.hasHighCarbTrigger)
        assertFalse("Total protein is 55g (>= 50g after 2 meals), so proteinDeficit is false", summary.hasProteinDeficitTrigger)
    }

    @Test
    fun logLowProteinMeal_triggersProteinDeficitFlag() {
        val dalRice = MealNutrition(
            rawQuery = "dal and rice",
            mealName = "Hostel Mess Lunch: Dal & Rice",
            proteinGrams = 11.0f,
            carbsGrams = 72.0f,
            fatsGrams = 5.0f,
            calories = 380,
            isHighCarb = true,
            isLowProtein = true
        )

        repository.logMeal(dalRice, testDate)

        val summary = repository.getDailySummary(testDate).value
        assertTrue("Dal and rice is low protein (< 20g), must trigger protein deficit", summary.hasProteinDeficitTrigger)
        assertTrue("Dal and rice is high carb (>= 60g), must trigger carb surplus", summary.hasHighCarbTrigger)
    }

    @Test
    fun cumulativeCarbTrigger_firesWhenDailyCarbsExceed200g() {
        // Four meals each with 55g carbs (individually under 60g threshold)
        val meal1 = MealNutrition("meal 1", "Snack 1", 25.0f, 55.0f, 5.0f, 365, isHighCarb = false, isLowProtein = false)
        val meal2 = MealNutrition("meal 2", "Snack 2", 25.0f, 55.0f, 5.0f, 365, isHighCarb = false, isLowProtein = false)
        val meal3 = MealNutrition("meal 3", "Snack 3", 25.0f, 55.0f, 5.0f, 365, isHighCarb = false, isLowProtein = false)
        val meal4 = MealNutrition("meal 4", "Snack 4", 25.0f, 55.0f, 5.0f, 365, isHighCarb = false, isLowProtein = false)

        repository.logMeal(meal1, testDate)
        assertFalse("55g total carbs does not trigger carb surplus", repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        repository.logMeal(meal2, testDate)
        assertFalse("110g total carbs does not trigger carb surplus", repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        repository.logMeal(meal3, testDate)
        assertFalse("165g total carbs does not trigger carb surplus", repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        repository.logMeal(meal4, testDate)
        assertTrue("220g total carbs exceeds 200g cumulative limit; must trigger carb surplus", repository.getDailySummary(testDate).value.hasHighCarbTrigger)
    }

    @Test
    fun cumulativeProteinDeficit_firesWhenTwoMealsLoggedWithTotalProteinUnder50g() {
        // Two meals each with 22g protein (individually >= 20g, so not individually low protein)
        val meal1 = MealNutrition("meal 1", "Meal 1", 22.0f, 40.0f, 5.0f, 293, isHighCarb = false, isLowProtein = false)
        val meal2 = MealNutrition("meal 2", "Meal 2", 22.0f, 40.0f, 5.0f, 293, isHighCarb = false, isLowProtein = false)

        repository.logMeal(meal1, testDate)
        assertFalse("Single meal with 22g protein does not trigger deficit", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        repository.logMeal(meal2, testDate)
        // After 2 meals, total protein is 44g < 50g threshold
        assertTrue("2 meals with total protein < 50g must trigger cumulative protein deficit", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)

        // Adding 3rd meal with 20g protein pushes total to 64g >= 50g
        val meal3 = MealNutrition("meal 3", "Meal 3", 20.0f, 40.0f, 5.0f, 285, isHighCarb = false, isLowProtein = false)
        repository.logMeal(meal3, testDate)
        assertFalse("Total protein reaches 64g; deficit must resolve", repository.getDailySummary(testDate).value.hasProteinDeficitTrigger)
    }

    @Test
    fun deleteMeal_recalculatesTotalsAndTriggers() {
        val meal = MealNutrition(
            rawQuery = "mutton, 4 dry rotis, eggs",
            mealName = "Mutton Feast",
            proteinGrams = 48.0f,
            carbsGrams = 68.0f,
            fatsGrams = 23.0f,
            calories = 670,
            isHighCarb = true,
            isLowProtein = false
        )

        repository.logMeal(meal, testDate)
        assertEquals(1, repository.getMealsForDate(testDate).value.size)
        assertTrue(repository.getDailySummary(testDate).value.hasHighCarbTrigger)

        val deleted = repository.deleteMeal(meal.id, testDate)
        assertTrue("Deletion should succeed", deleted)

        val summary = repository.getDailySummary(testDate).value
        val meals = repository.getMealsForDate(testDate).value

        assertEquals(0, meals.size)
        assertEquals(0f, summary.totalProtein, 0.01f)
        assertEquals(0f, summary.totalCarbs, 0.01f)
        assertFalse(summary.hasHighCarbTrigger)
        assertFalse(summary.hasProteinDeficitTrigger)
    }

    @Test
    fun multipleDates_isolatedStatePerDate() {
        val date1 = "2026-11-10"
        val date2 = "2026-11-11"

        val meal1 = MealNutrition("meal1", "Lunch", 30.0f, 50.0f, 10.0f, 410)
        val meal2 = MealNutrition("meal2", "Dinner", 40.0f, 70.0f, 15.0f, 575)

        repository.logMeal(meal1, date1)
        repository.logMeal(meal2, date2)

        assertEquals(1, repository.getMealsForDate(date1).value.size)
        assertEquals(1, repository.getMealsForDate(date2).value.size)
        assertEquals(30.0f, repository.getDailySummary(date1).value.totalProtein, 0.01f)
        assertEquals(40.0f, repository.getDailySummary(date2).value.totalProtein, 0.01f)

        val dates = repository.getAllLoggedDates()
        assertEquals(listOf(date1, date2), dates)
    }

    @Test
    fun clearAll_resetsAllDatesAndFlows() {
        repository.logMeal(MealNutrition("meal", "Meal", 30f, 40f, 10f, 370), "2026-11-10")
        repository.logMeal(MealNutrition("meal", "Meal", 30f, 40f, 10f, 370), "2026-11-11")

        repository.clearAll()

        assertTrue(repository.getAllLoggedDates().isEmpty())
        assertEquals(0, repository.getMealsForDate("2026-11-10").value.size)
        assertEquals(0f, repository.getDailySummary("2026-11-10").value.totalProtein, 0.01f)
    }
}
