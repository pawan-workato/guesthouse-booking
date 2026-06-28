package com.guesthouse.booking.data.remote

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val assignedPropertyIds: List<Long>
) {
    fun toStaffDto(): StaffDto = StaffDto(staffId, email, displayName, role, assignedPropertyIds)
}

data class StaffDto(
    val id: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val assignedPropertyIds: List<Long>
)

data class PropertyDto(
    val id: Long,
    val name: String,
    val address: String,
    val region: String,
    val checkInTime: String,
    val checkOutTime: String,
    val isActive: Boolean
)

data class RoomDto(
    val id: Long,
    val propertyId: Long,
    val name: String,
    val description: String,
    val pricePerNight: Double,
    val capacity: Int
)

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

data class BookingDto(
    val id: Long,
    val propertyId: Long,
    val roomId: Long,
    val guestId: Long? = null,
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

data class GuestSyncRequest(val items: List<GuestSyncItem>)

data class GuestSyncResult(
    val localId: Long,
    val serverId: Long?,
    val status: String,
    val error: String? = null
)

data class GuestSyncResponse(val results: List<GuestSyncResult>)

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

data class BookingSyncRequest(val items: List<BookingSyncItem>)

data class BookingSyncResultItem(
    val localId: Long,
    val serverId: Long?,
    val status: String,
    val bookingReference: String? = null,
    val error: String? = null
)

data class BookingSyncResponse(val results: List<BookingSyncResultItem>)
