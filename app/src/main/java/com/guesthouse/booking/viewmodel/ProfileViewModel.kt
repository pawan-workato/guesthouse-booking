package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.ProfileRepository
import com.guesthouse.booking.data.repository.StaffProfileDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val profile: StateFlow<StaffProfileDetails?> = profileRepository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveDisplayName(displayName: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isSaving = true)
            profileRepository.updateDisplayName(displayName)
                .onSuccess { _uiState.value = ProfileUiState(message = "Profile updated") }
                .onFailure { error ->
                    _uiState.value = ProfileUiState(error = error.message ?: "Could not save profile")
                }
        }
    }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isSaving = true)
            authRepository.sendPasswordResetEmail()
                .onSuccess {
                    _uiState.value = ProfileUiState(
                        message = "If an account exists for your email, a reset link has been sent."
                    )
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState(error = error.message ?: "Could not send reset email")
                }
        }
    }

    fun clearMessage() {
        _uiState.value = ProfileUiState()
    }
}
