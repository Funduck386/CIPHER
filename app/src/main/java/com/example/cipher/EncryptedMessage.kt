package com.example.cipher

data class EncryptedMessage(
    val id: String,
    val senderPublicKeyBase64: String,
    val ciphertext: String,
    val receivedAt: String
)