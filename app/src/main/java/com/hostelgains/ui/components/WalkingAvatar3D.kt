package com.hostelgains.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hostelgains.ui.three.Exercise3DView

/**
 * Animated 3D walking avatar powered by the articulated `Male-low.OBJ` anatomical mesh.
 */
@Composable
fun WalkingAvatar3D(
    modifier: Modifier = Modifier,
    isWalking: Boolean = true,
    cadenceSpeed: Float = 1.0f
) {
    Exercise3DView(
        exerciseKey = "d7_active_recovery_walk",
        targetMuscles = listOf("Calves", "Glutes", "Core"),
        modifier = modifier,
        isPlaying = isWalking,
        speed = cadenceSpeed,
        yawDeg = 55f,
        pitchDeg = 12f,
        interactive = false,
        showGrid = true
    )
}
