package com.example.cipher

data class ChatMessage(
    val id: String,
    val text: String,
    val isSentByMe: Boolean,
    val timestamp: String
)