package com.hostelgains.ui.kinematics

/**
 * Authoritative 28 exercise routines ported 1:1 from moves.ts.
 * Includes complete joint poses, target muscles, form cues, and equipment specifications.
 */
object ExerciseRoutines {

    private val LIE = JointPose(hip = 90f, knee = 90f, torso = -90f)

    val BENCH = ExerciseMove(
        id = "bench",
        name = "Barbell Bench Press",
        targetMuscles = listOf("Chest", "Front delts", "Triceps"),
        primaryPose = LIE.copy(shoulder = 180f, elbow = 0f),
        secondaryPose = LIE.copy(shoulder = 70f, elbow = 110f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Lower to mid-chest, drive up and lock out."),
        isBench = true,
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO),
        cameraOffsetX = -110f
    )

    val INCLINE = ExerciseMove(
        id = "incline",
        name = "Incline Dumbbell Press",
        targetMuscles = listOf("Upper chest", "Front delts"),
        primaryPose = JointPose(hip = 90f, knee = 90f, torso = -60f, shoulder = 180f, elbow = 0f),
        secondaryPose = JointPose(hip = 90f, knee = 90f, torso = -60f, shoulder = 95f, elbow = 95f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Press up and slightly together over the upper chest."),
        isBench = true,
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO),
        cameraOffsetX = -80f
    )

    val FLY = ExerciseMove(
        id = "fly",
        name = "Cable Fly",
        targetMuscles = listOf("Chest", "Front delts"),
        primaryPose = JointPose(torso = 8f, shoulder = 20f, elbow = 15f, abduction = 80f),
        secondaryPose = JointPose(torso = 8f, shoulder = 70f, elbow = 15f, abduction = 8f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Soft elbows, hug a barrel and squeeze at the centre."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val SKULL = ExerciseMove(
        id = "skull",
        name = "Skull Crushers",
        targetMuscles = listOf("Triceps"),
        primaryPose = LIE.copy(shoulder = 180f, elbow = 0f),
        secondaryPose = LIE.copy(shoulder = 180f, elbow = 105f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Elbows fixed, lower the bar towards the forehead."),
        isBench = true,
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS),
        cameraOffsetX = -110f
    )

    val PUSHDOWN = ExerciseMove(
        id = "pushdown",
        name = "Rope Pushdown",
        targetMuscles = listOf("Triceps"),
        primaryPose = JointPose(torso = 6f, shoulder = 8f, elbow = 125f),
        secondaryPose = JointPose(torso = 6f, shoulder = 8f, elbow = 5f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Pin elbows to your sides and extend fully."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS)
    )

    val DEADLIFT = ExerciseMove(
        id = "deadlift",
        name = "Deadlift",
        targetMuscles = listOf("Glutes", "Hamstrings", "Back"),
        primaryPose = JointPose(hip = 70f, knee = 75f, torso = 62f, shoulder = 5f, elbow = 0f),
        secondaryPose = JointPose(hip = 0f, knee = 0f, torso = 0f, shoulder = 16f, elbow = 0f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Flat back, push the floor away, lock hips at top."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.TORSO)
    )

    val RDL = ExerciseMove(
        id = "rdl",
        name = "Romanian Deadlift",
        targetMuscles = listOf("Hamstrings", "Glutes"),
        primaryPose = JointPose(hip = 40f, knee = 12f, torso = 70f, shoulder = 0f, elbow = 0f),
        secondaryPose = JointPose(hip = 0f, knee = 0f, torso = 0f, shoulder = 10f, elbow = 0f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Hips back, bar slides down the thighs."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.TORSO)
    )

    val PULLUP = ExerciseMove(
        id = "pullup",
        name = "Weighted Pull-up",
        targetMuscles = listOf("Lats", "Biceps", "Upper back"),
        primaryPose = JointPose(hip = 0f, knee = 60f, torso = 0f, shoulder = 180f, elbow = 0f),
        secondaryPose = JointPose(hip = 0f, knee = 60f, torso = -8f, shoulder = 25f, elbow = 155f),
        apparatus = ApparatusType.BAR,
        formCues = listOf("Start from a dead hang, chin over the bar."),
        isHang = true,
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val ROW = ExerciseMove(
        id = "row",
        name = "Barbell Row",
        targetMuscles = listOf("Lats", "Rhomboids", "Rear delts"),
        primaryPose = JointPose(hip = 40f, knee = 45f, torso = 50f, shoulder = 0f, elbow = 0f),
        secondaryPose = JointPose(hip = 40f, knee = 45f, torso = 50f, shoulder = -45f, elbow = 70f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Hinge, brace, pull the bar to your lower ribs."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val CURL = ExerciseMove(
        id = "curl",
        name = "Hammer Curl",
        targetMuscles = listOf("Biceps", "Brachialis"),
        primaryPose = JointPose(shoulder = 0f, elbow = 0f),
        secondaryPose = JointPose(shoulder = 0f, elbow = 140f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Elbows stay still; no swinging."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS)
    )

    val ICURL = ExerciseMove(
        id = "icurl",
        name = "Incline Curl",
        targetMuscles = listOf("Biceps (long head)"),
        primaryPose = JointPose(torso = -25f, shoulder = -30f, elbow = 0f),
        secondaryPose = JointPose(torso = -25f, shoulder = -30f, elbow = 140f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Arms behind the torso for a deep stretch."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS)
    )

    val SQUAT = ExerciseMove(
        id = "squat",
        name = "Back Squat",
        targetMuscles = listOf("Quads", "Glutes", "Core"),
        primaryPose = JointPose(shoulder = -33f, elbow = 197f),
        secondaryPose = JointPose(hip = 80f, knee = 115f, torso = 35f, shoulder = -68f, elbow = 197f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Brace, sit between the hips, drive up."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS)
    )

    val FRONT = ExerciseMove(
        id = "front",
        name = "Front Squat",
        targetMuscles = listOf("Quads", "Upper back", "Core"),
        primaryPose = JointPose(shoulder = 29f, elbow = 155f),
        secondaryPose = JointPose(hip = 85f, knee = 120f, torso = 12f, shoulder = 17f, elbow = 155f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Elbows high, chest tall."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.TORSO)
    )

    val LEGPRESS = ExerciseMove(
        id = "legpress",
        name = "Leg Press",
        targetMuscles = listOf("Quads", "Glutes"),
        primaryPose = JointPose(shoulder = 60f, elbow = 0f),
        secondaryPose = JointPose(hip = 70f, knee = 100f, torso = 25f, shoulder = 60f, elbow = 0f),
        apparatus = ApparatusType.NONE,
        formCues = listOf("Controlled depth, never lock knees hard."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS)
    )

    val LUNGE = ExerciseMove(
        id = "lunge",
        name = "Walking Lunge",
        targetMuscles = listOf("Quads", "Glutes"),
        primaryPose = JointPose(shoulder = 0f, elbow = 0f),
        secondaryPose = JointPose(hip = 60f, knee = 100f, torso = 15f, shoulder = 0f, elbow = 0f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Long step, vertical shin, drive through the heel."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS)
    )

    val CALF = ExerciseMove(
        id = "calf",
        name = "Calf Raise",
        targetMuscles = listOf("Gastrocnemius", "Soleus"),
        primaryPose = JointPose(shoulder = 0f),
        secondaryPose = JointPose(shoulder = 0f, dy = 14f, foot = 35f),
        apparatus = ApparatusType.NONE,
        formCues = listOf("Pause at the top, full stretch at the bottom."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS)
    )

    val OHP = ExerciseMove(
        id = "ohp",
        name = "Overhead Press",
        targetMuscles = listOf("Shoulders", "Triceps", "Core"),
        primaryPose = JointPose(shoulder = 29f, elbow = 155f),
        secondaryPose = JointPose(shoulder = 180f, elbow = 0f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Squeeze glutes, press straight overhead."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val LATERAL = ExerciseMove(
        id = "lateral",
        name = "Lateral Raise",
        targetMuscles = listOf("Side delts"),
        primaryPose = JointPose(shoulder = 0f, elbow = 12f, abduction = 4f),
        secondaryPose = JointPose(shoulder = 0f, elbow = 12f, abduction = 88f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Lead with the elbows to shoulder height."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS)
    )

    val FACEPULL = ExerciseMove(
        id = "facepull",
        name = "Face Pull",
        targetMuscles = listOf("Rear delts", "Upper back"),
        primaryPose = JointPose(torso = -4f, shoulder = 88f, elbow = 0f, abduction = 10f),
        secondaryPose = JointPose(torso = -4f, shoulder = 70f, elbow = 85f, abduction = 40f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Pull to the face, elbows high and wide."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val LEGRAISE = ExerciseMove(
        id = "legraise",
        name = "Hanging Leg Raise",
        targetMuscles = listOf("Abs", "Hip flexors"),
        primaryPose = JointPose(hip = 0f, knee = 0f, torso = 0f, shoulder = 180f, elbow = 0f),
        secondaryPose = JointPose(hip = 95f, knee = 10f, torso = 6f, shoulder = 180f, elbow = 0f),
        apparatus = ApparatusType.BAR,
        formCues = listOf("No swing, curl the pelvis up."),
        isHang = true,
        highlightedGroups = listOf(MuscleHighlightGroup.TORSO, MuscleHighlightGroup.LEGS)
    )

    val CRUNCH = ExerciseMove(
        id = "crunch",
        name = "Cable Crunch",
        targetMuscles = listOf("Abs"),
        primaryPose = LIE.copy(shoulder = -100f, elbow = 120f),
        secondaryPose = JointPose(hip = 90f, knee = 90f, torso = -52f, shoulder = -100f, elbow = 120f),
        apparatus = ApparatusType.NONE,
        formCues = listOf("Ribs to pelvis, exhale at the top."),
        isBench = true,
        highlightedGroups = listOf(MuscleHighlightGroup.TORSO),
        cameraOffsetX = -110f
    )

    val CLEAN = ExerciseMove(
        id = "clean",
        name = "Power Clean",
        targetMuscles = listOf("Full body", "Explosive hips"),
        primaryPose = JointPose(hip = 70f, knee = 75f, torso = 62f, shoulder = 5f, elbow = 0f),
        secondaryPose = JointPose(hip = 0f, knee = 0f, torso = 0f, shoulder = 29f, elbow = 155f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Jump, shrug, catch the bar on the shoulders."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO)
    )

    val PUSHPRESS = ExerciseMove(
        id = "pushpress",
        name = "Push Press",
        targetMuscles = listOf("Shoulders", "Triceps", "Legs"),
        primaryPose = JointPose(hip = 25f, knee = 50f, torso = 0f, shoulder = 29f, elbow = 155f),
        secondaryPose = JointPose(shoulder = 180f, elbow = 0f),
        apparatus = ApparatusType.BARBELL,
        formCues = listOf("Dip, drive, punch the bar overhead."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.ARMS)
    )

    val CARRY = ExerciseMove(
        id = "carry",
        name = "Farmer Carry",
        targetMuscles = listOf("Grip", "Traps", "Core"),
        primaryPose = JointPose(hip = 4f, knee = 4f, shoulder = 0f, elbow = 0f, dy = 0f),
        secondaryPose = JointPose(hip = -3f, knee = 8f, shoulder = 0f, elbow = 0f, dy = 3f),
        apparatus = ApparatusType.DUMBBELL,
        formCues = listOf("Tall posture, short quick steps."),
        highlightedGroups = listOf(MuscleHighlightGroup.ARMS, MuscleHighlightGroup.TORSO),
        periodSeconds = 1.2f
    )

    val FOLD = ExerciseMove(
        id = "fold",
        name = "Foam Rolling",
        targetMuscles = listOf("Hamstrings", "Lower back", "Hips"),
        primaryPose = JointPose(shoulder = 180f, elbow = 0f),
        secondaryPose = JointPose(hip = 8f, knee = 0f, torso = 85f, shoulder = 5f, elbow = 0f),
        apparatus = ApparatusType.NONE,
        formCues = listOf("Breathe slowly and relax into the stretch."),
        highlightedGroups = listOf(MuscleHighlightGroup.LEGS, MuscleHighlightGroup.TORSO),
        periodSeconds = 4.0f
    )

    val REST = ExerciseMove(
        id = "rest",
        name = "Sleep, eat, repeat",
        targetMuscles = listOf("Whole body"),
        primaryPose = JointPose(hip = 90f, knee = 0f, torso = -90f, shoulder = 100f, elbow = 0f),
        secondaryPose = JointPose(hip = 90f, knee = 0f, torso = -90f, shoulder = 100f, elbow = 0f, dy = 2f),
        apparatus = ApparatusType.NONE,
        formCues = listOf("Sleep 7-9 hours. Muscle is built while you rest."),
        highlightedGroups = emptyList(),
        cameraOffsetX = -110f,
        periodSeconds = 5.0f
    )

    // Secondary aliases for 28 complete moves
    val HIP_MOBILITY_FLOW = FOLD.copy(id = "fold_hip", name = "Hip Mobility Flow")
    val LIGHT_WALK = CARRY.copy(id = "carry_walk", name = "Light Walk")
    val BICEPS_CURL = CURL.copy(id = "curl_biceps", name = "Biceps curl")

    /**
     * Complete list of all 28 exercise routines.
     */
    val ALL_28_MOVES: List<ExerciseMove> = listOf(
        BENCH,
        INCLINE,
        FLY,
        SKULL,
        PUSHDOWN,
        DEADLIFT,
        PULLUP,
        ROW,
        ICURL,
        CURL,
        SQUAT,
        RDL,
        LEGPRESS,
        LUNGE,
        CALF,
        FOLD,
        HIP_MOBILITY_FLOW,
        LIGHT_WALK,
        OHP,
        LATERAL,
        FACEPULL,
        LEGRAISE,
        CRUNCH,
        CLEAN,
        PUSHPRESS,
        FRONT,
        CARRY,
        REST
    )

    private val MOVES_BY_ID: Map<String, ExerciseMove> = buildMap {
        ALL_28_MOVES.forEach { move ->
            put(move.id.lowercase(), move)
        }
    }

    private val MOVES_BY_NAME: Map<String, ExerciseMove> = buildMap {
        // Exact 28 mappings from moves.ts BY_NAME
        put("Barbell Bench Press".lowercase(), BENCH)
        put("Incline Dumbbell Press".lowercase(), INCLINE)
        put("Cable Fly".lowercase(), FLY)
        put("Skull Crushers".lowercase(), SKULL)
        put("Rope Pushdown".lowercase(), PUSHDOWN)
        put("Deadlift".lowercase(), DEADLIFT)
        put("Weighted Pull-up".lowercase(), PULLUP)
        put("Barbell Row".lowercase(), ROW)
        put("Incline Curl".lowercase(), ICURL)
        put("Hammer Curl".lowercase(), CURL)
        put("Back Squat".lowercase(), SQUAT)
        put("Romanian Deadlift".lowercase(), RDL)
        put("Leg Press".lowercase(), LEGPRESS)
        put("Walking Lunge".lowercase(), LUNGE)
        put("Calf Raise".lowercase(), CALF)
        put("Foam Rolling".lowercase(), FOLD)
        put("Hip Mobility Flow".lowercase(), HIP_MOBILITY_FLOW)
        put("Light Walk".lowercase(), LIGHT_WALK)
        put("Overhead Press".lowercase(), OHP)
        put("Lateral Raise".lowercase(), LATERAL)
        put("Face Pull".lowercase(), FACEPULL)
        put("Hanging Leg Raise".lowercase(), LEGRAISE)
        put("Cable Crunch".lowercase(), CRUNCH)
        put("Power Clean".lowercase(), CLEAN)
        put("Push Press".lowercase(), PUSHPRESS)
        put("Front Squat".lowercase(), FRONT)
        put("Farmer Carry".lowercase(), CARRY)
        put("Sleep, eat, repeat".lowercase(), REST)

        // University Hostel Gains V-Taper aliases
        put("Wide-Grip Lat Pulldowns".lowercase(), PULLUP)
        put("Lat Pulldowns".lowercase(), PULLUP)
        put("Neutral-Grip Lat Pulldowns".lowercase(), PULLUP)
        put("Seated Cable Rows".lowercase(), ROW)
        put("Chest-Supported DB Rows".lowercase(), ROW)
        put("Machine Rear Delt Flyes".lowercase(), FACEPULL)
        put("Dumbbell Hammer Curls".lowercase(), CURL)
        put("Dumbbell Lateral Raises".lowercase(), LATERAL)
        put("Cable Lateral Raises".lowercase(), LATERAL)
        put("Incline Push-ups".lowercase(), BENCH)
        put("Tricep Rope Pushdowns".lowercase(), PUSHDOWN)
        put("Goblet Squats".lowercase(), SQUAT)
        put("Bodyweight Squats".lowercase(), SQUAT)
        put("Lying Leg Curls".lowercase(), RDL)
        put("Plank".lowercase(), CRUNCH)
        put("Preacher Curls".lowercase(), CURL)
        put("Standing Overhead DB Press".lowercase(), OHP)
        put("Flat Dumbbell Press".lowercase(), BENCH)
        put("Overhead Cable Tricep Extension".lowercase(), PUSHDOWN)
        put("Bodyweight Dips".lowercase(), PUSHDOWN)
        put("Active Recovery Walking".lowercase(), LIGHT_WALK)
    }

    /**
     * Resolves the appropriate ExerciseMove for an exercise name or routine ID.
     * Defaults safely to Back Squat if unrecognized.
     */
    fun moveFor(nameOrId: String): ExerciseMove {
        val key = nameOrId.trim().lowercase()
        return MOVES_BY_ID[key]
            ?: MOVES_BY_NAME[key]
            ?: MOVES_BY_NAME.entries.find { key.contains(it.key) || it.key.contains(key) }?.value
            ?: SQUAT
    }
}
