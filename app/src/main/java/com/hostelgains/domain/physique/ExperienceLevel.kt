package com.hostelgains.domain.physique

/**
 * Lifting experience level for calibrating training volume, technical complexity,
 * rep ranges, and central nervous system (CNS) fatigue management.
 */
enum class ExperienceLevel(
    val id: String,
    val title: String,
    val subtitle: String,
    val repFocus: String,
    val restFocus: String,
    val description: String,
    val volumeMultiplier: Float
) {
    BEGINNER(
        id = "beginner",
        title = "Beginner",
        subtitle = "0 - 1 Years Lifting",
        repFocus = "Foundational Mechanics & Controlled Reps (10-15 reps)",
        restFocus = "90 - 120s recovery between sets",
        description = "Emphasizes joint stability, form mastery, linear progression, and manageable volume to build connective tissue and prevent injuries in the hostel gym.",
        volumeMultiplier = 0.85f
    ),
    INTERMEDIATE(
        id = "intermediate",
        title = "Intermediate",
        subtitle = "1 - 3 Years Consistent",
        repFocus = "Progressive Overload & Muscular Volume (8-12 reps)",
        restFocus = "60 - 90s recovery",
        description = "Optimized for progressive tension overload, higher volume tolerance, targeted muscle isolation, and accelerated hypertrophy toward your target build.",
        volumeMultiplier = 1.0f
    ),
    ADVANCED(
        id = "advanced",
        title = "Advanced / Pro",
        subtitle = "3+ Years Serious Training",
        repFocus = "High Intensity & Failure Threshold (4-8 heavy / 12-15 burnout)",
        restFocus = "45 - 75s metabolic density (120s on heavy compounds)",
        description = "High density, mechanical drop sets, dead-stop pauses, weak-point specialization, and maximum hypertrophy stimulus to break through plateaus.",
        volumeMultiplier = 1.2f
    );

    companion object {
        val ALL_LEVELS: List<ExperienceLevel> = listOf(BEGINNER, INTERMEDIATE, ADVANCED)

        fun fromId(id: String?): ExperienceLevel =
            ALL_LEVELS.find { it.id.equals(id, ignoreCase = true) } ?: INTERMEDIATE
    }
}
