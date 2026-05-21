package com.kinetic.trainer.ui.viewmodels

import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.fake.FakeTrainerData
import com.kinetic.trainer.data.models.ActivityEvent
import com.kinetic.trainer.data.models.ClientSummary
import com.kinetic.trainer.data.repository.AuthRepository
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.domain.InsightSeverity
import com.kinetic.trainer.domain.TrainerInsight
import com.kinetic.trainer.domain.insights.TrainerInsightRuleEngine
import com.kinetic.trainer.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepo = mockk<TrainerRepository>()
    private val mockInsightEngine = mockk<TrainerInsightRuleEngine>()
    private val mockSession = mockk<SessionManager>()
    private val mockAuthRepository = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() {
        every { mockSession.trainerId } returns "trainer1"
    }

    private fun createViewModel() =
        HomeViewModel(mockRepo, mockInsightEngine, mockSession, mockAuthRepository)

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun test_initial_loading_state_is_true_when_no_data_emitted() {
        // Neither flow emits → state update never runs → isLoading stays true
        every { mockRepo.observeActivityFeed(any()) } returns emptyFlow()
        every { mockRepo.observeClientRoster(any()) } returns emptyFlow()

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    // ─── Activity feed ────────────────────────────────────────────────────────

    @Test
    fun test_activity_feed_populated_from_repository() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns flowOf(FakeTrainerData.activityFeed)
        every { mockRepo.observeClientRoster("trainer1") } returns emptyFlow()
        every { mockInsightEngine.generateInsights(any()) } returns emptyList()

        val viewModel = createViewModel()

        assertEquals(FakeTrainerData.activityFeed, viewModel.uiState.value.activityFeed)
    }

    @Test
    fun test_loading_becomes_false_when_activity_feed_emits() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns flowOf(emptyList())
        every { mockRepo.observeClientRoster("trainer1") } returns emptyFlow()

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun test_empty_activity_feed_is_valid() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns flowOf(emptyList<ActivityEvent>())
        every { mockRepo.observeClientRoster("trainer1") } returns emptyFlow()

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.activityFeed.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Client roster ────────────────────────────────────────────────────────

    @Test
    fun test_client_roster_populated_from_repository() = runTest {
        val clients = FakeTrainerData.clients
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(clients)
        every { mockInsightEngine.generateInsights(clients) } returns emptyList()

        val viewModel = createViewModel()

        assertEquals(clients, viewModel.uiState.value.clients)
    }

    @Test
    fun test_loading_becomes_false_when_client_roster_emits() = runTest {
        val clients = FakeTrainerData.clients
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(clients)
        every { mockInsightEngine.generateInsights(any()) } returns emptyList()

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun test_empty_client_list_is_valid() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(emptyList<ClientSummary>())
        every { mockInsightEngine.generateInsights(emptyList()) } returns emptyList()

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.clients.isEmpty())
    }

    // ─── Insights ─────────────────────────────────────────────────────────────

    @Test
    fun test_insights_generated_from_client_roster() = runTest {
        val clients = FakeTrainerData.clients
        val expectedInsight = TrainerInsight(
            message = "Test insight",
            clientId = "c1",
            severity = InsightSeverity.INFO
        )
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(clients)
        every { mockInsightEngine.generateInsights(clients) } returns listOf(expectedInsight)

        val viewModel = createViewModel()

        assertEquals(listOf(expectedInsight), viewModel.uiState.value.insights)
    }

    @Test
    fun test_insights_empty_when_all_clients_healthy() = runTest {
        val clients = FakeTrainerData.clients
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(clients)
        every { mockInsightEngine.generateInsights(clients) } returns emptyList()

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.insights.isEmpty())
    }

    @Test
    fun test_insights_ordered_by_severity_urgent_first() = runTest {
        val clients = FakeTrainerData.clients
        val urgentInsight = TrainerInsight("Missed sessions", "c2", InsightSeverity.URGENT)
        val infoInsight = TrainerInsight("Exceeded targets", "c3", InsightSeverity.INFO)
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flowOf(clients)
        // Engine returns them in sorted order (URGENT > WARNING > INFO)
        every { mockInsightEngine.generateInsights(clients) } returns listOf(urgentInsight, infoInsight)

        val viewModel = createViewModel()

        assertEquals(InsightSeverity.URGENT, viewModel.uiState.value.insights.first().severity)
    }

    // ─── Trainer ID sourced from session ──────────────────────────────────────

    @Test
    fun test_repository_called_with_session_trainer_id() = runTest {
        every { mockSession.trainerId } returns "my_trainer_id"
        every { mockRepo.observeActivityFeed("my_trainer_id") } returns flowOf(emptyList())
        every { mockRepo.observeClientRoster("my_trainer_id") } returns emptyFlow()

        createViewModel()
        // If the viewModel used a different trainerId, the mock would fail
        // (mockk strict mode throws for unstubbed calls)
    }

    @Test
    fun test_activity_feed_failure_sets_error_state() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns flow {
            throw RuntimeException("activity feed failed")
        }
        every { mockRepo.observeClientRoster("trainer1") } returns emptyFlow()

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("activity feed failed", viewModel.uiState.value.error)
    }

    @Test
    fun test_client_roster_failure_sets_error_state() = runTest {
        every { mockRepo.observeActivityFeed("trainer1") } returns emptyFlow()
        every { mockRepo.observeClientRoster("trainer1") } returns flow {
            throw RuntimeException("client roster failed")
        }

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("client roster failed", viewModel.uiState.value.error)
    }
}
