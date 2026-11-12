package com.hostelgains.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MealReminderSchedulerTest {

    @Test
    fun verifyAllFourHostelMealSlotsAreConfigured() {
        val slots = MealReminderSlot.ALL_SLOTS
        assertEquals(4, slots.size)

        val breakfast = slots.find { it.name == "Breakfast" }!!
        assertEquals(101, breakfast.id)
        assertEquals(9, breakfast.hour)
        assertEquals(0, breakfast.minute)
        assertTrue(breakfast.title.contains("breakfast", ignoreCase = true))

        val lunch = slots.find { it.name == "Lunch" }!!
        assertEquals(102, lunch.id)
        assertEquals(13, lunch.hour)
        assertEquals(45, lunch.minute)
        assertTrue(lunch.title.contains("lunch", ignoreCase = true))

        val snacks = slots.find { it.name == "Snacks" }!!
        assertEquals(103, snacks.id)
        assertEquals(17, snacks.hour)
        assertEquals(40, snacks.minute)
        assertTrue(snacks.title.contains("snacks", ignoreCase = true))

        val dinner = slots.find { it.name == "Dinner" }!!
        assertEquals(104, dinner.id)
        assertEquals(20, dinner.hour)
        assertEquals(45, dinner.minute)
        assertTrue(dinner.title.contains("dinner", ignoreCase = true))
    }

    @Test
    fun slotIdsAreDistinct() {
        val ids = MealReminderSlot.ALL_SLOTS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun calculateNextTriggerMillis_schedulesTodayWhenBeforeTargetTime() {
        val refCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMillis = refCalendar.timeInMillis

        // Target: 9:00 AM today
        val nextTrigger = MealReminderScheduler.calculateNextTriggerMillis(9, 0, currentMillis)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = nextTrigger }
        assertEquals(refCalendar.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(0, triggerCal.get(Calendar.SECOND))
        assertTrue(nextTrigger > currentMillis)
    }

    @Test
    fun calculateNextTriggerMillis_rollsOverToTomorrowWhenAfterTargetTime() {
        val refCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMillis = refCalendar.timeInMillis

        // Target: 9:00 AM (already passed at 9:15 AM)
        val nextTrigger = MealReminderScheduler.calculateNextTriggerMillis(9, 0, currentMillis)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = nextTrigger }
        assertEquals(refCalendar.get(Calendar.DAY_OF_YEAR) + 1, triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertTrue(nextTrigger > currentMillis)
    }

    @Test
    fun calculateNextTriggerMillis_allSlotsProduceValidFutureTimestamps() {
        val now = System.currentTimeMillis()
        val twentyFourHoursMillis = 24L * 60L * 60L * 1000L

        for (slot in MealReminderSlot.ALL_SLOTS) {
            val trigger = MealReminderScheduler.calculateNextTriggerMillis(slot.hour, slot.minute, now)
            assertTrue("Trigger for ${slot.name} should be in the future", trigger > now)
            assertTrue(
                "Trigger for ${slot.name} should be within next 24 hours",
                (trigger - now) <= twentyFourHoursMillis + 1000L
            )
        }
    }
}
