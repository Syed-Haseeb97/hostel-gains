package com.hostelgains.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Automatically restores all 4 daily meal reminder alarms when the device reboots
 * or when the application is updated.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MealReminderScheduler.scheduleAllReminders(context.applicationContext)
        }
    }
}
