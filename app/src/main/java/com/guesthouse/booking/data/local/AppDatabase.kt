package com.guesthouse.booking.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.guesthouse.booking.BuildConfig
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        PropertyEntity::class,
        RoomEntity::class,
        BookingEntity::class,
        BlockDateEntity::class,
        GuestEntity::class,
        StaffEntity::class,
        StaffPropertyAssignmentEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun roomDao(): RoomDao
    abstract fun bookingDao(): BookingDao
    abstract fun blockDateDao(): BlockDateDao
    abstract fun guestDao(): GuestDao
    abstract fun staffDao(): StaffDao

    companion object {
        private const val DB_NAME = "guesthouse.db"

        @Volatile
        private var instance: AppDatabase? = null

        @Volatile
        private var sqlCipherLoaded = false

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun ensureSqlCipherLoaded() {
            if (sqlCipherLoaded) return
            synchronized(AppDatabase::class.java) {
                if (!sqlCipherLoaded) {
                    System.loadLibrary("sqlcipher")
                    sqlCipherLoaded = true
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            ensureSqlCipherLoaded()
            val passphrase = DatabaseKeyManager.getPassphrase(context)
            PlaintextDatabaseMigrator.migrateIfNeeded(context, DB_NAME, passphrase)
            val factory = SupportOpenHelperFactory(passphrase)
            val builder = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addMigrations(*AppDatabaseMigrations.ALL)
            if (BuildConfig.DEBUG) {
                // DEV-ONLY safety net for version jumps without a registered migration path.
                builder.fallbackToDestructiveMigration(dropAllTables = true)
            }
            return builder.build()
        }
    }
}
