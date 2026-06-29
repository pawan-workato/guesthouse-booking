package com.guesthouse.booking.testutil

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.StaffRole

object UiTestSessions {
    val manager = StaffSession(
        staffId = 2L,
        email = "manager.mountain@chain.com",
        displayName = "Alex Mountain",
        role = StaffRole.PROPERTY_MANAGER,
        assignedPropertyIds = setOf(1L)
    )

    val admin = StaffSession(
        staffId = 1L,
        email = "admin@chain.com",
        displayName = "Chain Admin",
        role = StaffRole.CHAIN_ADMIN,
        assignedPropertyIds = emptySet()
    )
}
