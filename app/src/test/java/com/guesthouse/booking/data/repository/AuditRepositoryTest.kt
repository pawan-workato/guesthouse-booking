package com.guesthouse.booking.data.repository

import app.cash.turbine.test
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.AuditEventDao
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.AuditAction
import com.guesthouse.booking.data.local.entities.AuditEntityType
import com.guesthouse.booking.data.local.entities.AuditEventEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.testutil.MainDispatcherRule
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

class AuditRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val database = mockk<AppDatabase>()
    private val auditEventDao = mockk<AuditEventDao>(relaxed = true)
    private val authRepository = mockk<AuthRepository>()
    private val sessionFlow = MutableStateFlow<StaffSession?>(null)

    @Test
    fun append_insertsEventForCurrentSession() = runTest {
        every { database.auditEventDao() } returns auditEventDao
        every { authRepository.currentSession() } returns StaffSession(
            staffId = 5L,
            email = "m@test.com",
            displayName = "Manager",
            role = StaffRole.PROPERTY_MANAGER,
            assignedPropertyIds = setOf(1L)
        )

        AuditRepository(database, authRepository).append(
            action = AuditAction.CREATE,
            entityType = AuditEntityType.BOOKING,
            summary = "Created booking",
            propertyId = 1L,
            entityId = 42L
        )

        coVerify { auditEventDao.insert(match { it.staffId == 5L && it.summary == "Created booking" }) }
    }

    @Test
    fun observeEvents_chainAdminUsesObserveAll() = runTest {
        every { database.auditEventDao() } returns auditEventDao
        every { authRepository.session } returns sessionFlow
        every { auditEventDao.observeAll() } returns flowOf(emptyList<AuditEventEntity>())
        sessionFlow.value = StaffSession(
            staffId = 1L,
            email = "admin@test.com",
            displayName = "Admin",
            role = StaffRole.CHAIN_ADMIN,
            assignedPropertyIds = emptySet()
        )

        val repository = AuditRepository(database, authRepository)
        advanceUntilIdle()
        repository.observeEvents().test {
            assertEquals(emptyList<AuditEventEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
