# Staff guide

## Who uses the app

The Android app is **staff-only**. Property managers book rooms for guests; guests do not log in or pay through the app.

## App navigation

### Bottom tabs

| Tab | Who | Purpose |
|-----|-----|---------|
| **Book** | All staff | Create a booking with search and calendar |
| **Today** | All staff | Arrivals, departures, in-house; check-in/out (opens on launch) |
| **Bookings** | All staff | All reservations for your properties; edit, cancel, check-in/out |
| **Properties** | All staff | Browse assigned locations; occupancy on each card; open room lists |

### Top bar

- **Guests** (person icon) — guest profiles and property-scoped stay history (all properties for chain admin)
- **Staff** (group icon, chain admin only) — add/edit property managers and assignments
- **Sync** (circular arrows) — opens **Sync status** (pending uploads, conflicts, **Sync now**); badge = pending + conflict count
- **Sign out**

There is no separate **Sync** tab. **Guests** and **Staff** are in the top bar so the bottom navigation stays uncluttered for chain admins. Pending uploads and conflicts also appear on **Bookings** and on the **Sync status** screen (opened from the sync icon).

### Typical workflow

1. **Today** — check arrivals, departures, and in-house guests; check-in/out as needed.
2. **Properties** (last tab) — each card shows tonight’s occupancy; tap a site to open rooms (search by name or region).
3. Open the property → review **room type breakdown** (e.g. `4 Double · 2 Single`) and rooms.
4. Tap a room → calendar (booked + blocked dates) → **Book for guest**, or use the **Book** tab.
5. On **Book**: search property/room, filter by room type chips, select dates, pick saved guest or enter details.
6. Confirm under **Bookings**; use **Today** on arrival/departure day.

## Guest management

From **Guests**:

- **Add** — **+** FAB
- **Edit** — pencil on a card
- **Remove** — edit screen → **Remove guest** (soft-delete; past bookings keep snapshot fields)
- **Show removed** — toggle inactive profiles

**All signed-in staff** see every active guest profile and **may edit any guest** (name, email, phone, notes). Guest detail includes **stay history**: chain admins see bookings at all properties; property managers see only stays at their assigned properties. **Only chain admins** can remove (soft-delete) or reactivate guests.

## Staff management (chain admin)

Open **Staff** from the top bar (group icon):

- **Add manager** — name, email, temporary password, property assignments
- **Edit** / **Remove** (soft-deactivate)
- **Show removed** — reactivate

Cannot remove the last active chain admin.

## Property management (chain admin)

**Properties** tab (last bottom tab):

- Each property card shows **occupied / arrivals / departures / vacant** for tonight
- Chain admins with multiple sites see an **All properties — tonight** summary at the top
- **Add** / **Edit** / **Remove** (deactivate) — **Add property** button and **+** FAB
- **Show removed** — reactivate

Managers only see **active** properties assigned to them.

## Room management

Anyone with access to a property:

- **Add** — **+** on the property room list
- **Edit** — pencil on a card or **Edit** on room detail
- Fields: name, description, nightly price, max guests, **room type** (Single, Double, Suite, Family)

Room types show on the property room list, room cards, **Book** tab (summary + filter chips), and room picker.

## Morning notifications

After sign-in, ~**7:00 AM local** arrival summary (property-scoped). Allow notifications on Android 13+. Stops on sign-out.

## Booking features

- **Book** tab — dynamic search for properties (name, region, city) and rooms (name, description, type)
- **Edit booking** — **Bookings** → **Edit** on a confirmed reservation
- **Check-in / check-out** — **Bookings** or **Today** (when dates allow)
- **Block dates** — room detail → **Block dates** (syncs to Firestore; orange on calendar)

## Staff roles

| Role | Access |
|------|--------|
| **Chain admin** | All 12 properties; all guests (full edit); staff admin; full bookings |
| **Property manager** | Assigned properties only; view and **edit any guest profile**; stay history limited to assigned properties; rooms CRUD at assigned sites |

Demo assignments come from seed data — see [seeding guide](../../seeding.md).

## Demo accounts (development)

Run `npm run seed` in `scripts/` to create Firebase Auth users and Firestore data. Copy `scripts/.env.example` → `scripts/.env` and set `SEED_ADMIN_PASSWORD` / `SEED_MANAGER_PASSWORD` (min 12 characters).

| Email | Password | Display name | Role | Property IDs |
|-------|----------|--------------|------|----------------|
| `admin@chain.com` | `SEED_ADMIN_PASSWORD` | Chain Admin | Chain admin | All (1–12) |
| `manager.mountain@chain.com` | `SEED_MANAGER_PASSWORD` | Alex Mountain | Property manager | 1, 3, 7, 11 |
| `manager.coastal@chain.com` | `SEED_MANAGER_PASSWORD` | Sam Coastal | Property manager | 2, 4, 8 |
| `manager.southwest@chain.com` | `SEED_MANAGER_PASSWORD` | Jordan Southwest | Property manager | 6, 9 |
| `manager.east@chain.com` | `SEED_MANAGER_PASSWORD` | Taylor East | Property manager | 5, 10, 12 |

### Manager → property map

| Manager | Properties |
|---------|------------|
| Alex Mountain | Hill View Guesthouse, Cedar Inn, Pinecrest Lodge, Summit Stay |
| Sam Coastal | Riverside Lodge, Harbor House, Lakeside Haven |
| Jordan Southwest | Sunstone Villa, Desert Bloom Inn |
| Taylor East | Maple Retreat, Oak & Ivy Guesthouse, Meadowbrook Cottage |

## Property manager contacts (by region)

| Region | Manager | Email |
|--------|---------|-------|
| Mountain West | Alex Mountain | manager.mountain@chain.com |
| Pacific NW, Coastal, Midwest (Lakeside) | Sam Coastal | manager.coastal@chain.com |
| Southwest | Jordan Southwest | manager.southwest@chain.com |
| Northeast, Southeast, Midwest (Meadowbrook) | Taylor East | manager.east@chain.com |

For chain-wide issues, use the chain admin account.
