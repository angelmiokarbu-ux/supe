package com.supe.assistant.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderType {
    TEXT,
    VOICE,
    CAMERA,
    IMAGE,
    BILL_NOTIFICATION
}

@Serializable
data class ReminderItem(
    val id: String,
    val title: String,
    val content: String = "",
    val type: ReminderType = ReminderType.TEXT,
    val amount: Long? = null,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val category: String = "Catatan",
    val imageUri: String? = null,
    val audioPath: String? = null,
    val sourceApp: String? = null
)
