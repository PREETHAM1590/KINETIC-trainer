package com.kinetic.trainer.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kinetic.trainer.data.ChatEncryption
import com.kinetic.trainer.data.SessionManager
import com.kinetic.trainer.data.fake.FakeTrainerData
import com.kinetic.trainer.data.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTrainerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : TrainerRepository {

    private fun gymRef() = firestore.collection("gyms").document(sessionManager.gymId)
    private fun isReady() = sessionManager.gymId.isNotEmpty() && sessionManager.trainerId.isNotEmpty()

    private suspend fun getOrCreateConversationKey(conversationId: String): String {
        val convRef = firestore.collection("conversations").document(conversationId)
        val snap = convRef.get().await()
        val existing = snap.getString("keyBase64")
        if (existing != null) return existing
        val newKey = ChatEncryption.generateKeyBase64()
        convRef.set(
            mapOf(
                "keyBase64" to newKey,
                "gymId" to sessionManager.gymId,
                "createdAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge()
        ).await()
        return newKey
    }

    override fun observeActivityFeed(trainerId: String): Flow<List<ActivityEvent>> {
        if (sessionManager.gymId.isEmpty()) return flow { emit(FakeTrainerData.activityFeed) }
        return callbackFlow {
            val reg = gymRef()
                .collection("activityFeed")
                .orderBy("timestampMs", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(FakeTrainerData.activityFeed)
                        return@addSnapshotListener
                    }
                    val events = snapshot.documents.mapNotNull { it.toActivityEvent() }
                    trySend(events.ifEmpty { FakeTrainerData.activityFeed })
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeClientRoster(trainerId: String): Flow<List<ClientSummary>> {
        if (sessionManager.gymId.isEmpty()) return flow { emit(FakeTrainerData.clients) }
        return callbackFlow {
            val reg = gymRef()
                .collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(FakeTrainerData.clients)
                        return@addSnapshotListener
                    }
                    val clients = snapshot.documents.mapNotNull { it.toClientSummary() }
                    trySend(clients.ifEmpty { FakeTrainerData.clients })
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeClientDetail(clientId: String): Flow<ClientDetail?> {
        if (sessionManager.gymId.isEmpty()) return flow { emit(FakeTrainerData.clientDetails[clientId]) }
        return callbackFlow {
            val reg = gymRef()
                .collection("members")
                .document(clientId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(FakeTrainerData.clientDetails[clientId])
                        return@addSnapshotListener
                    }
                    trySend(snapshot.toClientDetail() ?: FakeTrainerData.clientDetails[clientId])
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeChatMessages(clientId: String): Flow<List<ChatMessage>> {
        if (!isReady()) return flow { emit(FakeTrainerData.chatMessages[clientId] ?: emptyList()) }
        return flow {
            val conversationId = listOf(sessionManager.trainerId, clientId).sorted().joinToString("_")
            val keyBase64 = try {
                getOrCreateConversationKey(conversationId)
            } catch (e: Exception) {
                emit(FakeTrainerData.chatMessages[clientId] ?: emptyList())
                return@flow
            }
            emitAll(callbackFlow {
                val reg = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestampMs", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) {
                            trySend(FakeTrainerData.chatMessages[clientId] ?: emptyList())
                            return@addSnapshotListener
                        }
                        val msgs = snapshot.documents.mapNotNull { it.toChatMessage(keyBase64) }
                        trySend(msgs.ifEmpty { FakeTrainerData.chatMessages[clientId] ?: emptyList() })
                    }
                awaitClose { reg.remove() }
            })
        }
    }

    override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> {
        if (sessionManager.gymId.isEmpty()) return FakeTrainerData.workoutTemplates
        return try {
            val snapshot = gymRef().collection("workoutTemplates").get().await()
            val templates = snapshot.documents.mapNotNull { it.toWorkoutTemplate() }
            templates.ifEmpty { FakeTrainerData.workoutTemplates }
        } catch (_: Exception) {
            FakeTrainerData.workoutTemplates
        }
    }

    override suspend fun assignWorkout(workout: AssignedWorkout) {
        if (sessionManager.gymId.isEmpty()) return
        try {
            gymRef()
                .collection("members").document(workout.clientId)
                .collection("assignments")
                .add(workout.toFirestoreMap())
                .await()
        } catch (_: Exception) {}
    }

    override suspend fun sendMessage(clientId: String, message: ChatMessage) {
        if (!isReady()) return
        try {
            val conversationId = listOf(message.senderId, clientId).sorted().joinToString("_")
            val keyBase64 = getOrCreateConversationKey(conversationId)
            val ciphertextBase64 = ChatEncryption.encrypt(keyBase64, message.text)

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(message.id)
                .set(
                    mapOf(
                        "senderId" to message.senderId,
                        "ciphertextBase64" to ciphertextBase64,
                        "timestampMs" to message.timestampMs,
                        "isFromTrainer" to message.isFromTrainer,
                        "isRead" to message.isRead,
                    )
                ).await()
        } catch (_: Exception) {}
    }
}

// ─── Firestore → Model mappers ────────────────────────────────────────────────

private fun DocumentSnapshot.toActivityEvent(): ActivityEvent? {
    val clientId = getString("clientId") ?: return null
    val clientName = getString("clientName") ?: return null
    val message = getString("message") ?: return null
    val typeStr = getString("type") ?: return null
    val type = try { ActivityEventType.valueOf(typeStr) } catch (_: Exception) { return null }
    return ActivityEvent(
        id = id,
        clientId = clientId,
        clientName = clientName,
        type = type,
        message = message,
        timestampMs = getLong("timestampMs") ?: System.currentTimeMillis()
    )
}

private fun DocumentSnapshot.toClientSummary(): ClientSummary? {
    val name = getString("name") ?: return null
    return ClientSummary(
        clientId = id,
        name = name,
        consecutiveMissedSessions = getLong("consecutiveMissedSessions")?.toInt() ?: 0,
        performanceTrendPct = getDouble("performanceTrendPct")?.toFloat() ?: 0f,
        hasActiveWorkoutPlan = getBoolean("hasActiveWorkoutPlan") ?: false,
        daysSinceLastVisit = getLong("daysSinceLastVisit")?.toInt() ?: 0,
        weeklyCalorieAdherencePct = getDouble("weeklyCalorieAdherencePct")?.toFloat() ?: 1f,
        currentStreakDays = getLong("currentStreakDays")?.toInt() ?: 0,
        injuryFlags = toInjuryFlags(),
        unreadMessages = getLong("unreadMessages")?.toInt() ?: 0,
        lastActivityMs = getLong("lastActivityMs") ?: System.currentTimeMillis()
    )
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toInjuryFlags(): List<InjuryFlag> {
    val raw = get("injuryFlags") as? List<Map<String, Any>> ?: return emptyList()
    return raw.mapNotNull { map ->
        val bodyPartStr = map["bodyPart"] as? String ?: return@mapNotNull null
        val description = map["description"] as? String ?: return@mapNotNull null
        val severityStr = map["severity"] as? String ?: Severity.MONITOR.name
        val bodyPart = try { BodyPart.valueOf(bodyPartStr) } catch (_: Exception) { return@mapNotNull null }
        val severity = try { Severity.valueOf(severityStr) } catch (_: Exception) { Severity.MONITOR }
        InjuryFlag(
            bodyPart = bodyPart,
            description = description,
            severity = severity,
            reportedAt = (map["reportedAt"] as? Long) ?: System.currentTimeMillis()
        )
    }
}

private fun DocumentSnapshot.toClientDetail(): ClientDetail? {
    val summary = toClientSummary() ?: return null
    return ClientDetail(
        summary = summary,
        todayCalories = getLong("todayCalories")?.toInt() ?: 0,
        targetCalories = getLong("targetCalories")?.toInt() ?: 2200,
        attendanceLast30Days = getLong("attendanceLast30Days")?.toInt() ?: 0,
        totalSessions = getLong("totalSessions")?.toInt() ?: 0,
        weightKg = getDouble("weightKg")?.toFloat(),
        progressPhotoCount = getLong("progressPhotoCount")?.toInt() ?: 0,
        unreadMessages = getLong("unreadMessages")?.toInt() ?: 0
    )
}

private fun DocumentSnapshot.toChatMessage(keyBase64: String): ChatMessage? {
    return try {
        val senderId = getString("senderId") ?: return null
        val ciphertextBase64 = getString("ciphertextBase64") ?: return null
        val text = try {
            ChatEncryption.decrypt(keyBase64, ciphertextBase64)
        } catch (e: Exception) {
            // Legacy fallback: try reading plaintext "text" field for old messages
            getString("text") ?: return null
        }
        ChatMessage(
            id = id,
            senderId = senderId,
            text = text,
            timestampMs = getLong("timestampMs") ?: System.currentTimeMillis(),
            isFromTrainer = getBoolean("isFromTrainer") ?: false,
            isRead = getBoolean("isRead") ?: false,
        )
    } catch (e: Exception) {
        null
    }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toWorkoutTemplate(): WorkoutTemplate? {
    val name = getString("name") ?: return null
    val description = getString("description") ?: ""
    val durationMinutes = getLong("durationMinutes")?.toInt() ?: 0
    val exerciseMaps = get("exercises") as? List<Map<String, Any>> ?: emptyList()
    val exercises = exerciseMaps.mapNotNull { map ->
        val eName = map["name"] as? String ?: return@mapNotNull null
        Exercise(
            name = eName,
            sets = (map["sets"] as? Long)?.toInt() ?: 3,
            repsPerSet = (map["repsPerSet"] as? Long)?.toInt() ?: 10,
            targetWeightKg = (map["targetWeightKg"] as? Double)?.toFloat() ?: 0f,
            notes = map["notes"] as? String ?: ""
        )
    }
    return WorkoutTemplate(id = id, name = name, description = description,
        exercises = exercises, durationMinutes = durationMinutes)
}

// ─── Model → Firestore serializers ────────────────────────────────────────────

private fun AssignedWorkout.toFirestoreMap(): Map<String, Any> = mapOf(
    "clientId" to clientId,
    "clientName" to clientName,
    "assignedForDateMs" to assignedForDateMs,
    "templateId" to (templateId ?: ""),
    "exercises" to exercises.map { ex ->
        mapOf(
            "name" to ex.name,
            "sets" to ex.sets,
            "repsPerSet" to ex.repsPerSet,
            "targetWeightKg" to ex.targetWeightKg,
            "notes" to ex.notes
        )
    }
)
