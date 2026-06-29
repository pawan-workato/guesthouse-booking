package com.guesthouse.booking.data.guest

import com.guesthouse.booking.data.local.entities.GuestEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestMatchingTest {

    private val guest = GuestEntity(
        id = 1L,
        name = "Jane Guest",
        email = "jane@example.com",
        phone = "+1 (555) 123-4567"
    )

    @Test
    fun matches_nameSubstring() {
        assertTrue(GuestMatching.matches(guest, "jane", "", ""))
    }

    @Test
    fun matches_exactEmail() {
        assertTrue(GuestMatching.matches(guest, "", "JANE@Example.com", ""))
    }

    @Test
    fun matches_phoneDigits() {
        assertTrue(GuestMatching.matches(guest, "", "", "5551234567"))
    }

    @Test
    fun noMatch_unrelatedGuest() {
        assertFalse(GuestMatching.matches(guest, "Bob", "bob@example.com", "9999999999"))
    }
}
