package com.example.cipher.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates one EC keypair per calendar day, stored in Android Keystore.
 * Private keys never leave the device and are non-exportable.
 * When a new day's key is generated, the previous day's private key is deleted,
 * so messages encrypted to yesterday's public key can no longer be decrypted.
 */
object KeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS_PREFIX = "cipher_key_"
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun todayAlias(): String = ALIAS_PREFIX + dateFormat.format(Date())

    private fun hasTodayKeyPair(): Boolean = keyStore.containsAlias(todayAlias())

    /** Call on app start / after sign-in. Generates today's key if missing, and clears out old keys. */
    fun ensureTodayKeyPair() {
        if (!hasTodayKeyPair()) {
            generateKeyPair(todayAlias())
        }
        deleteOldKeyPairs()
    }

    private fun generateKeyPair(alias: String) {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .build()

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
        )
        generator.initialize(spec)
        generator.generateKeyPair()
    }

    /** Deletes every keypair whose alias isn't today's — old messages become unreadable. */
    private fun deleteOldKeyPairs() {
        val today = todayAlias()
        keyStore.aliases().toList()
            .filter { it.startsWith(ALIAS_PREFIX) && it != today }
            .forEach { keyStore.deleteEntry(it) }
    }

    /** Today's public key — share this so others can encrypt messages to you. */
    fun getPublicKey(): PublicKey? =
        keyStore.getCertificate(todayAlias())?.publicKey

    fun getPublicKeyBase64(): String? =
        getPublicKey()?.encoded?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
}