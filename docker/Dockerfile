# syntax=docker/dockerfile:1

# ==============================================================================
# Stage 1: Build the application using Gradle & Eclipse Temurin JDK 21
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Install bash for Gradle wrapper script compatibility
RUN apk add --no-cache bash

# 1. Copy Gradle wrapper and build configuration to leverage Docker layer caching
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (warm up layer cache)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 2. Copy source code and resources
COPY src ./src

# 3. Build executable Spring Boot JAR (skip tests since tests run in CI job)
RUN ./gradlew bootJar -x test --no-daemon && \
    find build/libs -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" -exec cp {} /workspace/app.jar \;

# ==============================================================================
# Stage 2: Minimal & Hardened JRE 21 Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl tzdata

# Create dedicated non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy executable jar from builder stage
COPY --from=builder /workspace/app.jar /app/app.jar

# Set correct ownership
RUN chown -R appuser:appgroup /app

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

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
