package com.guesthouse.booking.data.guest

import com.guesthouse.booking.data.local.entities.GuestEntity

object GuestMatching {
    fun normalizePhone(phone: String): String = phone.filter { it.isDigit() }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun matches(guest: GuestEntity, name: String, email: String, phone: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.length >= 2 && guest.name.contains(trimmedName, ignoreCase = true)) {
            return true
        }

        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail.isNotBlank()) {
            val guestEmail = normalizeEmail(guest.email)
            if (guestEmail.isNotBlank() && guestEmail == normalizedEmail) {
                return true
            }
        }

        val normalizedPhone = normalizePhone(phone)
        if (normalizedPhone.length >= 7) {
            val guestPhone = normalizePhone(guest.phone)
            if (guestPhone.isNotBlank() &&
                (guestPhone == normalizedPhone ||
                    guestPhone.endsWith(normalizedPhone) ||
                    normalizedPhone.endsWith(guestPhone))
            ) {
                return true
            }
        }
        return false
    }
}
