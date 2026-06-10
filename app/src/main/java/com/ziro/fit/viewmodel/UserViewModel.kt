package com.ziro.fit.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.data.repository.ProfileRepository
import com.ziro.fit.data.repository.TrainerRepository
import com.ziro.fit.model.LinkedTrainer
import com.ziro.fit.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val api: ZiroApi,
    private val trainerRepository: TrainerRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var user by mutableStateOf<User?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    var linkedTrainer by mutableStateOf<LinkedTrainer?>(null)
        private set
    var linkedTrainerLoading by mutableStateOf(false)
        private set

    var deleteAccountLoading by mutableStateOf(false)
        private set
    var deleteAccountError by mutableStateOf<String?>(null)
        private set
    var accountDeleted by mutableStateOf(false)
        private set

    var unlinkTrainerLoading by mutableStateOf(false)
        private set
    var unlinkTrainerError by mutableStateOf<String?>(null)
        private set

    init {
        fetchUserProfile()
        fetchLinkedTrainer()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = api.getMe()
                user = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchLinkedTrainer() {
        viewModelScope.launch {
            linkedTrainerLoading = true
            trainerRepository.getLinkedTrainer()
                .onSuccess { linkedTrainer = it }
                .onFailure { linkedTrainer = null }
            linkedTrainerLoading = false
        }
    }

    fun unlinkTrainer(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            unlinkTrainerLoading = true
            unlinkTrainerError = null
            trainerRepository.unlinkTrainer()
                .onSuccess {
                    linkedTrainer = null
                    fetchLinkedTrainer()
                    onSuccess()
                }
                .onFailure { e ->
                    unlinkTrainerError = e.message
                }
            unlinkTrainerLoading = false
        }
    }

    fun deleteAccount(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            deleteAccountLoading = true
            deleteAccountError = null
            profileRepository.deleteAccount()
                .onSuccess {
                    accountDeleted = true
                    onSuccess()
                }
                .onFailure { e ->
                    deleteAccountError = e.message
                }
            deleteAccountLoading = false
        }
    }

    fun clearDeleteAccountError() {
        deleteAccountError = null
    }

    fun clearUnlinkError() {
        unlinkTrainerError = null
    }
}
