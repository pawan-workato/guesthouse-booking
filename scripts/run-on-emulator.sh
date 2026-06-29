#!/usr/bin/env bash
# Build and install the debug app on a running Android emulator.
#
# Usage:
#   ./scripts/run-on-emulator.sh              # build + install + launch
#   ./scripts/run-on-emulator.sh --pull       # git pull main first
#   ./scripts/run-on-emulator.sh --fresh      # uninstall app, then reinstall
#   ./scripts/run-on-emulator.sh --start-avd  # boot first AVD if none running
#
# Prerequisites:
#   - Android Studio SDK + at least one AVD (Device Manager)
#   - Emulator running, unless you pass --start-avd
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_ID="com.guesthouse.booking"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

DO_PULL=false
DO_FRESH=false
DO_START_AVD=false

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --pull) DO_PULL=true ;;
    --fresh) DO_FRESH=true ;;
    --start-avd) DO_START_AVD=true ;;
    -h|--help) usage 0 ;;
    *)
      echo "Unknown option: $1" >&2
      usage 1
      ;;
  esac
  shift
done

if [[ "$(uname -s)" == "Darwin" ]]; then
  AS_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [[ -z "${JAVA_HOME:-}" && -d "$AS_JBR" ]]; then
    export JAVA_HOME="$AS_JBR"
  fi
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Set ANDROID_HOME or install Android SDK platform-tools." >&2
  exit 1
fi

if [[ "$DO_PULL" == true ]]; then
  echo "==> Pulling latest main..."
  git -C "$ROOT" fetch origin main
  git -C "$ROOT" checkout main
  git -C "$ROOT" pull --ff-only origin main
fi

wait_for_emulator() {
  local serial="$1"
  echo "==> Waiting for emulator ($serial)..."
  adb -s "$serial" wait-for-device
  local booted=""
  for _ in $(seq 1 120); do
    booted="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$booted" == "1" ]]; then
      return 0
    fi
    sleep 1
  done
  echo "Emulator did not finish booting in time." >&2
  exit 1
}

pick_emulator_serial() {
  adb devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1; exit }'
}

SERIAL="$(pick_emulator_serial || true)"

if [[ -z "$SERIAL" && "$DO_START_AVD" == true ]]; then
  if ! command -v emulator >/dev/null 2>&1; then
    echo "emulator binary not found under ANDROID_HOME." >&2
    exit 1
  fi
  AVD_NAME="$(emulator -list-avds | head -n 1)"
  if [[ -z "$AVD_NAME" ]]; then
    echo "No AVDs found. Create one in Android Studio → Device Manager." >&2
    exit 1
  fi
  echo "==> Starting AVD: $AVD_NAME"
  nohup emulator -avd "$AVD_NAME" -netdelay none -netspeed full >/dev/null 2>&1 &
  sleep 3
  SERIAL="$(pick_emulator_serial || true)"
fi

if [[ -z "$SERIAL" ]]; then
  echo "No emulator running." >&2
  echo "Start one in Android Studio (Device Manager), or re-run with --start-avd." >&2
  exit 1
fi

wait_for_emulator "$SERIAL"
echo "==> Using device: $SERIAL"

if [[ "$DO_FRESH" == true ]]; then
  echo "==> Uninstalling existing app (fresh install)..."
  adb -s "$SERIAL" uninstall "$APP_ID" >/dev/null 2>&1 || true
fi

echo "==> Building and installing debug APK..."
cd "$ROOT"
./gradlew :app:installDebug

echo "==> Launching app..."
adb -s "$SERIAL" shell am start -n "$MAIN_ACTIVITY" >/dev/null

echo ""
echo "Done. App installed on $SERIAL."
echo "Demo login: admin@chain.com (password from scripts/.env SEED_ADMIN_PASSWORD)"
