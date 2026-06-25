package com.guesthouse.booking.data.auth

import com.guesthouse.booking.data.local.entities.StaffRole

data class StaffSession(
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: StaffRole,
    val assignedPropertyIds: Set<Long>
) {
    val isChainAdmin: Boolean get() = role == StaffRole.CHAIN_ADMIN

    fun canAccessProperty(propertyId: Long): Boolean =
        isChainAdmin || propertyId in assignedPropertyIds

    fun filterPropertyIds(allPropertyIds: Collection<Long>): Set<Long> =
        if (isChainAdmin) allPropertyIds.toSet() else assignedPropertyIds.intersect(allPropertyIds.toSet())
}
