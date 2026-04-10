package com.kinetic.trainer.data.models

// ─── Auth ─────────────────────────────────────────────────────────────────────

sealed class AuthResult {
    data class Success(val userId: String, val gymId: String = "") : AuthResult()
    data class Error(val message: String) : AuthResult()
}

// ─── Injury ───────────────────────────────────────────────────────────────────

enum class BodyPart { SHOULDER, KNEE, BACK, ANKLE, HIP, WRIST, ELBOW }

enum class Severity { CANNOT_USE, MODIFIED_ONLY, MONITOR }

data class InjuryFlag(
    val bodyPart: BodyPart,
    val description: String,
    val severity: Severity = Severity.MONITOR,
    val reportedAt: Long = System.currentTimeMillis()
)

// ─── Workouts ─────────────────────────────────────────────────────────────────

data class Exercise(
    val name: String,
    val sets: Int,
    val repsPerSet: Int,
    val targetWeightKg: Float,
    val notes: String = "",
    val isRestrictedFor: List<BodyPart> = emptyList()
)

data class CompletedExercise(
    val name: String,
    val setsCompleted: Int,
    val repsCompleted: Int,
    val actualWeightKg: Float
)

data class CompletedWorkout(
    val clientId: String,
    val completedAtMs: Long,
    val exercises: List<CompletedExercise>,
    val durationMins: Int = 0,
    val caloriesBurned: Int = 0
)

data class AssignedWorkout(
    val clientId: String,
    val clientName: String = "",
    val exercises: List<Exercise>,
    val assignedForDateMs: Long = System.currentTimeMillis(),
    val templateId: String? = null
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val exercises: List<Exercise>,
    val durationMinutes: Int
) {
    val durationMins: Int get() = durationMinutes
}

// ─── Client ───────────────────────────────────────────────────────────────────

data class ClientSummary(
    val clientId: String,
    val name: String,
    val consecutiveMissedSessions: Int = 0,
    val performanceTrendPct: Float = 0f,          // negative = dropped, e.g. -0.15 = -15%
    val hasActiveWorkoutPlan: Boolean = true,
    val daysSinceLastVisit: Int = 0,
    val weeklyCalorieAdherencePct: Float = 1f,    // 1.2 = 120% of target
    val currentStreakDays: Int = 0,
    val injuryFlags: List<InjuryFlag> = emptyList(),
    val unreadMessages: Int = 0,
    val lastActivityMs: Long = System.currentTimeMillis()
) {
    val avatarInitials: String get() = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    // Legacy aliases kept for TrainerInsightEngine
    val lastPerformanceDelta: Float get() = performanceTrendPct
    val hasWorkoutThisWeek: Boolean get() = hasActiveWorkoutPlan
    val exceededTargetConsecutively: Int get() =
        if (weeklyCalorieAdherencePct >= 1.15f) 4 else 0
    val daysSinceLastAssignment: Int get() = if (hasActiveWorkoutPlan) 0 else daysSinceLastVisit
}

data class ClientDetail(
    val summary: ClientSummary,
    val thisWeekWorkout: AssignedWorkout? = null,
    val lastSessionActual: CompletedWorkout? = null,
    val todayCalories: Int = 0,
    val targetCalories: Int = 2200,
    val attendanceLast30Days: Int = 0,
    val totalSessions: Int = 0,
    val weightKg: Float? = null,
    val progressPhotoCount: Int = 0,
    val unreadMessages: Int = 0
)

// ─── Activity Feed ────────────────────────────────────────────────────────────

enum class ActivityEventType {
    WORKOUT_COMPLETED, WORKOUT_MISSED, PERSONAL_BEST, STREAK_MILESTONE,
    MEAL_LOGGED, WEIGHT_UPDATED, NEW_MESSAGE
}

data class ActivityEvent(
    val id: String,
    val clientId: String,
    val clientName: String,
    val type: ActivityEventType,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)

// ─── Chat ─────────────────────────────────────────────────────────────────────

data class EncryptedMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val ciphertext: ByteArray,
    val timestampMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedMessage) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isFromTrainer: Boolean = true,
    val isRead: Boolean = false
)

// ─── Notifications ────────────────────────────────────────────────────────────

data class NotificationSettings(
    val clientMessages: Boolean = true,
    val missedSessions: Boolean = true,
    val personalBests: Boolean = true,
    val activityFeed: Boolean = false
)
