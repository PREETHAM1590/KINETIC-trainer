package com.kinetic.trainer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TrainerMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "trainer_alerts"
        private const val CHANNEL_NAME = "Trainer Alerts"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_PENDING_TOKEN = "pending_fcm_token"
        private val ALLOWED_TYPES = setOf("client_message", "missed_sessions", "personal_best")

        fun retryPendingToken(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_PENDING_TOKEN, null) ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Firebase.functions
                        .getHttpsCallable("registerFcmToken")
                        .call(mapOf("token" to token, "platform" to "android", "app" to "trainer"))
                        .await()
                    prefs.edit().remove(KEY_PENDING_TOKEN).apply()
                } catch (_: Exception) { /* will retry next launch */ }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: return
        if (type !in ALLOWED_TYPES) return

        val title = remoteMessage.data["title"] ?: "KINETIC Trainer"
        val body = remoteMessage.data["body"] ?: return
        val clientId = remoteMessage.data["clientId"]
        showNotification(title, body, clientId)
    }

    private fun showNotification(title: String, body: String, clientId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Trainer client alerts and messages"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            clientId?.let { putExtra("clientId", it) }
        }
        val requestCode = clientId?.hashCode() ?: System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_TOKEN, token).apply()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Firebase.functions
                    .getHttpsCallable("registerFcmToken")
                    .call(mapOf("token" to token, "platform" to "android", "app" to "trainer"))
                    .await()
                prefs.edit().remove(KEY_PENDING_TOKEN).apply()
            } catch (_: Exception) { /* saved to prefs, will retry on next app launch */ }
        }
    }
}
