package com.hostelgains.domain.physique

/**
 * Target physique build pathways supported by the Dynamic Workout Engine.
 */
enum class PhysiqueBuild(
    val id: String,
    val title: String,
    val archetypeTag: String,
    val repRangeSummary: String,
    val shortDescription: String,
    val fullBio: String,
    val visualBadge: String,
    val targetAesthetic: String,
    val primaryExerciseFocus: String,
    val realLifeVisualUrl: String
) {
    V_TAPER(
        id = "v_taper",
        title = "The V-Taper",
        archetypeTag = "Aesthetic Focus",
        repRangeSummary = "8-12 Hypertrophy",
        shortDescription = "Wide side delts & flaring lats tapering into a tight, narrow waist.",
        fullBio = "Prioritizes maximum lateral delt capping, sweeping lat flare, and upper-chest shelves to create the optical illusion of a razor-sharp small waist. Leg volume is maintained efficiently without blowing up waist circumference.",
        visualBadge = "⚡",
        targetAesthetic = "Wide lats, 3D side delts, tapered core",
        primaryExerciseFocus = "Wide-Grip Lat Pulldowns, DB Lateral Raises, Incline DB Press, Face Pulls",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=800&q=80"
    ),
    TANK(
        id = "tank",
        title = "The Tank",
        archetypeTag = "Raw Strength Focus",
        repRangeSummary = "4-6 Heavy Power",
        shortDescription = "Dense upper-back mass, mountain traps, heavy squats & brutal CNS power.",
        fullBio = "Mirroring a heavy strength colossus in an action RPG. Built around thick spinal erectors, bar-bending barbell rows, heavy squats, and overhead pressing in the 4-6 rep strength zone to maximize central nervous system adaptation and fill out oversized clothes immediately.",
        visualBadge = "🛡️",
        targetAesthetic = "Mountain traps, thick slabs of back, heavy legs",
        primaryExerciseFocus = "Barbell Deadlifts, Heavy Pendlay Rows, Military Press, Heavy Back Squats",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80"
    ),
    ATHLETE(
        id = "athlete",
        title = "The Athlete",
        archetypeTag = "Lean & Functional",
        repRangeSummary = "12-15 Stamina & Power",
        shortDescription = "Hybrid conditioning, explosive plyometrics, unilateral stability & mobility.",
        fullBio = "Balances functional hypertrophy with stamina, mobility, and cardiovascular endurance. Blends standard PPL mechanics with explosive plyometrics, single-leg Bulgarian split squats, and higher rep ranges (12-15) for relentless athleticism and 10-13% year-round body fat.",
        visualBadge = "🔥",
        targetAesthetic = "Sprinter physique, chiseled core, elastic stamina",
        primaryExerciseFocus = "Explosive Push-ups, Bulgarian Split Squats, Kettlebell Swings, Pull-ups",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800&q=80"
    ),
    X_FRAME(
        id = "x_frame",
        title = "The X-Frame",
        archetypeTag = "Balanced Head-to-Toe",
        repRangeSummary = "6-10 Heavy Hypertrophy",
        shortDescription = "Direct upgrade to V-Taper: balances upper width with sweeping outer quads and calves.",
        fullBio = "Turns the 'V' into a complete 'X'. Keeps wide shoulders, big lats, and a tight waist, but balances upper width with sweeping outer quads and solid calves so you never look top-heavy.",
        visualBadge = "⚔️",
        targetAesthetic = "Wide shoulders, narrow waist, sweeping quads",
        primaryExerciseFocus = "Heavy Barbell Squats, Leg Extensions, Lat Pulldowns, Standing Calf Raises",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=800&q=80"
    ),
    YOKE(
        id = "yoke",
        title = "The Yoke (Bear Mode)",
        archetypeTag = "Sheer Upper Density",
        repRangeSummary = "5-8 Heavy Mass",
        shortDescription = "Thick neck, heavy upper back, full chest and thick arms at 15-18% body fat.",
        fullBio = "Trades razor-sharp leanness for sheer upper-body density. Fills out t-shirts immediately without having to starve yourself on hostel mess food.",
        visualBadge = "🐻",
        targetAesthetic = "Heavy traps, thick arms, wide chest shelf",
        primaryExerciseFocus = "Heavy Shrugs, Farmer's Carries, T-Bar Rows, Incline Bench Press",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800&q=80"
    ),
    GOLDEN_ERA(
        id = "golden_era",
        title = "Golden Era (Classic)",
        archetypeTag = "1970s Aesthetics",
        repRangeSummary = "8-12 Classic Volume",
        shortDescription = "Proportions and roundness: shelf-like upper chest, big arms, and 3D side profile.",
        fullBio = "Alongside wide lats and a tight waist, prioritizes a thick shelf-like upper chest and peaked biceps and triceps that match your shoulder size for that iconic Arnold-era aesthetic.",
        visualBadge = "🏆",
        targetAesthetic = "Chest shelf, peaked biceps, vacuum waist",
        primaryExerciseFocus = "Incline Dumbbell Flyes, Dumbbell Pullovers, Barbell Curls, Dips",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800&q=80"
    ),
    SLEEPER(
        id = "sleeper",
        title = "The Sleeper Build",
        archetypeTag = "Deceptive Density",
        repRangeSummary = "10-15 Calisthenic & Hypertrophy",
        shortDescription = "Ultra-lean 10-12% body fat, average in clothes, shredded and dense shirt-off.",
        fullBio = "High relative strength with dense muscle separation in arms, back, and abs. In normal clothes you look completely average; shirt off, the vascularity and striations pop instantly.",
        visualBadge = "🥷",
        targetAesthetic = "Stealth definition, wire-lean core, high strength-to-weight",
        primaryExerciseFocus = "Weighted Dips, L-Sit Pull-ups, Hanging Leg Raises, Lateral Raises",
        realLifeVisualUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800&q=80"
    );

    companion object {
        val PRIMARY_BUILDS = listOf(V_TAPER, TANK, ATHLETE)
        val ALL_BUILDS = entries.toList()

        fun fromId(id: String?): PhysiqueBuild =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: V_TAPER
    }
}
