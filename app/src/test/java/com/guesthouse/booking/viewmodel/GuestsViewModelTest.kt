package com.guesthouse.booking.viewmodel

import app.cash.turbine.test
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val guestRepository = mockk<GuestRepository>()

    private val guests = listOf(
        GuestEntity(id = 1L, name = "Alice Anderson", email = "alice@test.com", phone = "555-0101"),
        GuestEntity(id = 2L, name = "Bob Baker", email = "bob@test.com", phone = "555-0102"),
        GuestEntity(id = 3L, name = "Carol Chen", email = "carol@test.com", phone = "555-0103")
    )

    @Test
    fun guests_filtersBySearchQueryAcrossNameEmailAndPhone() = runTest {
        every { guestRepository.observeActiveGuests() } returns flowOf(guests)
        every { guestRepository.observeAllGuests() } returns flowOf(guests)

        val viewModel = GuestsViewModel(guestRepository)
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
        every { guestRepository.observeActiveGuests() } returns flowOf(guests)
        every { guestRepository.observeAllGuests() } returns flowOf(guests)

        val viewModel = GuestsViewModel(guestRepository)
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
}
