package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.data.nutrition.DefaultMealRepository
import com.hostelgains.data.nutrition.MealRepository
import com.hostelgains.data.nutrition.getCurrentDateIso
import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.FixtureNutritionParser
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.domain.nutrition.NutritionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MealLogUiState(
    val inputQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val meals: List<MealNutrition> = emptyList(),
    val dailySummary: DailyMacroSummary = DailyMacroSummary(
        dateIso = getCurrentDateIso(),
        totalProtein = 0f,
        totalCarbs = 0f,
        totalFats = 0f,
        totalCalories = 0,
        hasHighCarbTrigger = false,
        hasProteinDeficitTrigger = false,
        mealCount = 0
    ),
    val targetCalories: Int = 2600,
    val targetProtein: Float = 160f,
    val targetCarbs: Float = 300f,
    val targetFats: Float = 70f
)

/**
 * Shared singleton instance for default app runtime so ViewModels share repository state.
 */
object AppRepositories {
    @Volatile
    private var customMealRepository: MealRepository? = null

    @Volatile
    var apiKeyRepository: ApiKeyRepository? = null

    fun init(mealRepo: MealRepository, apiRepo: ApiKeyRepository? = null) {
        customMealRepository = mealRepo
        if (apiRepo != null) {
            apiKeyRepository = apiRepo
        }
    }

    val mealRepository: MealRepository
        get() = customMealRepository ?: synchronized(this) {
            customMealRepository ?: DefaultMealRepository().also { customMealRepository = it }
        }
}

class MealLogViewModel(
    private val mealRepository: MealRepository = AppRepositories.mealRepository,
    private val nutritionParser: NutritionParser = GeminiNutritionParser(
        apiKeyProvider = { AppRepositories.apiKeyRepository?.getApiKey() }
    ),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: CoroutineScope(Dispatchers.Default)

    private val _uiState = MutableStateFlow(
        MealLogUiState(
            meals = mealRepository.getMealsForDate().value,
            dailySummary = mealRepository.getDailySummary().value
        )
    )
    val uiState: StateFlow<MealLogUiState> = _uiState.asStateFlow()

    init {
        syncFromRepository()
    }

    private fun syncFromRepository() {
        _uiState.value = _uiState.value.copy(
            meals = mealRepository.getMealsForDate().value,
            dailySummary = mealRepository.getDailySummary().value
        )
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(
            inputQuery = newQuery,
            errorMessage = if (_uiState.value.errorMessage != null) null else _uiState.value.errorMessage
        )
    }

    fun logMeal(query: String = _uiState.value.inputQuery) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        scope.launch {
            try {
                val result = nutritionParser.parseMeal(trimmed)
                result.onSuccess { meal ->
                    mealRepository.logMeal(meal)
                    _uiState.value = _uiState.value.copy(
                        inputQuery = "",
                        isLoading = false,
                        meals = mealRepository.getMealsForDate().value,
                        dailySummary = mealRepository.getDailySummary().value
                    )
                }.onFailure { ex ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Failed to calculate macronutrients."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An unexpected error occurred."
                )
            }
        }
    }

    fun logPreset(presetText: String) {
        logMeal(presetText)
    }

    fun deleteMeal(mealId: String) {
        mealRepository.deleteMeal(mealId)
        syncFromRepository()
    }

    fun clearAllMeals() {
        mealRepository.clearAll()
        syncFromRepository()
    }
}
