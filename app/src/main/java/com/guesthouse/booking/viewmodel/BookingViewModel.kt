package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookingUiState(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class BookingViewModel(private val repository: BookingRepository) : ViewModel() {
    val rooms: StateFlow<List<RoomEntity>> = repository.observeRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun observeRoomBookings(roomId: Long): StateFlow<List<BookingEntity>> =
        repository.observeConfirmedBookingsForRoom(roomId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun submitBooking(
        roomId: Long,
        guestName: String,
        guestEmail: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long
    ) {
        viewModelScope.launch {
            _uiState.value = BookingUiState(isSubmitting = true)
            val result = repository.createBooking(
                roomId, guestName, guestEmail, checkInEpochDay, checkOutEpochDay
            )
            _uiState.value = result.fold(
                onSuccess = { BookingUiState(successMessage = "Booking confirmed!") },
                onFailure = { BookingUiState(errorMessage = it.message ?: "Booking failed") }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = BookingUiState()
    }
}
