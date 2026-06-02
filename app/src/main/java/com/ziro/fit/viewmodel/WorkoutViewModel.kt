package com.ziro.fit.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.repository.LiveWorkoutRepository
import com.ziro.fit.model.AppMode
import com.ziro.fit.model.Exercise
import com.ziro.fit.model.LiveWorkoutUiModel
import com.ziro.fit.service.MatchConfidence
import com.ziro.fit.service.VoiceLogManager
import com.ziro.fit.model.NewRecord
import com.ziro.fit.model.RecordType
import com.ziro.fit.model.SetStatus
import com.ziro.fit.model.WorkoutExerciseUi
import com.ziro.fit.model.WorkoutSetUi
import com.ziro.fit.model.WorkoutStats
import com.ziro.fit.model.SyncActionType
import com.ziro.fit.model.LogSetPayload
import com.ziro.fit.model.FinishWorkoutPayload
import com.ziro.fit.service.SyncManager
 import com.ziro.fit.service.ActiveWorkoutService
 import com.ziro.fit.service.WorkoutStateManager
 import com.ziro.fit.service.WorkoutRealtimeService
 import com.ziro.fit.util.VoiceFeedbackManager
 import dagger.hilt.android.lifecycle.HiltViewModel
 import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import javax.inject.Inject

// Re-using the same UI state data class, but mapping from Manager state
data class WorkoutUiState(
    val activeSession: LiveWorkoutUiModel? = null,
    val elapsedSeconds: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFinishing: Boolean = false,
    val isSessionCompleted: Boolean = false,
    val workoutSuccessStats: WorkoutStats? = null,
    val availableExercises: List<Exercise> = emptyList(),
    val isExercisesLoading: Boolean = false,
    val isRestActive: Boolean = false,
    val restingExerciseId: String? = null,
    val restSecondsRemaining: Int = 0,
    val restTotalSeconds: Int = 60,
    val latestCommand: ParsedWorkoutCommand? = null,
    // PR tracking — mirrors iOS sessionNewRecords + showNewRecordsToast
    val newRecords: List<NewRecord> = emptyList(),
    val showNewRecordsToast: Boolean = false,
    val showVoiceCorrectionPicker: Boolean = false,
    // Conflict resolution state
    val showConflictDialog: Boolean = false,
    val conflictingSessionTitle: String? = null,
    val conflictingClientId: String? = null,
    val conflictingTemplateId: String? = null,
    val conflictingPlannedSessionId: String? = null
)

data class ParsedWorkoutCommand(
    var exercise: String? = null,
    var sets: Int? = null,
    var reps: Int? = null,
    var weight: Double? = null,
    var matchConfidence: MatchConfidence = MatchConfidence.NONE,
    var commandType: VoiceCommandType = VoiceCommandType.UNKNOWN,
    var adjustmentWeight: Double? = null,
    var adjustmentReps: Int? = null,
    var isAbsoluteReps: Boolean = false
)

enum class VoiceCommandType {
    ADD_SET,
    REPEAT_LAST_SET,
    ADJUST_LAST_SET,
    DELETE_LAST_SET,
    FINISH_SESSION,
    UNKNOWN
}

 @HiltViewModel
 class WorkoutViewModel @Inject constructor(
     private val repository: LiveWorkoutRepository,
     private val workoutStateManager: WorkoutStateManager,
     private val application: Application,
     private val syncManager: SyncManager,
     private val voiceLogManager: VoiceLogManager,
     private val voiceFeedbackManager: VoiceFeedbackManager,
     private val calendarRepository: com.ziro.fit.data.repository.CalendarRepository,
     private val clientRepository: com.ziro.fit.data.repository.ClientRepository,
     private val tokenManager: TokenManager,
     private val realtimeService: WorkoutRealtimeService
 ) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    // Track the current exercise search job so rapid keystrokes cancel previous in-flight requests
    private var searchJob: Job? = null

    // Accumulate PRs during session — mirrors iOS sessionNewRecords
    private val _sessionNewRecords = mutableStateListOf<NewRecord>()

    init {
        viewModelScope.launch {
            workoutStateManager.state.collect { managerState ->
                _uiState.update { 
                    it.copy(
                        activeSession = managerState.activeSession,
                        elapsedSeconds = managerState.elapsedSeconds,
                        isRestActive = managerState.isRestActive,
                        restSecondsRemaining = managerState.restSecondsRemaining,
                        restTotalSeconds = managerState.restTotalSeconds,
                        restingExerciseId = managerState.restingExerciseId
                    )
                }
            }
        }
        // Listen for agent-driven session updates from WorkoutRealtimeService
        viewModelScope.launch {
            realtimeService.activeSession.collect { session ->
                if (session != null && !_uiState.value.isLoading) {
                    val merged = mergeSessions(session, workoutStateManager.state.value.activeSession)
                    workoutStateManager.updateSession(merged)
                }
            }
        }
        refreshActiveSession()
    }

    private fun updateServiceState(session: LiveWorkoutUiModel?) {
        if (!hasRequiredPermissions()) return

        val intent = Intent(application, ActiveWorkoutService::class.java)
        if (session != null) {
            application.startForegroundService(intent)
        } else {
            intent.action = ActiveWorkoutService.ACTION_STOP_SERVICE
            application.startService(intent)
        }
    }

    fun onPermissionsResult() {
        // Retry starting service if we have an active session
        val currentSession = workoutStateManager.state.value.activeSession
        if (currentSession != null && hasRequiredPermissions()) {
            val intent = Intent(application, ActiveWorkoutService::class.java)
            application.startForegroundService(intent)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(application, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun refreshActiveSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Step 1: Try standard /live endpoint (mirrors iOS step 1)
            repository.getActiveSession()
                .onSuccess { session ->
                    if (session != null) {
                        val merged = mergeSessions(session, workoutStateManager.state.value.activeSession)
                        workoutStateManager.updateSession(merged)
                        updateServiceState(merged)
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        // Step 2: Fallback — scan calendar for stale sessions (mirrors iOS step 2)
                        checkForStaleSession()
                    }
                    return@launch
                }
                .onFailure {
                    // Network error — try calendar recovery anyway
                    checkForStaleSession()
                }
        }
    }

    private fun checkForStaleSession() {
        if (tokenManager.activeMode.value != AppMode.TRAINER) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        
        viewModelScope.launch {
            try {
                val now = java.time.LocalDate.now()
                val startDate = now.minusDays(30)
                
                calendarRepository.getEventsInRange(startDate, now)
                    .onSuccess { events ->
                        val staleEvent = events
                            .filter { it.type == com.ziro.fit.model.EventType.session_in_progress }
                            .maxByOrNull { it.startTime }
                        
                        if (staleEvent != null) {
                            val sessionAge = java.time.Duration.between(
                                staleEvent.startTime,
                                java.time.LocalDateTime.now()
                            )
                            if (sessionAge.toHours() <= 12) {
                                // Fetch the session and resume
                                repository.getActiveSession().onSuccess { recoveredSession ->
                                    if (recoveredSession != null) {
                                        workoutStateManager.updateSession(recoveredSession)
                                        updateServiceState(recoveredSession)
                                    }
                                }
                            }
                        }
                    }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startWorkout(
        clientId: String?, 
        templateId: String?, 
        plannedSessionId: String?,
        onSuccess: () -> Unit = {}
    ) {
        val activeSession = workoutStateManager.state.value.activeSession
        if (activeSession != null) {
            _uiState.update { it.copy(
                showConflictDialog = true,
                conflictingSessionTitle = activeSession.title,
                conflictingClientId = clientId,
                conflictingTemplateId = templateId,
                conflictingPlannedSessionId = plannedSessionId
            ) }
            return
        }

        performStartWorkout(clientId, templateId, plannedSessionId, onSuccess)
    }

    private fun performStartWorkout(
        clientId: String?, 
        templateId: String?, 
        plannedSessionId: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.startWorkout(clientId, templateId, plannedSessionId)
                .onSuccess { session ->
                    workoutStateManager.updateSession(session)
                    updateServiceState(session)
                    _uiState.update { it.copy(isLoading = false, isSessionCompleted = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.localizedMessage, isLoading = false) }
                }
        }
    }

    fun resumeCurrent() {
        _uiState.update { it.copy(showConflictDialog = false) }
    }

    fun forceStartNew() {
        val state = _uiState.value
        _uiState.update { it.copy(showConflictDialog = false) }
        performStartWorkout(
            clientId = state.conflictingClientId,
            templateId = state.conflictingTemplateId,
            plannedSessionId = state.conflictingPlannedSessionId,
            onSuccess = {}
        )
    }

    fun dismissConflictDialog() {
        _uiState.update { it.copy(showConflictDialog = false) }
    }

    // Pass-through methods to Manager
    fun startRestTimer(seconds: Int, exerciseId: String? = null) {
        workoutStateManager.startRestTimer(seconds, exerciseId)
    }
    
    fun stopRestTimer() {
        workoutStateManager.stopRestTimer()
    }
    
    fun adjustRestTime(secondsToAdd: Int) {
        workoutStateManager.adjustRestTime(secondsToAdd)
    }

    // ... (Keep existing methods: loadExercises, addExerciseToSession, updateSetInput, logSet)
    // Ensure they update workoutStateManager.updateSession(...) when session structure changes locally.

    fun addExercisesToSession(exercises: List<Exercise>) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val newExercisesUi = exercises.map { exercise ->
             WorkoutExerciseUi(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                targetReps = null,
                restSeconds = null,
                sets = listOf(
                    WorkoutSetUi(null, 1, "", "", false, 0)
                ),
                superSetId = null
            )
        }
        val updatedExercises = currentSession.exercises + newExercisesUi
        val updatedSession = currentSession.copy(exercises = updatedExercises)
        workoutStateManager.updateSession(updatedSession)
    }

    fun updateSetInput(exerciseId: String, setIndex: Int, weight: String, reps: String) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val updatedSets = ex.sets.toMutableList()
                if (setIndex in updatedSets.indices) {
                    updatedSets[setIndex] = updatedSets[setIndex].copy(weight = weight, reps = reps)
                    ex.copy(sets = updatedSets)
                } else ex
            } else ex
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }

    fun updateSetRpe(exerciseId: String, setIndex: Int, rpe: Double?) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val updatedSets = ex.sets.toMutableList()
                if (setIndex in updatedSets.indices) {
                    updatedSets[setIndex] = updatedSets[setIndex].copy(rpe = rpe)
                    ex.copy(sets = updatedSets)
                } else ex
            } else ex
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }

    fun addSetToExercise(exerciseId: String) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
             if (ex.exerciseId == exerciseId) {
                 val nextSetNumber = ex.sets.size + 1
                 val nextOrder = ex.sets.maxOfOrNull { it.order }?.plus(1) ?: 0
                 val newSet = WorkoutSetUi(null, nextSetNumber, ex.sets.lastOrNull()?.weight ?: "", "", false, nextOrder, null, SetStatus.NORMAL)
                 ex.copy(sets = ex.sets + newSet)
             } else ex
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }

    fun toggleSuperset(exerciseId: String) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val exercises = currentSession.exercises.toMutableList()
        val idx = exercises.indexOfFirst { it.exerciseId == exerciseId }
        if (idx == -1) return

        val target = exercises[idx]

        if (target.superSetId != null) {
            val updated = exercises.mapIndexed { i, ex ->
                if (i == idx) ex.copy(superSetId = null) else ex
            }
            workoutStateManager.updateSession(currentSession.copy(exercises = updated))
        } else {
            val newSuperSetId = java.util.UUID.randomUUID().toString()
            val nextIdx = idx + 1
            val prevIdx = idx - 1
            val nextExercise = exercises.getOrNull(nextIdx)
            val prevExercise = exercises.getOrNull(prevIdx)

            val linkedNeighborIdx: Int? = when {
                nextExercise != null && nextExercise.superSetId == null -> nextIdx
                prevExercise != null && prevExercise.superSetId == null -> prevIdx
                else -> null
            }

            val updated = exercises.mapIndexed { i, ex ->
                when (i) {
                    idx -> ex.copy(superSetId = newSuperSetId)
                    linkedNeighborIdx -> ex.copy(superSetId = newSuperSetId)
                    else -> ex
                }
            }
            workoutStateManager.updateSession(currentSession.copy(exercises = updated))
        }
    }

    fun logSet(exerciseId: String, set: WorkoutSetUi, status: SetStatus = SetStatus.NORMAL) {
        val session = workoutStateManager.state.value.activeSession ?: return
        val weightVal = set.weight.toDoubleOrNull()
        val repsVal = set.reps.toIntOrNull()
        if (weightVal == null || repsVal == null) return

        // Optimistic Update
        val updatedExercises = session.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val updatedSets = ex.sets.map { s ->
                    if (s.order == set.order) s.copy(isCompleted = true, status = status) else s
                }
                ex.copy(sets = updatedSets)
            } else ex
        }
        workoutStateManager.updateSession(session.copy(exercises = updatedExercises))
        
        // Timer
        val exercise = session.exercises.find { it.exerciseId == exerciseId }
        val restTime = exercise?.restSeconds ?: 60
        startRestTimer(restTime, exerciseId)

        viewModelScope.launch {
            repository.logSet(session.id, exerciseId, repsVal, weightVal, set.order, set.isCompleted, set.logId, set.rpe, status)
                .onSuccess { newRecords ->
                    if (newRecords.isNotEmpty()) {
                        _sessionNewRecords.addAll(newRecords)
                        _uiState.update { it.copy(newRecords = _sessionNewRecords.toList(), showNewRecordsToast = true) }
                    }
                    repository.getActiveSession().onSuccess { refreshed ->
                        if (refreshed != null) {
                            val merged = mergeSessions(refreshed, workoutStateManager.state.value.activeSession)
                            workoutStateManager.updateSession(merged)
                        }
                    }
                }
                .onFailure {
                    val payload = LogSetPayload(
                        workoutSessionId = session.id,
                        exerciseId = exerciseId,
                        reps = repsVal,
                        weight = weightVal,
                        rpe = set.rpe,
                        order = set.order,
                        isCompleted = set.isCompleted,
                        logId = set.logId
                    )
                    syncManager.enqueue(SyncActionType.LOG_SET, payload)
                    _uiState.update { it.copy(error = "Network offline. Set saved locally.") }
                }
        }
    }

    fun finishWorkout(notes: String? = null) {
        val sessionId = workoutStateManager.state.value.activeSession?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFinishing = true) }
            repository.finishSession(sessionId, notes)
                .onSuccess { response ->
                    workoutStateManager.updateSession(null) // This stops the service
                    updateServiceState(null)
                    
                    // Trigger PR toast if records were broken (mirrors iOS newRecords toast)
                    val recordsBroken = response.stats?.recordsBroken ?: 0
                    val showPrToast = recordsBroken > 0 || _sessionNewRecords.isNotEmpty()
                    
                    _uiState.update { it.copy(
                        isFinishing = false, 
                        isSessionCompleted = true,
                        workoutSuccessStats = response.stats,
                        showNewRecordsToast = showPrToast,
                        newRecords = _sessionNewRecords.toList().ifEmpty {
                            // Build NewRecord from recordsBroken count if no per-set records accumulated
                            if (recordsBroken > 0) {
                                listOf(NewRecord(RecordType.MAX_WEIGHT, "", "", "", ""))
                            } else emptyList()
                        }
                    ) }
                    _sessionNewRecords.clear()
                }
                // NEW CODE START - Use SyncManager
                .onFailure { e ->
                    val payload = FinishWorkoutPayload(
                        sessionId = sessionId,
                        notes = notes
                    )
                    syncManager.enqueue(SyncActionType.FINISH_WORKOUT, payload)
                    
                    // Locally complete the session for the UI
                    workoutStateManager.updateSession(null)
                    updateServiceState(null)
                    
                    // Fake a success payload to allow UI to proceed offline
                    _uiState.update { it.copy(
                        isFinishing = false, 
                        isSessionCompleted = true,
                        workoutSuccessStats = WorkoutStats(
                            durationSeconds = 0,
                            volumeKg = 0.0,
                            setsCompleted = 0,
                            recordsBroken = 0,
                            message = "Network offline. Workout saved and queued for sync.",
                            exerciseSummaries = emptyList()
                        )
                    ) }
                }
                // NEW CODE END
        }
    }

    fun cancelWorkout() {
        val sessionId = workoutStateManager.state.value.activeSession?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.cancelActiveWorkout(sessionId)
                .onSuccess {
                    workoutStateManager.updateSession(null)
                    updateServiceState(null)
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    // Even if it fails (like 404), we should probably clear local state
                    // if the session is gone on the server.
                    if (error.localizedMessage?.contains("404") == true) {
                        workoutStateManager.updateSession(null)
                        updateServiceState(null)
                    }
                    _uiState.update { it.copy(error = error.localizedMessage, isLoading = false) }
                }
        }
    }

    fun loadExercises(query: String? = null) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isExercisesLoading = true) }
            repository.getExercises(query)
                .onSuccess { response ->
                    _uiState.update { it.copy(availableExercises = response.exercises, isExercisesLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExercisesLoading = false, error = e.localizedMessage ?: "Failed to load exercises") }
                }
        }
    }

    private fun mergeSessions(newSession: LiveWorkoutUiModel, oldSession: LiveWorkoutUiModel?): LiveWorkoutUiModel {
        if (oldSession == null) return newSession
        val newExerciseIds = newSession.exercises.map { it.exerciseId }.toSet()
        val localOnlyExercises = oldSession.exercises.filter { !newExerciseIds.contains(it.exerciseId) }
        return newSession.copy(exercises = newSession.exercises + localOnlyExercises)
    }

    fun onSessionCompletedNavigated() {
        _uiState.update { it.copy(isSessionCompleted = false, workoutSuccessStats = null, showNewRecordsToast = false, newRecords = emptyList()) }
    }

    fun dismissNewRecordsToast() {
        _uiState.update { it.copy(showNewRecordsToast = false) }
    }

    fun showVoiceCorrectionPicker() {
        _uiState.update { it.copy(showVoiceCorrectionPicker = true) }
    }

    fun updateSetStatus(exerciseId: String, setIndex: Int, status: SetStatus) {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val updatedSets = ex.sets.toMutableList()
                if (setIndex in updatedSets.indices) {
                    updatedSets[setIndex] = updatedSets[setIndex].copy(status = status)
                    ex.copy(sets = updatedSets)
                } else ex
            } else ex
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Voice command functions
     fun parseVoiceCommand(text: String) {
         val command = voiceLogManager.parseVoiceCommand(text)
         val lowerText = text.lowercase()
         
         val commandType = when {
             lowerText.contains("repeat") || lowerText.contains("again") -> VoiceCommandType.REPEAT_LAST_SET
             lowerText.contains("increase") || lowerText.contains("adjust") || lowerText.contains("change") || lowerText.contains("more") || command.adjustmentWeight != null || command.adjustmentReps != null -> VoiceCommandType.ADJUST_LAST_SET
             lowerText.contains("delete") || lowerText.contains("remove last") || lowerText.contains("cancel last") -> VoiceCommandType.DELETE_LAST_SET
             lowerText.contains("finish") || lowerText.contains("end") || lowerText.contains("complete") -> VoiceCommandType.FINISH_SESSION
             command.reps != null -> VoiceCommandType.ADD_SET
             else -> VoiceCommandType.UNKNOWN
         }
         
         val confidence = if (command.exercise != null) MatchConfidence.STARTS_WITH else MatchConfidence.NONE
         _uiState.update { it.copy(latestCommand = ParsedWorkoutCommand(
             exercise = command.exercise,
             sets = command.sets,
             reps = command.reps,
             weight = command.weight,
             matchConfidence = confidence,
             commandType = commandType,
             adjustmentWeight = command.adjustmentWeight,
             adjustmentReps = command.adjustmentReps,
             isAbsoluteReps = command.isAbsoluteReps
         )) }
     }

    fun confirmVoiceCommand() {
        val command = _uiState.value.latestCommand ?: return
        val currentSession = workoutStateManager.state.value.activeSession ?: return

        when (command.commandType) {
            VoiceCommandType.ADD_SET -> {
                val searchNormalized = command.exercise?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: ""
                var targetExerciseId: String? = null

                val exactMatch = currentSession.exercises.firstOrNull { 
                    it.exerciseName.lowercase().replace(Regex("[^a-z0-9]"), "") == searchNormalized 
                }
                
                if (exactMatch != null) {
                    targetExerciseId = exactMatch.exerciseId
                } else {
                    val sessionExerciseNames = currentSession.exercises.map { it.exerciseName }
                    val (matchedName, confidence) = voiceLogManager.findBestExerciseMatch(
                        searchNormalized, 
                        sessionExerciseNames,
                        fuzzyThreshold = 0.5
                    )
                    
                    if (matchedName != null) {
                        val sessionMatch = currentSession.exercises.firstOrNull { 
                            it.exerciseName.lowercase().replace(Regex("[^a-z0-9]"), "") == matchedName.lowercase().replace(Regex("[^a-z0-9]"), "") 
                        }
                        if (sessionMatch != null) {
                            targetExerciseId = sessionMatch.exerciseId
                        }
                    }

                    if (targetExerciseId == null) {
                        val libExerciseNames = _uiState.value.availableExercises.map { it.name }
                        val (libMatchedName, _) = voiceLogManager.findBestExerciseMatch(
                            searchNormalized,
                            libExerciseNames,
                            fuzzyThreshold = 0.5
                        )
                        
                        if (libMatchedName != null) {
                            val libMatch = _uiState.value.availableExercises.firstOrNull { 
                                it.name.lowercase().replace(Regex("[^a-z0-9]"), "") == libMatchedName.lowercase().replace(Regex("[^a-z0-9]"), "") 
                            }
                            if (libMatch != null) {
                                targetExerciseId = libMatch.id
                                addExercisesToSession(listOf(libMatch))
                            }
                        }
                    }

                    if (targetExerciseId == null) {
                        targetExerciseId = currentSession.exercises.lastOrNull()?.exerciseId
                    }
                }

                if (targetExerciseId != null && command.reps != null) {
                    val sessionAfterAdd = workoutStateManager.state.value.activeSession
                    val exIndex = sessionAfterAdd?.exercises?.indexOfFirst { it.exerciseId == targetExerciseId } ?: -1
                    if (exIndex != -1) {
                        val exercise = sessionAfterAdd!!.exercises[exIndex]
                        val nextSetNum = exercise.sets.size + 1
                        val nextOrder = exercise.sets.maxOfOrNull { it.order }?.plus(1) ?: 0
                        val newSet = WorkoutSetUi(
                            logId = java.util.UUID.randomUUID().toString(),
                            setNumber = nextSetNum,
                            weight = (command.weight ?: 0.0).toString(),
                            reps = command.reps.toString(),
                            isCompleted = true,
                            order = nextOrder,
                            rpe = null,
                            status = SetStatus.NORMAL
                        )
                        logSet(targetExerciseId, newSet)
                        voiceFeedbackManager.speakConfirmation(exercise.exerciseName, command.reps!!, command.weight)
                    }
                } else {
                    voiceFeedbackManager.speakStatus("Could not log set. Please specify exercise and reps.")
                }
            }
            VoiceCommandType.REPEAT_LAST_SET -> {
                val allCompletedSets = currentSession.exercises.flatMap { ex ->
                    ex.sets.filter { it.isCompleted }.map { set -> Pair(ex, set) }
                }.sortedBy { it.second.order }
                val lastSetPair = allCompletedSets.lastOrNull()
                if (lastSetPair != null) {
                    val (exercise, lastSet) = lastSetPair
                    val nextSetNum = exercise.sets.size + 1
                    val nextOrder = exercise.sets.maxOfOrNull { it.order }?.plus(1) ?: 0
                    val newSet = WorkoutSetUi(
                        logId = java.util.UUID.randomUUID().toString(),
                        setNumber = nextSetNum,
                        weight = lastSet.weight,
                        reps = lastSet.reps,
                        isCompleted = true,
                        order = nextOrder,
                        rpe = null,
                        status = SetStatus.NORMAL
                    )
                    logSet(exercise.exerciseId, newSet)
                    voiceFeedbackManager.speakConfirmation(exercise.exerciseName, lastSet.reps.toInt(), lastSet.weight.toDoubleOrNull())
                } else {
                    voiceFeedbackManager.speakStatus("No previous set to repeat")
                }
            }
            VoiceCommandType.ADJUST_LAST_SET -> {
                val hasWeightAdjust = command.adjustmentWeight != null
                val hasRepsAdjust = command.adjustmentReps != null
                if (!hasWeightAdjust && !hasRepsAdjust) {
                    voiceFeedbackManager.speakStatus("Please specify weight or reps to adjust")
                    return
                }
                val allCompletedSets = currentSession.exercises.flatMap { ex ->
                    ex.sets.filter { it.isCompleted }.map { set -> Pair(ex, set) }
                }.sortedBy { it.second.order }
                val lastSetPair = allCompletedSets.lastOrNull()
                if (lastSetPair != null) {
                    val (exercise, lastSet) = lastSetPair
                    val currentWeight = lastSet.weight.toDoubleOrNull() ?: 0.0
                    val currentReps = lastSet.reps.toIntOrNull() ?: 0
                    val newWeight = if (hasWeightAdjust) currentWeight + command.adjustmentWeight!! else currentWeight
                    val newReps = when {
                        hasRepsAdjust && command.isAbsoluteReps -> command.adjustmentReps!!
                        hasRepsAdjust -> currentReps + command.adjustmentReps!!
                        else -> currentReps
                    }
                    val updatedSet = lastSet.copy(
                        weight = newWeight.toString(),
                        reps = newReps.toString()
                    )
                    val updatedExercises = currentSession.exercises.map { ex ->
                        if (ex.exerciseId == exercise.exerciseId) {
                            val updatedSets = ex.sets.map { set ->
                                if (set.order == lastSet.order) updatedSet else set
                            }
                            ex.copy(sets = updatedSets)
                        } else ex
                    }
                    workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
                    com.ziro.fit.ui.components.HapticManagerCompat.notification(com.ziro.fit.util.HapticNotification.SUCCESS)
                    val feedbackParts = mutableListOf<String>()
                    if (hasWeightAdjust) {
                        val direction = if (command.adjustmentWeight!! >= 0) "Added" else "Removed"
                        feedbackParts.add("$direction ${kotlin.math.abs(command.adjustmentWeight!!)} kilos")
                    }
                    if (hasRepsAdjust) {
                        val direction = if (command.isAbsoluteReps) "Set reps to" else "Added"
                        val value = if (command.isAbsoluteReps) command.adjustmentReps else kotlin.math.abs(command.adjustmentReps!!)
                        feedbackParts.add("$direction $value reps")
                    }
                    voiceFeedbackManager.speakStatus(feedbackParts.joinToString(", "))
                    if (lastSet.logId != null) {
                        viewModelScope.launch {
                            repository.logSet(
                                sessionId = currentSession.id,
                                exerciseId = exercise.exerciseId,
                                reps = newReps,
                                weight = newWeight,
                                order = lastSet.order,
                                isCompleted = true,
                                logId = lastSet.logId,
                                rpe = lastSet.rpe,
                                status = lastSet.status
                            )
                        }
                    }
                } else {
                    voiceFeedbackManager.speakStatus("No previous set to adjust")
                    com.ziro.fit.ui.components.HapticManagerCompat.notification(com.ziro.fit.util.HapticNotification.ERROR)
                }
            }
            VoiceCommandType.DELETE_LAST_SET -> {
                val allCompletedSets = currentSession.exercises.flatMap { ex ->
                    ex.sets.filter { it.isCompleted }.map { set -> Pair(ex, set) }
                }.sortedBy { it.second.order }
                val lastSetPair = allCompletedSets.lastOrNull()
                if (lastSetPair != null) {
                    val (exercise, lastSet) = lastSetPair
                    val updatedExercises = currentSession.exercises.map { ex ->
                        if (ex.exerciseId == exercise.exerciseId) {
                            val filteredSets = ex.sets.filter { it.order != lastSet.order }
                            ex.copy(sets = filteredSets)
                        } else ex
                    }
                    workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
                    voiceFeedbackManager.speakStatus("Deleted last set of ${exercise.exerciseName}")
                } else {
                    voiceFeedbackManager.speakStatus("No set to delete")
                }
            }
            VoiceCommandType.FINISH_SESSION -> {
                finishWorkout()
                voiceFeedbackManager.speakStatus("Finishing workout.")
            }
            VoiceCommandType.UNKNOWN -> {
                voiceFeedbackManager.speakStatus("Unknown command")
            }
        }
        _uiState.update { it.copy(latestCommand = null) }
    }

    fun dismissVoiceCommand() {
        _uiState.update { it.copy(latestCommand = null) }
    }

     fun onExerciseSelectedForCorrection(exercise: Exercise) {
         val currentCommand = _uiState.value.latestCommand ?: return
         val updatedCommand = ParsedWorkoutCommand(
             exercise = exercise.name,
             sets = currentCommand.sets,
             reps = currentCommand.reps,
             weight = currentCommand.weight,
             matchConfidence = currentCommand.matchConfidence,
             commandType = currentCommand.commandType,
             adjustmentWeight = currentCommand.adjustmentWeight,
             adjustmentReps = currentCommand.adjustmentReps,
             isAbsoluteReps = currentCommand.isAbsoluteReps
         )
         _uiState.update { it.copy(
             latestCommand = updatedCommand,
             showVoiceCorrectionPicker = false
         ) }
     }

    // Check if there are any unlogged sets (sets with logId == null)
    fun hasUnloggedSets(): Boolean {
        val session = workoutStateManager.state.value.activeSession ?: return false
        return session.exercises.any { exercise ->
            exercise.sets.any { it.logId == null }
        }
    }

    // Complete unlogged sets by filling them with placeholder values
    // Uses previous completed set's values or defaults (0.0 kg, 0 reps)
    fun completeUnloggedSets() {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
            val updatedSets = ex.sets.map { set ->
                if (set.logId == null) {
                    // Find the previous completed set in this exercise to copy values from
                    val previousCompletedSet = ex.sets
                        .filter { it.logId != null && it.isCompleted }
                        .sortedBy { it.order }
                        .lastOrNull { it.order < set.order }
                    
                    val weight = previousCompletedSet?.weight ?: "0.0"
                    val reps = previousCompletedSet?.reps ?: "0"
                    
                    // Mark as completed but still no logId (will be created on server during finish)
                    set.copy(
                        weight = weight,
                        reps = reps,
                        isCompleted = true
                    )
                } else {
                    set
                }
            }
            ex.copy(sets = updatedSets)
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }

    // Discard unlogged sets by removing them from the session
    fun discardUnloggedSets() {
        val currentSession = workoutStateManager.state.value.activeSession ?: return
        val updatedExercises = currentSession.exercises.map { ex ->
            val filteredSets = ex.sets.filter { it.logId != null }
            ex.copy(sets = filteredSets)
        }
        workoutStateManager.updateSession(currentSession.copy(exercises = updatedExercises))
    }
}