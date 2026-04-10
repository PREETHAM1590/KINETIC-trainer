package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetic.trainer.data.models.*
import com.kinetic.trainer.ui.components.*
import com.kinetic.trainer.ui.theme.*
import com.kinetic.trainer.ui.viewmodels.ClientDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onAssignWorkout: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.detail

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        detail?.summary?.name ?: "",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        detail?.summary?.let { onOpenChat(it.clientId) }
                    }) {
                        Icon(Icons.Default.Message, "Chat", tint = Lime)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { detail?.summary?.let { onAssignWorkout(it.clientId) } },
                containerColor = Lime, contentColor = Background
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Assign Workout", fontWeight = FontWeight.Black)
                }
            }
        }
    ) { padding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Lime)
            }
            return@Scaffold
        }

        val d = detail!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Injury warning banner
            if (d.summary.injuryFlags.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Active limitations: " + d.summary.injuryFlags.joinToString { it.bodyPart.name },
                            color = Warning, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Workout Card ─────────────────────────────────────────────────
            DashboardCard(title = "THIS WEEK'S WORKOUT", icon = Icons.Default.FitnessCenter) {
                if (d.thisWeekWorkout == null) {
                    Text("No workout assigned yet", color = TextMuted, fontSize = 14.sp)
                } else {
                    d.thisWeekWorkout.exercises.take(3).forEach { ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ex.name, color = TextPrimary, fontSize = 14.sp)
                            Text(
                                "${ex.sets}×${ex.repsPerSet} @ ${ex.targetWeightKg}kg",
                                color = TextMuted, fontSize = 13.sp
                            )
                        }
                    }
                    if ((d.thisWeekWorkout.exercises.size) > 3) {
                        Text(
                            "+${d.thisWeekWorkout.exercises.size - 3} more exercises",
                            color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Compare with actual if available
                d.lastSessionActual?.let { actual ->
                    Spacer(Modifier.height(8.dp))
                    Divider(color = Surface2)
                    Spacer(Modifier.height(8.dp))
                    Text("LAST SESSION ACTUAL", color = TextMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    actual.exercises.take(2).forEach { ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ex.name, color = TextPrimary, fontSize = 13.sp)
                            Text(
                                "${ex.setsCompleted}×${ex.repsCompleted} @ ${ex.actualWeightKg}kg",
                                color = Lime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Diet Card ────────────────────────────────────────────────────
            DashboardCard(title = "DIET TODAY", icon = Icons.Default.Restaurant) {
                val pct = if (d.targetCalories > 0) d.todayCalories.toFloat() / d.targetCalories else 0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProgressRing(
                        progress = pct.coerceIn(0f, 1f),
                        size = 64.dp, strokeWidth = 8.dp,
                        content = {
                            Text(
                                "${(pct * 100).toInt()}%",
                                color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${d.todayCalories} kcal consumed", color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text("Target: ${d.targetCalories} kcal", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            // ── Consistency Card ─────────────────────────────────────────────
            DashboardCard(title = "CONSISTENCY", icon = Icons.Default.BarChart) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatChip("Streak", "${d.summary.currentStreakDays}d 🔥")
                    StatChip("Last 30d", "${d.attendanceLast30Days}/30")
                    StatChip("Total", "${d.totalSessions} sessions")
                }
            }

            // ── Metrics Card ─────────────────────────────────────────────────
            DashboardCard(title = "METRICS", icon = Icons.Default.Monitor) {
                d.weightKg?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatChip("Weight", "${it}kg")
                        StatChip("Last visit", "${d.summary.daysSinceLastVisit}d ago")
                    }
                } ?: Text("No metrics recorded", color = TextMuted, fontSize = 14.sp)
            }

            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    KineticCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Lime, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextMuted, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}
