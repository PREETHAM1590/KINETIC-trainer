package com.kinetic.trainer

import com.kinetic.trainer.data.ChatEncryption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Unit tests for [ChatEncryption] (AES-256-GCM).
 *
 * Pure JVM tests — no Robolectric needed since ChatEncryption uses java.util.Base64.
 */
class ChatEncryptionTest {

    // ─── Key generation ───────────────────────────────────────────────────────

    @Test
    fun test_generate_key_base64_produces_32_bytes() {
        val keyBase64 = ChatEncryption.generateKeyBase64()
        val keyBytes = Base64.getDecoder().decode(keyBase64)
        assertEquals("AES-256 key must be exactly 32 bytes (256 bits)", 32, keyBytes.size)
    }

    @Test
    fun test_generate_key_base64_is_no_wrap_encoded() {
        val keyBase64 = ChatEncryption.generateKeyBase64()
        // NO_WRAP encoding must not contain newline characters
        assertTrue("Key must not contain newlines (NO_WRAP)", !keyBase64.contains('\n'))
    }

    @Test
    fun test_successive_key_generations_are_unique() {
        val k1 = ChatEncryption.generateKeyBase64()
        val k2 = ChatEncryption.generateKeyBase64()
        assertNotEquals("Each generateKeyBase64() call must produce a unique key", k1, k2)
    }

    // ─── Encrypt / Decrypt round-trips ────────────────────────────────────────

    @Test
    fun test_encrypt_decrypt_roundtrip() {
        val key = ChatEncryption.generateKeyBase64()
        val original = "Hello, KINETIC!"
        val ciphertext = ChatEncryption.encrypt(key, original)
        val decrypted = ChatEncryption.decrypt(key, ciphertext)
        assertEquals(original, decrypted)
    }

    @Test
    fun test_empty_string_encryption() {
        val key = ChatEncryption.generateKeyBase64()
        val ciphertext = ChatEncryption.encrypt(key, "")
        val decrypted = ChatEncryption.decrypt(key, ciphertext)
        assertEquals("Empty string must survive encrypt/decrypt cycle", "", decrypted)
    }

    @Test
    fun test_unicode_message_encryption() {
        val key = ChatEncryption.generateKeyBase64()
        val unicode = "💪 Great leg day! 🔥 体育館へようこそ 🎯"
        val decrypted = ChatEncryption.decrypt(key, ChatEncryption.encrypt(key, unicode))
        assertEquals("Unicode/emoji must survive encrypt/decrypt cycle", unicode, decrypted)
    }

    @Test
    fun test_long_message_roundtrip() {
        val key = ChatEncryption.generateKeyBase64()
        val longMessage = "Squat 3×10 @60kg. ".repeat(500)   // ~9 000 chars
        val decrypted = ChatEncryption.decrypt(key, ChatEncryption.encrypt(key, longMessage))
        assertEquals(longMessage, decrypted)
    }

    @Test
    fun test_multiple_messages_all_round_trip_correctly() {
        val key = ChatEncryption.generateKeyBase64()
        val messages = listOf(
            "Hi Priya! Great leg day 💪",
            "Let's bump squats to 70 kg next session.",
            "See you Thursday 🙏",
            ""
        )
        for (msg in messages) {
            assertEquals(
                "Message '$msg' must round-trip",
                msg,
                ChatEncryption.decrypt(key, ChatEncryption.encrypt(key, msg))
            )
        }
    }

    // ─── IV randomness ────────────────────────────────────────────────────────

    @Test
    fun test_different_messages_produce_different_ciphertexts() {
        // Each encryption call generates a fresh random IV, so encrypting the
        // same plaintext twice must produce different ciphertexts.
        val key = ChatEncryption.generateKeyBase64()
        val plaintext = "same plaintext, different IV each time"
        val ct1 = ChatEncryption.encrypt(key, plaintext)
        val ct2 = ChatEncryption.encrypt(key, plaintext)
        assertNotEquals(
            "Two encryptions of identical plaintext must differ (IV randomness)",
            ct1, ct2
        )
    }

    @Test
    fun test_different_keys_produce_different_ciphertexts() {
        val key1 = ChatEncryption.generateKeyBase64()
        val key2 = ChatEncryption.generateKeyBase64()
        val ct1 = ChatEncryption.encrypt(key1, "same message")
        val ct2 = ChatEncryption.encrypt(key2, "same message")
        assertNotEquals(ct1, ct2)
    }

    // ─── Blob layout ──────────────────────────────────────────────────────────

    @Test
    fun test_ciphertext_blob_is_longer_than_iv() {
        // Blob = 12-byte IV || ciphertext+tag (tag alone is 16 bytes).
        // So a non-empty message should produce a blob > 12 bytes.
        val key = ChatEncryption.generateKeyBase64()
        val ciphertext = ChatEncryption.encrypt(key, "test")
        val blob = Base64.getDecoder().decode(ciphertext)
        assertTrue("Blob must be longer than the 12-byte IV prefix", blob.size > 12)
    }

    // ─── Authentication failures ───────────────────────────────────────────────

    @Test
    fun test_decrypt_with_wrong_key_throws() {
        val key1 = ChatEncryption.generateKeyBase64()
        val key2 = ChatEncryption.generateKeyBase64()
        val ciphertext = ChatEncryption.encrypt(key1, "top secret training plan")
        assertThrows(
            "Decrypting with the wrong key must throw",
            Exception::class.java
        ) {
            ChatEncryption.decrypt(key2, ciphertext)
        }
    }

    @Test
    fun test_tampered_ciphertext_throws() {
        val key = ChatEncryption.generateKeyBase64()
        val ciphertext = ChatEncryption.encrypt(key, "authentic message")
        // Corrupt the last 4 characters to invalidate the GCM authentication tag
        val tampered = ciphertext.dropLast(4) + "ZZZZ"
        assertThrows(
            "Tampered ciphertext must throw (GCM authentication failure)",
            Exception::class.java
        ) {
            ChatEncryption.decrypt(key, tampered)
        }
    }

    @Test
    fun test_truncated_ciphertext_throws() {
        val key = ChatEncryption.generateKeyBase64()
        val ciphertext = ChatEncryption.encrypt(key, "message")
        // Strip enough bytes to corrupt the GCM tag
        val truncated = ciphertext.take(ciphertext.length / 2)
        assertThrows(Exception::class.java) {
            ChatEncryption.decrypt(key, truncated)
        }
    }
}
