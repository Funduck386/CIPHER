package com.example.cipher

import kotlinx.serialization.Serializable

/** Mirrors a row in the `messages` table, for sending. */
@Serializable
data class MessageInsert(
    val sender_id: String,
    val recipient_id: String,
    val encrypted_aes_key: String,
    val iv: String,
    val ciphertext: String
)

/** Mirrors a row in the `messages` table, for reading (includes server-generated fields). */
@Serializable
data class MessageRow(
    val id: String,
    val sender_id: String,
    val recipient_id: String,
    val encrypted_aes_key: String,
    val iv: String,
    val ciphertext: String,
    val created_at: String
)