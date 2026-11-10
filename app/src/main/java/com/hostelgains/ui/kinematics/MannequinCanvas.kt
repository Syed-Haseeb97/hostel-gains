package com.hostelgains.ui.kinematics

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Theme color constants
private val COLOR_INK = Color(0xFF0B0B0D)
private val COLOR_PANEL = Color(0xFF151519)
private val COLOR_VOLT = Color(0xFFC6FF3D)
private val COLOR_EMBER = Color(0xFFFF5A36)
private val COLOR_SKIN = Color(0xFF52525B)
private val COLOR_JOINT = Color(0xFF27272A)
private val COLOR_LIGHT_ZINC = Color(0xFFD4D4D8)
private val COLOR_BENCH = Color(0xFF18181B)

/**
 * 3D Procedural Mesh Box Builder for anatomical segments.
 */
private class BoxBuilder {
    val faces = mutableListOf<PolygonFace3D>()

    fun addBox(
        width: Float,
        height: Float,
        depth: Float,
        matrix: Matrix4x4,
        color: Color,
        glow: Boolean = false,
        offsetX: Float = -width / 2f,
        offsetY: Float = 0f,
        offsetZ: Float = -depth / 2f
    ) {
        val x0 = offsetX
        val x1 = offsetX + width
        val y0 = offsetY
        val y1 = offsetY + height
        val z0 = offsetZ
        val z1 = offsetZ + depth

        // 8 local vertices
        val p000 = matrix.transformPoint(Point3D(x0, y0, z0))
        val p100 = matrix.transformPoint(Point3D(x1, y0, z0))
        val p110 = matrix.transformPoint(Point3D(x1, y1, z0))
        val p010 = matrix.transformPoint(Point3D(x0, y1, z0))

        val p001 = matrix.transformPoint(Point3D(x0, y0, z1))
        val p101 = matrix.transformPoint(Point3D(x1, y0, z1))
        val p111 = matrix.transformPoint(Point3D(x1, y1, z1))
        val p011 = matrix.transformPoint(Point3D(x0, y1, z1))

        // 6 quad faces
        // Front (Z+)
        faces.add(PolygonFace3D(p001, p101, p111, p011, color, glow))
        // Back (Z-)
        faces.add(PolygonFace3D(p100, p000, p010, p110, color, glow))
        // Left (X-)
        faces.add(PolygonFace3D(p000, p001, p011, p010, color, glow))
        // Right (X+)
        faces.add(PolygonFace3D(p101, p100, p110, p111, color, glow))
        // Top (Y+)
        faces.add(PolygonFace3D(p011, p111, p110, p010, color, glow))
        // Bottom (Y-)
        faces.add(PolygonFace3D(p000, p100, p101, p001, color, glow))
    }
}

/**
 * Native Jetpack Compose 3D Canvas Mesh Engine.
 * Features 4x4 affine matrix transformation, forward kinematics pose interpolation,
 * flat polygon directional shading, Painter's algorithm depth sorting, and interactive touch orbiting.
 */
@Composable
fun MannequinCanvas(
    move: ExerciseMove,
    modifier: Modifier = Modifier,
    initialYaw: Float = -35f,
    initialPitch: Float = -14f,
    showControls: Boolean = true
) {
    val solver = remember { BiomechanicalKinematicsSolver() }

    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var autoOrbit by remember { mutableStateOf(true) }
    var yaw by remember { mutableFloatStateOf(initialYaw) }
    var pitch by remember { mutableFloatStateOf(initialPitch) }

    var animationClock by remember { mutableFloatStateOf(0f) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    // Real-time smooth animation loop
    LaunchedEffect(isPlaying, speed, autoOrbit) {
        lastFrameTime = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs ->
                if (lastFrameTime > 0L) {
                    val dt = ((frameTimeMs - lastFrameTime) / 1000f).coerceAtMost(0.05f)
                    if (isPlaying) {
                        animationClock += dt * speed
                    }
                    if (autoOrbit) {
                        yaw += dt * 18f
                        if (yaw > 360f) yaw -= 360f
                    }
                }
                lastFrameTime = frameTimeMs
            }
        }
    }

    val currentPhase = solver.calculatePhase(animationClock, move.periodSeconds)
    val currentPose = solver.interpolatePose(move, currentPhase)

    Column(
        modifier = modifier
            .background(COLOR_INK)
            .padding(8.dp)
    ) {
        // Exercise header display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D BIOMECHANICAL GUIDE",
                    color = COLOR_EMBER,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = move.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "${(currentPhase * 100).toInt()}%",
                color = COLOR_VOLT,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 3D Canvas Box with touch drag orbiting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(COLOR_PANEL, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { autoOrbit = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            yaw += dragAmount.x * 0.5f
                            pitch = (pitch - dragAmount.y * 0.3f).coerceIn(-60f, 60f)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                render3DMannequin(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    move = move,
                    pose = currentPose,
                    yaw = yaw,
                    pitch = pitch,
                    solver = solver
                )
            }

            Text(
                text = "DRAG TO ORBIT",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        if (showControls) {
            Spacer(modifier = Modifier.height(10.dp))

            // Playback and Speed Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isPlaying = !isPlaying },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) COLOR_VOLT else COLOR_SKIN,
                        contentColor = COLOR_INK
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (isPlaying) "Pause" else "Play", fontWeight = FontWeight.Bold)
                }

                listOf(0.5f, 1.0f, 1.5f).forEach { s ->
                    FilterChip(
                        selected = (speed == s),
                        onClick = { speed = s },
                        label = { Text("${s}x", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = COLOR_VOLT,
                            selectedLabelColor = COLOR_INK,
                            containerColor = COLOR_PANEL,
                            labelColor = Color.White
                        )
                    )
                }

                FilterChip(
                    selected = autoOrbit,
                    onClick = { autoOrbit = !autoOrbit },
                    label = { Text("Orbit", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = COLOR_VOLT,
                        selectedLabelColor = COLOR_INK,
                        containerColor = COLOR_PANEL,
                        labelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // View Angle Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Front" to 90f,
                    "Side" to 0f,
                    "3/4 View" to -35f,
                    "Back" to -90f
                ).forEach { (label, presetYaw) ->
                    Button(
                        onClick = {
                            yaw = presetYaw
                            pitch = -14f
                            autoOrbit = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = COLOR_PANEL,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target muscles and form cues
            Card(
                colors = CardDefaults.cardColors(containerColor = COLOR_PANEL),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Target Muscles: ${move.targetMuscles.joinToString(", ")}",
                        color = COLOR_VOLT,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Form Cue: ${move.formCues.joinToString(" ")}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 3D hierarchical skeletal transformation and projection renderer.
 */
private fun DrawScope.render3DMannequin(
    canvasWidth: Float,
    canvasHeight: Float,
    move: ExerciseMove,
    pose: JointPose,
    yaw: Float,
    pitch: Float,
    solver: BiomechanicalKinematicsSolver
) {
    val builder = BoxBuilder()

    // Highlight helper
    fun colorFor(group: MuscleHighlightGroup): Color =
        if (move.highlightedGroups.contains(group)) COLOR_VOLT else COLOR_SKIN

    fun isGlowing(group: MuscleHighlightGroup): Boolean =
        move.highlightedGroups.contains(group)

    val solved = solver.solve(pose, move)
    val pelvisX = solved.hip.x
    val pelvisY = solved.hip.y

    val a1 = pose.hip
    val a2 = pose.hip - pose.knee

    // World Transformation Matrix
    val worldMatrix = Matrix4x4.rotationX(pitch) *
            Matrix4x4.rotationY(yaw) *
            Matrix4x4.translation(move.cameraOffsetX, 0f, 0f)

    // Floor grid plane
    val floorMatrix = worldMatrix * Matrix4x4.rotationX(90f)
    builder.addBox(
        width = 440f, height = 440f, depth = 2f,
        matrix = floorMatrix,
        color = Color(0x1F27272A),
        offsetX = -220f, offsetY = -220f, offsetZ = -1f
    )

    // Optional Bench for lying exercises
    if (move.isBench) {
        val benchPostMatrix = worldMatrix * Matrix4x4.translation(-60f, -40f, 0f)
        builder.addBox(40f, 80f, 36f, benchPostMatrix, COLOR_BENCH, offsetX = -20f, offsetY = 0f, offsetZ = -18f)
    }

    // Optional Pullup Bar for hanging exercises
    if (move.isHang) {
        val barMatrix = worldMatrix * Matrix4x4.translation(0f, BiomechanicalKinematicsSolver.BAR_Y, 0f)
        builder.addBox(140f, 8f, 8f, barMatrix, COLOR_LIGHT_ZINC, offsetX = -70f, offsetY = -4f, offsetZ = -4f)
    }

    // Pelvis
    val pelvisMatrix = worldMatrix * Matrix4x4.translation(pelvisX, pelvisY, 0f)
    builder.addBox(
        width = 26f, height = 22f, depth = 36f,
        matrix = pelvisMatrix,
        color = COLOR_JOINT,
        offsetX = -13f, offsetY = -11f, offsetZ = -18f
    )

    // Left and Right Legs
    for (side in listOf(-1f, 1f)) {
        val zOffset = side * 10f

        // Thigh
        val thighMatrix = pelvisMatrix *
                Matrix4x4.translation(0f, 0f, zOffset) *
                Matrix4x4.rotationZ(-a1)

        builder.addBox(
            width = 20f, height = BiomechanicalKinematicsSolver.L_THIGH, depth = 19f,
            matrix = thighMatrix,
            color = colorFor(MuscleHighlightGroup.LEGS),
            glow = isGlowing(MuscleHighlightGroup.LEGS),
            offsetX = -10f, offsetY = -BiomechanicalKinematicsSolver.L_THIGH, offsetZ = -9.5f
        )

        // Knee Joint
        val kneeMatrix = thighMatrix * Matrix4x4.translation(0f, -BiomechanicalKinematicsSolver.L_THIGH, 0f)
        builder.addBox(16f, 16f, 16f, kneeMatrix, COLOR_JOINT, offsetX = -8f, offsetY = -8f, offsetZ = -8f)

        // Shin
        val shinMatrix = kneeMatrix * Matrix4x4.rotationZ(-(a2 - a1))
        builder.addBox(
            width = 16f, height = BiomechanicalKinematicsSolver.L_SHIN, depth = 16f,
            matrix = shinMatrix,
            color = colorFor(MuscleHighlightGroup.LEGS),
            glow = isGlowing(MuscleHighlightGroup.LEGS),
            offsetX = -8f, offsetY = -BiomechanicalKinematicsSolver.L_SHIN, offsetZ = -8f
        )

        // Foot
        val footMatrix = shinMatrix *
                Matrix4x4.translation(0f, -BiomechanicalKinematicsSolver.L_SHIN, 0f) *
                Matrix4x4.rotationZ(-pose.foot)
        builder.addBox(
            width = 32f, height = BiomechanicalKinematicsSolver.L_FOOT, depth = 16f,
            matrix = footMatrix,
            color = COLOR_LIGHT_ZINC,
            offsetX = -8f, offsetY = 0f, offsetZ = -8f
        )
    }

    // Torso
    val torsoMatrix = pelvisMatrix * Matrix4x4.rotationZ(180f + pose.torso)
    builder.addBox(
        width = 30f, height = BiomechanicalKinematicsSolver.L_TORSO, depth = 38f,
        matrix = torsoMatrix,
        color = colorFor(MuscleHighlightGroup.TORSO),
        glow = isGlowing(MuscleHighlightGroup.TORSO),
        offsetX = -15f, offsetY = 0f, offsetZ = -19f
    )

    // Shoulders / Head / Neck
    val shoulderBarMatrix = torsoMatrix * Matrix4x4.translation(0f, BiomechanicalKinematicsSolver.L_TORSO, 0f)
    builder.addBox(34f, 9f, 62f, shoulderBarMatrix, COLOR_JOINT, offsetX = -17f, offsetY = -3f, offsetZ = -31f)
    builder.addBox(22f, 8f, 14f, shoulderBarMatrix, COLOR_LIGHT_ZINC, offsetX = -11f, offsetY = -2f, offsetZ = -7f)
    builder.addBox(26f, 26f, 26f, shoulderBarMatrix, COLOR_LIGHT_ZINC, offsetX = -13f, offsetY = 8f, offsetZ = -13f)

    // Left and Right Arms
    for (side in listOf(-1f, 1f)) {
        val zArmOffset = side * 30f

        // Upper Arm
        val upperArmMatrix = shoulderBarMatrix *
                Matrix4x4.translation(0f, 0f, zArmOffset) *
                Matrix4x4.rotationX(pose.abduction * side) *
                Matrix4x4.rotationZ(-pose.shoulder - 180f - pose.torso)

        builder.addBox(
            width = 15f, height = BiomechanicalKinematicsSolver.L_UA, depth = 15f,
            matrix = upperArmMatrix,
            color = colorFor(MuscleHighlightGroup.ARMS),
            glow = isGlowing(MuscleHighlightGroup.ARMS),
            offsetX = -7.5f, offsetY = 0f, offsetZ = -7.5f
        )

        // Elbow Joint
        val elbowMatrix = upperArmMatrix * Matrix4x4.translation(0f, BiomechanicalKinematicsSolver.L_UA, 0f)
        builder.addBox(13f, 13f, 13f, elbowMatrix, COLOR_JOINT, offsetX = -6.5f, offsetY = -7f, offsetZ = -6.5f)

        // Forearm
        val forearmMatrix = elbowMatrix * Matrix4x4.rotationZ(-pose.elbow)
        builder.addBox(
            width = 13f, height = BiomechanicalKinematicsSolver.L_FA, depth = 13f,
            matrix = forearmMatrix,
            color = colorFor(MuscleHighlightGroup.ARMS),
            glow = isGlowing(MuscleHighlightGroup.ARMS),
            offsetX = -6.5f, offsetY = 0f, offsetZ = -6.5f
        )

        // Hand & Apparatus
        val handMatrix = forearmMatrix * Matrix4x4.translation(0f, BiomechanicalKinematicsSolver.L_FA, 0f)
        builder.addBox(11f, 11f, 11f, handMatrix, COLOR_LIGHT_ZINC, offsetX = -5.5f, offsetY = -3f, offsetZ = -5.5f)

        // Apparatus Rendering
        when (move.apparatus) {
            ApparatusType.BARBELL -> {
                if (side > 0) { // Single bar across both hands
                    val barMatrix = handMatrix * Matrix4x4.translation(0f, 0f, -zArmOffset)
                    builder.addBox(6f, 6f, 190f, barMatrix, COLOR_LIGHT_ZINC, offsetX = -3f, offsetY = -3f, offsetZ = -95f)

                    // Plates on both sides
                    listOf(-1f, 1f).forEach { pSide ->
                        val pZ = pSide * 65f
                        val plateMatrix1 = barMatrix * Matrix4x4.translation(0f, 0f, pZ)
                        builder.addBox(44f, 44f, 6f, plateMatrix1, COLOR_VOLT, offsetX = -22f, offsetY = -22f, offsetZ = -3f)
                        val plateMatrix2 = barMatrix * Matrix4x4.translation(0f, 0f, pZ + pSide * 8f)
                        builder.addBox(34f, 34f, 5f, plateMatrix2, COLOR_EMBER, offsetX = -17f, offsetY = -17f, offsetZ = -2.5f)
                    }
                }
            }
            ApparatusType.DUMBBELL -> {
                builder.addBox(34f, 7f, 7f, handMatrix, COLOR_LIGHT_ZINC, offsetX = -17f, offsetY = -3.5f, offsetZ = -3.5f)
                listOf(-1f, 1f).forEach { dSide ->
                    val plateMatrix = handMatrix * Matrix4x4.translation(dSide * 15f, 0f, 0f)
                    builder.addBox(6f, 26f, 26f, plateMatrix, COLOR_EMBER, offsetX = -3f, offsetY = -13f, offsetZ = -13f)
                }
            }
            ApparatusType.BAR, ApparatusType.NONE -> {
                // No additional hand tool
            }
        }
    }

    // Directional light vector (top-left-front)
    val lightDir = Point3D(-0.4f, 0.7f, 0.6f)
    val lightLen = sqrt(lightDir.x * lightDir.x + lightDir.y * lightDir.y + lightDir.z * lightDir.z)
    val normLight = Point3D(lightDir.x / lightLen, lightDir.y / lightLen, lightDir.z / lightLen)

    // Depth Sorting (Painter's Algorithm): Furthest (lowest Z) to nearest (highest Z)
    val sortedFaces = builder.faces.sortedBy { it.averageDepth }

    // Perspective projection parameters
    val fov = 650.0f
    val cameraDistance = 450.0f
    val centerX = canvasWidth / 2.0f
    val centerY = canvasHeight * 0.75f // origin anchored towards bottom

    fun project(p: Point3D): Offset {
        val depth = p.z + cameraDistance
        val scale = if (depth > 10.0f) fov / depth else fov / 10.0f
        val sx = centerX + p.x * scale
        val sy = centerY - p.y * scale
        return Offset(sx, sy)
    }

    // Render sorted shaded polygons
    for (face in sortedFaces) {
        val pt0 = project(face.v0)
        val pt1 = project(face.v1)
        val pt2 = project(face.v2)
        val pt3 = project(face.v3)

        val shadedColor = face.computeShadedColor(normLight)

        val path = Path().apply {
            moveTo(pt0.x, pt0.y)
            lineTo(pt1.x, pt1.y)
            lineTo(pt2.x, pt2.y)
            lineTo(pt3.x, pt3.y)
            close()
        }

        drawPath(path = path, color = shadedColor)
    }
}
