package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.BookingDao
import com.guesthouse.booking.data.local.GuestDao
import com.guesthouse.booking.data.local.PropertyDao
import com.guesthouse.booking.data.local.RoomDao
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.sync.NetworkMonitor
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuestRepositoryTest {

    private val database = mockk<AppDatabase>()
    private val guestDao = mockk<GuestDao>(relaxed = true)
    private val bookingDao = mockk<BookingDao>(relaxed = true)
    private val propertyDao = mockk<PropertyDao>(relaxed = true)
    private val roomDao = mockk<RoomDao>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    private lateinit var repository: GuestRepository

    private val managerSession = StaffSession(
        staffId = 2L,
        email = "manager@test.com",
        displayName = "Manager",
        role = StaffRole.PROPERTY_MANAGER,
        assignedPropertyIds = setOf(1L, 3L)
    )

    @Before
    fun setUp() {
        every { database.guestDao() } returns guestDao
        every { database.bookingDao() } returns bookingDao
        every { database.propertyDao() } returns propertyDao
        every { database.roomDao() } returns roomDao
        every { authRepository.session } returns sessionFlow
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { firestore.isSignedIn } returns false
        every { authRepository.currentSession() } returns managerSession
        sessionFlow.value = managerSession
        repository = GuestRepository(
            database,
            authRepository,
            networkMonitor,
            firestore
        )
    }

    @Test
    fun createGuest_rejectsWhenNotSignedIn() = runTest {
        every { authRepository.currentSession() } returns null

        val result = repository.createGuest("Alice", "a@b.com", "555", "notes")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { guestDao.insert(any()) }
    }

    @Test
    fun createGuest_rejectsBlankName() = runTest {
        val result = repository.createGuest("   ", "a@b.com", "555", "notes")

        assertTrue(result.isFailure)
        assertEquals("Guest name is required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { guestDao.insert(any()) }
    }

    @Test
    fun canViewGuest_allowsManagerForAnyExistingGuest() = runTest {
        coEvery { guestDao.getById(99L) } returns GuestEntity(id = 99L, name = "Bob")

        assertTrue(repository.canViewGuest(99L))
    }

    @Test
    fun canViewGuest_deniesWhenGuestMissing() = runTest {
        coEvery { guestDao.getById(99L) } returns null

        assertFalse(repository.canViewGuest(99L))
    }

    @Test
    fun canEditGuest_allowsManagerForAnyExistingGuest() = runTest {
        coEvery { guestDao.getById(99L) } returns GuestEntity(id = 99L, name = "Bob")

        assertTrue(repository.canEditGuest(99L))
    }

    @Test
    fun canDeleteGuest_chainAdminOnly() = runTest {
        coEvery { guestDao.getById(10L) } returns GuestEntity(id = 10L, name = "Bob")

        assertFalse(repository.canDeleteGuest(10L))

        val adminSession = managerSession.copy(role = StaffRole.CHAIN_ADMIN)
        every { authRepository.currentSession() } returns adminSession
        assertTrue(repository.canDeleteGuest(10L))
    }

    @Test
    fun canAccessGuest_matchesCanViewGuest() = runTest {
        coEvery { guestDao.getById(99L) } returns null
        coEvery { guestDao.getById(10L) } returns GuestEntity(id = 10L, name = "Bob")

        assertFalse(repository.canAccessGuest(99L))
        assertTrue(repository.canAccessGuest(10L))
    }

    @Test
    fun updateGuest_allowsManagerForAnyGuest() = runTest {
        coEvery { guestDao.getById(99L) } returns GuestEntity(id = 99L, name = "Bob")
        val guest = GuestEntity(id = 99L, name = " Bob ", email = " bob@test.com ", phone = " 555 ", notes = " note ")

        val result = repository.updateGuest(guest)

        assertTrue(result.isSuccess)
        coVerify { guestDao.update(any()) }
    }

    @Test
    fun setGuestActive_deniesManager() = runTest {
        coEvery { guestDao.getById(10L) } returns GuestEntity(id = 10L, name = "Bob", isActive = true)

        repository.setGuestActive(10L, false)

        coVerify(exactly = 0) { guestDao.update(any()) }
    }

    @Test
    fun observeGuestStayHistory_managerSeesOnlyAssignedPropertyBookings() = runTest {
        val bookingAtAssigned = BookingEntity(
            id = 1L,
            propertyId = 1L,
            roomId = 10L,
            guestId = 5L,
            guestName = "Alice",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 102L,
            status = BookingStatus.CONFIRMED.name
        )
        every { bookingDao.observeForGuestAtProperties(5L, listOf(1L, 3L)) } returns flowOf(listOf(bookingAtAssigned))
        every { propertyDao.observeAll() } returns flowOf(listOf(PropertyEntity(id = 1L, name = "Hill View", address = "", region = "")))
        every { roomDao.observeAll() } returns flowOf(listOf(RoomEntity(id = 10L, propertyId = 1L, name = "Room A", description = "", pricePerNight = 100.0, capacity = 2, roomType = "DOUBLE")))

        repository.observeGuestStayHistory(5L).test {
            val history = awaitItem()
            assertEquals(1, history.size)
            assertEquals("Hill View", history.first().propertyName)
            assertEquals("Room A", history.first().roomName)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { bookingDao.observeForGuest(any()) }
    }

    @Test
    fun observeGuestStayHistory_chainAdminSeesAllPropertyBookings() = runTest {
        val adminSession = managerSession.copy(role = StaffRole.CHAIN_ADMIN, assignedPropertyIds = emptySet())
        sessionFlow.value = adminSession
        every { authRepository.currentSession() } returns adminSession
        val booking = BookingEntity(
            id = 2L,
            propertyId = 99L,
            roomId = 20L,
            guestId = 5L,
            guestName = "Alice",
            guestEmail = "",
            checkInEpochDay = 200L,
            checkOutEpochDay = 203L
        )
        every { bookingDao.observeForGuest(5L) } returns flowOf(listOf(booking))
        every { propertyDao.observeAll() } returns flowOf(listOf(PropertyEntity(id = 99L, name = "Remote Lodge", address = "", region = "")))
        every { roomDao.observeAll() } returns flowOf(listOf(RoomEntity(id = 20L, propertyId = 99L, name = "Suite", description = "", pricePerNight = 200.0, capacity = 3, roomType = "SUITE")))

        repository.observeGuestStayHistory(5L).test {
            val history = awaitItem()
            assertEquals(1, history.size)
            assertEquals("Remote Lodge", history.first().propertyName)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { bookingDao.observeForGuestAtProperties(any(), any()) }
    }
}
