package com.hostelgains.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hostelgains.data.ApiKeyRepository
import com.hostelgains.data.profile.UserProfileRepository
import com.hostelgains.domain.physique.GeminiPhysiqueOptimizer
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.domain.physique.PhysiqueOptimizationResult
import com.hostelgains.domain.physique.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PhysiqueProfileUiState(
    val heightCm: Float = 178f,
    val weightKg: Float = 87f,
    val ageYears: Int = 21,
    val selectedBuild: PhysiqueBuild = PhysiqueBuild.V_TAPER,
    val isOptimizing: Boolean = false,
    val optimizationResult: PhysiqueOptimizationResult? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    val currentProfile: UserProfile
        get() = UserProfile(
            heightCm = heightCm,
            weightKg = weightKg,
            ageYears = ageYears,
            targetBuild = selectedBuild,
            customCalorieTarget = optimizationResult?.dailyCalories,
            customProteinTarget = optimizationResult?.proteinGrams,
            customNotes = optimizationResult?.fullSummary
        )
}

class PhysiqueProfileViewModel(
    private val profileRepository: UserProfileRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val optimizer: GeminiPhysiqueOptimizer = GeminiPhysiqueOptimizer(apiKeyRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<PhysiqueProfileUiState> = _uiState.asStateFlow()

    private fun buildInitialState(): PhysiqueProfileUiState {
        val current = profileRepository.userProfile.value
        return PhysiqueProfileUiState(
            heightCm = current.heightCm,
            weightKg = current.weightKg,
            ageYears = current.ageYears,
            selectedBuild = current.targetBuild,
            isSaved = current.isConfigured
        )
    }

    fun updateHeight(heightCm: Float) {
        _uiState.value = _uiState.value.copy(heightCm = heightCm)
    }

    fun updateWeight(weightKg: Float) {
        _uiState.value = _uiState.value.copy(weightKg = weightKg)
    }

    fun updateAge(ageYears: Int) {
        _uiState.value = _uiState.value.copy(ageYears = ageYears)
    }

    fun selectBuild(build: PhysiqueBuild) {
        _uiState.value = _uiState.value.copy(selectedBuild = build)
    }

    fun runGeminiOptimization() {
        val current = _uiState.value.currentProfile
        _uiState.value = _uiState.value.copy(isOptimizing = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val result = optimizer.optimizePlan(current)
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    optimizationResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    errorMessage = e.message ?: "Failed to optimize plan"
                )
            }
        }
    }

    fun saveProfile() {
        val profile = _uiState.value.currentProfile.copy(
            aiCalibratedPlan = _uiState.value.optimizationResult?.fullSummary,
            customCalorieTarget = _uiState.value.optimizationResult?.dailyCalories,
            customProteinTarget = _uiState.value.optimizationResult?.proteinGrams,
            customNotes = _uiState.value.optimizationResult?.leverageAdvice,
            isConfigured = true
        )
        profileRepository.saveProfile(profile)
        _uiState.value = _uiState.value.copy(isSaved = true)
    }
}
