package com.guesthouse.booking.viewmodel

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    @Test
    fun cancelBooking_isBlockedForOutOfScopeProperty() = runTest {
        val outOfScopeBooking = BookingEntity(
            id = 9L,
            propertyId = 99L,
            roomId = 1L,
            guestName = "Jane Doe",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L
        )
        every { bookingRepository.observeBookings() } returns flowOf(listOf(outOfScopeBooking))
        every { bookingRepository.observeRooms() } returns flowOf(
            listOf(RoomEntity(id = 1L, propertyId = 99L, name = "Room 1", description = "", pricePerNight = 100.0, capacity = 2))
        )
        every { bookingRepository.observeProperties() } returns flowOf(
            listOf(PropertyEntity(id = 99L, name = "Remote Lodge", address = "X", region = "Nevada"))
        )
        every { authRepository.session } returns sessionFlow
        every { authRepository.currentSession() } returns StaffSession(
            staffId = 2L,
            email = "manager.mountain@chain.com",
            displayName = "Mountain Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L)
        )
        coEvery { bookingRepository.getBookingById(9L) } returns outOfScopeBooking

        val viewModel = AdminViewModel(bookingRepository, authRepository)
        advanceUntilIdle()
        viewModel.cancelBooking(9L)
        advanceUntilIdle()

        coVerify(exactly = 0) { bookingRepository.cancelBooking(any()) }
    }
}
