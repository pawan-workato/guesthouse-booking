package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.PropertyDao
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.sync.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PropertyRepositoryTest {

    private val database = mockk<AppDatabase>()
    private val propertyDao = mockk<PropertyDao>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)

    private lateinit var repository: PropertyRepository

    @Before
    fun setUp() {
        every { database.propertyDao() } returns propertyDao
        every { networkMonitor.isCurrentlyOnline() } returns false
        repository = PropertyRepository(database, networkMonitor, firestore)
    }

    @Test
    fun createProperty_rejectsBlankName() = runTest {
        val result = repository.createProperty("", "123 Main", "Colorado", "15:00", "11:00")

        assertTrue(result.isFailure)
        assertEquals("Property name is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun createProperty_rejectsBlankAddress() = runTest {
        val result = repository.createProperty("Mountain Lodge", "  ", "Colorado", "15:00", "11:00")

        assertTrue(result.isFailure)
        assertEquals("Address is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun createProperty_rejectsBlankRegion() = runTest {
        val result = repository.createProperty("Mountain Lodge", "123 Main", "  ", "15:00", "11:00")

        assertTrue(result.isFailure)
        assertEquals("Region is required", result.exceptionOrNull()?.message)
    }

    @Test
    fun createProperty_appliesDefaultCheckTimesAndPersists() = runTest {
        coEvery { propertyDao.insert(any()) } returns 3L

        val result = repository.createProperty("  Mountain Lodge ", " 123 Main St ", " Colorado ", "", "")

        assertTrue(result.isSuccess)
        assertEquals(3L, result.getOrNull())
        coVerify {
            propertyDao.insert(
                match {
                    it.name == "Mountain Lodge" &&
                        it.address == "123 Main St" &&
                        it.region == "Colorado" &&
                        it.checkInTime == "15:00" &&
                        it.checkOutTime == "11:00" &&
                        it.isActive
                }
            )
        }
    }

    @Test
    fun updateProperty_rejectsBlankAddress() = runTest {
        val property = PropertyEntity(id = 1L, name = "Lodge", address = "", region = "Colorado")

        val result = repository.updateProperty(property)

        assertTrue(result.isFailure)
        assertEquals("Address is required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { propertyDao.update(any()) }
    }
}
