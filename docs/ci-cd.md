# CI/CD 가이드

이 저장소는 GitHub Actions로 CI와 Oracle Cloud VM 수동 배포 CD를 구성합니다.

## 1. 구성 요약

- CI: `push` / `pull_request` 시 Gradle build와 Docker image build 검증
- CD: GitHub Actions `Deploy to Oracle VM` workflow를 수동 실행
- 배포 방식: GitHub Actions runner가 Oracle VM에 SSH 접속 후 서버에서 `git pull` 및 `docker compose up --build -d` 실행
- 운영 `.env`: GitHub에 저장하지 않고 Oracle VM의 `/home/ubuntu/devcamel-backend/.env`에만 보관

## 2. 추가된 파일

- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`
- `scripts/deploy.sh`

## 3. CI 동작

CI는 아래 상황에서 자동 실행됩니다.

- `main` 브랜치 push
- `main` 대상 pull request

실행 내용:

1. JDK 21 설치
2. Gradle cache 사용
3. `./gradlew clean build --no-daemon`
4. `docker build -t devcamel-backend:ci .`

## 4. CD 사전 조건

Oracle VM에 아래가 준비되어 있어야 합니다.

- SSH 접속 가능
- Git 설치
- Docker / Docker Compose 설치
- 저장소 clone 완료
- 서버에 실제 `.env` 작성 완료
- `scripts/deploy.sh`가 포함된 최신 코드가 한 번 이상 pull 되어 있음
- Caddy 및 방화벽 설정 완료

서버 경로 기본값:

```text
/home/ubuntu/devcamel-backend
```

운영 `.env` 예시:

```dotenv
POSTGRES_DB=backendlab
POSTGRES_USER=backendlab_app
POSTGRES_PASSWORD=<secret>
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://dev-camel.vercel.app
DB_URL=jdbc:postgresql://postgres:5432/backendlab
DB_USERNAME=backendlab_app
DB_PASSWORD=<secret>
```

## 5. GitHub Secrets

GitHub 저장소에서 아래로 이동합니다.

```text
Settings > Secrets and variables > Actions > New repository secret
```

필수 Secrets:

| Name | Example | 설명 |
|---|---|---|
| `ORACLE_VM_HOST` | `168.110.106.188` | Oracle VM Public IP |
| `ORACLE_VM_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----...` | VM 접속용 private key 전체 내용 |

선택 Secrets:

| Name | Default | 설명 |
|---|---|---|
| `ORACLE_VM_USER` | `ubuntu` | SSH 사용자 |
| `ORACLE_VM_SSH_PORT` | `22` | SSH 포트 |
| `ORACLE_VM_DEPLOY_PATH` | `/home/ubuntu/devcamel-backend` | 서버 저장소 경로 |
| `ORACLE_VM_HEALTHCHECK_URL` | `http://127.0.0.1:18080/actuator/health` | 서버 내부 헬스체크 URL |
| `ORACLE_VM_EXTERNAL_HEALTHCHECK_URL` | empty | 외부 HTTPS 헬스체크 URL |

현재 외부 URL을 쓰려면 선택 Secret으로 아래를 넣을 수 있습니다.

```text
ORACLE_VM_EXTERNAL_HEALTHCHECK_URL=https://168.110.106.188.sslip.io/actuator/health
```

## 6. SSH private key 등록 방법

로컬 Windows PowerShell에서 private key 내용을 확인합니다.

```powershell
Get-Content "$HOME\.ssh\id_ed25519" -Raw
```

출력 전체를 `ORACLE_VM_SSH_KEY` Secret에 넣습니다.

주의:

- public key(`.pub`)가 아니라 private key입니다.
- 외부에 노출하면 안 됩니다.
- 가능하면 배포 전용 SSH key를 따로 만드는 것을 권장합니다.

## 7. CD 실행 방법

GitHub 저장소에서:

```text
Actions > Deploy to Oracle VM > Run workflow
```

입력값:

```text
ref: main
```

실행 순서:

1. SSH key 준비
2. VM host key 등록
3. VM 접속
4. `git fetch`
5. `git checkout main`
6. `git pull --ff-only origin main`
7. `scripts/deploy.sh` 실행
8. Docker Compose build/up
9. 내부 healthcheck
10. 선택 외부 healthcheck

## 8. 서버에서 수동 배포

GitHub Actions를 쓰지 않고 서버에서 직접 배포하려면:

```bash
cd ~/devcamel-backend
git pull origin main
bash scripts/deploy.sh
```

또는 기존 명령으로:

```bash
docker compose -f docker-compose.yml --env-file .env up --build -d
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/api/ping
```

## 9. 안전장치

### 동시 배포 방지

GitHub Actions `concurrency`로 같은 운영 VM에 동시에 배포되지 않게 했습니다.

```yaml
concurrency:
  group: oracle-vm-production
  cancel-in-progress: false
```

서버 스크립트에서도 `flock`으로 배포 락을 잡습니다.

### DB 삭제 방지

CD 스크립트는 `down -v`를 사용하지 않습니다.

아래 명령은 초기화가 필요할 때만 수동으로 실행합니다.

```bash
docker compose -f docker-compose.yml --env-file .env down -v
```

### Healthcheck 실패 시 실패 처리

내부 healthcheck가 실패하면 CD workflow도 실패합니다.

기본 URL:

```text
http://127.0.0.1:18080/actuator/health
```

## 10. 배포 후 확인

서버에서:

```bash
cd ~/devcamel-backend
docker compose -f docker-compose.yml --env-file .env ps
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/api/ping
```

외부에서:

```powershell
Invoke-WebRequest -Uri "https://168.110.106.188.sslip.io/api/ping" -UseBasicParsing
```

## 11. 자주 나는 문제

### SSH 접속 실패

확인:

- `ORACLE_VM_HOST`가 Public IP인지
- `ORACLE_VM_USER`가 `ubuntu`인지
- `ORACLE_VM_SSH_KEY`가 private key인지
- 서버의 `~/.ssh/authorized_keys`에 대응 public key가 있는지
- Oracle Security List에 TCP 22가 열려 있는지

### 배포 중 `.env` 없음

서버에 `.env`를 직접 만들어야 합니다.

```bash
cd ~/devcamel-backend
cp .env.prod.example .env
nano .env
```

### healthcheck 실패

로그 확인:

```bash
docker compose -f docker-compose.yml --env-file .env logs backend --tail=200
```

### 외부 URL 실패

확인:

- Caddy running
- Oracle Security List TCP 80/443
- 서버 iptables TCP 80/443 ACCEPT
- Caddy 인증서 발급 성공 여부

## 12. 현재 운영 프론트/백엔드 값

Frontend:

```text
https://dev-camel.vercel.app
```

Backend:

```text
https://168.110.106.188.sslip.io
```

Vercel env:

```text
VITE_API_BASE_URL=https://168.110.106.188.sslip.io
```

