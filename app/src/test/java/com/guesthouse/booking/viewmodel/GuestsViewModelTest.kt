package com.guesthouse.booking.viewmodel

import app.cash.turbine.test
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val guestRepository = mockk<GuestRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    private val guests = listOf(
        GuestEntity(id = 1L, name = "Alice Anderson", email = "alice@test.com", phone = "555-0101"),
        GuestEntity(id = 2L, name = "Bob Baker", email = "bob@test.com", phone = "555-0102"),
        GuestEntity(id = 3L, name = "Carol Chen", email = "carol@test.com", phone = "555-0103")
    )

    @Test
    fun guests_filtersBySearchQueryAcrossNameEmailAndPhone() = runTest {
        every { authRepository.session } returns sessionFlow
        every { guestRepository.observeScopedActiveGuests() } returns flowOf(guests)
        every { guestRepository.observeScopedAllGuests() } returns flowOf(guests)
        every { guestRepository.observeGuestStayHistory(any()) } returns flowOf(emptyList())

        val viewModel = GuestsViewModel(guestRepository, authRepository)
        advanceUntilIdle()
        viewModel.setSearchQuery("555-0102")
        advanceUntilIdle()

        viewModel.guests.test {
            skipItems(1)
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("Bob Baker", filtered.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guests_searchIsCaseInsensitive() = runTest {
        every { authRepository.session } returns sessionFlow
        every { guestRepository.observeScopedActiveGuests() } returns flowOf(guests)
        every { guestRepository.observeScopedAllGuests() } returns flowOf(guests)
        every { guestRepository.observeGuestStayHistory(any()) } returns flowOf(emptyList())

        val viewModel = GuestsViewModel(guestRepository, authRepository)
        advanceUntilIdle()
        viewModel.setSearchQuery("carol")
        advanceUntilIdle()

        viewModel.guests.test {
            skipItems(1)
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertTrue(filtered.first().name.contains("Carol"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadGuestForEdit_allowsManagerToEditAnyGuest() = runTest {
        val guest = guests.first()
        every { authRepository.session } returns sessionFlow
        coEvery { guestRepository.getGuest(guest.id) } returns guest
        coEvery { guestRepository.canEditGuest(guest.id) } returns true
        every { guestRepository.observeScopedActiveGuests() } returns flowOf(guests)
        every { guestRepository.observeScopedAllGuests() } returns flowOf(guests)
        every { guestRepository.observeGuestStayHistory(guest.id) } returns flowOf(emptyList())

        val viewModel = GuestsViewModel(guestRepository, authRepository)
        viewModel.loadGuestForEdit(guest.id)
        advanceUntilIdle()

        assertTrue(viewModel.canEditGuest.value)
        assertEquals(guest, viewModel.editGuest.value)
    }
}
