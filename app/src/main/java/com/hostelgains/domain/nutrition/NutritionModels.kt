package com.hostelgains.domain.nutrition

/**
 * Detailed macronutrient breakdown of an individual food component in a mess meal.
 */
data class FoodItemNutrition(
    val name: String,
    val quantityDescription: String,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatsGrams: Float,
    val calories: Int
)

/**
 * Complete macronutrient representation of a logged hostel mess meal.
 */
data class MealNutrition(
    val rawQuery: String,
    val mealName: String,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatsGrams: Float,
    val calories: Int,
    val items: List<FoodItemNutrition> = emptyList(),
    val isHighCarb: Boolean = carbsGrams >= 60.0f,
    val isLowProtein: Boolean = proteinGrams < 20.0f,
    val dietaryNotes: String = "",
    val id: String = java.util.UUID.randomUUID().toString()
)

/**
 * Aggregated daily macronutrient status tracking cumulative volume trigger conditions.
 */
data class DailyMacroSummary(
    val dateIso: String,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFats: Float,
    val totalCalories: Int,
    val hasHighCarbTrigger: Boolean = totalCarbs >= 60.0f,
    val hasProteinDeficitTrigger: Boolean = totalProtein < 20.0f,
    val mealCount: Int = 0
) {
    companion object {
        fun fromSingleMeal(dateIso: String, meal: MealNutrition): DailyMacroSummary {
            return DailyMacroSummary(
                dateIso = dateIso,
                totalProtein = meal.proteinGrams,
                totalCarbs = meal.carbsGrams,
                totalFats = meal.fatsGrams,
                totalCalories = meal.calories,
                hasHighCarbTrigger = meal.isHighCarb || meal.carbsGrams >= 60.0f,
                hasProteinDeficitTrigger = meal.isLowProtein || meal.proteinGrams < 20.0f,
                mealCount = 1
            )
        }

        fun calculate(dateIso: String, meals: List<MealNutrition>): DailyMacroSummary {
            if (meals.isEmpty()) {
                return DailyMacroSummary(
                    dateIso = dateIso,
                    totalProtein = 0f,
                    totalCarbs = 0f,
                    totalFats = 0f,
                    totalCalories = 0,
                    hasHighCarbTrigger = false,
                    hasProteinDeficitTrigger = false,
                    mealCount = 0
                )
            }
            val totalProtein = meals.sumOf { it.proteinGrams.toDouble() }.toFloat()
            val totalCarbs = meals.sumOf { it.carbsGrams.toDouble() }.toFloat()
            val totalFats = meals.sumOf { it.fatsGrams.toDouble() }.toFloat()
            val totalCalories = meals.sumOf { it.calories }
            val mealCount = meals.size
            val hasHighCarb = meals.any { it.isHighCarb || it.carbsGrams >= 60.0f } || totalCarbs >= 200.0f
            val hasProteinDeficit = meals.any { it.isLowProtein } || (mealCount >= 2 && totalProtein < 50.0f)

            return DailyMacroSummary(
                dateIso = dateIso,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFats = totalFats,
                totalCalories = totalCalories,
                hasHighCarbTrigger = hasHighCarb,
                hasProteinDeficitTrigger = hasProteinDeficit,
                mealCount = mealCount
            )
        }
    }
}
