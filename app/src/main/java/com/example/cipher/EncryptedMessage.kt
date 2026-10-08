package com.example.cipher

data class EncryptedMessage(
    val id: String,
    val senderId: String,
    val decryptedPreview: String,
    val receivedAt: String
)