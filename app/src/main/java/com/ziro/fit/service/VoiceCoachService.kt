package com.ziro.fit.service

import android.app.Activity
import com.ziro.fit.model.VoiceCoachAgentState
import com.ziro.fit.model.VoiceCoachConnectionState
import com.ziro.fit.model.VoiceMessage
import com.ziro.fit.model.MessageRole
import io.elevenlabs.ConversationClient
import io.elevenlabs.ConversationConfig
import io.elevenlabs.ConversationSession
import io.elevenlabs.models.ConversationMode
import io.elevenlabs.models.ConversationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the ElevenLabs Conversational AI Agent connection lifecycle.
 *
 * Replaces the manual WebSocket + AudioRecord/AudioTrack implementation
 * with the official ElevenLabs Android SDK (LiveKit/WebRTC).
 *
 * Agent ID: agent_3501ksjc2xtnemsbkta6xfm7hg7x
 */
@Singleton
class VoiceCoachService @Inject constructor(
    private val realtimeService: WorkoutRealtimeService
) {
    companion object {
        /** ElevenLabs Conversational AI Agent ID */
        // private const val AGENT_ID = "agent_3501ksjc2xtnemsbkta6xfm7hg7x"
        //   dev agent: agent_3901ksj8q6s0ejwrgzt8a1j1jw3r
          private const val AGENT_ID = "agent_3901ksj8q6s0ejwrgzt8a1j1jw3r"
        
    }

    // ── Published State ──────────────────────────────────────────────

    private val _connectionState = MutableStateFlow(VoiceCoachConnectionState.DISCONNECTED)
    val connectionState: StateFlow<VoiceCoachConnectionState> = _connectionState.asStateFlow()

    private val _agentState = MutableStateFlow(VoiceCoachAgentState.UNKNOWN)
    val agentState: StateFlow<VoiceCoachAgentState> = _agentState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _messages = MutableSharedFlow<VoiceMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<VoiceMessage> = _messages.asSharedFlow()

    /** The current user ID for realtime service subscription */
    private var userId: String = ""

    /** Active SDK session */
    private var session: ConversationSession? = null

    /** Job tracking the ongoing connection coroutine */
    private var connectJob: Job? = null

    /** Job tracking the StateFlow collectors */
    private var observeJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ── Public API ───────────────────────────────────────────────────

    fun configure(userId: String) {
        this.userId = userId
    }

    /**
     * Start a conversation with the ElevenLabs agent.
     * Requires an Activity context for WebRTC audio routing.
     */
    fun start(activity: Activity) {
        if (session != null) return // Already started

        _connectionState.value = VoiceCoachConnectionState.CONNECTING

        val config = ConversationConfig(
            agentId = AGENT_ID,
            userId = userId.ifBlank { null },
            dynamicVariables = if (userId.isNotBlank()) mapOf("userId" to userId) else null,
            // ── Callbacks ──────────────────────────────────────────────
            onConnect = { conversationId ->
                // Subscribe to DB changes when connected
                if (userId.isNotBlank()) {
                    realtimeService.subscribe(userId)
                }
            },
            onDisconnect = { reason ->
                _connectionState.value = VoiceCoachConnectionState.DISCONNECTED
                _agentState.value = VoiceCoachAgentState.UNKNOWN
                _audioLevel.value = 0.0f
                session = null
            },
            onStatusChange = { status ->
                _connectionState.value = when (status) {
                    ConversationStatus.CONNECTED    -> VoiceCoachConnectionState.CONNECTED
                    ConversationStatus.CONNECTING   -> VoiceCoachConnectionState.CONNECTING
                    ConversationStatus.DISCONNECTED -> VoiceCoachConnectionState.DISCONNECTED
                    ConversationStatus.DISCONNECTING -> VoiceCoachConnectionState.DISCONNECTED
                    ConversationStatus.ERROR        -> VoiceCoachConnectionState.ERROR
                }
                if (status == ConversationStatus.DISCONNECTED || status == ConversationStatus.ERROR) {
                    session = null
                }
            },
            onModeChange = { mode ->
                _agentState.value = when (mode) {
                    ConversationMode.SPEAKING  -> VoiceCoachAgentState.SPEAKING
                    ConversationMode.LISTENING -> VoiceCoachAgentState.LISTENING
                }
            },
            onAudioLevelChanged = { level ->
                _audioLevel.value = level
            },
            onUserTranscript = { transcript ->
                val msg = VoiceMessage(
                    id = "user-${System.currentTimeMillis()}",
                    role = MessageRole.USER,
                    content = transcript
                )
                _messages.tryEmit(msg)
            },
            onAgentResponse = { response ->
                val msg = VoiceMessage(
                    id = "agent-${System.currentTimeMillis()}",
                    role = MessageRole.AGENT,
                    content = response
                )
                _messages.tryEmit(msg)
            }
        )

        // startSession is a suspend function — launch in coroutine scope
        // with a timeout to prevent hanging on invalid agent IDs or network issues
        connectJob = scope.launch {
            try {
                val newSession = withTimeout(30_000L) {
                    ConversationClient.startSession(config, activity)
                }
                session = newSession
                // Reactively observe SDK StateFlows for any state changes
                // not covered by callbacks above
                observeJob?.cancel()
                observeJob = scope.launch {
                    newSession.status.collect { status ->
                        _connectionState.value = when (status) {
                            ConversationStatus.CONNECTED    -> VoiceCoachConnectionState.CONNECTED
                            ConversationStatus.CONNECTING   -> VoiceCoachConnectionState.CONNECTING
                            ConversationStatus.DISCONNECTED -> VoiceCoachConnectionState.DISCONNECTED
                            ConversationStatus.DISCONNECTING -> VoiceCoachConnectionState.DISCONNECTED
                            ConversationStatus.ERROR        -> VoiceCoachConnectionState.ERROR
                        }
                        if (status == ConversationStatus.DISCONNECTED || status == ConversationStatus.ERROR) {
                            session = null
                        }
                    }
                }
                scope.launch {
                    newSession.mode.collect { mode ->
                        _agentState.value = when (mode) {
                            ConversationMode.SPEAKING  -> VoiceCoachAgentState.SPEAKING
                            ConversationMode.LISTENING -> VoiceCoachAgentState.LISTENING
                        }
                    }
                }
                scope.launch {
                    newSession.audioLevel.collect { level ->
                        _audioLevel.value = level
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _connectionState.value = VoiceCoachConnectionState.ERROR
                session = null
            } catch (e: Exception) {
                _connectionState.value = VoiceCoachConnectionState.ERROR
                session = null
            }
        }
    }

    /** End the current conversation */
    fun stop() {
        realtimeService.unsubscribe()
        connectJob?.cancel()
        connectJob = null
        observeJob?.cancel()
        observeJob = null
        // Capture session reference BEFORE nulling it, so the async
        // endSession() call actually reaches the ElevenLabs server.
        val currentSession = session
        session = null
        scope.launch {
            currentSession?.endSession()
        }
        _connectionState.value = VoiceCoachConnectionState.DISCONNECTED
        _agentState.value = VoiceCoachAgentState.UNKNOWN
        _audioLevel.value = 0.0f
    }

    /** Toggle connection on/off. Requires Activity for connect branch. */
    fun toggle(activity: Activity? = null) {
        when (_connectionState.value) {
            VoiceCoachConnectionState.CONNECTED,
            VoiceCoachConnectionState.CONNECTING -> stop()
            VoiceCoachConnectionState.DISCONNECTED,
            VoiceCoachConnectionState.ERROR -> {
                val act = activity ?: return
                start(act)
            }
        }
    }

    /** Clean up all resources. Call when the service is no longer needed. */
    fun destroy() {
        stop()
        scope.cancel()
    }
}
