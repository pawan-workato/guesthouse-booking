# FAQ

Common questions from property staff using the booking app.

## General

### Do guests log in to the app?

No. The app is staff-only. You enter guest name, phone, and email when creating a booking.

### Can guests pay through the app?

No. Payments and invoicing are handled outside the app.

### How many properties are in the chain?

Twelve. See the [chain overview](chain-overview.md) for the full list.

## Bookings

### What happens if I pick overlapping dates?

The app blocks the booking and shows "Room is not available for those dates." Only **confirmed** bookings count; cancelled ones free the room.

### Is guest email required?

No, but collect it when possible for follow-up. **Guest name** is the only required field.

### Does check-out day count as a booked night?

No. A stay from June 1 check-in to June 3 check-out books June 1 and June 2 only.

### How do I cancel a booking?

Open **Bookings**, find the reservation, and tap **Cancel booking**. Status changes to cancelled; the room becomes available again.

## Properties and rooms

### Where do room prices come from?

Each room has a seeded **price per night** in the app (see property pages under [properties/](properties/)). The app does not process payment — prices are for reference when quoting guests.

### What are the standard check-in and check-out times?

**15:00** check-in and **11:00** check-out unless a property page notes otherwise.

### Which manager handles my property?

See the [staff guide](staff-guide.md#manager--property-map) for manager assignments and demo login emails.

## Demo / development

### What are the test login passwords?

| Account | Password |
|---------|----------|
| `admin@chain.com` | `admin123` |
| All `manager.*@chain.com` accounts | `manager123` |

### Why don't I see bookings from another phone?

Bookings sync when the device is online via the Ktor API or Firestore. Offline bookings stay on the device until sync completes — see [offline operations](offline-operations.md).

### The database looks empty after an update

If the local Room cache looks empty after an app update, clear app data (or reinstall), then sign in again while online to pull properties, rooms, and bookings from Firestore. Demo staff and entity data are seeded in Firebase via `scripts/seed-firebase-demo.mjs` — see [seeding.md](../seeding.md).
