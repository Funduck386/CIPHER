package com.example.cipher

/**
 * A chat contact and the public key we currently trust for them.
 * keyDate is the calendar day (yyyyMMdd) that publicKeyBase64 was entered/confirmed.
 * Since keys rotate daily, a keyDate that isn't today means the key is stale.
 */
data class Contact(
    val id: String,
    val displayName: String,
    val publicKeyBase64: String,
    val keyDate: String
)