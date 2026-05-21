package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.kinetic.trainer.data.fake.FakeTrainerData
import com.kinetic.trainer.data.models.Exercise
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutAssignmentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepo = mockk<TrainerRepository>()
    private val templates = FakeTrainerData.workoutTemplates

    private fun createViewModel(clientId: String = "c1"): WorkoutAssignmentViewModel {
        coEvery { mockRepo.getWorkoutTemplates() } returns templates
        return WorkoutAssignmentViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to clientId)),
            trainerRepository = mockRepo
        )
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun test_templates_loaded_on_init() = runTest {
        val viewModel = createViewModel()
        assertEquals(templates, viewModel.uiState.value.templates)
    }

    @Test
    fun test_loading_becomes_false_after_templates_loaded() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun test_client_id_comes_from_saved_state_handle() = runTest {
        val viewModel = createViewModel(clientId = "client_xyz")
        assertEquals("client_xyz", viewModel.clientId)
    }

    @Test
    fun test_default_selected_tab_is_templates() = runTest {
        val viewModel = createViewModel()
        assertEquals(WorkoutAssignmentTab.TEMPLATES, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun test_initial_scratch_exercises_is_empty() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.scratchExercises.isEmpty())
    }

    @Test
    fun test_is_saved_is_false_initially() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isSaved)
    }

    // ─── Tab selection ────────────────────────────────────────────────────────

    @Test
    fun test_select_tab_scratch() = runTest {
        val viewModel = createViewModel()
        viewModel.selectTab(WorkoutAssignmentTab.SCRATCH)
        assertEquals(WorkoutAssignmentTab.SCRATCH, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun test_select_tab_clone_last() = runTest {
        val viewModel = createViewModel()
        viewModel.selectTab(WorkoutAssignmentTab.CLONE_LAST)
        assertEquals(WorkoutAssignmentTab.CLONE_LAST, viewModel.uiState.value.selectedTab)
    }

    // ─── Add / remove exercises ───────────────────────────────────────────────

    @Test
    fun test_add_exercise_appends_to_scratch_list() = runTest {
        val viewModel = createViewModel()
        val exercise = Exercise("Squat", 3, 10, 60f)

        viewModel.addExercise(exercise)

        assertEquals(listOf(exercise), viewModel.uiState.value.scratchExercises)
    }

    @Test
    fun test_add_multiple_exercises_preserves_order() = runTest {
        val viewModel = createViewModel()
        val ex1 = Exercise("Squat", 3, 10, 60f)
        val ex2 = Exercise("Bench Press", 4, 8, 80f)
        val ex3 = Exercise("Deadlift", 3, 5, 100f)

        viewModel.addExercise(ex1)
        viewModel.addExercise(ex2)
        viewModel.addExercise(ex3)

        assertEquals(listOf(ex1, ex2, ex3), viewModel.uiState.value.scratchExercises)
    }

    @Test
    fun test_remove_exercise_by_index() = runTest {
        val viewModel = createViewModel()
        val ex1 = Exercise("Squat", 3, 10, 60f)
        val ex2 = Exercise("Bench Press", 4, 8, 80f)

        viewModel.addExercise(ex1)
        viewModel.addExercise(ex2)
        viewModel.removeExercise(0)

        assertEquals(listOf(ex2), viewModel.uiState.value.scratchExercises)
    }

    @Test
    fun test_remove_last_exercise_leaves_empty_list() = runTest {
        val viewModel = createViewModel()
        viewModel.addExercise(Exercise("Squat", 3, 10, 60f))
        viewModel.removeExercise(0)

        assertTrue(viewModel.uiState.value.scratchExercises.isEmpty())
    }

    // ─── Save scratch workout ─────────────────────────────────────────────────

    @Test
    fun test_save_workout_marks_is_saved_true() = runTest {
        coEvery { mockRepo.assignWorkout(any()) } just Runs
        val viewModel = createViewModel()
        viewModel.addExercise(Exercise("Squat", 3, 10, 60f))

        viewModel.saveWorkout("c1") { }

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun test_save_workout_invokes_on_success_callback() = runTest {
        coEvery { mockRepo.assignWorkout(any()) } just Runs
        val viewModel = createViewModel()
        viewModel.addExercise(Exercise("Squat", 3, 10, 60f))

        var callbackInvoked = false
        viewModel.saveWorkout("c1") { callbackInvoked = true }

        assertTrue(callbackInvoked)
    }

    @Test
    fun test_save_workout_calls_assign_workout_once() = runTest {
        coEvery { mockRepo.assignWorkout(any()) } just Runs
        val viewModel = createViewModel()
        viewModel.addExercise(Exercise("Squat", 3, 10, 60f))

        viewModel.saveWorkout("c1") { }

        coVerify(exactly = 1) { mockRepo.assignWorkout(any()) }
    }

    // ─── Assign template ──────────────────────────────────────────────────────

    @Test
    fun test_assign_template_marks_is_saved_true() = runTest {
        coEvery { mockRepo.assignWorkout(any()) } just Runs
        val viewModel = createViewModel()

        viewModel.assignTemplate(templates.first().id, "c1") { }

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun test_assign_template_invokes_on_success_callback() = runTest {
        coEvery { mockRepo.assignWorkout(any()) } just Runs
        val viewModel = createViewModel()

        var callbackInvoked = false
        viewModel.assignTemplate(templates.first().id, "c1") { callbackInvoked = true }

        assertTrue(callbackInvoked)
    }

    @Test
    fun test_assign_unknown_template_id_does_nothing() = runTest {
        val viewModel = createViewModel()

        var callbackInvoked = false
        viewModel.assignTemplate("nonexistent_template_id", "c1") { callbackInvoked = true }

        assertFalse("Unknown template ID: callback must not be called", callbackInvoked)
        assertFalse("Unknown template ID: isSaved must remain false", viewModel.uiState.value.isSaved)
    }
    @Test
    fun test_templates_empty_and_loading_false_when_repository_throws() = runTest {
        coEvery { mockRepo.getWorkoutTemplates() } throws IllegalStateException("Trainer session not initialized")
        val viewModel = WorkoutAssignmentViewModel(
            savedStateHandle = SavedStateHandle(mapOf("clientId" to "c1")),
            trainerRepository = mockRepo
        )

        assertFalse(
            "isLoading must be false even when getWorkoutTemplates throws",
            viewModel.uiState.value.isLoading
        )
        assertTrue(
            "templates must be empty when getWorkoutTemplates throws",
            viewModel.uiState.value.templates.isEmpty()
        )
    }
}