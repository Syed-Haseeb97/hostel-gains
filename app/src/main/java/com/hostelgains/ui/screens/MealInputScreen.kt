package com.hostelgains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.ui.components.AiModeChip
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.components.ChoiceChip
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.HgPrimaryButton
import com.hostelgains.ui.components.HgTextField
import com.hostelgains.ui.components.MacroBar
import com.hostelgains.ui.components.ProgressRing
import com.hostelgains.ui.components.ScreenHeader
import com.hostelgains.ui.components.SectionLabel
import com.hostelgains.ui.components.StatusPill
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.CarbColor
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.FatColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.ProteinColor
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val QUICK_MEAL_PRESETS = listOf(
    "Mutton Curry, 1 bowl",
    "4 Rotis",
    "4 Boiled Eggs",
    "Dal & Rice",
    "Paneer Bhurji"
)

@Composable
fun MealInputScreen(
    viewModel: MealLogViewModel,
    modifier: Modifier = Modifier,
    isPremiumAi: Boolean = false,
    onUnlockAiClicked: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val summary = state.dailySummary
    val focusManager = LocalFocusManager.current
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    }

    fun submit() {
        if (state.inputQuery.isBlank() || state.isLoading) return
        focusManager.clearFocus()
        viewModel.logMeal()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(top = top + 12.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = dateLabel,
                title = "Fuel",
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                AiModeChip(isPremium = isPremiumAi, onClick = onUnlockAiClicked)
            }
        }

        // ── Energy + macros ───────────────────────────────────────────────────
        item {
            HgCard(Modifier.fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cardWidth = maxWidth
                    val progress = if (state.targetCalories > 0) summary.totalCalories / state.targetCalories.toFloat() else 0f
                    val ring: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { ringSize ->
                        ProgressRing(
                            progress = progress,
                            color = Ignite,
                            strokeWidth = 11.dp,
                            modifier = Modifier.size(ringSize)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "%,d".format(summary.totalCalories),
                                    style = if (ringSize < 140.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                                    color = TextHigh,
                                    maxLines = 1
                                )
                                Text("of %,d kcal".format(state.targetCalories), style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 1)
                            }
                        }
                    }
                    val bars: @Composable (Modifier) -> Unit = { m ->
                        Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MacroBar("Protein", summary.totalProtein, state.targetProtein, ProteinColor)
                            MacroBar("Carbs", summary.totalCarbs, state.targetCarbs, CarbColor)
                            MacroBar("Fat", summary.totalFats, state.targetFats, FatColor)
                        }
                    }
                    if (cardWidth < 300.dp) {
                        // Narrow phones: stack so neither the ring nor the bars get cramped.
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ring(156.dp)
                            Spacer(Modifier.height(16.dp))
                            bars(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ring((cardWidth * 0.42f).coerceIn(120.dp, 150.dp))
                            Spacer(Modifier.width(16.dp))
                            bars(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                val remaining = state.targetCalories - summary.totalCalories
                Text(
                    text = if (remaining >= 0) "%,d kcal left today".format(remaining) else "%,d kcal over target".format(-remaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (remaining >= 0) TextMid else Warning
                )
            }
        }

        // ── Workout effects from nutrition ────────────────────────────────────
        if (summary.hasHighCarbTrigger) {
            item {
                NoticeCard(
                    icon = Icons.Filled.Grain,
                    color = Warning,
                    title = "Carb surplus",
                    body = "Extra fuel today: +1 set added to lat pulldowns and squats."
                )
            }
        }
        if (summary.hasProteinDeficitTrigger) {
            item {
                NoticeCard(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    color = Danger,
                    title = "Protein is low",
                    body = "Training volume is capped at 3 sets to protect recovery. Add eggs, paneer or dal."
                )
            }
        }

        // ── Composer ──────────────────────────────────────────────────────────
        item {
            HgCard(Modifier.fillMaxWidth()) {
                Text("Log a meal", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Describe what you ate in the mess. Macros are calculated for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
                Spacer(Modifier.height(12.dp))
                HgTextField(
                    value = state.inputQuery,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = "e.g. mutton curry, 4 rotis, 2 boiled eggs",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = state.errorMessage != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                state.errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = Danger)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Quick add", style = MaterialTheme.typography.labelMedium, color = TextLow)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUICK_MEAL_PRESETS) { preset ->
                        ChoiceChip(
                            text = preset,
                            selected = false,
                            leading = Icons.Filled.Add,
                            onClick = { viewModel.logPreset(preset) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                HgPrimaryButton(
                    text = if (state.isLoading) "Calculating…" else "Analyze & log",
                    onClick = { submit() },
                    enabled = state.inputQuery.isNotBlank(),
                    loading = state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            SectionLabel(text = "Today's meals", modifier = Modifier.padding(horizontal = 4.dp)) {
                if (state.meals.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear all", color = TextMid)
                    }
                }
            }
        }

        if (state.meals.isEmpty()) {
            item {
                HgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(28.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = TextMid)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing logged yet", style = MaterialTheme.typography.titleSmall, color = TextHigh)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Log breakfast to start tracking today's macros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                }
            }
        } else {
            items(state.meals, key = { it.id }) { meal ->
                MealCard(meal = meal, onDelete = { viewModel.deleteMeal(meal.id) })
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Surface1,
            title = { Text("Clear today's meals?", color = TextHigh) },
            text = { Text("All ${state.meals.size} logged meals and their macros will be removed.", color = TextMid) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllMeals(); confirmClear = false }) {
                    Text("Clear", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = TextMid) }
            }
        )
    }
}

@Composable
private fun NoticeCard(icon: ImageVector, color: Color, title: String, body: String) {
    HgCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = color.copy(alpha = 0.08f),
        borderColor = color.copy(alpha = 0.3f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextHigh)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealCard(meal: MealNutrition, onDelete: () -> Unit) {
    HgCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(
                    meal.mealName.ifBlank { meal.rawQuery },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meal.items.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        meal.items.joinToString(" · ") { "${it.name} (${it.quantityDescription})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 4.dp)) {
                Text("${meal.calories}", style = MaterialTheme.typography.titleLarge, color = TextHigh)
                Text("kcal", style = MaterialTheme.typography.bodySmall, color = TextLow)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete ${meal.mealName}", tint = TextLow)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroChip("Protein", meal.proteinGrams, ProteinColor, Modifier.weight(1f))
            MacroChip("Carbs", meal.carbsGrams, CarbColor, Modifier.weight(1f))
            MacroChip("Fat", meal.fatsGrams, FatColor, Modifier.weight(1f))
        }
        if (meal.isHighCarb || meal.isLowProtein) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (meal.isHighCarb) StatusPill("Carb surplus", Warning)
                if (meal.isLowProtein) StatusPill("Low protein", Danger)
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, grams: Float, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text("${grams.toInt()} g", style = MaterialTheme.typography.labelLarge, color = TextHigh, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 1)
        }
    }
}
