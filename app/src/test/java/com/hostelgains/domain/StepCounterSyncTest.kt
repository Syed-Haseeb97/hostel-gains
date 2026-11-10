package com.hostelgains.domain

import com.hostelgains.domain.steps.DefaultStepAlertEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Genuine automated unit tests verifying Step Counter Sync and 6:00 PM alert logic.
 */
class StepCounterSyncTest {

    private lateinit var evaluator: DefaultStepAlertEvaluator

    @Before
    fun setUp() {
        evaluator = DefaultStepAlertEvaluator()
    }

    @Test
    fun evaluateStepAlert_steps4000_time1759_returnsNoAlert() {
        // At 17:59 (5:59 PM), even with under 5,000 steps, cutoff time (18:00) has not arrived
        val result = evaluator.evaluate(currentSteps = 4000, hourOfDay = 17, minuteOfHour = 59)

        assertFalse("Alert should not trigger prior to 6:00 PM", result.isAlertTriggered)
        assertNull(result.alertTitle)
        assertNull(result.alertMessage)
        assertEquals(4000, result.currentSteps)
    }

    @Test
    fun evaluateStepAlert_steps4999_time1800_triggersAlert() {
        // At 18:00 (6:00 PM) with 4,999 steps, exactly 1 step under threshold -> Alert Triggers
        val result = evaluator.evaluate(currentSteps = 4999, hourOfDay = 18, minuteOfHour = 0)

        assertTrue("Alert must trigger at 6:00 PM when steps are under 5,000", result.isAlertTriggered)
        val title = result.alertTitle
        val message = result.alertMessage
        requireNotNull(title)
        requireNotNull(message)
        assertTrue(title.contains("Step Deficit"))
        assertTrue(message.contains("10,000-step"))
        assertEquals(10000, result.targetSteps)
    }

    @Test
    fun evaluateStepAlert_steps5000_time1800_returnsNoAlert() {
        // At 18:00 with exactly 5,000 steps, user has satisfied the threshold -> No Alert
        val result = evaluator.evaluate(currentSteps = 5000, hourOfDay = 18, minuteOfHour = 0)

        assertFalse("Alert should not trigger when steps equal or exceed 5,000", result.isAlertTriggered)
        assertNull(result.alertTitle)
        assertNull(result.alertMessage)
    }

    @Test
    fun evaluateStepAlert_steps3500_time2030_triggersAlert() {
        // At 20:30 (8:30 PM post-dinner) with 3,500 steps -> Alert persists to prompt walking
        val result = evaluator.evaluate(currentSteps = 3500, hourOfDay = 20, minuteOfHour = 30)

        assertTrue(result.isAlertTriggered)
        val message = result.alertMessage
        assertNotNull(message)
        requireNotNull(message)
        assertTrue(message.contains("3500"))
        assertTrue(message.contains("10,000-step"))
    }

    @Test
    fun evaluateStepAlert_steps10000_time2100_returnsNoAlert() {
        // Goal achieved (10,000 steps) -> No alert
        val result = evaluator.evaluate(currentSteps = 10000, hourOfDay = 21, minuteOfHour = 0)

        assertFalse(result.isAlertTriggered)
        assertNull(result.alertTitle)
    }
}
