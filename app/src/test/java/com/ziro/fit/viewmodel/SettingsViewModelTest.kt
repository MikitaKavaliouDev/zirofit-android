package com.ziro.fit.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk()
    private val api: ZiroApi = mockk()
    private val tokenManager: TokenManager = mockk()
    private val prefs: SharedPreferences = mockk()
    private val prefsEditor: SharedPreferences.Editor = mockk()

    @Before
    fun setup() {
        clearAllMocks()
        every { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.putBoolean(any(), any()) } returns prefsEditor
        every { prefsEditor.apply() } just Runs
    }

    @Test
    fun `init loads default feature flags as false`() {
        every { prefs.getBoolean(any(), any()) } returns false

        val viewModel = SettingsViewModel(context, api, tokenManager)

        val state = viewModel.uiState.value
        assertFalse(state.dailyTargetsEnabled)
        assertFalse(state.voiceFeedbackEnabled)
        assertFalse(state.routinesEnabled)
        assertFalse(state.isCustomModeEnabled)
    }

    @Test
    fun `init loads saved banner states as true by default`() {
        every { prefs.getBoolean("dashboard_coach_banner", true) } returns true
        every { prefs.getBoolean("dashboard_checkin_banner", true) } returns true
        every { prefs.getBoolean(any(), eq(false)) } returns false

        val viewModel = SettingsViewModel(context, api, tokenManager)

        assertTrue(viewModel.uiState.value.coachBannerEnabled)
        assertTrue(viewModel.uiState.value.checkInBannerEnabled)
    }

    @Test
    fun `setDailyTargetsEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setDailyTargetsEnabled(true)

        assertTrue(viewModel.uiState.value.dailyTargetsEnabled)
        verify { prefsEditor.putBoolean("feature_daily_targets", true) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `setVoiceFeedbackEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setVoiceFeedbackEnabled(true)

        assertTrue(viewModel.uiState.value.voiceFeedbackEnabled)
        verify { prefsEditor.putBoolean("feature_voice_feedback", true) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `setRoutinesEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setRoutinesEnabled(true)

        assertTrue(viewModel.uiState.value.routinesEnabled)
        verify { prefsEditor.putBoolean("feature_routines", true) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `setCustomModeEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setCustomModeEnabled(true)

        assertTrue(viewModel.uiState.value.isCustomModeEnabled)
        verify { prefsEditor.putBoolean("feature_custom_mode", true) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `setCoachBannerEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setCoachBannerEnabled(false)

        assertFalse(viewModel.uiState.value.coachBannerEnabled)
        verify { prefsEditor.putBoolean("dashboard_coach_banner", false) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `setCheckInBannerEnabled updates state and persists`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setCheckInBannerEnabled(false)

        assertFalse(viewModel.uiState.value.checkInBannerEnabled)
        verify { prefsEditor.putBoolean("dashboard_checkin_banner", false) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `toggle feature flags multiple times correctly`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        // Enable then disable daily targets
        viewModel.setDailyTargetsEnabled(true)
        assertTrue(viewModel.uiState.value.dailyTargetsEnabled)

        viewModel.setDailyTargetsEnabled(false)
        assertFalse(viewModel.uiState.value.dailyTargetsEnabled)
    }

    @Test
    fun `all feature flags start independently`() {
        every { prefs.getBoolean(any(), any()) } returns false
        val viewModel = SettingsViewModel(context, api, tokenManager)

        viewModel.setCustomModeEnabled(true)
        viewModel.setCoachBannerEnabled(false)

        assertTrue(viewModel.uiState.value.isCustomModeEnabled)
        assertFalse(viewModel.uiState.value.coachBannerEnabled)
        assertFalse(viewModel.uiState.value.dailyTargetsEnabled)
        assertFalse(viewModel.uiState.value.voiceFeedbackEnabled)
        assertFalse(viewModel.uiState.value.routinesEnabled)
    }

    @Test
    fun `loads all persisted values from prefs on init`() {
        every { prefs.getBoolean("feature_daily_targets", false) } returns true
        every { prefs.getBoolean("feature_voice_feedback", false) } returns true
        every { prefs.getBoolean("feature_routines", false) } returns true
        every { prefs.getBoolean("feature_custom_mode", false) } returns true
        every { prefs.getBoolean("dashboard_coach_banner", true) } returns false
        every { prefs.getBoolean("dashboard_checkin_banner", true) } returns false

        val viewModel = SettingsViewModel(context, api, tokenManager)

        val state = viewModel.uiState.value
        assertTrue(state.dailyTargetsEnabled)
        assertTrue(state.voiceFeedbackEnabled)
        assertTrue(state.routinesEnabled)
        assertTrue(state.isCustomModeEnabled)
        assertFalse(state.coachBannerEnabled)
        assertFalse(state.checkInBannerEnabled)
    }
}
