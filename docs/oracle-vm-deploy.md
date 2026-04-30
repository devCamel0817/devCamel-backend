# Oracle Cloud VM 배포 가이드

이 문서는 `Spring Boot + PostgreSQL + Docker Compose` 구조의 현재 저장소를 Oracle Cloud Always Free VM 1대에 배포하는 기준입니다.

## 권장 구성

- VM: Oracle Cloud Compute Instance 1대
- OS: Ubuntu 24.04 LTS 또는 Oracle Linux 9 계열
- 앱: `docker-compose.yml` 의 `backend`
- DB: `docker-compose.yml` 의 `postgres`
- 프록시: Caddy 또는 Nginx
- 공개 포트: `80`, `443`
- 비공개 포트: `5432`

## 사전 준비

- Oracle Cloud 계정
- 고정 Public IP
- 연결할 도메인 (`api.example.com` 같은 서브도메인 권장)
- SSH 키
- GitHub 접근 권한

## VM 생성 권장값

- Shape: Always Free 범위 내 ARM 또는 AMD
- 메모리: 가능하면 4GB 이상 권장
- 부트 볼륨: 50GB 안팎부터 시작
- 네트워크 보안 규칙:
  - Ingress 허용: `22`, `80`, `443`
  - DB 포트 `5432` 는 외부 미개방

## 1) 서버 접속

```bash
ssh ubuntu@<VM_PUBLIC_IP>
```

> Oracle Linux 를 쓰면 사용자 계정이 `opc` 인 경우가 많습니다.

## 2) 기본 패키지 업데이트

Ubuntu 기준:

```bash
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y ca-certificates curl git
```

## 3) Docker / Compose 설치

Ubuntu 기준:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
sudo systemctl enable docker
sudo systemctl start docker
docker --version
docker compose version
```

## 4) 소스 내려받기

```bash
git clone https://github.com/devCamel0817/devCamel-backend.git
cd devCamel-backend
```

## 5) 운영 환경 파일 준비

```bash
cp .env.prod.example .env
nano .env
```

최소 수정 항목:

- `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- 필요 시 `POSTGRES_DB`, `POSTGRES_USER`

예시:

```dotenv
POSTGRES_DB=backendlab
POSTGRES_USER=backendlab_app
POSTGRES_PASSWORD=change-this-to-a-strong-secret
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://api.example.com,https://devcamel.dev
```

## 6) 앱 + DB 기동

```bash
docker compose -f docker-compose.yml --env-file .env up --build -d
```

상태 확인:

```bash
docker compose -f docker-compose.yml --env-file .env ps
docker compose -f docker-compose.yml --env-file .env logs backend --tail=100
docker compose -f docker-compose.yml --env-file .env logs postgres --tail=100
```

## 7) 서버 내부 검증

```bash
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS http://127.0.0.1:18080/api/ping
```

정상이면:

- `/actuator/health` 는 `{"status":"UP"...}` 응답
- `/api/ping` 는 `pong` JSON 응답

## 8) 리버스 프록시(Caddy 권장)

이 저장소의 `deploy/Caddyfile` 예시를 사용합니다.

설치 예시(Ubuntu):

```bash
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update
sudo apt-get install -y caddy
```

Caddyfile 적용:

```bash
sudo cp deploy/Caddyfile /etc/caddy/Caddyfile
sudo nano /etc/caddy/Caddyfile
sudo systemctl restart caddy
sudo systemctl status caddy
```

`your-api-domain.example.com` 을 실제 도메인으로 바꾸세요.

## 9) DNS 연결

도메인 DNS 에서 아래를 설정합니다.

- `A` 레코드 → Oracle VM Public IP

예:

- `api.example.com` → `123.123.123.123`

DNS 전파 후 HTTPS 확인:

```bash
curl -I https://api.example.com/actuator/health
```

## 10) 운영 체크리스트

- `22`, `80`, `443` 외 포트는 닫혀 있는가?
- `5432` 가 외부에서 보이지 않는가?
- `.env` 가 git 추적 대상이 아닌가?
- `CORS_ALLOWED_ORIGINS` 에 `localhost` 가 남아 있지 않은가?
- `/actuator/health` 가 200 인가?
- `/api/ping` 가 200 인가?
- `docker compose ps` 에서 `healthy` 인가?

## 자주 만나는 문제

### 1. DB 인증 실패

증상:

- `password authentication failed`

원인:

- 기존 volume 에 이전 `POSTGRES_*` 값이 남아 있음

조치:

```bash
docker compose -f docker-compose.yml --env-file .env down -v
docker compose -f docker-compose.yml --env-file .env up --build -d
```

### 2. 80/443 접속 실패

확인 순서:

- Oracle Cloud Security List / NSG
- OS 방화벽(`ufw` 등)
- Caddy/Nginx 기동 여부
- DNS A 레코드

### 3. 메모리 부족

증상:

- 앱 재시작 반복
- OOM kill

조치:

- 더 큰 메모리 shape 사용
- 동시 트래픽 낮추기
- JVM 옵션 점검

## 업데이트 절차

```bash
cd ~/devCamel-backend
git pull origin main
docker compose -f docker-compose.yml --env-file .env up --build -d
```

## 백업

백업/복구 절차는 `docs/backup-restore.md` 를 참고하세요.

