package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookingDetailState(
    val booking: BookingEntity? = null,
    val propertyName: String = "",
    val roomName: String = "",
    val isLoading: Boolean = true,
    val accessDenied: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
    val extendMessage: String? = null,
    val extendError: String? = null,
    val isExtending: Boolean = false
) {
    val canEdit: Boolean
        get() = booking?.status == BookingStatus.CONFIRMED.name

    val canCancel: Boolean
        get() = booking?.status == BookingStatus.CONFIRMED.name

    val canCheckIn: Boolean
        get() = booking?.status == BookingStatus.CONFIRMED.name

    val canCheckOut: Boolean
        get() = booking?.status == BookingStatus.CHECKED_IN.name

    val canExtend: Boolean
        get() = booking?.status == BookingStatus.CONFIRMED.name ||
            booking?.status == BookingStatus.CHECKED_IN.name

    val hasConflict: Boolean
        get() = booking?.syncStatus == com.guesthouse.booking.data.local.entities.SyncStatus.CONFLICT.name
}

private data class FeedbackState(
    val actionMessage: String? = null,
    val actionError: String? = null,
    val extendMessage: String? = null,
    val extendError: String? = null,
    val isExtending: Boolean = false
)

class BookingDetailViewModel(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _bookingId = MutableStateFlow<Long?>(null)
    private val _actionMessage = MutableStateFlow<String?>(null)
    private val _actionError = MutableStateFlow<String?>(null)
    private val _extendMessage = MutableStateFlow<String?>(null)
    private val _extendError = MutableStateFlow<String?>(null)
    private val _isExtending = MutableStateFlow(false)

    private val coreState = combine(
        _bookingId,
        repository.observeBookings(),
        repository.observeProperties(),
        repository.observeRooms(),
        authRepository.session
    ) { bookingId, bookings, properties, rooms, session ->
        if (bookingId == null || session == null) {
            return@combine BookingDetailState(isLoading = bookingId != null)
        }
        val booking = bookings.find { it.id == bookingId }
            ?: return@combine BookingDetailState(isLoading = false, accessDenied = true)
        if (!session.canAccessProperty(booking.propertyId)) {
            return@combine BookingDetailState(isLoading = false, accessDenied = true)
        }
        val propertyName = properties.find { it.id == booking.propertyId }?.name ?: "Unknown property"
        val roomName = rooms.find { it.id == booking.roomId }?.name ?: "Unknown room"
        BookingDetailState(
            booking = booking,
            propertyName = propertyName,
            roomName = roomName,
            isLoading = false,
            accessDenied = false
        )
    }

    private val feedbackState = combine(
        _actionMessage,
        _actionError,
        _extendMessage,
        _extendError,
        _isExtending
    ) { msg, err, extendMsg, extendErr, extending ->
        FeedbackState(msg, err, extendMsg, extendErr, extending)
    }

    val uiState: StateFlow<BookingDetailState> = combine(coreState, feedbackState) { core, feedback ->
        core.copy(
            actionMessage = feedback.actionMessage,
            actionError = feedback.actionError,
            extendMessage = feedback.extendMessage,
            extendError = feedback.extendError,
            isExtending = feedback.isExtending
        )
    }.stateIn(viewModelScope, ViewModelSharing, BookingDetailState())

    fun loadBooking(bookingId: Long) {
        _bookingId.value = bookingId
    }

    fun cancelBooking() {
        val bookingId = _bookingId.value ?: return
        viewModelScope.launch {
            val session = authRepository.currentSession()
                ?: run { _actionError.value = "Not signed in"; return@launch }
            val booking = repository.getBookingById(bookingId)
                ?: run { _actionError.value = "Booking not found"; return@launch }
            if (!session.canAccessProperty(booking.propertyId)) {
                _actionError.value = "You don't have access to this property"
                return@launch
            }
            repository.cancelBooking(bookingId)
            _actionMessage.value = "Booking cancelled"
            _actionError.value = null
        }
    }

    fun checkIn() {
        val bookingId = _bookingId.value ?: return
        viewModelScope.launch {
            repository.checkInBooking(bookingId)
                .onSuccess { _actionMessage.value = "Guest checked in"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-in failed"; _actionMessage.value = null }
        }
    }

    fun checkOut() {
        val bookingId = _bookingId.value ?: return
        viewModelScope.launch {
            repository.checkOutBooking(bookingId)
                .onSuccess { _actionMessage.value = "Guest checked out"; _actionError.value = null }
                .onFailure { _actionError.value = it.message ?: "Check-out failed"; _actionMessage.value = null }
        }
    }

    fun extendCheckout(additionalNights: Int) {
        val bookingId = _bookingId.value ?: return
        val booking = uiState.value.booking ?: return
        val newCheckOut = booking.checkOutEpochDay + additionalNights
        extendCheckoutTo(newCheckOut)
    }

    fun extendCheckoutTo(newCheckOutEpochDay: Long) {
        val bookingId = _bookingId.value ?: return
        viewModelScope.launch {
            _isExtending.value = true
            repository.extendCheckout(bookingId, newCheckOutEpochDay, networkMonitor.isCurrentlyOnline())
                .onSuccess {
                    _extendMessage.value = "Stay extended"
                    _extendError.value = null
                }
                .onFailure {
                    _extendError.value = it.message ?: "Could not extend stay"
                    _extendMessage.value = null
                }
            _isExtending.value = false
        }
    }

    fun dismissActionFeedback() {
        _actionMessage.value = null
        _actionError.value = null
        _extendMessage.value = null
        _extendError.value = null
    }
}
