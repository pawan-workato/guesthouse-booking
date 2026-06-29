package com.guesthouse.booking.data.repository
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.*
import com.guesthouse.booking.data.local.entities.*
import com.guesthouse.booking.data.sync.NetworkMonitor
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
    private val networkMonitor = mockk<NetworkMonitor>()
    private val firestore = mockk<FirestoreDataSource>(relaxed = true)
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private lateinit var repository: BlockDateRepository
    private val room = RoomEntity(id=10L, propertyId=1L, name="A", description="D", pricePerNight=120.0, capacity=2)
    private val session = StaffSession(2L,"m@c.com","M",StaffRole.PROPERTY_MANAGER,setOf(1L))

    @Before
    fun setUp() {
        every { database.blockDateDao() } returns blockDateDao
        every { database.bookingDao() } returns bookingDao
        every { database.roomDao() } returns roomDao
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { firestore.isSignedIn } returns false
        repository = BlockDateRepository(database, authRepository, networkMonitor, firestore, lazy { syncRepository })
    }

    @Test
    fun createBlock_succeedsOfflineWithPendingSync() = runTest {
        every { authRepository.currentSession() } returns session
        coEvery { roomDao.getById(10L) } returns room
        coEvery { blockDateDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { bookingDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { blockDateDao.insert(any()) } returns 7L

        assertEquals(7L, repository.createBlock(10L, 100L, 105L, "Maintenance").getOrNull())
        coVerify {
            blockDateDao.insert(match {
                it.roomId == 10L && it.syncStatus == SyncStatus.PENDING_SYNC.name
            })
        }
        verify { syncRepository.enqueueSyncWorker() }
    }

    @Test
    fun createBlock_pushesToFirestoreWhenOnline() = runTest {
        every { authRepository.currentSession() } returns session
        every { networkMonitor.isCurrentlyOnline() } returns true
        every { firestore.isSignedIn } returns true
        coEvery { roomDao.getById(10L) } returns room
        coEvery { blockDateDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { bookingDao.findOverlapping(10L, 100L, 105L) } returns emptyList()
        coEvery { blockDateDao.insert(any()) } returns 7L
        coEvery { blockDateDao.getById(7L) } returns BlockDateEntity(
            id = 7L,
            propertyId = 1L,
            roomId = 10L,
            startEpochDay = 100L,
            endEpochDay = 105L,
            reason = "Maintenance",
            syncStatus = SyncStatus.SYNCED.name
        )

        assertTrue(repository.createBlock(10L, 100L, 105L, "Maintenance").isSuccess)
        coVerify { firestore.upsertBlockDate(any()) }
        verify(exactly = 0) { syncRepository.enqueueSyncWorker() }
    }

    @Test
    fun removeBlock_deletesLocallyWhenNeverSynced() = runTest {
        val block = BlockDateEntity(
            id = 3L,
            propertyId = 1L,
            roomId = 10L,
            startEpochDay = 100L,
            endEpochDay = 105L,
            syncStatus = SyncStatus.PENDING_SYNC.name
        )
        every { authRepository.currentSession() } returns session
        coEvery { blockDateDao.getById(3L) } returns block

        assertTrue(repository.removeBlock(3L).isSuccess)
        coVerify { blockDateDao.deleteById(3L) }
        coVerify(exactly = 0) { firestore.deleteBlockDate(any()) }
    }

    @Test
    fun removeBlock_marksForDeletionWhenOfflineAndSynced() = runTest {
        val block = BlockDateEntity(
            id = 3L,
            propertyId = 1L,
            roomId = 10L,
            startEpochDay = 100L,
            endEpochDay = 105L,
            syncStatus = SyncStatus.SYNCED.name
        )
        every { authRepository.currentSession() } returns session
        coEvery { blockDateDao.getById(3L) } returns block

        assertTrue(repository.removeBlock(3L).isSuccess)
        coVerify { blockDateDao.markForDeletion(3L, SyncStatus.PENDING_SYNC.name) }
        verify { syncRepository.enqueueSyncWorker() }
    }
}
