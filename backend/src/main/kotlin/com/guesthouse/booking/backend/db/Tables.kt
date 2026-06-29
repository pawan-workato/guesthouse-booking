package com.guesthouse.booking.backend.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object Properties : LongIdTable("properties") {
    val name = varchar("name", 255)
    val address = varchar("address", 512)
    val region = varchar("region", 128)
    val checkInTime = varchar("check_in_time", 8).default("15:00")
    val checkOutTime = varchar("check_out_time", 8).default("11:00")
    val isActive = bool("is_active").default(true)
}

object Rooms : LongIdTable("rooms") {
    val propertyId = reference("property_id", Properties, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val description = text("description").default("")
    val pricePerNight = double("price_per_night")
    val capacity = integer("capacity")
    val roomType = varchar("room_type", 16).default("DOUBLE")
}

object Guests : LongIdTable("guests") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).default("")
    val phone = varchar("phone", 64).default("")
    val notes = text("notes").default("")
    val isActive = bool("is_active").default(true)
    val createdAtEpochMs = long("created_at_epoch_ms")
    val updatedAtEpochMs = long("updated_at_epoch_ms")
}

object Bookings : LongIdTable("bookings") {
    val propertyId = reference("property_id", Properties, onDelete = ReferenceOption.CASCADE)
    val roomId = reference("room_id", Rooms, onDelete = ReferenceOption.CASCADE)
    val guestId = reference("guest_id", Guests, onDelete = ReferenceOption.SET_NULL).nullable()
    val guestName = varchar("guest_name", 255)
    val guestEmail = varchar("guest_email", 255).default("")
    val guestPhone = varchar("guest_phone", 64).default("")
    val checkInEpochDay = long("check_in_epoch_day")
    val checkOutEpochDay = long("check_out_epoch_day")
    val status = varchar("status", 32).default("CONFIRMED")
    val syncStatus = varchar("sync_status", 32).default("SYNCED")
    val bookingReference = varchar("booking_reference", 64).default("")
    val createdAtEpochMs = long("created_at_epoch_ms")
    val updatedAtEpochMs = long("updated_at_epoch_ms")
}

object Staff : LongIdTable("staff") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val displayName = varchar("display_name", 255)
    val role = varchar("role", 32)
    val firebaseUid = varchar("firebase_uid", 128).default("")
    val isActive = bool("is_active").default(true)
}

object StaffPropertyAssignments : LongIdTable("staff_property_assignments") {
    val staffId = reference("staff_id", Staff, onDelete = ReferenceOption.CASCADE)
    val propertyId = reference("property_id", Properties, onDelete = ReferenceOption.CASCADE)

    init {
        uniqueIndex(staffId, propertyId)
    }
}
