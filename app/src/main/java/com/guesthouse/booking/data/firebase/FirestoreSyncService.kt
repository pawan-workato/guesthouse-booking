package com.guesthouse.booking.data.firebase

import android.util.Log
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
        if (profile.role != StaffRole.CHAIN_ADMIN.name && profile.assignedPropertyIds.isNotEmpty()) {
            val existingPropertyIds = database.propertyDao().getAllIncludingInactive().map { it.id }.toSet()
            val assignments = profile.assignedPropertyIds
                .filter { it in existingPropertyIds }
                .map { propertyId -> StaffPropertyAssignmentEntity(profile.staffId, propertyId) }
            if (assignments.isNotEmpty()) {
                database.staffDao().insertAssignments(assignments)
            }
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
        val blockDates = if (session.isChainAdmin) {
            runCatching { firestore.fetchBlockDates() }
                .getOrElse {
                    errors.add("block_dates: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        } else {
            runCatching { firestore.fetchBlockDatesForProperties(accessiblePropertyIds) }
                .getOrElse {
                    errors.add("block_dates: ${it.message ?: it.javaClass.simpleName}")
                    emptyList()
                }
        }

        database.withTransaction {
            // Properties must exist before staff_property_assignments (FK).
            if (properties.isNotEmpty()) {
                database.propertyDao().insertAll(properties)
            }
            if (rooms.isNotEmpty()) {
                database.roomDao().insertAll(rooms)
            }
            for (profile in profiles) {
                cacheStaffProfile(profile)
            }
            if (guests.isNotEmpty()) {
                database.guestDao().upsertAll(guests)
            }
            if (bookings.isNotEmpty()) {
                database.bookingDao().upsertAll(bookings)
            }
            if (blockDates.isNotEmpty()) {
                database.blockDateDao().upsertAll(blockDates)
            }
        }

        if (errors.isNotEmpty()) {
            Log.w(TAG, "pullRemoteData errors for ${session.email}: ${errors.joinToString()}")
        }
        Log.i(
            TAG,
            "pullRemoteData ${session.email} role=${session.role}: staff=${profiles.size} properties=${properties.size} " +
                "rooms=${rooms.size} guests=${guests.size} bookings=${bookings.size} blockDates=${blockDates.size}"
        )

        return PullRemoteDataResult(
            staffCount = profiles.size,
            propertiesCount = properties.size,
            roomsCount = rooms.size,
            guestsCount = guests.size,
            bookingsCount = bookings.size,
            blockDatesCount = blockDates.size,
            errors = errors
        )
    }

    private companion object {
        private const val TAG = "FirestoreSyncService"
    }
}
