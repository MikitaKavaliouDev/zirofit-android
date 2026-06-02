package com.ziro.fit.viewmodel

import android.app.Activity
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.model.AIWorkoutEvent
import com.ziro.fit.model.LiveWorkoutUiModel
import com.ziro.fit.model.VoiceCoachAgentState
import com.ziro.fit.model.VoiceCoachConnectionState
import com.ziro.fit.model.VoiceMessage
import com.ziro.fit.service.VoiceCoachService
import com.ziro.fit.service.WorkoutRealtimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * UI State for the voice training session screen.
 * Mirrors the iOS VoiceCoachService + WorkoutDashboard integration.
 */
data class VoiceTrainingUiState(
    /** ElevenLabs connection state */
    val connectionState: VoiceCoachConnectionState = VoiceCoachConnectionState.DISCONNECTED,
    /** Agent state (listening/thinking/speaking) */
    val agentState: VoiceCoachAgentState = VoiceCoachAgentState.UNKNOWN,
    /** Audio level for waveform visualization (0.0–1.0) */
    val audioLevel: Float = 0.0f,
    /** Conversation transcript messages */
    val messages: List<VoiceMessage> = emptyList(),
    /** Currently active workout session (from Realtime/Polling) */
    val activeSession: LiveWorkoutUiModel? = null,
    /** Whether polling/realtime is active */
    val isMonitoring: Boolean = false,
    /** Error message if any */
    val error: String? = null
)

@HiltViewModel
class VoiceTrainingViewModel @Inject constructor(
    private val voiceCoachService: VoiceCoachService,
    private val realtimeService: WorkoutRealtimeService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceTrainingUiState())
    val uiState: StateFlow<VoiceTrainingUiState> = _uiState.asStateFlow()

    init {
        // Observe connection state from VoiceCoachService
        viewModelScope.launch {
            voiceCoachService.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        error = if (state == VoiceCoachConnectionState.ERROR) "Voice Coach connection failed" else null
                    )
                }
            }
        }

        // Observe agent state
        viewModelScope.launch {
            voiceCoachService.agentState.collect { state ->
                _uiState.update { it.copy(agentState = state) }
            }
        }

        // Observe audio level
        viewModelScope.launch {
            voiceCoachService.audioLevel.collect { level ->
                _uiState.update { it.copy(audioLevel = level) }
            }
        }

        // Observe conversation messages
        viewModelScope.launch {
            voiceCoachService.messages.collect { message ->
                _uiState.update {
                    val updatedMessages = it.messages + message
                    it.copy(messages = updatedMessages)
                }
            }
        }

        // Observe realtime events
        viewModelScope.launch {
            realtimeService.events.collect { event ->
                handleAIEvent(event)
            }
        }

        // Observe active session updates
        viewModelScope.launch {
            realtimeService.activeSession.collect { session ->
                _uiState.update { it.copy(activeSession = session) }
            }
        }

        // Observe polling status
        viewModelScope.launch {
            realtimeService.isPolling.collect { polling ->
                _uiState.update { it.copy(isMonitoring = polling) }
            }
        }
    }

    /**
     * Configure the voice coach with the user ID from the JWT token.
     */
    fun configure() {
        val userId = getUserIdFromToken()
        if (!userId.isNullOrBlank()) {
            voiceCoachService.configure(userId)
        }
    }

    /**
     * Start the ElevenLabs conversation.
     * @param activity Required Activity context for WebRTC audio routing.
     */
    fun start(activity: Activity) {
        configure() // Set userId if available
        voiceCoachService.start(activity)
    }

    /** Stop the ElevenLabs conversation */
    fun stop() {
        voiceCoachService.stop()
    }

    /**
     * Toggle the ElevenLabs conversation on/off.
     * @param activity Activity context required only when connecting (DISCONNECTED/ERROR states).
     */
    fun toggle(activity: Activity? = null) {
        when (_uiState.value.connectionState) {
            VoiceCoachConnectionState.DISCONNECTED,
            VoiceCoachConnectionState.ERROR -> {
                configure() // Set userId if available
                voiceCoachService.toggle(activity)
            }
            VoiceCoachConnectionState.CONNECTED,
            VoiceCoachConnectionState.CONNECTING -> {
                voiceCoachService.stop()
            }
        }
    }

    /** Clear any error state */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Reset to initial state */
    fun reset() {
        voiceCoachService.stop()
        _uiState.value = VoiceTrainingUiState()
    }

    private fun handleAIEvent(event: AIWorkoutEvent) {
        when (event) {
            is AIWorkoutEvent.SessionStarted -> { /* handled via activeSession flow */ }
            is AIWorkoutEvent.SetLogged -> { /* session data auto-refreshes */ }
            is AIWorkoutEvent.ExerciseAdded -> { /* handled via activeSession flow */ }
            is AIWorkoutEvent.RestStarted -> { /* handled via activeSession flow */ }
            is AIWorkoutEvent.StateRefreshed -> { /* handled via activeSession flow */ }
        }
    }

    /**
     * Extract user ID from the JWT token's "sub" claim.
     * Used for realtime service subscription; not required for ElevenLabs connection.
     */
    private fun getUserIdFromToken(): String? {
        val token = tokenManager.getToken() ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.optString("sub").ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCoachService.destroy()
    }
}
