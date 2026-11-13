package com.hostelgains.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.ui.graphics.vector.ImageVector

/** The four primary destinations matching the Figma design system. */
enum class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    TODAY("Today", Icons.Filled.Home, "today"),
    MEALS("Meal AI", Icons.AutoMirrored.Filled.Chat, "meals"),
    WORKOUT("Workout", Icons.Filled.FitnessCenter, "workout"),
    PROGRESS("Progress", Icons.Filled.BarChart, "progress");

    companion object {
        // Deep link backward-compatibility mappings
        val VIEWER_3D = WORKOUT
        val STEPS = PROGRESS
    }
}
