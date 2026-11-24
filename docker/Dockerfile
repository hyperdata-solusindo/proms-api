# syntax=docker/dockerfile:1

# ==============================================================================
# Stage 1: Build & Extract Application Layers using Maven & Eclipse Temurin JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Install bash for wrapper script compatibility
RUN apk add --no-cache bash

# 1. Copy Maven wrapper and POM to leverage Docker layer caching
COPY mvnw pom.xml ./
COPY .mvn ./.mvn

# Download dependencies (warm up layer cache)
RUN chmod +x mvnw && ./mvnw dependency:go-offline || true

# 2. Copy source code and resources
COPY src ./src

# 3. Build executable Spring Boot JAR (skip tests since tests run in CI job)
# and extract layers using Spring Boot tools
RUN ./mvnw clean package -DskipTests && \
    UBER_JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" | head -n 1) && \
    java -Djarmode=tools -jar "$UBER_JAR" extract --layers dependencies,spring-boot-loader,snapshot-dependencies,application --launcher --destination /workspace/extracted --force

# ==============================================================================
# Stage 2: Minimal & Hardened JRE 21 Runtime (Optimized Multi-Layer Caching)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl tzdata

# Create dedicated non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy extracted layers in order of change frequency (least frequent to most frequent)
# 1. External dependencies (~90MB, rarely changes -> stays cached across builds)
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/dependencies/ /app/
# 2. Spring Boot loader (~600KB, rarely changes)
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/spring-boot-loader/ /app/
# 3. Snapshot dependencies (if any)
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/snapshot-dependencies/ /app/
# 4. Application code & resources (~2MB, changes on each commit -> only this layer is pulled on deploy)
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/application/ /app/

# Switch to unprivileged system user
USER appuser

# Default Environment Variables
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Default application port
EXPOSE 8080

# Healthcheck using Spring Boot Actuator probe
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=25s \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher \"$@\"", "--"]
