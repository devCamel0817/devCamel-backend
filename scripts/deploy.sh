#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRANCH="${DEPLOY_BRANCH:-main}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
ENV_FILE="${ENV_FILE:-.env}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:18080/actuator/health}"
EXTERNAL_HEALTHCHECK_URL="${EXTERNAL_HEALTHCHECK_URL:-}"
LOCK_FILE="${LOCK_FILE:-/tmp/devcamel-backend-deploy.lock}"
DEPLOY_SKIP_PULL="${DEPLOY_SKIP_PULL:-false}"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "Another deployment is already running." >&2
  exit 1
fi

cd "$PROJECT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Create it on the server before deploying." >&2
  exit 1
fi

if [[ "$DEPLOY_SKIP_PULL" != "true" ]]; then
  git fetch origin "$BRANCH"
  git checkout "$BRANCH"
  git pull --ff-only origin "$BRANCH"
fi

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" config >/dev/null
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up --build -d

echo "Waiting for internal healthcheck: $HEALTHCHECK_URL"
for attempt in {1..30}; do
  if curl -fsS "$HEALTHCHECK_URL" >/dev/null; then
    echo "Internal healthcheck passed."
    break
  fi

  if [[ "$attempt" == "30" ]]; then
    echo "Internal healthcheck failed after $attempt attempts." >&2
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps >&2
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs backend --tail=120 >&2
    exit 1
  fi

  sleep 5
done

if [[ -n "$EXTERNAL_HEALTHCHECK_URL" ]]; then
  echo "Waiting for external healthcheck: $EXTERNAL_HEALTHCHECK_URL"
  for attempt in {1..12}; do
    if curl -fsS "$EXTERNAL_HEALTHCHECK_URL" >/dev/null; then
      echo "External healthcheck passed."
      break
    fi

    if [[ "$attempt" == "12" ]]; then
      echo "External healthcheck failed after $attempt attempts." >&2
      exit 1
    fi

    sleep 5
  done
fi

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps

