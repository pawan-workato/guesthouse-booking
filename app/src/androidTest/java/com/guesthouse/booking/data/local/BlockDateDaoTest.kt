package com.guesthouse.booking.data.local
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.local.entities.*
import com.guesthouse.booking.testutil.TestDatabase
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class BlockDateDaoTest {
    private lateinit var db: AppDatabase; private lateinit var dao: BlockDateDao; private var roomId=0L
    @Before fun setUp()=runTest{ db=TestDatabase.createInMemoryDatabase(); dao=db.blockDateDao(); val pid=db.propertyDao().insert(PropertyEntity("L","1","CO")); db.roomDao().insertAll(listOf(RoomEntity(pid,"R","D",100.0,2))); roomId=db.roomDao().getAll().first().id; dao.insert(BlockDateEntity(pid,roomId,100L,105L,"M"))}
    @After fun tearDown(){db.close()}
    @Test fun overlapping()=runTest{assertEquals(1,dao.findOverlapping(roomId,102L,107L).size)}
    @Test fun adjacent()=runTest{assertTrue(dao.findOverlapping(roomId,105L,110L).isEmpty())}
}
