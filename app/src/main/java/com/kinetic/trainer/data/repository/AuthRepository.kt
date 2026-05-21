package com.kinetic.trainer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.kinetic.trainer.data.models.AuthResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val ROLE_TRAINER = "trainer"

interface AuthRepository {
    val currentUser: Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut()
    fun isSignedIn(): Boolean
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Sign in failed: missing user")

            // Validate trainer role via custom claims
            val tokenResult = user.getIdToken(true).await()
            val claims = tokenResult.claims
            val role = claims["role"] as? String
            if (role != ROLE_TRAINER) {
                firebaseAuth.signOut()
                return AuthResult.Error("Access denied: this app is for trainers only")
            }
            val gymId = claims["gymId"] as? String ?: ""
            AuthResult.Success(userId = user.uid, gymId = gymId)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    override suspend fun signOut() {
        try {
            Firebase.functions
                .getHttpsCallable("unregisterFcmToken")
                .call(mapOf("reason" to "logout"))
                .await()
        } catch (_: Exception) {
            // Keep logout resilient even when token cleanup fails.
        }
        firebaseAuth.signOut()
    }

    override fun isSignedIn(): Boolean = firebaseAuth.currentUser != null
}

