package com.hostelgains.data.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Manages notification channel registration and daily scheduled alarm triggers
 * for the 4 hostel mess meal intervals (9:00 AM, 1:45 PM, 5:40 PM, 8:45 PM).
 */
object MealReminderScheduler {

    const val CHANNEL_ID = "hostel_meal_reminders_channel"
    const val CHANNEL_NAME = "Hostel Meal Reminders"
    const val CHANNEL_DESC = "Notifications reminding students to log hostel meals after dining"

    const val EXTRA_SLOT_ID = "extra_slot_id"
    const val EXTRA_SLOT_TITLE = "extra_slot_title"
    const val EXTRA_SLOT_MESSAGE = "extra_slot_message"
    const val EXTRA_TARGET_TAB = "extra_target_tab"

    /**
     * Creates the Android 8.0+ notification channel with high importance, vibration, and lights.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules all 4 daily reminders using AlarmManager.
     */
    fun scheduleAllReminders(context: Context) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (slot in MealReminderSlot.ALL_SLOTS) {
            scheduleSlot(context, alarmManager, slot)
        }
    }

    /**
     * Schedules a single slot's next trigger time.
     */
    fun scheduleSlot(context: Context, alarmManager: AlarmManager, slot: MealReminderSlot) {
        val triggerAtMillis = calculateNextTriggerMillis(slot.hour, slot.minute)

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_ID, slot.id)
            putExtra(EXTRA_SLOT_TITLE, slot.title)
            putExtra(EXTRA_SLOT_MESSAGE, slot.message)
            putExtra(EXTRA_TARGET_TAB, slot.targetTab)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if SCHEDULE_EXACT_ALARM is restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Calculates the next epoch timestamp for a given hour and minute.
     * If the specified time has already elapsed today, returns the timestamp for tomorrow.
     */
    fun calculateNextTriggerMillis(
        hour: Int,
        minute: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= currentTimeMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    /**
     * Cancels all scheduled reminder alarms.
     */
    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (slot in MealReminderSlot.ALL_SLOTS) {
            val intent = Intent(context, MealReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                slot.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
