package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.domain.nutrition.FoodItemNutrition
import com.hostelgains.domain.nutrition.MealNutrition
import com.hostelgains.ui.components.AiModeChip
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.viewmodel.MealLogViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MEAL_SUGGESTIONS = listOf(
    "2 eggs, toast and a coffee",
    "Chicken curry, rice and salad",
    "4 rotis and hostel dal",
    "Maggi with 2 boiled eggs",
    "Paneer bhurji and 3 chapatis"
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || state.isLoading) return
        inputText = ""
        focusManager.clearFocus()
        viewModel.logMeal(trimmed)
    }

    LaunchedEffect(state.meals.size, state.isLoading) {
        if (state.meals.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem((state.meals.size * 2).coerceAtLeast(0))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0)
            .imePadding()
    ) {
        // ── Header: Meal AI · Progress ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = topPadding + 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Meal AI",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextHigh
                )
                Text(
                    text = "${summary.totalCalories} / ${state.targetCalories} kcal today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
            }
            AiModeChip(isPremium = isPremiumAi, onClick = onUnlockAiClicked)
        }

        // ── Messages Stream ─────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Welcome AI message
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                            .background(Surface1)
                            .border(1.dp, Outline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "What did you eat today?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = TextHigh
                        )
                    }
                }
            }

            // Logged meals stream (User prompt followed by AI structured breakdown)
            state.meals.forEach { meal ->
                // User message
                item(key = "user_${meal.id}") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                                .background(Ignite)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = meal.rawQuery,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                ),
                                color = OnIgnite
                            )
                        }
                    }
                }

                // AI response with Food Table breakdown
                item(key = "ai_${meal.id}") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Column(modifier = Modifier.fillMaxWidth(0.88f)) {
                            // Items breakdown table
                            if (meal.items.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Surface1)
                                        .border(1.dp, Outline, RoundedCornerShape(18.dp))
                                ) {
                                    // Header with meal name and total calories
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = meal.mealName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextHigh
                                        )
                                        Text(
                                            text = "${meal.calories} kcal",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Ignite
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Outline)
                                    )

                                    meal.items.forEachIndexed { idx, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = TextHigh
                                                )
                                                Text(
                                                    text = "${item.proteinGrams.roundToInt()}g P · ${item.carbsGrams.roundToInt()}g C · ${item.fatsGrams.roundToInt()}g F",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextMid
                                                )
                                            }
                                            Text(
                                                text = "${item.calories} kcal",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextHigh
                                            )
                                        }
                                        if (idx < meal.items.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(Outline.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    // Green "Logged to today" status bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Ignite.copy(alpha = 0.15f))
                                            .padding(horizontal = 16.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Ignite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Logged to today · ${meal.proteinGrams.roundToInt()}g protein",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Ignite
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                                        .background(Surface1)
                                        .border(1.dp, Outline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "${meal.mealName} · ${meal.calories} kcal (${meal.proteinGrams.roundToInt()}g P)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextHigh
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading state indicator
            if (state.isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(Surface1)
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Ignite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Analyzing meal with Gemini AI...", style = MaterialTheme.typography.bodySmall, color = TextMid)
                            }
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // ── Bottom Composer with Suggestion Chips ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Canvas0)
                .border(width = 1.dp, color = Outline, shape = RoundedCornerShape(0.dp))
                .padding(vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Suggestion chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MEAL_SUGGESTIONS) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface1)
                            .border(1.dp, Outline, CircleShape)
                            .clickable(role = Role.Button) { send(suggestion) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextHigh
                        )
                    }
                }
            }

            // Input pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Outline, CircleShape)
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(color = TextHigh, fontSize = 15.sp),
                    cursorBrush = SolidColor(Ignite),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(inputText) }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text("What did you eat?", style = MaterialTheme.typography.bodyMedium, color = TextLow)
                        }
                        innerTextField()
                    }
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Ignite else Surface2)
                        .clickable(
                            enabled = inputText.isNotBlank() && !state.isLoading,
                            role = Role.Button,
                            onClick = { send(inputText) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send meal query",
                        tint = if (inputText.isNotBlank()) OnIgnite else TextLow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
