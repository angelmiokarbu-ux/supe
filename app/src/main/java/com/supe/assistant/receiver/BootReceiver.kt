package com.supe.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.supe.assistant.data.local.SupeDatabaseHelper
import com.supe.assistant.service.ReminderNotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = SupeDatabaseHelper.getInstance(context)
            val reminders = db.getAllReminders()
            val now = System.currentTimeMillis()

            for (item in reminders) {
                if (!item.isCompleted && item.dueDate != null && item.dueDate > now) {
                    ReminderNotificationHelper.scheduleReminderAlarm(context, item)
                }
            }
        }
    }
}
