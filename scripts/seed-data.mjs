/**
 * Demo seed data — mirrors backend DatabaseSeeder.kt entity definitions
 */

export const ADMIN_PASSWORD = 'admin123';
export const MANAGER_PASSWORD = 'manager123';

export const staffProfiles = [
  {
    staffId: 1,
    email: 'admin@chain.com',
    password: ADMIN_PASSWORD,
    displayName: 'Chain Admin',
    role: 'CHAIN_ADMIN',
    assignedPropertyIds: [],
  },
  {
    staffId: 2,
    email: 'manager.mountain@chain.com',
    password: MANAGER_PASSWORD,
    displayName: 'Alex Mountain',
    role: 'PROPERTY_MANAGER',
    assignedPropertyIds: [1, 3, 7, 11],
  },
  {
    staffId: 3,
    email: 'manager.coastal@chain.com',
    password: MANAGER_PASSWORD,
    displayName: 'Sam Coastal',
    role: 'PROPERTY_MANAGER',
    assignedPropertyIds: [2, 4, 8],
  },
  {
    staffId: 4,
    email: 'manager.southwest@chain.com',
    password: MANAGER_PASSWORD,
    displayName: 'Jordan Southwest',
    role: 'PROPERTY_MANAGER',
    assignedPropertyIds: [6, 9],
  },
  {
    staffId: 5,
    email: 'manager.east@chain.com',
    password: MANAGER_PASSWORD,
    displayName: 'Taylor East',
    role: 'PROPERTY_MANAGER',
    assignedPropertyIds: [5, 10, 12],
  },
];

export const properties = [
  { id: 1, name: 'Hill View Guesthouse', address: '142 Ridge Rd, Aspen, CO', region: 'Mountain West' },
  { id: 2, name: 'Riverside Lodge', address: '88 River Walk, Portland, OR', region: 'Pacific NW' },
  { id: 3, name: 'Cedar Inn', address: '19 Cedar Ln, Bozeman, MT', region: 'Mountain West' },
  { id: 4, name: 'Harbor House', address: '5 Pier St, Monterey, CA', region: 'Coastal' },
  { id: 5, name: 'Maple Retreat', address: '301 Maple Ave, Burlington, VT', region: 'Northeast' },
  { id: 6, name: 'Sunstone Villa', address: '44 Desert Dr, Sedona, AZ', region: 'Southwest' },
  { id: 7, name: 'Pinecrest Lodge', address: '77 Pine Rd, Jackson, WY', region: 'Mountain West' },
  { id: 8, name: 'Lakeside Haven', address: '12 Lakeview Dr, Traverse City, MI', region: 'Midwest' },
  { id: 9, name: 'Desert Bloom Inn', address: '210 Cactus Way, Santa Fe, NM', region: 'Southwest' },
  { id: 10, name: 'Oak & Ivy Guesthouse', address: '56 Oak St, Asheville, NC', region: 'Southeast' },
  { id: 11, name: 'Summit Stay', address: '901 Summit Blvd, Denver, CO', region: 'Mountain West' },
  { id: 12, name: 'Meadowbrook Cottage', address: '3 Meadow Ln, Madison, WI', region: 'Midwest' },
];


function inferRoomType(name, capacity) {
  const lower = name.toLowerCase();
  if (lower.includes('single')) return 'SINGLE';
  if (lower.includes('double')) return 'DOUBLE';
  if (lower.includes('suite')) return 'SUITE';
  if (lower.includes('family') || lower.includes('cottage')) return 'FAMILY';
  if (lower.includes('den') && capacity >= 4) return 'FAMILY';
  if (capacity <= 1) return 'SINGLE';
  if (capacity >= 4) return 'FAMILY';
  return 'DOUBLE';
}

const roomRows = [
  [1, 'Garden Suite', 'Ground-floor patio and garden views.', 89.0, 2],
  [1, 'Loft Room', 'Upper-floor skylight and workspace.', 75.0, 2],
  [1, 'Family Room', 'Two queen beds for families.', 120.0, 4],
  [2, 'River View', 'Balcony overlooking the river.', 95.0, 2],
  [2, 'Studio', 'Compact studio with kitchenette.', 70.0, 2],
  [2, 'Suite', 'Separate living area and bedroom.', 130.0, 3],
  [3, 'Cedar Double', 'Warm wood finishes, mountain view.', 80.0, 2],
  [3, 'Cozy Single', 'Ideal for solo travelers.', 55.0, 1],
  [4, 'Harbor King', 'King bed with ocean glimpse.', 110.0, 2],
  [4, 'Anchor Room', 'Nautical theme, queen bed.', 85.0, 2],
  [4, "Captain's Suite", 'Corner suite with bay windows.', 145.0, 4],
  [5, 'Maple Standard', 'Classic room with maple grove view.', 78.0, 2],
  [5, 'Autumn Suite', 'Spacious suite, fireplace.', 115.0, 3],
  [6, 'Adobe Room', 'Southwestern adobe styling.', 92.0, 2],
  [6, 'Terrace Double', 'Private terrace, red rock views.', 105.0, 2],
  [6, 'Poolside', 'Steps from the courtyard pool.', 98.0, 2],
  [7, 'Pine Standard', 'Forest-facing double room.', 88.0, 2],
  [7, 'Bear Den', 'Rustic lodge feel, two beds.', 100.0, 4],
  [8, 'Lakeview Double', 'Direct lake views.', 90.0, 2],
  [8, 'Dock Room', 'Near the private dock.', 82.0, 2],
  [8, 'Family Cottage', 'Two-bedroom cottage unit.', 155.0, 6],
  [9, 'Bloom Single', 'Courtyard garden access.', 65.0, 1],
  [9, 'Adobe Double', 'Traditional pueblo design.', 85.0, 2],
  [10, 'Ivy Room', 'Garden-level, wheelchair accessible.', 79.0, 2],
  [10, 'Oak Suite', 'Top floor with mountain views.', 112.0, 3],
  [10, 'Carriage House', 'Detached unit with kitchen.', 135.0, 4],
  [11, 'Summit Double', 'City and mountain skyline.', 86.0, 2],
  [11, 'Alpine Room', 'Quiet rear-facing room.', 72.0, 2],
  [12, 'Meadow Double', 'Pasture views, ground floor.', 74.0, 2],
  [12, 'Brook Suite', 'Stream-side suite with sitting area.', 99.0, 3],
];

export const rooms = roomRows.map(([propertyId, name, description, pricePerNight, capacity], index) => ({
  id: index + 1,
  propertyId,
  name,
  description,
  pricePerNight,
  capacity,
  roomType: inferRoomType(name, capacity),
}));

const DEMO_EPOCH_MS = 1700000000000;

export const guests = [
  { id: 1, name: 'Maria Chen', email: 'maria.chen@example.com', phone: '+1 555-0101', notes: 'Prefers ground-floor rooms' },
  { id: 2, name: "James O'Brien", email: 'j.obrien@example.com', phone: '+1 555-0102', notes: 'Late check-in often' },
  { id: 3, name: 'Priya Sharma', email: 'priya.sh@example.com', phone: '+1 555-0103', notes: '' },
  { id: 4, name: 'Robert Kim', email: 'r.kim@example.com', phone: '+1 555-0104', notes: 'Traveling with service dog' },
  { id: 5, name: 'Elena Vasquez', email: 'elena.v@example.com', phone: '+1 555-0105', notes: 'Allergic to down pillows' },
].map((guest) => ({ ...guest, isActive: true, createdAtEpochMs: DEMO_EPOCH_MS }));
