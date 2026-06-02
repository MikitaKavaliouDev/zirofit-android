package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.AICoachRepository

import com.ziro.fit.model.GoalSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AICoachStep {
    INPUT,
    SELECTION,
    METRICS,
    GENERATING,
    SUCCESS
}

data class AICoachUiState(
    val step: AICoachStep = AICoachStep.INPUT,
    val userInput: String = "",
    val suggestions: List<GoalSuggestion> = emptyList(),
    val selectedSuggestion: GoalSuggestion? = null,
    val requiredMetrics: List<String> = emptyList(),
    val metricValues: Map<String, String> = emptyMap(),
    val generatedProgramId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val aiCoachRepository: AICoachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AICoachUiState())
    val uiState: StateFlow<AICoachUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    fun onUserInputChange(input: String) {
        _uiState.update { it.copy(userInput = input, error = null) }
    }

    fun analyzeGoal() {
        val input = _uiState.value.userInput
        if (input.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val result = aiCoachRepository.refineGoal(input)
            if (result.isSuccess) {
                val data = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestions = data?.suggestions ?: emptyList(),
                        requiredMetrics = emptyList(), // Clear previous metrics until selection
                        step = AICoachStep.SELECTION
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to analyze goal"
                    )
                }
            }
        }
    }

    fun selectSuggestion(suggestion: GoalSuggestion) {
        val metrics = suggestion.requiredMetrics
        
        _uiState.update {
            it.copy(
                selectedSuggestion = suggestion,
                requiredMetrics = metrics,
                metricValues = emptyMap() // Reset values for new selection
            )
        }
        
        if (metrics.isEmpty()) {
             // specific logic if no metrics are needed
             // We can treat it as ready to generate, maybe stay on selection but show generate button?
             // Or skip to a "Review" step? 
             // For now, let's go to METRICS step anyway, implying "No explicit metrics needed, just confirm" 
             // or similar. The Metrics view needs to handle empty list gracefully.
             _uiState.update { it.copy(step = AICoachStep.METRICS) }
        } else {
            _uiState.update { it.copy(step = AICoachStep.METRICS) }
        }
    }

    fun updateMetric(key: String, value: String) {
        _uiState.update {
            val newMetrics = it.metricValues.toMutableMap()
            newMetrics[key] = value
            it.copy(metricValues = newMetrics)
        }
    }

    fun generateProgram() {
        val currentState = _uiState.value
        val suggestion = currentState.selectedSuggestion ?: return
        
        // Ensure all metrics are filled if any
        if (currentState.requiredMetrics.any { !currentState.metricValues.containsKey(it) || currentState.metricValues[it].isNullOrBlank() }) {
            _uiState.update { it.copy(error = "Please fill in all metrics") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, step = AICoachStep.GENERATING) }

        // Cancel any previous generation job
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            // Fetch current user ID to use as clientId
            val userResult = aiCoachRepository.getCurrentUserId()
            val clientId = userResult.getOrNull()
            
            if (clientId == null) {
                 _uiState.update { it.copy(isLoading = false, error = "Failed to get user profile") }
                 return@launch
            }

            val result = aiCoachRepository.generateProgram(
                clientId = clientId,
                goal = suggestion.title, 
                focus = suggestion.focus,
                metrics = currentState.metricValues
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generatedProgramId = result.getOrNull(),
                        step = AICoachStep.SUCCESS
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        step = AICoachStep.METRICS, // Go back to allow retry
                        error = result.exceptionOrNull()?.message ?: "Failed to generate program"
                    )
                }
            }
        }
    }
    
    fun cancelGeneration() {
        generateJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                step = AICoachStep.INPUT,
                error = null,
                generatedProgramId = null,
                suggestions = emptyList(),
                selectedSuggestion = null,
                requiredMetrics = emptyList(),
                metricValues = emptyMap()
            )
        }
    }
    
    fun reset() {
        generateJob?.cancel()
        _uiState.value = AICoachUiState()
    }
}
