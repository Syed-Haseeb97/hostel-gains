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
import com.hostelgains.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated 3D walking avatar that steps in place on Canvas to represent daily physical activity.
 */
@Composable
fun WalkingAvatar3D(
    modifier: Modifier = Modifier,
    isWalking: Boolean = true,
    cadenceSpeed: Float = 1.0f
) {
    val transition = rememberInfiniteTransition(label = "walkCycle")
    val stridePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000 / cadenceSpeed).toInt().coerceAtLeast(300),
                easing = LinearEasing
            )
        ),
        label = "stride"
    )

    val currentPhase = if (isWalking) stridePhase else 0f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 10f
            val s = (size.height / 180f).coerceIn(0.5f, 1.5f)

            // Bobbing of torso during walk cycle
            val bobY = sin(currentPhase * 2f) * 3f * s

            val headCenter = Offset(cx, cy - 60f * s + bobY)
            val neckPoint = Offset(cx, cy - 45f * s + bobY)
            val hipCenter = Offset(cx, cy + 5f * s + bobY)

            // Arm swings (opposite to leg swings)
            val armSwing = sin(currentPhase) * 22f * s
            val legSwing = sin(currentPhase) * 28f * s

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 24f * s,
                center = Offset(cx, cy + 62f * s)
            )

            // 1. Back leg (thigh + shin)
            val backKnee = Offset(cx - legSwing * 0.5f, cy + 32f * s + bobY)
            val backFoot = Offset(cx - legSwing, cy + 60f * s)
            drawLine(
                color = Color(0xFF52525B),
                start = hipCenter,
                end = backKnee,
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF52525B),
                start = backKnee,
                end = backFoot,
                strokeWidth = 5f * s,
                cap = StrokeCap.Round
            )

            // 2. Back arm
            val backElbow = Offset(cx + armSwing * 0.4f, cy - 20f * s + bobY)
            val backHand = Offset(cx + armSwing * 0.9f, cy - 5f * s + bobY)
            drawLine(
                color = Color(0xFF52525B),
                start = neckPoint,
                end = backElbow,
                strokeWidth = 5f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF52525B),
                start = backElbow,
                end = backHand,
                strokeWidth = 4f * s,
                cap = StrokeCap.Round
            )

            // 3. Spine / Torso & Head
            drawLine(
                color = Volt,
                start = neckPoint,
                end = hipCenter,
                strokeWidth = 10f * s,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White,
                radius = 9f * s,
                center = headCenter
            )

            // 4. Front leg (highlighted in Volt)
            val frontKnee = Offset(cx + legSwing * 0.5f, cy + 32f * s + bobY)
            val frontFoot = Offset(cx + legSwing, cy + 60f * s)
            drawLine(
                color = Volt,
                start = hipCenter,
                end = frontKnee,
                strokeWidth = 7f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = frontKnee,
                end = frontFoot,
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )

            // 5. Front arm
            val frontElbow = Offset(cx - armSwing * 0.4f, cy - 20f * s + bobY)
            val frontHand = Offset(cx - armSwing * 0.9f, cy - 5f * s + bobY)
            drawLine(
                color = Volt,
                start = neckPoint,
                end = frontElbow,
                strokeWidth = 6f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = frontElbow,
                end = frontHand,
                strokeWidth = 5f * s,
                cap = StrokeCap.Round
            )
        }
    }
}
