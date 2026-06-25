package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.guesthouse.booking.data.repository.BookingRepository

class ViewModelFactory(
    private val repository: BookingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RoomsViewModel::class.java) ->
                RoomsViewModel(repository) as T
            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(repository) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                AdminViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
