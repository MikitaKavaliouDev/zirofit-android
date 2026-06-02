package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.ExploreRepository
import com.ziro.fit.model.ExploreEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EventsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val events: List<ExploreEvent> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isFreeOnly: Boolean? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val sortedDateKeys: List<String> = emptyList(),
    val groupedEvents: Map<String, List<ExploreEvent>> = emptyMap()
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: ExploreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(currentPage = 1, events = emptyList(), isLoading = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            val result = repository.getEvents(
                page = _uiState.value.currentPage,
                categoryId = _uiState.value.selectedCategory,
                search = _uiState.value.searchQuery.ifBlank { null },
                isFree = _uiState.value.isFreeOnly
            )

            result.onSuccess { response ->
                val mergedEvents = if (refresh) response.events else _uiState.value.events + response.events
                val (sortedKeys, grouped) = groupEventsByDate(mergedEvents)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        events = mergedEvents,
                        sortedDateKeys = sortedKeys,
                        groupedEvents = grouped,
                        hasMore = response.pagination?.hasMore ?: false,
                        currentPage = (response.pagination?.page ?: state.currentPage) + 1,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = repository.getEvents(
                page = 1,
                categoryId = _uiState.value.selectedCategory,
                search = _uiState.value.searchQuery.ifBlank { null },
                isFree = _uiState.value.isFreeOnly
            )
            result.onSuccess { response ->
                val (sortedKeys, grouped) = groupEventsByDate(response.events)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        events = response.events,
                        sortedDateKeys = sortedKeys,
                        groupedEvents = grouped,
                        hasMore = response.pagination?.hasMore ?: false,
                        currentPage = 2,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isRefreshing = false, error = error.message) }
            }
        }
    }

    fun toggleFreeFilter() {
        val newValue = if (_uiState.value.isFreeOnly == true) null else true
        _uiState.update { it.copy(isFreeOnly = newValue) }
        loadEvents(refresh = true)
    }

    private fun groupEventsByDate(events: List<ExploreEvent>): Pair<List<String>, Map<String, List<ExploreEvent>>> {
        val grouped = events.groupBy { event ->
            try {
                ZonedDateTime.parse(event.startTime).toLocalDate().toString()
            } catch (e: Exception) {
                "Unknown"
            }
        }
        val sortedKeys = grouped.keys.sorted()
        return Pair(sortedKeys, grouped)
    }

    fun setSearchQuery(query: String) {
        onSearchQueryChanged(query)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadEvents(refresh = true)
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update { it.copy(selectedCategory = categoryId) }
        loadEvents(refresh = true)
    }

    fun onFilterFree(isFree: Boolean?) {
        _uiState.update { it.copy(isFreeOnly = isFree) }
        loadEvents(refresh = true)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
