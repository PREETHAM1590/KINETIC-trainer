package com.kinetic.trainer.data.repository

import android.util.Log
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class ConsentSource(val wireValue: String) {
    SETTINGS_CHANGE("settings_change"),
}

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

private const val TAG = "PrivacyRepository"

private fun warn(message: String) {
    try {
        Log.w(TAG, message)
    } catch (_: RuntimeException) {
        // android.util.Log is unavailable in plain JVM unit tests.
    }
}

private fun nestedMap(parent: Map<*, *>?, key: String): Map<*, *>? {
    if (parent == null) return null
    val raw = parent[key] ?: return null
    return if (raw is Map<*, *>) {
        raw
    } else {
        warn("Unexpected type for '$key': ${raw::class.java.simpleName}")
        null
    }
}

private fun boolValue(map: Map<*, *>, key: String, default: Boolean): Boolean {
    val raw = map[key] ?: return default
    return when (raw) {
        is Boolean -> raw
        is Number -> {
            val converted = raw.toInt() != 0
            warn("Coerced numeric boolean for '$key': $raw -> $converted")
            converted
        }
        is String -> {
            val normalized = raw.trim().lowercase()
            when (normalized) {
                "true", "1", "yes" -> {
                    warn("Coerced string boolean for '$key': $raw -> true")
                    true
                }
                "false", "0", "no" -> {
                    warn("Coerced string boolean for '$key': $raw -> false")
                    false
                }
                else -> {
                    warn("Invalid boolean payload for '$key': $raw")
                    default
                }
            }
        }
        else -> {
            warn("Invalid boolean payload for '$key': ${raw::class.java.simpleName}")
            default
        }
    }
}

private fun intValue(map: Map<*, *>, key: String, default: Int): Int {
    val raw = map[key] ?: return default
    return when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()?.also {
            warn("Coerced string number for '$key': $raw -> $it")
        } ?: run {
            warn("Invalid int payload for '$key': $raw")
            default
        }
        else -> {
            warn("Invalid int payload for '$key': ${raw::class.java.simpleName}")
            default
        }
    }
}

private fun longValue(map: Map<*, *>, key: String): Long? {
    val raw = map[key] ?: return null
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()?.also {
            warn("Coerced string long for '$key': $raw -> $it")
        } ?: run {
            warn("Invalid long payload for '$key': $raw")
            null
        }
        else -> {
            warn("Invalid long payload for '$key': ${raw::class.java.simpleName}")
            null
        }
    }
}

private fun stringValue(map: Map<*, *>, key: String, default: String): String {
    val raw = map[key] ?: return default
    return if (raw is String) {
        raw
    } else {
        warn("Invalid string payload for '$key': ${raw::class.java.simpleName}")
        default
    }
}

internal fun mapConsentPayload(raw: Map<*, *>?): ConsentPreferences {
    val consent = nestedMap(raw, "consent") ?: return ConsentPreferences()
    return ConsentPreferences(
        analyticsEnabled = boolValue(consent, "analyticsEnabled", default = false),
        marketingEnabled = boolValue(consent, "marketingEnabled", default = false),
        crashReportingEnabled = boolValue(consent, "crashReportingEnabled", default = true),
        version = intValue(consent, "version", default = 1),
    )
}

internal fun mapDeletionStatusPayload(raw: Map<*, *>?): DeletionStatusData {
    val status = nestedMap(raw, "status") ?: return DeletionStatusData()
    val state = stringValue(status, "status", default = "unknown")
    return DeletionStatusData(
        status = state,
        backendClearedAt = longValue(status, "backendClearedAt"),
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
        source: ConsentSource = ConsentSource.SETTINGS_CHANGE,
    ) {
        Firebase.functions
            .getHttpsCallable("updateUserConsent")
            .call(
                mapOf(
                    "analyticsEnabled" to analyticsEnabled,
                    "marketingEnabled" to marketingEnabled,
                    "crashReportingEnabled" to crashReportingEnabled,
                    "source" to source.wireValue,
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
