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

가장 빠른 로컬 실행 방법입니다.

생성된 파일:
- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`

### 1) 컨테이너 실행

```powershell
Set-Location "C:\Users\User1\Desktop\devcamel-backend"
docker compose up --build -d
```

### 2) 상태 확인

```powershell
docker compose ps
```

정상이라면 아래 두 컨테이너가 떠야 합니다.
- `devcamel-postgres`
- `devcamel-backend-app`

### 3) 헬스 체크

```powershell
Invoke-WebRequest -Uri "http://localhost:18080/api/ping" -UseBasicParsing
```

### 4) 종료

```powershell
docker compose down
```

데이터까지 같이 지우려면:

```powershell
docker compose down -v
```

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

기본값은 `src/main/resources/application.yml`에 이미 들어 있으므로, 로컬 PostgreSQL을 기본 설정으로 띄웠다면 별도 설정 없이도 실행 가능합니다.

기준 값은 `.env.example` 참고:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/backendlab
DB_USERNAME=postgres
DB_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://devcamel.dev
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
- Hibernate Statistics 활성화 상태라 `queryCount` 비교가 가능합니다.
- DevTools가 포함되어 있어 개발 중 저장 후 재시작 흐름을 사용할 수 있습니다.
- Docker Compose 실행 시 PostgreSQL은 호스트 `5433` 포트, 백엔드는 호스트 `18080` 포트로 노출됩니다.
