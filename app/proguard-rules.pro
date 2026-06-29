# ─── Room — keep entity classes and their field names ───────────────────────
# Room KSP generates code referencing entity fields by name at compile time,
# but also accesses them via reflection in some paths.
-keep class com.guesthouse.booking.data.local.entities.** { *; }

# ─── Enum values stored as strings in Room columns ──────────────────────────
# BookingStatus, SyncStatus, RoomType, StaffRole are stored/retrieved by name
-keepclassmembers enum com.guesthouse.booking.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── SQLCipher — JNI bridge ─────────────────────────────────────────────────
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.**

# ─── Bcrypt — reflective class loading ──────────────────────────────────────
-keep class at.favre.lib.** { *; }
-dontwarn at.favre.lib.**

# ─── Firebase (supplemental — Firebase AAR ships its own consumer rules) ────
-keepattributes Signature
-keepattributes *Annotation*

# ─── Kotlin data classes used in UI state ───────────────────────────────────
-keepclassmembers class com.guesthouse.booking.viewmodel.** { *; }

# ─── Kotlin coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ─── WorkManager worker class — loaded by name by WorkManager runtime ────────
-keep class com.guesthouse.booking.worker.** { *; }

# ─── Suppress noisy warnings from transitive deps ───────────────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
