package com.hostelgains.data.nutrition

import com.hostelgains.data.database.MealDao
import com.hostelgains.data.database.toDomain
import com.hostelgains.data.database.toEntity
import com.hostelgains.domain.nutrition.DailyMacroSummary
import com.hostelgains.domain.nutrition.MealNutrition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * Robust implementation of [MealRepository] backed by Room database persistence with
 * high-performance in-memory reactive [StateFlow] caches and ordered write queue.
 */
class DefaultMealRepository(
    private val dao: MealDao? = null,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : MealRepository {

    private val lock = Any()
    private val mealsFlows = mutableMapOf<String, MutableStateFlow<List<MealNutrition>>>()
    private val summaryFlows = mutableMapOf<String, MutableStateFlow<DailyMacroSummary>>()

    // Ordered write queue: loads from disk first, then applies writes in order
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            hydrate()
            for (op in writes) runCatching { op() }
        }
    }

    private suspend fun hydrate() {
        val currentDao = dao ?: return
        val rows = currentDao.all
        val byDate = rows.groupBy { it.dateIso }
        synchronized(lock) {
            byDate.forEach { (date, entityList) ->
                val flow = mealsFlows.getOrPut(date) { MutableStateFlow(emptyList()) }
                val merged = (entityList.map { it.toDomain() } + flow.value).distinctBy { it.id }
                flow.value = merged
                val summary = DailyMacroSummary.calculate(date, merged)
                summaryFlows.getOrPut(date) { MutableStateFlow(summary) }.value = summary
            }
        }
    }

    override fun logMeal(meal: MealNutrition, dateIso: String): MealNutrition {
        synchronized(lock) {
            val mealsFlow = mealsFlows.getOrPut(dateIso) { MutableStateFlow(emptyList()) }
            val updatedMeals = mealsFlow.value + meal
            mealsFlow.value = updatedMeals

            val summaryFlow = summaryFlows.getOrPut(dateIso) {
                MutableStateFlow(DailyMacroSummary.calculate(dateIso, emptyList()))
            }
            summaryFlow.value = DailyMacroSummary.calculate(dateIso, updatedMeals)

            // Queue write to Room
            writes.trySend {
                dao?.insert(meal.toEntity(dateIso))
            }

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

        val target = currentMeals[targetIndex]
        val updatedMeals = currentMeals.toMutableList().apply { removeAt(targetIndex) }
        mealsFlow.value = updatedMeals

        val summaryFlow = summaryFlows.getOrPut(dateIso) {
            MutableStateFlow(DailyMacroSummary.calculate(dateIso, emptyList()))
        }
        summaryFlow.value = DailyMacroSummary.calculate(dateIso, updatedMeals)

        // Queue delete to Room
        writes.trySend {
            dao?.deleteById(target.id)
        }

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
                MutableStateFlow(DailyMacroSummary.calculate(dateIso, emptyList()))
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
                summaryFlows[date]?.value = DailyMacroSummary.calculate(date, emptyList())
            }
            mealsFlows.clear()
            summaryFlows.clear()

            // Queue clear to Room
            writes.trySend {
                dao?.clear()
            }
        }
    }
}
