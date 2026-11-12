package com.hostelgains.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hostelgains.domain.nutrition.GeminiNutritionParser
import com.hostelgains.domain.rebalance.GeminiWorkoutRebalancer
import com.hostelgains.ui.components.HostelBottomNavigation
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.components.UpdateBanner
import com.hostelgains.ui.components.UpdateReadyDialog
import com.hostelgains.ui.screens.ApiKeyOnboardingScreen
import com.hostelgains.ui.screens.MealInputScreen
import com.hostelgains.ui.screens.StepStatusScreen
import com.hostelgains.ui.screens.WorkoutPlanScreen
import com.hostelgains.ui.screens.WorkoutViewerScreen
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.viewmodel.ApiKeyViewModel
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import com.hostelgains.ui.viewmodel.UpdateViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel

@Composable
fun HostelGainsApp(
    apiKeyViewModel: ApiKeyViewModel,
    initialTab: NavigationTab = NavigationTab.MEALS,
    updateViewModel: UpdateViewModel = viewModel(),
    mealLogViewModel: MealLogViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    stepTrackerViewModel: StepTrackerViewModel = viewModel(),
    workoutViewerViewModel: WorkoutViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val apiKeyUiState by apiKeyViewModel.uiState.collectAsState()
    val updateUiState by updateViewModel.uiState.collectAsState()

    // ── Check in real time on every launch for latest available version ──────
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdatesOnLaunch(context.applicationContext)
    }

    // ── Onboarding gate ──────────────────────────────────────────────────────
    if (apiKeyUiState.showOnboarding) {
        ApiKeyOnboardingScreen(
            viewModel = apiKeyViewModel,
            onDismiss = { /* state drives navigation */ }
        )
        return
    }

    val liveKey: String? = apiKeyViewModel.getStoredKey()
    var currentTab by remember { mutableStateOf(initialTab) }

    // If initialTab changes (e.g. from push notification navigation intent), switch tab
    LaunchedEffect(initialTab) {
        currentTab = initialTab
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            HostelBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live background download progress banner
            UpdateBanner(
                uiState = updateUiState,
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.weight(1f)) {
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

        // 1-Tap Ready-to-Install Prompt when background APK download finishes
        if (updateUiState.showInstallDialog) {
            UpdateReadyDialog(
                uiState = updateUiState,
                onInstallClicked = {
                    updateViewModel.installDownloadedUpdate(context)
                },
                onDismiss = {
                    updateViewModel.dismissInstallDialog()
                }
            )
        }
    }
}
