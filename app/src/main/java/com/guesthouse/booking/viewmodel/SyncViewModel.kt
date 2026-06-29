package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
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

data class BlockDateWithDetails(
    val block: BlockDateEntity,
    val propertyName: String,
    val roomName: String
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
        syncRepository.observePendingBlocks(),
        syncRepository.observeConflicts(),
        syncRepository.observeBlockConflicts(),
        authRepository.session
    ) { pendingBookings, pendingBlocks, bookingConflicts, blockConflicts, session ->
        if (session == null) 0
        else pendingBookings.count { session.canAccessProperty(it.propertyId) } +
            pendingBlocks.count { session.canAccessProperty(it.propertyId) } +
            bookingConflicts.count { session.canAccessProperty(it.propertyId) } +
            blockConflicts.count { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, ViewModelSharing, 0)

    val pending: StateFlow<List<BookingEntity>> = combine(
        syncRepository.observePending(),
        authRepository.session
    ) { bookings, session ->
        if (session == null) emptyList()
        else bookings.filter { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val conflicts: StateFlow<List<BookingEntity>> = combine(
        syncRepository.observeConflicts(),
        authRepository.session
    ) { bookings, session ->
        if (session == null) emptyList()
        else bookings.filter { session.canAccessProperty(it.propertyId) }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val pendingBookingsWithDetails: StateFlow<List<BookingWithDetails>> = combine(
        pending,
        bookingRepository.observeProperties(),
        bookingRepository.observeRooms()
    ) { bookings, properties, rooms ->
        val propertyMap = properties.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        bookings.map { booking ->
            BookingWithDetails(
                booking = booking,
                propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
            )
        }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val conflictBookingsWithDetails: StateFlow<List<BookingWithDetails>> = combine(
        conflicts,
        bookingRepository.observeProperties(),
        bookingRepository.observeRooms()
    ) { bookings, properties, rooms ->
        val propertyMap = properties.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        bookings.map { booking ->
            BookingWithDetails(
                booking = booking,
                propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
            )
        }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val pendingBlocksWithDetails: StateFlow<List<BlockDateWithDetails>> = combine(
        syncRepository.observePendingBlocks(),
        bookingRepository.observeProperties(),
        bookingRepository.observeRooms(),
        authRepository.session
    ) { blocks, properties, rooms, session ->
        if (session == null) return@combine emptyList()
        val propertyMap = properties.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        blocks
            .filter { session.canAccessProperty(it.propertyId) }
            .map { block ->
                BlockDateWithDetails(
                    block = block,
                    propertyName = propertyMap[block.propertyId]?.name ?: "Unknown property",
                    roomName = roomMap[block.roomId]?.name ?: "Unknown room"
                )
            }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    val blockConflictsWithDetails: StateFlow<List<BlockDateWithDetails>> = combine(
        syncRepository.observeBlockConflicts(),
        bookingRepository.observeProperties(),
        bookingRepository.observeRooms(),
        authRepository.session
    ) { blocks, properties, rooms, session ->
        if (session == null) return@combine emptyList()
        val propertyMap = properties.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        blocks
            .filter { session.canAccessProperty(it.propertyId) }
            .map { block ->
                BlockDateWithDetails(
                    block = block,
                    propertyName = propertyMap[block.propertyId]?.name ?: "Unknown property",
                    roomName = roomMap[block.roomId]?.name ?: "Unknown room"
                )
            }
    }.stateIn(viewModelScope, ViewModelSharing, emptyList())

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = SyncUiState(isSyncing = true)
            val result = syncRepository.syncNow()
            _uiState.value = when {
                result.noNetwork -> SyncUiState(error = "No network connection")
                result.notAuthenticated -> SyncUiState(error = "Sign in to sync with Firebase")
                result.pullErrors.isNotEmpty() && result.propertiesPulled == 0 && result.guestsPulled == 0 ->
                    SyncUiState(error = "Download failed: ${result.pullErrors.first()}")
                result.propertiesPulled > 0 || result.guestsPulled > 0 -> {
                    val pullSummary = buildList {
                        if (result.propertiesPulled > 0) add("${result.propertiesPulled} properties")
                        if (result.guestsPulled > 0) add("${result.guestsPulled} guests")
                    }.joinToString(", ")
                    val uploadSummary = when {
                        result.syncedCount > 0 || result.conflictCount > 0 ->
                            "; uploaded ${result.syncedCount}, conflicts ${result.conflictCount}"
                        else -> ""
                    }
                    SyncUiState(message = "Downloaded $pullSummary$uploadSummary")
                }
                result.syncedCount > 0 || result.conflictCount > 0 -> SyncUiState(
                    message = "Synced ${result.syncedCount}, conflicts ${result.conflictCount}"
                )
                else -> SyncUiState(message = "Up to date")
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
