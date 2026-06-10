package com.ziro.fit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val coreInfo: ProfileCoreInfo? = null,
    val branding: ProfileBranding? = null,
    val services: List<ProfileService> = emptyList(),
    val packages: List<ProfilePackage> = emptyList(),
    val availability: ProfileAvailability? = null,
    val transformationPhotos: List<ProfileTransformationPhoto> = emptyList(),
    val testimonials: List<Testimonial> = emptyList(),
    val socialLinks: List<SocialLink> = emptyList(),
    val externalLinks: List<ExternalLink> = emptyList(),
    val billing: ProfileBilling? = null,
    val benefits: List<Benefit> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val bookingWindowSettings: BookingWindowSettings? = null,
    val deleteAccountLoading: Boolean = false,
    val deleteAccountError: String? = null,
    val accountDeleted: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun fetchCoreInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getCoreInfo()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, coreInfo = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchBranding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getBranding()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, branding = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateBranding(bannerUri: android.net.Uri?, profileUri: android.net.Uri?, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val bannerFile = bannerUri?.let { com.ziro.fit.util.FileUtils.getFileFromUri(context, it) }
            val profileFile = profileUri?.let { com.ziro.fit.util.FileUtils.getFileFromUri(context, it) }
            
            val result = repository.updateBranding(bannerFile, profileFile)
            result.onSuccess {
               fetchBranding() // Refresh data
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchServices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getServices()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, services = data.services) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchPackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getPackages()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, packages = data.packages) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchAvailability() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getAvailability()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, availability = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchBookingWindowSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getBookingWindowSettings()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, bookingWindowSettings = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun saveBookingWindowSettings(advanceNoticeHours: Int, bookingHorizonHours: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val settings = BookingWindowSettings(advanceNoticeHours, bookingHorizonHours)
            val result = repository.updateBookingWindowSettings(settings)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, bookingWindowSettings = settings) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    
    fun saveWorkingHours(workingHours: com.ziro.fit.model.WorkingHours) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.updateWorkingHours(workingHours)
            result.onSuccess {
            
                fetchAvailability()
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            
            if (_uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun fetchTransformationPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getTransformationPhotos()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, transformationPhotos = data.photos) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchTestimonials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getTestimonials()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, testimonials = data.testimonials) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchSocialLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getSocialLinks()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, socialLinks = data.links) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchExternalLinks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getExternalLinks()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, externalLinks = data.links) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchBilling() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getBilling()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, billing = data) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchBenefits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getBenefits()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, benefits = data.benefits) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getNotifications()
            result.onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, notifications = data.notifications) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteAccountLoading = true, deleteAccountError = null) }
            val result = repository.deleteAccount()
            result.onSuccess {
                _uiState.update { it.copy(deleteAccountLoading = false, accountDeleted = true) }
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(deleteAccountLoading = false, deleteAccountError = e.message) }
            }
        }
    }

    fun clearDeleteAccountError() {
        _uiState.update { it.copy(deleteAccountError = null) }
    }
}
