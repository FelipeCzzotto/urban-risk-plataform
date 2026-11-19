#!/usr/bin/env bash
# Start/stop the real backend for the fullstack e2e. Uses a local Docker Postgres, never Supabase.
# ponytail: one backend per machine (fixed PID file + port 8080); parametrize if parallel runs are needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PID_FILE="${BACKEND_PID_FILE:-/tmp/urip-qa-backend.pid}"
LOG_FILE="${BACKEND_LOG_FILE:-/tmp/urip-qa-backend.log}"
HEALTH="${BACKEND_HEALTH_URL:-http://localhost:8080/actuator/health}"

# USE_ENV_DB=1: let the backend read DB + secrets from the repo-root .env (real Supabase).
if [ "${USE_ENV_DB:-}" != 1 ]; then
  export DB_URL="${DB_URL:-jdbc:postgresql://localhost:55433/postgres}"
  export DB_USERNAME="${DB_USERNAME:-postgres}" DB_PASSWORD="${DB_PASSWORD:-qa}"
  export INGEST_API_KEY="${INGEST_API_KEY:-qa-key}" REPORTER_HASH_PEPPER="${REPORTER_HASH_PEPPER:-qa-pepper}"
fi
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:4174,http://127.0.0.1:4174}"

up() { curl -fsS -m 2 "$HEALTH" 2>/dev/null | grep -q '"UP"'; }

case "${1:-}" in
  start)
    if up; then echo "already up"; exit 0; fi
    cd "$ROOT/backend"
    # setsid: own process group so stop also kills the JVM forked by spring-boot:run.
    setsid ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=dev </dev/null >>"$LOG_FILE" 2>&1 &
    echo $! >"$PID_FILE"
    for _ in $(seq 1 60); do
      if up; then echo "up"; exit 0; fi
      sleep 1
    done
    echo "backend not healthy after 60s, see $LOG_FILE" >&2; exit 1 ;;
  stop)
    if [ ! -f "$PID_FILE" ]; then echo "no pid file"; exit 0; fi
    PGID="$(cat "$PID_FILE")"
    kill -TERM -- "-$PGID" 2>/dev/null || true
    for _ in $(seq 1 30); do
      if ! kill -0 -- "-$PGID" 2>/dev/null; then rm -f "$PID_FILE"; echo "stopped"; exit 0; fi
      sleep 1
    done
    kill -KILL -- "-$PGID" 2>/dev/null || true
    rm -f "$PID_FILE"; echo "killed" ;;
  *) echo "usage: $0 start|stop" >&2; exit 2 ;;
esac
