package com.guesthouse.booking.data.local.entities

enum class HousekeepingStatus {
    CLEAN,
    DIRTY,
    IN_PROGRESS,
    INSPECTED;

    val label: String
        get() = when (this) {
            CLEAN -> "Clean"
            DIRTY -> "Dirty"
            IN_PROGRESS -> "In progress"
            INSPECTED -> "Inspected"
        }
}
