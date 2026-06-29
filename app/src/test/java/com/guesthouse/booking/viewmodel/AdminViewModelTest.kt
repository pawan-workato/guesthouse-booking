package com.guesthouse.booking.viewmodel

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    private fun managerSession(propertyIds: Set<Long> = setOf(1L)) = StaffSession(
        staffId = 2L,
        email = "manager.mountain@chain.com",
        displayName = "Mountain Manager",
        role = StaffRole.PROPERTY_MANAGER,
        assignedPropertyIds = propertyIds
    )

    private fun setupBookings(vararg bookings: BookingEntity) {
        every { bookingRepository.observeBookings() } returns flowOf(bookings.toList())
        every { bookingRepository.observeRooms() } returns flowOf(
            listOf(RoomEntity(id = 1L, propertyId = 1L, name = "Room 1", description = "", pricePerNight = 100.0, capacity = 2))
        )
        every { bookingRepository.observeProperties() } returns flowOf(
            listOf(PropertyEntity(id = 1L, name = "Mountain Lodge", address = "X", region = "Colorado"))
        )
        every { authRepository.session } returns sessionFlow
    }

    @Test
    fun bookingsWithDetails_hidesCancelledByDefault() = runTest {
        val activeBooking = BookingEntity(
            id = 1L,
            propertyId = 1L,
            roomId = 1L,
            guestName = "Active Guest",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L,
            status = BookingStatus.CONFIRMED.name
        )
        val cancelledBooking = BookingEntity(
            id = 2L,
            propertyId = 1L,
            roomId = 1L,
            guestName = "Cancelled Guest",
            guestEmail = "",
            checkInEpochDay = 110L,
            checkOutEpochDay = 115L,
            status = BookingStatus.CANCELLED.name
        )
        setupBookings(activeBooking, cancelledBooking)
        sessionFlow.value = managerSession()

        val viewModel = AdminViewModel(bookingRepository, authRepository)
        backgroundScope.launch { viewModel.bookingsWithDetails.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.bookingsWithDetails.value.map { it.booking.id })
    }

    @Test
    fun bookingsWithDetails_includesCancelledWhenToggleEnabled() = runTest {
        val activeBooking = BookingEntity(
            id = 1L,
            propertyId = 1L,
            roomId = 1L,
            guestName = "Active Guest",
            guestEmail = "",
            checkInEpochDay = 100L,
            checkOutEpochDay = 105L,
            status = BookingStatus.CONFIRMED.name
        )
        val cancelledBooking = BookingEntity(
            id = 2L,
            propertyId = 1L,
            roomId = 1L,
            guestName = "Cancelled Guest",
            guestEmail = "",
            checkInEpochDay = 110L,
            checkOutEpochDay = 115L,
            status = BookingStatus.CANCELLED.name
        )
        setupBookings(activeBooking, cancelledBooking)
        sessionFlow.value = managerSession()

        val viewModel = AdminViewModel(bookingRepository, authRepository)
        backgroundScope.launch { viewModel.bookingsWithDetails.collect {} }
        viewModel.setShowCancelled(true)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.bookingsWithDetails.value.map { it.booking.id })
    }

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

    @Test
    fun checkOut_surfacesFailureMessage() = runTest {
        setupBookings()
        sessionFlow.value = managerSession()
        coEvery { bookingRepository.checkOutBooking(1L) } returns Result.failure(
            IllegalStateException("Only checked-in guests can be checked out")
        )

        val viewModel = AdminViewModel(bookingRepository, authRepository)
        viewModel.checkOut(1L)
        advanceUntilIdle()

        assertEquals("Only checked-in guests can be checked out", viewModel.actionError.value)
        assertEquals(null, viewModel.actionMessage.value)
    }
}
