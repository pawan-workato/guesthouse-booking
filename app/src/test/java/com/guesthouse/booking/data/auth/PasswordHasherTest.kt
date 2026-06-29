package com.guesthouse.booking.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun hash_usesBcryptFormat() {
        val hash = PasswordHasher.hash("manager123")
        assertTrue(hash.startsWith("$2"))
    }

    @Test
    fun hash_producesDifferentValuesForSamePassword() {
        val first = PasswordHasher.hash("manager123")
        val second = PasswordHasher.hash("manager123")
        assertNotEquals(first, second)
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

    @Test
    fun verify_acceptsLegacySha256Hash() {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val legacy = digest.digest("guesthouse-chain-v1:admin123".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertTrue(PasswordHasher.verify("admin123", legacy))
    }

    @Test
    fun needsUpgrade_detectsLegacyHash() {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val legacy = digest.digest("guesthouse-chain-v1:admin123".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertTrue(PasswordHasher.needsUpgrade(legacy))
        assertFalse(PasswordHasher.needsUpgrade(PasswordHasher.hash("admin123")))
    }
}
