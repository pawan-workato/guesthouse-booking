package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookingWithRoom(
    val booking: BookingEntity,
    val roomName: String
)

class AdminViewModel(private val repository: BookingRepository) : ViewModel() {
    val bookingsWithRooms: StateFlow<List<BookingWithRoom>> = combine(
        repository.observeBookings(),
        repository.observeRooms()
    ) { bookings, rooms ->
        val roomMap = rooms.associateBy { it.id }
        bookings.map { booking ->
            BookingWithRoom(
                booking = booking,
                roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId)
        }
    }
}
