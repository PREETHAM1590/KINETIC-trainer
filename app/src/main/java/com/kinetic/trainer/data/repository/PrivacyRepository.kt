package com.kinetic.trainer.data.repository

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ConsentPreferences(
    val analyticsEnabled: Boolean = false,
    val marketingEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true,
    val version: Int = 1,
)

data class DeletionStatusData(
    val status: String = "unknown",
    val backendClearedAt: Long? = null,
    val completed: Boolean = false,
)

internal fun mapConsentPayload(raw: Map<*, *>?): ConsentPreferences {
    val consent = raw?.get("consent") as? Map<*, *> ?: return ConsentPreferences()
    return ConsentPreferences(
        analyticsEnabled = consent["analyticsEnabled"] as? Boolean ?: false,
        marketingEnabled = consent["marketingEnabled"] as? Boolean ?: false,
        crashReportingEnabled = consent["crashReportingEnabled"] as? Boolean ?: true,
        version = (consent["version"] as? Number)?.toInt() ?: 1,
    )
}

internal fun mapDeletionStatusPayload(raw: Map<*, *>?): DeletionStatusData {
    val status = raw?.get("status") as? Map<*, *> ?: return DeletionStatusData()
    val state = status["status"] as? String ?: "unknown"
    return DeletionStatusData(
        status = state,
        backendClearedAt = (status["backendClearedAt"] as? Number)?.toLong(),
        completed = state == "verified_complete",
    )
}

@Singleton
class PrivacyRepository @Inject constructor() {

    suspend fun getConsent(): ConsentPreferences {
        val raw = Firebase.functions
            .getHttpsCallable("getUserConsent")
            .call()
            .await()
            .data as? Map<*, *>

        return mapConsentPayload(raw)
    }

    suspend fun updateConsent(
        analyticsEnabled: Boolean,
        marketingEnabled: Boolean,
        crashReportingEnabled: Boolean,
        source: String = "trainer_settings_change",
    ) {
        Firebase.functions
            .getHttpsCallable("updateUserConsent")
            .call(
                mapOf(
                    "analyticsEnabled" to analyticsEnabled,
                    "marketingEnabled" to marketingEnabled,
                    "crashReportingEnabled" to crashReportingEnabled,
                    "source" to source,
                ),
            )
            .await()
    }

    suspend fun requestDataExport(format: String = "json"): String {
        val raw = Firebase.functions
            .getHttpsCallable("generateDataExport")
            .call(mapOf("format" to format))
            .await()
            .data as? Map<*, *>
            ?: return ""

        return raw["exportId"] as? String ?: ""
    }

    suspend fun getDeletionStatus(): DeletionStatusData {
        val raw = Firebase.functions
            .getHttpsCallable("getDeletionStatus")
            .call()
            .await()
            .data as? Map<*, *>

        return mapDeletionStatusPayload(raw)
    }

    suspend fun unregisterToken(reason: String) {
        Firebase.functions
            .getHttpsCallable("unregisterFcmToken")
            .call(mapOf("reason" to reason))
            .await()
    }
}
