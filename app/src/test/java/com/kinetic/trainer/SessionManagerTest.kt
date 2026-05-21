package com.kinetic.trainer.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kinetic.trainer.data.SessionManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [SessionManager].
 *
 * Robolectric provides a real Android [Context] so that [SessionManager]'s
 * [androidx.security.crypto.EncryptedSharedPreferences] initialises correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionManager = SessionManager(context)
    }

    @After
    fun tearDown() {
        // Leave the prefs clean for the next test
        sessionManager.clearSession()
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun test_initial_state_gymId_is_empty() {
        assertEquals("", sessionManager.gymId)
    }

    @Test
    fun test_initial_state_trainerId_is_empty() {
        assertEquals("", sessionManager.trainerId)
    }

    @Test
    fun test_initial_state_is_not_logged_in() {
        assertFalse(sessionManager.isLoggedIn())
    }

    // ─── Save & retrieve ──────────────────────────────────────────────────────

    @Test
    fun test_save_and_retrieve_session() {
        sessionManager.gymId = "gym_abc123"
        sessionManager.trainerId = "trainer_xyz789"

        assertEquals("gym_abc123", sessionManager.gymId)
        assertEquals("trainer_xyz789", sessionManager.trainerId)
    }

    @Test
    fun test_is_logged_in_true_after_trainer_id_saved() {
        sessionManager.trainerId = "trainer_xyz"
        assertTrue(sessionManager.isLoggedIn())
    }

    @Test
    fun test_is_logged_in_false_when_only_gym_id_is_set() {
        sessionManager.gymId = "gym_abc"
        // trainerId is still empty → isLoggedIn must be false
        assertFalse(sessionManager.isLoggedIn())
    }

    @Test
    fun test_gym_id_can_be_updated() {
        sessionManager.gymId = "gym_v1"
        sessionManager.gymId = "gym_v2"
        assertEquals("gym_v2", sessionManager.gymId)
    }

    @Test
    fun test_trainer_id_can_be_updated() {
        sessionManager.trainerId = "trainer_v1"
        sessionManager.trainerId = "trainer_v2"
        assertEquals("trainer_v2", sessionManager.trainerId)
    }

    // ─── Clear session ────────────────────────────────────────────────────────

    @Test
    fun test_clear_removes_gym_id() {
        sessionManager.gymId = "gym_abc"
        sessionManager.clearSession()
        assertEquals("", sessionManager.gymId)
    }

    @Test
    fun test_clear_removes_trainer_id() {
        sessionManager.trainerId = "trainer_xyz"
        sessionManager.clearSession()
        assertEquals("", sessionManager.trainerId)
    }

    @Test
    fun test_clear_sets_is_logged_in_to_false() {
        sessionManager.trainerId = "trainer_xyz"
        sessionManager.clearSession()
        assertFalse(sessionManager.isLoggedIn())
    }

    @Test
    fun test_values_can_be_written_after_clear() {
        sessionManager.gymId = "gym_old"
        sessionManager.trainerId = "trainer_old"
        sessionManager.clearSession()

        sessionManager.gymId = "gym_new"
        sessionManager.trainerId = "trainer_new"

        assertEquals("gym_new", sessionManager.gymId)
        assertEquals("trainer_new", sessionManager.trainerId)
    }
}
