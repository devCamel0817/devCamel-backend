FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# 의존성 캐시 최적화: 먼저 빌드 스크립트만 복사
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# curl(헬스체크용) 설치, 비-root 사용자 생성
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd  --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=builder --chown=app:app /workspace/build/libs/devcamel-backend-*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

# 컨테이너 친화적 JVM 옵션 (메모리 제한 인지, 빠른 OOM 종료, 엔트로피)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 \
  CMD curl --fail http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
