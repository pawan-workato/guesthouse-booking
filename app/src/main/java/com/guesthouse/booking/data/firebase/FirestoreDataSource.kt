package com.guesthouse.booking.data.firebase

import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val isSignedIn: Boolean get() = auth.currentUser != null

    suspend fun getStaffByUid(uid: String): StaffProfile? {
        val snapshot = firestore.collection(FirestoreCollections.STAFF)
            .document(uid)
            .get(Source.SERVER)
            .await()
        return if (snapshot.exists()) snapshot.toStaffProfile() else null
    }

    suspend fun getStaffByEmail(email: String): StaffProfile? {
        val snapshot = firestore.collection(FirestoreCollections.STAFF)
            .whereEqualTo(FirestoreFields.EMAIL, email.trim())
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toStaffProfile()
    }

    suspend fun fetchAllStaff(): List<StaffProfile> =
        firestore.collection(FirestoreCollections.STAFF)
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { it.toStaffProfile() }

    suspend fun fetchProperties(): List<PropertyEntity> =
        firestore.collection(FirestoreCollections.PROPERTIES)
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { it.toPropertyEntity() }

    suspend fun fetchPropertiesByIds(propertyIds: Collection<Long>): List<PropertyEntity> =
        propertyIds.mapNotNull { propertyId ->
            firestore.collection(FirestoreCollections.PROPERTIES)
                .document(propertyId.toString())
                .get(Source.SERVER)
                .await()
                .takeIf { it.exists() }
                ?.toPropertyEntity()
        }

    suspend fun fetchRooms(): List<RoomEntity> =
        firestore.collection(FirestoreCollections.ROOMS)
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { it.toRoomEntity() }

    suspend fun fetchRoomsForProperties(propertyIds: Collection<Long>): List<RoomEntity> =
        propertyIds.chunked(WHERE_IN_LIMIT).flatMap { chunk ->
            firestore.collection(FirestoreCollections.ROOMS)
                .whereIn(FirestoreFields.PROPERTY_ID, chunk)
                .get(Source.SERVER)
                .await()
                .documents.mapNotNull { it.toRoomEntity() }
        }

    suspend fun fetchGuests(): List<GuestEntity> =
        firestore.collection(FirestoreCollections.GUESTS)
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { it.toGuestEntity() }

    suspend fun fetchBookings(): List<BookingEntity> =
        firestore.collection(FirestoreCollections.BOOKINGS)
            .get(Source.SERVER)
            .await()
            .documents.mapNotNull { it.toBookingEntity() }

    suspend fun fetchBookingsForProperties(propertyIds: Collection<Long>): List<BookingEntity> =
        propertyIds.chunked(WHERE_IN_LIMIT).flatMap { chunk ->
            firestore.collection(FirestoreCollections.BOOKINGS)
                .whereIn(FirestoreFields.PROPERTY_ID, chunk)
                .get(Source.SERVER)
                .await()
                .documents.mapNotNull { it.toBookingEntity() }
        }

    suspend fun upsertProperty(property: PropertyEntity) {
        firestore.collection(FirestoreCollections.PROPERTIES)
            .document(property.id.toString())
            .set(property.toFirestoreMap())
            .await()
    }

    suspend fun upsertGuest(guest: GuestEntity) {
        firestore.collection(FirestoreCollections.GUESTS)
            .document(guest.id.toString())
            .set(guest.toFirestoreMap())
            .await()
    }

    suspend fun upsertBooking(booking: BookingEntity) {
        firestore.collection(FirestoreCollections.BOOKINGS)
            .document(booking.id.toString())
            .set(booking.toFirestoreMap())
            .await()
    }

    suspend fun findOverlappingRemoteBookings(
        roomId: Long,
        checkIn: Long,
        checkOut: Long,
        excludeId: Long = 0L
    ): List<BookingEntity> {
        val snapshot = firestore.collection(FirestoreCollections.BOOKINGS)
            .whereEqualTo(FirestoreFields.ROOM_ID, roomId)
            .whereEqualTo(FirestoreFields.STATUS, BookingStatus.CONFIRMED.name)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toBookingEntity() }
            .filter { booking ->
                booking.id != excludeId &&
                    booking.syncStatus != SyncStatus.CONFLICT.name &&
                    booking.checkInEpochDay < checkOut &&
                    booking.checkOutEpochDay > checkIn
            }
    }

    suspend fun updateBookingStatus(bookingId: Long, status: String) {
        firestore.collection(FirestoreCollections.BOOKINGS)
            .document(bookingId.toString())
            .update(FirestoreFields.STATUS, status)
            .await()
    }

    private companion object {
        const val WHERE_IN_LIMIT = 10
    }
}
