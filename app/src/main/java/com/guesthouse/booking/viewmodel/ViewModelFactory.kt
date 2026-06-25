package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository

class ViewModelFactory(
    private val repository: BookingRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(PropertiesViewModel::class.java) ->
                PropertiesViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(RoomsViewModel::class.java) ->
                RoomsViewModel(repository) as T
            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                AdminViewModel(repository, authRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
