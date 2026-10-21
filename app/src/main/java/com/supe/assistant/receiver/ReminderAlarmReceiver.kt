package com.supe.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.supe.assistant.service.ReminderNotificationHelper

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("EXTRA_REMINDER_ID") ?: return
        val title = intent.getStringExtra("EXTRA_REMINDER_TITLE") ?: "Pengingat Supe"
        val content = intent.getStringExtra("EXTRA_REMINDER_CONTENT") ?: ""
        val amount = intent.getLongExtra("EXTRA_REMINDER_AMOUNT", 0L).takeIf { it > 0 }

        ReminderNotificationHelper.showReminderNotification(
            context = context,
            id = id,
            title = title,
            content = content,
            amount = amount
        )
    }
}
