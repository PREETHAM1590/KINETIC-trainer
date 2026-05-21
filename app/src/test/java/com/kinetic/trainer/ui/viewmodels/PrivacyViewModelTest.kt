package com.kinetic.trainer.ui.viewmodels

import com.google.common.truth.Truth.assertThat
import com.kinetic.trainer.data.repository.ConsentPreferences
import com.kinetic.trainer.data.repository.ConsentSource
import com.kinetic.trainer.data.repository.DeletionStatusData
import com.kinetic.trainer.data.repository.PrivacyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loads_consent_and_deletion_status() = runTest {
        val repository = mockk<PrivacyRepository>()
        coEvery { repository.getConsent() } returns ConsentPreferences(
            analyticsEnabled = true,
            marketingEnabled = false,
            crashReportingEnabled = true,
            version = 2,
        )
        coEvery { repository.getDeletionStatus() } returns DeletionStatusData(
            status = "client_cleanup_pending",
            backendClearedAt = 42L,
            completed = false,
        )

        val viewModel = PrivacyViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.analyticsEnabled).isTrue()
        assertThat(state.consentVersion).isEqualTo(2)
        assertThat(state.deletionStatus.status).isEqualTo("client_cleanup_pending")
    }

    @Test
    fun saveConsent_persists_current_toggles() = runTest {
        val repository = mockk<PrivacyRepository>()
        coEvery { repository.getConsent() } returns ConsentPreferences()
        coEvery { repository.getDeletionStatus() } returns DeletionStatusData()
        coEvery {
            repository.updateConsent(
                analyticsEnabled = any(),
                marketingEnabled = any(),
                crashReportingEnabled = any(),
                source = any(),
            )
        } returns Unit

        val viewModel = PrivacyViewModel(repository)
        advanceUntilIdle()

        viewModel.toggleAnalytics()
        viewModel.toggleMarketing()
        viewModel.saveConsent()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateConsent(
                analyticsEnabled = true,
                marketingEnabled = true,
                crashReportingEnabled = true,
                source = ConsentSource.SETTINGS_CHANGE,
            )
        }
        assertThat(viewModel.uiState.value.message).isEqualTo("Consent preferences saved")
    }

    @Test
    fun disablePushForThisDevice_unregisters_token() = runTest {
        val repository = mockk<PrivacyRepository>()
        coEvery { repository.getConsent() } returns ConsentPreferences()
        coEvery { repository.getDeletionStatus() } returns DeletionStatusData()
        coEvery { repository.unregisterToken(reason = any()) } returns Unit

        val viewModel = PrivacyViewModel(repository)
        advanceUntilIdle()

        viewModel.disablePushForThisDevice()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.unregisterToken(reason = "privacy_opt_out") }
    }
}
