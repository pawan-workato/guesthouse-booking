package com.guesthouse.booking.data.remote

import androidx.room.withTransaction
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.SyncRepository

data class KtorPendingSyncOutcome(
    val syncedCount: Int = 0,
    val conflictCount: Int = 0
)

class KtorApiSyncService(
    private val database: AppDatabase,
    private val api: GuesthouseApi,
    private val authRepository: AuthRepository
) {

    suspend fun pullBootstrap(session: StaffSession) {
        database.withTransaction {
            val properties = api.getProperties().map { dto ->
                PropertyEntity(
                    id = dto.id,
                    name = dto.name,
                    address = dto.address,
                    region = dto.region,
                    checkInTime = dto.checkInTime,
                    checkOutTime = dto.checkOutTime,
                    isActive = dto.isActive
                )
            }
            if (properties.isNotEmpty()) {
                database.propertyDao().insertAll(properties)
            }

            val rooms = api.getRooms().map { dto ->
                RoomEntity(
                    id = dto.id,
                    propertyId = dto.propertyId,
                    name = dto.name,
                    description = dto.description,
                    pricePerNight = dto.pricePerNight,
                    capacity = dto.capacity
                )
            }
            if (rooms.isNotEmpty()) {
                database.roomDao().insertAll(rooms)
            }

            mergeGuestsFromServer(api.getGuests())
            mergeBookingsFromServer(
                api.getBookings().filter { session.canAccessProperty(it.propertyId) }
            )
        }
    }

    suspend fun syncPending(): KtorPendingSyncOutcome {
        var syncedCount = 0
        var conflictCount = 0

        val guestOutcome = pushPendingGuests()
        syncedCount += guestOutcome.syncedCount
        conflictCount += guestOutcome.conflictCount
        mergeGuestsFromServer(api.getGuests())

        val bookingOutcome = pushPendingBookings()
        syncedCount += bookingOutcome.syncedCount
        conflictCount += bookingOutcome.conflictCount
        val session = authRepository.currentSession()
        if (session != null) {
            mergeBookingsFromServer(
                api.getBookings().filter { session.canAccessProperty(it.propertyId) }
            )
        }

        return KtorPendingSyncOutcome(syncedCount = syncedCount, conflictCount = conflictCount)
    }

    private suspend fun pushPendingGuests(): KtorPendingSyncOutcome {
        val pending = database.guestDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
        if (pending.isEmpty()) return KtorPendingSyncOutcome()

        val items = pending.map { guest ->
            GuestSyncItem(
                localId = guest.id,
                serverId = guest.serverId,
                name = guest.name,
                email = guest.email,
                phone = guest.phone,
                notes = guest.notes,
                isActive = guest.isActive,
                updatedAtEpochMs = guest.updatedAtEpochMs
            )
        }
        val response = api.syncGuests(GuestSyncRequest(items))
        var syncedCount = 0
        var conflictCount = 0
        for (result in response.results) {
            when (result.status) {
                "SYNCED" -> {
                    val serverId = result.serverId ?: continue
                    val localId = result.localId
                    val updatedAt = System.currentTimeMillis()
                    database.guestDao().updateAfterSync(
                        id = localId,
                        serverId = serverId,
                        syncStatus = SyncStatus.SYNCED.name,
                        updatedAtEpochMs = updatedAt
                    )
                    syncedCount++
                }
                "CONFLICT", "ERROR", "NOT_FOUND", "FORBIDDEN" -> conflictCount++
            }
        }
        return KtorPendingSyncOutcome(syncedCount = syncedCount, conflictCount = conflictCount)
    }

    private suspend fun pushPendingBookings(): KtorPendingSyncOutcome {
        val pending = database.bookingDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
        if (pending.isEmpty()) return KtorPendingSyncOutcome()

        val items = pending.map { booking ->
            BookingSyncItem(
                localId = booking.id,
                serverId = booking.serverId,
                propertyId = booking.propertyId,
                roomId = booking.roomId,
                guestId = resolveServerGuestId(booking.guestId),
                guestName = booking.guestName,
                guestEmail = booking.guestEmail,
                guestPhone = booking.guestPhone,
                checkInEpochDay = booking.checkInEpochDay,
                checkOutEpochDay = booking.checkOutEpochDay,
                status = booking.status,
                updatedAtEpochMs = booking.updatedAtEpochMs
            )
        }
        val propertyByLocalId = pending.associate { it.id to it.propertyId }
        val response = api.syncBookings(BookingSyncRequest(items))
        var syncedCount = 0
        var conflictCount = 0
        for (result in response.results) {
            when (result.status) {
                "SYNCED" -> {
                    val serverId = result.serverId ?: continue
                    val reference = result.bookingReference
                        ?: SyncRepository.formatReference(
                            propertyByLocalId[result.localId] ?: 0L,
                            serverId
                        )
                    database.bookingDao().updateAfterSync(
                        id = result.localId,
                        syncStatus = SyncStatus.SYNCED.name,
                        reference = reference,
                        serverId = serverId,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                    syncedCount++
                }
                "CONFLICT", "ERROR", "FORBIDDEN" -> {
                    database.bookingDao().updateSyncStatus(result.localId, SyncStatus.CONFLICT.name)
                    conflictCount++
                }
            }
        }
        return KtorPendingSyncOutcome(syncedCount = syncedCount, conflictCount = conflictCount)
    }

    private suspend fun mergeGuestsFromServer(remoteGuests: List<GuestDto>) {
        for (dto in remoteGuests) {
            mergeGuestDto(dto)
        }
    }

    private suspend fun mergeGuestDto(dto: GuestDto) {
        val byServerId = database.guestDao().getByServerId(dto.id)
        if (byServerId != null) {
            if (byServerId.syncStatus != SyncStatus.PENDING_SYNC.name &&
                dto.updatedAtEpochMs >= byServerId.updatedAtEpochMs
            ) {
                database.guestDao().update(
                    byServerId.copy(
                        name = dto.name,
                        email = dto.email,
                        phone = dto.phone,
                        notes = dto.notes,
                        isActive = dto.isActive,
                        serverId = dto.id,
                        syncStatus = SyncStatus.SYNCED.name,
                        updatedAtEpochMs = dto.updatedAtEpochMs
                    )
                )
            }
            return
        }

        database.guestDao().insert(
            GuestEntity(
                name = dto.name,
                email = dto.email,
                phone = dto.phone,
                notes = dto.notes,
                isActive = dto.isActive,
                serverId = dto.id,
                syncStatus = SyncStatus.SYNCED.name,
                createdAtEpochMs = dto.createdAtEpochMs,
                updatedAtEpochMs = dto.updatedAtEpochMs
            )
        )
    }

    private suspend fun mergeBookingsFromServer(remoteBookings: List<BookingDto>) {
        for (dto in remoteBookings) {
            mergeBookingDto(dto)
        }
    }

    private suspend fun mergeBookingDto(dto: BookingDto) {
        val localGuestId = dto.guestId?.let { serverGuestId ->
            database.guestDao().getByServerId(serverGuestId)?.id
        }

        val byServerId = database.bookingDao().getByServerId(dto.id)
        if (byServerId != null) {
            if (byServerId.syncStatus != SyncStatus.PENDING_SYNC.name &&
                dto.updatedAtEpochMs >= byServerId.updatedAtEpochMs
            ) {
                database.bookingDao().upsertAll(
                    listOf(
                        byServerId.copy(
                            propertyId = dto.propertyId,
                            roomId = dto.roomId,
                            guestId = localGuestId,
                            guestName = dto.guestName,
                            guestEmail = dto.guestEmail,
                            guestPhone = dto.guestPhone,
                            checkInEpochDay = dto.checkInEpochDay,
                            checkOutEpochDay = dto.checkOutEpochDay,
                            status = dto.status,
                            bookingReference = dto.bookingReference,
                            serverId = dto.id,
                            syncStatus = SyncStatus.SYNCED.name,
                            updatedAtEpochMs = dto.updatedAtEpochMs
                        )
                    )
                )
            }
            return
        }

        database.bookingDao().upsertAll(
            listOf(
                BookingEntity(
                    id = dto.id,
                    propertyId = dto.propertyId,
                    roomId = dto.roomId,
                    guestId = localGuestId,
                    guestName = dto.guestName,
                    guestEmail = dto.guestEmail,
                    guestPhone = dto.guestPhone,
                    checkInEpochDay = dto.checkInEpochDay,
                    checkOutEpochDay = dto.checkOutEpochDay,
                    status = dto.status,
                    syncStatus = SyncStatus.SYNCED.name,
                    bookingReference = dto.bookingReference,
                    serverId = dto.id,
                    createdAtEpochMs = dto.createdAtEpochMs,
                    updatedAtEpochMs = dto.updatedAtEpochMs
                )
            )
        )
    }

    private suspend fun resolveServerGuestId(localGuestId: Long?): Long? {
        if (localGuestId == null) return null
        return database.guestDao().getById(localGuestId)?.serverId ?: localGuestId
    }
}
