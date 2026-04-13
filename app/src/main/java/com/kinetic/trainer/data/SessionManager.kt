package com.kinetic.trainer.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = createPreferences()

    private fun createPreferences(): SharedPreferences {
        val isDebuggable = runCatching {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }.getOrDefault(true)

        if (isDebuggable) {
            // Keep debug/test builds resilient in environments where Android Keystore is unstable.
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                "Encrypted session storage unavailable in non-debug build",
                e
            )
        }
    }

    var trainerId: String
        get() = prefs.getString(KEY_TRAINER_ID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_TRAINER_ID, value) }

    var gymId: String
        get() = prefs.getString(KEY_GYM_ID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_GYM_ID, value) }

    fun isLoggedIn(): Boolean = trainerId.isNotEmpty()

    fun clearSession() {
        prefs.edit { clear() }
    }

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "kinetic_session_enc"
        private const val KEY_TRAINER_ID = "trainer_id"
        private const val KEY_GYM_ID = "gym_id"
    }
}