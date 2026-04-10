package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.models.ActivityEvent
import com.kinetic.trainer.data.models.ClientSummary
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.domain.TrainerInsight
import com.kinetic.trainer.domain.TrainerInsightEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activityFeed: List<ActivityEvent> = emptyList(),
    val clients: List<ClientSummary> = emptyList(),
    val insights: List<TrainerInsight> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository,
    private val insightEngine: TrainerInsightEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val trainerId = "trainer_01"
        viewModelScope.launch {
            trainerRepository.observeActivityFeed(trainerId).collect { feed ->
                _uiState.value = _uiState.value.copy(activityFeed = feed, isLoading = false)
            }
        }
        viewModelScope.launch {
            trainerRepository.observeClientRoster(trainerId).collect { clients ->
                val insights = insightEngine.generateInsights(clients)
                _uiState.value = _uiState.value.copy(clients = clients, insights = insights, isLoading = false)
            }
        }
    }
}
