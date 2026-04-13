package com.kinetic.trainer.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyRepositoryTest {

    @Test
    fun mapConsentPayload_defaults_when_payload_missing() {
        val consent = mapConsentPayload(null)

        assertThat(consent.analyticsEnabled).isFalse()
        assertThat(consent.marketingEnabled).isFalse()
        assertThat(consent.crashReportingEnabled).isTrue()
        assertThat(consent.version).isEqualTo(1)
    }

    @Test
    fun mapConsentPayload_reads_server_fields() {
        val consent = mapConsentPayload(
            mapOf(
                "consent" to mapOf(
                    "analyticsEnabled" to true,
                    "marketingEnabled" to true,
                    "crashReportingEnabled" to false,
                    "version" to 2,
                )
            )
        )

        assertThat(consent.analyticsEnabled).isTrue()
        assertThat(consent.marketingEnabled).isTrue()
        assertThat(consent.crashReportingEnabled).isFalse()
        assertThat(consent.version).isEqualTo(2)
    }

    @Test
    fun mapDeletionStatusPayload_marks_completion_only_for_verified_state() {
        val pending = mapDeletionStatusPayload(mapOf("status" to mapOf("status" to "client_cleanup_pending")))
        val completed = mapDeletionStatusPayload(mapOf("status" to mapOf("status" to "verified_complete")))

        assertThat(pending.completed).isFalse()
        assertThat(completed.completed).isTrue()
    }
}
