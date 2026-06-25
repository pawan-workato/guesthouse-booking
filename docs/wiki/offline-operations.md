# Offline operations

> **Status:** Placeholder for Sprint 3 — offline booking with sync queue. Behavior described here is planned, not yet implemented.

## What staff should know (planned)

When offline mode ships, property managers will be able to create and cancel bookings without a live network connection. Changes will queue locally and sync when connectivity returns.

### Expected behavior

| Scenario | Planned staff experience |
|----------|-------------------------|
| No network at check-in | Create booking offline; app queues it for sync |
| Sync succeeds | Booking appears on all devices; calendar updates chain-wide |
| Sync conflict | App flags overlapping booking created elsewhere; staff resolves manually |
| Long offline period | Queue shows pending actions; do not assume other properties see your changes until synced |

### Until Sprint 3 ships

- The app stores all data **locally on the device** (Room/SQLite).
- There is **no cloud sync** today — each device has its own database.
- Bookings made on one device are **not visible** on another.
- Treat the current app as a single-device tool until offline/sync is delivered.

### Preparation checklist (for managers)

1. Designate one primary device per property for booking during the transition.
2. Export or screenshot critical bookings if switching devices (manual workaround until sync exists).
3. Watch release notes for Sprint 3 before relying on offline queue behavior.

See the [root README](../../README.md) roadmap for sprint timeline.
