# 배포 체크리스트

## 배포 전

- [ ] `main` 최신 코드 pull 완료
- [ ] `.env` 작성 완료
- [ ] `POSTGRES_PASSWORD` 를 충분히 강한 값으로 교체
- [ ] `CORS_ALLOWED_ORIGINS` 를 운영 도메인으로 설정
- [ ] Oracle Cloud Security List / NSG 에 `22`, `80`, `443` 만 허용
- [ ] `5432` 외부 미개방 확인
- [ ] 도메인 A 레코드 설정 완료

## 배포 실행

- [ ] `docker compose -f docker-compose.yml --env-file .env up --build -d`
- [ ] `docker compose -f docker-compose.yml --env-file .env ps`
- [ ] `docker compose -f docker-compose.yml --env-file .env logs backend --tail=100`
- [ ] `docker compose -f docker-compose.yml --env-file .env logs postgres --tail=100`

## 앱 검증

- [ ] `curl http://127.0.0.1:18080/actuator/health`
- [ ] `curl http://127.0.0.1:18080/api/ping`
- [ ] `/actuator/health` 가 `UP`
- [ ] `/api/ping` 이 200 응답
- [ ] `docker compose ps` 에서 `healthy`

## 프록시 / HTTPS 검증

- [ ] `https://<domain>/actuator/health` 200
- [ ] `https://<domain>/api/ping` 200
- [ ] 인증서 자동 발급 성공
- [ ] 브라우저 경고 없음

## 보안 점검

- [ ] `.env` 미커밋 확인
- [ ] `5432` 외부 접근 불가 확인
- [ ] `localhost` 가 운영용 `CORS_ALLOWED_ORIGINS` 에 남아있지 않음
- [ ] 불필요한 포트 미노출
- [ ] 정기 백업 설정 완료

## 장애 대응 준비

- [ ] `docs/backup-restore.md` 확인
- [ ] 최근 백업 파일 존재 확인
- [ ] `docker compose down -v` 가 데이터 삭제라는 점 인지
- [ ] DB 자격증명 변경 시 volume 재초기화 필요 여부 확인

