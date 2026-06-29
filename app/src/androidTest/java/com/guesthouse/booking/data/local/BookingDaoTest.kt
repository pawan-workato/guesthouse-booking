package com.guesthouse.booking.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookingDao: BookingDao
    private var roomId: Long = 0L

    @Before
    fun setUp() = runTest {
        database = TestDatabase.createInMemoryDatabase()
        bookingDao = database.bookingDao()
        val propertyId = database.propertyDao().insert(
            PropertyEntity(name = "Mountain Lodge", address = "123 Main", region = "Colorado")
        )
        database.roomDao().insertAll(
            listOf(
                RoomEntity(propertyId = propertyId, name = "Room 1", description = "Double", pricePerNight = 120.0, capacity = 2)
            )
        )
        roomId = database.roomDao().getAll().first().id
        bookingDao.insert(
            BookingEntity(
                propertyId = propertyId,
                roomId = roomId,
                guestName = "Existing Guest",
                guestEmail = "existing@test.com",
                checkInEpochDay = 100L,
                checkOutEpochDay = 105L,
                status = BookingStatus.CONFIRMED.name
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun findOverlapping_detectsIntersectingStay() = runTest {
        val overlaps = bookingDao.findOverlapping(roomId, checkIn = 102L, checkOut = 107L)
        assertEquals(1, overlaps.size)
    }

    @Test
    fun findOverlapping_ignoresAdjacentCheckoutDay() = runTest {
        val overlaps = bookingDao.findOverlapping(roomId, checkIn = 105L, checkOut = 110L)
        assertTrue(overlaps.isEmpty())
    }

    @Test
    fun findOverlapping_excludesCancelledBookingsByDefault() = runTest {
        val propertyId = database.propertyDao().getAllIncludingInactive().first().id
        bookingDao.insert(
            BookingEntity(
                propertyId = propertyId,
                roomId = roomId,
                guestName = "Cancelled Guest",
                guestEmail = "",
                checkInEpochDay = 110L,
                checkOutEpochDay = 115L,
                status = BookingStatus.CANCELLED.name
            )
        )

        val overlaps = bookingDao.findOverlapping(roomId, checkIn = 111L, checkOut = 114L)
        assertTrue(overlaps.isEmpty())
    }

    @Test
    fun observeForGuestAtProperties_filtersByPropertyScope() = runTest {
        val propertyId = database.propertyDao().getAllIncludingInactive().first().id
        val guestId = database.guestDao().insert(
            GuestEntity(name = "History Guest", email = "h@test.com")
        )
        bookingDao.insert(
            BookingEntity(
                propertyId = propertyId,
                roomId = roomId,
                guestId = guestId,
                guestName = "History Guest",
                guestEmail = "h@test.com",
                checkInEpochDay = 200L,
                checkOutEpochDay = 203L
            )
        )
        val otherPropertyId = database.propertyDao().insert(
            PropertyEntity(name = "Other", address = "Elsewhere", region = "Coastal")
        )
        database.roomDao().insertAll(
            listOf(
                RoomEntity(
                    propertyId = otherPropertyId,
                    name = "Other Room",
                    description = "",
                    pricePerNight = 90.0,
                    capacity = 2
                )
            )
        )
        val otherRoomId = database.roomDao().getAll().first { it.propertyId == otherPropertyId }.id
        bookingDao.insert(
            BookingEntity(
                propertyId = otherPropertyId,
                roomId = otherRoomId,
                guestId = guestId,
                guestName = "History Guest",
                guestEmail = "h@test.com",
                checkInEpochDay = 300L,
                checkOutEpochDay = 302L
            )
        )

        val scoped = bookingDao.observeForGuestAtProperties(guestId, listOf(propertyId)).first()
        assertEquals(1, scoped.size)
        assertEquals(propertyId, scoped.first().propertyId)
    }
}
