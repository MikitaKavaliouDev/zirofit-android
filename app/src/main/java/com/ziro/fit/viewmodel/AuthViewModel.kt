package com.ziro.fit.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ziro.fit.auth.GoogleAuthManager
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.data.repository.CalendarRepository
import com.ziro.fit.data.repository.ClientDashboardRepository
import com.ziro.fit.data.repository.ExerciseRepository
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.model.AppMode
import com.ziro.fit.model.ForgotPasswordRequest
import com.ziro.fit.model.LoginRequest
import com.ziro.fit.model.RegisterRequest
import com.ziro.fit.model.SignOutRequest
import com.ziro.fit.model.UpdatePasswordRequest
import com.ziro.fit.model.User
import com.ziro.fit.util.ApiErrorParser
import com.ziro.fit.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.ziro.fit.data.local.UserSessionManager
import com.ziro.fit.model.ProfileCoreInfo

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(
            val role: String,
            val userId: String,
            val isOnboardingComplete: Boolean = true
    ) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    data class EmailConfirmationRequired(val email: String) : AuthState()
}

@HiltViewModel
class AuthViewModel
@Inject
constructor(
        private val api: ZiroApi,
        private val tokenManager: TokenManager,
        private val googleAuthManager: GoogleAuthManager,
        private val profileRepository: ProfileRepository,
        private val dashboardRepository: ClientDashboardRepository,
        private val calendarRepository: CalendarRepository,
        private val exerciseRepository: ExerciseRepository,
        private val userSessionManager: UserSessionManager
) : ViewModel() {

    var authState by mutableStateOf<AuthState>(AuthState.Loading)
        private set

    var uiLoading by mutableStateOf(false)
        private set
    var uiError by mutableStateOf<String?>(null)
        private set

    var activeMode by mutableStateOf(AppMode.TRAINER)
        private set

    var isTrainerAuthenticated by mutableStateOf(false)
        private set
    var isPersonalAuthenticated by mutableStateOf(false)
        private set
    
    var justCompletedOnboarding by mutableStateOf(false)
        private set

    fun resetJustCompletedOnboarding() {
        justCompletedOnboarding = false
    }

    private var trainerUserId: String? = null
    private var personalUserId: String? = null

    val isAnyAuthenticated: Boolean
        get() = isTrainerAuthenticated || isPersonalAuthenticated

    init {
        activeMode = tokenManager.activeMode.value
        tokenManager.activeMode.onEach { mode -> activeMode = mode }.launchIn(viewModelScope)

        setupLogoutCollection()
        checkAuthStatus()
    }

    private fun setupLogoutCollection() {
        viewModelScope.launch {
            tokenManager.logoutSignal.collect {
                isTrainerAuthenticated = false
                isPersonalAuthenticated = false
                authState = AuthState.Unauthenticated
            }
        }
    }

    private fun tryAlternativeAuth() {
        viewModelScope.launch {
            val currentMode = tokenManager.activeMode.value
            Logger.d("AuthViewModel", "Trying alternative auth for mode: $currentMode")
            // Try to refresh using refresh token
            val refreshSuccess = checkRefreshTokenAndRefreshIfNeeded(currentMode)
            if (refreshSuccess) {
                            Logger.d("AuthViewModel", "Token refreshed, calling checkAuthStatus()")

                checkAuthStatus()
            } else {
                // Check if other mode has a token
                val otherMode =
                        if (currentMode == AppMode.TRAINER) AppMode.PERSONAL else AppMode.TRAINER
                if (tokenManager.hasToken(otherMode)) {
                    tokenManager.setActiveMode(otherMode)
                    checkAuthStatus()
                } else {
                    authState = AuthState.Unauthenticated
                }
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            authState = AuthState.Loading
            val currentMode = tokenManager.activeMode.value
            val token = tokenManager.getToken(currentMode)
            Logger.d("AuthViewModel", "checkAuthStatus: token = $token")

            if (token == null) {
                Logger.d("AuthViewModel", "No token, calling tryAlternativeAuth()")
                tryAlternativeAuth()
                return@launch
            }
            Logger.d("AuthViewModel", "Token exists, calling api.getMe()")

            try {
                val userResponse = api.getMe()
                val user = userResponse.data
                if (user != null) {
                    val detectedMode = detectModeFromRole(user.role ?: "client")
                    
                    if (detectedMode != currentMode) {
                        Logger.d("AuthViewModel", "Mode mismatch: saved=$currentMode, detected=$detectedMode. Switching without clearing tokens...")
                        tokenManager.setActiveMode(detectedMode)
                    }
                    
                    setAuthedStateForMode(user, detectedMode)
                } else {
                    handleUnauthenticated()
                }
            } catch (e: Exception) {
                tryAlternativeAuth()
            }
        }
    }
    private suspend fun fetchAndSaveCoreInfo() {
        try {
            val response = api.getCoreInfo()
            Logger.d("AuthViewModel", "Core info response: $response")
            response.data?.coreInfo?.let { coreInfo ->
                userSessionManager.saveCoreInfo(coreInfo)
            }
        } catch (e: Exception) {
            // Log error but don't block onboarding flow
            Logger.e("AuthViewModel", "Failed to fetch core info", e)

        }
    }


    private suspend fun setAuthedStateForMode(user: User, mode: AppMode) {
        val role = user.role ?: "pending"
        Logger.d("AuthViewModel", "User $user")
        if (!user.hasCompletedOnboarding) {
            fetchAndSaveCoreInfo()
        }
        authState =
                AuthState.Authenticated(
                        role,
                        user.id,
                        isOnboardingComplete = user.hasCompletedOnboarding
                )
        markModeAuthenticated(mode, user.id)
        syncPushToken()
         if (user.hasCompletedOnboarding) {
        triggerPrefetch(mode)
    }
      
    }

    private suspend fun checkRefreshTokenAndRefreshIfNeeded(mode: AppMode): Boolean {
        val refreshToken = tokenManager.getRefreshToken(mode)
        Logger.d(
                "AuthViewModel",
                "Attempting token refresh for mode: $mode with refresh token: $refreshToken"
        )

        if (!refreshToken.isNullOrEmpty()) {
            val refreshSuccess = tokenManager.refreshToken(mode)
            if (refreshSuccess) {
                return true
            }
        }
        return false
    }

    private fun saveTokensAndSwitchMode(
            accessToken: String,
            refreshToken: String?,
            detectedMode: AppMode
    ) {
        val currentMode = tokenManager.activeMode.value

        if (detectedMode != currentMode) {
            tokenManager.setActiveMode(detectedMode)
        }

        tokenManager.saveToken(accessToken, detectedMode)
        refreshToken?.let { tokenManager.saveRefreshToken(it, detectedMode) }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            uiLoading = true
            uiError = null
            try {
                val response = api.login(LoginRequest(email, pass))
                val loginData = response.data
                Logger.d("TEST", "Login response: $response")
                if (loginData == null) {
                    uiError = response.message ?: "Login failed: No data received"
                    throw Exception("Login failed: No data received")
                }

                val detectedMode = detectModeFromRole(loginData.role)

                saveTokensAndSwitchMode(loginData.accessToken, loginData.refreshToken, detectedMode)

                setAuthedStateForMode(loginData.user, detectedMode)
            } catch (e: Exception) {
                val apiError = ApiErrorParser.parse(e)
                uiError = ApiErrorParser.getErrorMessage(apiError)
            } finally {
                uiLoading = false
            }
        }
    }
    private fun processSocialAuth(
            accessToken: String,
            refreshToken: String?,
            userId: String,
            role: String
    ) {
        viewModelScope.launch {
            uiLoading = true
            uiError = null
            try {
                val detectedMode = detectModeFromRole(role)
                saveTokensAndSwitchMode(accessToken, refreshToken, detectedMode)
                checkAuthStatus()
            } finally {
                uiLoading = false
            }
        }
    }

    fun handleGoogleAuthResult(
            accessToken: String,
            refreshToken: String?,
            userId: String,
            role: String
    ) {
        processSocialAuth(accessToken, refreshToken, userId, role)
    }

    fun handleAppleAuthResult(
            idToken: String,
            authCode: String,
            email: String?,
            userId: String,
            role: String
    ) {
        // Apple uses idToken as accessToken
        processSocialAuth(idToken, null, userId, role)
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            uiLoading = true
            uiError = null
            try {
                val deepLinkRedirect = "zirofitapp://login?verified=true"
                val response =
                        api.register(
                                RegisterRequest(name, email, pass, redirect = deepLinkRedirect)
                        )
                if (response.success == true || response.data != null) {
                    val data = response.data
                    if (data?.confirmationRequired == true) {
                        uiLoading = false
                        authState = AuthState.EmailConfirmationRequired(email)
                    } else {
                        login(email, pass)
                    }
                } else {
                    uiError = response.message ?: "Registration failed"
                    uiLoading = false
                }
            } catch (e: Exception) {
                val apiError = ApiErrorParser.parse(e)
                uiError = ApiErrorParser.getErrorMessage(apiError)
                uiLoading = false
            }
        }
    }

    fun setMode(mode: AppMode) {
        if (activeMode == mode) return

        viewModelScope.launch {
            val previousMode = activeMode
            tokenManager.setActiveMode(mode)
            activeMode = mode

            val alreadyAuth = tokenManager.hasToken(mode)
            if (alreadyAuth) {
                checkAuthStatus()
                return@launch
            }
            markModeUnauthenticated(mode)
            val currentState = authState
            if (currentState is AuthState.Authenticated &&
                            currentState.userId == getUserIdForMode(previousMode)
            ) {
                authState = AuthState.Unauthenticated
            }
            checkAuthStatus()
        }
    }

    private fun markModeAuthenticated(mode: AppMode, userId: String) {
        when (mode) {
            AppMode.TRAINER -> {
                isTrainerAuthenticated = true
                trainerUserId = userId
            }
            AppMode.PERSONAL -> {
                isPersonalAuthenticated = true
                personalUserId = userId
            }
        }
    }

    private fun markModeUnauthenticated(mode: AppMode) {
        when (mode) {
            AppMode.TRAINER -> {
                isTrainerAuthenticated = false
                trainerUserId = null
            }
            AppMode.PERSONAL -> {
                isPersonalAuthenticated = false
                personalUserId = null
            }
        }
    }

    private fun getUserIdForMode(mode: AppMode): String? {
        return when (mode) {
            AppMode.TRAINER -> trainerUserId
            AppMode.PERSONAL -> personalUserId
        }
    }

    private fun getRoleForMode(mode: AppMode): String {
        return when (mode) {
            AppMode.TRAINER -> "trainer"
            AppMode.PERSONAL -> "client"
        }
    }

    private fun detectModeFromRole(role: String): AppMode {
        val r = role.lowercase()
        return if (r.contains("trainer") ||
                        r.contains("coach") ||
                        r.contains("instructor") ||
                        r.contains("admin") ||
                        r.contains("staff") ||
                        r.contains("owner")
        ) {
            AppMode.TRAINER
        } else {
            AppMode.PERSONAL
        }
    }

    private fun triggerPrefetch(mode: AppMode) {
        viewModelScope.launch {
            when (mode) {
                AppMode.TRAINER -> {
                    launch { calendarRepository.getEvents(LocalDate.now()) }
                    launch { calendarRepository.getCalendarSummary(LocalDate.now()) }
                    launch { exerciseRepository.getExercises(null, 1) }
                }
                AppMode.PERSONAL -> {
                    launch { dashboardRepository.getClientDashboard() }
                    launch { dashboardRepository.getWorkoutHistory(20, null) }
                    launch { dashboardRepository.getActiveProgramProgress() }
                    launch { exerciseRepository.getExercises(null, 1) }
                }
            }
        }
    }

    fun completeLocalOnboarding(role: String) {
        viewModelScope.launch {
            val currentState = authState
            if (currentState is AuthState.Authenticated) {
                val detectedMode = detectModeFromRole(role)
                
                if (detectedMode != activeMode) {
                    tokenManager.setActiveMode(detectedMode)
                }
                
                authState = AuthState.Authenticated(role, currentState.userId, isOnboardingComplete = true)
                
                if (role == "client") {
                    justCompletedOnboarding = true
                }
                
                triggerPrefetch(detectedMode)
            }
        }
    }

    private fun syncPushToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                profileRepository.registerPushToken(token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(mode: AppMode? = null) {
        viewModelScope.launch {
            val targetMode = mode ?: activeMode

            val token = tokenManager.getToken(targetMode)
            if (token != null) {
                try {
                    api.signOut(SignOutRequest(token))
                } catch (_: Exception) {}
            }

            tokenManager.clearToken(targetMode)
            markModeUnauthenticated(targetMode)

            val otherMode = if (targetMode == AppMode.TRAINER) AppMode.PERSONAL else AppMode.TRAINER
            if (tokenManager.hasToken(otherMode)) {
                tokenManager.setActiveMode(otherMode)
                activeMode = otherMode
                val otherUserId = getUserIdForMode(otherMode)
                val otherAuth = isTrainerAuthenticated || isPersonalAuthenticated
                authState =
                        AuthState.Authenticated(
                                role = getRoleForMode(otherMode),
                                userId = otherUserId ?: "",
                                isOnboardingComplete = true
                        )
            } else {
                authState = AuthState.Unauthenticated
            }
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            val currentToken = tokenManager.getToken(activeMode)
            if (currentToken != null) {
                try {
                    api.signOut(SignOutRequest(currentToken))
                } catch (_: Exception) {}
            }
            tokenManager.triggerLogoutAll()
        }
    }

    private fun handleUnauthenticated() {
        tokenManager.clearToken()
        authState = AuthState.Unauthenticated
        isTrainerAuthenticated = false
        isPersonalAuthenticated = false
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            uiLoading = true
            uiError = null
            try {
                val response = api.forgotPassword(ForgotPasswordRequest(email))
                if (response.success != false && response.data != null) {
                    uiLoading = false
                } else {
                    uiError = response.message ?: "Failed to send reset email"
                    uiLoading = false
                }
            } catch (e: Exception) {
                val apiError = ApiErrorParser.parse(e)
                uiError = ApiErrorParser.getErrorMessage(apiError)
                uiLoading = false
            }
        }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch {
            uiLoading = true
            uiError = null
            try {
                val response = api.updatePassword(UpdatePasswordRequest(password))
                if (response.success != false && response.data != null) {
                    uiLoading = false
                } else {
                    uiError = response.message ?: "Failed to update password"
                    uiLoading = false
                }
            } catch (e: Exception) {
                val apiError = ApiErrorParser.parse(e)
                uiError = ApiErrorParser.getErrorMessage(apiError)
                uiLoading = false
            }
        }
    }

    fun clearError() {
        uiError = null
    }

    fun resetToUnauthenticated() {
        authState = AuthState.Unauthenticated
    }
}
