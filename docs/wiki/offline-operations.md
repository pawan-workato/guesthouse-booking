# Offline operations

> **Status:** Implemented in Sprint 3 — offline staff booking with local sync queue.

## Overview

Staff can create bookings without a live network connection. Bookings are saved locally with a temporary reference and marked **PENDING SYNC**. When connectivity returns, the app syncs automatically (WorkManager) or on demand from the **Sync** tab.

Each device still uses a local Room database. Sprint 3 adds sync *status* and conflict handling on-device — there is no remote server yet. Sync marks bookings as **SYNCED** and assigns permanent references; conflicts are flagged when overlapping **SYNCED** bookings exist at sync time.

## Sync statuses

| Status | Meaning |
|--------|---------|
| **PENDING SYNC** | Created offline (or awaiting sync). Reference: `TMP-xxxx`. Calendar treats as booked. |
| **SYNCED** | Confirmed locally. Online bookings sync immediately; offline bookings after successful sync. Reference: `GH-{propertyId}-{id}`. |
| **CONFLICT** | Sync found overlapping dates with another synced booking. Staff must cancel from Sync tab. |

## Staff workflows

### Create booking offline

1. Enable airplane mode (or lose connectivity).
2. Sign in and open **Book**.
3. An orange banner shows: *Offline — bookings will sync when you're back online*.
4. Complete the booking form and submit.
5. Success message: *Saved offline — will sync when online. Ref: TMP-xxxx*.
6. The booking appears in **Bookings** with sync status **PENDING SYNC** and blocks the calendar like any confirmed stay.

### Sync when back online

1. Disable airplane mode.
2. Open the **Sync** tab (badge shows pending + conflict count).
3. Tap **Sync now**, or wait for automatic background sync (~15 min when network available).
4. Pending bookings become **SYNCED** with `GH-{propertyId}-{id}` references, unless dates conflict.

### Resolve conflicts

If two bookings overlap the same room and dates (e.g. one created offline, one already synced):

1. **Sync** tab lists the conflict with guest and dates.
2. Tap **Cancel this booking** to dismiss the conflicting offline booking.
3. The other booking remains confirmed.

## UI locations

| Screen | What to look for |
|--------|------------------|
| **Book** | Offline banner when no network |
| **Bookings** | Reference, sync status per booking |
| **Sync** | Online/offline chip, last sync time, pending list, conflicts, manual sync |
| **Bottom nav** | Sync tab badge = pending + conflicts for your properties |

## Demo logins

Same as main app — e.g. `manager.mountain@chain.com` / `manager123` for properties 1, 3, 7, 11.

## Testing offline mode (airplane mode)

1. Install debug build on device or emulator.
2. Sign in as a property manager.
3. Enable **Airplane mode**.
4. Create a booking on **Book** — expect offline banner and TMP reference.
5. Check **Bookings** — status **PENDING SYNC**; room calendar shows dates blocked.
6. Disable airplane mode → **Sync** tab → **Sync now**.
7. Booking should show **SYNCED** and `GH-*` reference.

### Conflict test

1. While online, book Room A for dates X–Y.
2. Enable airplane mode; book the same room for overlapping dates.
3. Go online and sync — second booking becomes **CONFLICT**.
4. Cancel from Sync tab.

## Technical notes

- Database schema v4: `syncStatus`, `bookingReference`, `createdAtEpochMs` on bookings.
- `findOverlapping` excludes **CONFLICT** bookings so failed syncs don't block new bookings incorrectly.
- WorkManager: one-time sync after offline save; periodic sync every 15 minutes when connected.
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`.

See the [root README](../../README.md) for sprint history.
