package com.supe.assistant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
