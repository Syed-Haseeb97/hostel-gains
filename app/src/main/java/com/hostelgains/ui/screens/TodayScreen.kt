package com.hostelgains.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.app.R
import com.hostelgains.ui.components.FigmaCalorieRing
import com.hostelgains.ui.components.FigmaMacroBar
import com.hostelgains.ui.components.FigmaRoastCard
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.CarbColor
import com.hostelgains.ui.theme.FatColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.ProteinColor
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    mealLogViewModel: MealLogViewModel,
    workoutViewModel: WorkoutViewModel,
    onNavigateToMeals: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mealState by mealLogViewModel.uiState.collectAsState()
    val workoutState by workoutViewModel.uiState.collectAsState()
    val summary = mealState.dailySummary

    val currentHour = remember { LocalTime.now().hour }
    val greeting = remember(currentHour) {
        when {
            currentHour < 12 -> "Good morning"
            currentHour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val dateSubtitle = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }

    val exercises = workoutState.adjustedPlan.exercises
    val doneCount = exercises.count { it.baseExercise.id in workoutState.completedExerciseIds }
    val isOverCalories = summary.totalCalories > mealState.targetCalories

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Greeting Header with Mascot ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextHigh
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, Ignite, CircleShape)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gym_cat),
                        contentDescription = "Sir Whiskers Gym Cat Mascot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }

        // ── Calorie Ring & Macro Bars Card ───────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface1)
                    .border(1.dp, Outline, RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FigmaCalorieRing(
                        value = summary.totalCalories.toFloat(),
                        max = mealState.targetCalories.toFloat(),
                        size = 180.dp
                    )
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FigmaMacroBar(
                        label = "Protein",
                        value = summary.totalProtein,
                        max = mealState.targetProtein,
                        color = ProteinColor,
                        modifier = Modifier.weight(1f)
                    )
                    FigmaMacroBar(
                        label = "Carbs",
                        value = summary.totalCarbs,
                        max = mealState.targetCarbs,
                        color = CarbColor,
                        modifier = Modifier.weight(1f)
                    )
                    FigmaMacroBar(
                        label = "Fat",
                        value = summary.totalFats,
                        max = mealState.targetFats,
                        color = FatColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Calorie Over Warning Card (if applicable) ───────────────────────
        if (isOverCalories) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Warning.copy(alpha = 0.15f))
                        .border(1.dp, Warning.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "${(summary.totalCalories - mealState.targetCalories)} kcal over target",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Warning
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "The AI coach has automatically added extra cardio volume to your routine to burn the surplus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextHigh
                    )
                }
            }
        }

        // ── Action Card 1: Log a Meal with AI ───────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Ignite)
                    .clickable(role = Role.Button, onClick = onNavigateToMeals)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OnIgnite.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = OnIgnite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Log a meal with AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnIgnite
                    )
                    Text(
                        text = "Just describe what you ate in plain words",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnIgnite.copy(alpha = 0.75f)
                    )
                }
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = OnIgnite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Action Card 2: Today's Workout ───────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .border(1.dp, Outline, RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button, onClick = onNavigateToWorkout)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = Warning,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's workout",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextHigh
                    )
                    Text(
                        text = "$doneCount of ${exercises.size} exercises completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                }
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Warning
                )
            }
        }

        // ── Daily Roast Card ────────────────────────────────────────────────
        item {
            FigmaRoastCard()
        }

        // ── Today's Meals Section ───────────────────────────────────────────
        item {
            Text(
                text = "Today's meals",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextHigh
            )
        }

        if (mealState.meals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface1)
                        .border(1.dp, Outline, RoundedCornerShape(24.dp))
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing logged yet. Tell the AI what you ate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                }
            }
        } else {
            items(mealState.meals) { meal ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface1)
                        .border(1.dp, Outline, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = meal.rawQuery,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextHigh
                        )
                        Text(
                            text = "${meal.calories} kcal",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Ignite
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "P ${meal.proteinGrams.roundToInt()}g · C ${meal.carbsGrams.roundToInt()}g · F ${meal.fatsGrams.roundToInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                }
            }
        }
    }
}
