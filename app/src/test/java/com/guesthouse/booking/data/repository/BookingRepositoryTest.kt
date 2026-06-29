package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.BlockDateDao
import com.guesthouse.booking.data.local.BookingDao
import com.guesthouse.booking.data.local.RoomDao
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookingRepositoryTest {

    private val database = mockk<AppDatabase>()
    private val bookingDao = mockk<BookingDao>(relaxed = true)
    private val blockDateDao = mockk<BlockDateDao>(relaxed = true)
    private val roomDao = mockk<RoomDao>()
    private val authRepository = mockk<AuthRepository>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)

    private lateinit var repository: BookingRepository

    private val room = RoomEntity(
        id = 10L,
        propertyId = 1L,
        name = "Room A",
        description = "Double",
        pricePerNight = 120.0,
        capacity = 2
    )

    @Before
    fun setUp() {
        every { database.bookingDao() } returns bookingDao
        every { database.blockDateDao() } returns blockDateDao
        every { database.roomDao() } returns roomDao
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { firestore.isSignedIn } returns false
        repository = BookingRepository(database, authRepository, networkMonitor, firestore, lazy { syncRepository })
    }

    @Test
    fun createBooking_rejectsBlankGuestName() = runTest {
        val result = repository.createBooking(
            roomId = 10L,
            guestId = null,
            guestName = "   ",
            guestEmail = "guest@example.com",
            guestPhone = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L,
            isOnline = false
        )

        assertTrue(result.isFailure)
        assertEquals("Guest name is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun createBooking_rejectsCheckOutOnOrBeforeCheckIn() = runTest {
        val result = repository.createBooking(
            roomId = 10L,
            guestId = null,
            guestName = "Jane Doe",
            guestEmail = "",
            guestPhone = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 100L,
            isOnline = false
        )

        assertTrue(result.isFailure)
        assertEquals("Check-out must be after check-in", result.exceptionOrNull()?.message)
    }

    @Test
    fun createBooking_rejectsOverlappingDates() = runTest {
        coEvery { roomDao.getById(10L) } returns room
        coEvery { bookingDao.findOverlapping(10L, 100L, 105L) } returns listOf(
            BookingEntity(
                id = 99L,
                propertyId = 1L,
                roomId = 10L,
                guestName = "Existing Guest",
                guestEmail = "",
                checkInEpochDay = 98L,
                checkOutEpochDay = 102L
            )
        )

        val result = repository.createBooking(
            roomId = 10L,
            guestId = null,
            guestName = "Jane Doe",
            guestEmail = "",
            guestPhone = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L,
            isOnline = false
        )

        assertTrue(result.isFailure)
        assertEquals("Room is not available for those dates", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { bookingDao.insert(any()) }
    }

    @Test
    fun createBooking_succeedsWhenRoomIsAvailable() = runTest {
        coEvery { roomDao.getById(10L) } returns room
        coEvery { bookingDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { blockDateDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { bookingDao.insert(any()) } returns 42L

        val result = repository.createBooking(
            roomId = 10L,
            guestId = null,
            guestName = "Jane Doe",
            guestEmail = "jane@example.com",
            guestPhone = "555-0100",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L,
            isOnline = false
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull()?.bookingId)
        assertTrue(result.getOrNull()?.savedOffline == true)
        coVerify { bookingDao.insert(match { it.guestName == "Jane Doe" && it.roomId == 10L }) }
    }

    @Test
    fun cancelBooking_doesNotUpdateWhenPropertyOutOfScope() = runTest {
        val booking = BookingEntity(
            id = 5L,
            propertyId = 99L,
            roomId = 10L,
            guestName = "Jane Doe",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L
        )
        every { authRepository.currentSession() } returns StaffSession(
            staffId = 2L,
            email = "manager.mountain@chain.com",
            displayName = "Mountain Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L)
        )
        coEvery { bookingDao.getById(5L) } returns booking

        repository.cancelBooking(5L)

        coVerify(exactly = 0) { bookingDao.updateStatus(any(), BookingStatus.CANCELLED.name) }
    }

    @Test
    fun cancelBooking_updatesStatusWhenPropertyInScope() = runTest {
        val booking = BookingEntity(
            id = 5L,
            propertyId = 1L,
            roomId = 10L,
            guestName = "Jane Doe",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L
        )
        every { authRepository.currentSession() } returns StaffSession(
            staffId = 2L,
            email = "manager.mountain@chain.com",
            displayName = "Mountain Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L)
        )
        coEvery { bookingDao.getById(5L) } returns booking

        repository.cancelBooking(5L)

        coVerify { bookingDao.updateStatus(5L, BookingStatus.CANCELLED.name) }
    }
}
