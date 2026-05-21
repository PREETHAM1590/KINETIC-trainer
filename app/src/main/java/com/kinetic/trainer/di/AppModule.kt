package com.kinetic.trainer.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kinetic.trainer.data.repository.AuthRepository
import com.kinetic.trainer.data.repository.AuthRepositoryImpl
import com.kinetic.trainer.data.repository.FirebaseTrainerRepository
import com.kinetic.trainer.data.repository.TrainerRepository
import com.kinetic.trainer.domain.TrainerInsightEngine
import com.kinetic.trainer.domain.insights.TrainerInsightRuleEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindTrainerInsightRuleEngine(impl: TrainerInsightEngine): TrainerInsightRuleEngine

    companion object {

        @Provides
        @Singleton
        fun provideFirebaseApp(@ApplicationContext context: Context): FirebaseApp {
            return FirebaseApp.initializeApp(context)
                ?: error("FirebaseApp.initializeApp returned null — check google-services.json is included in the build.")
        }

        @Provides
        @Singleton
        fun provideFirebaseAuth(firebaseApp: FirebaseApp): FirebaseAuth =
            FirebaseAuth.getInstance(firebaseApp)

        @Provides
        @Singleton
        fun provideFirestore(firebaseApp: FirebaseApp): FirebaseFirestore =
            FirebaseFirestore.getInstance(firebaseApp)
    }
}