package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.GuestEntity
import kotlinx.coroutines.flow.Flow

class GuestRepository(private val database: AppDatabase) {
    fun observeActiveGuests(): Flow<List<GuestEntity>> = database.guestDao().observeActive()

    fun observeAllGuests(): Flow<List<GuestEntity>> = database.guestDao().observeAllIncludingInactive()

    fun observeGuest(guestId: Long): Flow<GuestEntity?> = database.guestDao().observeById(guestId)

    suspend fun getGuest(guestId: Long): GuestEntity? = database.guestDao().getById(guestId)

    suspend fun createGuest(
        name: String,
        email: String,
        phone: String,
        notes: String
    ): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        val id = database.guestDao().insert(
            GuestEntity(
                name = trimmedName,
                email = email.trim(),
                phone = phone.trim(),
                notes = notes.trim(),
                isActive = true
            )
        )
        return Result.success(id)
    }

    suspend fun updateGuest(guest: GuestEntity): Result<Unit> {
        if (guest.name.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        database.guestDao().update(
            guest.copy(
                name = guest.name.trim(),
                email = guest.email.trim(),
                phone = guest.phone.trim(),
                notes = guest.notes.trim()
            )
        )
        return Result.success(Unit)
    }

    suspend fun setGuestActive(guestId: Long, active: Boolean) {
        database.guestDao().setActive(guestId, active)
    }
}
