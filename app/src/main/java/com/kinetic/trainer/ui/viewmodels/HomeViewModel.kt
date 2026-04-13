package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.models.ActivityEvent
import com.kinetic.trainer.data.models.ClientSummary
import com.kinetic.trainer.data.repository.AuthRepository
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.domain.TrainerInsight
import com.kinetic.trainer.domain.insights.TrainerInsightRuleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activityFeed: List<ActivityEvent> = emptyList(),
    val clients: List<ClientSummary> = emptyList(),
    val insights: List<TrainerInsight> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository,
    private val insightEngine: TrainerInsightRuleEngine,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val trainerId = sessionManager.trainerId
        viewModelScope.launch {
            trainerRepository.observeActivityFeed(trainerId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load activity feed",
                    )
                }
                .collect { feed ->
                    _uiState.value = _uiState.value.copy(
                        activityFeed = feed,
                        isLoading = false,
                        error = null,
                    )
                }
        }
        viewModelScope.launch {
            trainerRepository.observeClientRoster(trainerId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load client roster",
                    )
                }
                .collect { clients ->
                    val insights = insightEngine.generateInsights(clients)
                    _uiState.value = _uiState.value.copy(
                        clients = clients,
                        insights = insights,
                        isLoading = false,
                        error = null,
                    )
                }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            sessionManager.clearSession()
            onComplete()
        }
    }
}
