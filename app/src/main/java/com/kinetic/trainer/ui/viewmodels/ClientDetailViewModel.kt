package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.models.ClientDetail
import com.kinetic.trainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientDetailUiState(
    val detail: ClientDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    private val clientId: String = savedStateHandle["clientId"] ?: ""

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    init {
        loadClientDetail()
    }

    private fun loadClientDetail() {
        viewModelScope.launch {
            trainerRepository.observeClientDetail(clientId)
                .catch { error ->
                    _uiState.value = ClientDetailUiState(
                        isLoading = false,
                        error = error.message ?: "Failed to load client details",
                    )
                }
                .collect { detail ->
                    _uiState.value = if (detail != null) {
                        ClientDetailUiState(detail = detail, isLoading = false)
                    } else {
                        ClientDetailUiState(isLoading = false, error = "Client not found")
                    }
                }
        }
    }
}