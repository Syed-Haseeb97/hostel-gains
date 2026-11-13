package com.hostelgains.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import com.hostelgains.ui.components.ApiKeyReminderBanner
import com.hostelgains.ui.components.HostelBottomNavigation
import com.hostelgains.ui.components.HostelGainsTopBar
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.components.UpdateBanner
import com.hostelgains.ui.components.UpdateReadyDialog
import com.hostelgains.ui.screens.ApiKeyOnboardingScreen
import com.hostelgains.ui.screens.MealInputScreen
import com.hostelgains.ui.screens.StepStatusScreen
import com.hostelgains.ui.screens.WorkoutPlanScreen
import com.hostelgains.ui.screens.WorkoutViewerScreen
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.viewmodel.ApiKeyViewModel
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import com.hostelgains.ui.viewmodel.UpdateViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import com.hostelgains.ui.screens.PhysiqueSelectionScreen
import com.hostelgains.ui.viewmodel.PhysiqueProfileViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel

@Composable
fun HostelGainsApp(
    apiKeyViewModel: ApiKeyViewModel,
    physiqueProfileViewModel: PhysiqueProfileViewModel? = null,
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
    val profileUiState = physiqueProfileViewModel?.uiState?.collectAsState()?.value

    // ── Check in real time on every launch for latest available version ──────
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdatesOnLaunch(context.applicationContext)
    }

    // ── 1. API Key Onboarding gate ───────────────────────────────────────────
    if (apiKeyUiState.showOnboarding) {
        ApiKeyOnboardingScreen(
            viewModel = apiKeyViewModel,
            onDismiss = { /* state drives navigation */ }
        )
        return
    }

    // ── 2. Physique Build & Anthropometry Onboarding gate ────────────────────
    if (physiqueProfileViewModel != null && profileUiState != null && !profileUiState.isSaved) {
        PhysiqueSelectionScreen(
            viewModel = physiqueProfileViewModel,
            onConfirmed = { selectedBuild, selectedLevel ->
                workoutViewModel.selectPhysiqueBuildAndLevel(selectedBuild, selectedLevel)
            }
        )
        return
    }

    val liveKey: String? = apiKeyViewModel.getStoredKey()
    var currentTab by remember { mutableStateOf(initialTab) }

    // If initialTab changes (e.g. from push notification navigation intent), switch tab
    LaunchedEffect(initialTab) {
        currentTab = initialTab
    }

    // System back returns to the first tab before leaving the app.
    BackHandler(enabled = currentTab != NavigationTab.MEALS) {
        currentTab = NavigationTab.MEALS
    }

    Scaffold(
        containerColor = Canvas0,
        // Each screen handles the status bar itself so content can scroll edge-to-edge.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
            // Top brand bar with Sir Whiskers Gym Cat & Gemini AI status
            HostelGainsTopBar(
                statusMessage = apiKeyUiState.statusMessage ?: if (liveKey != null) "🟢 Gemini AI Connected" else "⚠️ AI Key Needed",
                isKeyValid = apiKeyUiState.isKeyValid || liveKey != null,
                onAiBadgeClick = { apiKeyViewModel.showOnboardingAgain() }
            )

            // Live Gemini API Key Reminder Banner when key is missing or invalid
            if (apiKeyUiState.showApiKeyReminder || liveKey == null) {
                ApiKeyReminderBanner(
                    onEnterKey = { apiKeyViewModel.showOnboardingAgain() },
                    onDismiss = { apiKeyViewModel.dismissApiKeyReminder() }
                )
            }

            // Live background download progress banner
            UpdateBanner(
                uiState = updateUiState,
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = currentTab, animationSpec = tween(220), label = "tab") { tab ->
                when (tab) {
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
                            profileViewModel = physiqueProfileViewModel,
                            isPremiumAi = liveKey != null,
                            onUnlockAiClicked = { apiKeyViewModel.showOnboardingAgain() },
                            onNavigateToViewer = { exerciseId ->
                                workoutViewerViewModel.selectExercise(exerciseId)
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
