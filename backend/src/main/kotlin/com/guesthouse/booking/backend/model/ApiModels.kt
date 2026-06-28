package com.guesthouse.booking.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val token: String,
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val assignedPropertyIds: List<Long>
)

@Serializable
data class PropertyDto(
    val id: Long,
    val name: String,
    val address: String,
    val region: String,
    val checkInTime: String,
    val checkOutTime: String,
    val isActive: Boolean
)

@Serializable
data class RoomDto(
    val id: Long,
    val propertyId: Long,
    val name: String,
    val description: String,
    val pricePerNight: Double,
    val capacity: Int
)

@Serializable
data class GuestDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val notes: String,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class BookingDto(
    val id: Long,
    val propertyId: Long,
    val roomId: Long,
    val guestId: Long?,
    val guestName: String,
    val guestEmail: String,
    val guestPhone: String,
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val status: String,
    val syncStatus: String,
    val bookingReference: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class StaffDto(
    val id: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val isActive: Boolean,
    val assignedPropertyIds: List<Long>
)

@Serializable
data class GuestSyncItem(
    val localId: Long,
    val serverId: Long? = null,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val updatedAtEpochMs: Long = 0
)

@Serializable
data class GuestSyncRequest(val items: List<GuestSyncItem>)

@Serializable
data class GuestSyncResult(
    val localId: Long,
    val serverId: Long?,
    val status: String,
    val error: String? = null
)

@Serializable
data class GuestSyncResponse(val results: List<GuestSyncResult>)

@Serializable
data class BookingSyncItem(
    val localId: Long,
    val serverId: Long? = null,
    val propertyId: Long,
    val roomId: Long,
    val guestId: Long? = null,
    val guestName: String,
    val guestEmail: String = "",
    val guestPhone: String = "",
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val status: String = "CONFIRMED",
    val updatedAtEpochMs: Long = 0
)

@Serializable
data class BookingSyncRequest(val items: List<BookingSyncItem>)

@Serializable
data class BookingSyncResultItem(
    val localId: Long,
    val serverId: Long?,
    val status: String,
    val bookingReference: String? = null,
    val error: String? = null
)

@Serializable
data class BookingSyncResponse(val results: List<BookingSyncResultItem>)
