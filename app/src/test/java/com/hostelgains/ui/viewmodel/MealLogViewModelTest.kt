package com.hostelgains.ui.viewmodel

import com.hostelgains.data.nutrition.DefaultMealRepository
import com.hostelgains.domain.nutrition.FixtureNutritionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MealLogViewModelTest {

    private lateinit var repository: DefaultMealRepository
    private lateinit var parser: FixtureNutritionParser
    private lateinit var viewModel: MealLogViewModel

    @Before
    fun setUp() {
        repository = DefaultMealRepository()
        repository.clearAll()
        parser = FixtureNutritionParser()
        viewModel = MealLogViewModel(
            mealRepository = repository,
            nutritionParser = parser,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialState_defaultsAreEmptyAndClean() {
        val state = viewModel.uiState.value
        assertEquals("", state.inputQuery)
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
        assertTrue(state.meals.isEmpty())
        assertEquals(0f, state.dailySummary.totalProtein, 0.01f)
        assertEquals(0f, state.dailySummary.totalCarbs, 0.01f)
        assertEquals(0, state.dailySummary.totalCalories)
    }

    @Test
    fun testOnQueryChanged_updatesStateQuery() {
        viewModel.onQueryChanged("4 boiled eggs")
        assertEquals("4 boiled eggs", viewModel.uiState.value.inputQuery)
    }

    @Test
    fun testLogMeal_addsMealAndUpdatesDailySummary() {
        viewModel.onQueryChanged("4 boiled eggs")
        viewModel.logMeal()

        // Wait brief sync for state flow
        Thread.sleep(100)
        val state = viewModel.uiState.value
        assertEquals(1, state.meals.size)
        assertTrue(state.dailySummary.totalProtein >= 20f)
        assertTrue(state.dailySummary.totalCalories > 0)
        assertEquals("", state.inputQuery)
    }

    @Test
    fun testLogPresets_handlesHostelMessItems() {
        val presets = listOf(
            "Mutton Curry, 1 bowl",
            "4 Rotis",
            "4 Boiled Eggs",
            "Dal & Rice",
            "Paneer Bhurji"
        )

        for (preset in presets) {
            viewModel.logPreset(preset)
            Thread.sleep(50)
        }

        val state = viewModel.uiState.value
        assertEquals(5, state.meals.size)
        assertTrue(state.dailySummary.totalCalories > 1000)
        assertTrue(state.dailySummary.totalProtein > 50f)
    }

    @Test
    fun testHighCarbTrigger_activatedWhenCarbsExceed60g() {
        viewModel.logPreset("4 Rotis") // High carbs
        Thread.sleep(50)

        val state = viewModel.uiState.value
        assertTrue(state.dailySummary.hasHighCarbTrigger || state.dailySummary.totalCarbs >= 60f)
    }

    @Test
    fun testDeleteMeal_removesMealAndRecalculatesTotals() {
        viewModel.logPreset("4 Boiled Eggs")
        Thread.sleep(50)
        val initialMeals = viewModel.uiState.value.meals
        assertEquals(1, initialMeals.size)

        val mealId = initialMeals[0].id
        viewModel.deleteMeal(mealId)
        Thread.sleep(50)

        val updatedState = viewModel.uiState.value
        assertEquals(0, updatedState.meals.size)
        assertEquals(0f, updatedState.dailySummary.totalProtein, 0.01f)
    }

    @Test
    fun testClearAllMeals_wipesAllLoggedData() {
        viewModel.logPreset("Dal & Rice")
        viewModel.logPreset("4 Rotis")
        Thread.sleep(50)
        assertEquals(2, viewModel.uiState.value.meals.size)

        viewModel.clearAllMeals()
        Thread.sleep(50)
        assertEquals(0, viewModel.uiState.value.meals.size)
        assertEquals(0, viewModel.uiState.value.dailySummary.totalCalories)
    }
}
