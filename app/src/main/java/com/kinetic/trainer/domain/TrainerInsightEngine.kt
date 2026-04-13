package com.kinetic.trainer.domain

import com.kinetic.trainer.data.models.ClientSummary
import com.kinetic.trainer.domain.insights.TrainerInsightRuleEngine
import javax.inject.Inject
import javax.inject.Singleton

enum class InsightSeverity { INFO, WARNING, URGENT }

data class TrainerInsight(
    val message: String,
    val clientId: String? = null,
    val severity: InsightSeverity,
    val actionLabel: String? = null
)

@Singleton
class TrainerInsightEngine @Inject constructor() : TrainerInsightRuleEngine {

    override fun generateInsights(clients: List<ClientSummary>): List<TrainerInsight> {
        val insights = mutableListOf<TrainerInsight>()

        for (client in clients) {
            // Rule 1: URGENT — 2+ consecutive missed sessions
            if (client.consecutiveMissedSessions >= 2) {
                insights += TrainerInsight(
                    message = "⚠️ ${client.name} has missed ${client.consecutiveMissedSessions} sessions — at risk of dropout",
                    clientId = client.clientId,
                    severity = InsightSeverity.URGENT,
                    actionLabel = "View client"
                )
            }

            // Rule 2: WARNING — performance dropped more than 15%
            if (client.lastPerformanceDelta < -0.15f) {
                val pct = String.format("%.0f", Math.abs(client.lastPerformanceDelta) * 100)
                insights += TrainerInsight(
                    message = "${client.name}'s performance dropped ${pct}% — check in",
                    clientId = client.clientId,
                    severity = InsightSeverity.WARNING,
                    actionLabel = "Message client"
                )
            }

            // Rule 3: INFO — exceeded targets 4+ times in a row
            if (client.exceededTargetConsecutively >= 4) {
                insights += TrainerInsight(
                    message = "💪 ${client.name} has exceeded targets ${client.exceededTargetConsecutively}× in a row — time to increase weight",
                    clientId = client.clientId,
                    severity = InsightSeverity.INFO,
                    actionLabel = "Assign workout"
                )
            }

            // Rule 4: WARNING — no workout assigned and last assignment was >7 days ago
            if (!client.hasWorkoutThisWeek && client.daysSinceLastAssignment > 7) {
                insights += TrainerInsight(
                    message = "${client.name} has no workout assigned this week",
                    clientId = client.clientId,
                    severity = InsightSeverity.WARNING,
                    actionLabel = "Assign workout"
                )
            }
        }

        // Rule 5: INFO — aggregate count of clients with no workout this week
        val noWorkoutCount = clients.count { !it.hasWorkoutThisWeek }
        if (noWorkoutCount > 0) {
            insights += TrainerInsight(
                message = "$noWorkoutCount client${if (noWorkoutCount > 1) "s" else ""} have no workout this week",
                clientId = null,
                severity = InsightSeverity.INFO,
                actionLabel = "Review roster"
            )
        }

        return insights.sortedByDescending { it.severity.ordinal }
    }
}
