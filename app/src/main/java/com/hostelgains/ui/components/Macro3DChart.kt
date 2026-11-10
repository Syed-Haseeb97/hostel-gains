package com.hostelgains.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.CyanAccent
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt

data class MacroBarData(
    val name: String,
    val current: Float,
    val target: Float,
    val color: Color
)

@Composable
fun Macro3DChart(
    carbsGrams: Float,
    carbsTarget: Float = 300f,
    proteinGrams: Float,
    proteinTarget: Float = 160f,
    fatGrams: Float,
    fatTarget: Float = 70f,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MacroBarData("CARBS", carbsGrams, carbsTarget, Volt),
        MacroBarData("PROTEIN", proteinGrams, proteinTarget, Ember),
        MacroBarData("FAT", fatGrams, fatTarget, CyanAccent)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, BorderStroke, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "3D ISOMETRIC MACRO BREAKDOWN",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hypertrophy Target",
                    color = Volt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { data ->
                    IsometricBarItem(
                        data = data,
                        modifier = Modifier
                            .weight(1f)
                            .height(170.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IsometricBarItem(
    data: MacroBarData,
    modifier: Modifier = Modifier
) {
    val targetRatio = if (data.target > 0) (data.current / data.target).coerceIn(0.05f, 1.25f) else 0.05f
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 800),
        label = "barRatio"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value label
        Text(
            text = "${data.current.toInt()}/${data.target.toInt()}g",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3D Canvas Isometric Pillar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val canvasW = size.width
            val canvasH = size.height

            val barWidth = canvasW * 0.42f
            val depthOffset = barWidth * 0.35f
            val baseLeft = (canvasW - barWidth - depthOffset) / 2f
            val baseRight = baseLeft + barWidth
            val baseBottom = canvasH - 6.dp.toPx()

            val maxBarHeight = canvasH - 30.dp.toPx()
            val fillHeight = (maxBarHeight * animatedRatio).coerceIn(10f, maxBarHeight)
            val barTop = baseBottom - fillHeight

            // Background guide frame (isometric ghost)
            val ghostTop = baseBottom - maxBarHeight
            val ghostFront = Path().apply {
                moveTo(baseLeft, baseBottom)
                lineTo(baseRight, baseBottom)
                lineTo(baseRight, ghostTop)
                lineTo(baseLeft, ghostTop)
                close()
            }
            drawPath(ghostFront, color = Color.White.copy(alpha = 0.04f))

            // Isometric Front Face
            val frontPath = Path().apply {
                moveTo(baseLeft, baseBottom)
                lineTo(baseRight, baseBottom)
                lineTo(baseRight, barTop)
                lineTo(baseLeft, barTop)
                close()
            }
            drawPath(frontPath, color = data.color.copy(alpha = 0.90f))

            // Isometric Side Face (right depth)
            val sidePath = Path().apply {
                moveTo(baseRight, baseBottom)
                lineTo(baseRight + depthOffset, baseBottom - depthOffset * 0.5f)
                lineTo(baseRight + depthOffset, barTop - depthOffset * 0.5f)
                lineTo(baseRight, barTop)
                close()
            }
            // Shaded darker to simulate 3D directional lighting
            drawPath(sidePath, color = data.color.copy(alpha = 0.60f))

            // Isometric Top Face (diamond cap)
            val topCapPath = Path().apply {
                moveTo(baseLeft, barTop)
                lineTo(baseRight, barTop)
                lineTo(baseRight + depthOffset, barTop - depthOffset * 0.5f)
                lineTo(baseLeft + depthOffset, barTop - depthOffset * 0.5f)
                close()
            }
            // Shaded brighter (highlight)
            drawPath(topCapPath, color = Color.White.copy(alpha = 0.45f))

            // Base glow dot
            drawCircle(
                color = data.color.copy(alpha = 0.4f),
                radius = 8.dp.toPx(),
                center = Offset(baseLeft + barWidth / 2f, baseBottom)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Macro Name
        Text(
            text = data.name,
            color = data.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
