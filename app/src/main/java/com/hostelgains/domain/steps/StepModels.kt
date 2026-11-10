package com.hostelgains.domain.steps

/**
 * Daily step activity snapshot.
 */
data class StepStatus(
    val currentSteps: Int,
    val targetSteps: Int = 10000,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

/**
 * Result of the step deficit evaluation check.
 */
data class StepAlertResult(
    val isAlertTriggered: Boolean,
    val currentSteps: Int,
    val targetSteps: Int = 10000,
    val alertTitle: String?,
    val alertMessage: String?
)
