package com.guesthouse.booking.data.local

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.RandomAccessFile

/**
 * One-time migration from legacy plaintext SQLite to SQLCipher-encrypted storage.
 * See docs/security/room-migrations.md.
 */
internal object PlaintextDatabaseMigrator {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun migrateIfNeeded(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || !isPlaintextSqlite(dbFile)) return

        val tempFile = File(dbFile.parent, "$dbName.encrypting")
        if (tempFile.exists()) tempFile.delete()

        SQLiteDatabase.openOrCreateDatabase(tempFile, passphrase, null, null, null).use { encrypted ->
            encrypted.execSQL("ATTACH DATABASE '${dbFile.absolutePath}' AS plaintext KEY ''")
            encrypted.rawQuery("SELECT sqlcipher_export('main', 'plaintext')", null).use { cursor ->
                cursor.moveToFirst()
            }
            encrypted.execSQL("DETACH DATABASE plaintext")
        }

        deleteIfExists(dbFile)
        deleteIfExists(File("${dbFile.absolutePath}-wal"))
        deleteIfExists(File("${dbFile.absolutePath}-shm"))
        deleteIfExists(File("${dbFile.absolutePath}-journal"))

        check(tempFile.renameTo(dbFile)) {
            "Failed to finalize encrypted database migration for $dbName"
        }
    }

    private fun isPlaintextSqlite(dbFile: File): Boolean {
        if (dbFile.length() < SQLITE_HEADER.size) return false
        RandomAccessFile(dbFile, "r").use { raf ->
            val header = ByteArray(SQLITE_HEADER.size)
            raf.readFully(header)
            return header.contentEquals(SQLITE_HEADER)
        }
    }

    private fun deleteIfExists(file: File) {
        if (file.exists()) file.delete()
    }
}
