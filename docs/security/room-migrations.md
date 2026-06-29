# Room database migrations

## Current state (v11)

`AppDatabase` version **11**. Registered migrations: `MIGRATION_8_9`, `MIGRATION_9_10`, `MIGRATION_10_11` in `AppDatabaseMigrations.kt`.

`fallbackToDestructiveMigration` is gated behind `BuildConfig.DEBUG` only — **release builds** will fail on unregistered version jumps rather than silently wiping data. Debug builds retain the destructive fallback for developer convenience.

## Encryption (KR-13, Sprint 8–9)

Room uses **SQLCipher** via `net.zetetic:sqlcipher-android` and `SupportOpenHelperFactory`.

| Component | Location | Role |
|-----------|----------|------|
| Passphrase storage | `DatabaseKeyManager.kt` | 32-byte random key in `EncryptedSharedPreferences` (`guesthouse_db_key`), Keystore-backed `MasterKey` |
| Plaintext upgrade | `PlaintextDatabaseMigrator.kt` | One-time export from legacy `guesthouse.db` |
| Database open | `AppDatabase.buildDatabase()` | Loads native lib, migrates if needed, opens encrypted Room |

### First install

1. `DatabaseKeyManager` generates a 32-byte passphrase and stores it Base64-encoded in EncryptedSharedPreferences.
2. Room creates a new encrypted `guesthouse.db`.

### Upgrade from plaintext (existing installs)

On first launch after this release:

1. Detect plaintext file via standard SQLite header (`SQLite format 3`).
2. Create temporary encrypted database (`guesthouse.db.encrypting`).
3. `ATTACH` plaintext DB with empty key; `SELECT sqlcipher_export('main', 'plaintext')`.
4. Delete plaintext file and `-wal` / `-shm` / `-journal` sidecars; rename temp file to `guesthouse.db`.

If migration fails, the app throws on startup (no silent data loss). Staff can sign in again to re-sync from Firestore.

### Validation

- `adb shell run-as com.guesthouse.booking cat databases/guesthouse.db | head -c 16` should **not** show `SQLite format 3`.
- Standard `sqlite3 guesthouse.db` on a copied file should fail without the passphrase.

## Version 8 → 9 — room types

Adds `roomType` (`SINGLE`, `DOUBLE`, `SUITE`, `FAMILY`) with backfill from name/capacity. Indexed on `roomType`.

Synced via Firestore `roomType`.

## Version 9 → 10 — block dates

Creates `block_dates` table:

- `propertyId`, `roomId`, `startEpochDay`, `endEpochDay`, `reason`, `createdByStaffId`, `createdAtEpochMs`

## Version 10 → 11 — block date sync

Adds Firestore sync fields to `block_dates`:

- `syncStatus` (default `PENDING_SYNC` for existing rows so they upload on next sync)
- `markedForDeletion` (soft-delete tombstone for offline removals)
- Index on `syncStatus`

## Security (shipped)

- **KR-01:** Staff passwords use BCrypt (cost 12) for local staff records.
- **KR-03:** `android:allowBackup="false"`.
- **KR-13:** SQLCipher-encrypted Room; passphrase in EncryptedSharedPreferences.

## Pre-pilot (KR-10)

✅ Release builds no longer use destructive fallback (debug-only). Add explicit migrations for any future version bumps before rollout.
