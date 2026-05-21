package com.kinetic.trainer.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object ChatEncryption {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128

    /** Generate a random AES-256 key, returned as a Base64 string for Firestore storage. */
    fun generateKeyBase64(): String {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(KEY_SIZE_BITS, SecureRandom())
        return Base64.getEncoder().encodeToString(kg.generateKey().encoded)
    }

    /**
     * Encrypt [plaintext] with [keyBase64].
     * Returns a Base64-encoded blob: 12-byte IV || AES-GCM ciphertext+tag.
     */
    fun encrypt(keyBase64: String, plaintext: String): String {
        val keyBytes = Base64.getDecoder().decode(keyBase64)
        val key = SecretKeySpec(keyBytes, "AES")
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val blob = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, blob, 0, iv.size)
        System.arraycopy(encrypted, 0, blob, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(blob)
    }

    /**
     * Decrypt [ciphertextBase64] with [keyBase64].
     * Returns plaintext string. Throws on tampered/invalid data.
     */
    fun decrypt(keyBase64: String, ciphertextBase64: String): String {
        val keyBytes = Base64.getDecoder().decode(keyBase64)
        val key = SecretKeySpec(keyBytes, "AES")
        val blob = Base64.getDecoder().decode(ciphertextBase64)
        val iv = blob.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = blob.copyOfRange(IV_SIZE_BYTES, blob.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
