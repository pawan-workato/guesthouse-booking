package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SyncViewModel(
    private val syncRepository: SyncRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = syncRepository.isOnline
    val lastSyncEpochMs: StateFlow<Long> = syncRepository.lastSyncEpochMs
    val issueCount: StateFlow<Int> = combine(
        syncRepository.observePending(),
        syncRepository.observeConflicts(),
        authRepository.session
    ) { pending, conflicts, session ->
        if (session == null) 0
        else pending.count { session.canAccessProperty(it.propertyId) } +
            conflicts.count { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pending: StateFlow<List<BookingEntity>> = combine(
        syncRepository.observePending(),
        authRepository.session
    ) { bookings, session ->
        if (session == null) emptyList()
        else bookings.filter { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conflicts: StateFlow<List<BookingEntity>> = combine(
        syncRepository.observeConflicts(),
        authRepository.session
    ) { bookings, session ->
        if (session == null) emptyList()
        else bookings.filter { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = SyncUiState(isSyncing = true)
            val result = syncRepository.syncNow()
            _uiState.value = when {
                result.noNetwork -> SyncUiState(error = "No network connection")
                result.notAuthenticated -> SyncUiState(error = "Sign in to sync with Firebase")
                result.syncedCount == 0 && result.conflictCount == 0 ->
                    SyncUiState(message = "Nothing to sync")
                else -> SyncUiState(
                    message = "Synced ${result.syncedCount}, conflicts ${result.conflictCount}"
                )
            }
        }
    }

    fun dismissConflict(bookingId: Long) {
        viewModelScope.launch {
            val session = authRepository.currentSession() ?: return@launch
            val booking = bookingRepository.getBookingById(bookingId) ?: return@launch
            if (!session.canAccessProperty(booking.propertyId)) return@launch
            syncRepository.dismissConflict(bookingId)
        }
    }

    fun clearMessage() {
        _uiState.value = SyncUiState()
    }
}
