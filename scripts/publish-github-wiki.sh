#!/usr/bin/env bash
# Publishes docs/wiki/ to GitHub Wiki. First create a blank Home page at:
# https://github.com/pawan-workato/guesthouse-booking/wiki/_new
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOCS="$ROOT/docs/wiki"
TMP="${TMPDIR:-/tmp}/guesthouse-wiki-publish"
rm -rf "$TMP" && mkdir -p "$TMP"
cp "$DOCS/README.md" "$TMP/Home.md"
cp "$DOCS/chain-overview.md" "$TMP/Chain-overview.md"
cp "$DOCS/staff-guide.md" "$TMP/Staff-guide.md"
cp "$DOCS/booking-procedures.md" "$TMP/Booking-procedures.md"
cp "$DOCS/offline-operations.md" "$TMP/Offline-operations.md"
cp "$DOCS/faq.md" "$TMP/FAQ.md"
cp "$DOCS/properties/mountain-west.md" "$TMP/Properties-Mountain-West.md"
cp "$DOCS/properties/pacific-nw.md" "$TMP/Properties-Pacific-NW.md"
cp "$DOCS/properties/coastal.md" "$TMP/Properties-Coastal.md"
cp "$DOCS/properties/northeast.md" "$TMP/Properties-Northeast.md"
cp "$DOCS/properties/southwest.md" "$TMP/Properties-Southwest.md"
cp "$DOCS/properties/midwest.md" "$TMP/Properties-Midwest.md"
cp "$DOCS/properties/southeast.md" "$TMP/Properties-Southeast.md"
python3 - "$TMP/Home.md" << 'PY'
import sys
from pathlib import Path
home = Path(sys.argv[1])
text = home.read_text()
for old, new in {
    "chain-overview.md": "Chain-overview", "staff-guide.md": "Staff-guide",
    "booking-procedures.md": "Booking-procedures", "offline-operations.md": "Offline-operations",
    "faq.md": "FAQ", "properties/mountain-west.md": "Properties-Mountain-West",
    "properties/pacific-nw.md": "Properties-Pacific-NW", "properties/coastal.md": "Properties-Coastal",
    "properties/northeast.md": "Properties-Northeast", "properties/southwest.md": "Properties-Southwest",
    "properties/midwest.md": "Properties-Midwest", "properties/southeast.md": "Properties-Southeast",
    "../../README.md": "https://github.com/pawan-workato/guesthouse-booking",
}.items():
    text = text.replace(old, new)
home.write_text(text)
PY
cd "$TMP" && git init -q && git add -A && git commit -q -m "Publish wiki from docs/wiki"
gh auth setup-git
git remote add origin "https://github.com/pawan-workato/guesthouse-booking.wiki.git"
git branch -M master
git push -u origin master --force
echo "Done: https://github.com/pawan-workato/guesthouse-booking/wiki"
