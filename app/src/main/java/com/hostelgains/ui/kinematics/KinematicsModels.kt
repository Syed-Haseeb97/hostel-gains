package com.hostelgains.ui.kinematics

/**
 * Joint angle configuration representing biomechanical posture at a given instant.
 * All angular values in degrees.
 */
data class JointPose(
    val hip: Float = 0f,
    val knee: Float = 0f,
    val torso: Float = 0f,
    val shoulder: Float = 0f,
    val elbow: Float = 0f,
    val abduction: Float = 0f,
    val dy: Float = 0f,
    val foot: Float = 0f
) {
    companion object {
        val ZERO = JointPose()
    }
}

/**
 * 2D projected coordinate on the sagittal/frontal view plane.
 */
data class Point2D(val x: Float, val y: Float)

/**
 * 3D point in world coordinate space.
 */
data class Point3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Point3D): Point3D = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D): Point3D = Point3D(x - other.x, y - other.y, z - other.z)
}

/**
 * Solved 2D joint coordinates from forward kinematics.
 */
data class SolvedSkeleton(
    val hip: Point2D,
    val knee: Point2D,
    val ankle: Point2D,
    val shoulder: Point2D,
    val elbow: Point2D,
    val hand: Point2D
)

/**
 * Highlighting group for anatomical muscle targeting.
 */
enum class MuscleHighlightGroup {
    ARMS,
    LEGS,
    TORSO
}

/**
 * Apparatus used in the exercise movement.
 */
enum class ApparatusType {
    BARBELL,
    DUMBBELL,
    BAR,
    NONE
}

/**
 * 3D exercise movement routine ported from biomechanical specification.
 */
data class ExerciseMove(
    val id: String,
    val name: String,
    val targetMuscles: List<String>,
    val primaryPose: JointPose,
    val secondaryPose: JointPose,
    val apparatus: ApparatusType,
    val formCues: List<String>,
    val isBench: Boolean = false,
    val isHang: Boolean = false,
    val highlightedGroups: List<MuscleHighlightGroup> = emptyList(),
    val cameraOffsetX: Float = 0f,
    val periodSeconds: Float = 3.0f
)

/**
 * Forward kinematics solver interface.
 */
interface KinematicsSolver {
    /**
     * Solves forward kinematics for a given joint pose and exercise configuration.
     */
    fun solve(pose: JointPose, move: ExerciseMove): SolvedSkeleton

    /**
     * Solves forward kinematics by move ID.
     */
    fun solve(pose: JointPose, moveId: String): SolvedSkeleton

    /**
     * Calculates the animated phase [0.0..1.0] from clock time and repetition period.
     */
    fun calculatePhase(timeSeconds: Float, periodSeconds: Float): Float

    /**
     * Biomechanically interpolates between primary (start) and secondary (peak) poses.
     */
    fun interpolatePose(move: ExerciseMove, phase: Float): JointPose
}
