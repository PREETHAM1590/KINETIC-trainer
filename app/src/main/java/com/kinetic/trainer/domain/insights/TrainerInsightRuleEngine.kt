package com.kinetic.trainer.domain.insights

import com.kinetic.trainer.data.models.ClientSummary
import com.kinetic.trainer.domain.TrainerInsight

interface TrainerInsightRuleEngine {
    fun generateInsights(clients: List<ClientSummary>): List<TrainerInsight>
}
