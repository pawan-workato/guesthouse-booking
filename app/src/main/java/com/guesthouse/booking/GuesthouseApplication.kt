package com.guesthouse.booking

import android.app.Application
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.DatabaseSeeder
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository

class GuesthouseApplication : Application() {
    lateinit var repository: BookingRepository
        private set
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        DatabaseSeeder.seedIfEmpty(database)
        repository = BookingRepository(database)
        authRepository = AuthRepository(database, this)
    }
}
