package com.hostelgains.ui.kinematics

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val COLOR_INK = Color(0xFF0B0B0D)
private val COLOR_PANEL = Color(0xFF151519)
private val COLOR_VOLT = Color(0xFFC6FF3D)
private val COLOR_EMBER = Color(0xFFFF5A36)
private val COLOR_STEEL = Color(0xFFA1A1AA)
private val COLOR_WHITE_PLATE = Color(0xFFE4E4E7)

/**
 * Interactive 3D Olympic Barbell component rendered in Native Jetpack Compose Canvas.
 * Features 3D spatial rotation, Olympic bumper plates (Volt 45lb, Ember 25lb, Zinc 10lb),
 * dynamic pointer tilt, and bottom Volt accent glow.
 */
@Composable
fun Hero3D(
    modifier: Modifier = Modifier,
    autoSpin: Boolean = true
) {
    var spinAngle by remember { mutableFloatStateOf(0f) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var lastTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(autoSpin) {
        lastTimeMs = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                if (lastTimeMs > 0L) {
                    val dt = ((now - lastTimeMs) / 1000f).coerceAtMost(0.05f)
                    if (autoSpin) {
                        spinAngle = (spinAngle + dt * 25f) % 360f
                    }
                }
                lastTimeMs = now
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(COLOR_INK, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tiltX = (tiltX + dragAmount.x * 0.4f).coerceIn(-40f, 40f)
                        tiltY = (tiltY - dragAmount.y * 0.4f).coerceIn(-30f, 30f)
                    },
                    onDragEnd = {
                        tiltX = 0f
                        tiltY = 0f
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Bottom ambient volt glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(COLOR_VOLT.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy + 80f),
                    radius = 140f
                ),
                radius = 140f,
                center = Offset(cx, cy + 80f)
            )

            renderBarbell(
                centerX = cx,
                centerY = cy,
                spinAngle = spinAngle,
                tiltX = tiltX,
                tiltY = tiltY
            )
        }
    }
}

private fun DrawScope.renderBarbell(
    centerX: Float,
    centerY: Float,
    spinAngle: Float,
    tiltX: Float,
    tiltY: Float
) {
    val matrix = Matrix4x4.rotationY(spinAngle + tiltX) * Matrix4x4.rotationX(-15f + tiltY)

    val faces = mutableListOf<PolygonFace3D>()

    // Barbell Shaft along Z-axis
    val barSegments = 16
    val barLength = 280f
    val barRadius = 5f
    val zStep = barLength / barSegments

    for (i in 0 until barSegments) {
        val z0 = -barLength / 2f + i * zStep
        val z1 = z0 + zStep

        for (deg in 0 until 360 step 45) {
            val r0 = Math.toRadians(deg.toDouble()).toFloat()
            val r1 = Math.toRadians((deg + 45).toDouble()).toFloat()

            val x0 = barRadius * cos(r0)
            val y0 = barRadius * sin(r0)
            val x1 = barRadius * cos(r1)
            val y1 = barRadius * sin(r1)

            val p0 = matrix.transformPoint(Point3D(x0, y0, z0))
            val p1 = matrix.transformPoint(Point3D(x1, y1, z0))
            val p2 = matrix.transformPoint(Point3D(x1, y1, z1))
            val p3 = matrix.transformPoint(Point3D(x0, y0, z1))

            faces.add(PolygonFace3D(p0, p1, p2, p3, COLOR_STEEL))
        }
    }

    // Olympic Bumper Plates on both sides (Z = ±85, ±105, ±122)
    val plateSpecs = listOf(
        Triple(85f, 65f, COLOR_VOLT),
        Triple(105f, 52f, COLOR_EMBER),
        Triple(122f, 40f, COLOR_WHITE_PLATE)
    )

    for (side in listOf(-1f, 1f)) {
        for ((baseZ, radius, color) in plateSpecs) {
            val zStart = side * baseZ
            val plateThick = 9f * side
            val zEnd = zStart + plateThick

            for (deg in 0 until 360 step 30) {
                val r0 = Math.toRadians(deg.toDouble()).toFloat()
                val r1 = Math.toRadians((deg + 30).toDouble()).toFloat()

                val x0 = radius * cos(r0)
                val y0 = radius * sin(r0)
                val x1 = radius * cos(r1)
                val y1 = radius * sin(r1)

                // Outer rim face
                val p0 = matrix.transformPoint(Point3D(x0, y0, zStart))
                val p1 = matrix.transformPoint(Point3D(x1, y1, zStart))
                val p2 = matrix.transformPoint(Point3D(x1, y1, zEnd))
                val p3 = matrix.transformPoint(Point3D(x0, y0, zEnd))
                faces.add(PolygonFace3D(p0, p1, p2, p3, color))

                // Front face
                val pf0 = matrix.transformPoint(Point3D(0f, 0f, zStart))
                val pf1 = matrix.transformPoint(Point3D(x0, y0, zStart))
                val pf2 = matrix.transformPoint(Point3D(x1, y1, zStart))
                faces.add(PolygonFace3D(pf0, pf1, pf2, pf0, color))

                // Back face
                val pb0 = matrix.transformPoint(Point3D(0f, 0f, zEnd))
                val pb1 = matrix.transformPoint(Point3D(x0, y0, zEnd))
                val pb2 = matrix.transformPoint(Point3D(x1, y1, zEnd))
                faces.add(PolygonFace3D(pb0, pb2, pb1, pb0, color))
            }
        }
    }

    // Sort faces (Painter's algorithm)
    val sorted = faces.sortedBy { it.averageDepth }
    val light = Point3D(-0.3f, 0.7f, 0.6f)

    val fov = 450f
    val camDist = 380f

    for (face in sorted) {
        fun prj(p: Point3D): Offset {
            val d = p.z + camDist
            val sc = if (d > 10f) fov / d else fov / 10f
            return Offset(centerX + p.x * sc, centerY - p.y * sc)
        }

        val pt0 = prj(face.v0)
        val pt1 = prj(face.v1)
        val pt2 = prj(face.v2)
        val pt3 = prj(face.v3)

        val shaded = face.computeShadedColor(light)

        val path = Path().apply {
            moveTo(pt0.x, pt0.y)
            lineTo(pt1.x, pt1.y)
            lineTo(pt2.x, pt2.y)
            lineTo(pt3.x, pt3.y)
            close()
        }

        drawPath(path, shaded)
    }
}
