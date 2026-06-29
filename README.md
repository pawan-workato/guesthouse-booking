# Guesthouse Booking (Android)

Staff-only Android app for a small guesthouse chain (12 properties). Property managers book on behalf of guests — no guest login or payments.

## Documentation

The wiki lives in the repo (always available):

- **[Browse wiki on GitHub](https://github.com/pawan-workato/guesthouse-booking/tree/main/docs/wiki)** — chain overview, property guides, staff procedures, FAQ
- **[Firebase setup guide](docs/firebase-setup.md)** — console steps, staff seeding, rules deploy
- **[Seeding guide](docs/seeding.md)** — demo Auth users and Firestore data
- **[Project tracker](docs/TRACKER.md)** — security backlog, sprint status, open bugs (update when items are fixed)

The [GitHub Wiki tab](https://github.com/pawan-workato/guesthouse-booking/wiki) mirrors `docs/wiki/` on every push to `main` (workflow **Sync Wiki**). Run `./scripts/publish-github-wiki.sh` locally to publish by hand.

## Features

- **Properties** — searchable list with Overview occupancy cards; chain admins add, edit, and deactivate properties
- **Rooms** — per-property inventory with **room type** (Single, Double, Suite, Family), price, and capacity; staff with property access can add and edit rooms
- **Availability** — calendar shows booked and **blocked** dates; block/unblock from room detail (syncs to Firestore)
- **Guests** — profiles with search; all managers can edit any guest; only chain admins remove guests; duplicate detection (read-only unless linked to their properties); chain admins full edit
- **Book** — search properties and rooms (including by type), pick dates, saved guest or manual entry
- **Today** — arrivals, departures, and in-house board with check-in / check-out actions
- **Bookings** — list, edit, cancel, check-in/out; sync status and conflict resolution
- **Staff** (chain admin) — add managers and assign properties
- **Sync** — toolbar **sync** icon (badge when pending/conflicts); background WorkManager every ~15 min when online
- **Notifications** — daily ~7 AM arrival summary (WorkManager)
- **Offline** — bookings queue as `PENDING_SYNC` with `TMP-xxxx` references until sync

## Backend

The app uses **Firebase Auth + Firestore** for cloud sync when `app/google-services.json` is present. **Room (SQLite)** is always the on-device cache. Without `google-services.json`, the app process starts for tests but full init is skipped — add Firebase config for normal development.

### Demo credentials

Seeded via `npm run seed` in `scripts/`:

| Role | Email | Password |
|------|-------|----------|
| Chain admin | `admin@chain.com` | Set via `SEED_ADMIN_PASSWORD` when seeding |
| Property manager | `manager.mountain@chain.com` | Set via `SEED_MANAGER_PASSWORD` when seeding |

See [staff guide](docs/wiki/staff-guide.md#demo-accounts-development) for all five manager accounts.

## Requirements

- Android Studio Ladybug or newer
- Android SDK 36
- JDK 17–21 (Android Studio bundled JBR on macOS)

## Run

**Quick path (emulator):**

```bash
./scripts/run-on-emulator.sh --fresh
```

**Manual:**

1. Open the project in Android Studio
2. Add `app/google-services.json` and run `npm run seed` in `scripts/`
3. Run on emulator or device (API 26+)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:installDebug
```

## Testing

**JDK:** Android Gradle Plugin 8.9 requires **JDK 17–21** to run Gradle. If you see `26.0.1`, your system Java is too new. Run:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew --stop
```

On macOS, `./gradlew` also auto-selects Android Studio’s JBR when `JAVA_HOME` is unset or points at Java 26+.

| Layer | Command |
|-------|---------|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Instrumented (Room DAO) | `./gradlew :app:connectedDebugAndroidTest` — requires device/emulator |
| Instrumented (Compose UI) | Same command — `app/src/androidTest/.../ui/*UiTest.kt` |

**CI:** `.github/workflows/android-test.yml` — unit tests + API 34 emulator instrumented tests on push/PR to `main`.

## Roadmap (shipped)

- Sprint 2: Staff login + property-scoped access ✅
- Sprint 3: Offline booking + sync queue ✅
- Sprint 4: Property management (chain admin) ✅
- Sprint 5: Guest profiles ✅
- Sprint 6: Firebase Auth + Firestore sync ✅
- Sprint 7: Today board, check-in/out, block dates, edit bookings, room types, room CRUD, booking search ✅

## GitHub

https://github.com/pawan-workato/guesthouse-booking
