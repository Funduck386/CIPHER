package com.example.cipher

import kotlinx.serialization.Serializable

/**
 * Mirrors a row in the `public_keys` table.
 * user_id is filled server-side implicitly via auth.uid() in the insert's
 * RLS check, but Postgrest still needs it sent explicitly in the payload.
 */
@Serializable
data class PublicKeyRecord(
    val user_id: String,
    val public_key: String,
    val key_date: String
)

/** Used when looking up just the owner of a given public key. */
@Serializable
data class PublicKeyOwner(
    val user_id: String
)