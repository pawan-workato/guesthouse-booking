# Booking procedures

## Creating a booking

Staff create bookings from the **Book** tab or **Book for guest** on a room detail screen.

### Search and filters (Book tab)

- **Search properties** — filters the property picker by name, region, or address.
- After selecting a property, the app shows **room count and type breakdown** (e.g. `12 rooms · 4 Double · 3 Single`).
- **Room type chips** — tap to filter the room list (tap again to clear).
- **Search rooms** — filters by name, description, or type label.

### Required information

| Field | Required | Notes |
|-------|----------|-------|
| Guest name | **Yes** | Trimmed; blank rejected |
| Guest email / phone | No | Trimmed when provided |
| Property | Yes | Scoped to your assignments |
| Room | Yes | Must belong to selected property |
| Check-in / check-out | Yes | Check-out must be **after** check-in |

### Date selection

- Tap check-in, then check-out on the calendar.
- **Confirmed** bookings and **block dates** show as unavailable.
- Nights are **check-in through the day before check-out** (check-out morning is not a charged night).

### Validation

1. Guest name not empty.
2. `check-out > check-in`.
3. No overlap with other **confirmed** bookings for the same room (`CONFIRMED` status only; cancelled bookings do not block).

On failure: e.g. *"Room is not available for those dates"*.

## Editing a booking

**Bookings** → **Edit** on a **confirmed** reservation. You can change room, guest fields, and dates (same overlap rules). Managers can only edit bookings at properties they can access.

## Viewing bookings

**Bookings** tab — newest check-in first. Shows property, room, guest, dates, booking status, and **sync status**. Cancelled bookings are hidden by default; turn on **Show cancelled** to include them.

Actions on confirmed bookings: **Edit**, **Check in**, **Cancel**. On checked-in: **Check out**.

## Today board

**Today** tab — **Arrivals**, **Departures**, **In-house** for today (filter by property if you have several). Use **Check in** / **Check out** when operational rules allow (app enforces arrival/departure dates).

## Cancellation

- **Bookings** → **Cancel** on a confirmed booking → status **CANCELLED**; room freed on calendar.
- No payment or refund logic in the app.

| Scenario | Action |
|----------|--------|
| Same-day change | Cancel and rebook if needed |
| No-show | Cancel after property policy |
| Early departure | Cancel remaining nights manually |

## Check-in and check-out times

Default (see [chain overview](chain-overview.md)): **15:00** check-in, **11:00** check-out. The app tracks **dates** only, not time-of-day.

## Capacity

Each room has a **capacity** field. The app does not block over-capacity bookings — verify party size before confirming.

## Block dates

From **room detail** → **Block dates**. Blocked ranges appear in orange on the calendar and prevent new overlapping bookings. Block dates sync to Firestore like bookings (offline creates queue as **PENDING SYNC** until upload).
