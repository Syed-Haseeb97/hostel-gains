package com.hostelgains.domain.physique

import com.hostelgains.domain.workout.DynamicWorkoutRepository
import com.hostelgains.domain.workout.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DynamicWorkoutRepositoryTest {

    private lateinit var repository: DynamicWorkoutRepository

    @Before
    fun setUp() {
        repository = DynamicWorkoutRepository()
    }

    @Test
    fun allPhysiqueBuildsHaveSevenDaySchedules() {
        for (build in PhysiqueBuild.ALL_BUILDS) {
            val plans = repository.getAllPlans(build)
            assertEquals("Build ${build.name} must have 7 days", 7, plans.size)
            assertTrue("Day 7 of ${build.name} must be active recovery", plans[6].isActiveRest)
        }
    }

    @Test
    fun vTaperBuild_prioritizesHypertrophyAndLatDelts() {
        val day1 = repository.getDayPlan(1, PhysiqueBuild.V_TAPER)
        assertTrue(day1.focusArea.contains("Lat Width", ignoreCase = true))
        val latPulldown = day1.exercises.find { it.id == "d1_lat_pulldown" }!!
        assertEquals("8-12 reps", latPulldown.repRange)

        val day2 = repository.getDayPlan(2, PhysiqueBuild.V_TAPER)
        val latRaises = day2.exercises.find { it.id == "d2_db_lateral_raises" }!!
        assertEquals(5, latRaises.baselineSets)
        assertEquals("10-15 reps", latRaises.repRange)
    }

    @Test
    fun tankBuild_prioritizesHeavyStrengthAndDenseBack() {
        val day1 = repository.getDayPlan(1, PhysiqueBuild.TANK)
        assertTrue(day1.focusArea.contains("Deadlifts", ignoreCase = true) || day1.focusArea.contains("Back Thickness", ignoreCase = true))
        val deadlift = day1.exercises.find { it.id == "t1_deadlift" }!!
        assertEquals("4-6 reps", deadlift.repRange)
        assertEquals(120, deadlift.restSeconds)

        val day3 = repository.getDayPlan(3, PhysiqueBuild.TANK)
        val squat = day3.exercises.find { it.id == "t3_bb_squat" }!!
        assertEquals("4-6 reps", squat.repRange)
    }

    @Test
    fun athleteBuild_prioritizesPlyometricsAndHighConditioning() {
        val day1 = repository.getDayPlan(1, PhysiqueBuild.ATHLETE)
        val pullups = day1.exercises.find { it.id == "a1_plyo_pullups" }!!
        assertEquals("10-12 reps", pullups.repRange)

        val day2 = repository.getDayPlan(2, PhysiqueBuild.ATHLETE)
        val plyoPushups = day2.exercises.find { it.id == "a2_plyo_pushups" }!!
        assertTrue(plyoPushups.name.contains("Plyo", ignoreCase = true))

        val day3 = repository.getDayPlan(3, PhysiqueBuild.ATHLETE)
        val bulgarian = day3.exercises.find { it.id == "a3_bulgarian_squat" }!!
        assertEquals(MuscleGroup.QUADS, bulgarian.targetMuscle)
    }

    @Test
    fun xFrameBuild_modifiesLegDayForOuterQuadSweep() {
        val day3 = repository.getDayPlan(3, PhysiqueBuild.X_FRAME)
        assertTrue(day3.focusArea.contains("Sweep", ignoreCase = true))
        val squats = day3.exercises.find { it.id == "x3_elevated_squats" }!!
        assertTrue(squats.formNotes.contains("outer quad sweep", ignoreCase = true))
    }
}
