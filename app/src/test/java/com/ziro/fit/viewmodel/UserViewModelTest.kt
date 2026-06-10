package com.ziro.fit.viewmodel

import android.util.Log
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.data.repository.TrainerRepository
import com.ziro.fit.model.ApiResponse
import com.ziro.fit.model.LinkedTrainer
import com.ziro.fit.model.LinkActionResponse
import com.ziro.fit.model.LinkedTrainerProfile
import com.ziro.fit.model.LinkedTrainerResponse
import com.ziro.fit.model.User
import com.ziro.fit.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: ZiroApi = mockk()
    private val trainerRepository: TrainerRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()

    private val testUser = User(
        id = "user1",
        email = "test@ziro.fit",
        name = "Test User",
        role = "client",
        username = "testuser",
        hasCompletedOnboarding = true,
        subscriptionStatus = "active",
        profilePhotoPath = null,
        isFreeAccessModeEnabled = false,
        tier = "free"
    )

    private val testLinkedTrainer = LinkedTrainer(
        id = "trainer1",
        name = "John Trainer",
        email = "john@ziro.fit",
        profile = LinkedTrainerProfile(
            profilePhotoPath = "https://example.com/photo.jpg",
            aboutMe = "Fitness expert"
        )
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Default stubs for init block calls
        coEvery { api.getMe() } returns ApiResponse(
            data = testUser,
            success = true
        )
        coEvery { trainerRepository.getLinkedTrainer() } returns Result.success(testLinkedTrainer)
    }

    @Test
    fun `init fetches user profile and linked trainer`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        assertEquals(testUser, viewModel.user)
        assertEquals(testLinkedTrainer, viewModel.linkedTrainer)
        assertFalse(viewModel.isLoading)
        assertFalse(viewModel.linkedTrainerLoading)
    }

    @Test
    fun `init handles null linked trainer gracefully`() = runTest {
        coEvery { trainerRepository.getLinkedTrainer() } returns Result.success(null)

        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        assertEquals(testUser, viewModel.user)
        assertNull(viewModel.linkedTrainer)
    }

    @Test
    fun `init handles API failure for linked trainer gracefully`() = runTest {
        coEvery { trainerRepository.getLinkedTrainer() } returns Result.failure(Exception("Failed"))

        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        assertNull(viewModel.linkedTrainer)
    }

    @Test
    fun `fetchLinkedTrainer updates linkedTrainer state on success`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        val newTrainer = testLinkedTrainer.copy(name = "New Trainer")
        coEvery { trainerRepository.getLinkedTrainer() } returns Result.success(newTrainer)

        viewModel.fetchLinkedTrainer()
        advanceUntilIdle()

        assertEquals(newTrainer, viewModel.linkedTrainer)
    }

    @Test
    fun `fetchLinkedTrainer sets null on API failure`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { trainerRepository.getLinkedTrainer() } returns Result.failure(Exception("Network error"))

        viewModel.fetchLinkedTrainer()
        advanceUntilIdle()

        assertNull(viewModel.linkedTrainer)
    }

    @Test
    fun `unlinkTrainer success clears linked trainer and invokes callback`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { trainerRepository.unlinkTrainer() } returns Result.success("Unlinked successfully")

        var callbackCalled = false
        viewModel.unlinkTrainer(onSuccess = { callbackCalled = true })
        advanceUntilIdle()

        assertTrue(callbackCalled)
        assertNull(viewModel.unlinkTrainerError)
    }

    @Test
    fun `unlinkTrainer failure sets error state`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { trainerRepository.unlinkTrainer() } returns Result.failure(Exception("Unlink failed"))

        viewModel.unlinkTrainer()
        advanceUntilIdle()

        assertEquals("Unlink failed", viewModel.unlinkTrainerError)
    }

    @Test
    fun `deleteAccount success sets accountDeleted and invokes callback`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { profileRepository.deleteAccount() } returns Result.success(Unit)

        var callbackCalled = false
        viewModel.deleteAccount(onSuccess = { callbackCalled = true })
        advanceUntilIdle()

        assertTrue(viewModel.accountDeleted)
        assertTrue(callbackCalled)
        assertNull(viewModel.deleteAccountError)
    }

    @Test
    fun `deleteAccount failure sets error state`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { profileRepository.deleteAccount() } returns Result.failure(Exception("Delete failed"))

        viewModel.deleteAccount()
        advanceUntilIdle()

        assertEquals("Delete failed", viewModel.deleteAccountError)
        assertFalse(viewModel.accountDeleted)
    }

    @Test
    fun `clearDeleteAccountError resets error`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { profileRepository.deleteAccount() } returns Result.failure(Exception("Error"))
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertNotNull(viewModel.deleteAccountError)

        viewModel.clearDeleteAccountError()
        assertNull(viewModel.deleteAccountError)
    }

    @Test
    fun `clearUnlinkError resets error`() = runTest {
        val viewModel = UserViewModel(api, trainerRepository, profileRepository)
        advanceUntilIdle()

        coEvery { trainerRepository.unlinkTrainer() } returns Result.failure(Exception("Error"))
        viewModel.unlinkTrainer()
        advanceUntilIdle()
        assertNotNull(viewModel.unlinkTrainerError)

        viewModel.clearUnlinkError()
        assertNull(viewModel.unlinkTrainerError)
    }
}
