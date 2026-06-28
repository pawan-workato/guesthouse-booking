package com.guesthouse.booking.data.auth

import com.guesthouse.booking.data.local.entities.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffSessionTest {

    private val managerSession = StaffSession(
        staffId = 2L,
        email = "manager.mountain@chain.com",
        displayName = "Mountain Manager",
        role = StaffRole.PROPERTY_MANAGER,
        assignedPropertyIds = setOf(1L, 3L)
    )

    private val adminSession = StaffSession(
        staffId = 1L,
        email = "admin@chain.com",
        displayName = "Chain Admin",
        role = StaffRole.CHAIN_ADMIN,
        assignedPropertyIds = emptySet()
    )

    @Test
    fun chainAdmin_canAccessAnyProperty() {
        assertTrue(adminSession.canAccessProperty(1L))
        assertTrue(adminSession.canAccessProperty(99L))
    }

    @Test
    fun propertyManager_canAccessOnlyAssignedProperties() {
        assertTrue(managerSession.canAccessProperty(1L))
        assertTrue(managerSession.canAccessProperty(3L))
        assertFalse(managerSession.canAccessProperty(2L))
    }

    @Test
    fun filterPropertyIds_returnsAllForAdmin() {
        val allIds = listOf(1L, 2L, 3L, 4L)
        assertEquals(setOf(1L, 2L, 3L, 4L), adminSession.filterPropertyIds(allIds))
    }

    @Test
    fun filterPropertyIds_returnsIntersectionForManager() {
        val allIds = listOf(1L, 2L, 3L, 4L)
        assertEquals(setOf(1L, 3L), managerSession.filterPropertyIds(allIds))
    }
}
