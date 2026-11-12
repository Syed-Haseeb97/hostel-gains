package com.hostelgains.data.database

import com.hostelgains.domain.nutrition.MealNutrition

fun MealNutrition.toEntity(dateIso: String): MealEntity = MealEntity(
    id,
    dateIso,
    mealName,
    rawQuery,
    proteinGrams,
    carbsGrams,
    fatsGrams,
    calories,
    System.currentTimeMillis()
)

fun MealEntity.toDomain(): MealNutrition = MealNutrition(
    id = id,
    rawQuery = rawQuery ?: "",
    mealName = mealName ?: "",
    proteinGrams = protein,
    carbsGrams = carbs,
    fatsGrams = fats,
    calories = calories,
    items = emptyList(),
    isHighCarb = carbs >= 60.0f,
    isLowProtein = protein < 20.0f,
    dietaryNotes = ""
)
