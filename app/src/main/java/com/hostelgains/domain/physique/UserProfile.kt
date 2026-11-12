package com.hostelgains.domain.physique

import kotlin.math.roundToInt

/**
 * Physical profile and target physique configuration for a hostel student.
 */
data class UserProfile(
    val heightCm: Float = 178f, // Default 5'10"
    val weightKg: Float = 87f,  // Default 87 kg
    val ageYears: Int = 21,     // Default college age
    val targetBuild: PhysiqueBuild = PhysiqueBuild.V_TAPER,
    val aiCalibratedPlan: String? = null,
    val customCalorieTarget: Int? = null,
    val customProteinTarget: Int? = null,
    val customNotes: String? = null,
    val isConfigured: Boolean = false
) {
    val bmi: Float
        get() {
            val heightM = heightCm / 100f
            return if (heightM > 0) (weightKg / (heightM * heightM) * 10f).roundToInt() / 10f else 0f
        }

    val bmiCategory: String
        get() = when {
            bmi < 18.5f -> "Lean / Ectomorph"
            bmi < 24.9f -> "Athletic Normal"
            bmi < 29.9f -> "Solid Muscular / Heavy Frame"
            else -> "Heavy Bulk / Power Lifter"
        }

    val heightImperialFormatted: String
        get() {
            val totalInches = (heightCm / 2.54f).roundToInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            return "$feet'$inches\""
        }

    val heightFormatted: String
        get() = "$heightImperialFormatted (${heightCm.roundToInt()} cm)"

    val weightFormatted: String
        get() {
            val lbs = (weightKg * 2.20462f).roundToInt()
            return "${weightKg.roundToInt()} kg ($lbs lbs)"
        }

    /**
     * Estimated baseline BMR (Mifflin-St Jeor formula for young adult male)
     */
    val estimatedBmr: Int
        get() = (10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5).roundToInt()

    /**
     * Recommended baseline daily protein in grams based on target build and bodyweight.
     */
    val recommendedProteinGrams: Int
        get() {
            val multiplier = when (targetBuild) {
                PhysiqueBuild.V_TAPER -> 2.0f
                PhysiqueBuild.TANK -> 1.8f
                PhysiqueBuild.ATHLETE -> 1.7f
                PhysiqueBuild.X_FRAME -> 2.0f
                PhysiqueBuild.YOKE -> 1.8f
                PhysiqueBuild.GOLDEN_ERA -> 2.2f
                PhysiqueBuild.SLEEPER -> 2.1f
            }
            return (weightKg * multiplier).roundToInt()
        }

    /**
     * Recommended baseline maintenance/target calories based on target build.
     */
    val recommendedCalories: Int
        get() {
            val activityMultiplier = 1.45f // moderate training + walking around university campus
            val maintenance = (estimatedBmr * activityMultiplier).roundToInt()
            return when (targetBuild) {
                PhysiqueBuild.V_TAPER -> maintenance - 200 // Slight deficit for tight waist
                PhysiqueBuild.TANK -> maintenance + 350    // Surplus for dense power
                PhysiqueBuild.ATHLETE -> maintenance       // Recomp / high conditioning
                PhysiqueBuild.X_FRAME -> maintenance + 150  // Slight surplus for quad growth
                PhysiqueBuild.YOKE -> maintenance + 400     // Bear mode surplus
                PhysiqueBuild.GOLDEN_ERA -> maintenance + 100 // Lean bulk
                PhysiqueBuild.SLEEPER -> maintenance - 300  // Caloric deficit for low body fat
            }
        }
}
