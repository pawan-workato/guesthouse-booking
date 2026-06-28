package com.guesthouse.booking.data.firebase

import androidx.room.withTransaction
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import com.guesthouse.booking.data.local.entities.StaffRole

class FirestoreSyncService(
    private val database: AppDatabase,
    private val firestore: FirestoreDataSource
) {
    suspend fun cacheStaffProfile(profile: StaffProfile) {
        database.staffDao().insertAll(
            listOf(
                StaffEntity(
                    id = profile.staffId,
                    email = profile.email,
                    passwordHash = "",
                    displayName = profile.displayName,
                    role = profile.role,
                    firebaseUid = profile.firebaseUid,
                    isActive = true
                )
            )
        )
        database.staffDao().deleteAssignmentsForStaff(profile.staffId)
        if (profile.role != StaffRole.CHAIN_ADMIN.name) {
            database.staffDao().insertAssignments(
                profile.assignedPropertyIds.map { propertyId ->
                    StaffPropertyAssignmentEntity(profile.staffId, propertyId)
                }
            )
        }
    }

    suspend fun pullRemoteData(session: StaffSession) {
        val profiles = firestore.fetchAllStaff()
        val properties = firestore.fetchProperties()
        val rooms = firestore.fetchRooms()
        val guests = firestore.fetchGuests()
        val bookings = firestore.fetchBookings()
            .filter { session.canAccessProperty(it.propertyId) }

        database.withTransaction {
            for (profile in profiles) {
                cacheStaffProfile(profile)
            }
            if (properties.isNotEmpty()) {
                database.propertyDao().insertAll(properties)
            }
            if (rooms.isNotEmpty()) {
                database.roomDao().insertAll(rooms)
            }
            if (guests.isNotEmpty()) {
                database.guestDao().insertAll(guests)
            }
            if (bookings.isNotEmpty()) {
                database.bookingDao().upsertAll(bookings)
            }
        }
    }
}
