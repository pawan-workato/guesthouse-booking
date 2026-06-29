package com.guesthouse.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.guesthouse.booking.data.repository.AuditRepository
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.ReportsRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.data.repository.ProfileRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import com.guesthouse.booking.data.local.AppDatabase

class ViewModelFactory(
    private val database: AppDatabase,
    private val repository: BookingRepository,
    private val blockDateRepository: BlockDateRepository,
    private val propertyRepository: PropertyRepository,
    private val guestRepository: GuestRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val staffRepository: StaffRepository,
    private val networkMonitor: NetworkMonitor,
    private val auditRepository: AuditRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(PropertiesViewModel::class.java) ->
                PropertiesViewModel(propertyRepository, OccupancyRepository(database), authRepository) as T
            modelClass.isAssignableFrom(GuestsViewModel::class.java) -> GuestsViewModel(guestRepository, authRepository) as T
            modelClass.isAssignableFrom(RoomsViewModel::class.java) -> RoomsViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel(repository, blockDateRepository, guestRepository, authRepository, syncRepository, networkMonitor) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(TodayViewModel::class.java) -> TodayViewModel(repository, authRepository) as T
            modelClass.isAssignableFrom(SyncViewModel::class.java) ->
                SyncViewModel(syncRepository, repository, authRepository) as T
            modelClass.isAssignableFrom(BookingDetailViewModel::class.java) ->
                BookingDetailViewModel(repository, authRepository, networkMonitor) as T
            modelClass.isAssignableFrom(StaffViewModel::class.java) ->
                StaffViewModel(staffRepository, propertyRepository, authRepository) as T
            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(
                    propertyRepository,
                    OccupancyRepository(database),
                    ReportsRepository(database),
                    authRepository
                ) as T
            modelClass.isAssignableFrom(AuditLogViewModel::class.java) ->
                AuditLogViewModel(auditRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(
                    ProfileRepository(
                        database,
                        authRepository,
                        propertyRepository,
                        FirestoreDataSource(),
                        networkMonitor
                    ),
                    authRepository
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
