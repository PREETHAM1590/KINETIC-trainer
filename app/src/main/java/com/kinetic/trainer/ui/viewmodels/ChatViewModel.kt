package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.models.ChatMessage
import com.kinetic.trainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val sendError: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainerRepository: TrainerRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val clientId: String = savedStateHandle["clientId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            trainerRepository.observeChatMessages(clientId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load messages",
                    )
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = null,
                    )
                }
        }
    }

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun sendMessage(onSent: () -> Unit) {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = sessionManager.trainerId,
                text = text,
                timestampMs = System.currentTimeMillis(),
                isFromTrainer = true,
                isRead = false
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + message,
                inputText = "",
                sendError = null,
                error = null,
            )
            try {
                trainerRepository.sendMessage(clientId, message)
                onSent()
            } catch (e: Exception) {
                // Roll back the optimistic message and surface the error to the UI
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.filter { it.id != message.id },
                    inputText = text,
                    sendError = "Failed to send message. Please try again.",
                )
            }
        }
    }

    fun clearSendError() {
        _uiState.value = _uiState.value.copy(sendError = null)
    }
}
