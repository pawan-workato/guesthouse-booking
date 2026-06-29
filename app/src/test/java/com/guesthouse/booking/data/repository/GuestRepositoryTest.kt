package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.BookingDao
import com.guesthouse.booking.data.local.GuestDao
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.sync.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private val authRepository = mockk<AuthRepository>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)

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
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { firestore.isSignedIn } returns false
        every { authRepository.currentSession() } returns managerSession
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
    fun canEditGuest_deniesManagerForGuestOutsideAssignedProperties() = runTest {
        coEvery { bookingDao.getGuestIdsForProperties(listOf(1L, 3L)) } returns listOf(10L)

        assertFalse(repository.canEditGuest(99L))
    }

    @Test
    fun canEditGuest_allowsManagerForGuestLinkedToAssignedProperty() = runTest {
        coEvery { bookingDao.getGuestIdsForProperties(listOf(1L, 3L)) } returns listOf(10L)

        assertTrue(repository.canEditGuest(10L))
    }

    @Test
    fun canAccessGuest_delegatesToCanEditGuest() = runTest {
        coEvery { bookingDao.getGuestIdsForProperties(listOf(1L, 3L)) } returns listOf(10L)

        assertFalse(repository.canAccessGuest(99L))
        assertTrue(repository.canAccessGuest(10L))
    }

    @Test
    fun updateGuest_rejectsWhenManagerCannotAccessGuest() = runTest {
        coEvery { bookingDao.getGuestIdsForProperties(listOf(1L, 3L)) } returns listOf(10L)
        val guest = GuestEntity(id = 99L, name = "Bob", email = "bob@test.com")

        val result = repository.updateGuest(guest)

        assertTrue(result.isFailure)
        assertEquals("You don't have access to this guest", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { guestDao.update(any()) }
    }

    @Test
    fun updateGuest_trimsFieldsAndPersistsWhenAllowed() = runTest {
        coEvery { bookingDao.getGuestIdsForProperties(listOf(1L, 3L)) } returns listOf(1L)
        val guest = GuestEntity(id = 1L, name = " Bob ", email = " bob@test.com ", phone = " 555 ", notes = " note ")

        val result = repository.updateGuest(guest)

        assertTrue(result.isSuccess)
        coVerify {
            guestDao.update(
                match {
                    it.id == 1L &&
                        it.name == "Bob" &&
                        it.email == "bob@test.com" &&
                        it.phone == "555" &&
                        it.notes == "note"
                }
            )
        }
    }
}
