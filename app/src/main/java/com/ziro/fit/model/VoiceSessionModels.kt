package com.ziro.fit.model

import com.google.gson.annotations.SerializedName

/**
 * Lightweight polling status from GET /api/client/active-workout
 * Mirrors iOS ActiveWorkoutStatus struct
 */
data class ActiveWorkoutStatus(
    @SerializedName("hasActiveWorkout")
    val hasActiveWorkout: Boolean,

    @SerializedName("sessionId")
    val sessionId: String? = null,

    @SerializedName("exerciseLogCount")
    val exerciseLogCount: Int = 0,

    @SerializedName("restStartedAt")
    val restStartedAt: String? = null
)

/**
 * Connection state for the ElevenLabs Conversational AI agent.
 * Mirrors iOS VoiceCoachService.ConnectionState
 */
enum class VoiceCoachConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Agent state — what the AI is currently doing.
 * Mirrors iOS VoiceCoachAgentState / ElevenLabs.AgentState
 */
enum class VoiceCoachAgentState {
    LISTENING,
    THINKING,
    SPEAKING,
    UNKNOWN
}

/**
 * A single message in the conversation transcript.
 * Mirrors iOS VoiceMessage struct.
 */
data class VoiceMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER,
    AGENT
}

/**
 * Events emitted by WorkoutRealtimeService to signal DB changes.
 * Mirrors iOS AIWorkoutEvent enum.
 */
sealed class AIWorkoutEvent {
    data class SessionStarted(val sessionId: String) : AIWorkoutEvent()
    data class ExerciseAdded(val sessionId: String, val exerciseName: String) : AIWorkoutEvent()
    data class SetLogged(
        val sessionId: String,
        val exerciseName: String,
        val reps: Int,
        val weight: Double,
        val isPR: Boolean = false
    ) : AIWorkoutEvent()
    data class RestStarted(val durationSeconds: Int) : AIWorkoutEvent()
    data object StateRefreshed : AIWorkoutEvent()
}

/**
 * Voice settings response from GET /api/user/voice-settings.
 * Mirrors iOS VoiceSettingsModel.
 */
data class VoiceSettingsResponse(
    @SerializedName("voiceId")
    val voiceId: String? = null,

    @SerializedName("settings")
    val settings: VoiceSettingsValues? = null
)

data class VoiceSettingsValues(
    @SerializedName("stability")
    val stability: Double = 0.5,

    @SerializedName("similarity_boost")
    val similarityBoost: Double = 0.75,

    @SerializedName("style")
    val style: Double = 0.0,

    @SerializedName("use_speaker_boost")
    val useSpeakerBoost: Boolean = true,

    @SerializedName("speed")
    val speed: Double = 1.0
)

/**
 * Voice list response from GET /api/ai-trainer/voices
 * Mirrors iOS VoiceModel.
 */
data class VoiceModel(
    @SerializedName("voice_id")
    val voiceId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("preview_url")
    val previewUrl: String? = null,

    @SerializedName("labels")
    val labels: Map<String, String>? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null
)
