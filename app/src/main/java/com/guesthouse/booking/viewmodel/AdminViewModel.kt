package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookingWithDetails(
    val booking: BookingEntity,
    val propertyName: String,
    val roomName: String
)

class AdminViewModel(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _showCancelled = MutableStateFlow(false)
    val showCancelled: StateFlow<Boolean> = _showCancelled.asStateFlow()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    val bookingsWithDetails: StateFlow<List<BookingWithDetails>> = combine(
        repository.observeBookings(),
        repository.observeRooms(),
        repository.observeProperties(),
        authRepository.session,
        _showCancelled
    ) { bookings, rooms, properties, session, showCancelled ->
        if (session == null) return@combine emptyList()
        val roomMap = rooms.associateBy { it.id }
        val propertyMap = properties.associateBy { it.id }
        bookings
            .filter { session.canAccessProperty(it.propertyId) }
            .filter { showCancelled || it.status != BookingStatus.CANCELLED.name }
            .map { booking ->
                BookingWithDetails(
                    booking = booking,
                    propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                    roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
                )
            }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    fun setShowCancelled(show: Boolean) {
        _showCancelled.value = show
    }

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            val session = authRepository.currentSession()
                ?: run { _actionError.value = "Not signed in"; _actionMessage.value = null; return@launch }
            val booking = repository.getBookingById(bookingId)
                ?: run { _actionError.value = "Booking not found"; _actionMessage.value = null; return@launch }
            if (!session.canAccessProperty(booking.propertyId)) {
                _actionError.value = "You don't have access to this property"
                _actionMessage.value = null
                return@launch
            }
            repository.cancelBooking(bookingId)
            _actionMessage.value = "Booking cancelled"
            _actionError.value = null
        }
    }

    fun checkIn(bookingId: Long) {
        viewModelScope.launch {
            repository.checkInBooking(bookingId)
                .onSuccess { _actionMessage.value = "Guest checked in"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-in failed"; _actionMessage.value = null }
        }
    }

    fun checkOut(bookingId: Long) {
        viewModelScope.launch {
            repository.checkOutBooking(bookingId)
                .onSuccess { _actionMessage.value = "Guest checked out"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-out failed"; _actionMessage.value = null }
        }
    }

    fun dismissActionFeedback() {
        _actionMessage.value = null
        _actionError.value = null
    }
}
