# Offline operations

## Overview

Staff can create bookings offline. They save locally as **PENDING SYNC** with reference `TMP-xxxx` and still block the room calendar. When online, **SyncRepository** + WorkManager push to Firebase.

Trigger sync manually via the **sync icon** in the top app bar (badge = pending + conflicts). Background sync runs about every **15 minutes** when the network is available.

## Sync statuses

| Status | Meaning |
|--------|---------|
| **PENDING SYNC** | Awaiting upload. Reference `TMP-xxxx`. Calendar treats as booked. |
| **SYNCED** | On server / merged. Reference `GH-{propertyId}-{id}`. |
| **CONFLICT** | Overlap with another synced booking at sync time. Resolve on **Bookings**. |

## Workflows

### Book offline

1. Lose connectivity (or airplane mode).
2. **Book** tab shows an offline banner.
3. Submit booking → *Saved offline — will sync when online. Ref: TMP-xxxx*.
4. **Bookings** shows **PENDING SYNC**.

### Sync when online

1. Restore network.
2. Tap **sync** in the top bar (or wait for background sync).
3. Pending items become **SYNCED** unless dates conflict.

### Resolve conflicts

1. **Bookings** — conflicting row shows **CONFLICT** and explanation.
2. **Cancel this booking** dismisses the conflicting local booking.
3. The other reservation stays confirmed.

## Where to look

| Location | What |
|----------|------|
| **Book** | Offline banner |
| **Bookings** | Reference, sync status, conflict actions |
| **Top bar sync** | Manual sync; badge count |
| Room calendar | Pending bookings block dates like confirmed stays |

## Block dates (local-only)

**Room detail** → **Block dates**. Not synced to cloud backends.

## Technical notes

- Room database **version 10** (`block_dates`, `roomType` on rooms, guests/bookings sync fields).
- Migrations: `MIGRATION_8_9` (room types), `MIGRATION_9_10` (block dates). `fallbackToDestructiveMigration` still enabled for uncovered version jumps — reinstall or clear data if upgrade fails.
- Overlap checks exclude **CONFLICT** bookings from blocking new entries incorrectly.
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`.

## Demo / test

1. Sign in as `manager.mountain@chain.com` with your seeded manager password (`SEED_MANAGER_PASSWORD`).
2. Airplane mode → book on **Book** → expect TMP reference.
3. Online → top-bar **sync** → **SYNCED** / `GH-*` reference.

**Conflict test:** book room online for dates X–Y; offline overlapping booking on same room; sync → second becomes **CONFLICT**; cancel from **Bookings**.
