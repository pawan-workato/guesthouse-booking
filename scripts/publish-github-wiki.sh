#!/usr/bin/env bash
# Publishes docs/wiki/ to the GitHub Wiki git repository.
#
# First-time setup: create a blank Home page so the wiki repo exists:
#   https://github.com/<owner>/<repo>/wiki/_new
#
# Local: run with gh auth login, or set GITHUB_TOKEN.
# CI:   GITHUB_TOKEN and GITHUB_REPOSITORY are set by GitHub Actions.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOCS="$ROOT/docs/wiki"
TMP="${TMPDIR:-/tmp}/guesthouse-wiki-publish"
REPO="${GITHUB_REPOSITORY:-pawan-workato/guesthouse-booking}"
WIKI_CREATE_URL="https://github.com/${REPO}/wiki/_new"

if [[ -n "${WIKI_REPO_URL:-}" ]]; then
  WIKI_URL="$WIKI_REPO_URL"
elif [[ -n "${GITHUB_TOKEN:-}" ]]; then
  WIKI_URL="https://x-access-token:${GITHUB_TOKEN}@github.com/${REPO}.wiki.git"
else
  WIKI_URL="https://github.com/${REPO}.wiki.git"
fi

sync_wiki_files() {
  local dest="$1"
  mkdir -p "$dest"
  cp "$DOCS/README.md" "$dest/Home.md"
  cp "$DOCS/chain-overview.md" "$dest/Chain-overview.md"
  cp "$DOCS/staff-guide.md" "$dest/Staff-guide.md"
  cp "$DOCS/booking-procedures.md" "$dest/Booking-procedures.md"
  cp "$DOCS/offline-operations.md" "$dest/Offline-operations.md"
  cp "$DOCS/faq.md" "$dest/FAQ.md"
  cp "$DOCS/properties/mountain-west.md" "$dest/Properties-Mountain-West.md"
  cp "$DOCS/properties/pacific-nw.md" "$dest/Properties-Pacific-NW.md"
  cp "$DOCS/properties/coastal.md" "$dest/Properties-Coastal.md"
  cp "$DOCS/properties/northeast.md" "$dest/Properties-Northeast.md"
  cp "$DOCS/properties/southwest.md" "$dest/Properties-Southwest.md"
  cp "$DOCS/properties/midwest.md" "$dest/Properties-Midwest.md"
  cp "$DOCS/properties/southeast.md" "$dest/Properties-Southeast.md"

  python3 - "$dest/Home.md" "$REPO" << 'PY'
import sys
from pathlib import Path

home = Path(sys.argv[1])
repo = sys.argv[2]
text = home.read_text()
for old, new in {
    "chain-overview.md": "Chain-overview",
    "staff-guide.md": "Staff-guide",
    "booking-procedures.md": "Booking-procedures",
    "offline-operations.md": "Offline-operations",
    "faq.md": "FAQ",
    "properties/mountain-west.md": "Properties-Mountain-West",
    "properties/pacific-nw.md": "Properties-Pacific-NW",
    "properties/coastal.md": "Properties-Coastal",
    "properties/northeast.md": "Properties-Northeast",
    "properties/southwest.md": "Properties-Southwest",
    "properties/midwest.md": "Properties-Midwest",
    "properties/southeast.md": "Properties-Southeast",
    "../../README.md": f"https://github.com/{repo}",
}.items():
    text = text.replace(old, new)
home.write_text(text)
PY
}

clone_err="$(mktemp)"
rm -rf "$TMP"
if ! git clone --depth 1 "$WIKI_URL" "$TMP" 2>"$clone_err"; then
  echo "::error title=GitHub Wiki not initialized::Could not clone ${REPO}.wiki.git"
  echo "The wiki repository does not exist yet or is not accessible."
  echo "Create the wiki by adding a first Home page at: ${WIKI_CREATE_URL}"
  echo "Then re-run this workflow or push another change under docs/wiki/."
  if [[ -s "$clone_err" ]]; then
    echo "--- git clone stderr ---"
    cat "$clone_err"
  fi
  rm -f "$clone_err"
  exit 1
fi
rm -f "$clone_err"

sync_wiki_files "$TMP"
cd "$TMP"

if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  git config user.name "github-actions[bot]"
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD)"

if git diff --quiet && git diff --cached --quiet; then
  echo "Wiki is already up to date with docs/wiki/."
  exit 0
fi

git add -A
git commit -m "Publish wiki from docs/wiki"
git push origin "$BRANCH"

echo "Done: https://github.com/${REPO}/wiki"
