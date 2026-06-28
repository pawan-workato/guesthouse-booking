package com.guesthouse.booking.backend.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/guesthouse"
        val user = System.getenv("DATABASE_USER") ?: "guesthouse"
        val password = System.getenv("DATABASE_PASSWORD") ?: "guesthouse"

        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )

        transaction {
            SchemaUtils.create(
                Properties,
                Rooms,
                Guests,
                Bookings,
                Staff,
                StaffPropertyAssignments
            )
        }
    }
}
