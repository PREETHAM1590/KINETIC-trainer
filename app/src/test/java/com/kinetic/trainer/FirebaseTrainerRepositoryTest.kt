package com.kinetic.trainer.data.repository

import app.cash.turbine.test
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.repository.FirebaseTrainerRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FirebaseTrainerRepository].
 *
 * All Firestore interactions are mocked with MockK so these tests remain
 * hermetic (no real network / Firebase calls).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseTrainerRepositoryTest {

    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockSession = mockk<SessionManager>()
    private lateinit var repository: FirebaseTrainerRepository

    @Before
    fun setUp() {
        repository = FirebaseTrainerRepository(mockFirestore, mockSession)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Stubs a Firestore collection chain for the members query. */
    private fun stubMembersQuery(
        gymId: String,
        trainerId: String,
        block: (query: Query, listenerReg: ListenerRegistration) -> Unit
    ) {
        val gymCollection = mockk<CollectionReference>()
        val gymDoc = mockk<DocumentReference>()
        val membersCollection = mockk<CollectionReference>()
        val query = mockk<Query>()
        val listenerReg = mockk<ListenerRegistration>(relaxed = true)

        every { mockFirestore.collection("gyms") } returns gymCollection
        every { gymCollection.document(gymId) } returns gymDoc
        every { gymDoc.collection("members") } returns membersCollection
        every { membersCollection.whereEqualTo("trainerId", trainerId) } returns query

        block(query, listenerReg)
    }

    /** Stubs a Firestore collection chain for the activity feed query. */
    private fun stubActivityFeedQuery(
        gymId: String,
        block: (limitedQuery: Query, listenerReg: ListenerRegistration) -> Unit
    ) {
        val gymCollection = mockk<CollectionReference>()
        val gymDoc = mockk<DocumentReference>()
        val activityCollection = mockk<CollectionReference>()
        val orderedQuery = mockk<Query>()
        val limitedQuery = mockk<Query>()
        val listenerReg = mockk<ListenerRegistration>(relaxed = true)

        every { mockFirestore.collection("gyms") } returns gymCollection
        every { gymCollection.document(gymId) } returns gymDoc
        every { gymDoc.collection("activityFeed") } returns activityCollection
        every { activityCollection.orderBy("timestampMs", Query.Direction.DESCENDING) } returns orderedQuery
        every { orderedQuery.limit(50) } returns limitedQuery

        block(limitedQuery, listenerReg)
    }

    // ─── Offline / empty gymId (no Firestore interaction) ─────────────────────

    @Test
    fun test_get_workout_templates_throws_when_session_not_initialized() = runTest {
        every { mockSession.gymId } returns ""

        val exception = runCatching { repository.getWorkoutTemplates() }.exceptionOrNull()

        assertTrue(
            "getWorkoutTemplates must throw when session is not initialized",
            exception is IllegalStateException
        )
        assertTrue(exception!!.message?.contains("not initialized") == true)
    }

    @Test
    fun test_observe_client_roster_emits_empty_list_when_gym_id_empty() = runTest {
        every { mockSession.gymId } returns ""
        every { mockSession.trainerId } returns ""

        repository.observeClientRoster("trainer1").test {
            val items = awaitItem()
            assertTrue(
                "observeClientRoster must emit empty list when session not initialized",
                items.isEmpty()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_observe_activity_feed_emits_empty_list_when_gym_id_empty() = runTest {
        every { mockSession.gymId } returns ""

        repository.observeActivityFeed("trainer1").test {
            val items = awaitItem()
            assertTrue(
                "observeActivityFeed must emit empty list when session not initialized",
                items.isEmpty()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_observe_client_detail_emits_null_when_gym_id_empty() = runTest {
        every { mockSession.gymId } returns ""

        repository.observeClientDetail("c1").test {
            val detail = awaitItem()
            assertTrue(
                "observeClientDetail must emit null when session not initialized",
                detail == null
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_observe_chat_messages_emits_empty_list_when_not_ready() = runTest {
        every { mockSession.gymId } returns ""
        every { mockSession.trainerId } returns ""

        repository.observeChatMessages("c1").test {
            val messages = awaitItem()
            assertTrue(
                "observeChatMessages must emit empty list when session not initialized",
                messages.isEmpty()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun test_observe_client_roster_returns_empty_list_on_empty_snapshot() = runTest {
        every { mockSession.gymId } returns "gym1"
        every { mockSession.trainerId } returns "trainer1"

        val emptySnapshot = mockk<QuerySnapshot> {
            every { documents } returns emptyList()
        }

        stubMembersQuery("gym1", "trainer1") { query, listenerReg ->
            every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
                firstArg<EventListener<QuerySnapshot>>().onEvent(emptySnapshot, null)
                listenerReg
            }
        }

        repository.observeClientRoster("trainer1").test {
            val items = awaitItem()
            assertTrue(
                "Empty Firestore snapshot must yield an empty list, NOT fake data",
                items.isEmpty()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_observe_client_roster_propagates_firestore_error() = runTest {
        every { mockSession.gymId } returns "gym1"
        every { mockSession.trainerId } returns "trainer1"
        val firestoreError = mockk<FirebaseFirestoreException>(relaxed = true)
        every { firestoreError.toString() } returns "firestore roster error"

        stubMembersQuery("gym1", "trainer1") { query, listenerReg ->
            every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
                firstArg<EventListener<QuerySnapshot>>().onEvent(
                    null, firestoreError
                )
                listenerReg
            }
        }

        repository.observeClientRoster("trainer1").test {
            val error = awaitError()
            assertTrue(error === firestoreError)
        }
    }

    @Test
    fun test_observe_client_roster_propagates_error_when_snapshot_is_null() = runTest {
        every { mockSession.gymId } returns "gym1"
        every { mockSession.trainerId } returns "trainer1"

        stubMembersQuery("gym1", "trainer1") { query, listenerReg ->
            every { query.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
                // Both null — treated same as an error
                firstArg<EventListener<QuerySnapshot>>().onEvent(null, null)
                listenerReg
            }
        }

        repository.observeClientRoster("trainer1").test {
            val error = awaitError()
            assertTrue(error is IllegalStateException)
        }
    }

    // ─── observeActivityFeed — Firestore snapshot behaviour ──────────────────

    @Test
    fun test_observe_activity_feed_returns_empty_list_on_empty_snapshot() = runTest {
        every { mockSession.gymId } returns "gym1"

        val emptySnapshot = mockk<QuerySnapshot> {
            every { documents } returns emptyList()
        }

        stubActivityFeedQuery("gym1") { limitedQuery, listenerReg ->
            every { limitedQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
                firstArg<EventListener<QuerySnapshot>>().onEvent(emptySnapshot, null)
                listenerReg
            }
        }

        repository.observeActivityFeed("trainer1").test {
            val items = awaitItem()
            assertTrue(
                "Empty activity snapshot is valid (no recent activity), must NOT fall back to fake data",
                items.isEmpty()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun test_observe_activity_feed_propagates_firestore_error() = runTest {
        every { mockSession.gymId } returns "gym1"
        val firestoreError = mockk<FirebaseFirestoreException>(relaxed = true)
        every { firestoreError.toString() } returns "firestore feed error"

        stubActivityFeedQuery("gym1") { limitedQuery, listenerReg ->
            every { limitedQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
                firstArg<EventListener<QuerySnapshot>>().onEvent(
                    null, firestoreError
                )
                listenerReg
            }
        }

        repository.observeActivityFeed("trainer1").test {
            val error = awaitError()
            assertTrue(error === firestoreError)
        }
    }

    // ─── getWorkoutTemplates — exception fallback ─────────────────────────────

    @Test
    fun test_get_workout_templates_rethrows_on_firestore_exception() = runTest {
        every { mockSession.gymId } returns "gym1"

        val gymCollection = mockk<CollectionReference>()
        val gymDoc = mockk<DocumentReference>()
        val templatesCollection = mockk<CollectionReference>()

        every { mockFirestore.collection("gyms") } returns gymCollection
        every { gymCollection.document("gym1") } returns gymDoc
        every { gymDoc.collection("workoutTemplates") } returns templatesCollection
        // Make `.get()` throw before await() is ever called
        every { templatesCollection.get() } throws RuntimeException("Firestore unavailable")

        val exception = runCatching { repository.getWorkoutTemplates() }.exceptionOrNull()

        assertTrue(
            "getWorkoutTemplates must rethrow Firestore exceptions — never swallow silently",
            exception is RuntimeException
        )
        assertEquals("Firestore unavailable", exception!!.message)
    }
}
