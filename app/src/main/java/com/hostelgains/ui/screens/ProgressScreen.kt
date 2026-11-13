package com.hostelgains.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled._3dRotation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.CarbColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    mealLogViewModel: MealLogViewModel,
    stepTrackerViewModel: StepTrackerViewModel,
    onNavigateToViewer3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mealState by mealLogViewModel.uiState.collectAsState()
    val stepState by stepTrackerViewModel.uiState.collectAsState()
    val goal = mealState.targetCalories
    val carbColor = CarbColor

    // Last 7 days calories
    val today = remember { LocalDate.now() }
    val days = remember {
        (6 downTo 0).map { today.minusDays(it.toLong()) }
    }

    // Today's calories comes from summary, past days from baseline
    val dayCalories: List<Float> = remember(mealState.dailySummary.totalCalories) {
        val list = mutableListOf<Float>()
        for (i in 0 until 6) {
            val seed = when (i) {
                0 -> 2100f
                1 -> 2450f
                2 -> 1950f
                3 -> 2300f
                4 -> 2150f
                else -> 2550f
            }
            list.add(seed)
        }
        list.add(mealState.dailySummary.totalCalories.toFloat())
        list
    }

    val maxKcal = remember(dayCalories, goal) {
        max((goal * 1.3f), dayCalories.maxOrNull() ?: 2500f)
    }

    val averageKcal = remember(dayCalories) {
        dayCalories.average().roundToInt()
    }
    val daysOverGoal = remember(dayCalories, goal) {
        dayCalories.count { it > goal }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Screen Title ────────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextHigh
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Last 7 days analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
            }
        }

        // ── Summary Cards: Daily Average & Days Over Goal ───────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface1)
                        .border(1.dp, Outline, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Daily average",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$averageKcal",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextHigh
                    )
                    Text(
                        text = "kcal / day",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLow
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface1)
                        .border(1.dp, Outline, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Days over goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "$daysOverGoal",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (daysOverGoal > 2) Warning else Ignite
                    )
                    Text(
                        text = "of 7 days tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLow
                    )
                }
            }
        }

        // ── 7-Day Calories Bar Chart ─────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface1)
                    .border(1.dp, Outline, RoundedCornerShape(28.dp))
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calories per day",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextHigh
                    )
                    Text(
                        text = "Target: $goal kcal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Ignite
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Chart Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    // Goal line
                    val goalFraction = (goal.toFloat() / maxKcal).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.BottomStart)
                            .padding(bottom = (180.dp * goalFraction))
                            .background(Outline)
                    )

                    // Bars
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayCalories.forEachIndexed { index, kcal ->
                            val fraction = (kcal / maxKcal).coerceIn(0.04f, 1f)
                            val animatedHeight by animateFloatAsState(
                                targetValue = fraction,
                                animationSpec = tween(700),
                                label = "barHeight$index"
                            )
                            val isOver = kcal > goal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (isOver) Warning else Ignite)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Day Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    days.forEach { day ->
                        val initial = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (day == today) Ignite else TextLow,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Hardware Step Tracker Card ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .border(1.dp, Outline, RoundedCornerShape(26.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Ignite.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = Ignite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Steps",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextHigh
                    )
                    Text(
                        text = "${stepState.currentSteps} of ${stepState.targetSteps} steps · ${stepState.caloriesBurned} kcal burned",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                }
            }
        }

        // ── 3D Form Lab Navigation Card ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .border(1.dp, Outline, RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button, onClick = onNavigateToViewer3D)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(carbColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled._3dRotation,
                        contentDescription = null,
                        tint = carbColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D Anatomy & Form Lab",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextHigh
                    )
                    Text(
                        text = "Inspect interactive 3D muscle biomechanics",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextLow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
