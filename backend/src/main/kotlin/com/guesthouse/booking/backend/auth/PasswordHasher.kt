package com.guesthouse.booking.backend.auth

import java.security.MessageDigest

object PasswordHasher {
    private const val SALT = "guesthouse-chain-v1"

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$SALT:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String): Boolean = hash(password) == hash
}
