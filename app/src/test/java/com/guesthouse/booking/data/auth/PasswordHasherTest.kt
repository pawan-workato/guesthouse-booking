package com.guesthouse.booking.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hash_isDeterministicForSamePassword() {
        val first = PasswordHasher.hash("manager123")
        val second = PasswordHasher.hash("manager123")
        assertEquals(first, second)
    }

    @Test
    fun hash_producesDifferentValuesForDifferentPasswords() {
        val adminHash = PasswordHasher.hash("admin123")
        val managerHash = PasswordHasher.hash("manager123")
        assertNotEquals(adminHash, managerHash)
    }

    @Test
    fun verify_returnsTrueForMatchingPassword() {
        val hash = PasswordHasher.hash("admin123")
        assertTrue(PasswordHasher.verify("admin123", hash))
    }

    @Test
    fun verify_returnsFalseForWrongPassword() {
        val hash = PasswordHasher.hash("admin123")
        assertFalse(PasswordHasher.verify("wrong-password", hash))
    }
}
