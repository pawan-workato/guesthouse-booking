package com.guesthouse.booking.data.local.entities

enum class RoomType {
    SINGLE,
    DOUBLE,
    SUITE,
    FAMILY;

    fun displayLabel(): String =
        name.lowercase().replaceFirstChar { it.titlecase() }

    companion object {
        fun fromStored(value: String): RoomType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: DOUBLE

        fun inferFromName(name: String, capacity: Int = 2): RoomType {
            val lower = name.lowercase()
            return when {
                "single" in lower -> SINGLE
                "double" in lower -> DOUBLE
                "suite" in lower -> SUITE
                "family" in lower || "cottage" in lower -> FAMILY
                "den" in lower && capacity >= 4 -> FAMILY
                capacity <= 1 -> SINGLE
                capacity >= 4 -> FAMILY
                else -> DOUBLE
            }
        }
    }
}
