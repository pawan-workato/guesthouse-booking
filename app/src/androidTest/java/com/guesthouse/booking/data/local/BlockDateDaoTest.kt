package com.guesthouse.booking.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockDateDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BlockDateDao
    private var roomId = 0L

    @Before
    fun setUp() = runTest {
        db = TestDatabase.createInMemoryDatabase()
        dao = db.blockDateDao()
        val pid = db.propertyDao().insert(
            PropertyEntity(name = "Lodge", address = "1 Main", region = "Colorado")
        )
        db.roomDao().insertAll(
            listOf(
                RoomEntity(
                    propertyId = pid,
                    name = "Room",
                    description = "Double",
                    pricePerNight = 100.0,
                    capacity = 2
                )
            )
        )
        roomId = db.roomDao().getAll().first().id
        dao.insert(
            BlockDateEntity(
                propertyId = pid,
                roomId = roomId,
                startEpochDay = 100L,
                endEpochDay = 105L,
                reason = "Maintenance"
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun overlapping() = runTest {
        assertEquals(1, dao.findOverlapping(roomId, 102L, 107L).size)
    }

    @Test
    fun adjacent() = runTest {
        assertTrue(dao.findOverlapping(roomId, 105L, 110L).isEmpty())
    }
}
