package com.hostelgains.data.profile

import android.content.Context
import android.content.SharedPreferences
import com.hostelgains.domain.physique.ExperienceLevel
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.domain.physique.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent storage of the user's physical profile, target build pathway,
 * and AI-calibrated recommendations in SharedPreferences.
 */
class UserProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val heightCm = prefs.getFloat(KEY_HEIGHT_CM, 178f)
        val weightKg = prefs.getFloat(KEY_WEIGHT_KG, 87f)
        val ageYears = prefs.getInt(KEY_AGE, 21)
        val buildId = prefs.getString(KEY_TARGET_BUILD, PhysiqueBuild.V_TAPER.id)
        val levelId = prefs.getString(KEY_EXPERIENCE_LEVEL, ExperienceLevel.INTERMEDIATE.id)
        val aiPlan = prefs.getString(KEY_AI_PLAN, null)
        val calorieTarget = if (prefs.contains(KEY_CALORIES)) prefs.getInt(KEY_CALORIES, 0) else null
        val proteinTarget = if (prefs.contains(KEY_PROTEIN)) prefs.getInt(KEY_PROTEIN, 0) else null
        val notes = prefs.getString(KEY_NOTES, null)
        val isConfigured = prefs.getBoolean(KEY_CONFIGURED, false)

        return UserProfile(
            heightCm = heightCm,
            weightKg = weightKg,
            ageYears = ageYears,
            targetBuild = PhysiqueBuild.fromId(buildId),
            experienceLevel = ExperienceLevel.fromId(levelId),
            aiCalibratedPlan = aiPlan,
            customCalorieTarget = calorieTarget,
            customProteinTarget = proteinTarget,
            customNotes = notes,
            isConfigured = isConfigured
        )
    }

    fun saveProfile(profile: UserProfile) {
        prefs.edit().apply {
            putFloat(KEY_HEIGHT_CM, profile.heightCm)
            putFloat(KEY_WEIGHT_KG, profile.weightKg)
            putInt(KEY_AGE, profile.ageYears)
            putString(KEY_TARGET_BUILD, profile.targetBuild.id)
            putString(KEY_EXPERIENCE_LEVEL, profile.experienceLevel.id)
            putString(KEY_AI_PLAN, profile.aiCalibratedPlan)
            if (profile.customCalorieTarget != null) {
                putInt(KEY_CALORIES, profile.customCalorieTarget)
            } else {
                remove(KEY_CALORIES)
            }
            if (profile.customProteinTarget != null) {
                putInt(KEY_PROTEIN, profile.customProteinTarget)
            } else {
                remove(KEY_PROTEIN)
            }
            putString(KEY_NOTES, profile.customNotes)
            putBoolean(KEY_CONFIGURED, true)
            apply()
        }
        _userProfile.value = profile.copy(isConfigured = true)
    }

    fun updateTargetBuild(build: PhysiqueBuild) {
        val current = _userProfile.value
        val updated = current.copy(targetBuild = build, isConfigured = true)
        saveProfile(updated)
    }

    fun updateExperienceLevel(level: ExperienceLevel) {
        val current = _userProfile.value
        val updated = current.copy(experienceLevel = level, isConfigured = true)
        saveProfile(updated)
    }

    fun saveAiWorkoutRoutineJson(json: String) {
        prefs.edit().putString(KEY_AI_WORKOUT_ROUTINE, json).apply()
    }

    fun loadAiWorkoutRoutineJson(): String? {
        return prefs.getString(KEY_AI_WORKOUT_ROUTINE, null)
    }

    fun clearAiWorkoutRoutine() {
        prefs.edit().remove(KEY_AI_WORKOUT_ROUTINE).apply()
    }

    companion object {
        private const val PREFS_NAME = "hostel_gains_user_profile"
        private const val KEY_HEIGHT_CM = "key_height_cm"
        private const val KEY_WEIGHT_KG = "key_weight_kg"
        private const val KEY_AGE = "key_age"
        private const val KEY_TARGET_BUILD = "key_target_build"
        private const val KEY_EXPERIENCE_LEVEL = "key_experience_level"
        private const val KEY_AI_PLAN = "key_ai_plan"
        private const val KEY_AI_WORKOUT_ROUTINE = "key_ai_workout_routine"
        private const val KEY_CALORIES = "key_calories"
        private const val KEY_PROTEIN = "key_protein"
        private const val KEY_NOTES = "key_notes"
        private const val KEY_CONFIGURED = "key_configured"
    }
}
