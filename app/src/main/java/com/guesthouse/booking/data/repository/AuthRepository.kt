package com.guesthouse.booking.data.repository

import android.content.Context
import android.util.Log
import com.guesthouse.booking.data.auth.LegacySessionStorage
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.firebase.FirebaseInitializer
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.firebase.FirestoreSyncService
import com.guesthouse.booking.data.firebase.PullRemoteDataResult
import com.guesthouse.booking.data.firebase.StaffProfile
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.sync.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val database: AppDatabase,
    private val appContext: Context,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncService: FirestoreSyncService = FirestoreSyncService(database, firestore),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val firebaseEnabled = FirebaseInitializer.isConfigured(appContext)
    private val _session = MutableStateFlow<StaffSession?>(null)
    val session: StateFlow<StaffSession?> = _session.asStateFlow()

    init {
        LegacySessionStorage.purge(appContext)
    }

    suspend fun restoreSession() {
        LegacySessionStorage.purge(appContext)
        if (!firebaseEnabled) {
            clearSession()
            return
        }
        val user = auth.currentUser ?: run {
            clearSession()
            return
        }
        bindSession(user.uid) ?: run {
            logout()
            return
        }
    }

    /** Re-validates role and assignments from Firestore when connectivity returns (AUTHZ-12). */
    suspend fun refreshSessionBinding() {
        if (!firebaseEnabled || auth.currentUser == null) return
        if (!networkMonitor.isCurrentlyOnline()) return
        val uid = auth.currentUser!!.uid
        val refreshed = bindSession(uid) ?: run {
            logout()
            return
        }
        val current = _session.value
        if (current == null ||
            current.staffId != refreshed.staffId ||
            current.role != refreshed.role ||
            current.assignedPropertyIds != refreshed.assignedPropertyIds
        ) {
            Log.i(TAG, "Session binding refreshed from Firestore for ${refreshed.email}")
            _session.value = refreshed
            pullRemoteDataWithRetry(refreshed)
        }
    }

    private suspend fun bindSession(uid: String): StaffSession? {
        val session = loadFirebaseSession(uid) ?: return null
        _session.value = session
        pullRemoteDataWithRetry(session)
        return session
    }

    private suspend fun awaitFirebaseAuthReady() {
        auth.currentUser?.getIdToken(true)?.await()
    }

    private suspend fun pullRemoteDataWithRetry(session: StaffSession): PullRemoteDataResult {
        awaitFirebaseAuthReady()
        val first = runCatching { syncService.pullRemoteData(session) }.getOrElse { error ->
            Log.w(TAG, "Initial Firestore pull failed for ${session.email}", error)
            return PullRemoteDataResult(errors = listOf(error.message ?: "Sync failed"))
        }
        if (first.hasData || first.errors.isEmpty()) {
            return first
        }
        Log.w(TAG, "Retrying Firestore pull for ${session.email}: ${first.errors}")
        delay(750)
        awaitFirebaseAuthReady()
        return runCatching { syncService.pullRemoteData(session) }.getOrElse { error ->
            Log.w(TAG, "Firestore pull retry failed for ${session.email}", error)
            first.copy(errors = first.errors + (error.message ?: "Sync failed"))
        }
    }

    suspend fun login(email: String, password: String): Result<StaffSession> {
        if (!firebaseEnabled) {
            return Result.failure(IllegalStateException("Firebase is not configured"))
        }
        return loginWithFirebase(email, password)
    }

    fun logout() {
        if (firebaseEnabled) auth.signOut()
        clearSession()
    }

    fun currentSession(): StaffSession? = _session.value

    fun currentFirebaseUid(): String? = auth.currentUser?.uid

    fun patchSessionDisplayName(displayName: String) {
        val current = _session.value ?: return
        _session.value = current.copy(displayName = displayName.trim())
    }

    suspend fun sendPasswordResetEmail(): Result<Unit> {
        if (!firebaseEnabled) {
            return Result.failure(IllegalStateException("Firebase is not configured"))
        }
        val email = _session.value?.email?.trim().orEmpty()
        if (email.isBlank()) {
            return Result.failure(IllegalStateException("Not signed in"))
        }
        return runCatching {
            auth.sendPasswordResetEmail(email).await()
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(mapFirebaseError(it)) }
        )
    }

    private suspend fun loginWithFirebase(email: String, password: String): Result<StaffSession> {
        return runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            awaitFirebaseAuthReady()
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Firebase sign-in succeeded without a user")
            val session = bindSession(uid)
                ?: throw IllegalArgumentException("No staff profile linked to this account")
            LegacySessionStorage.purge(appContext)
            session
        }.fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(mapFirebaseError(it)) })
    }

    private suspend fun loadFirebaseSession(uid: String): StaffSession? {
        val online = networkMonitor.isCurrentlyOnline()
        val profile = when {
            online -> {
                runCatching { firestore.getStaffByUid(uid) }.getOrElse { error ->
                    Log.w(TAG, "Firestore staff lookup failed while online for uid=$uid", error)
                    return null
                } ?: run {
                    Log.w(TAG, "No Firestore staff profile for uid=$uid while online")
                    return null
                }
            }
            else -> {
                runCatching { firestore.getStaffByUidFromCache(uid) }.getOrNull()
                    ?: loadLocalStaffProfile(uid)
                    ?: run {
                        Log.w(TAG, "No cached staff profile for uid=$uid while offline")
                        return null
                    }
            }
        }
        return profileToSession(profile, uid)
    }

    private suspend fun loadLocalStaffProfile(uid: String): StaffProfile? {
        val staff = database.staffDao().findByFirebaseUid(uid) ?: return null
        if (!staff.isActive || staff.firebaseUid != uid) return null
        Log.w(TAG, "Using local staff cache for uid=$uid (offline — re-sync when online)")
        val assigned = if (staff.role == StaffRole.CHAIN_ADMIN.name) emptyList()
        else database.staffDao().assignedPropertyIds(staff.id)
        return StaffProfile(
            firebaseUid = uid,
            staffId = staff.id,
            email = staff.email,
            displayName = staff.displayName,
            role = staff.role,
            assignedPropertyIds = assigned
        )
    }

    private suspend fun profileToSession(profile: StaffProfile, uid: String): StaffSession? {
        if (profile.firebaseUid != uid) return null
        syncService.cacheStaffProfile(profile)
        val local = database.staffDao().findById(profile.staffId)
        if (local != null && !local.isActive) return null
        if (local != null && local.firebaseUid != uid) return null
        return buildSession(
            profile.staffId,
            profile.email,
            profile.displayName,
            profile.role,
            assignedPropertyIds = profile.assignedPropertyIds.toSet()
        )
    }

    private suspend fun buildSession(
        staffId: Long,
        email: String,
        displayName: String,
        roleName: String,
        assignedPropertyIds: Set<Long>? = null
    ): StaffSession? {
        val role = runCatching { StaffRole.valueOf(roleName) }.getOrNull() ?: return null
        val assigned = assignedPropertyIds ?: if (role == StaffRole.CHAIN_ADMIN) emptySet()
        else database.staffDao().assignedPropertyIds(staffId).toSet()
        return StaffSession(staffId, email, displayName, role, assigned)
    }

    private fun clearSession() {
        LegacySessionStorage.purge(appContext)
        _session.value = null
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
        private const val TAG = "AuthRepository"
    }
}
