#!/usr/bin/env bash
# Builds the Pulse Android debug APK and installs it to every connected device.
# Detects each device's ABI (adb getprop) and picks the matching split APK.
#
# Usage: bash android.sh [--build-only] [-s <serial>]
#   --build-only   build the APK without installing
#   -s <serial>    install only to this device (adb devices serial)
clear

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(git rev-parse --show-toplevel)"   # repo root (gradlew lives here)

BUILD_ONLY=false
TARGET_SERIAL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-only) BUILD_ONLY=true ;;
    -s|--serial)  TARGET_SERIAL="$2"; shift ;;
    -h|--help)
      echo "Usage: bash android.sh [--build-only] [-s <serial>]"
      echo "  --build-only   build the APK without installing"
      echo "  -s <serial>    install only to this device"
      exit 0 ;;
    *) echo "ERROR: unknown option: $1" >&2; exit 1 ;;
  esac
  shift
done

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb not found on PATH (install Android SDK platform-tools)" >&2
  exit 1
fi

echo "==> Building debug APK..."
# --no-configuration-cache: Chaquopy's python task can't serialize its config
# cache entry (kotlin.SynchronizedLazyImpl), which fails the build otherwise.
./gradlew :app:assembleDebug --no-configuration-cache

APK_DIR="app/build/outputs/apk/debug"
if [ ! -d "$APK_DIR" ]; then
  echo "ERROR: no output dir: $APK_DIR" >&2
  exit 1
fi

if [ "$BUILD_ONLY" = true ]; then
  echo "==> Done (build only). APKs in $APK_DIR/"
  exit 0
fi

mapfile -t DEVICES < <(adb devices | awk 'NR>1 && $2=="device" { print $1 }')

if [ -n "$TARGET_SERIAL" ]; then
  DEVICES=("$TARGET_SERIAL")
fi

if [ "${#DEVICES[@]}" -eq 0 ]; then
  echo "ERROR: no devices connected. APK built but not installed." >&2
  exit 1
fi

echo "==> Devices: ${DEVICES[*]}"

for serial in "${DEVICES[@]}"; do
  echo ""
  echo "==> [$serial] detecting ABI..."

  abi=$(adb -s "$serial" shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')
  echo "    ABI: $abi"

  # splits.abi produces one APK per ABI (arm64-v8a, x86_64); fall back to a
  # single universal APK if the project ever disables splits.
  apk=""
  if [ -n "$abi" ]; then
    apk="$APK_DIR/app-$abi-debug.apk"
    [ -f "$apk" ] || apk=""
  fi
  [ -f "$apk" ] || apk="$APK_DIR/app-debug.apk"
  [ -f "$apk" ] || apk=$(ls "$APK_DIR"/*-debug.apk 2>/dev/null | head -1 || true)

  if [ -z "$apk" ]; then
    echo "    ERROR: no APK for ABI '$abi'. Built: $(ls "$APK_DIR" 2>/dev/null | tr '\n' ' ')" >&2
    continue
  fi

  echo "    Installing $(basename "$apk")"
  adb -s "$serial" install -r "$apk"
  echo "    OK: installed on $serial"

  # run the app after install
  echo "    Launching Pulse Debug..."
  if adb -s "$serial" shell pm path com.elza.pulse.debug >/dev/null 2>&1; then
    adb -s "$serial" shell monkey -p com.elza.pulse.debug -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    echo "    OK: launched on $serial"
  else
    echo "    WARN: com.elza.pulse.debug not installed, skipping launch" >&2
  fi
done

echo ""
echo "==> Done."
