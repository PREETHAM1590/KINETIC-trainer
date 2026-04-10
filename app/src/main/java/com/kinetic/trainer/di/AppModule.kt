package com.kinetic.trainer.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kinetic.trainer.data.repository.AuthRepository
import com.kinetic.trainer.data.repository.AuthRepositoryImpl
import com.kinetic.trainer.data.repository.FirebaseTrainerRepository
import com.kinetic.trainer.data.repository.TrainerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTrainerRepository(impl: FirebaseTrainerRepository): TrainerRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    }
}
