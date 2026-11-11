package com.hostelgains.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import com.hostelgains.domain.rebalance.GeminiWorkoutRebalancer
import com.hostelgains.ui.components.HostelBottomNavigation
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.screens.ApiKeyOnboardingScreen
import com.hostelgains.ui.screens.MealInputScreen
import com.hostelgains.ui.screens.StepStatusScreen
import com.hostelgains.ui.screens.WorkoutPlanScreen
import com.hostelgains.ui.screens.WorkoutViewerScreen
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.viewmodel.ApiKeyViewModel
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel

@Composable
fun HostelGainsApp(
    apiKeyViewModel: ApiKeyViewModel,
    mealLogViewModel: MealLogViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    stepTrackerViewModel: StepTrackerViewModel = viewModel(),
    workoutViewerViewModel: WorkoutViewerViewModel = viewModel()
) {
    val apiKeyUiState by apiKeyViewModel.uiState.collectAsState()

    // ── Onboarding gate ──────────────────────────────────────────────────────
    if (apiKeyUiState.showOnboarding) {
        ApiKeyOnboardingScreen(
            viewModel = apiKeyViewModel,
            onDismiss = { /* state drives navigation */ }
        )
        return
    }

    // ── Inject the runtime key into parsers ──────────────────────────────────
    // The key is read from EncryptedSharedPreferences each time the app
    // composable re-enters so it stays in sync without requiring a restart.
    val liveKey: String? = apiKeyViewModel.getStoredKey()

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
                    MealInputScreen(
                        viewModel = mealLogViewModel,
                        isPremiumAi = liveKey != null,
                        onUnlockAiClicked = { apiKeyViewModel.showOnboardingAgain() }
                    )
                }
                NavigationTab.WORKOUT -> {
                    WorkoutPlanScreen(
                        viewModel = workoutViewModel,
                        isPremiumAi = liveKey != null,
                        onUnlockAiClicked = { apiKeyViewModel.showOnboardingAgain() },
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
