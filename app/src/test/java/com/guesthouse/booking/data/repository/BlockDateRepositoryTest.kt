package com.guesthouse.booking.data.repository
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.*
import com.guesthouse.booking.data.local.entities.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
class BlockDateRepositoryTest {
    private val database = mockk<AppDatabase>()
    private val blockDateDao = mockk<BlockDateDao>(relaxed = true)
    private val bookingDao = mockk<BookingDao>(relaxed = true)
    private val roomDao = mockk<RoomDao>()
    private val authRepository = mockk<AuthRepository>()
    private lateinit var repository: BlockDateRepository
    private val room = RoomEntity(id=10L, propertyId=1L, name="A", description="D", pricePerNight=120.0, capacity=2)
    private val session = StaffSession(2L,"m@c.com","M",StaffRole.PROPERTY_MANAGER,setOf(1L))
    @Before fun setUp(){ every{database.blockDateDao()}returns blockDateDao; every{database.bookingDao()}returns bookingDao; every{database.roomDao()}returns roomDao; repository=BlockDateRepository(database,authRepository)}
    @Test fun createBlock_succeeds()=runTest{ every{authRepository.currentSession()}returns session; coEvery{roomDao.getById(10L)}returns room; coEvery{blockDateDao.findOverlapping(10L,100L,105L)}returns emptyList(); coEvery{bookingDao.findOverlapping(10L,100L,105L)}returns emptyList(); coEvery{blockDateDao.insert(any())}returns 7L; assertEquals(7L,repository.createBlock(10L,100L,105L,"M").getOrNull())}
}
