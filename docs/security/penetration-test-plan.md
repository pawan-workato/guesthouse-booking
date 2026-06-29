# Penetration Test Plan — Guesthouse Booking (Android)

**Document version:** 1.0  
**Last updated:** 2026-06-29  
**Application:** `com.guesthouse.booking` (staff-only Android app)  
**Repository:** [guesthouse-booking](https://github.com/pawan-workato/guesthouse-booking)

---

## 1. Scope & Objectives

### 1.1 In scope

| Area | Description |
|------|-------------|
| **Mobile application** | Kotlin / Jetpack Compose APK (debug and release builds) |
| **Local data stores** | Room SQLite (`guesthouse.db`), SharedPreferences (`guesthouse_auth`, `guesthouse_sync`) |
| **Authentication & RBAC** | Staff login, session persistence, `CHAIN_ADMIN` vs `PROPERTY_MANAGER`, property-scoped access |
| **Business logic** | Booking creation, cancellation, guest CRUD, property management, offline sync queue |
| **Android platform surface** | Manifest permissions, exported components, backup, WorkManager sync workers |
| **Client-side controls** | Navigation routes, ViewModel authorization, repository-layer enforcement gaps |
| **Future API (planned)** | Ktor API is implemented; include `POST/PUT /api/rooms`, JWT RBAC, and dual Firebase path in API-phase tests |

### 1.2 Out of scope (current phase)

- Cloud infrastructure hosting the **optional** Ktor API (in scope for P3 API pentest, not P0 mobile-only)
- Third-party SaaS integrations (payments, email, analytics)
- Physical security of guesthouse premises
- Social engineering of real staff (unless explicitly authorized red-team exercise)
- Denial-of-service against non-existent API endpoints
- iOS or web clients (not in product scope)
- Supply-chain audit of Google/Maven dependencies (optional separate assessment)

### 1.3 Objectives

1. **Identify exploitable weaknesses** in authentication, authorization, and local data protection before production deployment.
2. **Validate** that property-scoped RBAC cannot be bypassed by malicious staff, device compromise, or UI manipulation.
3. **Assess** resilience of offline/sync logic against tampering and reference forgery.
4. **Establish** a repeatable test baseline aligned with OWASP MASVS for future releases and backend integration.
5. **Produce** actionable findings with severity ratings and remediation owners.

---

## 2. Threat Model

### 2.1 Assets

| Asset | Sensitivity | Storage |
|-------|-------------|---------|
| Guest PII (name, email, phone, notes) | High | `guests` table, booking rows |
| Staff credentials (password hashes) | Critical | `staff` table |
| Staff session identity | High | SharedPreferences `guesthouse_auth` |
| Booking inventory & references | Medium | `bookings` table |
| Property/room configuration | Low–Medium | `properties`, `rooms` tables |

### 2.2 Threat actors

| Actor | Capability | Motivation | Example scenario |
|-------|------------|------------|------------------|
| **Malicious property manager** | Valid login, limited property assignments | Cancel competitors' bookings, view other regions' guests | Property manager for Aspen tries to cancel a Monterey booking |
| **Malicious chain admin** | Full app access | Bulk data exfiltration, credential harvesting | Admin exports guest DB via backup extraction |
| **Device thief** | Physical access, possibly unlocked device | PII theft, session hijack | Stolen tablet left logged in at front desk |
| **Rooted-device attacker** | Root + ADB, SQLite access | Direct DB tampering, prefs editing | Attacker sets `staff_id=1` in prefs for admin session |
| **Future API attacker** | Network access when backend ships | IDOR, token theft, replay | Replay stolen session token across properties |

### 2.3 Attack surfaces (current build)

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Compose / Navigation)                            │
│  Routes: properties, guests, book, today, bookings, staff (admin) │
├─────────────────────────────────────────────────────────────┤
│  ViewModels (RBAC enforced inconsistently)                   │
├─────────────────────────────────────────────────────────────┤
│  Repositories (minimal auth checks — BookingRepository)     │
├─────────────────────────────────────────────────────────────┤
│  Room DB + SharedPreferences + Android Backup               │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Testing Phases & Schedule

| Phase | When | Focus | Owner |
|-------|------|-------|-------|
| **P0 — Dev self-assessment** | After Sprint 5 (guest profiles) ✅ | SAST, manifest review, known-finding validation | Engineering |
| **P1 — Internal pentest** | After Sprint 7 feature complete | Full mobile test cases below on release candidate | Engineering + security champion |
| **P2 — Pre-production gate** | Before first pilot deployment to properties | Repeat P1 + rooted device extraction, backup abuse | External or dedicated security reviewer |
| **P3 — Backend integration** | Before API goes live (post-Sprint ~8–10) | Mobile + API combined pentest | External pentest firm |
| **P4 — Annual / major release** | Yearly or after significant RBAC/sync changes | Regression + new feature surface | External pentest |

### Suggested sprint placement

| Sprint | Security activity |
|--------|-------------------|
| **Sprint 7** (shipped) | BCrypt passwords, backup disabled, guest RBAC scoping, room CRUD, Today board, edit bookings |
| **Sprint 8–9** | Encrypted Room ✅, ProGuard/R8, certificate pinning for API |
| **Sprint 10** | **P1 internal pentest** — formal execution using this document |
| **Pre-pilot** | **P2 gate** — sign-off required before devices ship to properties |

---

## 4. Methodology

### 4.1 Standards alignment

| Standard | Application |
|----------|-------------|
| **OWASP MASVS** v2.1 | Level 1 (standard) minimum; Level 2 for production pilot |
| **OWASP MASTG** | Test case mapping for storage, crypto, auth, platform, network |
| **OWASP Mobile Top 10 (2024)** | M1–M10 coverage matrix in appendix |

### 4.2 Techniques

| Technique | Tools | Frequency |
|-----------|-------|-----------|
| **Static analysis (SAST)** | MobSF, Android Lint, semgrep (Kotlin rules) | Every release candidate |
| **Dynamic analysis (DAST)** | MobSF dynamic scan, Drozer (limited — single activity) | P1, P2 |
| **Manual exploitation** | ADB, Frida, objection, sqlite3 | P1, P2, P3 |
| **Configuration review** | Manifest, `build.gradle.kts`, ProGuard rules | P0 onward |
| **Threat-driven code review** | Git diff on auth/RBAC/sync paths | Every sprint touching security |

### 4.3 Test environment

- **Devices:** Emulator (API 26, 34) + physical device (rooted optional for P2)
- **Builds:** `assembleDebug` and `assembleRelease` (note: release currently has `isMinifyEnabled = false`)
- **Accounts:** Demo staff from `scripts/seed-firebase-demo.mjs` (see [docs/seeding.md](../seeding.md) and [staff-guide.md](../wiki/staff-guide.md#demo-accounts-development))
- **Data:** Fresh install; run Firebase seed script, then sign in to pull bootstrap into Room cache

---

## 5. Test Categories & Concrete Test Cases

### 5.1 Authentication & session

| ID | Test case | Steps | Expected | Code reference |
|----|-----------|-------|----------|----------------|
| AUTH-01 | Valid login | Login `admin@chain.com` with password from `scripts/.env` (`SEED_ADMIN_PASSWORD`) | Session established, main nav shown | `AuthRepository.login()` |
| AUTH-02 | Invalid password | Wrong password 5× | Generic error, no user enumeration | `AuthRepository.login()` |
| AUTH-03 | Brute force resistance | Automated login attempts (100+) | Rate limit or lockout (currently **none** — document as finding) | `LoginViewModel` |
| AUTH-04 | Session persistence | Login, kill app, reopen | Session restored from **Firebase Auth** + staff profile lookup by UID | `restoreSession()` |
| AUTH-05 | **Prefs tampering — privilege escalation** | Root/adb: set legacy `staff_id` in old prefs | **No effect** — prefs not read; session derived from Firebase UID | `LegacySessionStorage.purge()` |
| AUTH-06 | **Prefs tampering — invalid staff** | Set legacy `staff_id` to invalid ID | **No effect** — same as AUTH-05 | N/A (prefs unused) |
| AUTH-07 | Logout completeness | Logout, inspect prefs | Legacy session prefs deleted; Firebase sign-out | `LegacySessionStorage.purge()` |
| AUTH-08 | Session fixation | Login as manager, logout, login as admin | No stale property filters or cached ViewModels | `MainActivity` |
| AUTH-09 | Password hash extraction | SQLite: read `staff.password_hash` | Hashes present; assess crackability | `PasswordHasher`, `StaffEntity` |
| AUTH-10 | Offline login | Disable network, login with valid creds | Login succeeds (local DB) | No network dependency |

### 5.2 Authorization / IDOR (property-scoped RBAC)

**Roles:** `CHAIN_ADMIN` (all properties) vs `PROPERTY_MANAGER` (assigned properties only via `StaffPropertyAssignmentEntity`).

| ID | Test case | Steps | Expected | Known gap |
|----|-----------|-------|----------|-----------|
| AUTHZ-01 | Property list filtering | Login as `manager.mountain@chain.com` | Only properties 1, 3, 7, 11 visible | `PropertiesViewModel` filters |
| AUTHZ-02 | Deep link — property rooms | Navigate to `property/4/rooms` as mountain manager | Access denied screen | `AppNavigation` checks `canAccessProperty` |
| AUTHZ-03 | Deep link — room detail | Navigate to `room/{id}` for coastal property room as mountain manager | **Verify** — no nav guard on `RoomDetail` route | `Screen.RoomDetail` — **no check** |
| AUTHZ-04 | Property add/edit routes | Navigate `property/add`, `property/5/edit` as manager | Access denied | `isChainAdmin` guard in nav |
| AUTHZ-05 | **Cancel booking — cross-property** | As mountain manager, invoke cancel on booking ID for property 4 | **Should deny** — UI hides booking but API may allow | **`AdminViewModel.cancelBooking()` has no property check**; `BookingRepository.cancelBooking()` has no auth |
| AUTHZ-06 | **Cancel via DB/Frida** | Hook `cancelBooking(bookingId)` with out-of-scope ID | Cancellation should fail | Repository layer gap |
| AUTHZ-07 | **Dismiss sync conflict — cross-property** | As manager, call `dismissConflict` for other property booking | **Should deny** | **`SyncViewModel.dismissConflict()` — no check** |
| AUTHZ-08 | **Guest list scope** | Mountain manager views Guests tab | **All chain guests visible** — assess business risk | `GuestsViewModel` — no RBAC |
| AUTHZ-09 | **Guest edit cross-chain** | Navigate `guest/1/edit` — guest may be unrelated to assigned properties | Edit succeeds? Document exposure | `GuestFormScreen` route — no property guard |
| AUTHZ-10 | Create booking cross-property | Submit booking for room outside assigned properties via `BookingViewModel` | Error: no access | `submitBooking()` checks `canAccessProperty` |
| AUTHZ-11 | Chain admin property CRUD | Manager calls `createProperty` via Frida | Silent no-op | `PropertiesViewModel` early return |
| AUTHZ-12 | Direct SQLite — role change | Update `staff.role` to `CHAIN_ADMIN` in DB | **Online:** session from Firestore SERVER only — tampered SQLite ignored. **Offline:** local cache used until reconnect; `refreshSessionBinding()` re-validates | `AuthRepository.loadFirebaseSession()`, `refreshSessionBinding()` |

### 5.3 Data at rest

| ID | Test case | Steps | Expected | Code reference |
|----|-----------|-------|----------|----------------|
| DATA-01 | SQLite file location | `adb shell run-as ...` or root path | `/data/data/com.guesthouse.booking/databases/guesthouse.db` | `AppDatabase` name |
| DATA-02 | **Backup extraction** | `adb backup`, `bmgr backup`, or Android Backup Extractor | PII and password hashes recoverable if backup allowed | `android:allowBackup="true"` |
| DATA-03 | Root read without encryption | Rooted device: copy DB and prefs | DB encrypted (SQLCipher); session prefs encrypted | `AppDatabase` + `DatabaseKeyManager`; auth uses EncryptedSharedPreferences |
| DATA-04 | Guest PII in bookings | Query bookings table | Denormalized guest fields even when `guestId` set | `BookingEntity` |
| DATA-05 | Staff table exposure | Query staff table | Email + password hashes readable | `StaffEntity` |
| DATA-06 | Log leakage | Logcat during login/booking | No passwords or PII in logs | Manual log review |
| DATA-07 | Destructive migration data loss | Bump DB version, reinstall | Data wiped — assess operational/security impact | `fallbackToDestructiveMigration` |

### 5.4 Cryptography

| ID | Test case | Steps | Expected | Finding |
|----|-----------|-------|----------|---------|
| CRYP-01 | Password algorithm | Review `PasswordHasher.kt` | Strong adaptive hash (bcrypt/Argon2) | **SHA-256 + static salt `guesthouse-chain-v1`** |
| CRYP-02 | Salt uniqueness | Compare hashes for two users with same password | Per-user salt required | **Shared static salt** |
| CRYP-03 | Hash crack test | Extract hash, run hashcat/john with wordlist | Weak seeded passwords crack if chosen poorly — use strong `SEED_*` values | Demo password policy in `scripts/seed-env.mjs` |
| CRYP-04 | No secrets in APK | strings / apktool search | No production API keys (N/A today) | `INTERNET` only |
| CRYP-05 | TLS (future) | When API added: test weak TLS, pinning bypass | TLS 1.2+, pinning enforced | Not applicable yet |

### 5.5 Client-side controls (ViewModel / navigation bypass)

| ID | Test case | Steps | Expected |
|----|-----------|-------|----------|
| CLIENT-01 | Navigation deep route injection | Compose Navigation: manually navigate restricted routes | Guards block unauthorized screens |
| CLIENT-02 | Frida hook — skip login | Patch `session` StateFlow to non-null admin session | Full app access without credentials |
| CLIENT-03 | Frida hook — `canAccessProperty` | Force return `true` | UI shows all properties — expected client weakness; server must enforce later |
| CLIENT-04 | ViewModel direct invocation | Reflection or Frida on `AdminViewModel.cancelBooking` | Document as IDOR if succeeds |
| CLIENT-05 | Debug build on production device | Install debug APK | `android:debuggable` false on release; debug has tooling exposure |
| CLIENT-06 | Compose recomposition race | Rapid role switch via prefs tampering during navigation | No inconsistent state |

### 5.6 Sync / offline integrity

| ID | Test case | Steps | Expected | Code reference |
|----|-----------|-------|----------|----------------|
| SYNC-01 | Offline booking reference | Create booking offline | `TMP-####` reference assigned | `formatOfflineReference()` |
| SYNC-02 | Sync promotes reference | Go online, sync | `GH-{propertyId}-{id}` format | `formatReference()` |
| SYNC-03 | **Reference forgery** | SQLite: set `sync_reference` to forged `GH-4-0001` | Document trust model — local-only today | No server validation |
| SYNC-04 | Conflict injection | Insert overlapping SYNCED booking, sync pending | CONFLICT status set | `SyncRepository.syncNow()` |
| SYNC-05 | **Manipulate sync status** | Set `sync_status=SYNCED` on pending booking without overlap | Booking appears synced — integrity gap | Direct DB update |
| SYNC-06 | WorkManager abuse | Trigger `SyncWorker` manually | No privilege escalation | `SyncWorker` |
| SYNC-07 | Network flip during booking | Toggle airplane mode during submit | Correct PENDING vs SYNCED | `NetworkMonitor` |

### 5.7 Android platform

| ID | Test case | Steps | Expected | Current state |
|----|-----------|-------|----------|---------------|
| PLAT-01 | Exported components | `dumpsys package` / MobSF | Only launcher activity exported | `MainActivity` exported ✅ |
| PLAT-02 | **allowBackup** | Backup and restore app data | Backup disabled or encrypted for sensitive data | **`allowBackup=true`** ❌ |
| PLAT-03 | Permissions | Manifest review | Minimal permissions | `INTERNET`, `ACCESS_NETWORK_STATE` |
| PLAT-04 | **ProGuard / R8** | Release APK decompile (jadx) | Classes obfuscated, strings minimized | **`isMinifyEnabled = false`** ❌ |
| PLAT-05 | Cleartext traffic (future) | `networkSecurityConfig` when API added | Cleartext blocked | Not configured |
| PLAT-06 | Task hijacking / overlay | Third-party overlay on login | No sensitive data under overlays | Manual |
| PLAT-07 | Screenshot / recents | Login screen in app switcher | Consider `FLAG_SECURE` for sensitive screens | Not implemented |
| PLAT-08 | minSdk 26 | Verify deprecated API usage | No unsafe APIs | API 26+ |

### 5.8 Future API pentest checklist (when backend added)

Execute during **P3** before production API launch:

| ID | Area | Test cases |
|----|------|------------|
| API-01 | Authentication | Token expiry, refresh rotation, logout invalidates server session |
| API-02 | Authorization | Property-scoped IDOR on `/bookings/{id}`, `/guests/{id}`, `/properties/{id}` |
| API-03 | Mass assignment | PATCH booking with `propertyId` or `role` fields |
| API-04 | Sync protocol | Replay of sync payloads, duplicate reference handling, conflict resolution authority |
| API-05 | Rate limiting | Login and booking endpoints |
| API-06 | TLS & pinning | Certificate pinning bypass attempts, MITM with user CA |
| API-07 | Error handling | No stack traces or internal IDs in API errors |
| API-08 | CORS / mobile headers | API not exposed to browser origins inappropriately |
| API-09 | Offline queue upload | Tampered local queue accepted by server? Server must re-validate property scope |
| API-10 | Guest PII | Encryption in transit and at rest on server; retention policy |

---

## 6. Known High-Risk Findings to Validate

These items were identified in code review; pentest should confirm exploitability and document evidence.

| ID | Finding | Severity | Location | Validation test |
|----|---------|----------|----------|-----------------|
| **KR-01** | SHA-256 password hashing with static salt | **Critical** | `PasswordHasher.kt` | CRYP-01, CRYP-03 |
| **KR-02** | Session = `staff_id` only in SharedPreferences (no token, no binding) | **High** | `AuthRepository.kt` | AUTH-05 |
| **KR-03** | `allowBackup=true` enables PII/hash extraction | **High** | `AndroidManifest.xml` | DATA-02 |
| **KR-04** | Hardcoded demo passwords in Firebase seed script and wiki | **High** | `scripts/seed-env.mjs`, `scripts/.env.example`, wiki | **Fixed** — passwords from env only; docs redacted | AUTH-09, CRYP-03 |
| **KR-05** | `AdminViewModel.cancelBooking` — no property authorization | **High** | `AdminViewModel.kt:44-47` | AUTHZ-05, AUTHZ-06 |
| **KR-06** | `SyncViewModel.dismissConflict` — no property check | **Medium** | `SyncViewModel.kt:75-78` | AUTHZ-07 |
| **KR-07** | `GuestsViewModel` — no property-scoped guest access | **Medium** | `GuestsViewModel.kt` | AUTHZ-08 |
| **KR-08** | `RoomDetail` route — no property access guard | **Medium** | `AppNavigation.kt:173-188` | AUTHZ-03 |
| **KR-09** | `BookingRepository` — no repository-layer auth | **High** | `BookingRepository.kt` | AUTHZ-06 |
| **KR-10** | Destructive DB migrations wipe all data | **Medium** (ops) | `AppDatabase.kt:44` | DATA-07 |
| **KR-11** | Release build not minified | **Low** | `app/build.gradle.kts` | PLAT-04 |
| **KR-12** | No brute-force protection on login | **Medium** | `LoginViewModel.kt` | AUTH-03 |
| **KR-13** | Plaintext Room SQLite (guest PII, password hashes) | **High** | `AppDatabase.kt` | DATA-03 — **Fixed** (SQLCipher + Keystore-backed passphrase) |

### AdminViewModel cancel authorization (verified)

The UI filters bookings by `session.canAccessProperty(it.propertyId)` in the `bookingsWithDetails` flow, but **`cancelBooking(bookingId)` calls `repository.cancelBooking(bookingId)` without verifying the booking belongs to an accessible property**. A property manager who learns another property's booking ID (e.g., via SQLite, Frida, or future API) can cancel it if they can invoke the method.

```kotlin
// AdminViewModel.kt — display filtered, action not filtered
fun cancelBooking(bookingId: Long) {
    viewModelScope.launch {
        repository.cancelBooking(bookingId)  // no auth check
    }
}
```

**Recommended remediation (for dev team, not pentest):** Add property check in `AdminViewModel` and enforce in `BookingRepository` with session context.

---

## 7. Tools

| Tool | Purpose | Phase |
|------|---------|-------|
| **MobSF** | SAST/DAST, manifest analysis, APK entropy | P0, P1 |
| **jadx / apktool** | Decompile release APK | P1 |
| **ADB** | Install, backup, `run-as`, logcat | All |
| **Android Backup Extractor (abe)** | Extract `allowBackup` data | P1, P2 |
| **Frida** | Runtime hooking (session, RBAC, cancelBooking) | P1, P2 |
| **objection** | Frida-based exploration without custom scripts | P1, P2 |
| **sqlite3** | Direct DB queries on extracted/root DB | P2 |
| **hashcat / john** | Password hash cracking tests | P1 |
| **semgrep** | Kotlin security rules in CI | P0 ongoing |
| **Drozer** | Limited — single exported activity | P1 |
| **Burp Suite** | API testing when backend exists | P3 |
| **mitmproxy** | TLS interception for future API | P3 |

### Example commands

```bash
# Build APKs
./gradlew assembleDebug assembleRelease

# MobSF (docker)
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest

# ADB backup (validates KR-03)
adb backup -f guesthouse.ab -noapk com.guesthouse.booking

# Run-as on debuggable build
adb shell run-as com.guesthouse.booking ls databases/

# Frida session tampering (AUTH-05)
frida -U -f com.guesthouse.booking -l scripts/tamper_staff_id.js
```

---

## 8. Roles & Responsibilities

| Role | Responsibilities |
|------|------------------|
| **Engineering (dev)** | P0 SAST, fix findings, maintain `docs/security/`, ProGuard/backup config |
| **Security champion** | Coordinate P1, triage findings, track remediation template |
| **QA** | Regression tests for auth/RBAC after fixes |
| **External pentester** | P2 gate and P3 API assessment, independent report |
| **Product / ops** | Accept residual risk sign-off for pilot |

### RACI summary

| Activity | Dev | Sec champion | External | Product |
|----------|-----|--------------|----------|---------|
| P0 self-assessment | R/A | C | — | I |
| P1 internal pentest | C | R/A | — | I |
| P2 pre-production | C | A | R | A (sign-off) |
| P3 API pentest | C | A | R | A |
| Remediation | R | A | C | I |

*R = Responsible, A = Accountable, C = Consulted, I = Informed*

---

## 9. Entry & Exit Criteria

### 9.1 Entry criteria

- [ ] Release candidate APK buildable (`assembleRelease`)
- [ ] Test accounts seeded and documented
- [ ] This test plan reviewed and version pinned
- [ ] Test devices/emulators available
- [ ] Findings tracker created (see §10)

### 9.2 Exit criteria (P1 / P2)

- [ ] All test cases in §5 executed or explicitly waived with rationale
- [ ] No **Critical** or **High** findings open without accepted risk exception
- [ ] KR-01 through KR-05 either fixed or documented with compensating controls
- [ ] Pentest report delivered with evidence (screenshots, commands, PoC steps)
- [ ] Remediation tickets linked in tracker

### 9.3 Severity rubric

| Severity | Definition | Example in this app | SLA |
|----------|------------|---------------------|-----|
| **Critical** | Full chain compromise, mass PII exposure, no auth bypass needed for major impact | Extract all guest PII via backup; crack admin password from hash | Fix before pilot |
| **High** | Significant unauthorized action or data access with moderate effort | Cross-property booking cancel; prefs session hijack to admin | Fix before pilot |
| **Medium** | Limited data exposure or auth bypass requiring root/Frida | View all guests chain-wide; dismiss other property conflicts | Fix in Sprint 10–11 |
| **Low** | Defense-in-depth gap, unlikely exploit alone | Missing `FLAG_SECURE`; no ProGuard | Next release |
| **Info** | Best practice, no direct exploit | Destructive migrations noted | Backlog |

---

## 10. Remediation Tracking Template

Copy into issue tracker or spreadsheet for each finding:

```markdown
## Finding: [TITLE]

| Field | Value |
|-------|-------|
| **ID** | e.g. KR-05 / AUTHZ-05 |
| **Severity** | Critical / High / Medium / Low / Info |
| **Status** | Open / In Progress / Fixed / Accepted Risk |
| **Test case** | AUTHZ-05 |
| **Component** | AdminViewModel, BookingRepository |
| **Description** | Property manager can cancel bookings outside assigned properties |
| **Steps to reproduce** | 1. Login as manager.mountain@chain.com 2. ... |
| **Evidence** | Screenshot / Frida script / DB query output |
| **Impact** | Unauthorized cancellation of revenue bookings |
| **Recommendation** | Enforce `canAccessProperty` in cancel path; add repository auth |
| **Owner** | @engineer |
| **Target sprint** | Sprint 10 |
| **Retest date** | |
| **Retest result** | Pass / Fail |
```

### Finding status dashboard

| ID | Severity | Status | Sprint | Notes |
|----|----------|--------|--------|-------|
| KR-01 | Critical | **Open** | 8 | SHA-256 + static salt — Firebase Auth is the prod auth provider; local hash is demo-only |
| KR-02 | High | **Fixed** ✅ | 8 | Session from Firebase Auth UID only; no `staff_id` in prefs; legacy prefs purged |
| KR-03 | High | **Open** | 9 | `allowBackup=true` — add `android:dataExtractionRules` config before pilot |
| KR-04 | High | **Fixed** ✅ | — | Passwords from `SEED_*` env; `scripts/.env` gitignored; wiki/docs redacted |
| KR-05 | High | **Fixed** ✅ | 8 | `AdminViewModel.cancelBooking` checks `session.canAccessProperty(booking.propertyId)` |
| KR-06 | Medium | **Fixed** ✅ | 8 | `SyncViewModel.dismissConflict` checks session + property before delegating |
| KR-07 | Medium | **Fixed** ✅ | 8 | `GuestRepository.canEditGuest` scopes edits; `AppNavigation` shows access-denied screen |
| KR-08 | Medium | **Fixed** ✅ | 8 | `RoomDetail` route checks `canAccessProperty(propertyId)` before rendering |
| KR-09 | High | **Fixed** ✅ | 8 | `BookingRepository.cancelBooking` checks session + `canAccessProperty` |
| KR-10 | Medium | **Documented** | 8 | `fallbackToDestructiveMigration` marked dev-only; explicit migrations v8→9→10 in place |
| KR-11 | Low | **Fixed** ✅ | 8 | `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` added |
| KR-12 | Medium | **Fixed** ✅ | 8 | `LoginViewModel`: lockout after 5 failures, 30 s cooldown (in-memory per session) |
| KR-13 | High | **Fixed** ✅ | 8 | SQLCipher: 32-byte random key in Android Keystore; plaintext DB migration on upgrade |
| KR-14 | Low | **Fixed** ✅ | 8 | `FLAG_SECURE` on `MainActivity` — prevents screenshots/recents across all screens |
| KR-15 | Medium | **Fixed** ✅ | 8 | Firestore `guests/{id}`: `delete` requires `isChainAdmin()`; create/read/update → signed-in |

---

## Appendix A — OWASP Mobile Top 10 mapping

| Risk | Coverage in this plan |
|------|----------------------|
| M1 Improper credential usage | AUTH-*, CRYP-*, KR-01, KR-04 |
| M2 Inadequate supply chain security | Out of scope (optional) |
| M3 Insecure auth/authorization | AUTHZ-*, KR-05–09 |
| M4 Insufficient input/output validation | SYNC-*, booking overlap |
| M5 Insecure communication | API-06 (future) |
| M6 Inadequate privacy controls | DATA-*, AUTHZ-08, PLAT-07 |
| M7 Insufficient binary protections | PLAT-04, CLIENT-* |
| M8 Security misconfiguration | PLAT-02, PLAT-03, KR-03 |
| M9 Insecure data storage | DATA-*, KR-02 |
| M10 Insufficient cryptography | CRYP-* |

---

## Appendix B — Demo test accounts

From `docs/wiki/staff-guide.md` (do not use in production). Set passwords in `scripts/.env` before `npm run seed`:

| Email | Password | Role | Properties |
|-------|----------|------|------------|
| `admin@chain.com` | `SEED_ADMIN_PASSWORD` | Chain Admin | All (1–12) |
| `manager.mountain@chain.com` | `SEED_MANAGER_PASSWORD` | Property Manager | 1, 3, 7, 11 |
| `manager.coastal@chain.com` | `SEED_MANAGER_PASSWORD` | Property Manager | 2, 4, 8 |
| `manager.southwest@chain.com` | `SEED_MANAGER_PASSWORD` | Property Manager | 6, 9 |
| `manager.east@chain.com` | `SEED_MANAGER_PASSWORD` | Property Manager | 5, 10, 12 |

---

## Document history

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-27 | Engineering | Initial pentest plan for local-only Android app |
| 1.1 | 2026-06-29 | Engineering | Firebase + optional Ktor API, Sprint 7 scope, toolbar sync (no Sync tab), room CRUD |
| 1.2 | 2026-06-29 | Engineering | KR-13 SQLCipher Room encryption, plaintext migration documented |
| 1.3 | 2026-06-29 | Engineering | Sprint 8 security fixes: KR-02,05–15 marked Fixed; dashboard updated; KR-03 deferred to Sprint 9 |
