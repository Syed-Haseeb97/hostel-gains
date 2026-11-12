package com.hostelgains.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.ui.graphics.vector.ImageVector

/** The four primary destinations. Enum names are kept stable for notification deep links. */
enum class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    MEALS("Fuel", Icons.Filled.Restaurant, "meals"),
    WORKOUT("Train", Icons.Filled.FitnessCenter, "workout"),
    VIEWER_3D("Form 3D", Icons.Filled._3dRotation, "viewer_3d"),
    STEPS("Move", Icons.AutoMirrored.Filled.DirectionsWalk, "steps")
}
