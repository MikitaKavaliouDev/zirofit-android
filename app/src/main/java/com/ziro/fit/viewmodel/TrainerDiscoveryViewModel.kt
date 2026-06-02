package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.TrainerRepository
import com.ziro.fit.data.repository.ExploreRepository
import com.ziro.fit.model.ExploreEvent
import com.ziro.fit.model.ExploreEventsResponse
import com.ziro.fit.model.TrainerSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrainerDiscoveryUiState(
    val isLoading: Boolean = false,
    val trainers: List<TrainerSummary> = emptyList(),
    val events: List<ExploreEvent> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class TrainerDiscoveryViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository,
    private val exploreRepository: ExploreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainerDiscoveryUiState())
    val uiState: StateFlow<TrainerDiscoveryUiState> = _uiState.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val selectedSpecialty = MutableStateFlow<String?>(null)
    val selectedLocation = MutableStateFlow("")
    val minRating = MutableStateFlow(0.0)
    val discoveryType = MutableStateFlow(DiscoveryType.ALL)
    val selectedSortOption = MutableStateFlow(SortOption.CLOSEST)

    // Pagination state
    private var currentPage = 1
    private var canLoadMore = true
    private var currentFilter: FilterState? = null

    // Local fuzzy search cache
    private var cachedTrainers: List<TrainerSummary> = emptyList()
    private var cachedEvents: List<ExploreEvent> = emptyList()

    companion object {
        private const val PAGE_SIZE = 20
    }

    enum class DiscoveryType(val label: String) {
        SPECIALISTS("Specialists"),
        EVENTS("Events"),
        ALL("All")
    }

    enum class SortOption(val label: String, val apiKey: String) {
        CLOSEST("Closest", "distance"),
        BEST_RATED("Highest Rated", "rating"),
        NEWEST("Newest", "newest"),
        NAME_ASC("Name (A-Z)", "name_asc")
    }

    private data class FilterState(
        val search: String,
        val specialty: String?,
        val location: String,
        val rating: Double,
        val type: DiscoveryType,
        val sort: SortOption
    )

    init {
        val searchFlow = searchQuery.debounce(500).distinctUntilChanged()
        
        val filterFlow1 = combine(
            searchFlow,
            selectedSpecialty,
            selectedLocation,
            minRating
        ) { search, specialty, location, rating ->
            Pair(Pair(search, specialty), Pair(location, rating))
        }
        
        val filterFlow2 = combine(
            discoveryType,
            selectedSortOption
        ) { type, sort ->
            Pair(type, sort)
        }
        
        viewModelScope.launch {
            combine(filterFlow1, filterFlow2) { f1, f2 ->
                FilterState(
                    search = f1.first.first,
                    specialty = f1.first.second,
                    location = f1.second.first,
                    rating = f1.second.second,
                    type = f2.first,
                    sort = f2.second
                )
            }.collect { filter ->
                currentFilter = filter
                currentPage = 1
                canLoadMore = true
                loadResults(filter, page = 1)
            }
        }
    }

    private suspend fun loadResults(filter: FilterState, page: Int = 1) {
        val isAppending = page > 1
        if (isAppending) {
            _uiState.update { it.copy(isLoadingMore = true) }
        } else {
            _uiState.update { it.copy(isLoading = true, error = null, isLoadingMore = false) }
        }

        val fetchTrainers = filter.type == DiscoveryType.SPECIALISTS || filter.type == DiscoveryType.ALL
        val fetchEvents = filter.type == DiscoveryType.EVENTS || filter.type == DiscoveryType.ALL

        var trainersList = emptyList<TrainerSummary>()
        var eventsList = emptyList<ExploreEvent>()

        try {
            if (fetchTrainers) {
                val result = trainerRepository.getTrainers(
                    search = filter.search.ifBlank { null },
                    page = page,
                    pageSize = PAGE_SIZE,
                    location = filter.location.ifBlank { null },
                    minRating = if (filter.rating > 0.0) filter.rating else null,
                    specialties = filter.specialty,
                    sortBy = filter.sort.apiKey
                )
                trainersList = result.getOrDefault(emptyList())
            }

            if (fetchEvents) {
                val result = exploreRepository.getEvents(
                    search = filter.search.ifBlank { null },
                    page = page,
                    limit = PAGE_SIZE
                )
                eventsList = result.getOrDefault(ExploreEventsResponse(emptyList(), null)).events
            }

            // Determine if results are "full" (not truncated by search)
            val hasResults = trainersList.isNotEmpty() || eventsList.isNotEmpty()

            if (hasResults) {
                // Update local caches with fresh results
                if (trainersList.isNotEmpty()) cachedTrainers = trainersList
                if (eventsList.isNotEmpty()) cachedEvents = eventsList
            } else if (!isAppending && filter.search.isNotBlank() && (cachedTrainers.isNotEmpty() || cachedEvents.isNotEmpty())) {
                // API returned empty with a search query — fall back to local fuzzy filtering
                val query = filter.search.lowercase()
                trainersList = cachedTrainers.filter { t ->
                    t.name.lowercase().contains(query) ||
                        t.profile?.certifications?.lowercase()?.contains(query) == true ||
                        t.username?.lowercase()?.contains(query) == true
                }
                eventsList = cachedEvents.filter { e ->
                    val host = e.hostName ?: e.trainerName ?: e.trainer?.name
                    e.title.lowercase().contains(query) ||
                        e.locationName.lowercase().contains(query) ||
                        host?.lowercase()?.contains(query) == true
                }
            }

            if (isAppending) {
                val updatedTrainers = _uiState.value.trainers + trainersList
                val updatedEvents = _uiState.value.events + eventsList
                canLoadMore = trainersList.size >= PAGE_SIZE || eventsList.size >= PAGE_SIZE
                currentPage = page
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        trainers = updatedTrainers,
                        events = updatedEvents,
                        currentPage = page,
                        canLoadMore = canLoadMore
                    )
                }
            } else {
                canLoadMore = trainersList.size >= PAGE_SIZE || eventsList.size >= PAGE_SIZE
                currentPage = page
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        trainers = trainersList,
                        events = eventsList,
                        currentPage = page,
                        canLoadMore = canLoadMore
                    )
                }
            }
        } catch (e: Exception) {
            if (isAppending) {
                _uiState.update { it.copy(isLoadingMore = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        if (!canLoadMore || _uiState.value.isLoadingMore) return
        val filter = currentFilter ?: return
        currentPage++
        viewModelScope.launch {
            loadResults(filter, currentPage)
        }
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedSpecialty.value = null
        selectedLocation.value = ""
        minRating.value = 0.0
        selectedSortOption.value = SortOption.CLOSEST
    }
}
