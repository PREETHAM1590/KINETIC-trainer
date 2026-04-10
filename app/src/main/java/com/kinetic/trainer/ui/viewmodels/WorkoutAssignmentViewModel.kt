package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinetic.trainer.data.models.AssignedWorkout
import com.kinetic.trainer.data.models.Exercise
import com.kinetic.trainer.data.models.WorkoutTemplate
import com.kinetic.trainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WorkoutAssignmentTab { TEMPLATES, CLONE_LAST, SCRATCH }

data class WorkoutAssignmentUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val selectedTab: WorkoutAssignmentTab = WorkoutAssignmentTab.TEMPLATES,
    val scratchExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class WorkoutAssignmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    val clientId: String = savedStateHandle["clientId"] ?: ""

    private val _uiState = MutableStateFlow(WorkoutAssignmentUiState())
    val uiState: StateFlow<WorkoutAssignmentUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            val templates = trainerRepository.getWorkoutTemplates()
            _uiState.value = _uiState.value.copy(templates = templates, isLoading = false)
        }
    }

    fun selectTab(tab: WorkoutAssignmentTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun addExercise(exercise: Exercise) {
        _uiState.value = _uiState.value.copy(
            scratchExercises = _uiState.value.scratchExercises + exercise
        )
    }

    fun removeExercise(index: Int) {
        val updated = _uiState.value.scratchExercises.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(scratchExercises = updated)
    }

    fun saveWorkout(targetClientId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val workout = AssignedWorkout(
                clientId = targetClientId,
                exercises = _uiState.value.scratchExercises,
                assignedForDateMs = System.currentTimeMillis()
            )
            trainerRepository.assignWorkout(workout)
            _uiState.value = _uiState.value.copy(isSaved = true)
            onSuccess()
        }
    }

    fun assignTemplate(templateId: String, targetClientId: String, onSuccess: () -> Unit) {
        val template = _uiState.value.templates.find { it.id == templateId } ?: return
        viewModelScope.launch {
            val workout = AssignedWorkout(
                clientId = targetClientId,
                exercises = template.exercises,
                assignedForDateMs = System.currentTimeMillis(),
                templateId = templateId
            )
            trainerRepository.assignWorkout(workout)
            _uiState.value = _uiState.value.copy(isSaved = true)
            onSuccess()
        }
    }
}
