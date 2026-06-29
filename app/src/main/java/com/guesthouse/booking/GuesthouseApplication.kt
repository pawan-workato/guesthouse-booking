package com.guesthouse.booking

import android.app.Application
import com.guesthouse.booking.data.firebase.FirebaseInitializer
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.firebase.FirestoreSyncService
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class GuesthouseApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var repository: BookingRepository
        private set
    lateinit var blockDateRepository: BlockDateRepository
        private set
    lateinit var propertyRepository: PropertyRepository
        private set
    lateinit var guestRepository: GuestRepository
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var staffRepository: StaffRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    val isFirebaseConfigured: Boolean
        get() = FirebaseInitializer.isConfigured(this)

    override fun onCreate() {
        super.onCreate()
        if (!FirebaseInitializer.initialize(this)) {
            // No google-services.json (e.g. CI): Room instrumented tests only need the process.
            return
        }
        val database = AppDatabase.getInstance(this)
        val firestore = FirestoreDataSource()
        val syncService = FirestoreSyncService(database, firestore)
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start()

        authRepository = AuthRepository(
            database = database,
            appContext = this,
            networkMonitor = networkMonitor,
            firestore = firestore,
            syncService = syncService
        )

        applicationScope.launch {
            networkMonitor.isOnline.drop(1).filter { it }.collect {
                authRepository.refreshSessionBinding()
            }
        }

        syncRepository = SyncRepository(
            database = database,
            networkMonitor = networkMonitor,
            context = this,
            authRepository = authRepository,
            firestore = firestore,
            syncService = syncService
        )

        repository = BookingRepository(
            database,
            authRepository,
            networkMonitor,
            firestore,
            lazy { syncRepository }
        )
        blockDateRepository = BlockDateRepository(
            database,
            authRepository,
            networkMonitor,
            firestore,
            lazy { syncRepository }
        )
        propertyRepository = PropertyRepository(database, networkMonitor, firestore)
        guestRepository = GuestRepository(
            database,
            authRepository,
            networkMonitor,
            firestore
        )
        staffRepository = StaffRepository(database)
        syncRepository.schedulePeriodicSync()
    }
}
