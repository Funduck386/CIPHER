package com.example.cipher

data class KeyRequest(
    val id: String,
    val fromContactId: String,
    val fromDisplayName: String,
    val requestedAt: String
)