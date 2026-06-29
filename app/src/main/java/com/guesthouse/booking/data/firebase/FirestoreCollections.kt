package com.guesthouse.booking.data.firebase

object FirestoreCollections {
    const val STAFF = "staff"
    const val PROPERTIES = "properties"
    const val ROOMS = "rooms"
    const val BOOKINGS = "bookings"
    const val GUESTS = "guests"
    const val BLOCK_DATES = "block_dates"
    const val STAFF_ASSIGNMENTS = "staff_assignments"
}

object FirestoreFields {
    const val STAFF_ID = "staffId"
    const val EMAIL = "email"
    const val DISPLAY_NAME = "displayName"
    const val ROLE = "role"
    const val ASSIGNED_PROPERTY_IDS = "assignedPropertyIds"
    const val FIREBASE_UID = "firebaseUid"

    const val NAME = "name"
    const val ADDRESS = "address"
    const val REGION = "region"
    const val CHECK_IN_TIME = "checkInTime"
    const val CHECK_OUT_TIME = "checkOutTime"
    const val IS_ACTIVE = "isActive"

    const val PROPERTY_ID = "propertyId"
    const val DESCRIPTION = "description"
    const val PRICE_PER_NIGHT = "pricePerNight"
    const val CAPACITY = "capacity"
    const val ROOM_TYPE = "roomType"

    const val ROOM_ID = "roomId"
    const val GUEST_ID = "guestId"
    const val GUEST_NAME = "guestName"
    const val GUEST_EMAIL = "guestEmail"
    const val GUEST_PHONE = "guestPhone"
    const val CHECK_IN_EPOCH_DAY = "checkInEpochDay"
    const val CHECK_OUT_EPOCH_DAY = "checkOutEpochDay"
    const val STATUS = "status"
    const val SYNC_STATUS = "syncStatus"
    const val BOOKING_REFERENCE = "bookingReference"
    const val CREATED_AT_EPOCH_MS = "createdAtEpochMs"

    const val PHONE = "phone"
    const val NOTES = "notes"

    const val START_EPOCH_DAY = "startEpochDay"
    const val END_EPOCH_DAY = "endEpochDay"
    const val REASON = "reason"
    const val CREATED_BY_STAFF_ID = "createdByStaffId"
    const val MARKED_FOR_DELETION = "markedForDeletion"
}
