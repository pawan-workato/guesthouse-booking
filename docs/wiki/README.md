# Guesthouse Chain Wiki

Staff reference for the 12-property guesthouse chain. Use alongside the Android booking app.

## Pages

| Page | Description |
|------|-------------|
| [Chain overview](chain-overview.md) | Purpose and list of all 12 properties |
| [Staff guide](staff-guide.md) | Navigation, roles, room/booking features, demo accounts |
| [Booking procedures](booking-procedures.md) | Create, edit, cancel, check-in/out, block dates |
| [Offline operations](offline-operations.md) | Offline queue, Sync status screen, conflicts |
| [FAQ](faq.md) | Common staff questions |

## Properties by region

| Region | Properties | Wiki page |
|--------|------------|-----------|
| Mountain West | Hill View, Cedar Inn, Pinecrest Lodge, Summit Stay | [mountain-west.md](properties/mountain-west.md) |
| Pacific NW | Riverside Lodge | [pacific-nw.md](properties/pacific-nw.md) |
| Coastal | Harbor House | [coastal.md](properties/coastal.md) |
| Northeast | Maple Retreat | [northeast.md](properties/northeast.md) |
| Southwest | Sunstone Villa, Desert Bloom Inn | [southwest.md](properties/southwest.md) |
| Midwest | Lakeside Haven, Meadowbrook Cottage | [midwest.md](properties/midwest.md) |
| Southeast | Oak & Ivy Guesthouse | [southeast.md](properties/southeast.md) |

Property pages describe seeded room names, **types** (from `scripts/seed-data.mjs` `inferRoomType`), and prices. Live inventory staff add or edit in the app is authoritative after sync.

## Quick links

- [Root README](../../README.md)
- [Seeding](../seeding.md)
- Demo admin: `admin@chain.com` — password from `scripts/.env` (`SEED_ADMIN_PASSWORD`)
