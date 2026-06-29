package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoomFormUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedRoomId: Long? = null
)

class RoomsViewModel(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _formUiState = MutableStateFlow(RoomFormUiState())
    val formUiState: StateFlow<RoomFormUiState> = _formUiState.asStateFlow()

    private val _editRoom = MutableStateFlow<RoomEntity?>(null)
    val editRoom: StateFlow<RoomEntity?> = _editRoom.asStateFlow()

    private val _editAccessDenied = MutableStateFlow(false)
    val editAccessDenied: StateFlow<Boolean> = _editAccessDenied.asStateFlow()

    private val propertyFlows = mutableMapOf<Long, StateFlow<PropertyEntity?>>()
    private val roomsForPropertyFlows = mutableMapOf<Long, StateFlow<List<RoomEntity>>>()

    fun property(propertyId: Long): StateFlow<PropertyEntity?> =
        cachedStateFlow(propertyFlows, propertyId, null) {
            repository.observeProperty(propertyId)
        }

    fun roomsForProperty(propertyId: Long): StateFlow<List<RoomEntity>> =
        cachedStateFlow(roomsForPropertyFlows, propertyId, emptyList()) {
            repository.observeRoomsForProperty(propertyId)
        }

    fun canManageProperty(propertyId: Long): Boolean =
        authRepository.currentSession()?.canAccessProperty(propertyId) == true

    fun loadRoomForEdit(roomId: Long) {
        viewModelScope.launch {
            val room = repository.getRoomById(roomId)
            val session = authRepository.currentSession()
            val denied = room == null || session == null || !session.canAccessProperty(room.propertyId)
            _editAccessDenied.value = denied
            _editRoom.value = if (denied) null else room
        }
    }

    fun clearEditRoom() {
        _editRoom.value = null
        _editAccessDenied.value = false
    }

    fun createRoom(
        propertyId: Long,
        name: String,
        description: String,
        pricePerNight: Double,
        capacity: Int,
        roomType: String
    ) {
        viewModelScope.launch {
            _formUiState.value = RoomFormUiState(isSaving = true)
            repository.createRoom(propertyId, name, description, pricePerNight, capacity, roomType)
                .onSuccess { id -> _formUiState.value = RoomFormUiState(savedRoomId = id) }
                .onFailure { error -> _formUiState.value = RoomFormUiState(errorMessage = error.message) }
        }
    }

    fun updateRoom(room: RoomEntity) {
        viewModelScope.launch {
            _formUiState.value = RoomFormUiState(isSaving = true)
            repository.updateRoom(room)
                .onSuccess { _formUiState.value = RoomFormUiState(savedRoomId = room.id) }
                .onFailure { error -> _formUiState.value = RoomFormUiState(errorMessage = error.message) }
        }
    }

    fun clearFormState() {
        _formUiState.value = RoomFormUiState()
    }
}
