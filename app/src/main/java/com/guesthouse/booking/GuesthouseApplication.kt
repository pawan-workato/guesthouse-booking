package com.guesthouse.booking

import android.app.Application
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.DatabaseSeeder
import com.guesthouse.booking.data.repository.BookingRepository

class GuesthouseApplication : Application() {
    lateinit var repository: BookingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        DatabaseSeeder.seedIfEmpty(database)
        repository = BookingRepository(database)
    }
}
