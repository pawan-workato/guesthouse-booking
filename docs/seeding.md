# Seeding Firestore demo data

## Recommended: automated script

Creates everything in one command — Auth users, staff profiles, and entity data:

```bash
cd scripts
npm install
npm run seed
```

See [scripts/README.md](../scripts/README.md) for service account setup.

The script always creates/updates the 5 demo staff accounts. Properties, rooms, and guests upload only when the `properties` collection is empty.

## Alternative: in-app upload (properties/rooms/guests only)

Use this if you already ran the script for staff but want to push entity data from the device, or if you created staff manually.

1. Complete [firebase-setup.md](firebase-setup.md) and ensure demo Auth users + `staff/{uid}` docs exist (via script or manual).
2. Build and install: `./gradlew assembleDebug`
3. Sign in as **chain admin** (`admin@chain.com` / `admin123`)
4. Open **Sync** → tap **Upload demo seed (properties, rooms, guests)**

Firestore `properties` must be empty. The app uploads **12 properties, 30 rooms, and 5 guests** only — staff are handled by the script.

**Success example:** `Uploaded 47 documents: 12 properties, 30 rooms, 5 guests`

**Failure (data exists):** delete documents in `properties`, `rooms`, and `guests` in Firebase Console, then retry.

## Overview

| Collection   | Count | How to seed |
|-------------|-------|-------------|
| staff       | 5     | `scripts/seed-firebase-demo.mjs` (Auth + Firestore) |
| properties  | 12    | Script (if empty) or in-app upload |
| rooms       | 30    | Script (if empty) or in-app upload |
| guests      | 5     | Script (if empty) or in-app upload |

## Manual fallback

Without a service account:

1. Create Auth users in Firebase Console (see [scripts/seed-firestore-staff.md](../scripts/seed-firestore-staff.md))
2. Add `staff/{firebaseUid}` documents with the JSON templates in that file
3. Upload properties/rooms/guests via the in-app button or import JSON manually

## Adding new staff from the app

**Staff → Add manager** saves to local Room only. It does **not** create a Firebase Auth user.

For Firebase sign-in, new managers need:

- This seed script (add entries to `scripts/seed-data.mjs` and re-run), or
- A future Cloud Function (not implemented yet)

The add-manager form shows a reminder when Firebase is configured.

## Verify

1. Firebase Console → Authentication: 5 demo users
2. Firestore → `staff`: 5 documents keyed by UID
3. Sign in as admin and as a property manager; confirm assigned properties appear

## Single source of truth

Demo entity definitions:

- Firebase script: `scripts/seed-data.mjs` (used by `npm run seed`)
- Ktor backend: `backend/src/main/kotlin/com/guesthouse/booking/backend/seed/DatabaseSeeder.kt` (PostgreSQL on first API start)

## Wiping Firestore

Delete all documents in: `properties`, `rooms`, `guests`, `staff`. Bookings are not seeded.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "No staff profile linked" on login | Run `npm run seed` in `scripts/` |
| Upload button not visible | Need `google-services.json`, chain admin, online |
| Permission denied | Deploy `firestore.rules`; sign in as chain admin |
| Script: no service account | Follow [scripts/README.md](../scripts/README.md) |
