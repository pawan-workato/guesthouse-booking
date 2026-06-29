package com.guesthouse.booking.data.local.entities

enum class BookingSource {
    WALK_IN,
    PHONE,
    EMAIL,
    OTA,
    REPEAT;

    val label: String
        get() = when (this) {
            WALK_IN -> "Walk-in"
            PHONE -> "Phone"
            EMAIL -> "Email"
            OTA -> "Online travel"
            REPEAT -> "Repeat guest"
        }
}
