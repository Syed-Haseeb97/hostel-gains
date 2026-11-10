package com.hostelgains.domain.nutrition

/**
 * Interface contract for parsing natural language mess meal descriptions into structured macronutrients.
 */
interface NutritionParser {
    suspend fun parseMeal(rawDescription: String): Result<MealNutrition>
}
