package com.hostelgains.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hostelgains.ui.three.Exercise3DView

/**
 * 3D point in model coordinates (retained for backward compatibility).
 */
data class Point3D(val x: Float, val y: Float, val z: Float)

/**
 * 3D Line segment representing a limb or apparatus rod (retained for backward compatibility).
 */
data class LimbSegment(
    val p1: Point3D,
    val p2: Point3D,
    val strokeWidth: Float,
    val color: Color,
    val isMuscleHighlight: Boolean = false
)

/**
 * Real-time 3D exercise demonstration delegating to [Exercise3DView] and the articulated
 * `Male-low.OBJ` mesh with automatic mobile LOD and muscle-group highlighting.
 */
@Composable
fun Mannequin3DView(
    exerciseName: String,
    targetMuscles: List<String>,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    speed: Float = 1.0f,
    yawDeg: Float = 35f,
    pitchDeg: Float = 14f,
    autoOrbit: Boolean = false,
    onYawChanged: ((Float) -> Unit)? = null
) {
    Exercise3DView(
        exerciseKey = exerciseName,
        targetMuscles = targetMuscles,
        modifier = modifier,
        isPlaying = isPlaying,
        speed = speed,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        autoOrbit = autoOrbit,
        interactive = true,
        onCameraChange = { newYaw, _ -> onYawChanged?.invoke(newYaw) }
    )
}
