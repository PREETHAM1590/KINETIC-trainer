package com.kinetic.trainer.data

import android.content.Context
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
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kinetic_session_enc",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
        private const val KEY_TRAINER_ID = "trainer_id"
        private const val KEY_GYM_ID = "gym_id"
    }
}
