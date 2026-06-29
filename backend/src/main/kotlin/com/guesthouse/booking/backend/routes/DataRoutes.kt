package com.guesthouse.booking.backend.routes

import com.guesthouse.booking.backend.auth.StaffPrincipal
import com.guesthouse.booking.backend.db.Bookings
import com.guesthouse.booking.backend.db.Guests
import com.guesthouse.booking.backend.db.Properties
import com.guesthouse.booking.backend.db.Rooms
import com.guesthouse.booking.backend.model.BookingDto
import com.guesthouse.booking.backend.model.BookingSyncRequest
import com.guesthouse.booking.backend.model.BookingSyncResponse
import com.guesthouse.booking.backend.model.BookingSyncResultItem
import com.guesthouse.booking.backend.model.ErrorResponse
import com.guesthouse.booking.backend.model.GuestDto
import com.guesthouse.booking.backend.model.GuestSyncRequest
import com.guesthouse.booking.backend.model.GuestSyncResponse
import com.guesthouse.booking.backend.model.GuestSyncResult
import com.guesthouse.booking.backend.model.PropertyDto
import com.guesthouse.booking.backend.model.RoomDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Route.dataRoutes() {
    get("/api/properties") {
        val principal = call.principal<StaffPrincipal>()
        if (principal == null) {
            call.respondUnauthorized()
            return@get
        }
        val properties = transaction {
            Properties.selectAll().toList()
                .filter { it[Properties.isActive] }
                .map { it.toPropertyDto() }
                .filter { dto -> principal.canAccessProperty(dto.id) }
        }
        call.respond(properties)
    }

    get("/api/rooms") {
        val principal = call.principal<StaffPrincipal>()
        if (principal == null) {
            call.respondUnauthorized()
            return@get
        }
        val propertyId = call.request.queryParameters["propertyId"]?.toLongOrNull()
        val rooms = transaction {
            Rooms.selectAll().toList()
                .filter { row ->
                    val pid = row[Rooms.propertyId].value
                    (propertyId == null || pid == propertyId) && principal.canAccessProperty(pid)
                }
                .map { it.toRoomDto() }
        }
        call.respond(rooms)
    }

    get("/api/guests") {
        if (call.principal<StaffPrincipal>() == null) {
            call.respondUnauthorized()
            return@get
        }
        val guests = transaction {
            Guests.selectAll().toList()
                .filter { it[Guests.isActive] }
                .map { it.toGuestDto() }
        }
        call.respond(guests)
    }

    get("/api/bookings") {
        val principal = call.principal<StaffPrincipal>()
        if (principal == null) {
            call.respondUnauthorized()
            return@get
        }
        val propertyId = call.request.queryParameters["propertyId"]?.toLongOrNull()
        val bookings = transaction {
            Bookings.selectAll().toList()
                .filter { row ->
                    val pid = row[Bookings.propertyId].value
                    (propertyId == null || pid == propertyId) && principal.canAccessProperty(pid)
                }
                .map { it.toBookingDto() }
        }
        call.respond(bookings)
    }

    post("/api/guests/sync") {
        if (call.principal<StaffPrincipal>() == null) {
            call.respondUnauthorized()
            return@post
        }
        val request = call.receive<GuestSyncRequest>()
        val results = transaction {
            request.items.map { item ->
                runCatching {
                    val now = System.currentTimeMillis()
                    val updatedAt = if (item.updatedAtEpochMs > 0) item.updatedAtEpochMs else now
                    val serverId = item.serverId
                    if (serverId != null) {
                        val existing = Guests.selectAll().toList()
                            .filter { it[Guests.id].value == serverId }
                            .firstOrNull()
                        if (existing == null) {
                            GuestSyncResult(item.localId, null, "NOT_FOUND", "Guest not found on server")
                        } else {
                            Guests.update({ Guests.id eq serverId }) {
                                it[name] = item.name
                                it[email] = item.email
                                it[phone] = item.phone
                                it[notes] = item.notes
                                it[isActive] = item.isActive
                                it[updatedAtEpochMs] = updatedAt
                            }
                            GuestSyncResult(item.localId, serverId, "SYNCED")
                        }
                    } else {
                        val inserted = Guests.insert {
                            it[name] = item.name
                            it[email] = item.email
                            it[phone] = item.phone
                            it[notes] = item.notes
                            it[isActive] = item.isActive
                            it[createdAtEpochMs] = now
                            it[updatedAtEpochMs] = updatedAt
                        }
                        GuestSyncResult(item.localId, inserted[Guests.id].value, "SYNCED")
                    }
                }.getOrElse { e ->
                    GuestSyncResult(item.localId, item.serverId, "ERROR", e.message ?: "Sync failed")
                }
            }
        }
        call.respond(GuestSyncResponse(results))
    }

    post("/api/bookings/sync") {
        val principal = call.principal<StaffPrincipal>()
        if (principal == null) {
            call.respondUnauthorized()
            return@post
        }
        val request = call.receive<BookingSyncRequest>()
        val results = transaction {
            request.items.map { item ->
                if (!principal.canAccessProperty(item.propertyId)) {
                    return@map BookingSyncResultItem(
                        localId = item.localId,
                        serverId = item.serverId,
                        status = "FORBIDDEN",
                        error = "No access to property ${item.propertyId}"
                    )
                }
                runCatching {
                    val excludeId = item.serverId ?: -1L
                    val overlaps = Bookings.selectAll().toList().filter { row ->
                        row[Bookings.roomId].value == item.roomId &&
                            row[Bookings.status] != "CANCELLED" &&
                            row[Bookings.id].value != excludeId &&
                            row[Bookings.checkInEpochDay] < item.checkOutEpochDay &&
                            item.checkInEpochDay < row[Bookings.checkOutEpochDay]
                    }
                    if (overlaps.isNotEmpty()) {
                        BookingSyncResultItem(item.localId, item.serverId, "CONFLICT", error = "Overlapping booking")
                    } else {
                        val now = System.currentTimeMillis()
                        val updatedAt = if (item.updatedAtEpochMs > 0) item.updatedAtEpochMs else now
                        if (item.serverId != null) {
                            val serverId = item.serverId
                            Bookings.update({ Bookings.id eq serverId }) {
                                it[propertyId] = EntityID(item.propertyId, Properties)
                                it[roomId] = EntityID(item.roomId, Rooms)
                                it[guestId] = item.guestId?.let { gid -> EntityID(gid, Guests) }
                                it[guestName] = item.guestName
                                it[guestEmail] = item.guestEmail
                                it[guestPhone] = item.guestPhone
                                it[checkInEpochDay] = item.checkInEpochDay
                                it[checkOutEpochDay] = item.checkOutEpochDay
                                it[status] = item.status
                                it[syncStatus] = "SYNCED"
                                it[updatedAtEpochMs] = updatedAt
                            }
                            val reference = formatReference(item.propertyId, serverId)
                            Bookings.update({ Bookings.id eq serverId }) {
                                it[bookingReference] = reference
                            }
                            BookingSyncResultItem(item.localId, serverId, "SYNCED", reference)
                        } else {
                            val inserted = Bookings.insert {
                                it[propertyId] = EntityID(item.propertyId, Properties)
                                it[roomId] = EntityID(item.roomId, Rooms)
                                it[guestId] = item.guestId?.let { gid -> EntityID(gid, Guests) }
                                it[guestName] = item.guestName
                                it[guestEmail] = item.guestEmail
                                it[guestPhone] = item.guestPhone
                                it[checkInEpochDay] = item.checkInEpochDay
                                it[checkOutEpochDay] = item.checkOutEpochDay
                                it[status] = item.status
                                it[syncStatus] = "SYNCED"
                                it[createdAtEpochMs] = now
                                it[updatedAtEpochMs] = updatedAt
                            }
                            val newId = inserted[Bookings.id].value
                            val reference = formatReference(item.propertyId, newId)
                            Bookings.update({ Bookings.id eq newId }) {
                                it[bookingReference] = reference
                            }
                            BookingSyncResultItem(item.localId, newId, "SYNCED", reference)
                        }
                    }
                }.getOrElse { e ->
                    BookingSyncResultItem(item.localId, item.serverId, "ERROR", error = e.message ?: "Sync failed")
                }
            }
        }
        call.respond(BookingSyncResponse(results))
    }
}

private suspend fun ApplicationCall.respondUnauthorized() {
    respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized"))
}

private fun StaffPrincipal.canAccessProperty(propertyId: Long): Boolean =
    role == "CHAIN_ADMIN" || propertyId in assignedPropertyIds

private fun formatReference(propertyId: Long, bookingId: Long): String =
    "GH-$propertyId-${bookingId.toString().padStart(4, '0')}"

private fun ResultRow.toPropertyDto() = PropertyDto(
    id = this[Properties.id].value,
    name = this[Properties.name],
    address = this[Properties.address],
    region = this[Properties.region],
    checkInTime = this[Properties.checkInTime],
    checkOutTime = this[Properties.checkOutTime],
    isActive = this[Properties.isActive]
)

private fun ResultRow.toRoomDto() = RoomDto(
    id = this[Rooms.id].value,
    propertyId = this[Rooms.propertyId].value,
    name = this[Rooms.name],
    description = this[Rooms.description],
    pricePerNight = this[Rooms.pricePerNight],
    capacity = this[Rooms.capacity],
    roomType = this[Rooms.roomType]
)

private fun ResultRow.toGuestDto() = GuestDto(
    id = this[Guests.id].value,
    name = this[Guests.name],
    email = this[Guests.email],
    phone = this[Guests.phone],
    notes = this[Guests.notes],
    isActive = this[Guests.isActive],
    createdAtEpochMs = this[Guests.createdAtEpochMs],
    updatedAtEpochMs = this[Guests.updatedAtEpochMs]
)

private fun ResultRow.toBookingDto() = BookingDto(
    id = this[Bookings.id].value,
    propertyId = this[Bookings.propertyId].value,
    roomId = this[Bookings.roomId].value,
    guestId = this[Bookings.guestId]?.value,
    guestName = this[Bookings.guestName],
    guestEmail = this[Bookings.guestEmail],
    guestPhone = this[Bookings.guestPhone],
    checkInEpochDay = this[Bookings.checkInEpochDay],
    checkOutEpochDay = this[Bookings.checkOutEpochDay],
    status = this[Bookings.status],
    syncStatus = this[Bookings.syncStatus],
    bookingReference = this[Bookings.bookingReference],
    createdAtEpochMs = this[Bookings.createdAtEpochMs],
    updatedAtEpochMs = this[Bookings.updatedAtEpochMs]
)
