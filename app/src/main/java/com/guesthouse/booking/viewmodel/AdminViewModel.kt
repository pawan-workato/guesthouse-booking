package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val bookingsWithDetails: StateFlow<List<BookingWithDetails>> = combine(
        repository.observeBookings(),
        repository.observeRooms(),
        repository.observeProperties(),
        authRepository.session
    ) { bookings, rooms, properties, session ->
        if (session == null) return@combine emptyList()
        val roomMap = rooms.associateBy { it.id }
        val propertyMap = properties.associateBy { it.id }
        bookings
            .filter { session.canAccessProperty(it.propertyId) }
            .map { booking ->
                BookingWithDetails(
                    booking = booking,
                    propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                    roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            val session = authRepository.currentSession() ?: return@launch
            val booking = repository.getBookingById(bookingId) ?: return@launch
            if (!session.canAccessProperty(booking.propertyId)) return@launch
            repository.cancelBooking(bookingId)
        }
    }

    fun checkIn(bookingId: Long) {
        viewModelScope.launch { repository.checkInBooking(bookingId) }
    }

    fun checkOut(bookingId: Long) {
        viewModelScope.launch { repository.checkOutBooking(bookingId) }
    }
}
