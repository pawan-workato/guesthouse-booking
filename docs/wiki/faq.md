# FAQ

Common questions from property staff using the booking app.

## General

### Do guests log in to the app?

No. Staff enter guest details when creating a booking.

### Can guests pay through the app?

No. Payments are handled outside the app.

### How many properties are in the chain?

Twelve. See [chain overview](chain-overview.md).

### Where is the Sync tab?

There is no Sync tab. Use the **sync icon** in the top app bar (next to sign out). The badge shows pending uploads and conflicts.

## Bookings

### What happens if I pick overlapping dates?

The app blocks the booking: *"Room is not available for those dates."* Only **confirmed** bookings and **block dates** count; cancelled bookings free the room.

### Can I edit a booking after it's created?

Yes — **Bookings** → **Edit** on a **confirmed** reservation (room, guest, dates).

### How do I check a guest in or out?

Use **Check in** / **Check out** on **Bookings**, or the **Today** tab (arrivals / departures / in-house).

### Is guest email required?

No. **Guest name** is the only required field.

### Does check-out day count as a booked night?

No. Check-in Mon → check-out Wed books Mon and Tue nights only.

### How do I cancel?

**Bookings** → **Cancel** on a confirmed booking.

## Properties and rooms

### What are room types?

Each room has a type: **Single**, **Double**, **Suite**, or **Family**. Shown on property room lists, room cards, and the **Book** tab (summary line + filter chips).

### Can managers add or change rooms?

Yes — at **assigned properties** only: **+** on the room list or **Edit** on a room. Chain admins can manage rooms at any property.

### Where do prices come from?

Stored per room in the app (seed data or staff edits). Prices are for reference when quoting guests — no payment processing.

### Standard check-in / check-out times?

**15:00** and **11:00** unless a property page says otherwise.

### Which manager handles my property?

See [staff guide — manager map](staff-guide.md#manager--property-map).

### Why don't I see every guest?

All signed-in staff can view every guest profile (name, email, phone, notes). Guest screens do not show booking history. Property managers can only edit guests linked to bookings at **their** properties; chain admins can edit any guest.

## Demo / development

### Test passwords?

| Account | Password |
|---------|----------|
| `admin@chain.com` | `admin123` |
| All `manager.*@chain.com` | `manager123` |

### Why don't I see bookings from another phone?

Data syncs when online via Firebase. Offline bookings stay on the device until sync — [offline operations](offline-operations.md).

### Empty app after an update?

Room DB upgrades use migrations (v10). If data looks wrong, clear app storage or reinstall, then sign in **online** to pull bootstrap data. Seed Firebase with `npm run seed` in `scripts/`.

### How do I run on the emulator quickly?

```bash
./scripts/run-on-emulator.sh --fresh
```

See root [README](../../README.md).
