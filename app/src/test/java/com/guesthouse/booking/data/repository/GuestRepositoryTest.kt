package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.GuestDao
import com.guesthouse.booking.data.local.entities.GuestEntity
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

class GuestRepositoryTest {

    private val database = mockk<AppDatabase>()
    private val guestDao = mockk<GuestDao>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)

    private lateinit var repository: GuestRepository

    @Before
    fun setUp() {
        every { database.guestDao() } returns guestDao
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { firestore.isSignedIn } returns false
        every { syncRepository.enqueueSyncWorker() } returns Unit
        repository = GuestRepository(
            database,
            networkMonitor,
            firestore,
            lazy { syncRepository }
        )
    }

    @Test
    fun createGuest_rejectsBlankName() = runTest {
        val result = repository.createGuest("   ", "a@b.com", "555", "notes")

        assertTrue(result.isFailure)
        assertEquals("Guest name is required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { guestDao.insert(any()) }
    }

    @Test
    fun createGuest_trimsFieldsAndPersists() = runTest {
        coEvery { guestDao.insert(any()) } returns 7L

        val result = repository.createGuest("  Alice  ", " alice@test.com ", " 555-0100 ", " VIP ")

        assertTrue(result.isSuccess)
        assertEquals(7L, result.getOrNull())
        coVerify {
            guestDao.insert(
                match {
                    it.name == "Alice" &&
                        it.email == "alice@test.com" &&
                        it.phone == "555-0100" &&
                        it.notes == "VIP" &&
                        it.isActive
                }
            )
        }
    }

    @Test
    fun updateGuest_rejectsBlankName() = runTest {
        val guest = GuestEntity(id = 1L, name = "   ", email = "a@b.com")

        val result = repository.updateGuest(guest)

        assertTrue(result.isFailure)
        assertEquals("Guest name is required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { guestDao.update(any()) }
    }

    @Test
    fun updateGuest_trimsFieldsAndPersists() = runTest {
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
