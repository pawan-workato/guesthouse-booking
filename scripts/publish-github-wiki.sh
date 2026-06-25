#!/usr/bin/env bash
# Publishes docs/wiki/ to the GitHub Wiki git repository.
#
# First-time setup: create a blank Home page so the wiki repo exists:
#   https://github.com/<owner>/<repo>/wiki/_new
#
# Local: run with gh auth login, set GITHUB_TOKEN, or set WIKI_REPO_URL.
# CI:   GITHUB_TOKEN and GITHUB_REPOSITORY are set by GitHub Actions.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOCS="$ROOT/docs/wiki"
TMP="${TMPDIR:-/tmp}/guesthouse-wiki-publish"
REPO="${GITHUB_REPOSITORY:-pawan-workato/guesthouse-booking}"
WIKI_CREATE_URL="https://github.com/${REPO}/wiki/_new"

if [[ "${GITHUB_ACTIONS:-}" == "true" && -z "${GITHUB_TOKEN:-}" && -z "${WIKI_REPO_URL:-}" ]]; then
  echo "::error::GITHUB_TOKEN is not set. Cannot authenticate push to wiki."
  exit 1
fi

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

  python3 - "$DOCS" "$dest" "$REPO" << 'PY'
import sys
from pathlib import Path

docs_root = Path(sys.argv[1])
dest_root = Path(sys.argv[2])
repo = sys.argv[3]


def title_segment(segment: str) -> str:
    if len(segment) <= 3:
        return segment.upper()
    return segment.capitalize()


def wiki_page_name(rel: Path) -> str:
    rel_posix = rel.as_posix()
    if rel_posix == "README.md":
        return "Home"
    if rel.stem == "faq":
        return "FAQ"
    if rel.parts and rel.parts[0] == "properties":
        stem = rel.stem
        titled = "-".join(title_segment(part) for part in stem.split("-"))
        return f"Properties-{titled}"
    stem = rel.stem
    return stem[0].upper() + stem[1:] if stem else stem


def wiki_dest_file(rel: Path) -> str:
    return f"{wiki_page_name(rel)}.md"


source_files = sorted(docs_root.rglob("*.md"))
if not source_files:
    raise SystemExit(f"No markdown files found under {docs_root}")

dest_names: set[str] = set()
link_map: dict[str, str] = {}

for src in source_files:
    rel = src.relative_to(docs_root)
    page = wiki_page_name(rel)
    dest_name = wiki_dest_file(rel)
    dest_names.add(dest_name)

    dest_path = dest_root / dest_name
    dest_path.write_text(src.read_text())

    link_map[rel.as_posix()] = page
    link_map[rel.name] = page
    if len(rel.parts) > 1:
        link_map[str(rel).replace("\\", "/")] = page

link_map["../../README.md"] = f"https://github.com/{repo}"

for dest_file in dest_names:
    path = dest_root / dest_file
    text = path.read_text()
    for old, new in sorted(link_map.items(), key=lambda item: len(item[0]), reverse=True):
        text = text.replace(old, new)
    path.write_text(text)

for existing in dest_root.glob("*.md"):
    if existing.name not in dest_names:
        existing.unlink()

print(f"Synced {len(dest_names)} wiki page(s) from {docs_root}")
PY
}

clone_err="$(mktemp)"
rm -rf "$TMP"
if ! git clone --depth 1 "$WIKI_URL" "$TMP" 2>"$clone_err"; then
  echo "::error title=GitHub Wiki not initialized::Could not clone ${REPO}.wiki.git"
  echo "The wiki repository does not exist yet or is not accessible."
  echo "Create the wiki by adding a first Home page at: ${WIKI_CREATE_URL}"
  echo "Then re-run this workflow or trigger Sync Wiki manually."
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
