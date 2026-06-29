package com.guesthouse.booking.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var roomDao: RoomDao
    private var propertyId: Long = 0L

    @Before
    fun setUp() = runTest {
        database = TestDatabase.createInMemoryDatabase()
        roomDao = database.roomDao()
        propertyId = database.propertyDao().insert(
            PropertyEntity(name = "Test Lodge", address = "1 Main", region = "Test")
        )
        roomDao.insertAll(
            listOf(
                RoomEntity(propertyId = propertyId, name = "Double A", description = "", pricePerNight = 80.0, capacity = 2, roomType = RoomType.DOUBLE.name),
                RoomEntity(propertyId = propertyId, name = "Double B", description = "", pricePerNight = 85.0, capacity = 2, roomType = RoomType.DOUBLE.name),
                RoomEntity(propertyId = propertyId, name = "Single", description = "", pricePerNight = 55.0, capacity = 1, roomType = RoomType.SINGLE.name),
                RoomEntity(propertyId = propertyId, name = "Suite", description = "", pricePerNight = 120.0, capacity = 3, roomType = RoomType.SUITE.name),
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun countByTypeForProperty_returnsGroupedCounts() = runTest {
        val rows = roomDao.countByTypeForProperty(propertyId)
        assertEquals(
            mapOf("DOUBLE" to 2, "SINGLE" to 1, "SUITE" to 1),
            rows.associate { it.roomType to it.count }
        )
    }
}
