# Seeding demo data

## Recommended: Firebase script

Creates Auth users, Firestore `staff` docs, and (when `properties` is empty) properties, rooms, and guests:

```bash
cd scripts
npm install
npm run seed
```

See [scripts/README.md](../scripts/README.md) for `gcloud` or service-account setup.

The script always upserts **5 demo staff** accounts. Entity data uploads only when the `properties` collection is empty.

## Overview

| Data | Count | Firebase |
|------|-------|----------|
| staff | 5 | `npm run seed` |
| properties | 12 | Script if empty |
| rooms | 30 | Script if empty |
| guests | 5 | Script if empty |

Bookings are **not** pre-seeded.

## Manual Firebase fallback

Without the script:

1. Create Auth users in Firebase Console
2. Add `staff/{firebaseUid}` documents — [scripts/seed-firestore-staff.md](../scripts/seed-firestore-staff.md)
3. Import properties/rooms/guests via Console or extend `scripts/seed-data.mjs` and re-run seed

> **Note:** The in-app “Upload demo seed” button was removed. Use the script or manual import.

## Adding staff from the app

**Staff → Add manager** saves to **local Room** only. It does **not** create Firebase Auth credentials.

For sign-in, add users via the seed script or your identity provisioning process.

## Verify (Firebase)

1. Authentication: 5 demo users
2. Firestore `staff`: 5 documents keyed by UID
3. Sign in as admin and as a manager; confirm property scope

## Source of truth

- `scripts/seed-data.mjs` — used by `seed-firebase-demo.mjs`
- `app/.../SeedData.kt` — legacy Kotlin mirror (reference)

## Wiping Firestore

Delete documents in `properties`, `rooms`, `guests`, `staff` (and `bookings` if any). Re-run `npm run seed`.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| No staff profile on login | Run `npm run seed` |
| Permission denied | Deploy `firestore.rules`; sign in as chain admin |
| Script auth errors | [scripts/README.md](../scripts/README.md) — gcloud ADC or service account |
