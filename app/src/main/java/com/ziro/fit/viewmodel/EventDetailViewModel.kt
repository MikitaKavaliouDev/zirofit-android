package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.BillingRepository
import com.ziro.fit.data.repository.ExploreRepository
import com.ziro.fit.model.ExploreEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventDetailUiState(
    val isLoading: Boolean = false,
    val event: ExploreEvent? = null,
    val error: String? = null,
    val checkoutUrl: String? = null,
    val joinSuccess: Boolean = false,
    val showSuccessDialog: Boolean = false
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val exploreRepository: ExploreRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    fun loadEventDetails(eventId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = exploreRepository.getEventDetails(eventId)
            result.onSuccess { event ->
                _uiState.update { it.copy(isLoading = false, event = event) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun enroll(event: ExploreEvent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val eventId = event.id.takeIf { it.isNotBlank() }
            if (eventId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Invalid event: missing ID") }
                return@launch
            }
            
            if (event.price == null || event.price <= 0.0) {
                // Free event
                exploreRepository.joinFreeEvent(eventId)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, joinSuccess = true, showSuccessDialog = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
            } else {
                // Paid event
                billingRepository.createCheckoutSession(eventId = eventId, type = "EVENT_TICKET")
                    .onSuccess { url ->
                        _uiState.update { it.copy(isLoading = false, checkoutUrl = url) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
            }
        }
    }

    fun clearCheckoutUrl() {
        _uiState.update { it.copy(checkoutUrl = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }
}
