package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Query(
        """
        SELECT DISTINCT guestId FROM bookings
        WHERE guestId IS NOT NULL AND propertyId IN (:propertyIds)
        """
    )
    fun observeGuestIdsForProperties(propertyIds: List<Long>): Flow<List<Long>>

    @Query(
        """
        SELECT DISTINCT guestId FROM bookings
        WHERE guestId IS NOT NULL AND propertyId IN (:propertyIds)
        """
    )
    suspend fun getGuestIdsForProperties(propertyIds: List<Long>): List<Long>

    @Query("SELECT * FROM bookings ORDER BY checkInEpochDay DESC")
    fun observeAll(): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE guestId = :guestId
        ORDER BY checkInEpochDay DESC
        """
    )
    fun observeForGuest(guestId: Long): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE guestId = :guestId AND propertyId IN (:propertyIds)
        ORDER BY checkInEpochDay DESC
        """
    )
    fun observeForGuestAtProperties(guestId: Long, propertyIds: List<Long>): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: Long): BookingEntity?

    @Query(
        """
        SELECT * FROM bookings
        WHERE roomId = :roomId
        AND status IN ('CONFIRMED', 'CHECKED_IN')
        AND syncStatus != :conflictStatus
        ORDER BY checkInEpochDay ASC
        """
    )
    fun observeActiveForRoom(
        roomId: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name
    ): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE roomId = :roomId
        AND status IN ('CONFIRMED', 'CHECKED_IN')
        AND syncStatus != :conflictStatus
        AND (:excludeId = 0 OR id != :excludeId)
        AND checkInEpochDay < :checkOut AND checkOutEpochDay > :checkIn
        """
    )
    suspend fun findOverlapping(
        roomId: Long,
        checkIn: Long,
        checkOut: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name,
        excludeId: Long = 0L
    ): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE syncStatus = :syncStatus ORDER BY createdAtEpochMs ASC")
    suspend fun getBySyncStatus(syncStatus: String): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE syncStatus = :syncStatus ORDER BY createdAtEpochMs DESC")
    fun observeBySyncStatus(syncStatus: String): Flow<List<BookingEntity>>

    @Query("SELECT COUNT(*) FROM bookings WHERE syncStatus = :syncStatus")
    fun observeCountBySyncStatus(syncStatus: String): Flow<Int>

    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Update
    suspend fun update(booking: BookingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bookings: List<BookingEntity>)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        """
        UPDATE bookings SET syncStatus = :syncStatus, bookingReference = :reference
        WHERE id = :id
        """
    )
    suspend fun updateSync(id: Long, syncStatus: String, reference: String)

    @Query("UPDATE bookings SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)

    @Query("SELECT * FROM bookings WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): BookingEntity?

    @Query(
        """
        UPDATE bookings SET syncStatus = :syncStatus, bookingReference = :reference,
        serverId = :serverId, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id
        """
    )
    suspend fun updateAfterSync(
        id: Long,
        syncStatus: String,
        reference: String,
        serverId: Long,
        updatedAtEpochMs: Long
    )

    @Query(
        """
        SELECT DISTINCT roomId FROM bookings
        WHERE propertyId = :propertyId
        AND status IN ('CONFIRMED', 'CHECKED_IN')
        AND syncStatus != :conflictStatus
        AND checkInEpochDay <= :epochDay AND checkOutEpochDay > :epochDay
        """
    )
    suspend fun getOccupiedRoomIds(
        propertyId: Long,
        epochDay: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*) FROM bookings
        WHERE propertyId = :propertyId
        AND status = 'CONFIRMED'
        AND syncStatus != :conflictStatus
        AND checkInEpochDay = :epochDay
        """
    )
    suspend fun countArrivalsToday(
        propertyId: Long,
        epochDay: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM bookings
        WHERE propertyId = :propertyId
        AND status = 'CHECKED_IN'
        AND syncStatus != :conflictStatus
        AND checkOutEpochDay = :epochDay
        """
    )
    suspend fun countDeparturesToday(
        propertyId: Long,
        epochDay: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name
    ): Int

}
