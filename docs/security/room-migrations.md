# Room database migrations

## Current state (v9)

The app uses Room database version **9** after room type inventory. `AppDatabase` uses `AppDatabaseMigrations.MIGRATION_8_9` for upgrades; older versions still rely on development-time destructive rebuilds until full migration chain is added (KR-10).

## Version 9 — room type inventory

Adds `roomType` (`SINGLE`, `DOUBLE`, `SUITE`, `FAMILY`) to each room row. Migration `MIGRATION_8_9` backfills from name/capacity. Synced via Ktor `RoomDto`, Firestore `roomType`, and PostgreSQL `rooms.room_type`.

## Pre-pilot (KR-10)

Before pilot deployment, add explicit `Migration` objects for each version step and remove destructive fallback so staff devices retain local cache across app updates.

## Password hashing (KR-01)

Staff passwords now use **BCrypt** (cost 12). Legacy SHA-256 hashes from seed data remain verifiable via `PasswordHasher.verify()` and are upgraded to BCrypt on successful Ktor API login.

## Backup (KR-03)

`android:allowBackup` is set to **false** to prevent PII extraction via ADB backup.
