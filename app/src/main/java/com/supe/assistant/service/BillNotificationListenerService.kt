package com.supe.assistant.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.supe.assistant.data.local.SupeDatabaseHelper
import com.supe.assistant.data.model.CapturedNotification
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType
import com.supe.assistant.data.parser.BillExtractor
import java.util.UUID

class BillNotificationListenerService : NotificationListenerService() {

    companion object {
        const val ACTION_NOTIFICATION_CAPTURED = "com.supe.assistant.ACTION_NOTIFICATION_CAPTURED"

        fun isNotificationAccessGranted(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName
        if (pkgName == packageName) return // Hindari loop membaca notifikasi diri sendiri

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkgName
        }

        val extracted = BillExtractor.parse(text, title)
        val notifId = UUID.randomUUID().toString()

        val captured = CapturedNotification(
            id = notifId,
            packageName = pkgName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            isBill = extracted.isBill,
            extractedAmount = extracted.amount,
            extractedDueDate = extracted.dueDate,
            isConvertedToReminder = false
        )

        val db = SupeDatabaseHelper.getInstance(this)
        db.insertNotification(captured)

        // Jika terdeteksi sebagai tagihan riil, otomatis buatkan ReminderItem
        if (extracted.isBill && (extracted.amount != null || extracted.dueDate != null)) {
            val reminderTitle = extracted.vendorOrTitle ?: title.ifEmpty { "Tagihan Baru ($appName)" }
            val reminderItem = ReminderItem(
                id = UUID.randomUUID().toString(),
                title = reminderTitle,
                content = text,
                type = ReminderType.BILL_NOTIFICATION,
                amount = extracted.amount,
                dueDate = extracted.dueDate ?: (System.currentTimeMillis() + 86400000L), // Default 1 hari jika tanggal tidak eksplisit
                createdAt = System.currentTimeMillis(),
                isCompleted = false,
                category = "Tagihan",
                sourceApp = appName
            )
            db.insertReminder(reminderItem)
            db.markNotificationConverted(notifId)

            ReminderNotificationHelper.scheduleReminderAlarm(this, reminderItem)
            ReminderNotificationHelper.showBillDetectedNotification(
                context = this,
                title = reminderTitle,
                vendor = appName,
                amount = extracted.amount
            )
        }

        // Kirim broadcast internal agar UI reaktif terbarui
        val broadcast = Intent(ACTION_NOTIFICATION_CAPTURED).apply {
            setPackage(packageName)
        }
        sendBroadcast(broadcast)
    }
}
