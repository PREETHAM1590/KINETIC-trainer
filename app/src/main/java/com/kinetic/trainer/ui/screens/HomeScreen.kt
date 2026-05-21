package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetic.trainer.data.models.*
import com.kinetic.trainer.ui.components.*
import com.kinetic.trainer.ui.theme.*
import com.kinetic.trainer.ui.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onClientClick: (String) -> Unit,
    onPrivacyClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // Request notification permission on Android 13+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { _ -> }
        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feed = uiState.activityFeed
    val clients = uiState.clients
    val topInsight = uiState.insights.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding(),
    ) {
        // Error banner
        uiState.error?.let { errorMsg ->
            Surface(
                color = Error.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMsg,
                    color = Error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KINETIC",
                    color = Lime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "Trainer",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-1).sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()),
                    color = TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onPrivacyClick) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy settings",
                        tint = TextMuted,
                    )
                }
                IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log out",
                        tint = TextMuted,
                    )
                }
            }
        }

        topInsight?.let { insight ->
            TrainerInsightBanner(
                insight = insight,
                onActionClick = { insight.clientId?.let { onClientClick(it) } },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
            )
        }

        KineticCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TODAY'S ACTIVITY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(12.dp))
                if (feed.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No activity yet today", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(feed) { event -> ActivityEventRow(event) }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "YOUR CLIENTS  •  ${clients.size}",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(clients) { client -> ClientRosterCard(client, onClientClick) }
            }
        }
    }
}

@Composable
private fun ActivityEventRow(event: ActivityEvent) {
    val (icon, iconColor) = when (event.type) {
        ActivityEventType.WORKOUT_COMPLETED -> Icons.Default.CheckCircle to Lime
        ActivityEventType.WORKOUT_MISSED -> Icons.Default.Cancel to Error
        ActivityEventType.PERSONAL_BEST -> Icons.Default.EmojiEvents to Lime
        ActivityEventType.STREAK_MILESTONE -> Icons.Default.LocalFireDepartment to Warning
        ActivityEventType.MEAL_LOGGED -> Icons.Default.Restaurant to TextMuted
        ActivityEventType.NEW_MESSAGE -> Icons.Default.Message to Lime
        ActivityEventType.WEIGHT_UPDATED -> Icons.Default.Monitor to TextMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.message, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                formatTime(event.timestampMs),
                color = TextMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ClientRosterCard(client: ClientSummary, onClick: (String) -> Unit) {
    val hasUrgent = client.consecutiveMissedSessions >= 2
    val hasInjury = client.injuryFlags.any { it.severity == Severity.CANNOT_USE }

    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick(client.clientId) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        client.avatarInitials,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                }
                if (hasUrgent || hasInjury) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (hasUrgent) Urgent else Warning)
                            .align(Alignment.TopEnd),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                client.name.split(" ").first(),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    null,
                    tint = if (client.currentStreakDays > 0) Lime else TextMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text("${client.currentStreakDays}d", color = TextMuted, fontSize = 11.sp)
            }
            if (hasInjury) {
                Spacer(Modifier.height(4.dp))
                Text("Injury", color = Warning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
    }
}

