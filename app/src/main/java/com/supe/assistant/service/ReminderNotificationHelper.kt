package com.supe.assistant.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.supe.assistant.MainActivity
import com.supe.assistant.R
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.parser.BillExtractor
import com.supe.assistant.receiver.ReminderAlarmReceiver

object ReminderNotificationHelper {

    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_BILLS = "channel_bills"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
                enableVibration(true)
            }

            val billChannel = NotificationChannel(
                CHANNEL_BILLS,
                context.getString(R.string.notif_channel_bills),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_bills_desc)
                enableVibration(true)
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(billChannel)
        }
    }

    fun scheduleReminderAlarm(context: Context, item: ReminderItem) {
        val dueDate = item.dueDate ?: return
        if (dueDate <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.supe.assistant.ACTION_TRIGGER_REMINDER"
            putExtra("EXTRA_REMINDER_ID", item.id)
            putExtra("EXTRA_REMINDER_TITLE", item.title)
            putExtra("EXTRA_REMINDER_CONTENT", item.content)
            putExtra("EXTRA_REMINDER_AMOUNT", item.amount ?: 0L)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, dueDate, pendingIntent)
        }
    }

    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.supe.assistant.ACTION_TRIGGER_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun showReminderNotification(
        context: Context,
        id: String,
        title: String,
        content: String,
        amount: Long? = null
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_HIGHLIGHT_REMINDER_ID", id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (amount != null && amount > 0) {
            "Nominal: ${BillExtractor.formatRupiah(amount)}\n$content".trim()
        } else {
            content.ifEmpty { "Waktunya menyelesaikan pengingat Anda." }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_tile_mic)
            .setContentTitle("⏰ Supe: $title")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.hashCode(), builder.build())
    }

    fun showBillDetectedNotification(
        context: Context,
        title: String,
        vendor: String,
        amount: Long?
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", "bills")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = if (amount != null) "sebesar ${BillExtractor.formatRupiah(amount)}" else ""
        val text = "Terdeteksi notifikasi tagihan dari $vendor $amountStr. Otomatis dicatat ke Supe."

        val builder = NotificationCompat.Builder(context, CHANNEL_BILLS)
            .setSmallIcon(R.drawable.ic_tile_mic)
            .setContentTitle("Tagihan Baru Terdeteksi")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
