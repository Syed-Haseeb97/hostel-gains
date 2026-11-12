package com.hostelgains.data.notifications

import java.util.Calendar

/**
 * Definition of a daily scheduled hostel meal reminder slot.
 */
data class MealReminderSlot(
    val id: Int,
    val name: String,
    val hour: Int,
    val minute: Int,
    val title: String,
    val message: String,
    val targetTab: String = "MEALS"
) {
    companion object {
        val BREAKFAST = MealReminderSlot(
            id = 101,
            name = "Breakfast",
            hour = 9,
            minute = 0,
            title = "Done with breakfast? 🍳",
            message = "Enter what you ate to keep a record and lock in your morning protein!",
            targetTab = "MEALS"
        )

        val LUNCH = MealReminderSlot(
            id = 102,
            name = "Lunch",
            hour = 13,
            minute = 45,
            title = "Finished hostel lunch? 🍛",
            message = "Log your mess food so the dynamic optimizer can calibrate today's V-Taper workout!",
            targetTab = "MEALS"
        )

        val SNACKS = MealReminderSlot(
            id = 103,
            name = "Snacks",
            hour = 17,
            minute = 40,
            title = "Grabbed evening snacks? 🍌",
            message = "Log what you ate before your workout to optimize glycogen utilization!",
            targetTab = "MEALS"
        )

        val GYM_MOTIVATION = MealReminderSlot(
            id = 105,
            name = "GymMotivation",
            hour = 19,
            minute = 45,
            title = "She left you? 💔 Make her regret it. 🗿",
            message = "Still rotting on the hostel bed? Heartbreak is temporary, V-Taper is forever. Hit the gym and start your villain arc! 😤💥",
            targetTab = "WORKOUT"
        )

        val DINNER = MealReminderSlot(
            id = 104,
            name = "Dinner",
            hour = 20,
            minute = 45,
            title = "Done with dinner? 🍗",
            message = "Finalize today's meal log to calculate daily macros and set tomorrow's recovery plan!",
            targetTab = "MEALS"
        )

        val ALL_SLOTS = listOf(BREAKFAST, LUNCH, SNACKS, GYM_MOTIVATION, DINNER)
    }
}
