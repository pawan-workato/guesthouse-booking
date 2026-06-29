/**
 * Load seed passwords from environment (scripts/.env or exported vars).
 */

import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const MIN_LENGTH = 12;
const __dirname = dirname(fileURLToPath(import.meta.url));
const ENV_FILE = join(__dirname, '.env');

function loadDotEnvFile() {
  if (!existsSync(ENV_FILE)) return;
  for (const line of readFileSync(ENV_FILE, 'utf8').split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (!(key in process.env)) process.env[key] = value;
  }
}

export function loadSeedPasswords() {
  loadDotEnvFile();
  const admin = process.env.SEED_ADMIN_PASSWORD?.trim();
  const manager = process.env.SEED_MANAGER_PASSWORD?.trim();
  if (!admin || admin.length < MIN_LENGTH) {
    throw new Error(
      `Set SEED_ADMIN_PASSWORD (min ${MIN_LENGTH} characters). Copy scripts/.env.example to scripts/.env`
    );
  }
  if (!manager || manager.length < MIN_LENGTH) {
    throw new Error(
      `Set SEED_MANAGER_PASSWORD (min ${MIN_LENGTH} characters). See scripts/.env.example`
    );
  }
  if (admin === manager) {
    throw new Error('SEED_ADMIN_PASSWORD and SEED_MANAGER_PASSWORD must be different.');
  }
  return { admin, manager };
}

export function passwordForProfile(profile, passwords) {
  return profile.role === 'CHAIN_ADMIN' ? passwords.admin : passwords.manager;
}
