package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.fake.FakeTrainerData
import com.kinetic.trainer.data.models.ChatMessage
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepo = mockk<TrainerRepository>()
    private val mockSession = mockk<SessionManager>()

    @Before
    fun setUp() {
        every { mockSession.trainerId } returns "trainer1"
    }

    private fun createViewModel(clientId: String = "c1"): ChatViewModel {
        every { mockRepo.observeChatMessages(clientId) } returns emptyFlow()
        return ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to clientId)),
            trainerRepository = mockRepo,
            sessionManager = mockSession
        )
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun test_initial_messages_is_empty() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_initial_input_text_is_empty() {
        val viewModel = createViewModel()
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun test_initial_send_error_is_null() {
        val viewModel = createViewModel()
        assertNull(viewModel.uiState.value.sendError)
    }

    @Test
    fun test_client_id_from_saved_state() {
        every { mockRepo.observeChatMessages("c99") } returns emptyFlow()
        val vm = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to "c99")),
            trainerRepository = mockRepo,
            sessionManager = mockSession
        )
        assertEquals("c99", vm.clientId)
    }

    @Test
    fun test_messages_loaded_from_repository() = runTest {
        val messages = FakeTrainerData.chatMessages["c1"]!!
        every { mockRepo.observeChatMessages("c1") } returns flowOf(messages)

        val vm = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to "c1")),
            trainerRepository = mockRepo,
            sessionManager = mockSession
        )

        assertEquals(messages, vm.uiState.value.messages)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun test_message_stream_failure_sets_error_state() = runTest {
        every { mockRepo.observeChatMessages("c1") } returns flow {
            throw RuntimeException("chat stream failed")
        }

        val vm = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to "c1")),
            trainerRepository = mockRepo,
            sessionManager = mockSession
        )

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("chat stream failed", vm.uiState.value.error)
    }

    // ─── Input changes ────────────────────────────────────────────────────────

    @Test
    fun test_on_input_change_updates_text() {
        val viewModel = createViewModel()
        viewModel.onInputChange("Let's go 💪")
        assertEquals("Let's go 💪", viewModel.uiState.value.inputText)
    }

    // ─── Send message ─────────────────────────────────────────────────────────

    @Test
    fun test_send_blank_message_does_nothing() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChange("   ")

        viewModel.sendMessage { }

        // messages list untouched
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_send_empty_message_does_nothing() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChange("")

        viewModel.sendMessage { }

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun test_send_message_clears_input_text() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Hello!")

        viewModel.sendMessage { }

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun test_send_message_optimistically_appends_message() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Squat 3x10 @60kg")

        viewModel.sendMessage { }

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Squat 3x10 @60kg", viewModel.uiState.value.messages.first().text)
    }

    @Test
    fun test_send_message_marks_as_from_trainer() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Good work today!")

        viewModel.sendMessage { }

        assertTrue(viewModel.uiState.value.messages.first().isFromTrainer)
    }

    @Test
    fun test_send_message_uses_session_trainer_id_as_sender() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Hi!")

        viewModel.sendMessage { }

        assertEquals("trainer1", viewModel.uiState.value.messages.first().senderId)
    }

    @Test
    fun test_send_message_invokes_on_sent_callback() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Message!")

        var callbackInvoked = false
        viewModel.sendMessage { callbackInvoked = true }

        assertTrue(callbackInvoked)
    }

    @Test
    fun test_send_message_calls_repository_send_message() = runTest {
        coEvery { mockRepo.sendMessage(eq("c1"), any()) } just Runs
        val viewModel = createViewModel()
        viewModel.onInputChange("Test message")

        viewModel.sendMessage { }

        coVerify(exactly = 1) { mockRepo.sendMessage(eq("c1"), any()) }
    }

    // ─── Send failure + rollback ──────────────────────────────────────────────

    @Test
    fun test_send_message_failure_rolls_back_optimistic_message() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } throws RuntimeException("Network error")
        val viewModel = createViewModel()
        viewModel.onInputChange("Will fail")

        viewModel.sendMessage { }

        assertTrue(
            "Failed message must be rolled back from the messages list",
            viewModel.uiState.value.messages.isEmpty()
        )
    }

    @Test
    fun test_send_message_failure_restores_input_text() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } throws RuntimeException("Network error")
        val viewModel = createViewModel()
        viewModel.onInputChange("Will fail")

        viewModel.sendMessage { }

        assertEquals(
            "Input text must be restored after send failure",
            "Will fail", viewModel.uiState.value.inputText
        )
    }

    @Test
    fun test_send_message_failure_sets_send_error() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } throws RuntimeException("Network error")
        val viewModel = createViewModel()
        viewModel.onInputChange("Will fail")

        viewModel.sendMessage { }

        assertNotNull(viewModel.uiState.value.sendError)
    }

    // ─── Clear error ──────────────────────────────────────────────────────────

    @Test
    fun test_clear_send_error_nullifies_error() = runTest {
        coEvery { mockRepo.sendMessage(any(), any()) } throws RuntimeException("err")
        val viewModel = createViewModel()
        viewModel.onInputChange("fail")
        viewModel.sendMessage { }

        viewModel.clearSendError()

        assertNull(viewModel.uiState.value.sendError)
    }
}
