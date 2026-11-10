package com.hostelgains.data.nutrition

import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Standard ISO date string generator (YYYY-MM-DD) for meal logging.
 */
fun getCurrentDateIso(): String = LocalDate.now().toString()

/**
 * Repository interface for logging mess meals, managing state, and querying daily macronutrient totals.
 */
interface MealRepository {

    /**
     * Logs a new meal for [dateIso] and dynamically recalculates [DailyMacroSummary].
     */
    fun logMeal(meal: MealNutrition, dateIso: String = getCurrentDateIso()): MealNutrition

    /**
     * Deletes a meal matching [mealId] (or raw query / meal name).
     * If [dateIso] is null, searches across all dates.
     * Returns true if a meal was found and removed.
     */
    fun deleteMeal(mealId: String, dateIso: String? = null): Boolean

    /**
     * Returns a reactive [StateFlow] of all logged meals for [dateIso].
     */
    fun getMealsForDate(dateIso: String = getCurrentDateIso()): StateFlow<List<MealNutrition>>

    /**
     * Returns a reactive [StateFlow] of aggregated macronutrients and cumulative triggers for [dateIso].
     */
    fun getDailySummary(dateIso: String = getCurrentDateIso()): StateFlow<DailyMacroSummary>

    /**
     * Returns all ISO date strings with active logged meals.
     */
    fun getAllLoggedDates(): List<String>

    /**
     * Clears all repository state.
     */
    fun clearAll()
}

/**
 * Thread-safe in-memory implementation of [MealRepository] with reactive [StateFlow] streams.
 */
class DefaultMealRepository : MealRepository {

    private val lock = Any()
    private val mealsFlows = mutableMapOf<String, MutableStateFlow<List<MealNutrition>>>()
    private val summaryFlows = mutableMapOf<String, MutableStateFlow<DailyMacroSummary>>()

    override fun logMeal(meal: MealNutrition, dateIso: String): MealNutrition {
        synchronized(lock) {
            val mealsFlow = mealsFlows.getOrPut(dateIso) { MutableStateFlow(emptyList()) }
            val updatedMeals = mealsFlow.value + meal
            mealsFlow.value = updatedMeals

            val summaryFlow = summaryFlows.getOrPut(dateIso) {
                MutableStateFlow(
                    DailyMacroSummary(
                        dateIso = dateIso,
                        totalProtein = 0f,
                        totalCarbs = 0f,
                        totalFats = 0f,
                        totalCalories = 0,
                        hasHighCarbTrigger = false,
                        hasProteinDeficitTrigger = false,
                        mealCount = 0
                    )
                )
            }
            summaryFlow.value = DailyMacroSummary.calculate(dateIso, updatedMeals)
            return meal
        }
    }

    override fun deleteMeal(mealId: String, dateIso: String?): Boolean {
        synchronized(lock) {
            return if (dateIso != null) {
                deleteFromDate(mealId, dateIso)
            } else {
                var deleted = false
                for (date in mealsFlows.keys.toList()) {
                    if (deleteFromDate(mealId, date)) {
                        deleted = true
                    }
                }
                deleted
            }
        }
    }

    private fun deleteFromDate(mealId: String, dateIso: String): Boolean {
        val mealsFlow = mealsFlows[dateIso] ?: return false
        val currentMeals = mealsFlow.value
        val targetIndex = currentMeals.indexOfFirst {
            it.id == mealId || it.rawQuery == mealId || it.mealName == mealId
        }
        if (targetIndex == -1) return false

        val updatedMeals = currentMeals.toMutableList().apply { removeAt(targetIndex) }
        mealsFlow.value = updatedMeals

        val summaryFlow = summaryFlows.getOrPut(dateIso) {
            MutableStateFlow(
                DailyMacroSummary(
                    dateIso = dateIso,
                    totalProtein = 0f,
                    totalCarbs = 0f,
                    totalFats = 0f,
                    totalCalories = 0,
                    hasHighCarbTrigger = false,
                    hasProteinDeficitTrigger = false,
                    mealCount = 0
                )
            )
        }
        summaryFlow.value = DailyMacroSummary.calculate(dateIso, updatedMeals)
        return true
    }

    override fun getMealsForDate(dateIso: String): StateFlow<List<MealNutrition>> {
        synchronized(lock) {
            return mealsFlows.getOrPut(dateIso) {
                MutableStateFlow(emptyList())
            }.asStateFlow()
        }
    }

    override fun getDailySummary(dateIso: String): StateFlow<DailyMacroSummary> {
        synchronized(lock) {
            return summaryFlows.getOrPut(dateIso) {
                MutableStateFlow(
                    DailyMacroSummary(
                        dateIso = dateIso,
                        totalProtein = 0f,
                        totalCarbs = 0f,
                        totalFats = 0f,
                        totalCalories = 0,
                        hasHighCarbTrigger = false,
                        hasProteinDeficitTrigger = false,
                        mealCount = 0
                    )
                )
            }.asStateFlow()
        }
    }

    override fun getAllLoggedDates(): List<String> {
        synchronized(lock) {
            return mealsFlows.filter { it.value.value.isNotEmpty() }.keys.sorted()
        }
    }

    override fun clearAll() {
        synchronized(lock) {
            for ((date, flow) in mealsFlows) {
                flow.value = emptyList()
                summaryFlows[date]?.value = DailyMacroSummary(
                    dateIso = date,
                    totalProtein = 0f,
                    totalCarbs = 0f,
                    totalFats = 0f,
                    totalCalories = 0,
                    hasHighCarbTrigger = false,
                    hasProteinDeficitTrigger = false,
                    mealCount = 0
                )
            }
            mealsFlows.clear()
            summaryFlows.clear()
        }
    }
}
