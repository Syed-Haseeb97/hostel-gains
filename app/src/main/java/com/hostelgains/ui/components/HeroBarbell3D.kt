package com.hostelgains.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive / rotating 3D Olympic Barbell ported from Hero3D.tsx.
 * Renders the bar shaft, knurling, sleeves, and colored bumper plates in 3D perspective.
 */
@Composable
fun HeroBarbell3D(
    modifier: Modifier = Modifier,
    isSpinning: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "barbellSpin")
    val angleDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "barbellAngle"
    )

    val currentAngle = if (isSpinning) angleDeg else 25f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val s = (size.width / 320f).coerceIn(0.6f, 1.4f)

            val rad = currentAngle * (PI / 180f).toFloat()
            val cosA = cos(rad)
            val sinA = sin(rad)

            // Dynamic floaty bobbing
            val floatBob = sin(rad * 2f) * 6f * s

            // Ambient Volt glow under the barbell
            drawCircle(
                color = Volt.copy(alpha = 0.15f),
                radius = 70f * s,
                center = Offset(cx, cy + 30f * s + floatBob)
            )

            // Barbell 3D rotation along Z/X plane
            val halfLength = 120f * s
            val xLeft = cx - halfLength * cosA
            val yLeft = cy + floatBob - (halfLength * 0.25f) * sinA
            val xRight = cx + halfLength * cosA
            val yRight = cy + floatBob + (halfLength * 0.25f) * sinA

            // Steel Olympic Bar Shaft
            drawLine(
                color = Color(0xFFA1A1AA),
                start = Offset(xLeft, yLeft),
                end = Offset(xRight, yRight),
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )

            // Plates on both sides (left & right sleeves)
            val leftSleeve = Offset(cx - (halfLength * 0.75f) * cosA, cy + floatBob - (halfLength * 0.75f * 0.25f) * sinA)
            val rightSleeve = Offset(cx + (halfLength * 0.75f) * cosA, cy + floatBob + (halfLength * 0.75f * 0.25f) * sinA)

            listOf(leftSleeve, rightSleeve).forEach { sleevePos ->
                // Plate 1: 45 lb / 20 kg Volt plate
                drawCircle(
                    color = Volt,
                    radius = 28f * s,
                    center = sleevePos
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 16f * s,
                    center = sleevePos
                )

                // Plate 2: 25 lb / 10 kg Ember plate
                drawCircle(
                    color = Ember,
                    radius = 20f * s,
                    center = sleevePos
                )

                // Chrome collar hub
                drawCircle(
                    color = Color.White,
                    radius = 6f * s,
                    center = sleevePos
                )
            }
        }
    }
}
