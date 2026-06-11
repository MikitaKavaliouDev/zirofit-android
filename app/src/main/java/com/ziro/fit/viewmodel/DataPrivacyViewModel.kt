package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.model.SharingPreferences
import com.ziro.fit.util.ApiErrorParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataPrivacyUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val preferences: SharingPreferences = SharingPreferences()
)

@HiltViewModel
class DataPrivacyViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPrivacyUiState())
    val uiState: StateFlow<DataPrivacyUiState> = _uiState.asStateFlow()

    init {
        fetchPreferences()
    }

    fun fetchPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSharingPreferences()
                .onSuccess { prefs ->
                    _uiState.update { it.copy(isLoading = false, preferences = prefs) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))
                        )
                    }
                }
        }
    }

    fun setShareWorkoutHistory(enabled: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(shareWorkoutHistory = enabled)) }
    }

    fun setShareBodyMeasurements(enabled: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(shareBodyMeasurements = enabled)) }
    }

    fun setShareCheckinNotes(enabled: Boolean) {
        _uiState.update { it.copy(preferences = it.preferences.copy(shareCheckinNotes = enabled)) }
    }

    fun setDataRetentionDays(days: Int) {
        _uiState.update { it.copy(preferences = it.preferences.copy(dataRetentionDays = days)) }
    }

    fun savePreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = false) }
            repository.updateSharingPreferences(_uiState.value.preferences)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, success = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))
                        )
                    }
                }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(success = false) }
    }
}
