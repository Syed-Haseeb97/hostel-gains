package com.hostelgains.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hostelgains.ui.components.HostelBottomNavigation
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.screens.MealInputScreen
import com.hostelgains.ui.screens.StepStatusScreen
import com.hostelgains.ui.screens.WorkoutPlanScreen
import com.hostelgains.ui.screens.WorkoutViewerScreen
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel

@Composable
fun HostelGainsApp(
    mealLogViewModel: MealLogViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    stepTrackerViewModel: StepTrackerViewModel = viewModel(),
    workoutViewerViewModel: WorkoutViewerViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavigationTab.MEALS) }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            HostelBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.MEALS -> {
                    MealInputScreen(viewModel = mealLogViewModel)
                }
                NavigationTab.WORKOUT -> {
                    WorkoutPlanScreen(
                        viewModel = workoutViewModel,
                        onNavigateToViewer = { exerciseName ->
                            workoutViewerViewModel.selectExercise(exerciseName)
                            currentTab = NavigationTab.VIEWER_3D
                        }
                    )
                }
                NavigationTab.VIEWER_3D -> {
                    WorkoutViewerScreen(viewModel = workoutViewerViewModel)
                }
                NavigationTab.STEPS -> {
                    StepStatusScreen(viewModel = stepTrackerViewModel)
                }
            }
        }
    }
}
