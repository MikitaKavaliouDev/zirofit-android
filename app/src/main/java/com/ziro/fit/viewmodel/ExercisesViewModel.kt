package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.LiveWorkoutRepository
import com.ziro.fit.model.Exercise
import com.ziro.fit.model.GetExercisesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: LiveWorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExercisesUiState())
    val uiState: StateFlow<ExercisesUiState> = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    private var currentPage = 1
    private var hasMore = true
    // Keep track of the current query to know if we need to reset
    private var currentQuery: String? = null
    private var searchJob: Job? = null

    fun loadExercises(query: String? = null) {
        // Cancel any in-flight search to prevent stale responses overwriting newer ones
        searchJob?.cancel()

        // If query changed, reset everything
        if (query != currentQuery) {
            currentPage = 1
            hasMore = true
            currentQuery = query
            _uiState.value = _uiState.value.copy(exercises = emptyList(), isLoading = true, error = null)
        } else {
             // If just refreshing or initial load with same query
             if (currentPage == 1) {
                  _uiState.value = _uiState.value.copy(isLoading = true, error = null)
             }
        }

        fetchExercisesInternal()
    }

    fun loadNextPage() {
        if (!uiState.value.isLoading && hasMore) {
             fetchExercisesInternal()
        }
    }

    private fun fetchExercisesInternal() {
        searchJob = viewModelScope.launch {
            repository.getExercises(currentQuery, currentPage)
                .onSuccess { response ->
                    val currentList = if (currentPage == 1) emptyList() else _uiState.value.exercises
                    val updatedList = currentList + response.exercises
                    
                    hasMore = response.hasMore
                    if (hasMore) {
                        currentPage++
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        exercises = updatedList
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.localizedMessage ?: "Failed to load exercises"
                    )
                }
        }
    }
}
