# Guesthouse Booking (Android)

Staff-only Android app for a small guesthouse chain (10+ properties). Property managers book on behalf of guests — no guest login or payments.

## Documentation

The wiki lives in the repo (always available):

- **[Browse wiki on GitHub](https://github.com/pawan-workato/guesthouse-booking/tree/main/docs/wiki)** — chain overview, property guides, staff procedures, FAQ
- **[Backend architecture (Ktor API)](docs/backend-architecture.md)** — self-hosted API, sync, RBAC; Firebase optional
- **[Firebase setup guide](docs/firebase-setup.md)** — console steps, staff seeding, rules deploy

The [GitHub Wiki tab](https://github.com/pawan-workato/guesthouse-booking/wiki) mirrors `docs/wiki/` automatically on every push to `main` (GitHub Actions workflow **Sync Wiki**). You can also run `./scripts/publish-github-wiki.sh` locally.

## Features

- **Properties** — searchable list of chain locations; chain admins can add, edit, and remove properties
- **Rooms** — per-property room inventory with pricing and capacity
- **Availability** — booked and blocked dates; block/unblock from room detail
- **Guests** — add and maintain guest profiles (name, phone, email, notes)
- **Book** — staff picks a saved guest or enters details manually, then selects dates and room
- **Bookings admin** — view and cancel bookings across all properties
- **Staff management** — chain admins add/edit/remove property managers and assign properties
- **Cloud sync** — Firebase Auth + Firestore with offline-first Room cache

## Backend

Primary sync uses the **Ktor REST API** (`backend/`) with PostgreSQL. Room (SQLite) is the on-device cache; guests and bookings queue as `PENDING_SYNC` when offline and flush via `SyncRepository` / WorkManager when online.

### Backend setup (Ktor + Docker)

1. Start Postgres and API:

```bash
docker compose up
```

API listens on **http://localhost:8080** (health and routes under `/api/...`).

2. Alternatively, run the API from Gradle (Postgres must be running, e.g. via compose):

```bash
./gradlew :backend:run
```

3. **Android emulator** — debug builds use `API_BASE_URL` **`http://10.0.2.2:8080/`** (maps to host localhost).

4. **Demo credentials** (seeded in backend and local Room):

| Role | Email | Password |
|------|-------|----------|
| Chain admin | `admin@chain.com` | `admin123` |
| Property manager | `manager.mountain@chain.com` | `manager123` |

Sign in while online to obtain a JWT and pull bootstrap data (properties, rooms, guests, bookings).

### Firebase (optional legacy cloud)

When `google-services.json` is present, Firebase Auth + Firestore remain available if `USE_KTOR_API` is disabled. See [docs/firebase-setup.md](docs/firebase-setup.md).

Without `google-services.json`, use the Ktor API above (`docker compose up`) for auth and bootstrap data. Room is an offline cache only — there is no bundled offline seed in the app.

## Requirements

- Android Studio Ladybug or newer
- Android SDK 36
- JDK 17–21

## Run

1. Open this folder in **Android Studio**
2. Add `app/google-services.json` from Firebase Console (optional for local-only dev)
3. Sync Gradle and run on an emulator or device (API 26+)

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew assembleDebug
```


## Testing

Automated tests cover unit logic, Room DAOs, Compose UI, and CI on push/PR to `main`.

| Layer | Location | Command |
|-------|----------|---------|
| Unit tests | `app/src/test/` | `./gradlew testDebugUnitTest` |
| Instrumented (Room + Compose) | `app/src/androidTest/` | `./gradlew connectedDebugAndroidTest` (device/emulator required) |

**Unit tests** use JUnit 4, MockK, kotlinx-coroutines-test, and Turbine for repository and ViewModel coverage (auth, bookings, guests, properties, RBAC).

**Instrumented tests** use an in-memory Room database for DAO queries and a Compose UI test for the login screen (login UI test is `@Ignore` on API 36 emulators).

**CI:** GitHub Actions runs unit tests and instrumented tests on push/PR to `main` (see `.github/workflows/android-test.yml`). Instrumented tests run on an API 34 emulator via `reactivecircus/android-emulator-runner`.

## Roadmap

- Sprint 2: Staff login + property-scoped access ✅
- Sprint 3: Offline booking with sync queue ✅
- Sprint 4: Property management (chain admin) ✅
- Sprint 5: Guest profiles ✅
- Sprint 6: Firebase backend (Auth + Firestore sync) ✅
- Sprint 7: Check-in/out, block dates ✅, today board

## GitHub

https://github.com/pawan-workato/guesthouse-booking
