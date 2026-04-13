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

    @Test
    fun contract_empty_client_list_returns_empty_insight_set() {
        val insights = engine.generateInsights(emptyList())

        assertTrue(insights.isEmpty())
    }

    @Test
    fun contract_no_workout_clients_emit_aggregate_info_insight() {
        val insights = engine.generateInsights(
            listOf(
                ClientSummary(
                    clientId = "c3",
                    name = "Needs Plan",
                    hasActiveWorkoutPlan = false,
                    daysSinceLastVisit = 2,
                )
            )
        )

        val aggregate = insights.firstOrNull { it.clientId == null && it.severity == InsightSeverity.INFO }
        assertTrue(aggregate != null)
    }
}
