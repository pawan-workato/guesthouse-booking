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
import java.time.LocalDate

class TodayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)
    private val today = LocalDate.now().toEpochDay()

    private val managerSession = StaffSession(
        staffId = 2L,
        email = "manager.mountain@chain.com",
        displayName = "Mountain Manager",
        role = StaffRole.PROPERTY_MANAGER,
        assignedPropertyIds = setOf(1L)
    )

    @Test
    fun uiState_groupsArrivalsDeparturesAndInHouseForAccessibleProperties() = runTest {
        val arrival = BookingEntity(
            id = 1L, propertyId = 1L, roomId = 10L, guestName = "Arrival Guest",
            guestEmail = "", checkInEpochDay = today, checkOutEpochDay = today + 2,
            status = BookingStatus.CONFIRMED.name
        )
        val departure = BookingEntity(
            id = 2L, propertyId = 1L, roomId = 11L, guestName = "Departure Guest",
            guestEmail = "", checkInEpochDay = today - 1, checkOutEpochDay = today,
            status = BookingStatus.CHECKED_IN.name
        )
        val inHouse = BookingEntity(
            id = 3L, propertyId = 1L, roomId = 12L, guestName = "In-house Guest",
            guestEmail = "", checkInEpochDay = today - 1, checkOutEpochDay = today + 1,
            status = BookingStatus.CHECKED_IN.name
        )
        val outOfScope = BookingEntity(
            id = 4L, propertyId = 99L, roomId = 13L, guestName = "Hidden Guest",
            guestEmail = "", checkInEpochDay = today, checkOutEpochDay = today + 1,
            status = BookingStatus.CONFIRMED.name
        )

        every { bookingRepository.observeBookings() } returns flowOf(listOf(arrival, departure, inHouse, outOfScope))
        every { bookingRepository.observeRooms() } returns flowOf(
            listOf(
                RoomEntity(id = 10L, propertyId = 1L, name = "Room 10", description = "", pricePerNight = 100.0, capacity = 2),
                RoomEntity(id = 11L, propertyId = 1L, name = "Room 11", description = "", pricePerNight = 100.0, capacity = 2),
                RoomEntity(id = 12L, propertyId = 1L, name = "Room 12", description = "", pricePerNight = 100.0, capacity = 2),
            )
        )
        every { bookingRepository.observeProperties() } returns flowOf(
            listOf(PropertyEntity(id = 1L, name = "Mountain Lodge", address = "X", region = "Nevada"))
        )
        sessionFlow.value = managerSession
        every { authRepository.session } returns sessionFlow
        every { authRepository.currentSession() } returns managerSession

        val viewModel = TodayViewModel(bookingRepository, authRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.arrivals.size)
        assertEquals("Arrival Guest", state.arrivals.first().booking.guestName)
        assertEquals(1, state.departures.size)
        assertEquals("Departure Guest", state.departures.first().booking.guestName)
        assertEquals(1, state.inHouse.size)
        assertEquals("In-house Guest", state.inHouse.first().booking.guestName)
    }

    @Test
    fun checkIn_delegatesToRepository() = runTest {
        every { bookingRepository.observeBookings() } returns flowOf(emptyList())
        every { bookingRepository.observeRooms() } returns flowOf(emptyList())
        every { bookingRepository.observeProperties() } returns flowOf(emptyList())
        sessionFlow.value = managerSession
        every { authRepository.session } returns sessionFlow
        every { authRepository.currentSession() } returns managerSession
        coEvery { bookingRepository.checkInBooking(7L, today) } returns Result.success(Unit)

        val viewModel = TodayViewModel(bookingRepository, authRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.checkIn(7L)
        advanceUntilIdle()

        coVerify { bookingRepository.checkInBooking(7L, today) }
    }
}
