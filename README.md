# Guesthouse Booking (Android)

Staff-only Android app for a small guesthouse chain (10+ properties). Property managers book on behalf of guests — no guest login or payments.

## Documentation

- **[Guesthouse wiki](docs/wiki/README.md)** — chain overview, property details, staff procedures, and FAQ

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
