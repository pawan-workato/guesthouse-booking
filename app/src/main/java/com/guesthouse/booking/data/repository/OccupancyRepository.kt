package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import java.time.LocalDate

class OccupancyRepository(
    private val database: AppDatabase
) {
    suspend fun getStatsForProperty(
        property: PropertyEntity,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): PropertyOccupancyStats {
        val propertyId = property.id
        val conflictStatus = SyncStatus.CONFLICT.name
        val bookingDao = database.bookingDao()
        val blockDateDao = database.blockDateDao()
        val roomDao = database.roomDao()

        val totalRooms = roomDao.countForProperty(propertyId)
        val occupiedRoomIds = bookingDao.getOccupiedRoomIds(propertyId, epochDay, conflictStatus).toSet()
        val blockedRoomIds = blockDateDao.getBlockedRoomIdsForProperty(propertyId, epochDay, conflictStatus).toSet()
        val unavailable = occupiedRoomIds + blockedRoomIds

        return PropertyOccupancyStats(
            propertyId = propertyId,
            propertyName = property.name,
            totalRooms = totalRooms,
            occupiedTonight = occupiedRoomIds.size,
            arrivalsToday = bookingDao.countArrivalsToday(propertyId, epochDay, conflictStatus),
            departuresToday = bookingDao.countDeparturesToday(propertyId, epochDay, conflictStatus),
            blockedRooms = blockedRoomIds.size,
            vacant = (totalRooms - unavailable.size).coerceAtLeast(0)
        )
    }

    suspend fun getStatsForProperties(
        properties: List<PropertyEntity>,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): List<PropertyOccupancyStats> = properties.map { getStatsForProperty(it, epochDay) }

    suspend fun countArrivalsTodayForProperties(
        propertyIds: Collection<Long>,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): Int {
        if (propertyIds.isEmpty()) return 0
        val conflictStatus = SyncStatus.CONFLICT.name
        return propertyIds.sumOf { id ->
            database.bookingDao().countArrivalsToday(id, epochDay, conflictStatus)
        }
    }
}
