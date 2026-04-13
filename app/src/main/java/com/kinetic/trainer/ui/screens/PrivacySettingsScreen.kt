package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetic.trainer.ui.components.KineticCard
import com.kinetic.trainer.ui.components.ScreenHeadline
import com.kinetic.trainer.ui.components.StartButton
import com.kinetic.trainer.ui.theme.Background
import com.kinetic.trainer.ui.theme.Error
import com.kinetic.trainer.ui.theme.Lime
import com.kinetic.trainer.ui.theme.Surface2
import com.kinetic.trainer.ui.theme.TextMuted
import com.kinetic.trainer.ui.theme.TextPrimary
import com.kinetic.trainer.ui.viewmodels.PrivacyViewModel

@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeadline(title = "PRIVACY", subtitle = "CONSENT • EXPORT • DEVICE TOKEN")

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(color = Lime)
            Spacer(modifier = Modifier.height(16.dp))
        }

        KineticCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                PrivacyToggleRow(
                    title = "Usage Analytics",
                    subtitle = "Allow anonymized trainer usage analytics",
                    checked = uiState.analyticsEnabled,
                    onToggle = { viewModel.toggleAnalytics() },
                )
                HorizontalDivider(color = Surface2)
                PrivacyToggleRow(
                    title = "Marketing Updates",
                    subtitle = "Receive product and feature announcements",
                    checked = uiState.marketingEnabled,
                    onToggle = { viewModel.toggleMarketing() },
                )
                HorizontalDivider(color = Surface2)
                PrivacyToggleRow(
                    title = "Crash Reporting",
                    subtitle = "Share crash diagnostics for reliability",
                    checked = uiState.crashReportingEnabled,
                    onToggle = { viewModel.toggleCrashReporting() },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        StartButton(
            text = if (uiState.isSaving) "SAVING..." else "SAVE CONSENT",
            onClick = viewModel::saveConsent,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        StartButton(
            text = if (uiState.isSaving) "WORKING..." else "REQUEST DATA EXPORT",
            onClick = viewModel::requestDataExport,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        StartButton(
            text = if (uiState.isSaving) "WORKING..." else "DISABLE PUSH ON THIS DEVICE",
            onClick = viewModel::disablePushForThisDevice,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        StartButton(
            text = "REFRESH DELETION STATUS",
            onClick = viewModel::refreshDeletionStatus,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        KineticCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Deletion Status: ${uiState.deletionStatus.status}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (uiState.deletionStatus.completed) {
                        "Deletion cleanup verified complete"
                    } else {
                        "Cleanup verification pending"
                    },
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                if (uiState.latestExportId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latest Export ID: ${uiState.latestExportId}",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (uiState.message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.message ?: "",
                color = Lime,
                fontSize = 14.sp,
            )
        }

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.error ?: "",
                color = Error,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StartButton(
            text = "BACK",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}
