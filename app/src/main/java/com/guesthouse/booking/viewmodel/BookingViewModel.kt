package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockUiState(val isSubmitting: Boolean = false, val successMessage: String? = null, val errorMessage: String? = null)

data class BookingUiState(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BookingViewModel(
    private val repository: BookingRepository,
    private val blockDateRepository: BlockDateRepository,
    private val guestRepository: GuestRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val properties: StateFlow<List<PropertyEntity>> = combine(
        repository.observeProperties(),
        authRepository.session
    ) { properties, session ->
        if (session == null) emptyList()
        else properties.filter { session.canAccessProperty(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeGuests: StateFlow<List<GuestEntity>> = guestRepository.observeActiveGuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    val selectedPropertyId: StateFlow<Long?> = _selectedPropertyId.asStateFlow()

    val roomsForSelectedProperty: StateFlow<List<RoomEntity>> = _selectedPropertyId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeRoomsForProperty(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    private val _blockUiState = MutableStateFlow(BlockUiState())
    val blockUiState: StateFlow<BlockUiState> = _blockUiState.asStateFlow()
    private val _preselectedRoomId = MutableStateFlow<Long?>(null)

    fun selectProperty(propertyId: Long) { _selectedPropertyId.value = propertyId }
    fun preselect(propertyId: Long, roomId: Long) {
        _selectedPropertyId.value = propertyId
        _preselectedRoomId.value = roomId
    }
    fun consumePreselectedRoom(): Long? {
        val id = _preselectedRoomId.value
        _preselectedRoomId.value = null
        return id
    }

    fun room(roomId: Long): StateFlow<RoomEntity?> =
        repository.observeRoom(roomId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun observeRoomBookings(roomId: Long): StateFlow<List<BookingEntity>> =
        repository.observeActiveBookingsForRoom(roomId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun observeRoomBlocks(roomId: Long): StateFlow<List<BlockDateEntity>> =
        blockDateRepository.observeForRoom(roomId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createBlock(roomId: Long, startEpochDay: Long, endEpochDay: Long, reason: String) {
        viewModelScope.launch {
            _blockUiState.value = BlockUiState(isSubmitting = true)
            val room = repository.getRoomById(roomId) ?: run { _blockUiState.value = BlockUiState(errorMessage = "Room not found"); return@launch }
            if (!canAccessProperty(room.propertyId)) { _blockUiState.value = BlockUiState(errorMessage = "You don't have access to this property"); return@launch }
            _blockUiState.value = blockDateRepository.createBlock(roomId, startEpochDay, endEpochDay, reason).fold(
                onSuccess = { BlockUiState(successMessage = "Dates blocked") },
                onFailure = { BlockUiState(errorMessage = it.message ?: "Failed to block dates") }
            )
        }
    }

    fun removeBlock(blockId: Long) {
        viewModelScope.launch {
            _blockUiState.value = blockDateRepository.removeBlock(blockId).fold(
                onSuccess = { BlockUiState(successMessage = "Block removed") },
                onFailure = { BlockUiState(errorMessage = it.message ?: "Failed to remove block") }
            )
        }
    }

    fun clearBlockMessages() { _blockUiState.value = BlockUiState() }

    fun canAccessProperty(propertyId: Long): Boolean =
        authRepository.currentSession()?.canAccessProperty(propertyId) == true

    fun submitBooking(
        roomId: Long,
        guestId: Long?,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long
    ) {
        viewModelScope.launch {
            _uiState.value = BookingUiState(isSubmitting = true)
            val room = repository.getRoomById(roomId)
            if (room == null) {
                _uiState.value = BookingUiState(errorMessage = "Room not found")
                return@launch
            }
            if (!canAccessProperty(room.propertyId)) {
                _uiState.value = BookingUiState(errorMessage = "You don't have access to this property")
                return@launch
            }
            val online = networkMonitor.isCurrentlyOnline()
            val result = repository.createBooking(
                roomId, guestId, guestName, guestEmail, guestPhone, checkInEpochDay, checkOutEpochDay, online
            )
            _uiState.value = result.fold(
                onSuccess = { outcome ->
                    if (outcome.savedOffline) {
                        syncRepository.enqueueSyncWorker()
                        BookingUiState(
                            successMessage = "Saved offline — will sync when online. Ref: ${outcome.reference}"
                        )
                    } else {
                        BookingUiState(
                            successMessage = "Booking confirmed! Ref: ${outcome.reference}"
                        )
                    }
                },
                onFailure = { BookingUiState(errorMessage = it.message ?: "Booking failed") }
            )
        }
    }

    fun clearMessages() { _uiState.value = BookingUiState() }
}
