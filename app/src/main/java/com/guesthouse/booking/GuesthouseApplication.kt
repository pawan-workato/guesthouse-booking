package com.guesthouse.booking

import android.app.Application
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.DatabaseSeeder
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor

class GuesthouseApplication : Application() {
    lateinit var repository: BookingRepository
        private set
    lateinit var propertyRepository: PropertyRepository
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        DatabaseSeeder.seedIfEmpty(database)
        repository = BookingRepository(database)
        propertyRepository = PropertyRepository(database)
        authRepository = AuthRepository(database, this)
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start()
        syncRepository = SyncRepository(database, networkMonitor, this)
        syncRepository.schedulePeriodicSync()
    }
}
