package com.kinetic.trainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctionsException
import com.kinetic.trainer.data.repository.ConsentSource
import com.kinetic.trainer.data.repository.DeletionStatusData
import com.kinetic.trainer.data.repository.PrivacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportFormat(val wireValue: String) {
    JSON("json"),
    CSV("csv"),
}

data class PrivacyUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val marketingEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = true,
    val consentVersion: Int = 1,
    val latestExportId: String = "",
    val deletionStatus: DeletionStatusData = DeletionStatusData(),
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val privacyRepository: PrivacyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    private fun toUserMessage(error: Throwable, fallback: String): String {
        return when (error) {
            is IOException -> "Network unavailable. Check your connection and try again."
            is SecurityException -> "Session expired. Please sign in again."
            is FirebaseFunctionsException -> when (error.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Session expired. Please sign in again."
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> "You do not have permission to change this setting."
                FirebaseFunctionsException.Code.UNAVAILABLE -> "Privacy service is temporarily unavailable. Please retry."
                else -> error.message ?: fallback
            }
            else -> error.message ?: fallback
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            try {
                val consent = privacyRepository.getConsent()
                val deletionStatus = privacyRepository.getDeletionStatus()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analyticsEnabled = consent.analyticsEnabled,
                    marketingEnabled = consent.marketingEnabled,
                    crashReportingEnabled = consent.crashReportingEnabled,
                    consentVersion = consent.version,
                    deletionStatus = deletionStatus,
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = toUserMessage(e, "Failed to load privacy settings"),
                )
            }
        }
    }

    fun toggleAnalytics() {
        _uiState.value = _uiState.value.copy(analyticsEnabled = !_uiState.value.analyticsEnabled)
    }

    fun toggleMarketing() {
        _uiState.value = _uiState.value.copy(marketingEnabled = !_uiState.value.marketingEnabled)
    }

    fun toggleCrashReporting() {
        _uiState.value = _uiState.value.copy(crashReportingEnabled = !_uiState.value.crashReportingEnabled)
    }

    fun saveConsent() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null, message = null)
            try {
                privacyRepository.updateConsent(
                    analyticsEnabled = state.analyticsEnabled,
                    marketingEnabled = state.marketingEnabled,
                    crashReportingEnabled = state.crashReportingEnabled,
                    source = ConsentSource.SETTINGS_CHANGE,
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Consent preferences saved",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = toUserMessage(e, "Failed to save consent"),
                )
            }
        }
    }

    fun requestDataExport(format: ExportFormat = ExportFormat.JSON) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, message = null)
            try {
                val exportId = privacyRepository.requestDataExport(format = format.wireValue)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    latestExportId = exportId,
                    message = if (exportId.isBlank()) {
                        "Export request submitted"
                    } else {
                        "Export generated: $exportId"
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = toUserMessage(e, "Failed to request export"),
                )
            }
        }
    }

    fun refreshDeletionStatus() {
        viewModelScope.launch {
            try {
                val status = privacyRepository.getDeletionStatus()
                _uiState.value = _uiState.value.copy(deletionStatus = status, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = toUserMessage(e, "Failed to load deletion status"),
                )
            }
        }
    }

    fun disablePushForThisDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, message = null)
            try {
                privacyRepository.unregisterToken(reason = "privacy_opt_out")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    message = "Push notifications disabled for this device",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = toUserMessage(e, "Failed to disable push notifications"),
                )
            }
        }
    }
}
