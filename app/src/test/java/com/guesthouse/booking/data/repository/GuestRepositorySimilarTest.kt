package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.GuestDao
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.sync.NetworkMonitor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GuestRepositorySimilarTest {

    private val database = mockk<AppDatabase>()
    private val guestDao = mockk<GuestDao>()
    private val authRepository = mockk<AuthRepository>()
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)

    private lateinit var repository: GuestRepository

    @Before
    fun setUp() {
        every { database.guestDao() } returns guestDao
        every { authRepository.currentSession() } returns StaffSession(
            1L, "admin@chain.com", "Admin", StaffRole.CHAIN_ADMIN, emptySet()
        )
        repository = GuestRepository(database, authRepository, networkMonitor, firestore)
    }

    @Test
    fun findSimilarGuests_returnsMatchingActiveGuests() = runTest {
        val guests = listOf(
            GuestEntity(id = 1L, name = "Jane Guest", email = "jane@example.com", phone = ""),
            GuestEntity(id = 2L, name = "Bob Smith", email = "bob@example.com", phone = "")
        )
        coEvery { guestDao.getAllActive() } returns guests

        val matches = repository.findSimilarGuests("jane", "", "")

        assertEquals(1, matches.size)
        assertEquals(1L, matches.first().id)
    }
}
