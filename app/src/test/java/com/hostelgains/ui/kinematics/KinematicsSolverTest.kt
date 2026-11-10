package com.hostelgains.ui.kinematics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests verifying forward kinematics solver, joint angle rotations,
 * rep cadence interpolation, and all 28 exercise routine definitions.
 */
class KinematicsSolverTest {

    private lateinit var solver: BiomechanicalKinematicsSolver

    @Before
    fun setUp() {
        solver = BiomechanicalKinematicsSolver()
    }

    @Test
    fun testAll28ExerciseRoutinesAreDefinedAndDistinct() {
        val allMoves = ExerciseRoutines.ALL_28_MOVES
        assertEquals("Must contain exactly 28 unique exercise routines", 28, allMoves.size)

        val uniqueIds = allMoves.map { it.id }.toSet()
        assertEquals("All 28 moves must have unique identifiers", 28, uniqueIds.size)

        allMoves.forEach { move ->
            assertNotNull("Move ${move.id} must have a non-blank name", move.name)
            assertTrue("Move ${move.name} must have a non-empty name", move.name.isNotBlank())
            assertTrue("Move ${move.name} must specify form cues", move.formCues.isNotEmpty())
            assertTrue("Move ${move.name} must have a positive period", move.periodSeconds > 0f)
            assertNotNull("Move ${move.name} must have an apparatus type", move.apparatus)
        }
    }

    @Test
    fun testMoveForResolution_resolvesBothOriginalAndHostelAliases() {
        // Direct original names
        assertEquals("bench", ExerciseRoutines.moveFor("Barbell Bench Press").id)
        assertEquals("incline", ExerciseRoutines.moveFor("Incline Dumbbell Press").id)
        assertEquals("squat", ExerciseRoutines.moveFor("Back Squat").id)
        assertEquals("pullup", ExerciseRoutines.moveFor("Weighted Pull-up").id)
        assertEquals("row", ExerciseRoutines.moveFor("Barbell Row").id)
        assertEquals("curl", ExerciseRoutines.moveFor("Hammer Curl").id)
        assertEquals("lateral", ExerciseRoutines.moveFor("Lateral Raise").id)
        assertEquals("facepull", ExerciseRoutines.moveFor("Face Pull").id)
        assertEquals("rest", ExerciseRoutines.moveFor("Sleep, eat, repeat").id)

        // University Hostel Gains V-Taper aliases
        assertEquals("pullup", ExerciseRoutines.moveFor("Wide-Grip Lat Pulldowns").id)
        assertEquals("row", ExerciseRoutines.moveFor("Seated Cable Rows").id)
        assertEquals("squat", ExerciseRoutines.moveFor("Goblet Squats").id)
        assertEquals("squat", ExerciseRoutines.moveFor("Bodyweight Squats").id)
        assertEquals("bench", ExerciseRoutines.moveFor("Incline Push-ups").id)
        assertEquals("pushdown", ExerciseRoutines.moveFor("Tricep Rope Pushdowns").id)
        assertEquals("rdl", ExerciseRoutines.moveFor("Lying Leg Curls").id)
        assertEquals("crunch", ExerciseRoutines.moveFor("Plank").id)
    }

    @Test
    fun testForwardKinematics_standingUprightZeroPose() {
        val zeroPose = JointPose(hip = 0f, knee = 0f, torso = 0f, shoulder = 0f, elbow = 0f)
        val move = ExerciseRoutines.DEADLIFT
        val skeleton = solver.solve(zeroPose, move)

        // When standing upright, hip dx = 0, so hip.x == 0
        assertEquals(0f, skeleton.hip.x, 0.001f)

        // Hip height: L_thigh (70) + L_shin (68) + L_foot (8) = 146
        val expectedHipY = BiomechanicalKinematicsSolver.L_THIGH +
                BiomechanicalKinematicsSolver.L_SHIN +
                BiomechanicalKinematicsSolver.L_FOOT
        assertEquals(expectedHipY, skeleton.hip.y, 0.001f)

        // Torso upright (torso = 0): shoulder is directly above hip
        assertEquals(skeleton.hip.x, skeleton.shoulder.x, 0.001f)
        assertEquals(skeleton.hip.y + BiomechanicalKinematicsSolver.L_TORSO, skeleton.shoulder.y, 0.001f)

        // Arms hanging straight down (shoulder = 0): elbow directly below shoulder
        assertEquals(skeleton.shoulder.x, skeleton.elbow.x, 0.001f)
        assertEquals(skeleton.shoulder.y - BiomechanicalKinematicsSolver.L_UA, skeleton.elbow.y, 0.001f)

        // Hand directly below elbow (elbow = 0)
        assertEquals(skeleton.elbow.x, skeleton.hand.x, 0.001f)
        assertEquals(skeleton.elbow.y - BiomechanicalKinematicsSolver.L_FA, skeleton.hand.y, 0.001f)
    }

    @Test
    fun testForwardKinematics_hangingMovePositionsPelvisFromBar() {
        val hangPose = JointPose(hip = 0f, knee = 60f, torso = 0f, shoulder = 180f, elbow = 0f)
        val pullupMove = ExerciseRoutines.PULLUP
        assertTrue("Pull-up must be marked as hanging", pullupMove.isHang)

        val skeleton = solver.solve(hangPose, pullupMove)

        // In hanging position, the hands must be anchored at the bar height (BAR_Y = 330)
        assertEquals(BiomechanicalKinematicsSolver.BAR_Y, skeleton.hand.y, 0.001f)

        // Pelvis height must be strictly below bar height
        assertTrue("Pelvis must be below pull-up bar", skeleton.hip.y < BiomechanicalKinematicsSolver.BAR_Y)

        // Shoulders must be below hands when hanging
        assertTrue("Shoulders must be below bar when hanging", skeleton.shoulder.y < skeleton.hand.y)
    }

    @Test
    fun testRepCadencePhaseCalculation_smoothstepBiomechanicalTiming() {
        val period = 3.0f

        // At t = 0, phase = 0.0 (starting position)
        assertEquals(0.0f, solver.calculatePhase(0.0f, period), 0.001f)

        // Mid-eccentric lowering: 0 < phase < 1
        val midEccentric = solver.calculatePhase(0.7f, period)
        assertTrue("Mid-eccentric phase must be between 0 and 1", midEccentric in 0.01f..0.99f)

        // Isometric pause at stretch (between 45% and 52% of rep, e.g. t = 1.45s)
        val pausePhase = solver.calculatePhase(1.45f, period)
        assertEquals("Pause phase at peak stretch must be 1.0", 1.0f, pausePhase, 0.001f)

        // Concentric lift: decreasing back from 1.0 towards 0.0
        val concentricPhase = solver.calculatePhase(2.1f, period)
        assertTrue("Concentric phase must decrease toward lock", concentricPhase in 0.01f..0.99f)

        // Lockout at end of rep: phase = 0.0
        val lockoutPhase = solver.calculatePhase(2.85f, period)
        assertEquals("Lockout at end of rep must be 0.0", 0.0f, lockoutPhase, 0.001f)
    }

    @Test
    fun testJointPoseInterpolation_lerpBetweenPrimaryAndSecondary() {
        val squat = ExerciseRoutines.SQUAT
        val pStart = solver.interpolatePose(squat, 0.0f)
        val pMid = solver.interpolatePose(squat, 0.5f)
        val pEnd = solver.interpolatePose(squat, 1.0f)

        assertEquals(squat.primaryPose.hip, pStart.hip, 0.001f)
        assertEquals(squat.secondaryPose.hip, pEnd.hip, 0.001f)

        val expectedMidHip = (squat.primaryPose.hip + squat.secondaryPose.hip) * 0.5f
        assertEquals(expectedMidHip, pMid.hip, 0.001f)
    }

    @Test
    fun testMatrix4x4_identityAndAffineTransformations() {
        val identity = Matrix4x4.identity()
        val pt = Point3D(10f, 20f, 30f)
        val ptTransformed = identity.transformPoint(pt)

        assertEquals(10f, ptTransformed.x, 0.001f)
        assertEquals(20f, ptTransformed.y, 0.001f)
        assertEquals(30f, ptTransformed.z, 0.001f)

        // Translation
        val trans = Matrix4x4.translation(5f, -10f, 15f)
        val ptMoved = trans.transformPoint(pt)
        assertEquals(15f, ptMoved.x, 0.001f)
        assertEquals(10f, ptMoved.y, 0.001f)
        assertEquals(45f, ptMoved.z, 0.001f)

        // Rotation 90 degrees around Z axis: (x, y) -> (-y, x)
        val rotZ90 = Matrix4x4.rotationZ(90f)
        val ptRotated = rotZ90.transformPoint(Point3D(1f, 0f, 0f))
        assertEquals(0f, ptRotated.x, 0.001f)
        assertEquals(1f, ptRotated.y, 0.001f)
        assertEquals(0f, ptRotated.z, 0.001f)
    }
}
