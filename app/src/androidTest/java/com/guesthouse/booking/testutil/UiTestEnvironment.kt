package com.guesthouse.booking.testutil

import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.AuditRepository
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.HandoverNoteRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import com.guesthouse.booking.viewmodel.ViewModelFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

data class SeededUiData(
    val assignedPropertyId: Long,
    val otherPropertyId: Long,
    val guestId: Long,
    val roomId: Long,
    val todayBookingId: Long,
    val futureBookingId: Long
)

class UiTestEnvironment(
    initialSession: StaffSession = UiTestSessions.manager
) {
    val database: AppDatabase = TestDatabase.createInMemoryDatabase()
    private val sessionFlow = MutableStateFlow(initialSession)

    val authRepository: AuthRepository = mockk(relaxed = true)
    val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    val firestore: FirestoreDataSource = mockk(relaxed = true)
    val syncRepository: SyncRepository = mockk(relaxed = true)

    val propertyRepository = PropertyRepository(database, networkMonitor, firestore)
    val guestRepository = GuestRepository(database, authRepository, networkMonitor, firestore)
    val bookingRepository = BookingRepository(
        database,
        authRepository,
        networkMonitor,
        firestore,
        lazy { syncRepository }
    )
    val blockDateRepository = BlockDateRepository(
        database,
        authRepository,
        networkMonitor,
        firestore,
        lazy { syncRepository }
    )
    val staffRepository = StaffRepository(database)
    val auditRepository = AuditRepository(database, authRepository)
    val handoverNoteRepository = HandoverNoteRepository(database, authRepository)

    val viewModelFactory = ViewModelFactory(
        database = database,
        repository = bookingRepository,
        blockDateRepository = blockDateRepository,
        propertyRepository = propertyRepository,
        guestRepository = guestRepository,
        authRepository = authRepository,
        syncRepository = syncRepository,
        staffRepository = staffRepository,
        networkMonitor = networkMonitor,
        auditRepository = auditRepository,
        handoverNoteRepository = handoverNoteRepository
    )

    init {
        every { authRepository.session } returns sessionFlow
        every { authRepository.currentSession() } answers { sessionFlow.value }
        every { networkMonitor.isCurrentlyOnline() } returns false
        every { networkMonitor.isOnline } returns MutableStateFlow(false)
        every { firestore.isSignedIn } returns false
        every { syncRepository.isOnline } returns MutableStateFlow(false)
        every { syncRepository.lastSyncEpochMs } returns MutableStateFlow(0L)
        every { syncRepository.observePending() } returns flowOf(emptyList())
        every { syncRepository.observeConflicts() } returns flowOf(emptyList())
        coEvery { authRepository.login(any(), any()) } returns Result.failure(IllegalArgumentException("Invalid email or password"))
    }

    fun setSession(session: StaffSession) {
        sessionFlow.value = session
    }

    suspend fun seedStandardData(): SeededUiData {
        val assignedPropertyId = database.propertyDao().insert(
            PropertyEntity(name = "Hill View Guesthouse", address = "1 Peak Rd", region = "Mountain West")
        )
        val otherPropertyId = database.propertyDao().insert(
            PropertyEntity(name = "Coastal Lodge", address = "9 Harbor Ln", region = "Coastal")
        )
        database.roomDao().insertAll(
            listOf(
                RoomEntity(
                    propertyId = assignedPropertyId,
                    name = "Mountain Double",
                    description = "Double room",
                    pricePerNight = 120.0,
                    capacity = 2
                ),
                RoomEntity(
                    propertyId = otherPropertyId,
                    name = "Ocean Suite",
                    description = "Suite",
                    pricePerNight = 180.0,
                    capacity = 3
                )
            )
        )
        val rooms = database.roomDao().getAll()
        val assignedRoomId = rooms.first { it.propertyId == assignedPropertyId }.id
        val otherRoomId = rooms.first { it.propertyId == otherPropertyId }.id

        val guestId = database.guestDao().insert(
            GuestEntity(name = "Jane Guest", email = "jane@example.com", phone = "555-0100")
        )

        val today = LocalDate.now().toEpochDay()
        val todayBookingId = database.bookingDao().insert(
            BookingEntity(
                propertyId = assignedPropertyId,
                roomId = assignedRoomId,
                guestId = guestId,
                guestName = "Jane Guest",
                guestEmail = "jane@example.com",
                checkInEpochDay = today,
                checkOutEpochDay = today + 2,
                status = BookingStatus.CONFIRMED.name,
                bookingReference = "GH-1-100"
            )
        )
        val futureBookingId = database.bookingDao().insert(
            BookingEntity(
                propertyId = otherPropertyId,
                roomId = otherRoomId,
                guestId = guestId,
                guestName = "Jane Guest",
                guestEmail = "jane@example.com",
                checkInEpochDay = today + 30,
                checkOutEpochDay = today + 32,
                status = BookingStatus.CONFIRMED.name,
                bookingReference = "GH-2-200"
            )
        )

        val adminId = database.staffDao().insert(
            StaffEntity(
                email = "admin@chain.com",
                passwordHash = "hash",
                displayName = "Chain Admin",
                role = StaffRole.CHAIN_ADMIN.name
            )
        )
        val managerId = database.staffDao().insert(
            StaffEntity(
                email = "manager.mountain@chain.com",
                passwordHash = "hash",
                displayName = "Alex Mountain",
                role = StaffRole.PROPERTY_MANAGER.name
            )
        )
        database.staffDao().insertAssignments(
            listOf(
                StaffPropertyAssignmentEntity(staffId = managerId, propertyId = assignedPropertyId),
                StaffPropertyAssignmentEntity(staffId = adminId, propertyId = assignedPropertyId)
            )
        )

        if (sessionFlow.value.role == StaffRole.PROPERTY_MANAGER) {
            setSession(sessionFlow.value.copy(assignedPropertyIds = setOf(assignedPropertyId)))
        }

        return SeededUiData(
            assignedPropertyId = assignedPropertyId,
            otherPropertyId = otherPropertyId,
            guestId = guestId,
            roomId = assignedRoomId,
            todayBookingId = todayBookingId,
            futureBookingId = futureBookingId
        )
    }

    fun close() {
        database.close()
    }
}
