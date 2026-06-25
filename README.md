# Guesthouse Booking (Android)

Staff-only Android app for a small guesthouse chain (10+ properties). Property managers book on behalf of guests — no guest login or payments.

## Documentation

The wiki lives in the repo (always available):

- **[Browse wiki on GitHub](https://github.com/pawan-workato/guesthouse-booking/tree/main/docs/wiki)** — chain overview, property guides, staff procedures, FAQ

The [GitHub Wiki tab](https://github.com/pawan-workato/guesthouse-booking/wiki) mirrors `docs/wiki/` automatically on every push to `main` (GitHub Actions workflow **Sync Wiki**). You can also run `./scripts/publish-github-wiki.sh` locally.

## Features

- **Properties** — searchable list of all chain locations (12 seeded)
- **Rooms** — per-property room inventory with pricing and capacity
- **Availability** — calendar showing booked dates per room
- **Book** — staff enters guest name, phone, email, dates, and room
- **Bookings admin** — view and cancel bookings across all properties

Data is stored locally with Room (SQLite). Database resets on schema upgrade during development.

## Requirements

- Android Studio Ladybug or newer
- Android SDK 36
- JDK 17–21

## Run

1. Open this folder in **Android Studio**
2. Sync Gradle and run on an emulator or device (API 26+)

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew assembleDebug
```

## Roadmap

- Sprint 2: Staff login + property-scoped access
- Sprint 3: Offline booking with sync queue
- Sprint 4: Check-in/out, block dates, today board

## GitHub

https://github.com/pawan-workato/guesthouse-booking
