package com.example.cipher.crypto

import kotlinx.serialization.Serializable

/**
 * What actually gets stored/sent for one encrypted message.
 * - encryptedAesKey: a random AES key, encrypted with the RECIPIENT's RSA public key.
 *   Only their private key can recover it.
 * - iv: the AES-GCM initialization vector (not secret, but required to decrypt).
 * - ciphertext: the actual message text, encrypted with the AES key above.
 *
 * All three are Base64 strings so this can be stored as plain text columns
 * in Supabase and sent over JSON without extra encoding steps.
 */
@Serializable
data class EncryptedPayload(
    val encryptedAesKey: String,
    val iv: String,
    val ciphertext: String
)