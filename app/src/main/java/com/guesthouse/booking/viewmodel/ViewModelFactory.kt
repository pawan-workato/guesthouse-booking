package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor

class ViewModelFactory(
    private val repository: BookingRepository,
    private val propertyRepository: PropertyRepository,
    private val guestRepository: GuestRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val staffRepository: StaffRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(PropertiesViewModel::class.java) -> PropertiesViewModel(propertyRepository, authRepository) as T
            modelClass.isAssignableFrom(GuestsViewModel::class.java) -> GuestsViewModel(guestRepository) as T
            modelClass.isAssignableFrom(RoomsViewModel::class.java) -> RoomsViewModel(repository) as T
            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(repository, guestRepository, authRepository, syncRepository, networkMonitor) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(SyncViewModel::class.java) ->
                SyncViewModel(syncRepository, repository, authRepository) as T
            modelClass.isAssignableFrom(StaffViewModel::class.java) ->
                StaffViewModel(staffRepository, propertyRepository, authRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
