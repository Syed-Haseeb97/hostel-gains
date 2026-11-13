package com.hostelgains.domain.workout

import com.hostelgains.domain.physique.PhysiqueBuild

/**
 * Dynamic Multi-Path Workout Engine.
 * Decouples workout logic from a static single-plan architecture and dynamically
 * serves tailored 6-day PPL + Day 7 recovery routines based on the active [PhysiqueBuild].
 */
class DynamicWorkoutRepository(
    private var activeBuild: PhysiqueBuild = PhysiqueBuild.V_TAPER
) {
    private val vTaperRepo = VTaperWorkoutRepository()
    private val customAiPlans = mutableMapOf<Int, WorkoutDayPlan>()
    private var coachRationale: String = ""
    private var isCustomAiActive: Boolean = false

    fun setTargetBuild(build: PhysiqueBuild) {
        if (activeBuild != build) {
            activeBuild = build
            // Clear custom AI plan if the user explicitly switches physique builds
            clearCustomAiPlans()
        }
    }

    fun getActiveBuild(): PhysiqueBuild = activeBuild

    fun setCustomAiPlans(plans: List<WorkoutDayPlan>, rationale: String = "") {
        customAiPlans.clear()
        for (p in plans) {
            customAiPlans[p.dayNumber] = p
        }
        coachRationale = rationale
        isCustomAiActive = customAiPlans.isNotEmpty()
    }

    fun clearCustomAiPlans() {
        customAiPlans.clear()
        coachRationale = ""
        isCustomAiActive = false
    }

    fun hasCustomAiPlans(): Boolean = isCustomAiActive && customAiPlans.isNotEmpty()

    fun getCoachRationale(): String = coachRationale

    fun getDayPlan(dayNumber: Int, build: PhysiqueBuild = activeBuild): WorkoutDayPlan {
        require(dayNumber in 1..7) { "Day number must be between 1 and 7 (inclusive), got: $dayNumber" }
        if (isCustomAiActive && customAiPlans.containsKey(dayNumber)) {
            val custom = customAiPlans[dayNumber]
            if (custom != null) return custom
        }
        return when (build) {
            PhysiqueBuild.V_TAPER -> vTaperRepo.getDayPlan(dayNumber)
            PhysiqueBuild.TANK -> getTankPlan(dayNumber)
            PhysiqueBuild.ATHLETE -> getAthletePlan(dayNumber)
            PhysiqueBuild.X_FRAME -> getXFramePlan(dayNumber)
            PhysiqueBuild.YOKE -> getYokePlan(dayNumber)
            PhysiqueBuild.GOLDEN_ERA -> getGoldenEraPlan(dayNumber)
            PhysiqueBuild.SLEEPER -> getSleeperPlan(dayNumber)
        }
    }

    fun getAllPlans(build: PhysiqueBuild = activeBuild): List<WorkoutDayPlan> {
        return (1..7).map { getDayPlan(it, build) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. THE TANK (Raw Strength Focus: 4-6 Reps, Thick Back, Traps, Squats)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getTankPlan(dayNumber: Int): WorkoutDayPlan = when (dayNumber) {
        1 -> WorkoutDayPlan(
            dayNumber = 1,
            dayName = "Day 1: Pull A (Heavy Mass)",
            focusArea = "Heavy Deadlifts & Upper Back Thickness (Tank)",
            exercises = listOf(
                Exercise("t1_deadlift", "Conventional Barbell Deadlifts", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "4-6 reps", 120, "Drive floor away, lock hips with glutes, reset each rep.", isCompensatoryEligibleCarb = true),
                Exercise("t1_tbar_row", "Chest-Supported / T-Bar Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "5-6 reps", 120, "Pull to ribcage, squeeze thick rhomboids, control descent.", isCompensatoryEligibleCarb = true),
                Exercise("t1_heavy_shrugs", "Heavy Dumbbell / Barbell Shrugs", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "6-8 reps", 90, "2-second isometric hold at peak trap contraction.", isCompensatoryEligibleCarb = false),
                Exercise("t1_bb_curl", "Heavy Barbell Bicep Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "6-8 reps", 90, "Strict form, elbows pinned, heavy arm loading.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Maximal central nervous system recruitment. Rest 2 full minutes between heavy sets."
        )
        2 -> WorkoutDayPlan(
            dayNumber = 2,
            dayName = "Day 2: Push A (Heavy Power)",
            focusArea = "Heavy Bench & Military Press (Tank)",
            exercises = listOf(
                Exercise("t2_flat_bench", "Heavy Flat Barbell / DB Bench Press", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "4-6 reps", 120, "Retract scapulae, touch sternum, explode up with authority.", isCompensatoryEligibleCarb = true),
                Exercise("t2_military_press", "Standing Barbell Military Press", MuscleGroup.FRONT_DELTS, ExerciseCategory.COMPOUND_PUSH, 4, "4-6 reps", 120, "Full extension, brace core like concrete, press overhead.", isCompensatoryEligibleCarb = false),
                Exercise("t2_weighted_dips", "Heavy Weighted / Bodyweight Dips", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 3, "5-6 reps", 90, "Load extra weight if bodyweight allows, deep 90° depth.", isCompensatoryEligibleCarb = false),
                Exercise("t2_close_grip_bench", "Close-Grip Barbell Bench Press", MuscleGroup.TRICEPS, ExerciseCategory.COMPOUND_PUSH, 3, "5-6 reps", 90, "Hands shoulder-width, tuck elbows, brutal tricep overload.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Dense chest and tricep mass. Use controlled eccentrics to protect shoulders."
        )
        3 -> WorkoutDayPlan(
            dayNumber = 3,
            dayName = "Day 3: Legs (Colossus Power)",
            focusArea = "Heavy Squats & Posterior Chain (Tank)",
            exercises = listOf(
                Exercise("t3_bb_squat", "Heavy Barbell / DB Back Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 5, "4-6 reps", 150, "Break parallel, drive through midfoot, recruit heavy quad mass.", isCompensatoryEligibleCarb = true),
                Exercise("t3_stiff_leg_dl", "Stiff-Legged Deadlifts", MuscleGroup.HAMSTRINGS, ExerciseCategory.COMPOUND_LEGS, 4, "5-6 reps", 120, "Hinge back, stretch hamstrings to breaking point, snap hips.", isCompensatoryEligibleCarb = false),
                Exercise("t3_heavy_calves", "Standing Heavy Calf Raises", MuscleGroup.CALVES, ExerciseCategory.ISOLATION_LEGS, 4, "6-8 reps", 75, "Pause 2 seconds at bottom stretch, explode up.", isCompensatoryEligibleCarb = false),
                Exercise("t3_ab_wheel", "Ab Wheel Rollouts", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "8-10 reps", 60, "Brace anterior core against heavy spinal shear.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Build tree-trunk legs and bulletproof core for raw power."
        )
        4 -> WorkoutDayPlan(
            dayNumber = 4,
            dayName = "Day 4: Pull B (Back Density)",
            focusArea = "Heavy Pendlay Rows & Heavy Lats (Tank)",
            exercises = listOf(
                Exercise("t4_pendlay_row", "Barbell Pendlay Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "4-6 reps", 120, "Torso parallel to floor, explode bar off dead floor stop.", isCompensatoryEligibleCarb = true),
                Exercise("t4_weighted_pullups", "Weighted / Heavy Lat Pulldowns", MuscleGroup.LATS, ExerciseCategory.COMPOUND_PULL, 4, "5-6 reps", 120, "Drive elbows hard into pockets, thick lat flare.", isCompensatoryEligibleCarb = false),
                Exercise("t4_heavy_facepull", "Heavy Cable Face Pulls", MuscleGroup.REAR_DELTS, ExerciseCategory.ISOLATION_DELTS, 4, "6-8 reps", 75, "Reinforce rear shoulder girdle for heavy pressing.", isCompensatoryEligibleCarb = false),
                Exercise("t4_heavy_hammer", "Heavy DB Hammer Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "6-8 reps", 75, "Thicken forearms and brachialis for heavy pulling grip.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Dead-stop rows eliminate momentum, forcing 100% muscular contraction."
        )
        5 -> WorkoutDayPlan(
            dayNumber = 5,
            dayName = "Day 5: Push B (Armor Shell)",
            focusArea = "Heavy Incline & Shoulder Overload (Tank)",
            exercises = listOf(
                Exercise("t5_incline_heavy_bb", "Heavy Incline Barbell Press", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "4-6 reps", 120, "Thick upper shelf, lock out elbows with authority.", isCompensatoryEligibleCarb = true),
                Exercise("t5_seated_db_press", "Heavy Seated DB Shoulder Press", MuscleGroup.FRONT_DELTS, ExerciseCategory.COMPOUND_PUSH, 4, "5-6 reps", 120, "Solid back support, heavy compound shoulder loading.", isCompensatoryEligibleCarb = false),
                Exercise("t5_heavy_laterals", "Heavy Dumbbell Lateral Swings", MuscleGroup.LATERAL_DELTS, ExerciseCategory.ISOLATION_DELTS, 4, "8-10 reps", 75, "Controlled power swing to eye level.", isCompensatoryEligibleCarb = false),
                Exercise("t5_cable_pushdowns", "Straight Bar Cable Pushdowns", MuscleGroup.TRICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "6-8 reps", 75, "Heavy pin, lock out triceps at bottom.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "High force output. Prioritize bar speed and stability."
        )
        6 -> WorkoutDayPlan(
            dayNumber = 6,
            dayName = "Day 6: Power Armor",
            focusArea = "Farmer's Carries, Shrugs & Zerchers (Tank)",
            exercises = listOf(
                Exercise("t6_farmers_walk", "Heavy Farmer's Carries", MuscleGroup.CORE, ExerciseCategory.COMPOUND_PULL, 4, "40s walk", 90, "Pick up heaviest dumbbells in gym, walk upright with zero sway.", isCompensatoryEligibleCarb = true),
                Exercise("t6_power_shrugs", "Barbell Power Shrugs", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "6-8 reps", 90, "Thicken neck and upper traps to prevent neck pain.", isCompensatoryEligibleCarb = false),
                Exercise("t6_front_squat", "Front Squats / Zercher Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 3, "5-6 reps", 120, "Upright torso, brutal anterior core and quad loading.", isCompensatoryEligibleCarb = false),
                Exercise("t6_wrist_curls", "Barbell Wrist & Forearm Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "10-12 reps", 60, "Build crushing grip strength for heavy deadlifts.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Loaded carries build whole-body structural armor."
        )
        else -> createActiveRestDay(7)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THE ATHLETE (Lean & Functional: 12-15 Reps, Plyometrics & Conditioning)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getAthletePlan(dayNumber: Int): WorkoutDayPlan = when (dayNumber) {
        1 -> WorkoutDayPlan(
            dayNumber = 1,
            dayName = "Day 1: Pull A (Functional Pull)",
            focusArea = "Explosive Pull-ups & Scapular Stability (Athlete)",
            exercises = listOf(
                Exercise("a1_plyo_pullups", "Explosive Pull-ups / Banded Pulldowns", MuscleGroup.LATS, ExerciseCategory.COMPOUND_PULL, 4, "10-12 reps", 75, "Explosive chest-to-bar pull, smooth 2s descent.", isCompensatoryEligibleCarb = true),
                Exercise("a1_renegade_row", "Unilateral DB Renegade Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 3, "12 reps each", 60, "Push-up position plank, row dumbbell to hip without twisting hips.", isCompensatoryEligibleCarb = false),
                Exercise("a1_face_pull_rot", "Face Pulls with External Rotation", MuscleGroup.REAR_DELTS, ExerciseCategory.ISOLATION_DELTS, 4, "15 reps", 60, "High elbows, external shoulder rotation for rotator cuff health.", isCompensatoryEligibleCarb = false),
                Exercise("a1_incline_curls", "Incline Dumbbell Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "12-15 reps", 60, "Strict hypertrophy in lengthened position.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Explosive power on pulling movements with high core anti-rotation."
        )
        2 -> WorkoutDayPlan(
            dayNumber = 2,
            dayName = "Day 2: Push A (Elastic Power)",
            focusArea = "Plyo Push-ups & Push Press (Athlete)",
            exercises = listOf(
                Exercise("a2_plyo_pushups", "Explosive Plyo / Clapping Push-ups", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "10-12 reps", 75, "Explode off floor, absorb landing softly through chest.", isCompensatoryEligibleCarb = true),
                Exercise("a2_push_press", "Single-Arm DB Push Press", MuscleGroup.FRONT_DELTS, ExerciseCategory.COMPOUND_PUSH, 3, "12 reps each", 60, "Dip knees slightly, drive explosively with hips into overhead press.", isCompensatoryEligibleCarb = false),
                Exercise("a2_laterals_tempo", "Controlled Tempo DB Lateral Raises", MuscleGroup.LATERAL_DELTS, ExerciseCategory.ISOLATION_DELTS, 4, "15 reps", 60, "Lead with elbows, 3s descent for elastic shoulder stability.", isCompensatoryEligibleCarb = false),
                Exercise("a2_dips_bw", "Bodyweight Dips", MuscleGroup.TRICEPS, ExerciseCategory.COMPOUND_PUSH, 3, "12-15 reps", 60, "Full depth, drive through palms.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Focus on explosive intent and elastic energy return."
        )
        3 -> WorkoutDayPlan(
            dayNumber = 3,
            dayName = "Day 3: Legs & Conditioning",
            focusArea = "Bulgarian Split Squats & Swings (Athlete)",
            exercises = listOf(
                Exercise("a3_bulgarian_squat", "Bulgarian Split Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 4, "12 reps each", 75, "Rear foot on bench, break parallel, single-leg hip stability.", isCompensatoryEligibleCarb = true),
                Exercise("a3_kb_swings", "Kettlebell / DB Hip Swings", MuscleGroup.HAMSTRINGS, ExerciseCategory.COMPOUND_LEGS, 4, "15-20 reps", 60, "Violent hip hinge, snap glutes forward at top, do not squat.", isCompensatoryEligibleCarb = false),
                Exercise("a3_jump_squats", "Explosive Bodyweight Jump Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 3, "12 reps", 60, "Triple extension: ankle, knee, hip. Soft landing.", isCompensatoryEligibleCarb = false),
                Exercise("a3_hollow_body", "Hollow Body Rocks", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "45s hold", 45, "Lower back pressed flush to floor, gymnastics core tension.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Build unilateral balance, knee resilience, and athletic power."
        )
        4 -> WorkoutDayPlan(
            dayNumber = 4,
            dayName = "Day 4: Pull B (Rotational & Chain)",
            focusArea = "Single-Arm Cable Rows & Deadlift Hinge (Athlete)",
            exercises = listOf(
                Exercise("a4_cable_high_row", "Single-Arm Cable High Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 4, "12 reps each", 60, "Rotate torso slightly into row, engage core obliques.", isCompensatoryEligibleCarb = true),
                Exercise("a4_single_leg_rdl", "Single-Leg Dumbbell RDLs", MuscleGroup.HAMSTRINGS, ExerciseCategory.COMPOUND_LEGS, 3, "12 reps each", 60, "Hinge on single foot, balance ankle and hamstring.", isCompensatoryEligibleCarb = false),
                Exercise("a4_inverted_rows", "Bodyweight Inverted Rows", MuscleGroup.BACK, ExerciseCategory.COMPOUND_PULL, 3, "12-15 reps", 60, "Barbell at waist height, pull chest to bar.", isCompensatoryEligibleCarb = false),
                Exercise("a4_cross_body_hammer", "Cross-Body Hammer Curls", MuscleGroup.BICEPS, ExerciseCategory.ISOLATION_ARMS, 3, "12 reps", 45, "Curl across chest for forearm/grip strength.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Posterior chain connection and anti-rotational stability."
        )
        5 -> WorkoutDayPlan(
            dayNumber = 5,
            dayName = "Day 5: Push B (Mobility & Power)",
            focusArea = "Pike Press & Woodchoppers (Athlete)",
            exercises = listOf(
                Exercise("a5_pike_pushups", "Pike Push-ups / Handstand Progression", MuscleGroup.FRONT_DELTS, ExerciseCategory.COMPOUND_PUSH, 3, "10-12 reps", 60, "Hips high in pike, head moves forward of hands.", isCompensatoryEligibleCarb = false),
                Exercise("a5_incline_db_press", "Incline Dumbbell Press", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "12-15 reps", 75, "Rhythmic cadence, full range of motion.", isCompensatoryEligibleCarb = true),
                Exercise("a5_woodchoppers", "Cable / DB Diagonal Woodchoppers", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "15 reps each side", 60, "Pivot back foot, generate rotational core whip.", isCompensatoryEligibleCarb = false),
                Exercise("a5_waiter_walk", "Single-Arm Overhead Waiter's Walk", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "45s each arm", 60, "Lock out dumbbell overhead, walk tall with tight core.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Scapular upward rotation and shoulder overhead health."
        )
        6 -> WorkoutDayPlan(
            dayNumber = 6,
            dayName = "Day 6: Hybrid Stamina Circuit",
            focusArea = "Full-Body Metabolic Conditioning (Athlete)",
            exercises = listOf(
                Exercise("a6_thrusters", "Dumbbell Thrusters", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 4, "15 reps", 75, "Front squat into continuous explosive overhead press.", isCompensatoryEligibleCarb = true),
                Exercise("a6_bear_crawls", "Forward & Reverse Bear Crawls", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "20 meters", 60, "Knees hovering 2 inches off floor, contralateral limbs.", isCompensatoryEligibleCarb = false),
                Exercise("a6_burpees", "Burpees with Tuck Jump", MuscleGroup.ACTIVE_RECOVERY, ExerciseCategory.ACTIVE_RECOVERY, 3, "15 reps", 60, "Chest to deck, jump up explosively.", isCompensatoryEligibleCarb = false),
                Exercise("a6_hanging_knees", "Hanging Knee-to-Elbows", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "15 reps", 45, "Compress abs and hip flexors dynamically.", isCompensatoryEligibleCarb = false)
            ),
            guidanceNotes = "Elevate VO2 max and burn calories while preserving muscle."
        )
        else -> createActiveRestDay(7)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. EXTENDED ARCHETYPES (X-Frame, Yoke, Golden Era, Sleeper)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getXFramePlan(dayNumber: Int): WorkoutDayPlan {
        // Base V-Taper plan with aggressive outer quad sweep & calf volume on Leg days
        val vPlan = vTaperRepo.getDayPlan(dayNumber)
        if (dayNumber == 3) {
            return WorkoutDayPlan(
                dayNumber = 3,
                dayName = "Day 3: Legs (X-Frame Sweep)",
                focusArea = "Vastus Lateralis Quad Sweep & Calves (X-Frame)",
                exercises = listOf(
                    Exercise("x3_elevated_squats", "Heel-Elevated Goblet / BB Squats", MuscleGroup.QUADS, ExerciseCategory.COMPOUND_LEGS, 4, "8-10 reps", 90, "Narrow stance, elevated heels, direct tension on outer quad sweep.", isCompensatoryEligibleCarb = true),
                    Exercise("x3_leg_extensions", "Leg Extensions / Sissy Squats", MuscleGroup.QUADS, ExerciseCategory.ISOLATION_LEGS, 4, "10-12 reps", 60, "2-second peak quad squeeze at top.", isCompensatoryEligibleCarb = false),
                    Exercise("x3_standing_calves", "Standing Single-Leg Calf Raises", MuscleGroup.CALVES, ExerciseCategory.ISOLATION_LEGS, 4, "12-15 reps", 45, "Full stretch at bottom, drive through big toe.", isCompensatoryEligibleCarb = false),
                    Exercise("x3_leg_raises", "Hanging Leg Raises", MuscleGroup.CORE, ExerciseCategory.CORE, 3, "12-15 reps", 60, "Keep waist vacuum tight.", isCompensatoryEligibleCarb = false)
                ),
                guidanceNotes = "Build the lower half of the 'X' to balance out wide shoulders."
            )
        }
        return vPlan
    }

    private fun getYokePlan(dayNumber: Int): WorkoutDayPlan = getTankPlan(dayNumber)

    private fun getGoldenEraPlan(dayNumber: Int): WorkoutDayPlan {
        val vPlan = vTaperRepo.getDayPlan(dayNumber)
        if (dayNumber == 2) {
            return WorkoutDayPlan(
                dayNumber = 2,
                dayName = "Day 2: Push A (Chest Shelf)",
                focusArea = "Upper Chest Shelf & Peak Biceps (Golden Era)",
                exercises = listOf(
                    Exercise("g2_incline_flyes", "Incline Dumbbell Flyes", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "10-12 reps", 75, "Deep 1970s ribcage stretch at bottom, hug a tree at top.", isCompensatoryEligibleCarb = false),
                    Exercise("g2_pullovers", "Dumbbell Pullovers", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 4, "10-12 reps", 75, "Across flat bench, drop hips, expand ribcage.", isCompensatoryEligibleCarb = true),
                    Exercise("g2_laterals", "Dumbbell Lateral Raises", MuscleGroup.LATERAL_DELTS, ExerciseCategory.ISOLATION_DELTS, 4, "12 reps", 60, "Strict form, golden symmetry.", isCompensatoryEligibleCarb = false),
                    Exercise("g2_dips", "Chest Dips with Forward Lean", MuscleGroup.CHEST, ExerciseCategory.COMPOUND_PUSH, 3, "10 reps", 60, "Lower chest line definition.", isCompensatoryEligibleCarb = false)
                ),
                guidanceNotes = "Focus on aesthetic proportions, chest fullness, and vacuum posing."
            )
        }
        return vPlan
    }

    private fun getSleeperPlan(dayNumber: Int): WorkoutDayPlan = getAthletePlan(dayNumber)

    private fun createActiveRestDay(dayNumber: Int): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = dayNumber,
            dayName = "Day 7: Active Recovery",
            focusArea = "Hostel Step Clearance & Decompression",
            exercises = listOf(
                Exercise("d7_walk", "Campus Low-Intensity Walk", MuscleGroup.ACTIVE_RECOVERY, ExerciseCategory.ACTIVE_RECOVERY, 1, "45-60 min", 0, "Outdoor walk around university grounds to clear 8,000 steps."),
                Exercise("d7_dead_hangs", "Decompression Dead Hangs", MuscleGroup.ACTIVE_RECOVERY, ExerciseCategory.ACTIVE_RECOVERY, 3, "45-60s hold", 60, "Hang passively from pull-up bar, decompress spine and shoulders."),
                Exercise("d7_mobility", "Thoracic Spine & Hip Mobility Flow", MuscleGroup.ACTIVE_RECOVERY, ExerciseCategory.ACTIVE_RECOVERY, 3, "10 reps each", 45, "Cat-cow, world's greatest stretch, 90/90 hip switches.")
            ),
            isActiveRest = true,
            guidanceNotes = "Active recovery promotes glycogen resensitization, flushing metabolic waste from muscles."
        )
    }
}
