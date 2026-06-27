# Staff guide

## Who uses the app

The Android app is **staff-only**. Property managers book rooms for guests; guests do not log in or pay through the app.

## App navigation

The bottom bar has five tabs:

| Tab | Purpose |
|-----|---------|
| **Properties** | Browse all chain locations; tap a property to see its rooms |
| **Guests** | Add and maintain guest profiles; search by name, email, or phone |
| **Book** | Create a booking — pick a saved guest or enter details manually |
| **Sync** | Pending offline bookings, conflicts, and manual sync |
| **Bookings** | View all bookings and cancel confirmed reservations |

### Typical workflow

1. Open **Properties** and find the guest's location (search by name or region).
2. Tap the property → tap a room → review availability on the calendar.
3. Optionally add the guest under **Guests** if they're a repeat visitor.
4. Tap **Book now** (or go to the **Book** tab), pick the saved guest or enter details, and confirm dates.
5. Confirm the booking appears under **Bookings**.



## Guest management

All staff can manage guest profiles from the **Guests** tab:

- **Add** — tap the **+** floating button
- **Edit** — tap the pencil icon on a guest card
- **Remove** — on the edit screen, tap **Remove guest** (hides from active list; existing bookings keep their details)
- **Show removed** — toggle to view inactive profiles and reactivate them

When booking, use **Saved guest** on the **Book** tab to auto-fill name, phone, and email. You can still edit fields or enter a one-off guest manually.

## Property management (chain admin)

Chain admins can manage the property list from the **Properties** tab:

- **Add** — tap the **+** floating button
- **Edit** — tap the pencil icon on a property card
- **Remove** — on the edit screen, tap **Remove property** (hides the site; existing bookings and rooms are kept)
- **Show removed** — toggle to view inactive properties and reactivate them

Property managers only see active properties assigned to their account.

## Staff roles

| Role | Access |
|------|--------|
| **Chain admin** | All 12 properties; full bookings admin |
| **Property manager** | Only assigned properties; bookings for those locations |

Role definitions and property assignments match the seeded demo accounts in `DatabaseSeeder.kt`. Chain admins can access every property; managers are scoped to their assignments.

## Demo accounts (development)

Use these credentials in development builds with seeded data:

| Email | Password | Display name | Role | Assigned properties (IDs) |
|-------|----------|--------------|------|---------------------------|
| `admin@chain.com` | `admin123` | Chain Admin | Chain admin | All (1–12) |
| `manager.mountain@chain.com` | `manager123` | Alex Mountain | Property manager | 1, 3, 7, 11 |
| `manager.coastal@chain.com` | `manager123` | Sam Coastal | Property manager | 2, 4, 8 |
| `manager.southwest@chain.com` | `manager123` | Jordan Southwest | Property manager | 6, 9 |
| `manager.east@chain.com` | `manager123` | Taylor East | Property manager | 5, 10, 12 |

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
