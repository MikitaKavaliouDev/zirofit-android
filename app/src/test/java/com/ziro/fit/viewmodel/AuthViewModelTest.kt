package com.ziro.fit.viewmodel

import android.util.Log
import com.ziro.fit.auth.GoogleAuthManager
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.local.UserSessionManager
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.data.repository.CalendarRepository
import com.ziro.fit.data.repository.ClientDashboardRepository
import com.ziro.fit.data.repository.ExerciseRepository
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.model.*
import com.ziro.fit.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: ZiroApi = mockk(relaxed = true)
    private val googleAuthManager: GoogleAuthManager = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val dashboardRepository: ClientDashboardRepository = mockk(relaxed = true)
    private val calendarRepository: CalendarRepository = mockk(relaxed = true)
    private val exerciseRepository: ExerciseRepository = mockk(relaxed = true)
    private val userSessionManager: UserSessionManager = mockk(relaxed = true)

    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        tokenManager = mockk(relaxed = true)
        every { tokenManager.logoutSignal } returns MutableSharedFlow()
        every { tokenManager.activeMode } returns MutableStateFlow(AppMode.TRAINER)
        every { tokenManager.getToken(any()) } returns null
        every { tokenManager.hasToken(any()) } returns false
        every { tokenManager.hasAnyToken() } returns false
    }

    private fun createViewModel(mode: AppMode = AppMode.TRAINER): AuthViewModel {
        every { tokenManager.activeMode } returns MutableStateFlow(mode)
        return AuthViewModel(
            api = api,
            tokenManager = tokenManager,
            googleAuthManager = googleAuthManager,
            profileRepository = profileRepository,
            dashboardRepository = dashboardRepository,
            calendarRepository = calendarRepository,
            exerciseRepository = exerciseRepository,
            userSessionManager = userSessionManager
        )
    }

    private val authTestUser = User(
        id = "user_1",
        email = "test@example.com",
        name = "Test",
        role = "trainer",
        username = null,
        hasCompletedOnboarding = true,
        subscriptionStatus = null,
        profilePhotoPath = null,
        isFreeAccessModeEnabled = false,
        tier = null
    )

    @Test
    fun `handleGoogleAuthResult saves tokens and sets authenticated state`() = runTest {
        viewModel = createViewModel(AppMode.TRAINER)
        advanceUntilIdle()

        every { tokenManager.getToken(any()) } returns "saved_token"
        coEvery { api.getMe() } returns ApiResponse(success = true, data = authTestUser)

        viewModel.handleGoogleAuthResult(
            accessToken = "google_token_123",
            refreshToken = "refresh_123",
            userId = "user_1",
            role = "trainer"
        )
        advanceUntilIdle()

        verify { tokenManager.saveToken("google_token_123", AppMode.TRAINER) }
        verify { tokenManager.saveRefreshToken("refresh_123", AppMode.TRAINER) }

        val state = viewModel.authState
        assertTrue(state is AuthState.Authenticated)
        assertEquals("trainer", (state as AuthState.Authenticated).role)
        assertEquals("user_1", state.userId)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `handleGoogleAuthResult switches mode when role differs from current mode`() = runTest {
        viewModel = createViewModel(AppMode.TRAINER)
        advanceUntilIdle()

        viewModel.handleGoogleAuthResult(
            accessToken = "google_token_123",
            refreshToken = "refresh_123",
            userId = "user_1",
            role = "client"
        )
        advanceUntilIdle()

        verify(exactly = 0) { tokenManager.clearToken(any()) }
        verify { tokenManager.setActiveMode(AppMode.PERSONAL) }
        verify { tokenManager.saveToken("google_token_123", AppMode.PERSONAL) }
        verify { tokenManager.saveRefreshToken("refresh_123", AppMode.PERSONAL) }
    }

    @Test
    fun `handleGoogleAuthResult sets pending user as not onboarding complete`() = runTest {
        viewModel = createViewModel(AppMode.TRAINER)
        advanceUntilIdle()

        val pendingUser = authTestUser.copy(
            role = "pending",
            hasCompletedOnboarding = false
        )
        every { tokenManager.getToken(any()) } returns "saved_token"
        coEvery { api.getMe() } returns ApiResponse(success = true, data = pendingUser)

        viewModel.handleGoogleAuthResult(
            accessToken = "google_token_123",
            refreshToken = null,
            userId = "user_1",
            role = "pending"
        )
        advanceUntilIdle()

        val state = viewModel.authState
        assertTrue(state is AuthState.Authenticated)
        assertFalse((state as AuthState.Authenticated).isOnboardingComplete)
    }

    @Test
    fun `register with confirmationRequired shows email confirmation state`() = runTest {
        coEvery { api.register(any()) } returns ApiResponse(
            success = true,
            data = RegisterResponse(
                userId = "user_1",
                message = "Check your email",
                requiresSubscription = null,
                confirmationRequired = true
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()

        val state = viewModel.authState
        assertTrue(state is AuthState.EmailConfirmationRequired)
        assertEquals("test@example.com", (state as AuthState.EmailConfirmationRequired).email)
        coVerify(exactly = 0) { api.login(any()) }
    }

    @Test
    fun `register without confirmationRequired auto-logs in`() = runTest {
        coEvery { api.register(any()) } returns ApiResponse(
            success = true,
            data = RegisterResponse(
                userId = "user_1",
                message = "Registered",
                requiresSubscription = null,
                confirmationRequired = false
            )
        )

        val loginResponse = LoginResponse(
            message = "Login successful",
            role = "trainer",
            accessToken = "access_token_123",
            refreshToken = "refresh_token_123",
            user = User(
                id = "user_1",
                email = "test@example.com",
                name = "Test",
                role = "trainer",
                username = null,
                hasCompletedOnboarding = false,
                subscriptionStatus = null,
                profilePhotoPath = null,
                isFreeAccessModeEnabled = false,
                tier = null
            )
        )
        coEvery { api.login(any()) } returns ApiResponse(success = true, data = loginResponse)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()

        coVerify { api.login(any()) }

        val state = viewModel.authState
        assertTrue(state is AuthState.Authenticated)
    }

    @Test
    fun `register failure sets uiError`() = runTest {
        coEvery { api.register(any()) } throws RuntimeException("Network error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()

        assertNotNull(viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `register sends deep link redirect in request`() = runTest {
        coEvery { api.register(any()) } returns ApiResponse(
            success = true,
            data = RegisterResponse(
                userId = "user_1",
                message = "Check your email",
                requiresSubscription = null,
                confirmationRequired = true
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()

        coVerify {
            api.register(match { it.redirect == "zirofitapp://login?verified=true" })
        }
    }

    @Test
    fun `resetToUnauthenticated sets auth state to Unauthenticated`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.resetToUnauthenticated()

        assertTrue(viewModel.authState is AuthState.Unauthenticated)
    }

    @Test
    fun `clearError sets uiError to null`() = runTest {
        coEvery { api.register(any()) } throws RuntimeException("Network error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()
        assertNotNull(viewModel.uiError)

        viewModel.clearError()
        assertNull(viewModel.uiError)
    }

    @Test
    fun `register without server success sets error message`() = runTest {
        coEvery { api.register(any()) } returns ApiResponse(
            success = false,
            data = null,
            message = "Email already registered",
            error = null
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.register("Test", "test@example.com", "password123")
        advanceUntilIdle()

        assertEquals("Email already registered", viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `handleGoogleAuthResult does not save refreshToken when null`() = runTest {
        viewModel = createViewModel(AppMode.TRAINER)
        advanceUntilIdle()

        viewModel.handleGoogleAuthResult(
            accessToken = "token_only",
            refreshToken = null,
            userId = "user_1",
            role = "trainer"
        )
        advanceUntilIdle()

        verify { tokenManager.saveToken("token_only", AppMode.TRAINER) }
        verify(exactly = 0) { tokenManager.saveRefreshToken(any(), any()) }
    }

    @Test
    fun `forgotPassword success clears uiError`() = runTest {
        coEvery { api.forgotPassword(any()) } returns ApiResponse(
            success = true,
            data = ForgotPasswordResponse("Reset email sent")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.forgotPassword("test@example.com")
        advanceUntilIdle()

        coVerify { api.forgotPassword(match { it.email == "test@example.com" }) }
        assertNull(viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `forgotPassword failure sets uiError`() = runTest {
        coEvery { api.forgotPassword(any()) } throws RuntimeException("Network error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.forgotPassword("test@example.com")
        advanceUntilIdle()

        assertNotNull(viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `forgotPassword API returns success=false sets uiError`() = runTest {
        coEvery { api.forgotPassword(any()) } returns ApiResponse(
            success = false,
            data = null,
            message = "Email not found",
            error = null
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.forgotPassword("unknown@example.com")
        advanceUntilIdle()

        assertEquals("Email not found", viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `forgotPassword sets uiLoading correctly`() = runTest {
        coEvery { api.forgotPassword(any()) } returns ApiResponse(
            success = true,
            data = ForgotPasswordResponse("Reset email sent")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiLoading)

        viewModel.forgotPassword("test@example.com")
        advanceUntilIdle()

        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `updatePassword success clears uiError`() = runTest {
        coEvery { api.updatePassword(any()) } returns ApiResponse(
            success = true,
            data = UpdatePasswordResponse("Password updated")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updatePassword("newpassword123")
        advanceUntilIdle()

        coVerify { api.updatePassword(match { it.password == "newpassword123" }) }
        assertNull(viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `updatePassword failure sets uiError`() = runTest {
        coEvery { api.updatePassword(any()) } throws RuntimeException("Network error")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updatePassword("newpassword123")
        advanceUntilIdle()

        assertNotNull(viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `updatePassword API returns success=false sets uiError`() = runTest {
        coEvery { api.updatePassword(any()) } returns ApiResponse(
            success = false,
            data = null,
            message = "Update failed",
            error = null
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updatePassword("newpassword123")
        advanceUntilIdle()

        assertEquals("Update failed", viewModel.uiError)
        assertFalse(viewModel.uiLoading)
    }

    @Test
    fun `updatePassword sets uiLoading correctly`() = runTest {
        coEvery { api.updatePassword(any()) } returns ApiResponse(
            success = true,
            data = UpdatePasswordResponse("Password updated")
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiLoading)

        viewModel.updatePassword("newpassword123")
        advanceUntilIdle()

        assertFalse(viewModel.uiLoading)
    }
}
