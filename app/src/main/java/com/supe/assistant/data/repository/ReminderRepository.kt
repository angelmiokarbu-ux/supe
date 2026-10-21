package com.supe.assistant.data.repository

import android.content.Context
import com.supe.assistant.data.local.SupeDatabaseHelper
import com.supe.assistant.data.model.CapturedNotification
import com.supe.assistant.data.model.ChatMessage
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType
import com.supe.assistant.service.ReminderNotificationHelper
import com.supe.assistant.widget.QuickVoiceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class ReminderRepository(private val context: Context) {

    private val db = SupeDatabaseHelper.getInstance(context)

    private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadAll() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            _reminders.value = db.getAllReminders()
            _notifications.value = db.getAllNotifications()
            val savedChats = db.getAllChatMessages()
            if (savedChats.isEmpty()) {
                val initialGreeting = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Halo! Saya Supe, asisten pengingat dan catatan Anda. Tanyakan apa saja tentang tagihan, jadwal, atau catatan yang tersimpan.",
                    isFromUser = false
                )
                db.insertChatMessage(initialGreeting)
                _chatMessages.value = listOf(initialGreeting)
            } else {
                _chatMessages.value = savedChats
            }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun addReminder(item: ReminderItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.insertReminder(item)
            _reminders.value = db.getAllReminders()

            if (item.dueDate != null && item.dueDate > System.currentTimeMillis()) {
                ReminderNotificationHelper.scheduleReminderAlarm(context, item)
            }
            QuickVoiceWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun toggleReminderCompletion(id: String, isCompleted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.updateReminderCompletion(id, isCompleted)
            _reminders.value = db.getAllReminders()
            if (isCompleted) {
                ReminderNotificationHelper.cancelReminderAlarm(context, id)
            }
            QuickVoiceWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun deleteReminder(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.deleteReminder(id)
            ReminderNotificationHelper.cancelReminderAlarm(context, id)
            _reminders.value = db.getAllReminders()
            QuickVoiceWidgetProvider.updateAllWidgets(context)
        }
    }

    suspend fun addChatMessage(text: String, isFromUser: Boolean): Result<ChatMessage> = withContext(Dispatchers.IO) {
        runCatching {
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isFromUser = isFromUser
            )
            db.insertChatMessage(msg)
            _chatMessages.value = db.getAllChatMessages()
            msg
        }
    }

    suspend fun convertNotificationToReminder(notification: CapturedNotification): Result<ReminderItem> = withContext(Dispatchers.IO) {
        runCatching {
            val reminder = ReminderItem(
                id = UUID.randomUUID().toString(),
                title = notification.title.ifEmpty { "Tagihan ${notification.appName}" },
                content = notification.text,
                type = ReminderType.BILL_NOTIFICATION,
                amount = notification.extractedAmount,
                dueDate = notification.extractedDueDate ?: (System.currentTimeMillis() + 86400000L),
                category = "Tagihan",
                sourceApp = notification.appName
            )
            db.insertReminder(reminder)
            db.markNotificationConverted(notification.id)

            _reminders.value = db.getAllReminders()
            _notifications.value = db.getAllNotifications()

            ReminderNotificationHelper.scheduleReminderAlarm(context, reminder)
            QuickVoiceWidgetProvider.updateAllWidgets(context)
            reminder
        }
    }
}
