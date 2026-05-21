package com.kinetic.trainer.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyStore

/**
 * Unit tests for SecureKeyManager encryption using Android Keystore.
 * 
 * Verifies:
 * - Keys are generated and stored in Keystore
 * - Encryption/decryption works correctly
 * - Keys persist across manager instances
 * - Multiple conversations have isolated keys
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SecureKeyManagerTest {

    private val testConversationId1 = "test_gym_trainer123_client456"
    private val testConversationId2 = "test_gym_trainer123_client789"
    
    @Before
    fun setup() {
        // Clean up any existing test keys
        deleteTestKeys()
    }
    
    @After
    fun cleanup() {
        deleteTestKeys()
    }
    
    private fun deleteTestKeys() {
        try {
            SecureKeyManager.deleteConversationKey(testConversationId1)
            SecureKeyManager.deleteConversationKey(testConversationId2)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `getOrCreateConversationKey generates key on first call`() {
        // Verify key doesn't exist
        assertFalse(SecureKeyManager.hasConversationKey(testConversationId1))
        
        // Generate key
        val key = SecureKeyManager.getOrCreateConversationKey(testConversationId1)
        assertNotNull(key)
        
        // Verify key now exists
        assertTrue(SecureKeyManager.hasConversationKey(testConversationId1))
    }

    @Test
    fun `getOrCreateConversationKey returns same key on subsequent calls`() {
        val key1 = SecureKeyManager.getOrCreateConversationKey(testConversationId1)
        val key2 = SecureKeyManager.getOrCreateConversationKey(testConversationId1)
        
        // Keys should be the same instance
        assertEquals(key1, key2)
    }

    @Test
    fun `encryptMessage and decryptMessage work correctly`() {
        val plaintext = "Hello, this is a secure message!"
        
        // Encrypt
        val ciphertext = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        assertNotEquals(plaintext, ciphertext)
        assertTrue(ciphertext.isNotEmpty())
        
        // Decrypt
        val decrypted = SecureKeyManager.decryptMessage(testConversationId1, ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypted messages are different each time due to random IV`() {
        val plaintext = "Same message"
        
        val ciphertext1 = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        val ciphertext2 = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        
        // Ciphertexts should be different (different IVs)
        assertNotEquals(ciphertext1, ciphertext2)
        
        // But both should decrypt to same plaintext
        assertEquals(plaintext, SecureKeyManager.decryptMessage(testConversationId1, ciphertext1))
        assertEquals(plaintext, SecureKeyManager.decryptMessage(testConversationId1, ciphertext2))
    }

    @Test
    fun `different conversations have isolated keys`() {
        val plaintext = "Secret message"
        
        // Encrypt with conversation 1 key
        val ciphertext1 = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        
        // Try to decrypt with conversation 2 key (should fail)
        try {
            SecureKeyManager.decryptMessage(testConversationId2, ciphertext1)
            fail("Should not decrypt with wrong conversation key")
        } catch (e: Exception) {
            // Expected: decryption should fail with wrong key
            assertTrue(e is javax.crypto.AEADBadTagException || e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun `deleteConversationKey removes key from keystore`() {
        // Create key
        SecureKeyManager.getOrCreateConversationKey(testConversationId1)
        assertTrue(SecureKeyManager.hasConversationKey(testConversationId1))
        
        // Delete key
        SecureKeyManager.deleteConversationKey(testConversationId1)
        assertFalse(SecureKeyManager.hasConversationKey(testConversationId1))
    }

    @Test
    fun `encryption handles special characters and unicode`() {
        val plaintext = "Special chars: @#$%^&*() Unicode: 你好🔐"
        
        val ciphertext = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        val decrypted = SecureKeyManager.decryptMessage(testConversationId1, ciphertext)
        
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encryption handles empty string`() {
        val plaintext = ""
        
        val ciphertext = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        val decrypted = SecureKeyManager.decryptMessage(testConversationId1, ciphertext)
        
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encryption handles long messages`() {
        val plaintext = "A".repeat(10000) // 10KB message
        
        val ciphertext = SecureKeyManager.encryptMessage(testConversationId1, plaintext)
        val decrypted = SecureKeyManager.decryptMessage(testConversationId1, ciphertext)
        
        assertEquals(plaintext, decrypted)
    }
}