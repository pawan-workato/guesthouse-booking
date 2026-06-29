# Firestore staff seed documents (manual fallback)

> **Recommended:** run `npm run seed` in `scripts/` instead — see [README.md](README.md).

Create Firebase Auth users first (see [docs/seeding.md](../docs/seeding.md)), then add one `staff/{firebaseUid}` document per user. The document ID must match the Auth user's UID.

Replace `<FIREBASE_UID>` with the UID from Firebase Console → Authentication.

## Chain admin

**Email:** [admin@chain.com](mailto:admin@chain.com) · **Password:** admin123

```json
{
  "staffId": 1,
  "email": "admin@chain.com",
  "displayName": "Chain Admin",
  "role": "CHAIN_ADMIN",
  "assignedPropertyIds": [],
  "firebaseUid": "M6GHY61M6PYpXDBf8iaUwVkFPgd2"
}
```

> Document ID must equal the Auth user's UID.



## Property managers



### Alex Mountain — Mountain West

**Email:** [manager.mountain@chain.com](mailto:manager.mountain@chain.com) · **Password:** manager123

```json
{
  "staffId": 2,
  "email": "manager.mountain@chain.com",
  "displayName": "Alex Mountain",
  "role": "PROPERTY_MANAGER",
  "assignedPropertyIds": [1, 3, 7, 11],
  "firebaseUid": "5iiNmRCFJkhi8q4m0lDez1GLu4A2"
}
```



### Sam Coastal — Coastal / Pacific NW

**Email:** [manager.coastal@chain.com](mailto:manager.coastal@chain.com) · **Password:** manager123

```json
{
  "staffId": 3,
  "email": "manager.coastal@chain.com",
  "displayName": "Sam Coastal",
  "role": "PROPERTY_MANAGER",
  "assignedPropertyIds": [2, 4, 8],
  "firebaseUid": "OqbKMTUPn4R0pcbe1vKjtBbDR5D3"
}
```



### Jordan Southwest — Southwest

**Email:** [manager.southwest@chain.com](mailto:manager.southwest@chain.com) · **Password:** manager123

```json
{
  "staffId": 4,
  "email": "manager.southwest@chain.com",
  "displayName": "Jordan Southwest",
  "role": "PROPERTY_MANAGER",
  "assignedPropertyIds": [6, 9],
  "firebaseUid": "5bdIOYB6Ssd2YmR5ebDJP0owWEk2"
}
```



### Taylor East — Northeast / Southeast / Midwest

**Email:** [manager.east@chain.com](mailto:manager.east@chain.com) · **Password:** manager123

```json
{
  "staffId": 5,
  "email": "manager.east@chain.com",
  "displayName": "Taylor East",
  "role": "PROPERTY_MANAGER",
  "assignedPropertyIds": [5, 10, 12],
  "firebaseUid": "zuu5NwjDrTTt2clO6qRQemlun4t1"
}
```



## Field reference


| Field               | Type             | Description                         |
| ------------------- | ---------------- | ----------------------------------- |
| staffId             | number           | Matches local seed ID (1–5)         |
| email               | string           | Must match Firebase Auth email      |
| displayName         | string           | Shown in app header                 |
| role                | string           | `CHAIN_ADMIN` or `PROPERTY_MANAGER` |
| assignedPropertyIds | array of numbers | Property IDs; empty for chain admin |
| firebaseUid         | string           | Same as document ID and Auth UID    |




## Automated alternative

Use the Admin SDK seed script instead of manual Console steps:

```bash
cd scripts && npm install && npm run seed
```

See [README.md](README.md). The in-app Sync upload handles properties, rooms, and guests only.