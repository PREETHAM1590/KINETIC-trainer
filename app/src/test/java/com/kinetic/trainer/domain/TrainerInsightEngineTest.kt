package com.kinetic.trainer.domain

import com.kinetic.trainer.data.models.ClientSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrainerInsightEngineTest {

    private lateinit var engine: TrainerInsightEngine

    @Before
    fun setUp() {
        engine = TrainerInsightEngine()
    }

    private fun baseClient(
        clientId: String = "c1",
        name: String = "Test User",
        consecutiveMissedSessions: Int = 0,
        lastPerformanceDelta: Float = 0f,
        hasWorkoutThisWeek: Boolean = true,
        daysSinceLastVisit: Int = 0,
        exceededTargetConsecutively: Int = 0,
        daysSinceLastAssignment: Int = 0
    ) = ClientSummary(
        clientId = clientId,
        name = name,
        consecutiveMissedSessions = consecutiveMissedSessions,
        performanceTrendPct = lastPerformanceDelta,
        hasActiveWorkoutPlan = hasWorkoutThisWeek,
        // daysSinceLastAssignment alias = if (hasActiveWorkoutPlan) 0 else daysSinceLastVisit
        // so to get a specific daysSinceLastAssignment value when hasWorkout=false, pass it as daysSinceLastVisit
        daysSinceLastVisit = if (!hasWorkoutThisWeek && daysSinceLastAssignment > 0) daysSinceLastAssignment else daysSinceLastVisit,
        // exceededTargetConsecutively alias = if (weeklyCalorieAdherencePct >= 1.15f) 4 else 0
        weeklyCalorieAdherencePct = if (exceededTargetConsecutively >= 4) 1.20f else 1.0f,
        currentStreakDays = 0,
        injuryFlags = emptyList(),
        unreadMessages = 0
    )

    @Test
    fun `no insights when all clients healthy`() {
        val clients = listOf(baseClient())
        val insights = engine.generateInsights(clients)
        // Only the aggregate rule 5 — but hasWorkoutThisWeek = true so noWorkoutCount = 0
        assertTrue(insights.isEmpty())
    }

    @Test
    fun `URGENT for 2 consecutive missed sessions`() {
        val clients = listOf(baseClient(name = "Rahul", consecutiveMissedSessions = 2))
        val insights = engine.generateInsights(clients)
        val urgent = insights.find { it.severity == InsightSeverity.URGENT }
        assertNotNull(urgent)
        assertTrue(urgent!!.message.contains("Rahul"))
        assertTrue(urgent.message.contains("2"))
    }

    @Test
    fun `URGENT for 3 consecutive missed sessions`() {
        val clients = listOf(baseClient(name = "Arjun", consecutiveMissedSessions = 3))
        val insights = engine.generateInsights(clients)
        val urgent = insights.find { it.severity == InsightSeverity.URGENT }
        assertNotNull(urgent)
        assertTrue(urgent!!.message.contains("3"))
    }

    @Test
    fun `no URGENT when only 1 missed session`() {
        val clients = listOf(baseClient(consecutiveMissedSessions = 1))
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.severity == InsightSeverity.URGENT })
    }

    @Test
    fun `WARNING for performance drop greater than 15 percent`() {
        val clients = listOf(baseClient(name = "Priya", lastPerformanceDelta = -0.20f))
        val insights = engine.generateInsights(clients)
        val warning = insights.find { it.severity == InsightSeverity.WARNING && it.message.contains("Priya") && it.message.contains("performance") }
        assertNotNull(warning)
        assertTrue(warning!!.message.contains("20"))
    }

    @Test
    fun `no WARNING for performance drop exactly at threshold`() {
        val clients = listOf(baseClient(lastPerformanceDelta = -0.15f))
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.severity == InsightSeverity.WARNING && it.message.contains("performance") })
    }

    @Test
    fun `no WARNING for small performance drop`() {
        val clients = listOf(baseClient(lastPerformanceDelta = -0.10f))
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.message.contains("performance") })
    }

    @Test
    fun `INFO for exceeding targets 4 times consecutively`() {
        val clients = listOf(baseClient(name = "Meena", exceededTargetConsecutively = 4))
        val insights = engine.generateInsights(clients)
        val info = insights.find { it.severity == InsightSeverity.INFO && it.message.contains("Meena") }
        assertNotNull(info)
        assertTrue(info!!.message.contains("4"))
        assertTrue(info.message.contains("weight"))
    }

    @Test
    fun `INFO for exceeding targets 6 times`() {
        val clients = listOf(baseClient(name = "Arjun", exceededTargetConsecutively = 6))
        val insights = engine.generateInsights(clients)
        // alias maps any value >= 4 → returns 4 (boolean threshold)
        val info = insights.find { it.severity == InsightSeverity.INFO && it.message.contains("Arjun") }
        assertNotNull(info)
    }

    @Test
    fun `no INFO when exceeded targets only 3 times`() {
        val clients = listOf(baseClient(exceededTargetConsecutively = 3))
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.message.contains("exceeded targets") })
    }

    @Test
    fun `WARNING when no workout this week and last assignment over 7 days ago`() {
        val clients = listOf(baseClient(name = "Rahul", hasWorkoutThisWeek = false, daysSinceLastAssignment = 8))
        val insights = engine.generateInsights(clients)
        val warning = insights.find { it.severity == InsightSeverity.WARNING && it.message.contains("Rahul") && it.message.contains("no workout") }
        assertNotNull(warning)
    }

    @Test
    fun `no individual WARNING when no workout but last assignment within 7 days`() {
        val clients = listOf(baseClient(hasWorkoutThisWeek = false, daysSinceLastAssignment = 5))
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.message.contains("no workout assigned this week") && it.clientId != null })
    }

    @Test
    fun `aggregate INFO when one client has no workout this week`() {
        val clients = listOf(baseClient(hasWorkoutThisWeek = false, daysSinceLastAssignment = 3))
        val insights = engine.generateInsights(clients)
        val agg = insights.find { it.clientId == null && it.message.contains("client") && it.message.contains("no workout") }
        assertNotNull(agg)
        assertEquals(InsightSeverity.INFO, agg!!.severity)
        assertTrue(agg.message.contains("1"))
    }

    @Test
    fun `aggregate INFO counts multiple clients with no workout`() {
        val clients = listOf(
            baseClient("c1", "Alice", hasWorkoutThisWeek = false),
            baseClient("c2", "Bob", hasWorkoutThisWeek = false),
            baseClient("c3", "Charlie", hasWorkoutThisWeek = true)
        )
        val insights = engine.generateInsights(clients)
        val agg = insights.find { it.clientId == null && it.message.contains("no workout") }
        assertNotNull(agg)
        assertTrue(agg!!.message.contains("2"))
    }

    @Test
    fun `no aggregate INFO when all clients have workout this week`() {
        val clients = listOf(
            baseClient("c1", hasWorkoutThisWeek = true),
            baseClient("c2", hasWorkoutThisWeek = true)
        )
        val insights = engine.generateInsights(clients)
        assertNull(insights.find { it.clientId == null })
    }

    @Test
    fun `insights sorted with URGENT first`() {
        val clients = listOf(
            baseClient("c1", "Alice", consecutiveMissedSessions = 2),
            baseClient("c2", "Bob", lastPerformanceDelta = -0.20f)
        )
        val insights = engine.generateInsights(clients)
        val severities = insights.map { it.severity }
        val urgentIdx = severities.indexOfFirst { it == InsightSeverity.URGENT }
        val warningIdx = severities.indexOfFirst { it == InsightSeverity.WARNING }
        if (urgentIdx != -1 && warningIdx != -1) {
            assertTrue("URGENT should come before WARNING", urgentIdx < warningIdx)
        }
    }

    @Test
    fun `empty client list produces no insights`() {
        val insights = engine.generateInsights(emptyList())
        assertTrue(insights.isEmpty())
    }

    @Test
    fun `URGENT insight has clientId set`() {
        val clients = listOf(baseClient("c99", consecutiveMissedSessions = 2))
        val urgent = engine.generateInsights(clients).find { it.severity == InsightSeverity.URGENT }
        assertNotNull(urgent)
        assertEquals("c99", urgent!!.clientId)
    }

    @Test
    fun `aggregate insight has null clientId`() {
        val clients = listOf(baseClient(hasWorkoutThisWeek = false))
        val agg = engine.generateInsights(clients).find { it.clientId == null }
        assertNotNull(agg)
        assertNull(agg!!.clientId)
    }
}
