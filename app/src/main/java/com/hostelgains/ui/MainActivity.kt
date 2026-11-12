package com.hostelgains.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.data.notifications.MealReminderScheduler
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.theme.HostelGainsTheme
import com.hostelgains.ui.viewmodel.ApiKeyViewModel

class MainActivity : ComponentActivity() {
    private lateinit var apiKeyViewModel: ApiKeyViewModel
    private var selectedTab by mutableStateOf(NavigationTab.MEALS)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission callback - reminders remain scheduled via AlarmManager regardless
        }

    private val requestActivityRecognitionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Activity recognition callback for hardware step counter
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Arm all 5 daily hostel meal & workout reminders
        MealReminderScheduler.scheduleAllReminders(applicationContext)

        // 2. Request runtime POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 3. Request runtime ACTIVITY_RECOGNITION permission on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestActivityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // 4. Build Room database and repositories with application Context (never leaks Activity)
        val apiKeyRepository = ApiKeyRepository(applicationContext)
        apiKeyViewModel = ApiKeyViewModel(apiKeyRepository)

        val db = com.hostelgains.data.database.AppDatabase.getInstance(applicationContext)
        val mealRepository = com.hostelgains.data.nutrition.DefaultMealRepository(dao = db.mealDao())
        com.hostelgains.ui.viewmodel.AppRepositories.init(mealRepository, apiKeyRepository)

        val mealLogViewModel = com.hostelgains.ui.viewmodel.MealLogViewModel(
            mealRepository = mealRepository,
            nutritionParser = com.hostelgains.domain.nutrition.GeminiNutritionParser(
                apiKeyProvider = { apiKeyRepository.getApiKey() }
            )
        )

        // 5. Hardware Step Tracker with 15-minute periodic WorkManager sync
        val stepRepository = com.hostelgains.data.steps.StepRepository(applicationContext)
        val sensorSource = com.hostelgains.data.steps.StepSensorSource(applicationContext)
        com.hostelgains.data.steps.StepSyncWorker.schedule(applicationContext)
        val stepTrackerViewModel = com.hostelgains.ui.viewmodel.StepTrackerViewModel(
            stepRepository = stepRepository,
            sensorSource = sensorSource
        )

        val profileRepository = com.hostelgains.data.profile.UserProfileRepository(applicationContext)
        val physiqueProfileViewModel = com.hostelgains.ui.viewmodel.PhysiqueProfileViewModel(
            profileRepository = profileRepository,
            apiKeyRepository = apiKeyRepository
        )
        val workoutViewModel = com.hostelgains.ui.viewmodel.WorkoutViewModel(
            dynamicWorkoutRepository = com.hostelgains.domain.workout.DynamicWorkoutRepository(),
            profileRepository = profileRepository,
            workoutRebalancer = com.hostelgains.domain.rebalance.GeminiWorkoutRebalancer(
                apiKeyProvider = { apiKeyRepository.getApiKey() }
            )
        )

        // 6. Handle incoming notification target tab
        parseTargetTab(intent)

        setContent {
            HostelGainsTheme {
                HostelGainsApp(
                    apiKeyViewModel = apiKeyViewModel,
                    physiqueProfileViewModel = physiqueProfileViewModel,
                    workoutViewModel = workoutViewModel,
                    mealLogViewModel = mealLogViewModel,
                    stepTrackerViewModel = stepTrackerViewModel,
                    initialTab = selectedTab
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseTargetTab(intent)
    }

    private fun parseTargetTab(intent: Intent?) {
        val targetTabName = intent?.getStringExtra("target_tab")
        if (targetTabName != null) {
            selectedTab = when (targetTabName) {
                "WORKOUT" -> NavigationTab.WORKOUT
                "VIEWER_3D" -> NavigationTab.VIEWER_3D
                "STEPS" -> NavigationTab.STEPS
                else -> NavigationTab.MEALS
            }
        }
    }
}
