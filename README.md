# devcamel-backend

Spring Boot 기반의 백엔드 실험 프로젝트입니다.
프론트에서 호출해 JPA / ORM / 동시성 / 캐시 같은 시나리오를 시각적으로 비교하는 용도로 사용합니다.

현재 MVP로 구현된 시나리오는 **N+1 vs Fetch Join vs EntityGraph** 입니다.

## 기술 스택

- Java 21
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Gradle 8.5
- Lombok
- Docker
- Docker Compose

## 현재 구현 상태

- `GET /api/ping`
- `GET /api/nplus1`
- `GET /api/nplus1/compare`
- `GET /api/nplus1/variants`

향후 확장 예정 패키지
- `scenario/locking`
- `scenario/bulk`
- `scenario/caching`
- `scenario/async`

## Docker 실행

운영 기준 기본 파일은 `docker-compose.yml` 이고, 로컬 개발 전용 오버레이는 `docker-compose.dev.yml` 입니다.

- `docker-compose.yml` 단독 실행: `prod` 기준
- `docker-compose.yml` + `docker-compose.dev.yml`: 로컬 개발 기준
- `docker-compose.override.yml` 는 자동 병합 충돌을 막기 위해 중립 상태로 유지합니다.

생성된 파일:
- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`

### 1) 로컬 개발용 컨테이너 실행

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.example up --build -d
```

운영/배포 검증처럼 `prod` 기준으로 올리려면 아래처럼 base 파일만 사용하세요.

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
docker compose -f docker-compose.yml --env-file .env.example up --build -d
```

### 2) 상태 확인

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.example ps
```

정상이라면 아래 두 컨테이너가 떠야 합니다.
- `devcamel-postgres`
- `devcamel-backend-app`

### 3) 헬스 체크

컨테이너 healthcheck 기준:

```powershell
Invoke-WebRequest -Uri "http://localhost:18080/actuator/health" -UseBasicParsing
```

기존 앱 Ping 확인:

```powershell
Invoke-WebRequest -Uri "http://localhost:18080/api/ping" -UseBasicParsing
```

### 4) 종료

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.example down
```

데이터까지 같이 지우려면:

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.example down -v
```

`POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` 값을 바꿨는데 접속이 계속 실패하면 기존 볼륨에 이전 초기화 정보가 남아 있을 수 있습니다. 이 경우 `down -v` 로 볼륨을 지운 뒤 다시 올리세요.

### 5) 포트

- 앱(호스트): `18080`
- 앱(컨테이너 내부): `8080`
- PostgreSQL(호스트): `5433`
- PostgreSQL(컨테이너 내부): `5432`

컨테이너 내부에서 앱은 아래 DB URL로 연결됩니다.

```text
jdbc:postgresql://postgres:5432/backendlab
```

## 로컬 실행

### 1) PostgreSQL만 따로 실행

Docker 기준:

```powershell
docker run -d --name backendlab-pg -e POSTGRES_DB=backendlab -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
```

이미 같은 이름의 컨테이너가 있으면 기존 컨테이너를 삭제하거나 이름을 바꿔서 실행하세요.

### 2) 환경 변수 확인

`application.yml` 은 운영 기준으로 DB 환경변수를 필수로 요구합니다.
로컬 개발 편의를 위한 fallback 값은 `application-dev.yml` 에만 들어 있습니다.

즉 아래 둘 중 하나로 실행하면 됩니다.

- `.env.example` 값을 기준으로 환경변수를 주입하고 실행
- `dev` 프로필에서 로컬 PostgreSQL(`localhost:5432`)을 기본값으로 사용해 실행

기준 값은 `.env.example` 참고:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/backendlab
DB_USERNAME=backendlab_app
DB_PASSWORD=change-me-strong-random-secret
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### 3) 애플리케이션 실행

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
.\gradlew.bat bootRun
```

또는 IntelliJ에서 `BackendLabApplication`을 실행해도 됩니다.

### 4) 헬스 체크

```text
GET http://localhost:8080/api/ping
```

예상 응답:

```json
{
  "status": "OK",
  "service": "backend-lab",
  "timestamp": 1714460000000,
  "message": "pong"
}
```

## 빌드

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
.\gradlew.bat clean build
```

테스트만 빠르게 확인하려면:

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
.\gradlew.bat test
```

## 무료 배포 추천안

앱 + PostgreSQL 을 **최대한 무료로** 운영하려면, 현재 구조에서는 **Oracle Cloud Always Free VM 1대에 `docker compose` 로 앱과 DB를 함께 올리는 방식**이 가장 현실적입니다.

### 왜 이 방식을 추천하나요?

- 현재 저장소에 `Dockerfile`, `docker-compose.yml` 이 이미 준비되어 있음
- `Spring Boot + PostgreSQL + Flyway` 구성을 별도 리팩터링 없이 그대로 가져갈 수 있음
- 앱과 DB를 같은 VM 에 두면 무료 외부 DB 티어의 연결 수 제한/휴면 정책 영향을 덜 받음
- 저트래픽 MVP / 포트폴리오 / 개인 프로젝트 기준으로 비용을 `0원`에 가깝게 유지 가능

### 권장 배포 구성

- VM 1대
- Docker Engine + Docker Compose
- `backend` 컨테이너
- `postgres` 컨테이너
- 리버스 프록시(Caddy 또는 Nginx)
- 도메인 연결 + HTTPS

### 운영 포트 권장

- 외부 공개: `80`, `443`
- 외부 미공개: `5432`
- 앱 컨테이너: 내부 `8080`

### 배포 절차 요약

1. Oracle Cloud VM 생성
2. Docker / Compose 설치
3. 저장소 clone
4. `.env` 작성 (`POSTGRES_*`, `CORS_ALLOWED_ORIGINS`, 필요 시 `SPRING_PROFILES_ACTIVE=prod`)
5. `docker compose -f docker-compose.yml up --build -d`
6. `/actuator/health`, `/api/ping` 검증
7. reverse proxy + HTTPS 연결
8. `pg_dump` 기준 정기 백업 추가

### 비용 감각

- 인프라: 무료 티어 범위 내면 `0원`
- 도메인: 별도(보통 연 단위 과금)
- 숨은 비용: 백업 저장공간, 무료 티어 정책 변경, 리전 수급 문제

### 주의사항

- `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` 변경 후 기존 볼륨을 재사용하면 인증 오류가 날 수 있습니다.
- 이 경우 `docker compose down -v` 후 다시 기동하세요.
- 운영에서는 `CORS_ALLOWED_ORIGINS` 에 `localhost` 를 남기지 마세요.
- 운영에서는 DB 포트(`5432`)를 외부에 열지 마세요.

### 배포 문서

- Oracle VM 실배포 순서: `docs/oracle-vm-deploy.md`
- 운영 백업 / 복구: `docs/backup-restore.md`
- 배포 직전 최종 점검: `docs/deploy-checklist.md`
- 운영용 예시 환경 파일: `.env.prod.example`
- Caddy 리버스 프록시 예시: `deploy/Caddyfile`

## 데이터베이스 / 마이그레이션

Flyway가 애플리케이션 시작 시 자동 실행됩니다.

현재 N+1 시나리오 기준 데이터셋:
- 최대 author 수: `200`
- author 당 book 수: `20`

즉 `authorCount=30`으로 요청하면 최대 `30 authors / 600 books` 기준으로 비교가 수행됩니다.

## 주요 API

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/ping` | 서버 상태 확인 |
| GET | `/api/nplus1` | 단일 variant 실행 |
| GET | `/api/nplus1/compare` | 3개 variant 비교 실행 |
| GET | `/api/nplus1/variants` | 프론트 표시용 variant 메타 조회 |

## N+1 시나리오

### 지원 variant

- `n-plus-one`
- `fetch-join`
- `entity-graph`

### 요청 파라미터

| 이름 | 타입 | 설명 |
|---|---|---|
| `variant` | string | `n-plus-one`, `fetch-join`, `entity-graph` 중 하나 |
| `authorCount` | integer | 비교에 사용할 author 수. `1 ~ 200` |

## 호출 예시

### 1) 단일 variant 실행

```text
GET /api/nplus1?variant=n-plus-one&authorCount=10
GET /api/nplus1?variant=fetch-join&authorCount=30
GET /api/nplus1?variant=entity-graph&authorCount=100
```

예시:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/nplus1?variant=fetch-join&authorCount=30" -UseBasicParsing
```

### 2) 전체 비교 실행

```text
GET /api/nplus1/compare?authorCount=30
```

예시:

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/nplus1/compare?authorCount=30" -UseBasicParsing
```

### 3) variant 메타 조회

```text
GET /api/nplus1/variants
```

## 응답 포맷 요약

단일 실행 응답은 아래 필드를 포함합니다.

- `scenario`
- `variant`
- `title`
- `subtitle`
- `request`
- `elapsedMs`
- `queryCount`
- `rows`
- `metrics`
- `dataset`
- `comparisonHints`
- `variants`
- `extra`

비교 응답은 아래 구조입니다.

- `scenario`
- `title`
- `subtitle`
- `request`
- `variants`
- `results`
- `summary.bestByElapsedMs`
- `summary.bestByQueryCount`

## 예시 응답

```json
{
  "scenario": "nplus1",
  "title": "N+1 vs Fetch Join vs EntityGraph",
  "subtitle": "같은 데이터셋에서 쿼리 수와 응답 시간을 한 번에 비교합니다.",
  "request": {
    "requestedAuthorCount": 30,
    "appliedAuthorCount": 30,
    "booksPerAuthor": 20,
    "maxAvailableAuthors": 200
  },
  "summary": {
    "bestByElapsedMs": {
      "value": "entity-graph",
      "label": "EntityGraph",
      "metricKey": "elapsedMs",
      "metricValue": 9,
      "reason": "가장 빠른 응답 시간을 기록한 variant"
    },
    "bestByQueryCount": {
      "value": "fetch-join",
      "label": "Fetch Join",
      "metricKey": "queryCount",
      "metricValue": 1,
      "reason": "가장 적은 SQL 쿼리를 실행한 variant"
    }
  }
}
```

## 참고

- `spring.jpa.open-in-view=false`
- `dev`, `prod` 프로필 모두 Hibernate Statistics 를 켜 두어 `queryCount` 비교 응답이 0으로 깨지지 않도록 했습니다.
- DevTools가 포함되어 있어 개발 중 저장 후 재시작 흐름을 사용할 수 있습니다.
- 로컬 개발(`docker-compose.dev.yml`) 실행 시 PostgreSQL은 호스트 `5433`, 백엔드는 호스트 `18080` 포트로 노출됩니다.
- 컨테이너 헬스체크는 `/actuator/health`, 외부 smoke test 는 `/api/ping` 기준입니다.
