package com.ziro.fit.data.repository

import android.util.Log
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.model.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileRepositoryTest {
    private val api: ZiroApi = mockk()
    private val repository = ProfileRepository(api)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @Test
    fun `deleteAccount success returns Unit`() = runBlocking {
        val response = ApiResponse<Any>(
            data = Unit,
            success = true
        )
        coEvery { api.deleteAccount() } returns response

        val result = repository.deleteAccount()

        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        coVerify { api.deleteAccount() }
    }

    @Test
    fun `deleteAccount API failure throws exception returns Result failure`() = runBlocking {
        coEvery { api.deleteAccount() } throws RuntimeException("Network Error")

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
        assertEquals("Network Error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteAccount API returns success false returns Result failure with message`() = runBlocking {
        val response = ApiResponse<Any>(
            data = null,
            success = false,
            message = "Failed to delete account"
        )
        coEvery { api.deleteAccount() } returns response

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
        assertEquals("Failed to delete account", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteAccount API returns success false with null message returns default error`() = runBlocking {
        val response = ApiResponse<Any>(
            data = null,
            success = false,
            message = null
        )
        coEvery { api.deleteAccount() } returns response

        val result = repository.deleteAccount()

        assertTrue(result.isFailure)
        assertEquals("Failed to delete account", result.exceptionOrNull()?.message)
    }
}
