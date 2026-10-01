#!/usr/bin/env bash
# Make this machine reachable from a phone running a `local` build.
#
#   scripts/android-link.sh          # open the tunnels and prove them from the device
#   scripts/android-link.sh --check  # only report, change nothing
#
# A `local` build asks for http://localhost:8000/ — the phone's own loopback — because no
# machine address may be committed anywhere (docs/runbooks/android-local-networking.md). What
# makes that address mean *this* machine is `adb reverse`, and those rules live in the adb
# connection: they are gone after the cable is pulled, the phone sleeps off Wi-Fi debugging, or
# the adb server restarts. The app then reaches nothing and looks broken while every service on
# this machine is running perfectly. That is what this script is for.
#
# Three ports, because the app talks to three things:
#   8000  Django, the API and the WebSocket          (scripts/local-stack.sh)
#   9000  MinIO: facility photographs and map tiles  (scripts/local-map.sh)
#   8002  Valhalla, for routes                       (scripts/valhalla.sh)
#
# It installs nothing, launches nothing and reads nothing from the phone but the answers to
# these three requests.
set -uo pipefail

PORTS=(8000 9000 8002)
CHECK_ONLY=0
[ "${1:-}" = "--check" ] && CHECK_ONLY=1

command -v adb >/dev/null 2>&1 || { echo "FAIL: adb is not on PATH"; exit 1; }

# One physical phone can appear twice over Wi-Fi debugging, once per mDNS registration, and
# every adb command then fails with "more than one device". A transport id names one of them,
# and either entry reaches the same phone.
mapfile -t TRANSPORTS < <(adb devices -l 2>/dev/null | grep -oE 'transport_id:[0-9]+' | cut -d: -f2)
if [ "${#TRANSPORTS[@]}" -eq 0 ]; then
  echo "FAIL: no device. Connect the phone (USB debugging, or Wi-Fi debugging already paired)."
  exit 1
fi
T="${TRANSPORTS[0]}"
MODEL="$(adb -t "$T" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
echo "device: ${MODEL:-unknown} (transport $T, ${#TRANSPORTS[@]} entr$([ "${#TRANSPORTS[@]}" -eq 1 ] && echo y || echo ies))"

if [ "$CHECK_ONLY" -eq 0 ]; then
  for port in "${PORTS[@]}"; do
    adb -t "$T" reverse "tcp:$port" "tcp:$port" >/dev/null 2>&1 \
      || { echo "FAIL: could not open tcp:$port"; exit 1; }
  done
fi

# The only answer that counts: what the phone itself gets. A service up on this machine proves
# nothing about the tunnel.
status=0
probe() {
  local port="$1" path="$2" name="$3"
  local code
  code="$(adb -t "$T" shell "curl -s -o /dev/null -m 5 -w '%{http_code}' http://localhost:$port$path" 2>/dev/null | tr -d '\r')"
  if [ "$code" = "200" ]; then
    printf '  %-9s tcp:%-5s 200\n' "$name" "$port"
  else
    printf '  %-9s tcp:%-5s %s  <- the phone cannot reach it\n' "$name" "$port" "${code:-no answer}"
    status=1
  fi
}
probe 8000 /health/live/ "api"
probe 9000 /minio/health/live "media"
probe 8002 /status "routing"

if [ "$status" -ne 0 ]; then
  echo
  echo "Whatever answered nothing is not running on this machine, or is not published on that"
  echo "port. scripts/local-stack.sh for 8000 and 9000, scripts/valhalla.sh up for 8002."
  exit 1
fi

echo
echo "The phone reaches this machine. Re-run this after it disconnects."
