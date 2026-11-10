package com.hostelgains.ui.kinematics

import kotlin.math.cos
import kotlin.math.sin

/**
 * Biomechanical forward kinematics solver calculating anatomical joint positions in 2D/3D space.
 * Follows planted feet origin at (0, 0), x forward, y upward, or hands on bar for hanging movements.
 */
class BiomechanicalKinematicsSolver : KinematicsSolver {

    companion object {
        // Segment lengths in arbitrary mannequin units
        const val L_THIGH = 70.0f
        const val L_SHIN = 68.0f
        const val L_TORSO = 84.0f
        const val L_UA = 48.0f
        const val L_FA = 44.0f
        const val L_FOOT = 8.0f

        const val BAR_Y = 330.0f

        fun rad(deg: Float): Float = (deg * Math.PI / 180.0).toFloat()

        fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        fun smoothstep(x: Float): Float {
            val clamped = x.coerceIn(0.0f, 1.0f)
            return clamped * clamped * (3.0f - 2.0f * clamped)
        }
    }

    override fun solve(pose: JointPose, move: ExerciseMove): SolvedSkeleton {
        val a1 = pose.hip
        val a2 = pose.hip - pose.knee

        val dx = L_THIGH * sin(rad(a1)) + L_SHIN * sin(rad(a2))
        val dyl = L_THIGH * cos(rad(a1)) + L_SHIN * cos(rad(a2))

        fun calculateArm(hx: Float, hy: Float): Pair<Point2D, Point2D> {
            val ex = hx + L_UA * sin(rad(pose.shoulder))
            val ey = hy - L_UA * cos(rad(pose.shoulder))
            val hxEnd = ex + L_FA * sin(rad(pose.shoulder + pose.elbow))
            val hyEnd = ey - L_FA * cos(rad(pose.shoulder + pose.elbow))
            return Pair(Point2D(ex, ey), Point2D(hxEnd, hyEnd))
        }

        var px = -dx
        var py = dyl + L_FOOT + pose.dy

        if (move.isHang) {
            val torsoTx = L_TORSO * sin(rad(pose.torso))
            val torsoTy = L_TORSO * cos(rad(pose.torso))
            val armRel = calculateArm(torsoTx, torsoTy)
            px = -armRel.second.x
            py = BAR_Y - armRel.second.y
        }

        val hip = Point2D(px, py)
        val knee = Point2D(
            px + L_THIGH * sin(rad(a1)),
            py - L_THIGH * cos(rad(a1))
        )
        val ankle = Point2D(
            knee.x + L_SHIN * sin(rad(a2)),
            knee.y - L_SHIN * cos(rad(a2))
        )
        val shoulder = Point2D(
            px + L_TORSO * sin(rad(pose.torso)),
            py + L_TORSO * cos(rad(pose.torso))
        )

        val (elbow, hand) = calculateArm(shoulder.x, shoulder.y)

        return SolvedSkeleton(
            hip = hip,
            knee = knee,
            ankle = ankle,
            shoulder = shoulder,
            elbow = elbow,
            hand = hand
        )
    }

    override fun solve(pose: JointPose, moveId: String): SolvedSkeleton {
        val move = ExerciseRoutines.moveFor(moveId)
        return solve(pose, move)
    }

    override fun calculatePhase(timeSeconds: Float, periodSeconds: Float): Float {
        val safePeriod = if (periodSeconds <= 0f) 3.0f else periodSeconds
        val u = (timeSeconds / safePeriod) % 1.0f
        val positiveU = if (u < 0f) u + 1.0f else u

        // Biomechanical rep cadence:
        // Eccentric (lowering): 0%..45%
        // Pause at stretch: 45%..52%
        // Concentric (lift): 52%..90%
        // Lockout at peak: 90%..100%
        return when {
            positiveU < 0.45f -> smoothstep(positiveU / 0.45f)
            positiveU < 0.52f -> 1.0f
            positiveU < 0.90f -> 1.0f - smoothstep((positiveU - 0.52f) / 0.38f)
            else -> 0.0f
        }
    }

    override fun interpolatePose(move: ExerciseMove, phase: Float): JointPose {
        val clampedPhase = phase.coerceIn(0.0f, 1.0f)
        val p1 = move.primaryPose
        val p2 = move.secondaryPose

        return JointPose(
            hip = lerp(p1.hip, p2.hip, clampedPhase),
            knee = lerp(p1.knee, p2.knee, clampedPhase),
            torso = lerp(p1.torso, p2.torso, clampedPhase),
            shoulder = lerp(p1.shoulder, p2.shoulder, clampedPhase),
            elbow = lerp(p1.elbow, p2.elbow, clampedPhase),
            abduction = lerp(p1.abduction, p2.abduction, clampedPhase),
            dy = lerp(p1.dy, p2.dy, clampedPhase),
            foot = lerp(p1.foot, p2.foot, clampedPhase)
        )
    }
}
