package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.ui.components.HeroBarbell3D
import com.hostelgains.ui.components.HighCarbBadge
import com.hostelgains.ui.components.Macro3DChart
import com.hostelgains.ui.components.ProteinDeficitBadge
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.CyanAccent
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Muted
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import com.hostelgains.ui.viewmodel.MealLogViewModel

val QUICK_MEAL_PRESETS = listOf(
    "Mutton Curry, 1 bowl",
    "4 Rotis",
    "4 Boiled Eggs",
    "Dal & Rice",
    "Paneer Bhurji"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealInputScreen(
    viewModel: MealLogViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MESS MEALS",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "AI Macronutrient Calculation & Intake Tracker",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Panel)
                        .border(1.dp, BorderStroke, CircleShape)
                ) {
                    HeroBarbell3D(
                        modifier = Modifier.fillMaxSize(),
                        isSpinning = true
                    )
                }
            }
        }

        // Active Trigger Status Badges
        item {
            val hasHighCarb = state.dailySummary.hasHighCarbTrigger
            val hasProteinDeficit = state.dailySummary.hasProteinDeficitTrigger

            if (hasHighCarb || hasProteinDeficit) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasHighCarb) {
                        HighCarbBadge(modifier = Modifier.fillMaxWidth())
                    }
                    if (hasProteinDeficit) {
                        ProteinDeficitBadge(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Daily Macro Summary Card
        item {
            DailyMacroSummaryCard(
                totalCalories = state.dailySummary.totalCalories,
                targetCalories = state.targetCalories,
                totalProtein = state.dailySummary.totalProtein,
                totalCarbs = state.dailySummary.totalCarbs,
                totalFats = state.dailySummary.totalFats
            )
        }

        // 3D Isometric Macro Chart
        item {
            Macro3DChart(
                carbsGrams = state.dailySummary.totalCarbs,
                carbsTarget = state.targetCarbs,
                proteinGrams = state.dailySummary.totalProtein,
                proteinTarget = state.targetProtein,
                fatGrams = state.dailySummary.totalFats,
                fatTarget = state.targetFats
            )
        }

        // Natural Language Meal Input Box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "LOG MESS MEAL (NATURAL LANGUAGE)",
                    color = Volt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.inputQuery,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "e.g., mutton curry, 4 dry rotis, 4 boiled eggs",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        focusedBorderColor = Volt,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.logMeal() })
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage ?: "",
                            color = Ember,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "Powered by AI Nutrition Parser",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.logMeal() },
                        enabled = !state.isLoading && state.inputQuery.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Volt,
                            contentColor = Ink,
                            disabledContainerColor = Surface,
                            disabledContentColor = TextSecondary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Ink,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "ANALYZE & LOG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips
                Text(
                    text = "QUICK HOSTEL PRESETS:",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_MEAL_PRESETS.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Surface)
                                .border(1.dp, BorderStroke, RoundedCornerShape(20.dp))
                                .clickable { viewModel.logPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "+",
                                    color = Volt,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = preset,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Meals Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S LOGGED MEALS (${state.meals.size})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                if (state.meals.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = Ember,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.clearAllMeals() }
                    )
                }
            }
        }

        // Meal Item Cards
        if (state.meals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Panel)
                        .border(1.dp, BorderStroke, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No meals logged today yet.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Enter your hostel mess menu above to calculate macros.",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(state.meals, key = { it.id }) { meal ->
                MealItemCard(
                    meal = meal,
                    onDelete = { viewModel.deleteMeal(meal.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DailyMacroSummaryCard(
    totalCalories: Int,
    targetCalories: Int,
    totalProtein: Float,
    totalCarbs: Float,
    totalFats: Float
) {
    val remaining = (targetCalories - totalCalories).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, BorderStroke, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "DAILY ENERGY EXPENDITURE",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$totalCalories",
                        color = Volt,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kcal consumed",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = "$remaining kcal left",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Mini macro status pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroMiniPill(
                    label = "CARBS",
                    value = "${totalCarbs.toInt()}g",
                    color = Volt,
                    modifier = Modifier.weight(1f)
                )
                MacroMiniPill(
                    label = "PROTEIN",
                    value = "${totalProtein.toInt()}g",
                    color = Ember,
                    modifier = Modifier.weight(1f)
                )
                MacroMiniPill(
                    label = "FAT",
                    value = "${totalFats.toInt()}g",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroMiniPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MealItemCard(
    meal: MealNutrition,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, BorderStroke, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.mealName.ifBlank { meal.rawQuery },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (meal.items.isNotEmpty()) {
                        Text(
                            text = meal.items.joinToString(", ") { "${it.name} (${it.quantityDescription})" },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${meal.calories} kcal",
                        color = Volt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete meal",
                            tint = Muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Macro row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "P: ${meal.proteinGrams.toInt()}g",
                    color = Ember,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "C: ${meal.carbsGrams.toInt()}g",
                    color = Volt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "F: ${meal.fatsGrams.toInt()}g",
                    color = CyanAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (meal.isHighCarb) {
                    Text(
                        text = "• Carb Surplus",
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (meal.isLowProtein) {
                    Text(
                        text = "• Low Protein",
                        color = Ember,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
