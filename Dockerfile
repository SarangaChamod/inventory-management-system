# syntax=docker/dockerfile:1

# ---------- Stage 1: build with Maven + JDK 17 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI (GitHub Actions); skip them here to keep image builds fast
RUN mvn -B -q package -DskipTests \
    && cp target/store-management-api.jar /workspace/app.jar

# ---------- Stage 2: minimal JRE 17 runtime ----------
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="store-management-api" \
      org.opencontainers.image.description="Store Management REST API (Spring Boot)"

# Run as an unprivileged, non-root user
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    SERVER_PORT=8080

EXPOSE 8080

# Container health check against the Actuator health endpoint (busybox wget ships with Alpine)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
