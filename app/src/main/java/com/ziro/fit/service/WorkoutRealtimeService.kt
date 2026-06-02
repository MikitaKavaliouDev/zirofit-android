package com.ziro.fit.service

import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.repository.LiveWorkoutRepository
import com.ziro.fit.model.ActiveWorkoutStatus
import com.ziro.fit.model.AIWorkoutEvent
import com.ziro.fit.model.LiveWorkoutUiModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to Supabase Realtime for database changes made by the ElevenLabs Agent,
 * with a 15-second polling loop as a *fallback only* (i.e. when realtime fails).
 *
 * Mirrors iOS: AgentChangeDetector (which polls at 10s) and the
 * liveSessionProvider in the iOS app (which polls at 15s).
 *
 * ## Flow
 * 1. ElevenLabs agent calls Server Tool (POST /api/agent/tools)
 * 2. Backend writes to PostgreSQL
 * 3. Supabase Realtime (CDC via WAL) broadcasts the change
 * 4. This service receives it via PostgresChangeFlow
 * 5. If realtime is unavailable, polling fallback checks
 *    GET /api/client/active-workout every 15s
 * 6. Events emitted via [events] SharedFlow and [currentSessionId] StateFlow
 *
 * ## Why this matters
 * Aggressive polling (e.g. 3s) interleaves HTTP requests with the ElevenLabs
 * WebRTC voice session and degrades audio quality. We only poll when Supabase
 * Realtime is unhealthy, and at a friendly 15s cadence (matching iOS).
 */
@Singleton
class WorkoutRealtimeService @Inject constructor(
    private val supabase: SupabaseClient,
    private val repository: LiveWorkoutRepository,
    private val tokenManager: TokenManager
) {
    companion object {
        /** Polling cadence when Supabase Realtime is unavailable. Mirrors iOS 15s. */
        private const val POLLING_INTERVAL_MS = 15_000L

        /** Slow polling cadence used as a safety net even when realtime is healthy. */
        private const val SAFETY_POLL_INTERVAL_MS = 60_000L
    }

    /** Whether the service is actively monitoring for changes */
    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    /** The currently active session ID (null if no workout active) */
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    /** Event bus for AI-driven workout changes */
    private val _events = MutableSharedFlow<AIWorkoutEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AIWorkoutEvent> = _events.asSharedFlow()

    /** The latest full session snapshot (updated on transitions) */
    private val _activeSession = MutableStateFlow<LiveWorkoutUiModel?>(null)
    val activeSession: StateFlow<LiveWorkoutUiModel?> = _activeSession.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var currentClientId: String? = null

    /**
     * True when at least one Supabase Realtime channel is SUBSCRIBED.
     * When `true`, polling falls back to a slow 60s safety cadence so it
     * doesn't compete with the WebRTC voice session for bandwidth.
     */
    @Volatile private var realtimeHealthy: Boolean = false

    /**
     * Start monitoring for AI-driven DB changes.
     * Subscribes to Supabase Realtime channels and starts the polling loop.
     */
    fun subscribe(clientId: String) {
        if (_isPolling.value) return
        currentClientId = clientId
        _isPolling.value = true

        subscribeToRealtimeChannels(clientId)
        startPollingLoop()
    }

    /**
     * Stop monitoring. Unsubscribes from all channels and cancels polling.
     */
    fun unsubscribe() {
        if (!_isPolling.value) return
        _isPolling.value = false
        currentClientId = null
        _currentSessionId.value = null

        pollingJob?.cancel()
        pollingJob = null
    }

    /** Legacy API for backward compatibility */
    fun startPolling(clientId: String) = subscribe(clientId)

    fun stopPolling() = unsubscribe()

    // ──────────────────────────────────────────────────────────────
    // Supabase Realtime Channels
    // ──────────────────────────────────────────────────────────────

    private fun subscribeToRealtimeChannels(clientId: String) {
        // Channel 1: WorkoutSession insert changes
        scope.launch {
            try {
                val channel = supabase.channel("workout-session:$clientId")
                channel.subscribe()
                realtimeHealthy = true
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "WorkoutSession"
                    filter("clientId", FilterOperator.EQ, clientId)
                }.collect { action ->
                    handleWorkoutInsert(action)
                }
            } catch (e: Exception) {
                realtimeHealthy = false
                // Channel may not be available — polling fallback will cover it
            }
        }

        scope.launch {
            try {
                val channel = supabase.channel("workout-session-updates:$clientId")
                channel.subscribe()
                realtimeHealthy = true
                channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "WorkoutSession"
                    filter("clientId", FilterOperator.EQ, clientId)
                }.collect { action ->
                    handleWorkoutUpdate(action)
                }
            } catch (e: Exception) {
                realtimeHealthy = false
                // Fallback to polling
            }
        }

        // Channel 2: ClientExerciseLog changes
        scope.launch {
            try {
                val channel = supabase.channel("client-exercise-log:$clientId")
                channel.subscribe()
                realtimeHealthy = true
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "ClientExerciseLog"
                    filter("clientId", FilterOperator.EQ, clientId)
                }.collect { action ->
                    handleExerciseLogInsert(action)
                }
            } catch (e: Exception) {
                realtimeHealthy = false
                // Fallback to polling
            }
        }
    }

    private fun handleWorkoutInsert(action: PostgresAction.Insert) {
        val record = action.record
        val sessionId = record["id"]?.toString()
        val status = record["status"]?.toString()

        if (sessionId != null && status == "IN_PROGRESS") {
            _currentSessionId.value = sessionId
            val event = AIWorkoutEvent.SessionStarted(sessionId = sessionId)
            _events.tryEmit(event)
        }
    }

    private fun handleWorkoutUpdate(action: PostgresAction.Update) {
        val record = action.record
        val restStartedAt = record["restStartedAt"]?.toString()
        if (!restStartedAt.isNullOrBlank()) {
            _events.tryEmit(AIWorkoutEvent.RestStarted(durationSeconds = 60))
        }
    }

    private fun handleExerciseLogInsert(action: PostgresAction.Insert) {
        val record = action.record
        val workoutSessionId = record["workoutSessionId"]?.toString()
        val exerciseId = record["exerciseId"]?.toString()
        val reps = (record["reps"] as? Number)?.toInt() ?: 0
        val weight = (record["weight"] as? Number)?.toDouble() ?: 0.0

        if (workoutSessionId != null && exerciseId != null) {
            val event = AIWorkoutEvent.SetLogged(
                sessionId = workoutSessionId,
                exerciseName = exerciseId,
                reps = reps,
                weight = weight,
                isPR = false
            )
            _events.tryEmit(event)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Polling Fallback (only runs when Supabase Realtime is unhealthy)
    // ──────────────────────────────────────────────────────────────

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            var lastStatus: ActiveWorkoutStatus? = null

            while (isActive) {
                // Use slow cadence when realtime is healthy (so we don't
                // compete with the WebRTC voice session for bandwidth).
                // Use the 15s fallback cadence when realtime is unhealthy.
                val interval = if (realtimeHealthy) SAFETY_POLL_INTERVAL_MS
                               else POLLING_INTERVAL_MS
                delay(interval)

                if (!_isPolling.value) break

                val status = try {
                    repository.getActiveWorkoutStatus().getOrNull()
                } catch (e: Exception) {
                    // Fallback: try full session endpoint
                    try {
                        val session = repository.getActiveSession().getOrNull()
                        if (session != null) {
                            ActiveWorkoutStatus(
                                hasActiveWorkout = true,
                                sessionId = session.id,
                                exerciseLogCount = session.exercises.sumOf { ex ->
                                    ex.sets.count { it.isCompleted }
                                }
                            )
                        } else {
                            ActiveWorkoutStatus(false)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                if (status == null) continue

                val prev = lastStatus
                lastStatus = status

                if (status.hasActiveWorkout && status.sessionId != null) {
                    val sessionId = status.sessionId
                    val logCount = status.exerciseLogCount

                    if (prev == null || !prev.hasActiveWorkout) {
                        // Transition: workout started
                        _currentSessionId.value = sessionId
                        _events.tryEmit(AIWorkoutEvent.SessionStarted(sessionId))
                        fetchFullSession()
                    } else if (sessionId != prev.sessionId) {
                        // Different session
                        _currentSessionId.value = sessionId
                        _events.tryEmit(AIWorkoutEvent.SessionStarted(sessionId))
                        fetchFullSession()
                    } else if (logCount > (prev.exerciseLogCount)) {
                        // New set logged
                        _events.tryEmit(AIWorkoutEvent.StateRefreshed)
                        fetchFullSession()
                    }

                    // Check rest timer
                    if (status.restStartedAt != null && status.restStartedAt != prev?.restStartedAt) {
                        _events.tryEmit(AIWorkoutEvent.RestStarted(durationSeconds = 90))
                    }
                } else {
                    if (prev != null && prev.hasActiveWorkout) {
                        // Workout ended
                        _currentSessionId.value = null
                        _activeSession.value = null
                    }
                }
            }
        }
    }

    private suspend fun fetchFullSession() {
        try {
            val session = repository.getActiveSession().getOrNull()
            _activeSession.value = session
        } catch (_: Exception) {
            // Silently fail — next poll cycle will retry
        }
    }
}
