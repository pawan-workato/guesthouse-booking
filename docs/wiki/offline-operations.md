# Offline operations

## Overview

Staff can create bookings offline. They save locally as **PENDING SYNC** with reference `TMP-xxxx` and still block the room calendar. When online, **SyncRepository** + WorkManager push to Firebase.

Tap the **sync icon** in the top app bar to open **Sync status** (online/offline, last sync, pending uploads, conflicts, **Sync now**). The badge shows pending + conflict count. Background sync runs about every **15 minutes** when the network is available.

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
2. Open **Sync status** from the top-bar sync icon → **Sync now** (or wait for background sync).
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
| **Sync status** (top-bar sync icon) | Online/offline, last sync, pending list, conflicts, **Sync now** |
| Room calendar | Pending bookings block dates like confirmed stays |

## Block dates

**Room detail** → **Block dates**. Blocks sync to Firestore like bookings: offline creates use **PENDING SYNC** and upload on the next sync; removals while offline are queued and deleted from the server when online.

## Technical notes

- Room database **version 11** (`block_dates` with `syncStatus` / `markedForDeletion`, `roomType` on rooms, guests/bookings sync fields).
- Migrations: `MIGRATION_8_9` (room types), `MIGRATION_9_10` (block dates table), `MIGRATION_10_11` (block date sync fields). `fallbackToDestructiveMigration` still enabled for uncovered version jumps — reinstall or clear data if upgrade fails.
- Overlap checks exclude **CONFLICT** bookings from blocking new entries incorrectly.
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`.

## Demo / test

1. Sign in as `manager.mountain@chain.com` with your seeded manager password (`SEED_MANAGER_PASSWORD`).
2. Airplane mode → book on **Book** → expect TMP reference.
3. Online → **Sync status** → **Sync now** → **SYNCED** / `GH-*` reference.

**Conflict test:** book room online for dates X–Y; offline overlapping booking on same room; sync → second becomes **CONFLICT**; cancel from **Bookings**.
