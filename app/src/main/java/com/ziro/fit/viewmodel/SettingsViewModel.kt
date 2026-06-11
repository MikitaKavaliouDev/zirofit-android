package com.ziro.fit.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.model.User
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.remote.ZiroApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val dailyTargetsEnabled: Boolean = false,
    val voiceFeedbackEnabled: Boolean = false,
    val routinesEnabled: Boolean = false,
    val isCustomModeEnabled: Boolean = false,
    val coachBannerEnabled: Boolean = true,
    val checkInBannerEnabled: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ZiroApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadFeatureFlags()
    }

    private fun loadFeatureFlags() {
        val dailyTargets = prefs.getBoolean(KEY_DAILY_TARGETS, false)
        val voiceFeedback = prefs.getBoolean(KEY_VOICE_FEEDBACK, false)
        val routines = prefs.getBoolean(KEY_ROUTINES, false)
        val customMode = prefs.getBoolean(KEY_CUSTOM_MODE, false)
        val coachBanner = prefs.getBoolean(KEY_COACH_BANNER, true)
        val checkInBanner = prefs.getBoolean(KEY_CHECKIN_BANNER, true)
        _uiState.update {
            it.copy(
                dailyTargetsEnabled = dailyTargets,
                voiceFeedbackEnabled = voiceFeedback,
                routinesEnabled = routines,
                isCustomModeEnabled = customMode,
                coachBannerEnabled = coachBanner,
                checkInBannerEnabled = checkInBanner
            )
        }
    }

    fun setDailyTargetsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_TARGETS, enabled).apply()
        _uiState.update { it.copy(dailyTargetsEnabled = enabled) }
    }

    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_FEEDBACK, enabled).apply()
        _uiState.update { it.copy(voiceFeedbackEnabled = enabled) }
    }

    fun setRoutinesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ROUTINES, enabled).apply()
        _uiState.update { it.copy(routinesEnabled = enabled) }
    }

    fun setCustomModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CUSTOM_MODE, enabled).apply()
        _uiState.update { it.copy(isCustomModeEnabled = enabled) }
    }

    fun setCoachBannerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COACH_BANNER, enabled).apply()
        _uiState.update { it.copy(coachBannerEnabled = enabled) }
    }

    fun setCheckInBannerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHECKIN_BANNER, enabled).apply()
        _uiState.update { it.copy(checkInBannerEnabled = enabled) }
    }

    companion object {
        private const val KEY_DAILY_TARGETS = "feature_daily_targets"
        private const val KEY_VOICE_FEEDBACK = "feature_voice_feedback"
        private const val KEY_ROUTINES = "feature_routines"
        private const val KEY_CUSTOM_MODE = "feature_custom_mode"
        private const val KEY_COACH_BANNER = "dashboard_coach_banner"
        private const val KEY_CHECKIN_BANNER = "dashboard_checkin_banner"
    }
}
