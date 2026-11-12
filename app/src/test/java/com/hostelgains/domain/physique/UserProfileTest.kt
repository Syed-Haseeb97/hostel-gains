package com.hostelgains.domain.physique

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun calculateBmi_forHostelStudentStats() {
        // 5'10" (178 cm), 87 kg (from user prompt)
        val profile = UserProfile(heightCm = 178f, weightKg = 87f, ageYears = 21, targetBuild = PhysiqueBuild.TANK)
        assertEquals(27.5f, profile.bmi, 0.1f)
        assertTrue(profile.bmiCategory.contains("Muscular") || profile.bmiCategory.contains("Heavy"))
        assertEquals("5'10\"", profile.heightImperialFormatted)
        assertTrue(profile.weightFormatted.contains("87 kg"))
    }

    @Test
    fun proteinRecommendations_scaleAppropriatelyByTargetBuild() {
        val base = UserProfile(weightKg = 80f)
        val vTaperProfile = base.copy(targetBuild = PhysiqueBuild.V_TAPER)
        val tankProfile = base.copy(targetBuild = PhysiqueBuild.TANK)
        val athleteProfile = base.copy(targetBuild = PhysiqueBuild.ATHLETE)

        assertEquals(160, vTaperProfile.recommendedProteinGrams) // 2.0x
        assertEquals(144, tankProfile.recommendedProteinGrams)   // 1.8x
        assertEquals(136, athleteProfile.recommendedProteinGrams) // 1.7x
    }

    @Test
    fun offlineOptimizer_generatesAccurateTailoredCalibrations() {
        val profile = UserProfile(heightCm = 178f, weightKg = 87f, ageYears = 21, targetBuild = PhysiqueBuild.V_TAPER)
        val result = OfflinePhysiqueOptimizer.optimize(profile)

        assertTrue(result.dailyCalories > 2000)
        assertEquals(174, result.proteinGrams) // 87 * 2.0 = 174g
        assertTrue(result.leverageAdvice.contains("lat pulldowns", ignoreCase = true))
        assertTrue(result.hostelMessStrategy.contains("mess", ignoreCase = true) || result.hostelMessStrategy.contains("dal", ignoreCase = true))
    }
}
