# syntax=docker/dockerfile:1

# ==========================================
# Stage 1: Build the application using Gradle & JDK 21
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Install bash for Gradle wrapper script compatibility
RUN apk add --no-cache bash

# 1. Copy Gradle wrapper and configuration files first to leverage build cache
COPY platform/gradlew ./platform/
COPY platform/gradle ./platform/gradle
COPY platform/build.gradle.kts platform/settings.gradle.kts ./platform/
COPY common-cli/build.gradle.kts ./common-cli/

# 2. Download dependencies (cached layer)
WORKDIR /workspace/platform
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 3. Copy source code for common-cli and platform
COPY common-cli/src /workspace/common-cli/src
COPY platform/src /workspace/platform/src

# 4. Build application executable jar without running tests
RUN ./gradlew bootJar -x test --no-daemon && \
    find build/libs -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" -exec cp {} /workspace/app.jar \;

# ==========================================
# Stage 2: Minimal JRE 21 Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy executable jar from builder stage
COPY --from=builder /workspace/app.jar /app/app.jar

# Set correct ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Default Environment Variables
ENV DB_HOST=postgres \
    DB_PORT=5432 \
    DB_NAME=app_platform \
    DB_USER=platform_user \
    DB_PASSWORD=platform_password \
    LIQUIBASE_ENABLED=true \
    JAVA_OPTS=""

# Default Spring Boot server port
EXPOSE 8080

# Support passing JVM options and CLI arguments (e.g. db:migrate, db:seed)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
