package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.ExploreRepository
import com.ziro.fit.data.repository.TrainerRepository
import com.ziro.fit.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val isLoading: Boolean = false,
    val cities: List<ExploreCity> = emptyList(),
    val categories: List<ExploreCategory> = emptyList(),
    val featuredEvents: List<ExploreEvent> = emptyList(),
    val featuredTrainers: List<TrainerSummary> = emptyList(),
    val allTrainers: List<TrainerSummary> = emptyList(),
    val upcomingEvents: Map<String, List<ExploreEvent>> = emptyMap(),
    val selectedCity: ExploreCity? = null,
    val selectedCategory: ExploreCategory? = null,
    val error: String? = null,
    // -- iOS-parity fields --
    val userLocationCity: String? = null,
    val nearbyTrainers: List<TrainerSummary> = emptyList(),
    val recommendedTrainers: List<TrainerSummary> = emptyList(),
    val sortedDateKeys: List<String> = emptyList(),
    val isSubscribing: Boolean = false,
    val notificationSubscriptionSuccess: Boolean = false,
    val isRefreshing: Boolean = false
)

/**
 * ViewModel for the Explore tab.
 *
 * Responsibilities:
 * - Load metadata (cities, categories) on init
 * - Fetch featured content, promoted trainers, events, and nearby trainers in parallel
 * - React to city/category filter changes
 * - Manage notification subscription state
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ExploreRepository,
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Track the last known lat/lng for "Current Location" queries.
     * Updated externally via [updateLocation] (e.g. from a LocationManager or screen).
     */
    private var currentLat: Double? = null
    private var currentLong: Double? = null

    init {
        loadMetadata()
    }

    /**
     * Fetches metadata (available cities + categories) from the API.
     * Prepends a synthetic "Current Location" city and an "All" category.
     * On success, triggers the first full content load.
     */
    fun loadMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getMetadata().onSuccess { meta ->
                val cities = listOf(
                    ExploreCity("current", "Current Location", isCurrentLocation = true)
                ) + meta.cities
                val categories = listOf(
                    ExploreCategory("all", "All")
                ) + meta.categories
                _uiState.update {
                    it.copy(
                        cities = cities,
                        categories = categories,
                        selectedCity = cities.first()
                    )
                }
                loadContent()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Public entry point for a full refresh of all content sections.
     * Delegates to the private [loadContent] which runs parallel fetches.
     */
    fun refreshContent() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadContent()
    }

    /**
     * Core loader that fetches all explore data sections in parallel:
     * 1. Featured events + algorithmic featured trainers (getFeatured)
     * 2. Promoted FEATURED trainers (getPromotedTrainers)
     * 3. Promoted ZIRO_RECOMMENDED trainers (getPromotedTrainers)
     * 4. Upcoming events list (getEvents) — grouped by date
     * 5. Nearby trainers (getTrainers with sortBy=distance)
     *
     * Each section is fetched independently and failures are handled
     * gracefully so one failed section does not prevent others from loading.
     */
    private fun loadContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val city = _uiState.value.selectedCity
            val category = _uiState.value.selectedCategory

            // Resolve lat/lng/cityId from the currently selected city
            val lat: Double?
            val long: Double?
            val cityId: String?

            if (city?.isCurrentLocation == true) {
                lat = currentLat
                long = currentLong
                cityId = null
            } else {
                cityId = city?.id.takeIf { it != "current" }
                lat = city?.latitude
                long = city?.longitude
            }

            // ── 1. Featured content (events + algorithmic featured trainers) ──
            val featuredDeferred = async {
                repository.getFeatured(lat = lat, long = long, cityId = cityId)
            }

            // ── 2. Promoted FEATURED trainers ──
            val promotedFeaturedDeferred = async {
                repository.getPromotedTrainers("FEATURED")
            }

            // ── 3. Promoted ZIRO_RECOMMENDED trainers ──
            val promotedRecommendedDeferred = async {
                repository.getPromotedTrainers("ZIRO_RECOMMENDED")
            }

            // ── 4. Upcoming events (paginated, grouped by date) ──
            val eventsDeferred = async {
                repository.getEvents(
                    categoryId = category?.id.takeIf { it != "all" },
                    limit = 50
                )
            }

            // ── 5. Nearby trainers ──
            val nearbyDeferred = async {
                val locationName: String? = when {
                    city?.isCurrentLocation == true -> _uiState.value.userLocationCity
                    else -> city?.name
                }
                // Skip nearby fetch when "Current Location" is selected but no coords available
                if (city?.isCurrentLocation == true && lat == null) {
                    Result.success(emptyList<TrainerSummary>())
                } else {
                    trainerRepository.getTrainers(
                        location = locationName,
                        sortBy = "distance",
                        latitude = lat,
                        longitude = long
                    )
                }
            }

            // ── Collect results ──
            // Section 1 — Featured
            featuredDeferred.await().onSuccess { featured ->
                _uiState.update { it.copy(
                    featuredEvents = featured.featuredEvents,
                    featuredTrainers = featured.featuredTrainers.map { it.toTrainerSummary() }
                ) }
            }

            // Section 2 — Featured promoted trainers (merge with algorithmic results)
            promotedFeaturedDeferred.await().onSuccess { trainers ->
                val promoted = trainers.map { it.toTrainerSummary() }
                val currentFeatured = _uiState.value.featuredTrainers
                val merged = (promoted + currentFeatured).distinctBy { it.id }
                _uiState.update { it.copy(featuredTrainers = merged) }
            }

            // Section 3 — Recommended promoted trainers
            promotedRecommendedDeferred.await().onSuccess { trainers ->
                _uiState.update { it.copy(
                    recommendedTrainers = trainers.map { it.toTrainerSummary() }
                ) }
            }

            // Section 4 — Upcoming events + sorted date keys
            eventsDeferred.await().onSuccess { res ->
                val grouped = res.events.groupBy { it.startTime.take(10) } // YYYY-MM-DD
                val sortedKeys = grouped.keys.sorted()
                _uiState.update { it.copy(
                    upcomingEvents = grouped,
                    sortedDateKeys = sortedKeys
                ) }
            }

            // Section 5 — Nearby trainers
            nearbyDeferred.await().onSuccess { trainers ->
                _uiState.update { it.copy(nearbyTrainers = trainers) }
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    /**
     * Selects a city and reloads all content for that city.
     */
    fun selectCity(city: ExploreCity) {
        _uiState.update { it.copy(selectedCity = city) }
        loadContent()
    }

    /**
     * Selects a category and reloads the content filtered by that category.
     */
    fun selectCategory(category: ExploreCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadContent()
    }

    /**
     * Updates the device location used for "Current Location" queries.
     * Called from the UI layer (e.g. when a LocationManager provides an update).
     * If the user has "Current Location" selected, triggers a content reload.
     *
     * @param lat Latitude of the device
     * @param long Longitude of the device
     * @param cityName Optional reverse-geocoded city name for display
     */
    fun updateLocation(lat: Double, long: Double, cityName: String? = null) {
        currentLat = lat
        currentLong = long
        _uiState.update { it.copy(userLocationCity = cityName ?: "Current Location") }

        // Auto-reload if the user has "Current Location" selected
        val city = _uiState.value.selectedCity
        if (city?.isCurrentLocation == true) {
            loadContent()
        }
    }

    /**
     * Subscribes the user to push notifications for explore events.
     * The topic is derived from the currently selected city.
     * On success, [notificationSubscriptionSuccess] is set to true
     * for 3 seconds before auto-resetting.
     */
    fun subscribeToEventNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubscribing = true) }
            val cityId = _uiState.value.selectedCity?.id ?: "any"
            val topic = "general_events_$cityId"

            repository.subscribeToNotifications(topic).onSuccess {
                _uiState.update { it.copy(
                    isSubscribing = false,
                    notificationSubscriptionSuccess = true
                ) }
                // Auto-reset the success indicator after 3 seconds
                delay(3000)
                _uiState.update { it.copy(notificationSubscriptionSuccess = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(
                    isSubscribing = false,
                    error = e.message
                ) }
            }
        }
    }
}
