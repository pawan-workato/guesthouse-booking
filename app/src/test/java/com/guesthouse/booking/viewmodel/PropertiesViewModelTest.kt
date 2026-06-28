package com.guesthouse.booking.viewmodel

import app.cash.turbine.test
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PropertiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val propertyRepository = mockk<PropertyRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    private val activeProperties = listOf(
        PropertyEntity(id = 1L, name = "Mountain Lodge", address = "A", region = "Colorado"),
        PropertyEntity(id = 2L, name = "Coastal Inn", address = "B", region = "California"),
        PropertyEntity(id = 3L, name = "Desert Retreat", address = "C", region = "Arizona")
    )

    @Test
    fun properties_filtersToAssignedSitesForPropertyManager() = runTest {
        every { propertyRepository.observeActiveProperties() } returns flowOf(activeProperties)
        every { propertyRepository.observeAllProperties() } returns flowOf(activeProperties)
        every { authRepository.session } returns sessionFlow
        sessionFlow.value = StaffSession(
            staffId = 2L,
            email = "manager.mountain@chain.com",
            displayName = "Mountain Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L, 3L)
        )

        val viewModel = PropertiesViewModel(propertyRepository, authRepository)
        advanceUntilIdle()

        viewModel.properties.test {
            skipItems(1)
            val filtered = awaitItem()
            assertEquals(listOf(1L, 3L), filtered.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun properties_showsAllSitesForChainAdmin() = runTest {
        every { propertyRepository.observeActiveProperties() } returns flowOf(activeProperties)
        every { propertyRepository.observeAllProperties() } returns flowOf(activeProperties)
        every { authRepository.session } returns sessionFlow
        sessionFlow.value = StaffSession(
            staffId = 1L,
            email = "admin@chain.com",
            displayName = "Chain Admin",
            role = StaffRole.CHAIN_ADMIN,
            assignedPropertyIds = emptySet()
        )

        val viewModel = PropertiesViewModel(propertyRepository, authRepository)
        advanceUntilIdle()

        viewModel.properties.test {
            skipItems(1)
            val all = awaitItem()
            assertEquals(3, all.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSearchQuery_filtersByNameRegionOrAddress() = runTest {
        every { propertyRepository.observeActiveProperties() } returns flowOf(activeProperties)
        every { propertyRepository.observeAllProperties() } returns flowOf(activeProperties)
        every { authRepository.session } returns sessionFlow
        sessionFlow.value = StaffSession(
            staffId = 1L,
            email = "admin@chain.com",
            displayName = "Chain Admin",
            role = StaffRole.CHAIN_ADMIN,
            assignedPropertyIds = emptySet()
        )

        val viewModel = PropertiesViewModel(propertyRepository, authRepository)
        advanceUntilIdle()
        viewModel.setSearchQuery("coastal")
        advanceUntilIdle()

        viewModel.properties.test {
            skipItems(1)
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals("Coastal Inn", filtered.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
