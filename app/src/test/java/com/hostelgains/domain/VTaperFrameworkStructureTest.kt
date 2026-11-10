package com.hostelgains.domain

import com.hostelgains.domain.workout.MuscleGroup
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Genuine automated unit tests verifying the structural integrity of the 7-day V-Taper PPL Framework.
 */
class VTaperFrameworkStructureTest {

    private lateinit var repository: VTaperWorkoutRepository

    @Before
    fun setUp() {
        repository = VTaperWorkoutRepository()
    }

    @Test
    fun getSchedule_containsAllSevenDays() {
        val allPlans = repository.getAllPlans()
        assertEquals(7, allPlans.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), allPlans.map { it.dayNumber })
    }

    @Test
    fun verifyDay1PullA_exercisesAndRepRanges() {
        val day1 = repository.getDayPlan(1)
        assertEquals("Day 1: Pull A", day1.dayName)
        assertEquals("Lat Width & Rear Delts", day1.focusArea)
        assertFalse(day1.isActiveRest)
        assertEquals(4, day1.exercises.size)

        val latPulldowns = day1.exercises[0]
        assertEquals("Wide-Grip Lat Pulldowns", latPulldowns.name)
        assertEquals(4, latPulldowns.baselineSets)
        assertEquals("8-12 reps", latPulldowns.repRange)
        assertEquals(90, latPulldowns.restSeconds)
        assertTrue(latPulldowns.isCompensatoryEligibleCarb)
        assertTrue(latPulldowns.formNotes.isNotEmpty())

        val cableRows = day1.exercises[1]
        assertEquals("Seated Cable Rows", cableRows.name)
        assertEquals(3, cableRows.baselineSets)
        assertEquals("8-12 reps", cableRows.repRange)
        assertFalse(cableRows.isCompensatoryEligibleCarb)

        val rearDeltFlyes = day1.exercises[2]
        assertEquals("Machine Rear Delt Flyes", rearDeltFlyes.name)
        assertEquals(4, rearDeltFlyes.baselineSets)
        assertEquals("10-15 reps", rearDeltFlyes.repRange)

        val hammerCurls = day1.exercises[3]
        assertEquals("Dumbbell Hammer Curls", hammerCurls.name)
        assertEquals(3, hammerCurls.baselineSets)
        assertEquals("8-12 reps", hammerCurls.repRange)
    }

    @Test
    fun verifyDay2PushA_exercisesAndRepRanges() {
        val day2 = repository.getDayPlan(2)
        assertEquals("Day 2: Push A", day2.dayName)
        assertEquals("Upper Chest & Lateral Delts", day2.focusArea)
        assertEquals(4, day2.exercises.size)

        val inclineDbPress = day2.exercises[0]
        assertEquals("Incline Dumbbell Press", inclineDbPress.name)
        assertEquals(4, inclineDbPress.baselineSets)
        assertEquals("8-12 reps", inclineDbPress.repRange)

        val latRaises = day2.exercises[1]
        assertEquals("Dumbbell Lateral Raises", latRaises.name)
        assertEquals(5, latRaises.baselineSets)
        assertEquals("10-15 reps", latRaises.repRange)

        val pushups = day2.exercises[2]
        assertEquals("Incline Push-ups", pushups.name)
        assertEquals(3, pushups.baselineSets)
        assertEquals("To failure", pushups.repRange)

        val tricepPushdowns = day2.exercises[3]
        assertEquals("Tricep Rope Pushdowns", tricepPushdowns.name)
        assertEquals(3, tricepPushdowns.baselineSets)
        assertEquals("8-12 reps", tricepPushdowns.repRange)
    }

    @Test
    fun verifyDay3LegsAndCore_exercisesAndRepRanges() {
        val day3 = repository.getDayPlan(3)
        assertEquals("Day 3: Legs & Core", day3.dayName)
        assertEquals("Quad Sweep & Core Stability", day3.focusArea)
        assertEquals(4, day3.exercises.size)

        val gobletSquats = day3.exercises[0]
        assertEquals("Goblet Squats", gobletSquats.name)
        assertEquals(4, gobletSquats.baselineSets)
        assertEquals("8-12 reps", gobletSquats.repRange)
        assertTrue(gobletSquats.isCompensatoryEligibleCarb)

        val bwSquats = day3.exercises[1]
        assertEquals("Bodyweight Squats", bwSquats.name)
        assertEquals(2, bwSquats.baselineSets)
        assertEquals("To failure", bwSquats.repRange)
        assertTrue(bwSquats.isCompensatoryEligibleCarb)

        val legCurls = day3.exercises[2]
        assertEquals("Lying Leg Curls", legCurls.name)
        assertEquals(3, legCurls.baselineSets)
        assertEquals("8-12 reps", legCurls.repRange)

        val plank = day3.exercises[3]
        assertEquals("Plank", plank.name)
        assertEquals(3, plank.baselineSets)
        assertEquals("60 sec", plank.repRange)
    }

    @Test
    fun verifyDay4PullB_horizontalPullsAndBicepsFocus() {
        val day4 = repository.getDayPlan(4)
        assertEquals("Day 4: Pull B", day4.dayName)
        assertEquals("Lat Thickness & Biceps Focus", day4.focusArea)
        assertTrue(day4.exercises.any { it.name == "Chest-Supported DB Rows" })
        assertTrue(day4.exercises.any { it.name == "Neutral-Grip Lat Pulldowns" })
        assertTrue(day4.exercises.any { it.name == "Incline Dumbbell Curls" })
        assertTrue(day4.exercises.any { it.name == "Preacher Curls" })
        assertTrue(day4.exercises.any { it.name == "Cable Face Pulls" })
    }

    @Test
    fun verifyDay5PushB_cableDeltsAndTricepsFocus() {
        val day5 = repository.getDayPlan(5)
        assertEquals("Day 5: Push B", day5.dayName)
        assertEquals("Cable Delts & Triceps Focus", day5.focusArea)
        assertTrue(day5.exercises.any { it.name == "Standing Overhead DB Press" })
        assertTrue(day5.exercises.any { it.name == "Cable Lateral Raises" })
        assertTrue(day5.exercises.any { it.name == "Flat Dumbbell Press" })
        assertTrue(day5.exercises.any { it.name == "Overhead Cable Tricep Extension" })
        assertTrue(day5.exercises.any { it.name == "Bodyweight Dips" })
    }

    @Test
    fun verifyDay6WeakPointPump_fst7Protocol() {
        val day6 = repository.getDayPlan(6)
        assertEquals("Day 6: Weak-Point Pump", day6.dayName)
        assertEquals(3, day6.exercises.size)

        // Lat Pulldowns: 7 sets of 14 reps, 45s rest
        val latPulldowns = day6.exercises[0]
        assertEquals("Lat Pulldowns", latPulldowns.name)
        assertEquals(7, latPulldowns.baselineSets)
        assertEquals("14 reps", latPulldowns.repRange)
        assertEquals(45, latPulldowns.restSeconds)
        assertTrue(latPulldowns.isCompensatoryEligibleCarb)
        assertTrue(latPulldowns.formNotes.contains("FST-7"))

        // Cable Lateral Raises: 5 sets of 12 reps, 45s rest
        val lateralRaises = day6.exercises[1]
        assertEquals("Cable Lateral Raises", lateralRaises.name)
        assertEquals(5, lateralRaises.baselineSets)
        assertEquals("12 reps", lateralRaises.repRange)
        assertEquals(45, lateralRaises.restSeconds)

        // Face Pulls: 3 sets of 15 reps, 60s rest
        val facePulls = day6.exercises[2]
        assertEquals("Face Pulls", facePulls.name)
        assertEquals(3, facePulls.baselineSets)
        assertEquals("15 reps", facePulls.repRange)
        assertEquals(60, facePulls.restSeconds)
    }

    @Test
    fun verifyDay7ActiveRest_stepsTarget() {
        val day7 = repository.getDayPlan(7)
        assertEquals("Day 7: Active Rest", day7.dayName)
        assertTrue(day7.isActiveRest)
        assertEquals("Active Rest & Recovery", day7.focusArea)
        assertEquals(1, day7.exercises.size)

        val walk = day7.exercises[0]
        assertEquals("Active Recovery Walking", walk.name)
        assertEquals(MuscleGroup.ACTIVE_RECOVERY, walk.targetMuscle)
        assertEquals("5,000 - 10,000 steps", walk.repRange)
        assertEquals(0, walk.restSeconds)
        assertEquals("5,000 - 10,000 steps only.", day7.guidanceNotes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDayPlan_outOfBoundsDay_throwsIllegalArgumentException() {
        repository.getDayPlan(8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getDayPlan_zeroDay_throwsIllegalArgumentException() {
        repository.getDayPlan(0)
    }
}
