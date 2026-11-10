package com.hostelgains.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 4 Core navigation tabs for The Hostel-Gains Dynamic Optimizer.
 */
enum class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    MEALS("Mess Meals", Icons.Default.Restaurant, "meals"),
    WORKOUT("V-Taper Plan", Icons.Default.FitnessCenter, "workout"),
    VIEWER_3D("3D Viewer", Icons.Default.ViewInAr, "viewer_3d"),
    STEPS("Step Tracker", Icons.AutoMirrored.Filled.DirectionsWalk, "steps")
}
