package com.kinetic.trainer.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure key management using Android Keystore System.
 *
 * CRITICAL SECURITY FIX: Keys never leave the device or enter Firestore.
 * All encryption keys are generated and stored in hardware-backed Android Keystore.
 *
 * Falls back to software-backed AES keys in environments where Android Keystore
 * is unavailable (e.g. unit tests running on JVM).
 */
object SecureKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS_PREFIX = "kinetic_chat_"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128

    // Software-backed fallback keys used when AndroidKeyStore is unavailable (e.g. unit tests)
    private val softwareKeys = ConcurrentHashMap<String, SecretKey>()

    private val keyStore: KeyStore? by lazy {
        try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        } catch (e: Exception) {
            null
        }
    }

    fun getOrCreateConversationKey(conversationId: String): SecretKey {
        val keyAlias = "$KEY_ALIAS_PREFIX$conversationId"
        softwareKeys[keyAlias]?.let { return it }
        return try {
            val ks = keyStore ?: return getSoftwareKey(keyAlias)
            ks.getKey(keyAlias, null) as? SecretKey
                ?: generateAndStoreHardwareKey(keyAlias)
        } catch (e: Exception) {
            Timber.e(e, "AndroidKeyStore unavailable for $conversationId, using software key")
            getSoftwareKey(keyAlias)
        }
    }

    private fun getSoftwareKey(keyAlias: String): SecretKey =
        softwareKeys.getOrPut(keyAlias) {
            javax.crypto.KeyGenerator.getInstance("AES").apply { init(KEY_SIZE_BITS) }.generateKey()
        }

    private fun generateAndStoreHardwareKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    fun encryptMessage(conversationId: String, plaintext: String): String {
        val key = getOrCreateConversationKey(conversationId)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val blob = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, blob, 0, iv.size)
        System.arraycopy(encrypted, 0, blob, iv.size, encrypted.size)

        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    fun decryptMessage(conversationId: String, ciphertextBase64: String): String {
        val key = getOrCreateConversationKey(conversationId)
        val blob = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val iv = blob.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = blob.copyOfRange(IV_SIZE_BYTES, blob.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun hasConversationKey(conversationId: String): Boolean {
        val keyAlias = "$KEY_ALIAS_PREFIX$conversationId"
        if (softwareKeys.containsKey(keyAlias)) return true
        return try {
            keyStore?.containsAlias(keyAlias) ?: false
        } catch (e: Exception) {
            Timber.e(e, "Failed to check key existence")
            false
        }
    }

    fun deleteConversationKey(conversationId: String) {
        val keyAlias = "$KEY_ALIAS_PREFIX$conversationId"
        softwareKeys.remove(keyAlias)
        try {
            keyStore?.let { ks ->
                if (ks.containsAlias(keyAlias)) {
                    ks.deleteEntry(keyAlias)
                    Timber.d("Deleted key for conversation $conversationId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete key for conversation $conversationId")
        }
    }
}
