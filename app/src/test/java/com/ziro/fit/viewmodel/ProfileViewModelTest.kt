package com.ziro.fit.viewmodel

import android.util.Log
import com.ziro.fit.data.repository.ProfileRepository
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
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProfileRepository = mockk()
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @Test
    fun `deleteAccount success sets accountDeleted and calls onSuccess`() = runTest {
        coEvery { repository.deleteAccount() } returns Result.success(Unit)

        viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        var callbackCalled = false
        viewModel.deleteAccount(onSuccess = { callbackCalled = true })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.accountDeleted)
        assertFalse(state.deleteAccountLoading)
        assertNull(state.deleteAccountError)
        assertTrue(callbackCalled)
    }

    @Test
    fun `deleteAccount failure sets error state`() = runTest {
        coEvery { repository.deleteAccount() } returns Result.failure(Exception("Delete failed"))

        viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Delete failed", state.deleteAccountError)
        assertFalse(state.deleteAccountLoading)
        assertFalse(state.accountDeleted)
    }

    @Test
    fun `clearDeleteAccountError resets error state`() = runTest {
        coEvery { repository.deleteAccount() } returns Result.failure(Exception("Error"))

        viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteAccount()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.deleteAccountError)

        viewModel.clearDeleteAccountError()
        assertNull(viewModel.uiState.value.deleteAccountError)
    }
}
