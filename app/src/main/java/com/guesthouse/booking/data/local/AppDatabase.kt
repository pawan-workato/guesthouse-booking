package com.guesthouse.booking.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity

@Database(
    entities = [RoomEntity::class, BookingEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(BookingStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guesthouse.db"
                ).build().also { instance = it }
            }
        }
    }
}

class BookingStatusConverter {
    @TypeConverter
    fun fromStatus(status: BookingStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): BookingStatus = BookingStatus.valueOf(value)
}
