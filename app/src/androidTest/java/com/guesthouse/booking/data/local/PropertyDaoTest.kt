package com.guesthouse.booking.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertyDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var propertyDao: PropertyDao

    @Before
    fun setUp() {
        database = TestDatabase.createInMemoryDatabase()
        propertyDao = database.propertyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_returnsOnlyActiveProperties() = runTest {
        propertyDao.insert(PropertyEntity(name = "Active Lodge", address = "A", region = "Colorado", isActive = true))
        val inactiveId = propertyDao.insert(
            PropertyEntity(name = "Closed Lodge", address = "B", region = "Utah", isActive = false)
        )

        val active = propertyDao.observeAll().first()
        assertEquals(1, active.size)
        assertEquals("Active Lodge", active.first().name)
        assertFalse(active.any { it.id == inactiveId })
    }

    @Test
    fun observeAllIncludingInactive_returnsEveryProperty() = runTest {
        propertyDao.insert(PropertyEntity(name = "Active Lodge", address = "A", region = "Colorado", isActive = true))
        propertyDao.insert(PropertyEntity(name = "Closed Lodge", address = "B", region = "Utah", isActive = false))

        val all = propertyDao.observeAllIncludingInactive().first()
        assertEquals(2, all.size)
    }

    @Test
    fun setActive_togglesVisibilityInActiveQuery() = runTest {
        val id = propertyDao.insert(PropertyEntity(name = "Seasonal Lodge", address = "C", region = "Montana", isActive = true))
        propertyDao.setActive(id, false)

        val active = propertyDao.observeAll().first()
        assertEquals(0, active.size)

        val stored = propertyDao.getById(id)
        assertFalse(stored!!.isActive)
    }
}
