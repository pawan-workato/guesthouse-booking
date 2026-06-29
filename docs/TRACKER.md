# Guesthouse Booking — Project Tracker

**Last updated:** 2026-06-27

Single source of truth for security remediation, product delivery, and open work. Status is verified against the codebase (`main` at `8b673fe` plus uncommitted working-tree changes where noted).

**Status legend:** ✅ Done · 🔄 In progress · ⏳ Pending · ❌ Deferred / accepted risk

---

## Summary (by status)

| Status | Count | Examples |
|--------|------:|----------|
| ✅ Done | 49 | KR-02, KR-10, SEC-FIRE, BUG-CHK-UI, FEAT-NAV/KTOR, login lockout UX |
| 🔄 In progress | 0 | — |
| ⏳ Pending | 6 | Sprint 10 P2 manual pentest, FEAT-S8+ |
| ❌ Deferred | 2 | KR-07 chain-wide guest read (product), AUTHZ-08 |

---

## How to update this file

1. **Verify in code** before marking ✅ — grep/build, don't trust docs alone (`app/build.gradle.kts`, `AndroidManifest.xml`, repositories, `firestore.rules`).
2. **One row per item** — update `Status`, `Notes`, and `Fixed in` (commit hash or `working tree`).
3. **Bump `Last updated`** at the top when anything changes.
4. **Link issues/PRs** in Notes when available.
5. **Do not create a second tracker** — extend this file or add subsections here.
6. After security fixes, also update `docs/security/penetration-test-plan.md` §6 dashboard (currently stale — see `DOC-01`).

---

## A. Known-risk findings (pentest plan §6)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| KR-01 | Crypto | SHA-256 + static salt passwords | ✅ Done | Critical | BCrypt cost 12; legacy SHA-256 verify path for migration | `3fd47c3` / PR #1 |
| KR-02 | Auth | Session = `staff_id` in SharedPreferences | ✅ Done | High | Removed prefs-based session; identity from Firebase Auth UID → staff profile only; legacy `guesthouse_auth*` prefs purged on start/login/logout | `LegacySessionStorage` + `AuthRepository` |
| KR-03 | Platform | `allowBackup=true` | ✅ Done | High | `android:allowBackup="false"` in manifest | `3fd47c3` / PR #1 |
| KR-04 | Crypto / ops | Hardcoded demo passwords in seed scripts & wiki | ✅ Done | High | Passwords from `SEED_*` env only; `scripts/.env.example`; docs redacted | Working tree |
| KR-05 | AuthZ | `AdminViewModel.cancelBooking` — no property check | ✅ Done | High | Property check in ViewModel + repository | On `main` (post PR #2) |
| KR-06 | AuthZ | `SyncViewModel.dismissConflict` — no property check | ✅ Done | Medium | `canAccessProperty` before dismiss | On `main` |
| KR-07 | AuthZ | Managers see all chain guests | ❌ Deferred | Medium | **Product decision:** managers view all guests read-only; edit limited to guests linked via bookings on assigned properties (`GuestRepository.canEditGuest`) | Uncommitted guest visibility change |
| KR-08 | AuthZ | `RoomDetail` route — no property guard | ✅ Done | Medium | `AppNavigation` checks `canAccessProperty(propertyId)` | On `main` |
| KR-09 | AuthZ | `BookingRepository` — no repository-layer auth | ✅ Done | High | `canAccessProperty` on cancel, check-in/out, create/update | On `main` |
| KR-10 | Data / ops | Destructive DB migrations | ✅ Done | Medium | `fallbackToDestructiveMigration` gated behind `BuildConfig.DEBUG`; release builds fail on unregistered jumps | Working tree |
| KR-11 | Platform | Release build not minified | ✅ Done | Low | `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` | Working tree |
| KR-12 | Auth | No brute-force protection on login | ✅ Done | Medium | `LoginViewModel`: 5 failures → 30 s lockout; `LoginScreen` live countdown + disabled button | Working tree |
| KR-13 | Data | Plaintext Room SQLite | ✅ Done | High | SQLCipher 4.16 + Keystore-backed passphrase; plaintext→encrypted migration on first launch | Working tree |
| KR-14 | Platform | `FLAG_SECURE` on sensitive screens | ✅ Done | Low | App-wide on `MainActivity` | Working tree |
| KR-15 | Cloud | Firestore guest delete too open | ✅ Done | Medium | Guest rules: read/create/update require `isStaff()`; delete chain-admin only; edit scope in app; **rules published manually** via Firebase Console | `firestore.rules` + manual deploy 2026-06-27 |

**Security subagent note:** Session `4cb960be` (Fix security backlog) applied KR-11/12/14/15 + pentest doc refresh; verified in working tree alongside prior PRs [#1](https://github.com/pawan-workato/guesthouse-booking/pull/1) and [#2](https://github.com/pawan-workato/guesthouse-booking/pull/2).

---

## B. Authorization test cases (pentest plan §5.2)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| AUTHZ-01 | AuthZ | Property list filtering for managers | ✅ Done | High | `PropertiesViewModel` filters by `canAccessProperty` | Sprint 2+ |
| AUTHZ-02 | AuthZ | Deep link — property rooms denied | ✅ Done | High | Nav guard + access denied screen | On `main` |
| AUTHZ-03 | AuthZ | Deep link — room detail denied | ✅ Done | Medium | Guard at `Screen.RoomDetail` | On `main` |
| AUTHZ-04 | AuthZ | Property add/edit denied for managers | ✅ Done | High | `isChainAdmin` in nav | On `main` |
| AUTHZ-05 | AuthZ | Cross-property booking cancel | ✅ Done | High | UI filtered + cancel path checks property | On `main` |
| AUTHZ-06 | AuthZ | Cancel via DB/Frida | ✅ Done | High | Repository enforces property scope | On `main` |
| AUTHZ-07 | AuthZ | Cross-property dismiss conflict | ✅ Done | Medium | `SyncViewModel.dismissConflict` checks property | On `main` |
| AUTHZ-08 | AuthZ | Guest list chain-wide for managers | ❌ Deferred | Medium | Intentional: all guests visible; edit gated | Product spec / README |
| AUTHZ-09 | AuthZ | Guest edit cross-chain | ✅ Done | Medium | Edit denied (`readOnly`) when guest not linked to manager properties; view allowed | Working tree |
| AUTHZ-10 | AuthZ | Create booking cross-property blocked | ✅ Done | High | `BookingViewModel.submitBooking` | On `main` |
| AUTHZ-11 | AuthZ | Manager property CRUD via Frida | ✅ Done | Medium | ViewModel early return for non-admin | On `main` |
| AUTHZ-12 | AuthZ | Direct SQLite role change | ✅ Done | Medium | Online: Firestore SERVER-only session; offline: Firestore cache then local; `refreshSessionBinding` on reconnect | Working tree |

---

## C. Data-at-rest & platform (pentest plan §5.3, §5.7)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| DATA-02 | Data | Backup extraction (`allowBackup`) | ✅ Done | High | Backup disabled | PR #1 |
| DATA-03 | Data | Root read — no SQLCipher | ✅ Done | High | Room encrypted via SQLCipher; passphrase in EncryptedSharedPreferences | KR-13 / working tree |
| DATA-07 | Data | Destructive migration data loss | ✅ Done | Medium | Same as KR-10 — debug-only destructive fallback | Working tree |
| PLAT-02 | Platform | `allowBackup` | ✅ Done | High | `false` in manifest | PR #1 |
| PLAT-04 | Platform | ProGuard / R8 minify | ✅ Done | Low | Release minify + shrink resources enabled | Working tree |
| PLAT-07 | Platform | `FLAG_SECURE` on sensitive screens | ✅ Done | Low | App-wide `MainActivity` | Working tree |
| SEC-SQL | Data | SQLCipher encrypted Room | ✅ Done | High | `net.zetetic:sqlcipher-android:4.16.0` in `app/build.gradle.kts` | Working tree |
| SEC-FIRE | Cloud | Firestore rules — guest write too open | ✅ Done | High | `isStaff()` required for guest read/create/update; delete chain-admin only; **deployed manually** to Firebase Console (CLI login blocked) | `firestore.rules` + manual deploy 2026-06-27 |
| SEC-BF | Auth | Login brute-force / lockout | ✅ Done | Medium | Same as KR-12 | Working tree |
| DOC-01 | Docs | Pentest plan stale vs code | ✅ Done | Low | §6 dashboard updated KR-01–KR-15 | Working tree |

---

## D. Sprint security plan (pentest plan §3, §95–100)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| SPRINT-7 | Security | BCrypt, backup off, guest RBAC, room CRUD | ✅ Done | High | Shipped per README & PRs #1–#2 | Merged 2026-06-29 |
| SPRINT-8 | Security | Encrypted Room (SQLCipher) | ✅ Done | High | KR-13 implemented | Working tree |
| SPRINT-8 | Security | ProGuard / R8 release minify | ✅ Done | Medium | KR-11 | Working tree |
| SPRINT-8 | Security | Certificate pinning (API) | ❌ Deferred | Low | Firebase-only — pinning N/A | Ktor removed |
| SPRINT-9 | Security | Continue hardening (migrations, Firestore guest read scope) | ✅ Done | High | KR-10 + SEC-FIRE complete | Working tree |
| SPRINT-10 | Security | P1 internal pentest execution | ✅ Done | High | Automated + static run — see [pentest-run-2026-06-27.md](./security/pentest-run-2026-06-27.md); P2 manual gate pending | 2026-06-27 |
| PRE-PILOT | Security | P2 pre-production gate | ⏳ Pending | Critical | After Sprint 10 pentest | — |

---

## E. Product & feature delivery (README roadmap)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| FEAT-S2 | Product | Staff login + property-scoped access | ✅ Done | — | | Sprint 2 |
| FEAT-S3 | Product | Offline booking + sync queue | ✅ Done | — | | Sprint 3 |
| FEAT-S4 | Product | Property management (chain admin) | ✅ Done | — | | Sprint 4 |
| FEAT-S5 | Product | Guest profiles | ✅ Done | — | | Sprint 5 |
| FEAT-S6 | Product | Firebase Auth + Firestore sync | ✅ Done | — | | Sprint 6 |
| FEAT-S7 | Product | Today board, check-in/out, block dates, edit bookings, room types, room CRUD, booking search | ✅ Done | — | | Sprint 7 / `61dc4ff` merge |
| FEAT-BOOK-FILTER | Product | Bookings tab hides cancelled by default; **Show cancelled** toggle | ✅ Done | Low | `AdminViewModel` + `AdminScreen` | Working tree |
| FEAT-NAV | Product | 4 bottom tabs (Properties, Book, Today, Bookings) + Guests/Staff in top bar | ✅ Done | Medium | `AppNavigation.kt` — bottom: Properties, Book, Today, Admin; top: Guests, Staff (admin) | Working tree |
| FEAT-KTOR | Product | Remove Ktor backend; Firebase-only | ✅ Done | Medium | `backend/` removed from settings; `data/remote/*` deleted; Firebase-only sync | Working tree |
| FEAT-GUEST | Product | Managers view all guests (read-only unless linked) | ✅ Done | Medium | `GuestRepository.canViewGuest` / `canEditGuest` split | Working tree |
| FEAT-DOCS | Product | README & wiki docs refresh | ✅ Done | Low | README Firebase-first; wiki offline/migrations updated | Working tree |
| FEAT-WIKI | Product | Property wiki **Type** column | ✅ Done | Low | All 7 region property pages include Type column | Working tree |
| FEAT-GLASS | Product | Apple Liquid Glass UI theme | ✅ Done | Medium | `Glass.kt` — gradient mesh, frosted cards, glass nav/scaffold on all screens | Working tree |
| FEAT-S8+ | Product | Sprint 8+ roadmap (TBD) | ⏳ Pending | — | Not defined in README yet | — |

---

## F. Bugs & follow-ups (this conversation)

| ID | Category | Item | Status | Priority | Notes | Fixed in |
|----|----------|------|--------|----------|-------|----------|
| BUG-CHK | Bug | Checkout button on Bookings tab (early checkout) | ✅ Done | High | Removed `checkOutEpochDay > today` guard in `BookingRepository.checkOutBooking`; tests added | Working tree (subagent `c6d1f1b3`) |
| BUG-CHK-UI | Bug | Admin check-out shows no error on failure | ✅ Done | Low | `AdminViewModel` surfaces check-in/out/cancel errors via `actionError`; test added | Working tree |
| BUG-CHK-CMT | Process | Checkout subagent completion | ✅ Done | — | Fix applied + `./gradlew :app:testDebugUnitTest` passed; **not committed** | 2026-06-29 |
| BUG-FLICKER | Bug | Tab/screen UI flicker on navigation | ✅ Done | Medium | `WhileSubscribed(Long.MAX_VALUE)` + cached per-id flows; skip redundant tab nav; `derivedStateOf` chrome; remove nested Scaffolds; `collectAsStateWithLifecycle` | Working tree |
| OPS-FIRE | Ops | Deploy `firestore.rules` to Firebase project | ✅ Done | High | Published manually via Firebase Console (CLI OAuth blocked); rules match repo `firestore.rules` | 2026-06-27 |

---

## G. Pentest phases (reference)

| Phase | Item | Status | Notes |
|-------|------|--------|-------|
| P0 | Dev self-assessment (SAST, manifest) | ✅ Done | Tracker + pentest doc refreshed |
| P1 | Internal pentest | ✅ Done (automated) | [pentest-run-2026-06-27.md](./security/pentest-run-2026-06-27.md); manual P2 follow-up |
| P2 | Pre-production gate | ⏳ Pending | Before pilot devices |
| P3 | Backend/API pentest | ❌ Deferred | No Ktor API in current direction |
| P4 | Annual regression | ⏳ Pending | — |

---

## H. Quick verification commands

```bash
# Security spot-checks
rg -n "isMinifyEnabled|allowBackup|fallbackToDestructiveMigration|FLAG_SECURE|sqlcipher" app/
rg -n "canAccessProperty|BCrypt|EncryptedSharedPreferences" app/src/main/

# Unit tests
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest
```

---

## Document history

| Date | Change |
|------|--------|
| 2026-06-27 | KR-02 — session identity from Firebase UID only; legacy session prefs removed |
| 2026-06-27 | FEAT-GLASS — Liquid Glass UI theme (`4a90eef`) |
| 2026-06-29 | BUG-FLICKER — tab/screen flicker fixes (ViewModel flow sharing, nav chrome) |
| 2026-06-29 | BUG-CHK-UI, SEC-FIRE, KR-10/DATA-07, FEAT-NAV/KTOR/GUEST/DOCS/WIKI, login lockout UX |
