package com.supe.assistant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBill: Boolean = false,
    val extractedAmount: Long? = null,
    val extractedDueDate: Long? = null,
    val isConvertedToReminder: Boolean = false
)
