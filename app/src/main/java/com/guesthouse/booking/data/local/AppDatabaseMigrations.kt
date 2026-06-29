package com.guesthouse.booking.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room migrations. See docs/security/room-migrations.md. */
object AppDatabaseMigrations {
    val MIGRATION_8_9 = Migration(8, 9) { db ->
        db.execSQL("ALTER TABLE rooms ADD COLUMN roomType TEXT NOT NULL DEFAULT 'DOUBLE'")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rooms_roomType` ON `rooms` (`roomType`)")
        db.execSQL("""
            UPDATE rooms SET roomType = CASE
                WHEN LOWER(name) LIKE '%single%' THEN 'SINGLE'
                WHEN LOWER(name) LIKE '%double%' THEN 'DOUBLE'
                WHEN LOWER(name) LIKE '%suite%' THEN 'SUITE'
                WHEN LOWER(name) LIKE '%family%' OR LOWER(name) LIKE '%cottage%' THEN 'FAMILY'
                WHEN LOWER(name) LIKE '%den%' AND capacity >= 4 THEN 'FAMILY'
                WHEN capacity <= 1 THEN 'SINGLE'
                WHEN capacity >= 4 THEN 'FAMILY'
                ELSE 'DOUBLE'
            END
        """.trimIndent())
    }

    val MIGRATION_9_10 = Migration(9, 10) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `block_dates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `propertyId` INTEGER NOT NULL,
                `roomId` INTEGER NOT NULL,
                `startEpochDay` INTEGER NOT NULL,
                `endEpochDay` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `createdByStaffId` INTEGER,
                `createdAtEpochMs` INTEGER NOT NULL,
                FOREIGN KEY(`propertyId`) REFERENCES `properties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_dates_propertyId` ON `block_dates` (`propertyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_dates_roomId` ON `block_dates` (`roomId`)")
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_8_9, MIGRATION_9_10)
}
