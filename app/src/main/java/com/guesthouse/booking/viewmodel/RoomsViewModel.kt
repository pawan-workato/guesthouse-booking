package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.repository.BookingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RoomsViewModel(private val repository: BookingRepository) : ViewModel() {
    fun property(propertyId: Long): StateFlow<PropertyEntity?> =
        repository.observeProperty(propertyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun roomsForProperty(propertyId: Long): StateFlow<List<RoomEntity>> =
        repository.observeRoomsForProperty(propertyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
