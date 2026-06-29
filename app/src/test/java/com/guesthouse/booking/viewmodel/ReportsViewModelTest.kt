package com.guesthouse.booking.viewmodel

import app.cash.turbine.test
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.PropertyRevenueStats
import com.guesthouse.booking.data.repository.ReportsRepository
import com.guesthouse.booking.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val propertyRepository = mockk<PropertyRepository>()
    private val occupancyRepository = mockk<OccupancyRepository>()
    private val reportsRepository = mockk<ReportsRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    private val properties = listOf(
        PropertyEntity(id = 1L, name = "Mountain Lodge", address = "A", region = "Colorado")
    )

    @Test
    fun properties_emptyWhenNotChainAdmin() = runTest {
        every { propertyRepository.observeActiveProperties() } returns flowOf(properties)
        every { authRepository.session } returns sessionFlow
        sessionFlow.value = StaffSession(
            staffId = 2L,
            email = "manager@chain.com",
            displayName = "Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L)
        )

        val viewModel = ReportsViewModel(propertyRepository, occupancyRepository, reportsRepository, authRepository)
        advanceUntilIdle()

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shiftReportDate_reloadStatsForNewDay() = runTest {
        every { propertyRepository.observeActiveProperties() } returns flowOf(properties)
        every { authRepository.session } returns sessionFlow
        coEvery { reportsRepository.getRevenueStats(any(), any()) } returns listOf(
                PropertyRevenueStats(propertyId = 1L, propertyName = "Mountain Lodge", bookingCount = 1, totalRevenue = 240.0)
            )
        coEvery { occupancyRepository.getStatsForProperties(any(), any()) } answers {
            val day = secondArg<Long>()
            listOf(
                PropertyOccupancyStats(
                    propertyId = 1L,
                    propertyName = "Mountain Lodge",
                    totalRooms = 4,
                    occupiedTonight = if (day == LocalDate.now().toEpochDay()) 2 else 1,
                    arrivalsToday = 0,
                    departuresToday = 0,
                    blockedRooms = 0,
                    vacant = 2
                )
            )
        }
        sessionFlow.value = StaffSession(
            staffId = 1L,
            email = "admin@chain.com",
            displayName = "Admin",
            role = StaffRole.CHAIN_ADMIN,
            assignedPropertyIds = emptySet()
        )

        val viewModel = ReportsViewModel(propertyRepository, occupancyRepository, reportsRepository, authRepository)
        advanceUntilIdle()

        viewModel.occupancyStats.test {
            skipItems(1)
            assertEquals(2, awaitItem().single().occupiedTonight)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.shiftReportDate(1)
        advanceUntilIdle()

        coVerify { occupancyRepository.getStatsForProperties(properties, LocalDate.now().plusDays(1).toEpochDay()) }
    }
}
