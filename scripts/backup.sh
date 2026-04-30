#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

if [[ ! -f .env ]]; then
  echo ".env file not found in $PROJECT_DIR" >&2
  exit 1
fi

set -a
source .env
set +a

mkdir -p backups logs
BACKUP_FILE="backups/backendlab-$(date +%F-%H%M%S).sql.gz"

docker compose -f docker-compose.yml --env-file .env exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" | gzip > "$BACKUP_FILE"

find backups -type f -name '*.sql.gz' -mtime +14 -delete

echo "Backup completed: $BACKUP_FILE"

