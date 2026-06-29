package com.guesthouse.booking.data.firebase

data class PullRemoteDataResult(
    val staffCount: Int = 0,
    val propertiesCount: Int = 0,
    val roomsCount: Int = 0,
    val guestsCount: Int = 0,
    val bookingsCount: Int = 0,
    val errors: List<String> = emptyList()
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val hasData: Boolean get() =
        staffCount + propertiesCount + roomsCount + guestsCount + bookingsCount > 0
}
