package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

data class BlockUiState(val isSubmitting: Boolean = false, val successMessage: String? = null, val errorMessage: String? = null)

data class BookingUiState(
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, FlowPreview::class)
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
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    private val _propertySearchQuery = MutableStateFlow("")
    val propertySearchQuery: StateFlow<String> = _propertySearchQuery.asStateFlow()

    private val _roomSearchQuery = MutableStateFlow("")
    val roomSearchQuery: StateFlow<String> = _roomSearchQuery.asStateFlow()

    private val _roomTypeFilter = MutableStateFlow<RoomType?>(null)
    val roomTypeFilter: StateFlow<RoomType?> = _roomTypeFilter.asStateFlow()

    val filteredProperties: StateFlow<List<PropertyEntity>> = combine(
        properties,
        _propertySearchQuery
    ) { props, query ->
        BookingSearchFilters.filterProperties(props, query)
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val activeGuests: StateFlow<List<GuestEntity>> = guestRepository.observeActiveGuests()
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    val selectedPropertyId: StateFlow<Long?> = _selectedPropertyId.asStateFlow()

    val roomsForSelectedProperty: StateFlow<List<RoomEntity>> = _selectedPropertyId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeRoomsForProperty(id)
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    val filteredRooms: StateFlow<List<RoomEntity>> = combine(
        roomsForSelectedProperty,
        _roomSearchQuery,
        _roomTypeFilter
    ) { rooms, query, typeFilter ->
        BookingSearchFilters.filterRooms(rooms, query, typeFilter)
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    private val _editBooking = MutableStateFlow<BookingEntity?>(null)
    val editBooking: StateFlow<BookingEntity?> = _editBooking.asStateFlow()
    private val _blockUiState = MutableStateFlow(BlockUiState())
    val blockUiState: StateFlow<BlockUiState> = _blockUiState.asStateFlow()
    private val _preselectedRoomId = MutableStateFlow<Long?>(null)
    private val _preselectedGuestId = MutableStateFlow<Long?>(null)
    private val _guestLookup = MutableStateFlow(Triple("", "", ""))

    val similarGuests: StateFlow<List<GuestEntity>> = _guestLookup
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { (name, email, phone) ->
            flow {
                emit(guestRepository.findSimilarGuests(name, email, phone))
            }
        }
        .stateIn(viewModelScope, ViewModelSharing, emptyList())

    fun selectProperty(propertyId: Long) {
        _selectedPropertyId.value = propertyId
        _roomSearchQuery.value = ""
        _roomTypeFilter.value = null
    }

    fun setPropertySearchQuery(query: String) {
        _propertySearchQuery.value = query
    }

    fun setRoomSearchQuery(query: String) {
        _roomSearchQuery.value = query
    }

    fun toggleRoomTypeFilter(type: RoomType) {
        _roomTypeFilter.value = if (_roomTypeFilter.value == type) null else type
    }

    fun clearRoomFilters() {
        _roomSearchQuery.value = ""
        _roomTypeFilter.value = null
    }
    fun preselect(propertyId: Long, roomId: Long) {
        _selectedPropertyId.value = propertyId
        _preselectedRoomId.value = roomId
    }
    fun preselectGuest(guestId: Long) {
        _preselectedGuestId.value = guestId
    }

    fun consumePreselectedGuest(): Long? {
        val id = _preselectedGuestId.value
        _preselectedGuestId.value = null
        return id
    }

    fun updateGuestLookup(name: String, email: String, phone: String, manualEntry: Boolean) {
        _guestLookup.value = if (manualEntry) Triple(name, email, phone) else Triple("", "", "")
    }

    fun clearGuestLookup() {
        _guestLookup.value = Triple("", "", "")
    }

    fun consumePreselectedRoom(): Long? {
        val id = _preselectedRoomId.value
        _preselectedRoomId.value = null
        return id
    }

    private val roomFlows = mutableMapOf<Long, StateFlow<RoomEntity?>>()
    private val roomBookingFlows = mutableMapOf<Long, StateFlow<List<BookingEntity>>>()
    private val roomBlockFlows = mutableMapOf<Long, StateFlow<List<BlockDateEntity>>>()

    fun room(roomId: Long): StateFlow<RoomEntity?> =
        cachedStateFlow(roomFlows, roomId, null) { repository.observeRoom(roomId) }

    fun observeRoomBookings(roomId: Long): StateFlow<List<BookingEntity>> =
        cachedStateFlow(roomBookingFlows, roomId, emptyList()) {
            repository.observeActiveBookingsForRoom(roomId)
        }

    fun observeRoomBlocks(roomId: Long): StateFlow<List<BlockDateEntity>> =
        cachedStateFlow(roomBlockFlows, roomId, emptyList()) {
            blockDateRepository.observeForRoom(roomId)
        }

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

    fun updateRoomHousekeeping(roomId: Long, status: String) {
        viewModelScope.launch {
            repository.updateRoomHousekeeping(roomId, status)
                .onFailure { _blockUiState.value = BlockUiState(errorMessage = it.message) }
        }
    }

    fun canAccessProperty(propertyId: Long): Boolean =
        authRepository.currentSession()?.canAccessProperty(propertyId) == true

    fun submitBooking(
        roomId: Long,
        guestId: Long?,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long,
        source: String = com.guesthouse.booking.data.local.entities.BookingSource.WALK_IN.name,
        maintenanceNotes: String = ""
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
                roomId, guestId, guestName, guestEmail, guestPhone, checkInEpochDay, checkOutEpochDay, online, source, maintenanceNotes
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

    fun loadBookingForEdit(bookingId: Long) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId)
            if (booking != null && canAccessProperty(booking.propertyId)) {
                _editBooking.value = booking
                _selectedPropertyId.value = booking.propertyId
            } else {
                _editBooking.value = null
                _uiState.value = BookingUiState(errorMessage = "Booking not found or access denied")
            }
        }
    }

    fun clearEditBooking() { _editBooking.value = null }

    fun updateBooking(
        bookingId: Long,
        roomId: Long,
        guestId: Long?,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long,
        source: String? = null,
        maintenanceNotes: String? = null
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
            val result = repository.updateBooking(
                bookingId, roomId, guestId, guestName, guestEmail, guestPhone,
                checkInEpochDay, checkOutEpochDay, online, source, maintenanceNotes
            )
            _uiState.value = result.fold(
                onSuccess = {
                    if (online) {
                        BookingUiState(successMessage = "Booking updated")
                    } else {
                        syncRepository.enqueueSyncWorker()
                        BookingUiState(successMessage = "Saved offline — will sync when online")
                    }
                },
                onFailure = { BookingUiState(errorMessage = it.message ?: "Update failed") }
            )
        }
    }

    fun clearMessages() { _uiState.value = BookingUiState() }
}
