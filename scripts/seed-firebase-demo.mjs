#!/usr/bin/env node
/**
 * Seeds Firebase Auth users, Firestore staff docs, and (if empty) demo properties/rooms/guests.
 *
 * Credentials (first match wins):
 *   1. GOOGLE_APPLICATION_CREDENTIALS (service account JSON path)
 *   2. scripts/serviceAccountKey.json
 *   3. Application Default Credentials: gcloud auth application-default login
 */

import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import admin from 'firebase-admin';
import { guests, properties, rooms, staffProfiles } from './seed-data.mjs';
import { loadSeedPasswords, passwordForProfile } from './seed-env.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_KEY_PATH = join(__dirname, 'serviceAccountKey.json');
const GOOGLE_SERVICES_PATH = join(__dirname, '..', 'app', 'google-services.json');

function readProjectIdFromGoogleServices() {
  if (!existsSync(GOOGLE_SERVICES_PATH)) return null;
  try {
    const json = JSON.parse(readFileSync(GOOGLE_SERVICES_PATH, 'utf8'));
    return json.project_info?.project_id ?? null;
  } catch {
    return null;
  }
}

function resolveProjectId(explicitFromKey) {
  return (
    explicitFromKey ??
    process.env.GOOGLE_CLOUD_PROJECT ??
    process.env.FIREBASE_PROJECT_ID ??
    process.env.GCLOUD_PROJECT ??
    readProjectIdFromGoogleServices()
  );
}

function resolveCredentialPath() {
  const fromEnv = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (fromEnv && existsSync(fromEnv)) return fromEnv;
  if (existsSync(DEFAULT_KEY_PATH)) return DEFAULT_KEY_PATH;
  return null;
}

function loadServiceAccount(path) {
  const raw = readFileSync(path, 'utf8');
  return JSON.parse(raw);
}

function initializeAdmin() {
  const keyPath = resolveCredentialPath();
  if (keyPath) {
    const serviceAccount = loadServiceAccount(keyPath);
    const projectId = resolveProjectId(serviceAccount.project_id);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId,
    });
    return { projectId, credentialLabel: keyPath };
  }

  const projectId = resolveProjectId(null);
  if (!projectId) {
    throw new Error(
      'No credentials and no project ID.\n' +
        '  Option A: gcloud auth application-default login && export GOOGLE_CLOUD_PROJECT=guesthouse-booking-dev\n' +
        '  Option B: scripts/serviceAccountKey.json\n' +
        '  Option C: manual Console setup — scripts/README.md'
    );
  }

  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId,
  });
  return {
    projectId,
    credentialLabel: `Application Default Credentials (project: ${projectId})`,
  };
}

async function ensureAuthUser(auth, profile, password) {
  try {
    const existing = await auth.getUserByEmail(profile.email);
    console.log(`  ✓ Auth user exists: ${profile.email} (${existing.uid})`);
    return { uid: existing.uid, created: false };
  } catch (error) {
    if (error.code !== 'auth/user-not-found') throw error;
  }

  const created = await auth.createUser({
    email: profile.email,
    password,
    displayName: profile.displayName,
    emailVerified: true,
  });
  console.log(`  + Created Auth user: ${profile.email} (${created.uid})`);
  return { uid: created.uid, created: true };
}

async function upsertStaffDoc(db, uid, profile) {
  const doc = {
    staffId: profile.staffId,
    email: profile.email,
    displayName: profile.displayName,
    role: profile.role,
    assignedPropertyIds: profile.assignedPropertyIds,
    firebaseUid: uid,
  };
  await db.collection('staff').doc(uid).set(doc, { merge: true });
  console.log(`  ✓ staff/${uid} (${profile.role}, staffId=${profile.staffId})`);
}

async function collectionIsEmpty(db, name) {
  const snap = await db.collection(name).limit(1).get();
  return snap.empty;
}

async function seedEntityData(db) {
  const empty = await collectionIsEmpty(db, 'properties');
  if (!empty) {
    console.log('\nFirestore already has properties — skipping properties/rooms/guests upload.');
    return { properties: 0, rooms: 0, guests: 0, skipped: true };
  }

  console.log('\nUploading demo properties, rooms, and guests…');
  const batch = db.batch();

  for (const property of properties) {
    const ref = db.collection('properties').doc(String(property.id));
    batch.set(ref, {
      name: property.name,
      address: property.address,
      region: property.region,
      checkInTime: '15:00',
      checkOutTime: '11:00',
      isActive: true,
    });
  }

  for (const room of rooms) {
    const ref = db.collection('rooms').doc(String(room.id));
    batch.set(ref, {
      propertyId: room.propertyId,
      name: room.name,
      description: room.description,
      pricePerNight: room.pricePerNight,
      capacity: room.capacity,
      roomType: room.roomType,
    });
  }

  for (const guest of guests) {
    const ref = db.collection('guests').doc(String(guest.id));
    batch.set(ref, {
      name: guest.name,
      email: guest.email,
      phone: guest.phone,
      notes: guest.notes,
      isActive: guest.isActive,
      createdAtEpochMs: guest.createdAtEpochMs,
    });
  }

  await batch.commit();
  console.log(`  ✓ ${properties.length} properties, ${rooms.length} rooms, ${guests.length} guests`);
  return { properties: properties.length, rooms: rooms.length, guests: guests.length, skipped: false };
}

async function main() {
  const seedPasswords = loadSeedPasswords();
  const { projectId, credentialLabel } = initializeAdmin();

  const auth = admin.auth();
  const db = admin.firestore();

  console.log(`Firebase project: ${projectId}`);
  console.log(`Using credentials: ${credentialLabel}\n`);
  console.log('Creating/updating demo Auth users and staff documents…');

  const staffResults = [];
  for (const profile of staffProfiles) {
    console.log(`\n${profile.email}`);
    const { uid, created } = await ensureAuthUser(auth, profile, passwordForProfile(profile, seedPasswords));
    await upsertStaffDoc(db, uid, profile);
    staffResults.push({ email: profile.email, uid, role: profile.role, created });
  }

  const entityCounts = await seedEntityData(db);

  console.log('\n========== Summary ==========');
  console.log('Staff (Auth + Firestore):');
  for (const row of staffResults) {
    const tag = row.created ? 'created' : 'existing';
    console.log(`  ${row.email}`);
    console.log(`    uid:  ${row.uid}`);
    console.log(`    role: ${row.role} (${tag})`);
  }
  if (entityCounts.skipped) {
    console.log('\nEntity data: skipped (Firestore not empty)');
  } else {
    console.log(
      `\nEntity data: ${entityCounts.properties} properties, ${entityCounts.rooms} rooms, ${entityCounts.guests} guests`
    );
  }
  console.log('\nDemo login (passwords from your SEED_* env — not printed):');
  console.log('  admin@chain.com');
  console.log('  manager.*@chain.com');
  console.log('\nDone.');
}

main().catch((error) => {
  console.error('\nSeed failed:', error.message ?? error);
  process.exit(1);
});
