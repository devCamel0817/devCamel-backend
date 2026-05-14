# Content Batch 가이드

Spring Boot 기반으로 경제 뉴스 RSS를 수집하고, Gemini API로 유튜브 스크립트를 생성한 뒤, Telegram API로 성공/실패 알림을 보내는 배치 기능 정리 문서다.

## 1. 목적

이 기능의 목적은 다음과 같다.

- 경제/재테크 관련 최신 헤드라인 수집
- 수집된 헤드라인을 바탕으로 Gemini에서 유튜브 스크립트 생성
- 생성 결과를 DB에 저장
- 배치 성공/실패를 Telegram으로 즉시 알림
- 실제 운영에서는 매일 저녁 6시에 자동 실행

## 2. 전체 흐름

```text
ContentScheduler
  -> ContentService.generateTodayScript()
    -> NaverNewsRssClient.fetchEconomyHeadlines()
    -> GeminiClient.generateYoutubeScript(headlines)
    -> ContentScriptRepository.save(...)
  -> TelegramClient.sendMessage(...)
```

성공 흐름:

1. RSS에서 경제 뉴스 제목 수집
2. Gemini에 프롬프트 전달
3. 스크립트 생성
4. DB 저장
5. Telegram 성공 알림 전송

실패 흐름:

1. RSS/Gemini/DB 단계에서 예외 발생
2. `ContentScheduler`가 예외를 잡음
3. 상태코드와 응답 본문까지 Telegram 실패 알림 전송

## 3. 주요 구현 파일

### 3.1 서비스

- `src/main/java/dev/camel/backendlab/scenario/content/service/ContentService.java`

역할:

- 오늘 날짜 기준 중복 생성 방지
- RSS 수집
- Gemini 스크립트 생성
- DB 저장
- 전체 조회 제공

핵심 로직:

- `existsByTargetDate(today)`로 당일 생성 여부 확인
- 이미 있으면 기존 데이터 반환
- 없으면 새로 생성 후 저장

### 3.2 스케줄러

- `src/main/java/dev/camel/backendlab/scenario/content/scheduler/ContentScheduler.java`

역할:

- cron 기반 자동 실행
- 성공 시 Telegram 성공 메시지 전송
- 실패 시 상태코드/응답본문 포함해서 Telegram 실패 메시지 전송

실행 시간:

- `content.scheduler.cron = "0 0 18 * * *"`
- 매일 18:00 실행

### 3.3 RSS 클라이언트

- `src/main/java/dev/camel/backendlab/scenario/content/client/NaverNewsRssClient.java`

역할:

- RSS 피드 목록을 순회하며 헤드라인 수집
- Rome 라이브러리로 XML 파싱
- `max-items` 개수만큼 제목 수집

현재 RSS 소스:

- `https://www.yna.co.kr/rss/economy.xml`
- `https://www.hankyung.com/feed/economy`
- `https://news.kbs.co.kr/rss/rss.do?cat=economy`

특징:

- HTML 크롤링이 아니라 RSS 사용
- 개별 피드 실패 시 전체 중단하지 않고 다음 URL 계속 시도

### 3.4 Gemini 클라이언트

- `src/main/java/dev/camel/backendlab/scenario/content/client/GeminiClient.java`
- `src/main/java/dev/camel/backendlab/scenario/content/GeminiProperties.java`

역할:

- 환경변수로 API 키/모델/기본 URL 주입
- 헤드라인 리스트를 기반으로 프롬프트 생성
- Gemini REST API 호출
- 생성된 텍스트 반환

주요 포인트:

- `@PostConstruct`에서 `GEMINI_API_KEY` 필수 검증
- `gemini-2.0-flash` 모델 사용
- REST 호출 경로:
  - `/v1beta/models/{model}:generateContent?key={key}`

프롬프트 방향:

- 경제·재테크 유튜브 채널 작가 역할 부여
- 시청자 관심이 높은 이슈 선정 요청
- 제목 / 후킹 / 본론 / CTA 구조로 생성

### 3.5 Telegram 클라이언트

- `src/main/java/dev/camel/backendlab/scenario/content/client/TelegramClient.java`
- `src/main/java/dev/camel/backendlab/scenario/content/TelegramProperties.java`

역할:

- Telegram Bot API로 메시지 전송
- 메시지가 길면 분할 전송
- 활성화 여부와 bot token/chat id 검증

주요 포인트:

- `TELEGRAM_ENABLED=false`면 아무 것도 보내지 않음
- 활성화 상태에서는 `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` 필수
- 긴 스크립트/오류 본문은 chunk 분할 전송

### 3.6 엔티티 / 저장소 / DTO

- `ContentScript.java`
- `ContentScriptRepository.java`
- `ContentScriptResponse.java`
- `V6__content_script.sql`

역할:

- 스크립트 저장용 JPA 엔티티
- 날짜별 존재 여부 조회
- 응답 DTO 변환
- Flyway 마이그레이션으로 테이블 생성

## 4. 설정 구조

### 4.1 공통 설정

- `src/main/resources/application.yml`

주요 항목:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

gemini:
  api-key: ${GEMINI_API_KEY}
  model: gemini-2.0-flash
  base-url: https://generativelanguage.googleapis.com

telegram:
  enabled: ${TELEGRAM_ENABLED:false}
  bot-token: ${TELEGRAM_BOT_TOKEN:}
  chat-id: ${TELEGRAM_CHAT_ID:}
  base-url: https://api.telegram.org

content:
  scheduler:
    cron: "0 0 18 * * *"
  news:
    rss-urls:
      - https://www.yna.co.kr/rss/economy.xml
      - https://www.hankyung.com/feed/economy
      - https://news.kbs.co.kr/rss/rss.do?cat=economy
    max-items: 10
```

### 4.2 개발 설정

- `src/main/resources/application-dev.yml`

특징:

- 로컬 DB 기본값 fallback 제공
- Gemini/Telegram env가 비어 있어도 dev에서는 주입 가능
- 수동 테스트 시 사용

### 4.3 운영 설정

- `src/main/resources/application-prod.yml`

특징:

- 운영은 환경변수 중심
- Actuator 노출 범위 최소화

## 5. Docker / 배포 연동 포인트

### 5.1 Docker Compose

- `docker-compose.yml`

backend 환경변수로 반드시 전달해야 하는 값:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
  DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
  DB_USERNAME: ${POSTGRES_USER}
  DB_PASSWORD: ${POSTGRES_PASSWORD}
  CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS is required}
  GEMINI_API_KEY: ${GEMINI_API_KEY:?GEMINI_API_KEY is required}
  TELEGRAM_ENABLED: ${TELEGRAM_ENABLED:-false}
  TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN:-}
  TELEGRAM_CHAT_ID: ${TELEGRAM_CHAT_ID:-}
```

핵심 포인트:

- `.env`에 값이 있어도 docker compose `backend.environment`에 명시하지 않으면 컨테이너 프로세스에 안 들어감
- 이번 작업에서 `GEMINI_API_KEY`, `TELEGRAM_*` 전달 누락 문제를 수정함

### 5.2 운영 `.env` 예시

```dotenv
POSTGRES_DB=backendlab
POSTGRES_USER=backendlab_app
POSTGRES_PASSWORD=실제_DB비밀번호

SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://dev-camel.vercel.app

DB_URL=jdbc:postgresql://postgres:5432/backendlab
DB_USERNAME=backendlab_app
DB_PASSWORD=실제_DB비밀번호

GEMINI_API_KEY=실제_GEMINI_API_KEY
TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=실제_TELEGRAM_BOT_TOKEN
TELEGRAM_CHAT_ID=841125471
```

주의:

- `DB_URL`은 compose 내부 통신 기준으로 `postgres`를 사용해야 함
- `POSTGRES_PASSWORD`와 `DB_PASSWORD`는 동일하게 맞추는 것이 안전함
- 기존 Postgres 볼륨이 있으면 `.env` 비밀번호만 바꿔도 실제 DB 계정 비밀번호는 안 바뀜

## 6. 테스트 전략

### 6.1 단위 테스트

#### `ContentServiceTest`

- RSS/Gemini/Repository 모두 mock
- 외부 API 호출 없이 순수 서비스 로직 검증
- 검증 항목:
  - 정상 생성
  - 당일 중복 생성 시 기존 반환
  - 전체 조회

#### `ContentSchedulerTest`

- `ContentService`, `TelegramClient` mock
- 배치 성공 시 Telegram 메시지 전송 검증
- 배치 실패 시 429 응답 본문까지 알림 메시지에 포함되는지 검증

### 6.2 수동 통합 테스트

#### `ContentManualIntegrationTest`

- 실제 RSS 수집
- 실제 Gemini 호출
- 실제 DB 저장
- `RUN_MANUAL_CONTENT_IT=true` 일 때만 실행

#### `ContentSchedulerManualIntegrationTest`

- 실제 스케줄러 실행
- 성공/실패 Telegram 알림 확인용
- Gemini 429가 나더라도 실패 알림 확인용으로 동작

### 6.3 수동 실행 스크립트

- `scripts/run-content-manual-test.ps1`

역할:

- `.env` 로드
- `RUN_MANUAL_CONTENT_IT=true` 세팅
- 수동 통합 테스트 실행

예시:

```powershell
.\scripts\run-content-manual-test.ps1
.\scripts\run-content-manual-test.ps1 -Scheduler
```

## 7. Telegram 알림 동작

### 성공 알림

포함 정보:

- 날짜
- 헤드라인 요약
- 생성된 스크립트 전문

### 실패 알림

포함 정보:

- 날짜
- 예외 메시지
- HTTP 상태코드
- 외부 API 응답 본문(responseBody)

예시:

```text
[콘텐츠 배치 실패]
날짜: 2026-05-07
오류:
429 TOO_MANY_REQUESTS
responseBody:
{"error":{"message":"quota exceeded"}}
```

## 8. 주요 트러블슈팅

### 8.1 Gemini 429 TooManyRequests

증상:

- 수동 통합 테스트 또는 실제 배치 시 Gemini가 429 반환

대응:

- 실패 알림에 상태코드와 응답 본문 포함
- Telegram에서 바로 실패 원인 확인 가능
- 장기적으로는 backoff/retry 추가 고려

### 8.2 Telegram chat_id 조회

절차:

1. BotFather로 bot 생성
2. 봇과 대화 시작, `/start` 또는 메시지 전송
3. `getUpdates` 호출
4. `message.chat.id` 값을 `TELEGRAM_CHAT_ID`로 사용

### 8.3 Docker 배포 후 backend healthcheck 실패

원인 후보:

- 컨테이너에 `GEMINI_API_KEY`, `TELEGRAM_*` env 미전달
- Postgres 인증 실패
- CORS 설정 오류

실제 문제 사례:

- 서버 `.env`를 바꿨지만 기존 postgres 볼륨이 남아 있어 `backendlab_app` 비밀번호 불일치 발생
- 로그:

```text
FATAL: password authentication failed for user "backendlab_app"
```

해결:

- 데이터 보존 불필요 시

```bash
docker compose -f docker-compose.yml --env-file .env down -v
docker compose -f docker-compose.yml --env-file .env up --build -d
```

- 데이터 보존 필요 시 기존 DB 내부 계정 비밀번호와 `.env` 일치시켜야 함

### 8.4 CORS 403

증상:

- 외부 브라우저 요청에서 `403 Forbidden`

원인:

- `CORS_ALLOWED_ORIGINS` 값이 실제 프론트 origin과 다름

예:

```dotenv
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://dev-camel.vercel.app
```

## 9. 운영 체크리스트

배포 전:

- `GEMINI_API_KEY` 준비
- `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` 준비
- 운영 `.env`에 DB/CORS/Gemini/Telegram 값 반영
- docker compose backend env 전달 여부 확인

배포 후:

```bash
docker compose -f docker-compose.yml --env-file .env ps
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/api/ping
docker compose -f docker-compose.yml --env-file .env logs backend --tail=200
```

확인 포인트:

- backend / postgres 둘 다 Up
- Actuator health = UP
- Flyway migrate 성공
- Telegram 성공 또는 실패 알림 수신
- 저녁 6시 스케줄 동작 확인

## 10. 정리

이번 content 배치는 단순한 스케줄러가 아니라 아래 요소를 한 번에 다루는 자동화 파이프라인이다.

- RSS 기반 뉴스 수집
- Gemini API 기반 스크립트 생성
- JPA/PostgreSQL 저장
- Telegram 기반 운영 알림
- 단위 테스트 / 수동 통합 테스트 분리
- Docker / GitHub Actions 배포 고려

결과적으로 이 기능은 “뉴스 수집 → AI 스크립트 생성 → 운영 알림” 흐름을 Spring Boot 안에 일관되게 넣은 예시다.

