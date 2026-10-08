package com.example.cipher.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hybrid RSA + AES encryption, the same pattern PGP and most E2E systems use.
 * RSA alone can't hold a full message (2048-bit RSA/OAEP caps out around
 * 190 bytes), so instead:
 *   1. generate a random one-time AES key
 *   2. encrypt the actual message with that AES key (no size limit)
 *   3. encrypt the small AES key itself with the recipient's RSA public key
 * Only the matching RSA private key can unlock the AES key, and only that
 * AES key can unlock the message.
 */
object EncryptionHelper {

    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * Encrypts [plainText] so only the holder of the private key matching
     * [recipientPublicKeyBase64] can read it.
     */
    fun encrypt(plainText: String, recipientPublicKeyBase64: String): EncryptedPayload {
        // 1. One-time AES key for this message
        val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        // 2. Encrypt the message with it
        val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = aesCipher.iv
        val ciphertext = aesCipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // 3. Encrypt the AES key with the recipient's RSA public key
        val recipientPublicKey = decodePublicKey(recipientPublicKeyBase64)
        val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

        return EncryptedPayload(
            encryptedAesKey = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts [payload] using TODAY's private key from Android Keystore.
     * Returns null if there's no key for today (expired/rotated) or decryption
     * otherwise fails — e.g. the message was encrypted to a different day's key.
     */
    fun decrypt(payload: EncryptedPayload): String? {
        val privateKeyEntry = KeyManager.getTodayPrivateKeyEntry() ?: return null

        return try {
            // 1. Recover the AES key using our RSA private key
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
            val aesKeyBytes = rsaCipher.doFinal(Base64.decode(payload.encryptedAesKey, Base64.NO_WRAP))
            val aesKey: SecretKey = SecretKeySpec(aesKeyBytes, "AES")

            // 2. Use the recovered AES key to decrypt the actual message
            val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plainBytes = aesCipher.doFinal(Base64.decode(payload.ciphertext, Base64.NO_WRAP))

            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Wrong/expired key, corrupted data, etc.
            e.printStackTrace()
            null
        }
    }

    private fun decodePublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}