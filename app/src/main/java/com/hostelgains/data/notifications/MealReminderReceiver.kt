package com.hostelgains.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hostelgains.app.R
import com.hostelgains.ui.MainActivity

/**
 * BroadcastReceiver triggered by AlarmManager at scheduled meal times.
 * Dispatches an athletic push notification and schedules the slot for the next day.
 */
class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getIntExtra(MealReminderScheduler.EXTRA_SLOT_ID, 101)
        val title = intent.getStringExtra(MealReminderScheduler.EXTRA_SLOT_TITLE)
            ?: "Hostel Meal Check-in 🍽️"
        val message = intent.getStringExtra(MealReminderScheduler.EXTRA_SLOT_MESSAGE)
            ?: "Time to log your meal and lock in today's macros!"
        val targetTab = intent.getStringExtra(MealReminderScheduler.EXTRA_TARGET_TAB)
            ?: "MEALS"

        // Ensure notification channel exists
        MealReminderScheduler.createNotificationChannel(context)

        // Tapping notification launches MainActivity directly into requested tab (MEALS or WORKOUT)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", targetTab)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            slotId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MealReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFC6FF3D.toInt()) // Volt accent brand color
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(slotId, notification)
        } catch (e: SecurityException) {
            // Notification permission might be revoked by user
        }

        // Reschedule this slot for the next day
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val slot = MealReminderSlot.ALL_SLOTS.find { it.id == slotId }
        if (alarmManager != null && slot != null) {
            MealReminderScheduler.scheduleSlot(context, alarmManager, slot)
        }
    }
}
