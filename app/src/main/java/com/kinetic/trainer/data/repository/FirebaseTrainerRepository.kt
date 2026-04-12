package com.kinetic.trainer.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
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

private const val TAG = "FirebaseTrainerRepo"

@Singleton
class FirebaseTrainerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : TrainerRepository {

    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    private fun gymRef() = firestore.collection("gyms").document(sessionManager.gymId)
    private fun isReady() = sessionManager.gymId.isNotEmpty() && sessionManager.trainerId.isNotEmpty()

    private fun buildConversationId(clientId: String): String {
        val gymId = sessionManager.gymId
        val trainerId = sessionManager.trainerId
        check(gymId.isNotEmpty() && trainerId.isNotEmpty() && clientId.isNotEmpty()) {
            "Session is not ready for chat operations"
        }
        val participants = listOf(trainerId, clientId).sorted()
        return listOf(gymId, participants[0], participants[1]).joinToString("_")
    }

    // Keys are stored in user_keys/{uid}/conversation_keys/{conversationId}
    // Separate per-user collection so key material is never co-located
    // with the conversation metadata or ciphertext that it protects.
    private suspend fun requestBackendKeyBootstrap(
        conversationId: String,
        clientId: String,
        keyBase64: String,
        createdAt: Long,
    ) {
        val payload = hashMapOf<String, Any>(
            "gymId" to sessionManager.gymId,
            "toUserId" to clientId,
            "conversationId" to conversationId,
            "keyBase64" to keyBase64,
            "createdAt" to createdAt,
        )
        functions
            .getHttpsCallable("bootstrapConversationKey")
            .call(payload)
            .await()
    }

    private suspend fun getOrCreateConversationKey(
        conversationId: String,
        clientId: String,
    ): String {
        val trainerId = sessionManager.trainerId
        val myKeyRef = firestore.collection("user_keys")
            .document(trainerId)
            .collection("conversation_keys")
            .document(conversationId)

        val snap = myKeyRef.get().await()
        val existing = snap.getString("keyBase64")
        if (existing != null) {
            val existingCreatedAt = snap.getLong("createdAt") ?: 0L
            requestBackendKeyBootstrap(conversationId, clientId, existing, existingCreatedAt)
            return existing
        }

        val newKey = ChatEncryption.generateKeyBase64()
        val createdAt = System.currentTimeMillis()
        val keyData = mapOf(
            "keyBase64" to newKey,
            "createdAt" to createdAt,
        )
        // Write to trainer's own key store
        myKeyRef.set(keyData).await()

        // Request backend-owned key distribution so clients do not fan out counterpart keys directly.
        requestBackendKeyBootstrap(conversationId, clientId, newKey, createdAt)

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
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        close(IllegalStateException("Activity feed listener returned null snapshot"))
                        return@addSnapshotListener
                    }
                    // Empty is a valid production state (no recent activity)
                    trySend(snapshot.documents.mapNotNull { it.toActivityEvent() })
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeClientRoster(trainerId: String): Flow<List<ClientSummary>> {
        if (sessionManager.gymId.isEmpty()) return flow { emit(FakeTrainerData.clients) }
        return callbackFlow {
            val reg = gymRef()
                .collection("members")
                .whereEqualTo("trainerId", trainerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        close(IllegalStateException("Client roster listener returned null snapshot"))
                        return@addSnapshotListener
                    }
                    // Empty is valid: trainer may have no assigned clients yet
                    trySend(snapshot.documents.mapNotNull { it.toClientSummary() })
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
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        close(IllegalStateException("Client detail listener returned null snapshot"))
                        return@addSnapshotListener
                    }
                    if (!snapshot.exists()) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(snapshot.toClientDetail())
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeChatMessages(clientId: String): Flow<List<ChatMessage>> {
        if (!isReady()) return flow { emit(FakeTrainerData.chatMessages[clientId] ?: emptyList()) }
        return flow {
            val conversationId = buildConversationId(clientId)
            val keyBase64 = try {
                getOrCreateConversationKey(conversationId, clientId)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to initialize conversation key", e)
            }
            emitAll(callbackFlow {
                val reg = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot == null) {
                            close(IllegalStateException("Chat listener returned null snapshot"))
                            return@addSnapshotListener
                        }
                        val msgs = snapshot.documents.mapNotNull { it.toChatMessage(keyBase64) }
                        // Empty conversation is valid (no messages yet)
                        trySend(msgs)
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
        } catch (e: Exception) {
            Log.e(TAG, "assignWorkout failed for client=${workout.clientId}", e)
            throw e
        }
    }

    override suspend fun sendMessage(clientId: String, message: ChatMessage) {
        if (!isReady()) {
            throw IllegalStateException("Session is not ready for chat operations")
        }
        try {
            val conversationId = buildConversationId(clientId)
            val keyBase64 = getOrCreateConversationKey(conversationId, clientId)
            val ciphertextBase64 = ChatEncryption.encrypt(keyBase64, message.text)
            val participants = listOf(message.senderId, clientId).sorted()

            // Ensure parent conversation doc exists with participants
            val conversationRef = firestore.collection("conversations").document(conversationId)
            val conversationDoc = conversationRef.get().await()
            if (!conversationDoc.exists()) {
                conversationRef.set(
                    mapOf(
                        "participants" to participants,
                        "gymId" to sessionManager.gymId,
                        "createdAt" to System.currentTimeMillis(),
                    )
                ).await()
            }

            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(message.id)
                .set(
                    mapOf(
                        "fromUserId" to message.senderId,
                        "senderId" to message.senderId,
                        "toUserId" to clientId,
                        "ciphertextBase64" to ciphertextBase64,
                        "timestamp" to message.timestampMs,
                        "timestampMs" to message.timestampMs,
                        "isFromTrainer" to message.isFromTrainer,
                        "isRead" to message.isRead,
                    )
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed for conversation with client=$clientId", e)
            throw e
        }
    }
}
// Firestore -> Model mappers

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

private fun DocumentSnapshot.resolveTimestampMs(vararg fields: String): Long? {
    for (field in fields) {
        getLong(field)?.let { return it }
        getTimestamp(field)?.let { return it.toDate().time }
    }
    return null
}

private fun DocumentSnapshot.toChatMessage(keyBase64: String): ChatMessage? {
    return try {
        val senderId = getString("fromUserId") ?: getString("senderId") ?: return null
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
            timestampMs = resolveTimestampMs("timestamp", "timestampMs") ?: System.currentTimeMillis(),
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

// Model -> Firestore serializers

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
