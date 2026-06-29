package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayBoardState(
    val todayLabel: String = LocalDate.now().toString(),
    val accessibleProperties: List<PropertyEntity> = emptyList(),
    val selectedPropertyId: Long? = null,
    val arrivals: List<BookingWithDetails> = emptyList(),
    val departures: List<BookingWithDetails> = emptyList(),
    val inHouse: List<BookingWithDetails> = emptyList(),
    val actionMessage: String? = null,
    val actionError: String? = null
)

class TodayViewModel(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val todayEpochDay = LocalDate.now().toEpochDay()
    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    private val _actionMessage = MutableStateFlow<String?>(null)
    private val _actionError = MutableStateFlow<String?>(null)

    private val boardState = combine(
        repository.observeBookings(),
        repository.observeRooms(),
        repository.observeProperties(),
        authRepository.session,
        _selectedPropertyId
    ) { bookings, rooms, properties, session, selectedPropertyId ->
        if (session == null) return@combine TodayBoardState()

        val accessibleProperties = properties.filter { session.canAccessProperty(it.id) }
        val scopedPropertyIds = when {
            selectedPropertyId != null && session.canAccessProperty(selectedPropertyId) -> setOf(selectedPropertyId)
            else -> accessibleProperties.map { it.id }.toSet()
        }
        val roomMap = rooms.associateBy { it.id }
        val propertyMap = properties.associateBy { it.id }

        fun enrich(booking: BookingEntity): BookingWithDetails =
            BookingWithDetails(
                booking = booking,
                propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
            )

        val scoped = bookings.filter {
            it.propertyId in scopedPropertyIds && it.syncStatus != SyncStatus.CONFLICT.name
        }

        TodayBoardState(
            todayLabel = LocalDate.now().toString(),
            accessibleProperties = accessibleProperties,
            selectedPropertyId = selectedPropertyId,
            arrivals = scoped
                .filter { it.status == BookingStatus.CONFIRMED.name && it.checkInEpochDay == todayEpochDay }
                .map(::enrich)
                .sortedBy { it.booking.guestName },
            departures = scoped
                .filter { it.status == BookingStatus.CHECKED_IN.name && it.checkOutEpochDay == todayEpochDay }
                .map(::enrich)
                .sortedBy { it.booking.guestName },
            inHouse = scoped
                .filter {
                    it.status == BookingStatus.CHECKED_IN.name &&
                        it.checkInEpochDay <= todayEpochDay &&
                        it.checkOutEpochDay > todayEpochDay
                }
                .map(::enrich)
                .sortedBy { it.booking.roomId },
        )
    }

    val uiState: StateFlow<TodayBoardState> = combine(boardState, _actionMessage, _actionError) { board, msg, err ->
        board.copy(actionMessage = msg, actionError = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayBoardState())

    fun selectProperty(propertyId: Long?) {
        val session = authRepository.currentSession() ?: return
        if (propertyId != null && !session.canAccessProperty(propertyId)) return
        _selectedPropertyId.value = propertyId
    }

    fun checkIn(bookingId: Long) {
        viewModelScope.launch {
            repository.checkInBooking(bookingId, todayEpochDay)
                .onSuccess { _actionMessage.value = "Guest checked in"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-in failed"; _actionMessage.value = null }
        }
    }

    fun checkOut(bookingId: Long) {
        viewModelScope.launch {
            repository.checkOutBooking(bookingId, todayEpochDay)
                .onSuccess { _actionMessage.value = "Guest checked out"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-out failed"; _actionMessage.value = null }
        }
    }

    fun dismissActionFeedback() {
        _actionMessage.value = null
        _actionError.value = null
    }
}
