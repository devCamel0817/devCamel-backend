# PostgreSQL 백업 / 복구 가이드

현재 구성은 `docker-compose.yml` 의 `postgres` 컨테이너에 데이터를 저장합니다.

## 1) 수동 백업

서버에서 실행:

```bash
cd ~/devCamel-backend
mkdir -p backups
BACKUP_FILE="backups/backendlab-$(date +%F-%H%M%S).sql"
docker compose -f docker-compose.yml --env-file .env exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" > "$BACKUP_FILE"
ls -lh "$BACKUP_FILE"
```

> `exec -T` 를 사용해 리다이렉션이 로컬 쉘 기준으로 동작하도록 합니다.

## 2) gzip 압축 백업

```bash
cd ~/devCamel-backend
mkdir -p backups
BACKUP_FILE="backups/backendlab-$(date +%F-%H%M%S).sql.gz"
docker compose -f docker-compose.yml --env-file .env exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" | gzip > "$BACKUP_FILE"
ls -lh "$BACKUP_FILE"
```

## 3) 복구

복구 전 주의:

- 운영 DB 에 바로 덮어쓰기 전 반드시 임시 DB / 스냅샷 / 파일 백업을 먼저 확보하세요.
- 대상 DB 구조와 Flyway migration 상태를 확인하세요.

압축되지 않은 `.sql` 복구:

```bash
cd ~/devCamel-backend
cat backups/<backup-file>.sql | docker compose -f docker-compose.yml --env-file .env exec -T postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

압축된 `.sql.gz` 복구:

```bash
cd ~/devCamel-backend
gzip -dc backups/<backup-file>.sql.gz | docker compose -f docker-compose.yml --env-file .env exec -T postgres \
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

## 4) 정기 백업 권장

간단한 cron 예시:

```bash
crontab -e
```

매일 새벽 3시 백업:

```cron
0 3 * * * cd /home/ubuntu/devCamel-backend && /bin/bash scripts/backup.sh >> /home/ubuntu/devCamel-backend/logs/backup.log 2>&1
```

## 5) 보관 정책 권장

- 일별 7개
- 주별 4개
- 월별 3개
- 중요한 릴리즈 전 수동 백업 1회

## 6) 외부 보관 권장

백업 파일은 같은 VM 에만 두지 말고 아래 중 하나로 추가 보관하세요.

- Object Storage
- 개인 NAS
- 다른 서버
- 주기적 scp / rclone 업로드

## 7) 복구 후 검증

```bash
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/api/ping
```

또는 앱 로그 확인:

```bash
docker compose -f docker-compose.yml --env-file .env logs backend --tail=100
```

