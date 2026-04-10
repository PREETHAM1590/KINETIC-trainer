package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.models.ChatMessage
import com.kinetic.trainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true
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
            trainerRepository.observeChatMessages(clientId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
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
                inputText = ""
            )
            trainerRepository.sendMessage(clientId, message)
            onSent()
        }
    }
}
