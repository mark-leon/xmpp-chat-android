
package com.example.whatsappclone.model

data class ChatMessage(
    val sender: String,
    val message: String,
    val isOutgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
