package com.example.cipher.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey

/**
 * Generates and stores this device's keypair in Android Keystore.
 * The private key is non-exportable — it never leaves secure hardware/OS storage.
 * Only the public key is meant to be uploaded to the backend.
 */
object KeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "cipher_user_key"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    fun hasKeyPair(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    /** Call once, right after a new user signs up. Does nothing if a key already exists. */
    fun generateKeyPairIfNeeded() {
        if (hasKeyPair()) return

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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

    /** The public key to upload to your backend so others can encrypt messages for this user. */
    fun getPublicKey(): PublicKey? =
        keyStore.getCertificate(KEY_ALIAS)?.publicKey

    /** Base64 form, ready to store/send over the network. */
    fun getPublicKeyBase64(): String? =
        getPublicKey()?.encoded?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    /** Deletes the local keypair. Only call this on explicit "reset identity" — old messages become unreadable. */
    fun deleteKeyPair() {
        if (hasKeyPair()) keyStore.deleteEntry(KEY_ALIAS)
    }
}