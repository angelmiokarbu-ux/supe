package com.supe.assistant.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.supe.assistant.data.model.CapturedNotification
import com.supe.assistant.data.model.ChatMessage
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType

class SupeDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "supe_assistant.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_REMINDERS = "reminders"
        private const val TABLE_NOTIFICATIONS = "captured_notifications"
        private const val TABLE_CHAT = "chat_messages"

        @Volatile
        private var instance: SupeDatabaseHelper? = null

        fun getInstance(context: Context): SupeDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: SupeDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_REMINDERS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT,
                type TEXT NOT NULL,
                amount INTEGER,
                due_date INTEGER,
                created_at INTEGER,
                is_completed INTEGER DEFAULT 0,
                category TEXT,
                image_uri TEXT,
                audio_path TEXT,
                source_app TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                id TEXT PRIMARY KEY,
                package_name TEXT NOT NULL,
                app_name TEXT NOT NULL,
                title TEXT,
                text TEXT,
                timestamp INTEGER,
                is_bill INTEGER DEFAULT 0,
                extracted_amount INTEGER,
                extracted_due_date INTEGER,
                is_converted INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CHAT (
                id TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                is_from_user INTEGER NOT NULL,
                timestamp INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REMINDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT")
        onCreate(db)
    }

    // --- Reminders CRUD ---

    fun insertReminder(item: ReminderItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", item.id)
            put("title", item.title)
            put("content", item.content)
            put("type", item.type.name)
            put("amount", item.amount)
            put("due_date", item.dueDate)
            put("created_at", item.createdAt)
            put("is_completed", if (item.isCompleted) 1 else 0)
            put("category", item.category)
            put("image_uri", item.imageUri)
            put("audio_path", item.audioPath)
            put("source_app", item.sourceApp)
        }
        return db.insertWithOnConflict(TABLE_REMINDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllReminders(): List<ReminderItem> {
        val list = mutableListOf<ReminderItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_REMINDERS ORDER BY created_at DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                val typeStr = it.getString(it.getColumnIndexOrThrow("type"))
                val type = runCatching { ReminderType.valueOf(typeStr) }.getOrDefault(ReminderType.TEXT)
                val amountIndex = it.getColumnIndexOrThrow("amount")
                val dueDateIndex = it.getColumnIndexOrThrow("due_date")

                list.add(
                    ReminderItem(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        content = it.getString(it.getColumnIndexOrThrow("content")) ?: "",
                        type = type,
                        amount = if (it.isNull(amountIndex)) null else it.getLong(amountIndex),
                        dueDate = if (it.isNull(dueDateIndex)) null else it.getLong(dueDateIndex),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        isCompleted = it.getInt(it.getColumnIndexOrThrow("is_completed")) == 1,
                        category = it.getString(it.getColumnIndexOrThrow("category")) ?: "Catatan",
                        imageUri = it.getString(it.getColumnIndexOrThrow("image_uri")),
                        audioPath = it.getString(it.getColumnIndexOrThrow("audio_path")),
                        sourceApp = it.getString(it.getColumnIndexOrThrow("source_app"))
                    )
                )
            }
        }
        return list
    }

    fun updateReminderCompletion(id: String, isCompleted: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_completed", if (isCompleted) 1 else 0)
        }
        return db.update(TABLE_REMINDERS, values, "id = ?", arrayOf(id))
    }

    fun deleteReminder(id: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_REMINDERS, "id = ?", arrayOf(id))
    }

    // --- Captured Notifications CRUD ---

    fun insertNotification(item: CapturedNotification): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", item.id)
            put("package_name", item.packageName)
            put("app_name", item.appName)
            put("title", item.title)
            put("text", item.text)
            put("timestamp", item.timestamp)
            put("is_bill", if (item.isBill) 1 else 0)
            put("extracted_amount", item.extractedAmount)
            put("extracted_due_date", item.extractedDueDate)
            put("is_converted", if (item.isConvertedToReminder) 1 else 0)
        }
        return db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllNotifications(): List<CapturedNotification> {
        val list = mutableListOf<CapturedNotification>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NOTIFICATIONS ORDER BY timestamp DESC LIMIT 100", null)
        cursor.use {
            while (it.moveToNext()) {
                val amountIndex = it.getColumnIndexOrThrow("extracted_amount")
                val dueDateIndex = it.getColumnIndexOrThrow("extracted_due_date")

                list.add(
                    CapturedNotification(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        packageName = it.getString(it.getColumnIndexOrThrow("package_name")),
                        appName = it.getString(it.getColumnIndexOrThrow("app_name")),
                        title = it.getString(it.getColumnIndexOrThrow("title")) ?: "",
                        text = it.getString(it.getColumnIndexOrThrow("text")) ?: "",
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        isBill = it.getInt(it.getColumnIndexOrThrow("is_bill")) == 1,
                        extractedAmount = if (it.isNull(amountIndex)) null else it.getLong(amountIndex),
                        extractedDueDate = if (it.isNull(dueDateIndex)) null else it.getLong(dueDateIndex),
                        isConvertedToReminder = it.getInt(it.getColumnIndexOrThrow("is_converted")) == 1
                    )
                )
            }
        }
        return list
    }

    fun markNotificationConverted(id: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("is_converted", 1)
        }
        return db.update(TABLE_NOTIFICATIONS, values, "id = ?", arrayOf(id))
    }

    // --- Chat CRUD ---

    fun insertChatMessage(msg: ChatMessage): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", msg.id)
            put("text", msg.text)
            put("is_from_user", if (msg.isFromUser) 1 else 0)
            put("timestamp", msg.timestamp)
        }
        return db.insertWithOnConflict(TABLE_CHAT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllChatMessages(): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CHAT ORDER BY timestamp ASC LIMIT 200", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ChatMessage(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        text = it.getString(it.getColumnIndexOrThrow("text")),
                        isFromUser = it.getInt(it.getColumnIndexOrThrow("is_from_user")) == 1,
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }
}
