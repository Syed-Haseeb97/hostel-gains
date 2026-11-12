package com.hostelgains.ui.three

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hostelgains.ui.theme.Clay
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.LocalHgColors
import com.hostelgains.ui.theme.StageCable
import com.hostelgains.ui.theme.StageGrid
import com.hostelgains.ui.theme.StageBottom
import com.hostelgains.ui.theme.StageTop
import kotlin.math.min

/** Animation + camera state shared between the 3D canvas and any HUD overlays. */
@Stable
class Exercise3DState {
    /** Motion time in seconds (already scaled by playback speed). */
    var time by mutableFloatStateOf(0f)

    /** Extra yaw accumulated by auto-orbit since the last manual camera change. */
    var orbit by mutableFloatStateOf(0f)

    var zoom by mutableFloatStateOf(1f)
}

@Composable
fun rememberExercise3DState(): Exercise3DState = remember { Exercise3DState() }

private const val CAMERA_DISTANCE = 720f

/**
 * Real-time 3D exercise demonstration rendered with Compose Canvas.
 *
 * @param exerciseKey exercise id or name; resolved to a [MotionKind].
 * @param targetMuscles free-text muscle names that glow on the mannequin.
 * @param interactive enables drag-to-orbit, pinch zoom and double-tap reset.
 */
@Composable
fun Exercise3DView(
    exerciseKey: String,
    targetMuscles: List<String>,
    modifier: Modifier = Modifier,
    state: Exercise3DState = rememberExercise3DState(),
    isPlaying: Boolean = true,
    speed: Float = 1f,
    yawDeg: Float = 35f,
    pitchDeg: Float = 14f,
    autoOrbit: Boolean = false,
    interactive: Boolean = true,
    showGrid: Boolean = true,
    /** Fraction of the height kept clear at the top/bottom for overlays (HUD, hints). */
    hudInset: Float = 0f,
    onCameraChange: ((yaw: Float, pitch: Float) -> Unit)? = null
) {
    val kind = remember(exerciseKey) { motionFor(exerciseKey) }
    val muscles = remember(targetMuscles) { musclesFrom(targetMuscles) }
    // Theme colours are read here (composable scope) and captured by the draw lambda.
    val accent = Ignite
    val stageTop = StageTop
    val stageBottom = StageBottom
    val grid = StageGrid
    val skin = Clay
    val cable = StageCable
    val isDark = LocalHgColors.current.isDark
    val style = remember(accent, stageBottom, grid, isDark) {
        StageStyle(
            accent = accent,
            background = stageBottom,
            gridColor = grid,
            shadowColor = Color.Black.copy(alpha = if (isDark) 0.38f else 0.16f),
            poolColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.6f)
        )
    }

    // Restart the rep cleanly when the exercise changes.
    LaunchedEffect(kind) { state.time = 0f }

    // Frame clock: integrates time so speed changes never cause the motion to jump.
    LaunchedEffect(isPlaying, speed, autoOrbit) {
        if (!isPlaying && !autoOrbit) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    if (isPlaying) state.time += dt * speed
                    if (autoOrbit) state.orbit += dt * 24f
                }
                last = now
            }
        }
    }

    val yaw by rememberUpdatedState(yawDeg)
    val pitch by rememberUpdatedState(pitchDeg)
    val cameraCallback by rememberUpdatedState(onCameraChange)

    val gestures = if (interactive) {
        Modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) state.zoom = (state.zoom * zoom).coerceIn(0.7f, 2.4f)
                    if (pan != Offset.Zero) {
                        val newYaw = yaw + state.orbit + pan.x * 0.35f
                        val newPitch = (pitch + pan.y * 0.22f).coerceIn(2f, 60f)
                        state.orbit = 0f
                        cameraCallback?.invoke(newYaw, newPitch)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { state.zoom = 1f })
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(stageTop, stageBottom)))
            .then(gestures)
            .semantics { contentDescription = "3D demonstration of $exerciseKey" }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (size.width < 2f || size.height < 2f) return@Canvas
            val frame = buildMotionFrame(kind, state.time, muscles, skin, cable)
            val focal = min(
                0.84f * (1f - 2f * hudInset) * size.height * CAMERA_DISTANCE / (2f * kind.halfHeight),
                0.9f * size.width * CAMERA_DISTANCE / (2f * kind.radius)
            ) * state.zoom
            val cam = OrbitCamera(
                yawDeg = yaw + state.orbit,
                pitchDeg = pitch.coerceIn(2f, 60f),
                target = kind.center,
                distance = CAMERA_DISTANCE,
                focal = focal,
                cx = size.width / 2f,
                cy = size.height * 0.52f
            )
            if (showGrid) {
                drawFloor(
                    cam = cam,
                    style = style,
                    halfExtent = 150f,
                    step = 25f,
                    scrollZ = frame.floorScroll,
                    shadowCenter = frame.shadowCenter,
                    shadowRadiusX = frame.shadowRx,
                    shadowRadiusZ = frame.shadowRz
                )
            }
            drawScene(frame.supports, cam, style, frame.focus)
            drawScene(frame.prims, cam, style, frame.focus)
        }
    }
}
