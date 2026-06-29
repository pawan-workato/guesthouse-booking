package com.guesthouse.booking.data.repository

data class PropertyOccupancyStats(
    val propertyId: Long,
    val propertyName: String,
    val totalRooms: Int,
    val occupiedTonight: Int,
    val arrivalsToday: Int,
    val departuresToday: Int,
    val blockedRooms: Int,
    val vacant: Int
)

data class ChainOccupancyTotals(
    val totalRooms: Int,
    val occupiedTonight: Int,
    val arrivalsToday: Int,
    val departuresToday: Int,
    val blockedRooms: Int,
    val vacant: Int
) {
    companion object {
        fun from(stats: List<PropertyOccupancyStats>): ChainOccupancyTotals = ChainOccupancyTotals(
            totalRooms = stats.sumOf { it.totalRooms },
            occupiedTonight = stats.sumOf { it.occupiedTonight },
            arrivalsToday = stats.sumOf { it.arrivalsToday },
            departuresToday = stats.sumOf { it.departuresToday },
            blockedRooms = stats.sumOf { it.blockedRooms },
            vacant = stats.sumOf { it.vacant }
        )
    }
}
