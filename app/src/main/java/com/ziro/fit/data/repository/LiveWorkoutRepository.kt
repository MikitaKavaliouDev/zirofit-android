package com.ziro.fit.data.repository

import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.model.ActiveWorkoutStatus
import com.ziro.fit.model.Exercise
import com.ziro.fit.model.GetExercisesResponse
import com.ziro.fit.model.LiveWorkoutUiModel
import com.ziro.fit.model.LogSetRequest
import com.ziro.fit.model.LogSetResponse
import com.ziro.fit.model.NewRecord
import com.ziro.fit.model.ServerLiveSessionResponse
import com.ziro.fit.model.SetStatus
import com.ziro.fit.model.WorkoutExerciseUi
import com.ziro.fit.model.WorkoutSetUi
import com.ziro.fit.model.FinishWorkoutRequest
import com.ziro.fit.model.FinishWorkoutResponse
import com.ziro.fit.model.StartWorkoutRequest
import com.ziro.fit.util.ApiErrorParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class LiveWorkoutRepository @Inject constructor(
    private val api: ZiroApi
) {
    suspend fun getActiveSession(): Result<LiveWorkoutUiModel?> {
        return try {
            val response = api.getActiveSession()
            val data = response.data!!
            if (data.session != null) {
                Result.success(mapResponseToUiModel(data.session))
            } else {
                // Return null when no session is active
                Result.success(null)
            }
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    suspend fun startWorkout(clientId: String?, templateId: String?, plannedSessionId: String?): Result<LiveWorkoutUiModel> {
        return try {
            val response = api.startWorkout(StartWorkoutRequest(clientId, templateId, plannedSessionId))
            Result.success(mapResponseToUiModel(response.data!!.session))
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    // logSet returns NewRecord list when the server detects PRs (mirrors iOS: newRecords in saveSet response)
    suspend fun logSet(
        sessionId: String,
        exerciseId: String,
        reps: Int,
        weight: Double,
        order: Int,
        isCompleted: Boolean? = null,
        logId: String? = null,
        rpe: Double? = null,
        status: SetStatus = SetStatus.NORMAL
    ): Result<List<NewRecord>> {
        return try {
            val statusStr = when (status) {
                SetStatus.NORMAL -> null
                SetStatus.WARM_UP -> "warm_up"
                SetStatus.DROP_SET -> "drop_set"
                SetStatus.FAILURE -> "failure"
            }
            val request = LogSetRequest(sessionId, exerciseId, reps, weight, order, isCompleted, logId, rpe, statusStr)
            api.logSet(request)
            Result.success(emptyList()) // TODO: extend when server returns newRecords in response
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    suspend fun finishSession(sessionId: String, notes: String?): Result<FinishWorkoutResponse> {
        return try {
            val response = api.finishWorkout(
                request = FinishWorkoutRequest(
                    workoutSessionId = sessionId,
                    notes = notes
                )
            )
            val session = response.data!!.session
            val stats = response.data!!.stats ?: calculateStats(session)
            
            Result.success(FinishWorkoutResponse(session, stats))
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    suspend fun cancelActiveWorkout(sessionId: String): Result<Unit> {
        return try {
            api.cancelActiveWorkout(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            // If 404, it might be already gone, treat as success or handle in VM
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }
    
    /**
     * Lightweight polling call for ElevenLabs voice training session.
     * Mirrors iOS: GET /api/client/active-workout
     * Returns minimal payload to detect state transitions.
     */
    suspend fun getActiveWorkoutStatus(): Result<ActiveWorkoutStatus> {
        return try {
            val response = api.getActiveWorkoutStatus()
            Result.success(response.data!!)
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    suspend fun getExercises(query: String?, page: Int = 1): Result<GetExercisesResponse> {
        return try {
            val response = api.getExercises(search = query, page = page)
            Result.success(response.data!!)
        } catch (e: Exception) {
            val apiError = ApiErrorParser.parse(e)
            Result.failure(Exception(ApiErrorParser.getErrorMessage(apiError)))
        }
    }

    private fun calculateStats(session: ServerLiveSessionResponse): com.ziro.fit.model.WorkoutStats {
        val durationSeconds = try {
            val start = java.time.Instant.parse(session.startTime)
            val end = session.endTime?.let { java.time.Instant.parse(it) } ?: java.time.Instant.now()
            java.time.Duration.between(start, end).seconds.toInt()
        } catch (e: Exception) {
            0
        }

        val volumeKg = session.exerciseLogs.sumOf { (it.weight ?: 0.0) * it.reps }
        val setsCompleted = session.exerciseLogs.size
        
        val exerciseSummaries = session.exerciseLogs
            .groupBy { it.exercise.id }
            .map { (id, logs) ->
                val name = logs.first().exercise.name
                val sets = logs.size
                val repsCount = logs.sumOf { it.reps }
                val maxWeight = logs.mapNotNull { it.weight }.maxOrNull() ?: 0.0
                com.ziro.fit.model.ExerciseSummary(id, name, sets, repsCount, maxWeight)
            }

        return com.ziro.fit.model.WorkoutStats(
            durationSeconds = durationSeconds,
            volumeKg = volumeKg,
            setsCompleted = setsCompleted,
            recordsBroken = 0, // Cannot calculate records locally without history
            message = "Workout Summary",
            exerciseSummaries = exerciseSummaries
        )
    }

    private fun mapResponseToUiModel(data: ServerLiveSessionResponse): LiveWorkoutUiModel {
        // 1. Group actual logs by Exercise ID
        val logsByExercise = data.exerciseLogs.groupBy { it.exercise.id }

        // 2. Collect all superset groups from logs (supersetKey → list of exerciseIds)
        val supersetGroups = mutableMapOf<String, MutableList<String>>()
        data.exerciseLogs.forEach { log ->
            log.supersetKey?.let { key ->
                supersetGroups.getOrPut(key) { mutableListOf() }.add(log.exercise.id)
            }
        }

        // Generate stable superset IDs for exercises without one but sharing a group
        val exerciseToSuperSetId = mutableMapOf<String, String>()
        supersetGroups.forEach { (supersetKey, exerciseIds) ->
            if (exerciseIds.size >= 2) {
                exerciseIds.forEach { exerciseId ->
                    exerciseToSuperSetId[exerciseId] = supersetKey
                }
            }
        }

        // 3. Build list from Template (The "Planned" Exercises)
        val uiExercises = mutableListOf<WorkoutExerciseUi>()
        
        // Track which exercises we've handled to identify ad-hoc ones later
        val processedExerciseIds = mutableSetOf<String>()

        val sortedTemplateExercises = data.workoutTemplate?.exercises?.sortedBy { it.order } ?: emptyList()
        for (i in sortedTemplateExercises.indices) {
            val templateStep = sortedTemplateExercises[i]
            
            // Skip REST steps - they are absorbed into the preceding exercise's rest
            if (templateStep.isRest == true) {
                continue
            }
            
            // Calculate rest duration by looking ahead at consecutive REST steps
            var restDuration = templateStep.restSeconds
            var j = i + 1
            while (j < sortedTemplateExercises.size) {
                val nextStep = sortedTemplateExercises[j]
                if (nextStep.isRest == true) {
                    restDuration = (restDuration ?: 0) + (nextStep.durationSeconds ?: 0)
                    j++
                } else {
                    break
                }
            }
            
            // SAFTEY FIX: Fallback if exercise info or ID is missing from API
            val exerciseId = templateStep.exerciseId ?: templateStep.id 
            
            // Try to get name from object, or parse from notes, or fallback
            val exerciseName = templateStep.exercise?.name ?: run {
                // Try to extract "Exercise: Name." from notes if available
                val noteName = templateStep.notes?.let { note ->
                    val match = Regex("Exercise: (.*?)[\\.,]").find(note)
                    match?.groupValues?.get(1)
                }
                noteName ?: "Exercise ${templateStep.order}"
            }
            
            processedExerciseIds.add(exerciseId)
            
            val logsForThisExercise = logsByExercise[exerciseId] ?: emptyList()
            
            // CRITICAL LOGIC: Merging Plan + Logs
            // If plan says 3 sets, but we logged 1, we show 1 real + 2 ghosts.
            val targetSetsCount = templateStep.targetSets ?: 0
            val actualLogsCount = logsForThisExercise.size
            
            // Determine total rows to show (at least as many as logged, or up to target)
            val setsToShowCount = max(targetSetsCount, actualLogsCount)
            // Ensure at least one set is shown if everything else is empty
            val finalSetsCount = max(setsToShowCount, 1)

            val setsUi = (0 until finalSetsCount).map { index ->
                // Try to find a real log for this index
                val existingLog = logsForThisExercise.find { it.order == index }
                
                if (existingLog != null) {
                    // Real Log (Saved in DB)
                    WorkoutSetUi(
                        logId = existingLog.id,
                        setNumber = index + 1,
                        weight = existingLog.weight?.toString() ?: "",
                        reps = existingLog.reps.toString(),
                        isCompleted = true, // It exists, so it's logged
                        order = index,
                        status = SetStatus.NORMAL // ServerExerciseLog has no status field yet; extend when API supports it
                    )
                } else {
                    // Ghost Set (Placeholder based on template)
                    WorkoutSetUi(
                        logId = null, // No ID yet
                        setNumber = index + 1,
                        weight = "", // Start empty
                        reps = "", // Start empty
                        isCompleted = false,
                        order = index,
                        status = SetStatus.NORMAL
                    )
                }
            }

            uiExercises.add(
                WorkoutExerciseUi(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    targetReps = templateStep.targetReps,
                    restSeconds = restDuration,
                    sets = setsUi,
                    superSetId = exerciseToSuperSetId[exerciseId]
                )
            )
        }

        // 3b. Handle Ad-Hoc Exercises (Logged but not in Template)
        logsByExercise.forEach { (exerciseId, logs) ->
            if (!processedExerciseIds.contains(exerciseId)) {
                val firstLog = logs.first()
                
                val setsUi = logs.sortedBy { it.order }.mapIndexed { index, log ->
                    WorkoutSetUi(
                        logId = log.id,
                        setNumber = index + 1,
                        weight = (log.weight ?: 0.0).toString(),
                        reps = log.reps.toString(),
                        isCompleted = true,
                        order = log.order,
                        status = SetStatus.NORMAL
                    )
                }

                uiExercises.add(
                    WorkoutExerciseUi(
                        exerciseId = exerciseId,
                        exerciseName = firstLog.exercise.name,
                        targetReps = null, // No target for ad-hoc
                        restSeconds = null,
                        sets = setsUi,
                        superSetId = exerciseToSuperSetId[exerciseId]
                    )
                )
            }
        }

        return LiveWorkoutUiModel(
            id = data.id,
            title = data.workoutTemplate?.name ?: "Freestyle Workout",
            startTime = data.startTime,
            exercises = uiExercises,
            clientId = data.client?.id,
            clientName = data.client?.name,
            totalPackageSessions = null,
            remainingPackageSessions = null
        )
    }
}