# Guesthouse Booking (Android)

Kotlin + Jetpack Compose Android app for managing guesthouse room bookings.

## Features (MVP)

- **Rooms** — browse available rooms with pricing and capacity
- **Availability** — calendar view of booked dates per room
- **Book** — select dates, enter guest details, confirm booking
- **Admin** — view and cancel all bookings

Data is stored locally with Room (SQLite). Four sample rooms are seeded on first launch.

## Requirements

- Android Studio Ladybug or newer
- Android SDK 36
- JDK 17+ (Android Studio bundles a compatible JDK)

## Run

1. Open this folder in **Android Studio**
2. Let Gradle sync complete
3. Run on an emulator or device (API 26+)

Or from the command line (requires JDK 17–21):

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew assembleDebug
```

## Project structure

```
app/src/main/java/com/guesthouse/booking/
├── data/          # Room database, DAOs, repository
├── viewmodel/     # Rooms, Booking, Admin ViewModels
├── ui/            # Compose screens, navigation, theme
└── MainActivity.kt
```

## GitHub

Repository: https://github.com/pawan-workato/guesthouse-booking
