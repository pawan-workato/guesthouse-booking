package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Int = 0
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var failedAttempts = 0
    private var lockoutUntilMs = 0L

    fun login(email: String, password: String) {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remaining = ((lockoutUntilMs - now + 999) / 1000).toInt()
            _uiState.value = LoginUiState(
                isLockedOut = true,
                lockoutSecondsRemaining = remaining,
                errorMessage = "Too many failed attempts. Try again in ${remaining}s."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            val result = authRepository.login(email, password)
            _uiState.value = result.fold(
                onSuccess = {
                    failedAttempts = 0
                    LoginUiState()
                },
                onFailure = { error ->
                    failedAttempts++
                    if (failedAttempts >= MAX_ATTEMPTS) {
                        lockoutUntilMs = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                        failedAttempts = 0
                        LoginUiState(
                            isLockedOut = true,
                            lockoutSecondsRemaining = (LOCKOUT_DURATION_MS / 1000).toInt(),
                            errorMessage = "Too many failed attempts. Try again in ${LOCKOUT_DURATION_MS / 1000}s."
                        )
                    } else {
                        LoginUiState(errorMessage = error.message ?: "Login failed")
                    }
                }
            )
        }
    }

    fun refreshLockoutCountdown() {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remaining = ((lockoutUntilMs - now + 999) / 1000).toInt()
            _uiState.value = LoginUiState(
                isLockedOut = true,
                lockoutSecondsRemaining = remaining,
                errorMessage = "Too many failed attempts. Try again in ${remaining}s."
            )
        } else if (_uiState.value.isLockedOut) {
            _uiState.value = LoginUiState()
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
