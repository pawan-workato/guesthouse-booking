package com.guesthouse.booking.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.guesthouse.booking.BuildConfig
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirebaseInitializer
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.firebase.FirestoreSyncService
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.remote.GuesthouseApi
import com.guesthouse.booking.data.remote.KtorApiSyncService
import com.guesthouse.booking.data.remote.LoginRequest
import com.guesthouse.booking.data.remote.TokenStorage
import com.guesthouse.booking.data.sync.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

class AuthRepository(
    private val database: AppDatabase,
    context: Context,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncService: FirestoreSyncService = FirestoreSyncService(database, firestore),
    private val api: GuesthouseApi? = null,
    private val tokenStorage: TokenStorage? = null,
    private val networkMonitor: NetworkMonitor? = null,
    private val ktorSync: Lazy<KtorApiSyncService>? = null
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseEnabled = FirebaseInitializer.isConfigured(context)
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val _session = MutableStateFlow<StaffSession?>(null)
    val session: StateFlow<StaffSession?> = _session.asStateFlow()

    suspend fun restoreSession() {
        if (BuildConfig.USE_KTOR_API) {
            val storage = tokenStorage ?: return
            if (!storage.hasToken()) return
            val staff = storage.loadStaff()
            if (staff == null) {
                storage.clear()
                return
            }
            val session = buildSession(staff.id, staff.email, staff.displayName, staff.role)
                ?: run {
                    storage.clear()
                    return
                }
            _session.value = session
            if (networkMonitor?.isCurrentlyOnline() == true) {
                runCatching { ktorSync?.value?.pullBootstrap(session) }
            }
            return
        }

        if (!firebaseEnabled) {
            clearPersistedSession()
            return
        }
        val user = auth.currentUser ?: run {
            clearPersistedSession()
            return
        }
        val session = loadFirebaseSession(user.uid) ?: run {
            logout()
            return
        }
        _session.value = session
        runCatching { syncService.pullRemoteData(session) }
    }

    suspend fun login(email: String, password: String): Result<StaffSession> {
        if (BuildConfig.USE_KTOR_API &&
            networkMonitor?.isCurrentlyOnline() == true &&
            api != null &&
            tokenStorage != null
        ) {
            val ktorResult = loginWithKtor(email, password)
            if (ktorResult.isSuccess) return ktorResult
            val message = ktorResult.exceptionOrNull()?.message.orEmpty()
            val isKtorUnreachable = message.contains("Network error", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Failed to connect", ignoreCase = true)
            if (!isKtorUnreachable) return ktorResult
        }
        if (!firebaseEnabled) {
            return Result.failure(IllegalStateException("Firebase is not configured"))
        }
        return loginWithFirebase(email, password)
    }

    fun logout() {
        if (BuildConfig.USE_KTOR_API) {
            tokenStorage?.clear()
        }
        if (firebaseEnabled) auth.signOut()
        clearPersistedSession()
        _session.value = null
    }

    fun currentSession(): StaffSession? = _session.value

    private suspend fun loginWithKtor(email: String, password: String): Result<StaffSession> {
        val apiClient = api ?: return Result.failure(IllegalStateException("API client not configured"))
        val storage = tokenStorage ?: return Result.failure(IllegalStateException("Token storage not configured"))
        return runCatching {
            val response = apiClient.login(LoginRequest(email.trim(), password))
            val staffDto = response.toStaffDto()
            storage.saveSession(response.token, staffDto)
            val session = buildSession(staffDto.id, staffDto.email, staffDto.displayName, staffDto.role)
                ?: throw IllegalStateException("Staff account is misconfigured")
            persistSession(session.staffId)
            _session.value = session
            ktorSync?.value?.pullBootstrap(session)
            session
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(mapKtorError(it)) }
        )
    }

    private suspend fun loginWithFirebase(email: String, password: String): Result<StaffSession> {
        return runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Firebase sign-in succeeded without a user")
            val session = loadFirebaseSession(uid)
                ?: throw IllegalArgumentException("No staff profile linked to this account")
            persistSession(session.staffId, uid)
            _session.value = session
            syncService.pullRemoteData(session)
            session
        }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(mapFirebaseError(it)) })
    }


    private suspend fun loadFirebaseSession(uid: String): StaffSession? {
        val profile = firestore.getStaffByUid(uid)
            ?: database.staffDao().findByFirebaseUid(uid)?.let { staff ->
                if (!staff.isActive) return null
                val assigned = if (staff.role == StaffRole.CHAIN_ADMIN.name) emptyList()
                else database.staffDao().assignedPropertyIds(staff.id)
                com.guesthouse.booking.data.firebase.StaffProfile(
                    firebaseUid = uid,
                    staffId = staff.id,
                    email = staff.email,
                    displayName = staff.displayName,
                    role = staff.role,
                    assignedPropertyIds = assigned
                )
            } ?: return null
        syncService.cacheStaffProfile(profile)
        val local = database.staffDao().findById(profile.staffId)
        if (local != null && !local.isActive) return null
        return buildSession(profile.staffId, profile.email, profile.displayName, profile.role)
    }


    private suspend fun buildSession(staffId: Long, email: String, displayName: String, roleName: String): StaffSession? {
        val role = runCatching { StaffRole.valueOf(roleName) }.getOrNull() ?: return null
        val assigned = if (role == StaffRole.CHAIN_ADMIN) emptySet()
        else database.staffDao().assignedPropertyIds(staffId).toSet()
        return StaffSession(staffId, email, displayName, role, assigned)
    }

    private fun persistSession(staffId: Long, firebaseUid: String? = null) {
        prefs.edit().putLong(KEY_STAFF_ID, staffId).apply()
        if (!firebaseUid.isNullOrBlank()) prefs.edit().putString(KEY_FIREBASE_UID, firebaseUid).apply()
    }

    private fun clearPersistedSession() {
        prefs.edit().clear().apply()
    }

    private fun mapKtorError(error: Throwable): Throwable {
        if (error is HttpException) {
            return when (error.code()) {
                401 -> IllegalArgumentException("Invalid email or password")
                in 500..599 -> IllegalStateException("Server error — try again later")
                else -> IllegalStateException("Network error — try again when online")
            }
        }
        val message = error.message.orEmpty()
        return if (message.contains("Unable to resolve host", true) || message.contains("Failed to connect", true)) {
            IllegalStateException("Network error — try again when online")
        } else {
            error
        }
    }

    private fun mapFirebaseError(error: Throwable): Throwable {
        val message = error.message.orEmpty()
        return when {
            message.contains("ERROR_INVALID_CREDENTIAL", true) ||
                message.contains("ERROR_WRONG_PASSWORD", true) ||
                message.contains("ERROR_USER_NOT_FOUND", true) ->
                IllegalArgumentException("Invalid email or password")
            message.contains("ERROR_NETWORK", true) ->
                IllegalStateException("Network error — try again when online")
            else -> error
        }
    }

    companion object {
        private const val PREFS_NAME = "guesthouse_auth_secure"
        private const val KEY_STAFF_ID = "staff_id"
        private const val KEY_FIREBASE_UID = "firebase_uid"
    }
}
