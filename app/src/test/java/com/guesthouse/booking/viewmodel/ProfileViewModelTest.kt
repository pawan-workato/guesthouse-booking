package com.guesthouse.booking.viewmodel

import app.cash.turbine.test
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.ProfileRepository
import com.guesthouse.booking.data.repository.StaffProfileDetails
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

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profileRepository = mockk<ProfileRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val profileFlow = MutableStateFlow<StaffProfileDetails?>(null)

    private val details = StaffProfileDetails(
        staffId = 1L,
        email = "manager@example.com",
        displayName = "Alex",
        role = StaffRole.PROPERTY_MANAGER,
        roleLabel = "Property manager",
        assignedPropertyNames = listOf("Mountain Lodge"),
        isChainAdmin = false
    )

    @Test
    fun profile_emitsFromRepository() = runTest {
        every { profileRepository.observeProfile() } returns flowOf(details)
        val viewModel = ProfileViewModel(profileRepository, authRepository)
        advanceUntilIdle()
        viewModel.profile.test {
            val first = awaitItem()
            if (first == null) assertEquals(details, awaitItem()) else assertEquals(details, first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveDisplayName_delegatesToRepository() = runTest {
        every { profileRepository.observeProfile() } returns flowOf(details)
        coEvery { profileRepository.updateDisplayName("Alex M") } returns Result.success(Unit)
        val viewModel = ProfileViewModel(profileRepository, authRepository)
        viewModel.saveDisplayName("Alex M")
        advanceUntilIdle()
        coVerify { profileRepository.updateDisplayName("Alex M") }
    }

    @Test
    fun sendPasswordResetEmail_delegatesToAuth() = runTest {
        every { profileRepository.observeProfile() } returns flowOf(details)
        coEvery { authRepository.sendPasswordResetEmail() } returns Result.success(Unit)
        val viewModel = ProfileViewModel(profileRepository, authRepository)
        viewModel.sendPasswordResetEmail()
        advanceUntilIdle()
        coVerify { authRepository.sendPasswordResetEmail() }
    }
}
