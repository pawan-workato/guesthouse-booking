package com.guesthouse.booking.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guesthouse.booking.data.local.AppDatabase

object TestDatabase {
    fun createInMemoryDatabase(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
}
