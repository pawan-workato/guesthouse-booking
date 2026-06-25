# Booking procedures

## Creating a booking

Staff create bookings on behalf of guests from the **Book** tab (or via **Book now** on a room detail screen).

### Required guest information

| Field | Required | Notes |
|-------|----------|-------|
| Guest name | **Yes** | Cannot be blank; stored trimmed |
| Guest email | No | Stored trimmed; collect when available for confirmations |
| Guest phone | No | Stored trimmed; primary contact for day-of coordination |
| Property | Yes | Selected from property list |
| Room | Yes | Must belong to selected property |
| Check-in date | Yes | First night of stay |
| Check-out date | Yes | Must be **after** check-in (departure day, not a booked night) |

### Date selection

- Tap check-in on the calendar, then check-out.
- Booked nights show as unavailable on the calendar.
- A stay from Monday check-in to Wednesday check-out occupies **Monday and Tuesday** nights (check-out morning is not charged as a night).

### Validation rules

The app enforces these rules at booking time:

1. **Guest name** must not be empty.
2. **Check-out** must be strictly after **check-in** (`check-out <= check-in` is rejected).
3. **No overlap** with existing **confirmed** bookings for the same room.

Overlap detection uses: `existing.checkIn < new.checkOut AND existing.checkOut > new.checkIn`. Only bookings with status **CONFIRMED** block availability; cancelled bookings do not.

If validation fails, the app shows an error (e.g. "Room is not available for those dates") and the booking is not created.

## Viewing bookings

The **Bookings** tab lists all reservations, newest check-in first. Each entry shows:

- Guest name and contact info
- Property and room
- Check-in → check-out dates
- Status (confirmed or cancelled)

## Cancellation policy

### In the app

- Staff can cancel any **confirmed** booking from the **Bookings** tab.
- Cancellation sets status to **CANCELLED**; the record remains for audit but **no longer blocks** the room calendar.
- There is no automatic refund or fee calculation in the app (payments are out of scope).

### Operational guidance

| Timing | Recommended action |
|--------|-------------------|
| Same day / walk-in change | Cancel in app and rebook if needed |
| Guest no-show | Cancel after property check-out time on day after expected arrival |
| Early departure | Cancel remaining nights manually; note in guest file if your property uses one |

Always confirm with the guest before cancelling on their behalf.

## Check-in and check-out times

Default property times (see [chain overview](chain-overview.md)):

- Check-in from **15:00**
- Check-out by **11:00**

Early check-in or late check-out requires manual coordination — the app does not track time-of-day, only calendar dates.

## Capacity

Each room has a maximum **capacity** (guest count). The app stores capacity per room but does not currently block over-capacity bookings; staff should verify party size against the room listing before confirming.
