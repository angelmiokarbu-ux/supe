package com.supe.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.supe.assistant.MainActivity
import com.supe.assistant.R
import com.supe.assistant.data.local.SupeDatabaseHelper
import com.supe.assistant.data.parser.BillExtractor

class QuickVoiceWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuickVoiceWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, QuickVoiceWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = SupeDatabaseHelper.getInstance(context)
        val allReminders = db.getAllReminders()
        val active = allReminders.filter { !it.isCompleted }

        val statusText = if (active.isEmpty()) {
            "Semua pengingat selesai"
        } else {
            "${active.size} Pengingat Aktif"
        }

        val nearest = active.minByOrNull { it.dueDate ?: Long.MAX_VALUE }
        val summaryText = if (nearest != null) {
            val amt = if (nearest.amount != null) " (${BillExtractor.formatRupiah(nearest.amount)})" else ""
            "Berikutnya: ${nearest.title}$amt"
        } else {
            "Ketuk tombol di bawah untuk input pengingat cepat"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_voice)
            views.setTextViewText(R.id.tv_widget_status, statusText)
            views.setTextViewText(R.id.tv_widget_summary, summaryText)

            // Intent Root App
            val rootIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val rootPendingIntent = PendingIntent.getActivity(
                context,
                0,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

            // Intent Mic
            val micIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.supe.assistant.ACTION_QUICK_VOICE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val micPendingIntent = PendingIntent.getActivity(
                context,
                1,
                micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_mic, micPendingIntent)

            // Intent Note
            val noteIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.supe.assistant.ACTION_QUICK_NOTE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val notePendingIntent = PendingIntent.getActivity(
                context,
                2,
                noteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_note, notePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
