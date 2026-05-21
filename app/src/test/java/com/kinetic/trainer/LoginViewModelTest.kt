package com.kinetic.trainer.ui.viewmodels

import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.models.AuthResult
import com.kinetic.trainer.data.repository.AuthRepository
import com.kinetic.trainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockAuthRepo = mockk<AuthRepository>()
    private val mockSession = mockk<SessionManager>(relaxed = true)

    private fun createViewModel() = LoginViewModel(mockAuthRepo, mockSession)

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun test_initial_email_is_empty() {
        assertEquals("", createViewModel().uiState.value.email)
    }

    @Test
    fun test_initial_password_is_empty() {
        assertEquals("", createViewModel().uiState.value.password)
    }

    @Test
    fun test_initial_is_loading_is_false() {
        assertFalse(createViewModel().uiState.value.isLoading)
    }

    @Test
    fun test_initial_error_is_null() {
        assertNull(createViewModel().uiState.value.error)
    }

    // ─── Field input changes ──────────────────────────────────────────────────

    @Test
    fun test_on_email_change_updates_state() {
        val viewModel = createViewModel()
        viewModel.onEmailChange("trainer@kinetic.app")
        assertEquals("trainer@kinetic.app", viewModel.uiState.value.email)
    }

    @Test
    fun test_on_password_change_updates_state() {
        val viewModel = createViewModel()
        viewModel.onPasswordChange("SuperSecret99!")
        assertEquals("SuperSecret99!", viewModel.uiState.value.password)
    }

    @Test
    fun test_on_email_change_clears_existing_error() {
        val viewModel = createViewModel()
        // Trigger validation error first
        viewModel.signIn { }
        // Now update email — error should clear
        viewModel.onEmailChange("new@email.com")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun test_on_password_change_clears_existing_error() {
        val viewModel = createViewModel()
        viewModel.signIn { }
        viewModel.onPasswordChange("newPassword")
        assertNull(viewModel.uiState.value.error)
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    fun test_sign_in_with_blank_email_sets_validation_error() {
        val viewModel = createViewModel()
        viewModel.onPasswordChange("password123")

        viewModel.signIn { }

        assertEquals("Email and password are required", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun test_sign_in_with_blank_password_sets_validation_error() {
        val viewModel = createViewModel()
        viewModel.onEmailChange("trainer@kinetic.app")

        viewModel.signIn { }

        assertEquals("Email and password are required", viewModel.uiState.value.error)
    }

    @Test
    fun test_sign_in_with_blank_fields_does_not_call_auth_repo() {
        val viewModel = createViewModel()

        viewModel.signIn { }

        // mockAuthRepo is strict — if signIn were called, it would throw because it's unstubbed
        // Reaching here without exception means it was NOT called
    }

    // ─── Successful sign-in ───────────────────────────────────────────────────

    @Test
    fun test_sign_in_success_invokes_on_success_callback() = runTest {
        coEvery {
            mockAuthRepo.signIn("trainer@kinetic.app", "Secret123!")
        } returns AuthResult.Success("uid_123", "gym_abc")

        val viewModel = createViewModel()
        viewModel.onEmailChange("trainer@kinetic.app")
        viewModel.onPasswordChange("Secret123!")

        var successCalled = false
        viewModel.signIn { successCalled = true }

        assertTrue(successCalled)
    }

    @Test
    fun test_sign_in_success_saves_trainer_id_in_session() = runTest {
        coEvery {
            mockAuthRepo.signIn(any(), any())
        } returns AuthResult.Success("uid_999", "gym_xyz")

        val viewModel = createViewModel()
        viewModel.onEmailChange("any@email.com")
        viewModel.onPasswordChange("anyPassword")

        viewModel.signIn { }

        verify { mockSession.trainerId = "uid_999" }
    }

    @Test
    fun test_sign_in_success_saves_gym_id_in_session() = runTest {
        coEvery {
            mockAuthRepo.signIn(any(), any())
        } returns AuthResult.Success("uid_999", "gym_xyz")

        val viewModel = createViewModel()
        viewModel.onEmailChange("any@email.com")
        viewModel.onPasswordChange("anyPassword")

        viewModel.signIn { }

        verify { mockSession.gymId = "gym_xyz" }
    }

    @Test
    fun test_sign_in_success_clears_loading() = runTest {
        coEvery { mockAuthRepo.signIn(any(), any()) } returns AuthResult.Success("u", "g")

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("pass")

        viewModel.signIn { }

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Failed sign-in ───────────────────────────────────────────────────────

    @Test
    fun test_sign_in_error_sets_error_message_in_state() = runTest {
        coEvery {
            mockAuthRepo.signIn("bad@email.com", "wrongpassword")
        } returns AuthResult.Error("Invalid credentials")

        val viewModel = createViewModel()
        viewModel.onEmailChange("bad@email.com")
        viewModel.onPasswordChange("wrongpassword")

        viewModel.signIn { }

        assertEquals("Invalid credentials", viewModel.uiState.value.error)
    }

    @Test
    fun test_sign_in_error_clears_loading() = runTest {
        coEvery { mockAuthRepo.signIn(any(), any()) } returns AuthResult.Error("Oops")

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("pass")

        viewModel.signIn { }

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun test_sign_in_error_does_not_invoke_on_success_callback() = runTest {
        coEvery { mockAuthRepo.signIn(any(), any()) } returns AuthResult.Error("Nope")

        val viewModel = createViewModel()
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("pass")

        var callbackInvoked = false
        viewModel.signIn { callbackInvoked = true }

        assertFalse(callbackInvoked)
    }

    // ─── Email trimming ───────────────────────────────────────────────────────

    @Test
    fun test_sign_in_trims_whitespace_from_email() = runTest {
        // The repo is called with the trimmed email, not the padded one
        coEvery {
            mockAuthRepo.signIn("trainer@kinetic.app", "pass")
        } returns AuthResult.Success("u", "g")

        val viewModel = createViewModel()
        viewModel.onEmailChange("  trainer@kinetic.app  ")
        viewModel.onPasswordChange("pass")

        var successCalled = false
        viewModel.signIn { successCalled = true }

        assertTrue("signIn must trim whitespace from email before calling the repo", successCalled)
    }
}
