package com.guesthouse.booking.data.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import java.security.MessageDigest

object PasswordHasher {
    private const val BCRYPT_COST = 12
    private const val LEGACY_SALT = "guesthouse-chain-v1"

    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.startsWith("$2")) {
            return BCrypt.verifyer().verify(password.toCharArray(), storedHash.toCharArray()).verified
        }
        return legacySha256Hash(password) == storedHash
    }

    fun needsUpgrade(storedHash: String): Boolean = !storedHash.startsWith("$2")

    private fun legacySha256Hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$LEGACY_SALT:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
