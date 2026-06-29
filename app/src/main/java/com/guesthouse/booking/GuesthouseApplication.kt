package com.guesthouse.booking

import android.app.Application
import com.guesthouse.booking.data.firebase.FirebaseInitializer
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.firebase.FirestoreSyncService
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.remote.ApiClient
import com.guesthouse.booking.data.remote.KtorApiSyncService
import com.guesthouse.booking.data.remote.TokenStorage
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.data.repository.BlockDateRepository
import com.guesthouse.booking.data.repository.BookingRepository
import com.guesthouse.booking.data.repository.GuestRepository
import com.guesthouse.booking.data.repository.PropertyRepository
import com.guesthouse.booking.data.repository.StaffRepository
import com.guesthouse.booking.data.repository.SyncRepository
import com.guesthouse.booking.data.sync.NetworkMonitor

class GuesthouseApplication : Application() {
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
    lateinit var tokenStorage: TokenStorage
        private set
    lateinit var apiClient: ApiClient
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
        tokenStorage = TokenStorage(this)
        apiClient = ApiClient(tokenStorage)

        lateinit var syncRef: SyncRepository
        val ktorSync = lazy {
            KtorApiSyncService(database, apiClient.api, authRepository)
        }

        authRepository = AuthRepository(
            database = database,
            context = this,
            firestore = firestore,
            syncService = syncService,
            api = apiClient.api,
            tokenStorage = tokenStorage,
            networkMonitor = networkMonitor,
            ktorSync = ktorSync
        )

        syncRef = SyncRepository(
            database = database,
            networkMonitor = networkMonitor,
            context = this,
            authRepository = authRepository,
            firestore = firestore,
            syncService = syncService,
            tokenStorage = tokenStorage,
            ktorSync = ktorSync
        )
        syncRepository = syncRef

        repository = BookingRepository(
            database,
            authRepository,
            networkMonitor,
            firestore,
            lazy { syncRef }
        )
        blockDateRepository = BlockDateRepository(database, authRepository)
        propertyRepository = PropertyRepository(database, networkMonitor, firestore)
        guestRepository = GuestRepository(
            database,
            authRepository,
            networkMonitor,
            firestore,
            lazy { syncRef }
        )
        staffRepository = StaffRepository(database)
        syncRepository.schedulePeriodicSync()
    }
}
