# Backend architecture — Ktor API (primary)

The guesthouse-booking Android app uses a **self-hosted Ktor + PostgreSQL** backend as its primary cloud sync path when `USE_KTOR_API` is enabled (default in debug builds). Room (SQLite) remains the on-device cache; the API provides JWT-authenticated sync and RBAC.

## Why Ktor over Supabase

| Factor | Ktor (chosen) | Supabase |
|--------|----------------|----------|
| Hosting | Self-hosted Docker / your VPS | Managed SaaS |
| Stack | Kotlin server + Exposed ORM, same language as Android | Postgres + REST/Realtime, different tooling |
| Dev loop | `docker compose up` + `./gradlew :backend:run` on localhost | Project dashboard + keys |
| RBAC | JWT claims + route-level property checks | RLS policies in SQL |
| Cost / data residency | Full control | Vendor-dependent |

Supabase is a strong fit for greenfield apps that want managed auth and realtime; this project prioritizes a **Kotlin-native stack**, **local Docker development**, and **predictable self-hosting** for a small staff-only chain.

## Overview

```
┌─────────────────┐     HTTPS + JWT        ┌──────────────────┐
│  Android app    │ ◄────────────────────► │  Ktor API :8080  │
│  (Compose UI)   │   Retrofit + Moshi     │  PostgreSQL      │
│       │         │                        └──────────────────┘
│       ▼         │
│  Room (SQLite)  │   offline-first queue (PENDING_SYNC)
└─────────────────┘
```

## Sync behavior

| Data | Online | Offline |
|------|--------|---------|
| Staff login | `POST /api/auth/login` → encrypted token storage | JWT restored from `TokenStorage`; bootstrap when back online |
| Properties / rooms | Pulled on login via `KtorApiSyncService.pullBootstrap` | Room seed / cache |
| Guests | Push pending via `POST /api/guests/sync`, then pull merge by `serverId` | Room `PENDING_SYNC` |
| Bookings | Push pending via `POST /api/bookings/sync`, merge pulls by `serverId` | Room queue; `SyncRepository` + WorkManager |

Reference format for confirmed bookings: `SyncRepository.formatReference(propertyId, serverBookingId)` → `GH-{propertyId}-{id}`.

## RBAC

- **CHAIN_ADMIN** — all properties in bootstrap and booking sync.
- **PROPERTY_MANAGER** — JWT `assignedPropertyIds`; server filters properties/bookings accordingly.

## Firebase (optional legacy)

When `google-services.json` is present and Ktor is disabled (`USE_KTOR_API = false`), the app can still use **Firebase Auth + Firestore** for sync (`FirestoreSyncService`, `SyncRepository.syncWithFirestore`). This path remains for migrations or dual-run experiments but is not the default dev setup.

## Local development

1. `docker compose up` — Postgres + API on port **8080**
2. Or run the API from Gradle: `./gradlew :backend:run` (with Postgres reachable)
3. Android emulator API base URL: **`http://10.0.2.2:8080/`** (host loopback)

See [seeding.md](seeding.md), [staff guide](wiki/staff-guide.md#demo-accounts-development), and [README.md](../README.md) for demo credentials and Gradle commands.
