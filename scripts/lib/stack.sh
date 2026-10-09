# Shared by the scripts that need a running platform: the end-to-end suites and the local stack.
#
#   . "$ROOT/scripts/lib/stack.sh"
#
# Everything runs on the compose files in infrastructure/docker, so a suite needs only Docker,
# Node and (for the phone harness) a JDK, on Linux, macOS or Windows (Git Bash) alike. The suites
# get their own compose project (compose.e2e.yml): own containers, volumes and ports, wiped
# before each run, so the development stack is never reset under someone's feet.
#
# Set STACK_NO_BUILD=1 to use an already built `daliini-backend:local` instead of building it.

# Git Bash would otherwise rewrite `/app/...` arguments into Windows paths.
export MSYS_NO_PATHCONV=1

ROOT="${ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
# On Windows (Git Bash), path translation is off (above), so a `/d/…` root reached Docker as is
# and Docker read it as `D:\d\…` — "the system cannot find the path specified" before any test
# ran. `cygpath -m` gives `D:/…`, which bash and Docker both read the same way. On a Unix
# machine there is no cygpath and nothing to translate.
if command -v cygpath >/dev/null 2>&1; then ROOT="$(cygpath -m "$ROOT")"; fi
COMPOSE_FILE_DEV="$ROOT/infrastructure/docker/compose.yml"
COMPOSE_FILE_E2E="$ROOT/infrastructure/docker/compose.e2e.yml"

E2E_PROJECT="${E2E_PROJECT:-daliini-e2e}"
export E2E_API_PORT="${E2E_API_PORT:-8021}"
export E2E_S3_PORT="${E2E_S3_PORT:-9021}"
export E2E_ADMIN_PORT="${E2E_ADMIN_PORT:-3021}"
export E2E_WEB_PORT="${E2E_WEB_PORT:-3022}"

http_code() { # url
  curl -s -o /dev/null -w '%{http_code}' --max-time 5 --noproxy '*' "$1"
}

wait_for() { # url, expected code, seconds
  local _
  for _ in $(seq 1 "$3"); do
    [ "$(http_code "$1")" = "$2" ] && return 0
    sleep 1
  done
  return 1
}

# Stop whatever listens on a port. `next start` serves from a child of the process that launched
# it, so killing the launcher would leave the port bound; this finds the listener itself.
# Whichever tool this machine has that can see the listener: `lsof` cannot always (in a
# container it may see nothing), `fuser` is Linux's, `taskkill` is Windows'.
stop_port() { # port
  local port="$1" pids="" _
  if command -v lsof >/dev/null 2>&1; then
    pids="$(lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null)"
  fi
  if [ -n "$pids" ]; then
    kill -9 $pids 2>/dev/null
  elif command -v fuser >/dev/null 2>&1 && fuser "$port/tcp" >/dev/null 2>&1; then
    fuser -k "$port/tcp" >/dev/null 2>&1
  elif command -v taskkill >/dev/null 2>&1; then
    # /T takes the whole tree. Single slashes, because MSYS_NO_PATHCONV passes them as they are.
    for pid in $(netstat -ano 2>/dev/null | grep ":$port " | grep LISTENING | awk '{print $5}' | sort -u); do
      taskkill /PID "$pid" /F /T >/dev/null 2>&1
    done
  fi
  # A killed server takes a moment to let go of its socket.
  for _ in $(seq 1 20); do
    port_in_use "$port" || return 0
    sleep 0.5
  done
  return 0
}

port_in_use() { # port
  [ "$(http_code "http://127.0.0.1:$1/")" != "000" ]
}

# ---------------------------------------------------------------- the suites' stack

e2e_compose() {
  docker compose -p "$E2E_PROJECT" -f "$COMPOSE_FILE_DEV" -f "$COMPOSE_FILE_E2E" "$@"
}

# A fresh stack: every volume of the previous run removed, migrations applied (which seed the
# launch baseline), the API answering.
e2e_up() {
  local build=(--build)
  [ "${STACK_NO_BUILD:-0}" = "1" ] && build=(--no-build)
  e2e_compose down -v --remove-orphans >/dev/null 2>&1
  e2e_compose up -d "${build[@]}" >/dev/null || { echo "FAIL: docker compose up"; return 1; }
  if ! wait_for "http://127.0.0.1:$E2E_API_PORT/health/ready/" 200 180; then
    echo "FAIL: the API did not become ready"
    e2e_compose logs --tail 30 api
    return 1
  fi
  echo "stack ready: API http://127.0.0.1:$E2E_API_PORT, media http://127.0.0.1:$E2E_S3_PORT"
}

e2e_down() {
  e2e_compose down -v --remove-orphans >/dev/null 2>&1
}

e2e_manage() { # manage.py arguments...
  e2e_compose exec -T api uv run python manage.py "$@"
}

# The API container's id, for the suites that run `docker exec` into it themselves.
e2e_api_container() {
  e2e_compose ps -q api
}

# ---------------------------------------------------------------- the console

# Build the console for production and serve it on a port, in the background. `next dev` is
# deliberately not used: the production build is what ships, and its server-only boundaries,
# bundling and instrumentation are what is worth testing.
admin_serve() { # api origin, port, log file
  local api="$1" port="$2" log="$3"
  stop_port "$port"
  if port_in_use "$port"; then
    echo "FAIL: port $port is still in use by another process"
    return 1
  fi
  (
    cd "$ROOT/apps/admin" || exit 1
    export ADMIN_API_ORIGIN="$api"
    export ADMIN_PUBLIC_ORIGIN="http://localhost:$port"
    ./node_modules/.bin/next build >"$log.build" 2>&1 || { echo "FAIL: next build, see $log.build"; exit 1; }
    ./node_modules/.bin/next start -p "$port" >"$log" 2>&1 &
  ) || return 1
  if ! wait_for "http://localhost:$port/login" 200 60; then
    echo "FAIL: the console did not start, see $log"
    return 1
  fi
  echo "console ready: http://localhost:$port"
}
