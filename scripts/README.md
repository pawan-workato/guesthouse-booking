# Firebase demo seed script

Creates demo **Firebase Auth users**, **Firestore staff documents**, and (when collections are empty) **properties, rooms, and guests**.

If your org **blocks service account key downloads** (Firebase sends you to IAM), use **Option A** or **Option C** below.

---

## Option A — gcloud login (no key file) **recommended**

Uses your Google account instead of a downloaded JSON key.

**1. Install Google Cloud CLI** (if needed): https://cloud.google.com/sdk/docs/install

**2. Log in and set project**

```bash
gcloud auth application-default login
export GOOGLE_CLOUD_PROJECT=guesthouse-booking-dev
```

Use your real project ID from `app/google-services.json` → `project_info.project_id`.

**3. Run seed**

```bash
cd scripts
npm run seed
```

You need **Firebase Admin** or **Owner** on the project (ask your GCP admin if permission denied).

---

## Option B — service account JSON key

Only if your org allows key creation:

1. Google Cloud Console → **IAM & Admin** → **Service Accounts** → `firebase-adminsdk-…` → **Keys** → **Add key**
2. Save as `scripts/serviceAccountKey.json` (gitignored)
3. `cd scripts && npm run seed`

---

## Option C — Firebase Console only (no script)

Use when you cannot run the script.

### Step 1 — Create admin user

Firebase Console → **Build → Authentication → Users → Add user**

- Email: `admin@chain.com`
- Password: value you set in `SEED_ADMIN_PASSWORD` (see `scripts/.env.example`)

### Step 2 — Create admin staff document

1. Copy the **User UID** for `admin@chain.com`.
2. **Build → Firestore →** collection `staff`, document ID = **that UID**
3. Fields:

| Field | Type | Value |
|-------|------|-------|
| `staffId` | number | `1` |
| `email` | string | `admin@chain.com` |
| `displayName` | string | `Chain Admin` |
| `role` | string | `CHAIN_ADMIN` |
| `assignedPropertyIds` | array | *(empty)* |
| `firebaseUid` | string | same as document ID |

### Step 3 — Properties, rooms, guests

Re-run `npm run seed` after wiping empty collections, or import manually in Firestore Console (see `scripts/seed-data.mjs`).

### Step 4 — More users (optional)

Add other Auth users and `staff/{uid}` docs using [seed-firestore-staff.md](seed-firestore-staff.md).

---

## After seeding

Deploy rules: `npx -y firebase-tools@latest deploy --only firestore:rules`  
Login with the emails below and the passwords from your `scripts/.env` (not committed).

Run the app: `../scripts/run-on-emulator.sh` from repo root (or `./gradlew :app:installDebug`).
