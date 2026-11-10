package com.hostelgains.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D point in model coordinates.
 */
data class Point3D(val x: Float, val y: Float, val z: Float)

/**
 * 3D Line segment representing a limb or apparatus rod.
 */
data class LimbSegment(
    val p1: Point3D,
    val p2: Point3D,
    val strokeWidth: Float,
    val color: Color,
    val isMuscleHighlight: Boolean = false
)

@Composable
fun Mannequin3DView(
    exerciseName: String,
    targetMuscles: List<String>,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    speed: Float = 1.0f,
    yawDeg: Float = -35f,
    pitchDeg: Float = 15f,
    autoOrbit: Boolean = false,
    onYawChanged: ((Float) -> Unit)? = null
) {
    var manualYaw by remember(yawDeg) { mutableFloatStateOf(yawDeg) }
    var manualPitch by remember(pitchDeg) { mutableFloatStateOf(pitchDeg) }

    // Dynamic animation loop for kinematics repetition
    val transition = rememberInfiniteTransition(label = "mannequinAnim")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / speed).toInt().coerceAtLeast(300),
                easing = LinearEasing
            )
        ),
        label = "animProgress"
    )

    // Orbit animation when autoOrbit is enabled
    val orbitDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing)
        ),
        label = "orbit"
    )

    val currentYaw = if (autoOrbit) orbitDeg else manualYaw
    val currentPitch = manualPitch
    val phase = if (isPlaying) animProgress else 0.5f

    Box(
        modifier = modifier
            .background(Ink)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    manualYaw = (manualYaw + dragAmount.x * 0.6f) % 360f
                    manualPitch = (manualPitch - dragAmount.y * 0.4f).coerceIn(-60f, 60f)
                    onYawChanged?.invoke(manualYaw)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 20f
            val scale = (size.height / 380f).coerceIn(0.7f, 1.6f)

            // Kinematic joint angle phase: 0 -> 1 -> 0 sinusoid
            val repFactor = (sin(phase * 2 * PI).toFloat() + 1f) / 2f

            // Generate kinematic model segments according to exercise type
            val segments = generateKinematicSegments(exerciseName, targetMuscles, repFactor)

            // Project 3D points to 2D canvas with depth sorting
            render3DModel(segments, currentYaw, currentPitch, cx, cy, scale)
        }
    }
}

/**
 * Generates 3D coordinates for skeleton and apparatus based on exercise biomechanics.
 */
private fun generateKinematicSegments(
    exerciseName: String,
    targetMuscles: List<String>,
    repFactor: Float
): List<LimbSegment> {
    val segments = mutableListOf<LimbSegment>()

    val isLatPull = exerciseName.contains("Lat Pulldown", ignoreCase = true) ||
            exerciseName.contains("Pull-up", ignoreCase = true)
    val isSquat = exerciseName.contains("Squat", ignoreCase = true)
    val isPushupOrPress = exerciseName.contains("Push", ignoreCase = true) ||
            exerciseName.contains("Press", ignoreCase = true)
    val isCurl = exerciseName.contains("Curl", ignoreCase = true)
    val isRow = exerciseName.contains("Row", ignoreCase = true)

    // Highlight colors
    val latsColor = if (targetMuscles.any { it.contains("Lat", true) || it.contains("Back", true) }) Volt else Color(0xFF4A4A55)
    val chestColor = if (targetMuscles.any { it.contains("Chest", true) }) Ember else Color(0xFF4A4A55)
    val deltsColor = if (targetMuscles.any { it.contains("Delt", true) || it.contains("Shoulder", true) }) Volt else Color(0xFF4A4A55)
    val quadsColor = if (targetMuscles.any { it.contains("Quad", true) || it.contains("Leg", true) }) Ember else Color(0xFF4A4A55)
    val armsColor = if (targetMuscles.any { it.contains("Bicep", true) || it.contains("Tricep", true) || it.contains("Arm", true) }) Volt else Color(0xFF4A4A55)
    val boneColor = Color(0xFF71717A)

    // Torso / Spine base coordinates
    val hipY = if (isSquat) 30f + repFactor * 45f else 30f
    val torsoY = if (isSquat) -30f + repFactor * 25f else -30f
    val neckY = torsoY - 20f
    val headY = neckY - 22f

    // 1. Head & Neck
    segments.add(LimbSegment(Point3D(0f, neckY, 0f), Point3D(0f, headY, 0f), 18f, boneColor))

    // 2. Torso / Ribcage (Spine)
    segments.add(LimbSegment(Point3D(0f, neckY, 0f), Point3D(0f, hipY, 0f), 24f, latsColor, isMuscleHighlight = true))

    // Chest / Upper Torso Bar
    segments.add(LimbSegment(Point3D(-26f, neckY + 8f, 4f), Point3D(26f, neckY + 8f, 4f), 14f, chestColor, isMuscleHighlight = true))

    // Pelvis Bar
    segments.add(LimbSegment(Point3D(-20f, hipY, 0f), Point3D(20f, hipY, 0f), 16f, boneColor))

    // 3. Arms & Shoulders
    val shoulderL = Point3D(-28f, neckY + 8f, 0f)
    val shoulderR = Point3D(28f, neckY + 8f, 0f)

    if (isLatPull) {
        // Arm movement: extended up high (repFactor = 0) to pulled down into sides (repFactor = 1)
        val pullDownArmY = neckY - 60f + repFactor * 50f
        val elbowY = neckY - 30f + repFactor * 35f
        val elbowX = 36f - repFactor * 8f

        val elbowL = Point3D(-elbowX, elbowY, -10f + repFactor * 10f)
        val elbowR = Point3D(elbowX, elbowY, -10f + repFactor * 10f)
        val handL = Point3D(-38f, pullDownArmY, 10f)
        val handR = Point3D(38f, pullDownArmY, 10f)

        segments.add(LimbSegment(shoulderL, elbowL, 12f, deltsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(shoulderR, elbowR, 12f, deltsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(elbowL, handL, 10f, armsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(elbowR, handR, 10f, armsColor, isMuscleHighlight = true))

        // Lat Pulldown Wide Bar apparatus
        segments.add(LimbSegment(Point3D(-55f, pullDownArmY, 10f), Point3D(55f, pullDownArmY, 10f), 6f, Volt))
        segments.add(LimbSegment(Point3D(0f, pullDownArmY - 40f, 10f), Point3D(0f, pullDownArmY, 10f), 3f, Color.White))
    } else if (isCurl) {
        // Arm curling: elbows tucked, forearms curl up
        val elbowL = Point3D(-30f, torsoY + 25f, 0f)
        val elbowR = Point3D(30f, torsoY + 25f, 0f)
        val curlHandY = (torsoY + 50f) - repFactor * 40f
        val curlHandZ = 10f + repFactor * 25f
        val handL = Point3D(-30f, curlHandY, curlHandZ)
        val handR = Point3D(30f, curlHandY, curlHandZ)

        segments.add(LimbSegment(shoulderL, elbowL, 12f, deltsColor))
        segments.add(LimbSegment(shoulderR, elbowR, 12f, deltsColor))
        segments.add(LimbSegment(elbowL, handL, 10f, armsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(elbowR, handR, 10f, armsColor, isMuscleHighlight = true))

        // Dumbbells in hands
        segments.add(LimbSegment(Point3D(-30f, curlHandY - 8f, curlHandZ), Point3D(-30f, curlHandY + 8f, curlHandZ), 12f, Ember))
        segments.add(LimbSegment(Point3D(30f, curlHandY - 8f, curlHandZ), Point3D(30f, curlHandY + 8f, curlHandZ), 12f, Ember))
    } else {
        // Default / Press / Squat arm pose
        val elbowL = Point3D(-34f, torsoY + 15f + repFactor * 15f, -10f)
        val elbowR = Point3D(34f, torsoY + 15f + repFactor * 15f, -10f)
        val handL = Point3D(-25f, torsoY - 5f + repFactor * 10f, 20f)
        val handR = Point3D(25f, torsoY - 5f + repFactor * 10f, 20f)

        segments.add(LimbSegment(shoulderL, elbowL, 12f, deltsColor))
        segments.add(LimbSegment(shoulderR, elbowR, 12f, deltsColor))
        segments.add(LimbSegment(elbowL, handL, 10f, armsColor))
        segments.add(LimbSegment(elbowR, handR, 10f, armsColor))
    }

    // 4. Legs (Thighs, Knees, Shins, Feet)
    val hipL = Point3D(-18f, hipY, 0f)
    val hipR = Point3D(18f, hipY, 0f)

    if (isSquat) {
        // Squat motion: knees push out and forward, hips drop
        val kneeY = hipY + 45f - repFactor * 15f
        val kneeZ = 25f + repFactor * 25f
        val kneeL = Point3D(-24f, kneeY, kneeZ)
        val kneeR = Point3D(24f, kneeY, kneeZ)
        val footL = Point3D(-24f, 120f, 10f)
        val footR = Point3D(24f, 120f, 10f)

        segments.add(LimbSegment(hipL, kneeL, 16f, quadsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(hipR, kneeR, 16f, quadsColor, isMuscleHighlight = true))
        segments.add(LimbSegment(kneeL, footL, 12f, boneColor))
        segments.add(LimbSegment(kneeR, footR, 12f, boneColor))
    } else {
        // Standing legs
        val kneeL = Point3D(-18f, hipY + 50f, 0f)
        val kneeR = Point3D(18f, hipY + 50f, 0f)
        val footL = Point3D(-18f, hipY + 100f, 5f)
        val footR = Point3D(18f, hipY + 100f, 5f)

        segments.add(LimbSegment(hipL, kneeL, 14f, quadsColor))
        segments.add(LimbSegment(hipR, kneeR, 14f, quadsColor))
        segments.add(LimbSegment(kneeL, footL, 12f, boneColor))
        segments.add(LimbSegment(kneeR, footR, 12f, boneColor))
    }

    return segments
}

/**
 * Projects 3D limb segments onto 2D canvas with yaw and pitch perspective.
 */
private fun DrawScope.render3DModel(
    segments: List<LimbSegment>,
    yawDeg: Float,
    pitchDeg: Float,
    cx: Float,
    cy: Float,
    scale: Float
) {
    val yawRad = yawDeg * (PI / 180f).toFloat()
    val pitchRad = pitchDeg * (PI / 180f).toFloat()

    val cosY = cos(yawRad)
    val sinY = sin(yawRad)
    val cosP = cos(pitchRad)
    val sinP = sin(pitchRad)

    // Sort segments by average Z depth (Painter's algorithm: draw farthest first)
    val projected = segments.map { seg ->
        val p1Rot = rotatePoint(seg.p1, cosY, sinY, cosP, sinP)
        val p2Rot = rotatePoint(seg.p2, cosY, sinY, cosP, sinP)
        val avgZ = (p1Rot.z + p2Rot.z) / 2f
        Triple(seg, p1Rot, p2Rot) to avgZ
    }.sortedBy { it.second }

    // Floor shadow circle
    drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = 55f * scale,
        center = Offset(cx, cy + 130f * scale)
    )

    projected.forEach { (data, _) ->
        val (seg, p1, p2) = data

        // Perspective division
        val fov = 400f
        val scale1 = (fov / (fov + p1.z)) * scale
        val scale2 = (fov / (fov + p2.z)) * scale

        val x1 = cx + p1.x * scale1
        val y1 = cy + p1.y * scale1
        val x2 = cx + p2.x * scale2
        val y2 = cy + p2.y * scale2

        val effectiveStroke = ((seg.strokeWidth * (scale1 + scale2) / 2f)).coerceAtLeast(3f)

        // Draw bone / limb line
        drawLine(
            color = seg.color,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = effectiveStroke,
            cap = StrokeCap.Round
        )

        // Highlight glow for targeted muscle groups
        if (seg.isMuscleHighlight) {
            drawLine(
                color = seg.color.copy(alpha = 0.35f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = effectiveStroke + 8f * scale,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun rotatePoint(
    p: Point3D,
    cosY: Float,
    sinY: Float,
    cosP: Float,
    sinP: Float
): Point3D {
    // 1. Yaw rotation around Y axis
    val x1 = p.x * cosY + p.z * sinY
    val y1 = p.y
    val z1 = -p.x * sinY + p.z * cosY

    // 2. Pitch rotation around X axis
    val x2 = x1
    val y2 = y1 * cosP - z1 * sinP
    val z2 = y1 * sinP + z1 * cosP

    return Point3D(x2, y2, z2)
}
