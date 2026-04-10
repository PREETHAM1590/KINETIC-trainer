package com.kinetic.trainer.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("kinetic_session", Context.MODE_PRIVATE)

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
