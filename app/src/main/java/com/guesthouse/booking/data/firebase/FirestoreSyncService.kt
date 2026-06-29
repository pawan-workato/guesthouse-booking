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

    suspend fun pullRemoteData(session: StaffSession): PullRemoteDataResult {
        val errors = mutableListOf<String>()
        val accessiblePropertyIds = session.assignedPropertyIds

        val profiles = if (session.isChainAdmin) {
            runCatching { firestore.fetchAllStaff() }
                .getOrElse {
                    errors.add("staff: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        } else {
            emptyList()
        }
        val properties = if (session.isChainAdmin) {
            runCatching { firestore.fetchProperties() }
                .getOrElse {
                    errors.add("properties: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        } else {
            runCatching { firestore.fetchPropertiesByIds(accessiblePropertyIds) }
                .getOrElse {
                    errors.add("properties: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        }
        val rooms = if (session.isChainAdmin) {
            runCatching { firestore.fetchRooms() }
                .getOrElse {
                    errors.add("rooms: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        } else {
            runCatching { firestore.fetchRoomsForProperties(accessiblePropertyIds) }
                .getOrElse {
                    errors.add("rooms: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        }
        val guests = runCatching { firestore.fetchGuests() }
            .getOrElse {
                errors.add("guests: ${it.message ?: it.javaClass.simpleName}")
                emptyList()
            }
        val bookings = if (session.isChainAdmin) {
            runCatching { firestore.fetchBookings() }
                .getOrElse {
                    errors.add("bookings: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        } else {
            runCatching { firestore.fetchBookingsForProperties(accessiblePropertyIds) }
                .getOrElse {
                    errors.add("bookings: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        }

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
                database.guestDao().upsertAll(guests)
            }
            if (bookings.isNotEmpty()) {
                database.bookingDao().upsertAll(bookings)
            }
        }

        return PullRemoteDataResult(
            staffCount = profiles.size,
            propertiesCount = properties.size,
            roomsCount = rooms.size,
            guestsCount = guests.size,
            bookingsCount = bookings.size,
            errors = errors
        )
    }
}
