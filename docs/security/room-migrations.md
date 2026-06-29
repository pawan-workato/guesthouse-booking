# Room database migrations

## Version 9 — room type inventory

Adds `roomType` (`SINGLE`, `DOUBLE`, `SUITE`, `FAMILY`) to each room row. Migration `MIGRATION_8_9` backfills from name/capacity. Synced via Ktor `RoomDto`, Firestore `roomType`, and PostgreSQL `rooms.room_type`.
