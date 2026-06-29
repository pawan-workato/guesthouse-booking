package com.guesthouse.booking.data.firebase

import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.HousekeepingStatus
import com.guesthouse.booking.data.local.entities.BookingSource
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.google.firebase.firestore.DocumentSnapshot

data class StaffProfile(
    val firebaseUid: String,
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val assignedPropertyIds: List<Long>
)

fun DocumentSnapshot.toStaffProfile(): StaffProfile? {
    val staffId = getLong(FirestoreFields.STAFF_ID) ?: return null
    val email = getString(FirestoreFields.EMAIL) ?: return null
    val displayName = getString(FirestoreFields.DISPLAY_NAME) ?: return null
    val role = getString(FirestoreFields.ROLE) ?: return null
    @Suppress("UNCHECKED_CAST")
    val assigned = (get(FirestoreFields.ASSIGNED_PROPERTY_IDS) as? List<Number>)?.map { it.toLong() } ?: emptyList()
    return StaffProfile(id, staffId, email, displayName, role, assigned)
}

fun StaffProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.STAFF_ID to staffId,
    FirestoreFields.EMAIL to email,
    FirestoreFields.DISPLAY_NAME to displayName,
    FirestoreFields.ROLE to role,
    FirestoreFields.ASSIGNED_PROPERTY_IDS to assignedPropertyIds,
    FirestoreFields.FIREBASE_UID to firebaseUid
)

fun PropertyEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.NAME to name,
    FirestoreFields.ADDRESS to address,
    FirestoreFields.REGION to region,
    FirestoreFields.CHECK_IN_TIME to checkInTime,
    FirestoreFields.CHECK_OUT_TIME to checkOutTime,
    FirestoreFields.IS_ACTIVE to isActive
)

fun DocumentSnapshot.toPropertyEntity(): PropertyEntity? {
    val docId = id.toLongOrNull() ?: return null
    return PropertyEntity(
        id = docId,
        name = getString(FirestoreFields.NAME) ?: return null,
        address = getString(FirestoreFields.ADDRESS) ?: "",
        region = getString(FirestoreFields.REGION) ?: "",
        checkInTime = getString(FirestoreFields.CHECK_IN_TIME) ?: "15:00",
        checkOutTime = getString(FirestoreFields.CHECK_OUT_TIME) ?: "11:00",
        isActive = getBoolean(FirestoreFields.IS_ACTIVE) ?: true
    )
}

fun RoomEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.PROPERTY_ID to propertyId,
    FirestoreFields.NAME to name,
    FirestoreFields.DESCRIPTION to description,
    FirestoreFields.PRICE_PER_NIGHT to pricePerNight,
    FirestoreFields.CAPACITY to capacity,
    FirestoreFields.ROOM_TYPE to roomType,
    "housekeepingStatus" to housekeepingStatus
)

fun DocumentSnapshot.toRoomEntity(): RoomEntity? {
    val docId = id.toLongOrNull() ?: return null
    val propertyId = getLong(FirestoreFields.PROPERTY_ID) ?: return null
    return RoomEntity(
        id = docId,
        propertyId = propertyId,
        name = getString(FirestoreFields.NAME) ?: return null,
        description = getString(FirestoreFields.DESCRIPTION) ?: "",
        pricePerNight = (get(FirestoreFields.PRICE_PER_NIGHT) as? Number)?.toDouble() ?: 0.0,
        capacity = getLong(FirestoreFields.CAPACITY)?.toInt() ?: 1,
        roomType = getString(FirestoreFields.ROOM_TYPE)
            ?: RoomType.inferFromName(getString(FirestoreFields.NAME) ?: "", getLong(FirestoreFields.CAPACITY)?.toInt() ?: 2).name
    )
}

fun GuestEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.NAME to name,
    FirestoreFields.EMAIL to email,
    FirestoreFields.PHONE to phone,
    FirestoreFields.NOTES to notes,
    "preferences" to preferences,
    FirestoreFields.IS_ACTIVE to isActive,
    FirestoreFields.CREATED_AT_EPOCH_MS to createdAtEpochMs
)

fun DocumentSnapshot.toGuestEntity(): GuestEntity? {
    val docId = id.toLongOrNull() ?: return null
    return GuestEntity(
        id = docId,
        name = getString(FirestoreFields.NAME) ?: return null,
        email = getString(FirestoreFields.EMAIL) ?: "",
        phone = getString(FirestoreFields.PHONE) ?: "",
        notes = getString(FirestoreFields.NOTES) ?: "",
        preferences = getString("preferences") ?: "",
        isActive = getBoolean(FirestoreFields.IS_ACTIVE) ?: true,
        createdAtEpochMs = getLong(FirestoreFields.CREATED_AT_EPOCH_MS) ?: System.currentTimeMillis()
    )
}

fun BookingEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.PROPERTY_ID to propertyId,
    FirestoreFields.ROOM_ID to roomId,
    FirestoreFields.GUEST_ID to guestId,
    FirestoreFields.GUEST_NAME to guestName,
    FirestoreFields.GUEST_EMAIL to guestEmail,
    FirestoreFields.GUEST_PHONE to guestPhone,
    FirestoreFields.CHECK_IN_EPOCH_DAY to checkInEpochDay,
    FirestoreFields.CHECK_OUT_EPOCH_DAY to checkOutEpochDay,
    FirestoreFields.STATUS to status,
    "source" to source,
    "maintenanceNotes" to maintenanceNotes,
    FirestoreFields.SYNC_STATUS to syncStatus,
    FirestoreFields.BOOKING_REFERENCE to bookingReference,
    FirestoreFields.CREATED_AT_EPOCH_MS to createdAtEpochMs
)

fun DocumentSnapshot.toBookingEntity(): BookingEntity? {
    val docId = id.toLongOrNull() ?: return null
    val propertyId = getLong(FirestoreFields.PROPERTY_ID) ?: return null
    val roomId = getLong(FirestoreFields.ROOM_ID) ?: return null
    return BookingEntity(
        id = docId,
        propertyId = propertyId,
        roomId = roomId,
        guestId = getLong(FirestoreFields.GUEST_ID),
        guestName = getString(FirestoreFields.GUEST_NAME) ?: "",
        guestEmail = getString(FirestoreFields.GUEST_EMAIL) ?: "",
        guestPhone = getString(FirestoreFields.GUEST_PHONE) ?: "",
        checkInEpochDay = getLong(FirestoreFields.CHECK_IN_EPOCH_DAY) ?: return null,
        checkOutEpochDay = getLong(FirestoreFields.CHECK_OUT_EPOCH_DAY) ?: return null,
        status = getString(FirestoreFields.STATUS) ?: "CONFIRMED",
        syncStatus = getString(FirestoreFields.SYNC_STATUS) ?: "SYNCED",
        bookingReference = getString(FirestoreFields.BOOKING_REFERENCE) ?: "",
        createdAtEpochMs = getLong(FirestoreFields.CREATED_AT_EPOCH_MS) ?: System.currentTimeMillis()
    )
}

fun StaffPropertyAssignmentEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.STAFF_ID to staffId,
    FirestoreFields.PROPERTY_ID to propertyId
)

fun BlockDateEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    FirestoreFields.PROPERTY_ID to propertyId,
    FirestoreFields.ROOM_ID to roomId,
    FirestoreFields.START_EPOCH_DAY to startEpochDay,
    FirestoreFields.END_EPOCH_DAY to endEpochDay,
    FirestoreFields.REASON to reason,
    FirestoreFields.CREATED_BY_STAFF_ID to createdByStaffId,
    FirestoreFields.CREATED_AT_EPOCH_MS to createdAtEpochMs,
    FirestoreFields.SYNC_STATUS to syncStatus,
    FirestoreFields.MARKED_FOR_DELETION to markedForDeletion
)

fun DocumentSnapshot.toBlockDateEntity(): BlockDateEntity? {
    val docId = id.toLongOrNull() ?: return null
    val propertyId = getLong(FirestoreFields.PROPERTY_ID) ?: return null
    val roomId = getLong(FirestoreFields.ROOM_ID) ?: return null
    return BlockDateEntity(
        id = docId,
        propertyId = propertyId,
        roomId = roomId,
        startEpochDay = getLong(FirestoreFields.START_EPOCH_DAY) ?: return null,
        endEpochDay = getLong(FirestoreFields.END_EPOCH_DAY) ?: return null,
        reason = getString(FirestoreFields.REASON) ?: "",
        createdByStaffId = getLong(FirestoreFields.CREATED_BY_STAFF_ID),
        createdAtEpochMs = getLong(FirestoreFields.CREATED_AT_EPOCH_MS) ?: System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED.name,
        markedForDeletion = getBoolean(FirestoreFields.MARKED_FOR_DELETION) ?: false
    )
}
