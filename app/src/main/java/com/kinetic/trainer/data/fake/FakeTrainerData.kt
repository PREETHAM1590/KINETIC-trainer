package com.kinetic.trainer.data.fake

import com.kinetic.trainer.data.models.*

object FakeTrainerData {

    val clients = listOf(
        ClientSummary(
            clientId = "c1", name = "Priya Sharma",
            consecutiveMissedSessions = 0, performanceTrendPct = 0.08f,
            hasActiveWorkoutPlan = true, daysSinceLastVisit = 1,
            weeklyCalorieAdherencePct = 1.20f, currentStreakDays = 12,
            injuryFlags = emptyList(), unreadMessages = 2,
            lastActivityMs = System.currentTimeMillis() - 86_400_000L
        ),
        ClientSummary(
            clientId = "c2", name = "Rahul Verma",
            consecutiveMissedSessions = 3, performanceTrendPct = -0.20f,
            hasActiveWorkoutPlan = false, daysSinceLastVisit = 5,
            weeklyCalorieAdherencePct = 0.55f, currentStreakDays = 0,
            injuryFlags = listOf(InjuryFlag(BodyPart.KNEE, "Mild patellar pain", Severity.MODIFIED_ONLY)),
            unreadMessages = 0, lastActivityMs = System.currentTimeMillis() - 5 * 86_400_000L
        ),
        ClientSummary(
            clientId = "c3", name = "Arjun Nair",
            consecutiveMissedSessions = 0, performanceTrendPct = 0.15f,
            hasActiveWorkoutPlan = true, daysSinceLastVisit = 0,
            weeklyCalorieAdherencePct = 1.07f, currentStreakDays = 21,
            injuryFlags = emptyList(), unreadMessages = 1,
            lastActivityMs = System.currentTimeMillis()
        ),
        ClientSummary(
            clientId = "c4", name = "Meena Patel",
            consecutiveMissedSessions = 1, performanceTrendPct = -0.05f,
            hasActiveWorkoutPlan = false, daysSinceLastVisit = 2,
            weeklyCalorieAdherencePct = 0.92f, currentStreakDays = 3,
            injuryFlags = listOf(InjuryFlag(BodyPart.SHOULDER, "Rotator cuff strain", Severity.CANNOT_USE)),
            unreadMessages = 0, lastActivityMs = System.currentTimeMillis() - 2 * 86_400_000L
        )
    )

    val activityFeed = listOf(
        ActivityEvent("e1", "c3", "Arjun Nair", ActivityEventType.PERSONAL_BEST,
            "Arjun hit new squat PR 💪 — 120kg", System.currentTimeMillis() - 30 * 60_000L),
        ActivityEvent("e2", "c1", "Priya Sharma", ActivityEventType.WORKOUT_COMPLETED,
            "Priya completed Leg Day ✅", System.currentTimeMillis() - 2 * 3_600_000L),
        ActivityEvent("e3", "c2", "Rahul Verma", ActivityEventType.WORKOUT_MISSED,
            "Rahul missed workout ❌", System.currentTimeMillis() - 4 * 3_600_000L),
        ActivityEvent("e4", "c4", "Meena Patel", ActivityEventType.MEAL_LOGGED,
            "Meena logged breakfast 🥗", System.currentTimeMillis() - 6 * 3_600_000L),
        ActivityEvent("e5", "c3", "Arjun Nair", ActivityEventType.STREAK_MILESTONE,
            "Arjun hit a 21-day streak 🔥", System.currentTimeMillis() - 8 * 3_600_000L)
    )

    val workoutTemplates = listOf(
        WorkoutTemplate(
            id = "t1", name = "PPL Day 1 — Push",
            description = "Chest, shoulders, triceps compound + isolation",
            exercises = listOf(
                Exercise("Bench Press", 4, 8, 60f),
                Exercise("Overhead Press", 3, 10, 40f),
                Exercise("Incline Dumbbell Press", 3, 12, 24f),
                Exercise("Lateral Raise", 3, 15, 10f),
                Exercise("Tricep Pushdown", 3, 15, 20f)
            ), durationMinutes = 55
        ),
        WorkoutTemplate(
            id = "t2", name = "PPL Day 2 — Pull",
            description = "Back and biceps",
            exercises = listOf(
                Exercise("Deadlift", 4, 5, 100f),
                Exercise("Pull-up", 4, 8, 0f),
                Exercise("Barbell Row", 3, 10, 60f),
                Exercise("Face Pull", 3, 15, 15f),
                Exercise("Bicep Curl", 3, 12, 16f)
            ), durationMinutes = 60
        ),
        WorkoutTemplate(
            id = "t3", name = "Beginner Full Body",
            description = "3-day full body for beginners",
            exercises = listOf(
                Exercise("Squat", 3, 10, 40f),
                Exercise("Bench Press", 3, 10, 40f),
                Exercise("Lat Pulldown", 3, 12, 40f),
                Exercise("Shoulder Press", 3, 12, 20f),
                Exercise("Romanian Deadlift", 3, 10, 40f)
            ), durationMinutes = 45
        )
    )

    val clientDetails: Map<String, ClientDetail> = mapOf(
        "c1" to ClientDetail(
            summary = clients[0],
            thisWeekWorkout = AssignedWorkout(
                clientId = "c1", clientName = "Priya Sharma",
                exercises = workoutTemplates[1].exercises,
                assignedForDateMs = System.currentTimeMillis() - 2 * 86_400_000L
            ),
            lastSessionActual = CompletedWorkout(
                clientId = "c1",
                completedAtMs = System.currentTimeMillis() - 86_400_000L,
                exercises = listOf(
                    CompletedExercise("Deadlift", 4, 5, 75f),
                    CompletedExercise("Barbell Row", 3, 10, 65f)
                ),
                durationMins = 62, caloriesBurned = 380
            ),
            todayCalories = 1850, targetCalories = 2000,
            attendanceLast30Days = 26, totalSessions = 84, weightKg = 58.2f,
            progressPhotoCount = 5, unreadMessages = 2
        ),
        "c2" to ClientDetail(
            summary = clients[1],
            thisWeekWorkout = null, lastSessionActual = null,
            todayCalories = 1200, targetCalories = 2200,
            attendanceLast30Days = 12, totalSessions = 38, weightKg = 82.0f,
            progressPhotoCount = 1, unreadMessages = 0
        ),
        "c3" to ClientDetail(
            summary = clients[2],
            thisWeekWorkout = AssignedWorkout(
                clientId = "c3", clientName = "Arjun Nair",
                exercises = workoutTemplates[0].exercises,
                assignedForDateMs = System.currentTimeMillis() - 86_400_000L
            ),
            lastSessionActual = CompletedWorkout(
                clientId = "c3",
                completedAtMs = System.currentTimeMillis() - 3_600_000L,
                exercises = listOf(
                    CompletedExercise("Bench Press", 4, 8, 92.5f),
                    CompletedExercise("Overhead Press", 3, 10, 42.5f)
                ),
                durationMins = 58, caloriesBurned = 420
            ),
            todayCalories = 3200, targetCalories = 3000,
            attendanceLast30Days = 29, totalSessions = 142, weightKg = 78.5f,
            progressPhotoCount = 12, unreadMessages = 1
        ),
        "c4" to ClientDetail(
            summary = clients[3],
            thisWeekWorkout = null, lastSessionActual = null,
            todayCalories = 1650, targetCalories = 1800,
            attendanceLast30Days = 20, totalSessions = 61, weightKg = 62.5f,
            progressPhotoCount = 3, unreadMessages = 0
        )
    )

    val chatMessages = mapOf(
        "c1" to listOf(
            ChatMessage("m1", "trainer_01", "Hi Priya! Great leg day today 💪", System.currentTimeMillis() - 3_600_000L, true),
            ChatMessage("m2", "c1", "Thank you! Finally hit the depth you wanted!", System.currentTimeMillis() - 3_000_000L, false),
            ChatMessage("m3", "trainer_01", "Perfect. Let's bump squats to 70kg next session.", System.currentTimeMillis() - 2_400_000L, true),
            ChatMessage("m4", "c1", "Sounds great! See you Thursday 🙏", System.currentTimeMillis() - 1_800_000L, false)
        )
    )
}
