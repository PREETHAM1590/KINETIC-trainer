package com.kinetic.trainer.data.repository

import com.kinetic.trainer.data.fake.FakeTrainerData
import com.kinetic.trainer.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface TrainerRepository {
    fun observeActivityFeed(trainerId: String): Flow<List<ActivityEvent>>
    fun observeClientRoster(trainerId: String): Flow<List<ClientSummary>>
    fun observeClientDetail(clientId: String): Flow<ClientDetail?>
    fun observeChatMessages(clientId: String): Flow<List<ChatMessage>>
    suspend fun getWorkoutTemplates(): List<WorkoutTemplate>
    suspend fun assignWorkout(workout: AssignedWorkout)
    suspend fun sendMessage(clientId: String, message: ChatMessage)
}

@Singleton
class FakeTrainerRepository @Inject constructor() : TrainerRepository {

    override fun observeActivityFeed(trainerId: String): Flow<List<ActivityEvent>> = flow {
        delay(300)
        emit(FakeTrainerData.activityFeed)
    }

    override fun observeClientRoster(trainerId: String): Flow<List<ClientSummary>> = flow {
        delay(300)
        emit(FakeTrainerData.clients)
    }

    override fun observeClientDetail(clientId: String): Flow<ClientDetail?> = flow {
        delay(200)
        emit(FakeTrainerData.clientDetails[clientId])
    }

    override fun observeChatMessages(clientId: String): Flow<List<ChatMessage>> = flow {
        delay(200)
        emit(FakeTrainerData.chatMessages[clientId] ?: emptyList())
    }

    override suspend fun getWorkoutTemplates(): List<WorkoutTemplate> {
        delay(200)
        return FakeTrainerData.workoutTemplates
    }

    override suspend fun assignWorkout(workout: AssignedWorkout) {
        delay(500)
    }

    override suspend fun sendMessage(clientId: String, message: ChatMessage) {
        delay(200)
    }
}
