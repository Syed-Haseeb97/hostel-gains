package com.hostelgains.ui.kinematics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val COLOR_PANEL = Color(0xFF151519)
private val COLOR_VOLT = Color(0xFFC6FF3D)
private val COLOR_EMBER = Color(0xFFFF5A36)
private val COLOR_FAT = Color(0xFF38BDF8)
private val COLOR_TRACK = Color(0x2AFFFFFF)

data class MacroBarData(
    val label: String,
    val currentGrams: Float,
    val targetGrams: Float,
    val color: Color
)

/**
 * 3D Isometric Macro Nutrient Breakdown Component rendered in Native Jetpack Compose Canvas.
 * Generates true 3D extruded isometric pillars for Carbs, Protein, and Fats with front, side, and top faces.
 */
@Composable
fun Macro3DBars(
    carbsGrams: Float,
    targetCarbsGrams: Float,
    proteinGrams: Float,
    targetProteinGrams: Float,
    fatGrams: Float,
    targetFatGrams: Float,
    modifier: Modifier = Modifier
) {
    val macros = listOf(
        MacroBarData("CARBS", carbsGrams, targetCarbsGrams.coerceAtLeast(1f), COLOR_VOLT),
        MacroBarData("PROTEIN", proteinGrams, targetProteinGrams.coerceAtLeast(1f), COLOR_EMBER),
        MacroBarData("FAT", fatGrams, targetFatGrams.coerceAtLeast(1f), COLOR_FAT)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(COLOR_PANEL, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "3D MACRONUTRIENT RATIO",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalWidth = size.width
                val canvasHeight = size.height
                val barWidth = 46f
                val barDepth = 28f
                val maxHeight = canvasHeight * 0.72f

                val columnSpacing = totalWidth / (macros.size + 1)

                macros.forEachIndexed { index, data ->
                    val baseX = columnSpacing * (index + 1) - barWidth / 2f
                    val baseY = canvasHeight - 20f

                    val pct = (data.currentGrams / data.targetGrams).coerceIn(0.05f, 1.25f)
                    val fillHeight = (maxHeight * (pct / 1.0f)).coerceAtMost(maxHeight * 1.15f)

                    // Draw Background Track Pillar (isometric)
                    drawIsometricPillar(
                        x = baseX,
                        y = baseY,
                        width = barWidth,
                        depth = barDepth,
                        height = maxHeight,
                        color = COLOR_TRACK
                    )

                    // Draw Extruded Active Macro Pillar
                    drawIsometricPillar(
                        x = baseX,
                        y = baseY,
                        width = barWidth,
                        depth = barDepth,
                        height = fillHeight,
                        color = data.color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Macro value labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            macros.forEach { m ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${m.currentGrams.toInt()} / ${m.targetGrams.toInt()}g",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = m.label,
                        color = m.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawIsometricPillar(
    x: Float,
    y: Float,
    width: Float,
    depth: Float,
    height: Float,
    color: Color
) {
    val isoX = depth * 0.7f
    val isoY = depth * 0.4f

    // 1. Front face
    val frontPath = Path().apply {
        moveTo(x, y)
        lineTo(x + width, y)
        lineTo(x + width, y - height)
        lineTo(x, y - height)
        close()
    }
    drawPath(frontPath, color)

    // 2. Right side face (darkened for 3D depth)
    val rightColor = Color(
        red = color.red * 0.75f,
        green = color.green * 0.75f,
        blue = color.blue * 0.75f,
        alpha = color.alpha
    )
    val rightPath = Path().apply {
        moveTo(x + width, y)
        lineTo(x + width + isoX, y - isoY)
        lineTo(x + width + isoX, y - height - isoY)
        lineTo(x + width, y - height)
        close()
    }
    drawPath(rightPath, rightColor)

    // 3. Top face (brightened for 3D depth)
    val topColor = Color(
        red = (color.red * 1.2f).coerceAtMost(1f),
        green = (color.green * 1.2f).coerceAtMost(1f),
        blue = (color.blue * 1.2f).coerceAtMost(1f),
        alpha = color.alpha
    )
    val topPath = Path().apply {
        moveTo(x, y - height)
        lineTo(x + width, y - height)
        lineTo(x + width + isoX, y - height - isoY)
        lineTo(x + isoX, y - height - isoY)
        close()
    }
    drawPath(topPath, topColor)
}
