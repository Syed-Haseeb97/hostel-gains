package com.hostelgains.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.domain.nutrition.FoodItemNutrition
import com.hostelgains.ui.theme.CarbColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.ProteinColor
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import kotlin.math.roundToInt

/**
 * Figma-accurate interactive Calorie Ring.
 * Displays value, target, and an electric lime or coral pill badge ("X left" or "X over").
 */
@Composable
fun FigmaCalorieRing(
    value: Float,
    max: Float,
    size: Dp = 190.dp,
    strokeWidth: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val targetMax = if (max > 0f) max else 2200f
    val fraction = (value / targetMax).coerceIn(0f, 1f)
    val over = value > targetMax
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(800),
        label = "calorieProgress"
    )

    val trackColor = Outline
    val progressColor = if (over) Warning else Ignite

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Background circle track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx)
            )

            // Foreground progress arc
            if (animatedProgress > 0.001f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = strokeWidth + 4.dp)
        ) {
            Text(
                text = "${value.roundToInt()}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = TextHigh
            )
            Text(
                text = "of ${targetMax.roundToInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
            Spacer(Modifier.height(6.dp))
            val badgeBg = if (over) Warning.copy(alpha = 0.15f) else Ignite.copy(alpha = 0.15f)
            val badgeFg = if (over) Warning else Ignite
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (over) "${(value - targetMax).roundToInt()} over" else "${(targetMax - value).roundToInt()} left",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = badgeFg
                )
            }
        }
    }
}

/**
 * Figma-accurate Macro progress bar with label, value/max g, and rounded track.
 */
@Composable
fun FigmaMacroBar(
    label: String,
    value: Float,
    max: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (max > 0f) (value / max).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(700),
        label = "macroProgress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextHigh
            )
            Text(
                text = "${value.roundToInt()}/${max.roundToInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Figma Daily Roast / Tough Love Card.
 */
@Composable
fun FigmaRoastCard(modifier: Modifier = Modifier) {
    val roasts = remember {
        listOf(
            "Your muscles are not going to build themselves. Believe us, we checked.",
            "Skipping leg day is how you get a chicken-leg personality.",
            "That mess dal is not a personality trait. Add some protein.",
            "Water: the cheapest thing you are still not drinking.",
            "Maggi at 2 AM is not a food group. We keep telling you.",
            "Motivation fades. Hostel mess food does not. Train accordingly."
        )
    }
    var roastIndex by remember { mutableIntStateOf(0) }
    val cardBg = CarbColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .padding(20.dp)
    ) {
        Text(
            text = "DAILY DOSE OF TOUGH LOVE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = roasts[roastIndex],
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable(role = Role.Button) {
                    roastIndex = (roastIndex + 1) % roasts.size
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Roast me again",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111111)
            )
        }
    }
}

/**
 * Food items card breakdown embedded inside AI chat messages.
 */
@Composable
fun FoodItemsCard(
    items: List<FoodItemNutrition>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Outline, RoundedCornerShape(18.dp))
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextHigh
                    )
                    Text(
                        text = "P ${item.proteinGrams.roundToInt()}g · C ${item.carbsGrams.roundToInt()}g · F ${item.fatsGrams.roundToInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMid
                    )
                }
                Text(
                    text = "${item.calories} kcal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextHigh
                )
            }
            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Outline)
                )
            }
        }

        // Green footer: Logged to today
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ignite.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                text = "Logged to today",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Ignite
            )
        }
    }
}
