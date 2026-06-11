package com.ziro.fit.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NotificationPrefsUiState(
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val workoutReminders: Boolean = true,
    val bookingAlerts: Boolean = true,
    val trainerAlertsInClientMode: Boolean = true,
    val clientAlertsInTrainerMode: Boolean = true
)

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(NotificationPrefsUiState())
    val uiState: StateFlow<NotificationPrefsUiState> = _uiState.asStateFlow()

    init {
        loadPrefs()
    }

    private fun loadPrefs() {
        _uiState.update {
            it.copy(
                pushNotifications = prefs.getBoolean(KEY_PUSH, true),
                emailNotifications = prefs.getBoolean(KEY_EMAIL, true),
                workoutReminders = prefs.getBoolean(KEY_WORKOUT_REMINDERS, true),
                bookingAlerts = prefs.getBoolean(KEY_BOOKING_ALERTS, true),
                trainerAlertsInClientMode = prefs.getBoolean(KEY_TRAINER_IN_CLIENT, true),
                clientAlertsInTrainerMode = prefs.getBoolean(KEY_CLIENT_IN_TRAINER, true)
            )
        }
    }

    fun setPushNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PUSH, enabled).apply()
        _uiState.update { it.copy(pushNotifications = enabled) }
    }

    fun setEmailNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMAIL, enabled).apply()
        _uiState.update { it.copy(emailNotifications = enabled) }
    }

    fun setWorkoutReminders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WORKOUT_REMINDERS, enabled).apply()
        _uiState.update { it.copy(workoutReminders = enabled) }
    }

    fun setBookingAlerts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BOOKING_ALERTS, enabled).apply()
        _uiState.update { it.copy(bookingAlerts = enabled) }
    }

    fun setTrainerAlertsInClientMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRAINER_IN_CLIENT, enabled).apply()
        _uiState.update { it.copy(trainerAlertsInClientMode = enabled) }
    }

    fun setClientAlertsInTrainerMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLIENT_IN_TRAINER, enabled).apply()
        _uiState.update { it.copy(clientAlertsInTrainerMode = enabled) }
    }

    companion object {
        private const val KEY_PUSH = "notif_push"
        private const val KEY_EMAIL = "notif_email"
        private const val KEY_WORKOUT_REMINDERS = "notif_workout_reminders"
        private const val KEY_BOOKING_ALERTS = "notif_booking_alerts"
        private const val KEY_TRAINER_IN_CLIENT = "notif_trainer_in_client"
        private const val KEY_CLIENT_IN_TRAINER = "notif_client_in_trainer"
    }
}
