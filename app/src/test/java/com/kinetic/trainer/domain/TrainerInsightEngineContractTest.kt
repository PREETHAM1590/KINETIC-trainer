package com.kinetic.trainer.domain

import com.kinetic.trainer.data.models.ClientSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerInsightEngineContractTest {

    private val engine = TrainerInsightEngine()

    @Test
    fun contract_high_risk_clients_emit_urgent_prioritized_insight() {
        val insights = engine.generateInsights(
            listOf(
                ClientSummary(
                    clientId = "c1",
                    name = "Risk User",
                    consecutiveMissedSessions = 3
                )
            )
        )

        assertTrue(insights.isNotEmpty())
        assertEquals(InsightSeverity.URGENT, insights.first().severity)
    }

    @Test
    fun contract_healthy_client_emits_no_actionable_insights() {
        val insights = engine.generateInsights(
            listOf(
                ClientSummary(
                    clientId = "c2",
                    name = "Healthy User",
                    hasActiveWorkoutPlan = true,
                    consecutiveMissedSessions = 0,
                    performanceTrendPct = 0f,
                    weeklyCalorieAdherencePct = 1f
                )
            )
        )

        assertTrue(insights.isEmpty())
    }
}
