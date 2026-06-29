# Room database migrations

## Current state (v8)

The app uses Room database version **8**. `AppDatabase` still uses `fallbackToDestructiveMigration` for development velocity.

## Pre-pilot (KR-10)

Before pilot deployment, add explicit `Migration` objects for each version step and remove destructive fallback so staff devices retain local cache across app updates.

## Password hashing (KR-01)

Staff passwords now use **BCrypt** (cost 12). Legacy SHA-256 hashes from seed data remain verifiable via `PasswordHasher.verify()` and are upgraded to BCrypt on successful Ktor API login.

## Backup (KR-03)

`android:allowBackup` is set to **false** to prevent PII extraction via ADB backup.
