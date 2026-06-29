# Firebase setup

## 1. Create a Firebase project

1. Open [Firebase Console](https://console.firebase.google.com/)
2. **Add project** → name it (e.g. `guesthouse-booking-dev`)
3. Disable Google Analytics if not needed

## 2. Register the Android app

1. Project overview → **Add app** → Android
2. Package name: `com.guesthouse.booking`
3. Download `google-services.json`
4. Copy to `app/google-services.json` (this path is gitignored)

Use `app/google-services.json.example` as a reference for the expected shape.

## 3. Enable services

### Authentication

1. **Build → Authentication → Sign-in method**
2. Enable **Email/Password**

Demo users are created by the seed script (step 4), not manually in the Console.

### Firestore

1. **Build → Firestore Database → Create database**
2. Start in **production mode** (rules are in `firestore.rules`)
3. Choose a region close to your users

Deploy rules from the repo root:

```bash
npx -y firebase-tools@latest login
npx -y firebase-tools@latest use --add   # select your project
npx -y firebase-tools@latest deploy --only firestore:rules
```

**Manual deploy (Console):** If CLI login fails, open **Firestore → Rules** in the Firebase Console, paste the contents of `firestore.rules` from this repo, and click **Publish**.

## 4. Seed demo data (recommended)

Run the automated seed script — it creates Auth users, Firestore staff docs, and (if empty) properties, rooms, and guests:

```bash
cd scripts
npm install
npm run seed
```

See [scripts/README.md](../scripts/README.md) for the 3-step guide (download service account key → place file → run).

**Demo logins after seeding** (passwords from `scripts/.env`, min 12 characters):

| Email | Password source |
|-------|-----------------|
| admin@chain.com | `SEED_ADMIN_PASSWORD` |
| manager.mountain@chain.com | `SEED_MANAGER_PASSWORD` |
| manager.coastal@chain.com | `SEED_MANAGER_PASSWORD` |
| manager.southwest@chain.com | `SEED_MANAGER_PASSWORD` |
| manager.east@chain.com | `SEED_MANAGER_PASSWORD` |

### Manual fallback

If you cannot use a service account, create Auth users and `staff/{uid}` documents manually — see [seeding.md](seeding.md) and [scripts/seed-firestore-staff.md](../scripts/seed-firestore-staff.md).

## 5. Build and run

```bash
./scripts/run-on-emulator.sh
```

Or `./gradlew assembleDebug`, then run from Android Studio (API 26+). Sign in with a demo account from step 4.

Entity data is seeded by **`npm run seed`**. There is no in-app Firestore upload button.

## Firestore guest rules

The `guests` collection in `firestore.rules`:

| Operation | Who |
|-----------|-----|
| **Read** | Any signed-in staff member (`staff/{uid}` doc must exist) — managers see the full chain guest list |
| **Create / update** | Any staff member |
| **Soft-delete (`isActive`)** | Chain admin only — enforced in Firestore rules (`isActive` must not change unless `isChainAdmin()`) and in the app (`GuestRepository.canDeleteGuest`) |
| **Delete** | Chain admin only |

Deploy rules after changes: `npx firebase-tools deploy --only firestore:rules`.

## Security notes

- Never commit `app/google-services.json` or `scripts/serviceAccountKey.json`
- Deploy `firestore.rules` before production use
- Rotate demo passwords outside development
