package com.hostelgains.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.Volt

/**
 * Hostel-Gains Dynamic Optimizer Athletic Emblem.
 *
 * Visual Motifs:
 * - Geometric Hexagonal Shield (matte obsidian carbon plate)
 * - Olympic Knurled Barbell with heavy Volt & Ember bumper plates
 * - Aggressive V-Taper Torso & Lat Wings (sculpted in Volt #C6FF3D)
 * - Metabolic Energy Core (glowing Ember #FF5A36 chevron)
 * - Subtle ambient energy pulse animation
 */
@Composable
fun HostelGainsLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    animatedGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoGlow")
    val glowAlpha by if (animatedGlow) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.35f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(Panel)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Volt.copy(alpha = glowAlpha),
                        Ember.copy(alpha = glowAlpha * 0.6f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(size * 0.24f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.88f)) {
            val w = this.size.width
            val h = this.size.height
            val scale = w / 108f

            fun sx(x: Float) = x * scale
            fun sy(y: Float) = y * scale

            // Ambient background glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Volt.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.42f
                )
            )

            // Subtle radar rings
            drawCircle(
                color = Volt.copy(alpha = 0.12f),
                radius = w * 0.40f,
                style = Stroke(width = 1f)
            )

            // 1. Barbell Bar
            drawRect(
                color = Color(0xFF4A4C56),
                topLeft = Offset(sx(24f), sy(43f)),
                size = Size(sx(60f), sy(3f))
            )
            drawRect(
                color = Color(0xFF7B7D8A),
                topLeft = Offset(sx(24f), sy(44f)),
                size = Size(sx(60f), sy(1f))
            )

            // 2. Barbell Left Plates
            drawRect(
                color = Volt,
                topLeft = Offset(sx(25f), sy(34f)),
                size = Size(sx(3f), sy(21f))
            )
            drawRect(
                color = Color(0xFFE1FF85),
                topLeft = Offset(sx(29f), sy(30f)),
                size = Size(sx(4f), sy(29f))
            )
            drawRect(
                color = Ember,
                topLeft = Offset(sx(34f), sy(41f)),
                size = Size(sx(2.5f), sy(7f))
            )

            // 3. Barbell Right Plates
            drawRect(
                color = Ember,
                topLeft = Offset(sx(71.5f), sy(41f)),
                size = Size(sx(2.5f), sy(7f))
            )
            drawRect(
                color = Color(0xFFE1FF85),
                topLeft = Offset(sx(75f), sy(30f)),
                size = Size(sx(4f), sy(29f))
            )
            drawRect(
                color = Volt,
                topLeft = Offset(sx(80f), sy(34f)),
                size = Size(sx(3f), sy(21f))
            )

            // 4. V-Taper Left Wing
            val leftWingPath = Path().apply {
                moveTo(sx(51f), sy(28f))
                lineTo(sx(39f), sy(32f))
                lineTo(sx(37f), sy(45f))
                lineTo(sx(42f), sy(54f))
                lineTo(sx(51f), sy(75f))
                lineTo(sx(51f), sy(53f))
                lineTo(sx(45f), sy(46f))
                lineTo(sx(51f), sy(40f))
                close()
            }
            drawPath(path = leftWingPath, color = Volt, style = Fill)

            // Left wing bevel shadow
            val leftBevelPath = Path().apply {
                moveTo(sx(51f), sy(53f))
                lineTo(sx(45f), sy(46f))
                lineTo(sx(51f), sy(40f))
                close()
            }
            drawPath(path = leftBevelPath, color = Color(0xFF8DB517), style = Fill)

            // 5. V-Taper Right Wing
            val rightWingPath = Path().apply {
                moveTo(sx(57f), sy(28f))
                lineTo(sx(69f), sy(32f))
                lineTo(sx(71f), sy(45f))
                lineTo(sx(66f), sy(54f))
                lineTo(sx(57f), sy(75f))
                lineTo(sx(57f), sy(53f))
                lineTo(sx(63f), sy(46f))
                lineTo(sx(57f), sy(40f))
                close()
            }
            drawPath(path = rightWingPath, color = Volt, style = Fill)

            // Right wing bevel shadow
            val rightBevelPath = Path().apply {
                moveTo(sx(57f), sy(53f))
                lineTo(sx(63f), sy(46f))
                lineTo(sx(57f), sy(40f))
                close()
            }
            drawPath(path = rightBevelPath, color = Color(0xFF8DB517), style = Fill)

            // 6. Center Dynamic Energy Core (Ember)
            // Upper diamond
            val upperDiamond = Path().apply {
                moveTo(sx(54f), sy(29f))
                lineTo(sx(56f), sy(33f))
                lineTo(sx(54f), sy(37f))
                lineTo(sx(52f), sy(33f))
                close()
            }
            drawPath(path = upperDiamond, color = Ember, style = Fill)

            // Main core metabolic chevron
            val coreChevron = Path().apply {
                moveTo(sx(54f), sy(41f))
                lineTo(sx(61f), sy(48f))
                lineTo(sx(54f), sy(70f))
                lineTo(sx(47f), sy(48f))
                close()
            }
            drawPath(path = coreChevron, color = Ember, style = Fill)

            // Core inner flame highlight
            val innerFlame = Path().apply {
                moveTo(sx(54f), sy(45f))
                lineTo(sx(58f), sy(50f))
                lineTo(sx(54f), sy(63f))
                lineTo(sx(50f), sy(50f))
                close()
            }
            drawPath(path = innerFlame, color = Color(0xFFFFA885), style = Fill)

            // Razor waist apex
            val apexPath = Path().apply {
                moveTo(sx(52f), sy(78f))
                lineTo(sx(56f), sy(78f))
                lineTo(sx(54f), sy(84f))
                close()
            }
            drawPath(path = apexPath, color = Volt, style = Fill)
        }
    }
}

/**
 * Branded Top Bar Header featuring the Logo Emblem + Typography.
 */
@Composable
fun HostelGainsHeaderBrand(
    modifier: Modifier = Modifier,
    title: String = "HOSTEL GAINS",
    subtitle: String = "DYNAMIC OPTIMIZER"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HostelGainsLogoBadge(size = 46.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = subtitle,
                color = Volt,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
        }
    }
}
