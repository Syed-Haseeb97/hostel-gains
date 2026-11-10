package com.hostelgains.domain.steps

/**
 * Interface contract for step counter sync and alert evaluation.
 */
interface StepAlertEvaluator {
    fun evaluate(currentSteps: Int, hourOfDay: Int, minuteOfHour: Int = 0): StepAlertResult
}

/**
 * Genuine implementation of StepAlertEvaluator.
 *
 * Rule: Monitor daily steps and trigger a UI alert if the user is under 5,000 steps by 6 PM (18:00),
 * prompting a 10,000-step walk after hostel dinner to maintain insulin sensitivity and body recomposition.
 */
class DefaultStepAlertEvaluator : StepAlertEvaluator {

    companion object {
        const val STEP_DEFICIT_THRESHOLD = 5000
        const val TARGET_DAILY_STEPS = 10000
        const val EVALUATION_CUTOFF_HOUR = 18 // 6:00 PM in 24-hour time
    }

    override fun evaluate(currentSteps: Int, hourOfDay: Int, minuteOfHour: Int): StepAlertResult {
        val isAtOrAfterCutoff = hourOfDay >= EVALUATION_CUTOFF_HOUR
        val isUnderThreshold = currentSteps < STEP_DEFICIT_THRESHOLD

        val isTriggered = isAtOrAfterCutoff && isUnderThreshold

        return if (isTriggered) {
            StepAlertResult(
                isAlertTriggered = true,
                currentSteps = currentSteps,
                targetSteps = TARGET_DAILY_STEPS,
                alertTitle = "Step Deficit Alert (Sub-5,000 at 6:00 PM)",
                alertMessage = "You have logged only $currentSteps steps today. To stay on track for body recomposition and insulin sensitivity, take a 10,000-step walk after your hostel dinner."
            )
        } else {
            StepAlertResult(
                isAlertTriggered = false,
                currentSteps = currentSteps,
                targetSteps = TARGET_DAILY_STEPS,
                alertTitle = null,
                alertMessage = null
            )
        }
    }
}
