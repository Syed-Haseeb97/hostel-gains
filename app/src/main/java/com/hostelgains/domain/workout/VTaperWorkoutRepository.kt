package com.hostelgains.domain.workout

/**
 * Authoritative 7-Day V-Taper PPL Framework Database.
 * Accurately models all 6 training days and Day 7 Active Rest per R3 specifications.
 */
class VTaperWorkoutRepository {

    private val workoutSchedule: Map<Int, WorkoutDayPlan> = mapOf(
        1 to createDay1PullA(),
        2 to createDay2PushA(),
        3 to createDay3LegsAndCore(),
        4 to createDay4PullB(),
        5 to createDay5PushB(),
        6 to createDay6WeakPointPump(),
        7 to createDay7ActiveRest()
    )

    fun getDayPlan(dayNumber: Int): WorkoutDayPlan {
        require(dayNumber in 1..7) { "Day number must be between 1 and 7 (inclusive), got: $dayNumber" }
        return workoutSchedule[dayNumber] ?: error("No plan defined for day $dayNumber")
    }

    fun getAllPlans(): List<WorkoutDayPlan> {
        return (1..7).map { getDayPlan(it) }
    }

    private fun createDay1PullA(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 1,
            dayName = "Day 1: Pull A",
            focusArea = "Lat Width & Rear Delts",
            exercises = listOf(
                Exercise(
                    id = "d1_lat_pulldown",
                    name = "Wide-Grip Lat Pulldowns",
                    targetMuscle = MuscleGroup.LATS,
                    category = ExerciseCategory.COMPOUND_PULL,
                    baselineSets = 4,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Pull to upper chest, drive elbows down, 2s eccentric.",
                    isCompensatoryEligibleCarb = true
                ),
                Exercise(
                    id = "d1_cable_rows",
                    name = "Seated Cable Rows",
                    targetMuscle = MuscleGroup.BACK,
                    category = ExerciseCategory.COMPOUND_PULL,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Chest upright, pull to belly button, squeeze scaps.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d1_rear_delt_flyes",
                    name = "Machine Rear Delt Flyes",
                    targetMuscle = MuscleGroup.REAR_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 4,
                    repRange = "10-15 reps",
                    restSeconds = 60,
                    formNotes = "Lead with elbows, keep pinkies rotated slightly out.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d1_hammer_curls",
                    name = "Dumbbell Hammer Curls",
                    targetMuscle = MuscleGroup.BICEPS,
                    category = ExerciseCategory.ISOLATION_ARMS,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 60,
                    formNotes = "Neutral grip, pin elbows, control descent.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "Focus on maximum lat width development and mind-muscle contraction on the eccentric phase."
        )
    }

    private fun createDay2PushA(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 2,
            dayName = "Day 2: Push A",
            focusArea = "Upper Chest & Lateral Delts",
            exercises = listOf(
                Exercise(
                    id = "d2_incline_db_press",
                    name = "Incline Dumbbell Press",
                    targetMuscle = MuscleGroup.CHEST,
                    category = ExerciseCategory.COMPOUND_PUSH,
                    baselineSets = 4,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "30-degree incline, full pec stretch, tuck elbows 45°.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d2_db_lateral_raises",
                    name = "Dumbbell Lateral Raises",
                    targetMuscle = MuscleGroup.LATERAL_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 5,
                    repRange = "10-15 reps",
                    restSeconds = 60,
                    formNotes = "Slight torso lean, raise in scapular plane, lead with elbows.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d2_incline_pushups",
                    name = "Incline Push-ups",
                    targetMuscle = MuscleGroup.CHEST,
                    category = ExerciseCategory.COMPOUND_PUSH,
                    baselineSets = 3,
                    repRange = "To failure",
                    restSeconds = 60,
                    formNotes = "Hands on bench, brace core, explosive push.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d2_tricep_rope_pushdowns",
                    name = "Tricep Rope Pushdowns",
                    targetMuscle = MuscleGroup.TRICEPS,
                    category = ExerciseCategory.ISOLATION_ARMS,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 60,
                    formNotes = "Flare rope at bottom, squeeze horseshoe tricep.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "Emphasize clavicular upper pec head and lateral deltoid capping for V-taper frame width."
        )
    }

    private fun createDay3LegsAndCore(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 3,
            dayName = "Day 3: Legs & Core",
            focusArea = "Quad Sweep & Core Stability",
            exercises = listOf(
                Exercise(
                    id = "d3_goblet_squats",
                    name = "Goblet Squats",
                    targetMuscle = MuscleGroup.QUADS,
                    category = ExerciseCategory.COMPOUND_LEGS,
                    baselineSets = 4,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Dumbbell at chest, hit parallel, drive through midfoot.",
                    isCompensatoryEligibleCarb = true
                ),
                Exercise(
                    id = "d3_bw_squats",
                    name = "Bodyweight Squats",
                    targetMuscle = MuscleGroup.QUADS,
                    category = ExerciseCategory.COMPOUND_LEGS,
                    baselineSets = 2,
                    repRange = "To failure",
                    restSeconds = 60,
                    formNotes = "Continuous cadence, full depth, quad burnout.",
                    isCompensatoryEligibleCarb = true
                ),
                Exercise(
                    id = "d3_lying_leg_curls",
                    name = "Lying Leg Curls",
                    targetMuscle = MuscleGroup.HAMSTRINGS,
                    category = ExerciseCategory.ISOLATION_LEGS,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 60,
                    formNotes = "Dorsiflex toes, 3-second eccentric lowering.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d3_plank",
                    name = "Plank",
                    targetMuscle = MuscleGroup.CORE,
                    category = ExerciseCategory.CORE,
                    baselineSets = 3,
                    repRange = "60 sec",
                    restSeconds = 60,
                    formNotes = "Hollow-body position, squeeze glutes and transverse abdominis.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "Develop lower body quad sweep and deep abdominal brace without waist thickening."
        )
    }

    private fun createDay4PullB(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 4,
            dayName = "Day 4: Pull B",
            focusArea = "Lat Thickness & Biceps Focus",
            exercises = listOf(
                Exercise(
                    id = "d4_db_rows",
                    name = "Chest-Supported DB Rows",
                    targetMuscle = MuscleGroup.BACK,
                    category = ExerciseCategory.COMPOUND_PULL,
                    baselineSets = 4,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Squeeze rhomboids and mid-traps, full stretch at bottom.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d4_neutral_lat_pulldowns",
                    name = "Neutral-Grip Lat Pulldowns",
                    targetMuscle = MuscleGroup.LATS,
                    category = ExerciseCategory.COMPOUND_PULL,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Neutral grip engages lower lats, drive elbows down.",
                    isCompensatoryEligibleCarb = true
                ),
                Exercise(
                    id = "d4_incline_db_curls",
                    name = "Incline Dumbbell Curls",
                    targetMuscle = MuscleGroup.BICEPS,
                    category = ExerciseCategory.ISOLATION_ARMS,
                    baselineSets = 3,
                    repRange = "10-12 reps",
                    restSeconds = 60,
                    formNotes = "60-degree bench, full bicep stretch, supinate at top.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d4_preacher_curls",
                    name = "Preacher Curls",
                    targetMuscle = MuscleGroup.BICEPS,
                    category = ExerciseCategory.ISOLATION_ARMS,
                    baselineSets = 3,
                    repRange = "10-12 reps",
                    restSeconds = 60,
                    formNotes = "Elbows fixed on pad, strict isolation, no swinging.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d4_cable_face_pulls",
                    name = "Cable Face Pulls",
                    targetMuscle = MuscleGroup.REAR_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 3,
                    repRange = "12-15 reps",
                    restSeconds = 60,
                    formNotes = "Rope to bridge of nose, external shoulder rotation.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "Horizontal pull emphasis for mid-back thickness and dedicated biceps peak overload."
        )
    }

    private fun createDay5PushB(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 5,
            dayName = "Day 5: Push B",
            focusArea = "Cable Delts & Triceps Focus",
            exercises = listOf(
                Exercise(
                    id = "d5_oh_db_press",
                    name = "Standing Overhead DB Press",
                    targetMuscle = MuscleGroup.FRONT_DELTS,
                    category = ExerciseCategory.COMPOUND_PUSH,
                    baselineSets = 4,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Vertical press, brace core, do not hyperextend spine.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d5_cable_lateral_raises",
                    name = "Cable Lateral Raises",
                    targetMuscle = MuscleGroup.LATERAL_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 5,
                    repRange = "12-15 reps",
                    restSeconds = 60,
                    formNotes = "Cable behind back, constant tension across deltoid.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d5_flat_db_press",
                    name = "Flat Dumbbell Press",
                    targetMuscle = MuscleGroup.CHEST,
                    category = ExerciseCategory.COMPOUND_PUSH,
                    baselineSets = 3,
                    repRange = "8-12 reps",
                    restSeconds = 90,
                    formNotes = "Controlled descent, squeeze pecs together at top.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d5_oh_tricep_ext",
                    name = "Overhead Cable Tricep Extension",
                    targetMuscle = MuscleGroup.TRICEPS,
                    category = ExerciseCategory.ISOLATION_ARMS,
                    baselineSets = 3,
                    repRange = "10-12 reps",
                    restSeconds = 60,
                    formNotes = "Keep elbows high and stationary, stretch tricep long head.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d5_dips",
                    name = "Bodyweight Dips",
                    targetMuscle = MuscleGroup.TRICEPS,
                    category = ExerciseCategory.COMPOUND_PUSH,
                    baselineSets = 3,
                    repRange = "To failure",
                    restSeconds = 60,
                    formNotes = "Upright torso to emphasize triceps over chest.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "Continuous cable tension on medial deltoids and triceps long head development."
        )
    }

    private fun createDay6WeakPointPump(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 6,
            dayName = "Day 6: Weak-Point Pump",
            focusArea = "Weak-Point Pump (V-Taper Specialization & Metabolic Stress)",
            exercises = listOf(
                Exercise(
                    id = "d6_fst7_lat_pulldown",
                    name = "Lat Pulldowns",
                    targetMuscle = MuscleGroup.LATS,
                    category = ExerciseCategory.COMPOUND_PULL,
                    baselineSets = 7,
                    repRange = "14 reps",
                    restSeconds = 45,
                    formNotes = "FST-7 Protocol. Fascia stretch protocol. Strict weight, intense pump.",
                    isCompensatoryEligibleCarb = true
                ),
                Exercise(
                    id = "d6_cable_lateral_raises",
                    name = "Cable Lateral Raises",
                    targetMuscle = MuscleGroup.LATERAL_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 5,
                    repRange = "12 reps",
                    restSeconds = 45,
                    formNotes = "Continuous tension on side delts, maximum width focus.",
                    isCompensatoryEligibleCarb = false
                ),
                Exercise(
                    id = "d6_face_pulls",
                    name = "Face Pulls",
                    targetMuscle = MuscleGroup.REAR_DELTS,
                    category = ExerciseCategory.ISOLATION_DELTS,
                    baselineSets = 3,
                    repRange = "15 reps",
                    restSeconds = 60,
                    formNotes = "High rep posture and rotator cuff endurance.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = false,
            guidanceNotes = "High-volume metabolic pump targeting V-taper weak points with minimal joint wear."
        )
    }

    private fun createDay7ActiveRest(): WorkoutDayPlan {
        return WorkoutDayPlan(
            dayNumber = 7,
            dayName = "Day 7: Active Rest",
            focusArea = "Active Rest & Recovery",
            exercises = listOf(
                Exercise(
                    id = "d7_active_recovery_walk",
                    name = "Active Recovery Walking",
                    targetMuscle = MuscleGroup.ACTIVE_RECOVERY,
                    category = ExerciseCategory.ACTIVE_RECOVERY,
                    baselineSets = 1,
                    repRange = "5,000 - 10,000 steps",
                    restSeconds = 0,
                    formNotes = "Zero resistance training. Focus on walking, hydration, foam rolling, and joint decompression.",
                    isCompensatoryEligibleCarb = false
                )
            ),
            isActiveRest = true,
            guidanceNotes = "5,000 - 10,000 steps only."
        )
    }
}
