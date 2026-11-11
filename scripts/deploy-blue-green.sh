#!/usr/bin/env bash
# ==============================================================================
# PROMS API - Production Blue/Green Deployment & Migration Script
# ==============================================================================
# Usage:
#   ./deploy-blue-green.sh <IMAGE_TAG>
# Example:
#   ./deploy-blue-green.sh ghcr.io/hyperdata-solusindo/proms-api-java:v1.0.0
# ==============================================================================

set -euo pipefail

IMAGE="${1:-}"
if [ -z "$IMAGE" ]; then
  echo "❌ Error: Docker image must be provided as the first argument."
  echo "Usage: $0 <FULL_IMAGE_NAME:TAG>"
  exit 1
fi

APP_DIR="${APP_DIR:-/opt/proms/app}"
NGINX_UPSTREAM_FILE="${NGINX_UPSTREAM_FILE:-/etc/nginx/conf.d/proms-active-upstream.conf}"
NETWORK_NAME="proms-network"

cd "$APP_DIR"

if [ ! -f .env ]; then
  echo "❌ Error: .env file not found in $APP_DIR"
  exit 1
fi

echo "===================================================================="
echo "🚀 Starting Production Blue/Green Deployment"
echo "📦 Target Image: $IMAGE"
echo "===================================================================="

# 1. Pastikan Docker network tersedia
docker network create "$NETWORK_NAME" 2>/dev/null || true

# 2. Pull image terbaru
echo "📥 Pulling latest image..."
docker pull "$IMAGE"

# ==============================================================================
# STEP 1: Dedicated Database Migration Runner (One-Shot Job)
# ==============================================================================
echo "--------------------------------------------------------------------"
echo "🛠️ Step 1: Running Database Migrations (Liquibase One-Shot Runner)..."
echo "--------------------------------------------------------------------"

# Jalankan container sementara khusus migrasi database sampai selesai (exit 0)
docker run --rm \
  --name proms-db-migrator \
  --network "$NETWORK_NAME" \
  --env-file .env \
  "$IMAGE" \
  --spring.main.web-application-type=none \
  --spring.liquibase.enabled=true \
  --spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml || {
    echo "❌ FATAL: Database migration failed! Aborting deployment."
    exit 1
  }

echo "✅ Database migrations applied successfully."

# ==============================================================================
# STEP 2: Tentukan Slot Aktif & Standby (Blue vs Green)
# ==============================================================================
echo "--------------------------------------------------------------------"
echo "🔄 Step 2: Resolving Active & Idle Slot..."
echo "--------------------------------------------------------------------"

PORT_BLUE=8081
PORT_GREEN=8082

CURRENT_ACTIVE="none"
if [ -f "$NGINX_UPSTREAM_FILE" ]; then
  if grep -q "127.0.0.1:$PORT_BLUE" "$NGINX_UPSTREAM_FILE"; then
    CURRENT_ACTIVE="blue"
  elif grep -q "127.0.0.1:$PORT_GREEN" "$NGINX_UPSTREAM_FILE"; then
    CURRENT_ACTIVE="green"
  fi
fi

if [ "$CURRENT_ACTIVE" = "blue" ]; then
  TARGET_SLOT="green"
  TARGET_PORT=$PORT_GREEN
  OLD_SLOT="blue"
  OLD_PORT=$PORT_BLUE
else
  # Default ke blue jika pertama kali setup atau active adalah green
  TARGET_SLOT="blue"
  TARGET_PORT=$PORT_BLUE
  OLD_SLOT="green"
  OLD_PORT=$PORT_GREEN
fi

echo "Current Active Slot : $CURRENT_ACTIVE"
echo "Target Standby Slot : $TARGET_SLOT (Port: $TARGET_PORT)"

# ==============================================================================
# STEP 3: Luncurkan Container Standby Slot Baru
# ==============================================================================
echo "--------------------------------------------------------------------"
echo "🚢 Step 3: Launching Target Slot Container (proms-api-$TARGET_SLOT)..."
echo "--------------------------------------------------------------------"

docker stop "proms-api-$TARGET_SLOT" 2>/dev/null || true
docker rm -f "proms-api-$TARGET_SLOT" 2>/dev/null || true

docker run -d \
  --name "proms-api-$TARGET_SLOT" \
  --restart unless-stopped \
  --network "$NETWORK_NAME" \
  -p "127.0.0.1:$TARGET_PORT:8080" \
  --env-file .env \
  -e SERVER_PORT=8080 \
  -e LIQUIBASE_ENABLED=false \
  -e SPRING_PROFILES_ACTIVE=prod \
  "$IMAGE"

# ==============================================================================
# STEP 4: Actuator Health Check Verification Gate
# ==============================================================================
echo "--------------------------------------------------------------------"
echo "🔍 Step 4: Verifying Spring Boot Actuator Health on Port $TARGET_PORT..."
echo "--------------------------------------------------------------------"

HEALTHY=false
for i in $(seq 1 15); do
  STATUS=$(curl -s "http://127.0.0.1:$TARGET_PORT/actuator/health/liveness" | grep -o '"status":"UP"' || true)
  if [ -n "$STATUS" ]; then
    HEALTHY=true
    echo "✅ Slot $TARGET_SLOT is UP and healthy!"
    break
  fi
  echo "Waiting for slot $TARGET_SLOT to boot ($i/15)..."
  sleep 4
done

if [ "$HEALTHY" != "true" ]; then
  echo "❌ Health check failed on slot $TARGET_SLOT!"
  echo "Dumping container logs:"
  docker logs --tail 50 "proms-api-$TARGET_SLOT"
  echo "Aborting deployment! Removing unhealthy container..."
  docker stop "proms-api-$TARGET_SLOT" && docker rm "proms-api-$TARGET_SLOT"
  exit 1
fi

# ==============================================================================
# STEP 5: Switch Nginx Traffic & Graceful Reload
# ==============================================================================
echo "--------------------------------------------------------------------"
echo "🔀 Step 5: Switching Nginx Upstream to $TARGET_SLOT & Reloading..."
echo "--------------------------------------------------------------------"

echo "server 127.0.0.1:$TARGET_PORT;" > "$NGINX_UPSTREAM_FILE"

# Validasi sintaks nginx sebelum reload
if command -v nginx >/dev/null 2>&1; then
  sudo nginx -t
  sudo nginx -s reload
  echo "✅ Nginx reloaded successfully. Traffic now routing to $TARGET_SLOT."
else
  echo "⚠️ Warning: Nginx command not found locally. Pastikan Nginx di-reload untuk mengaktifkan upstream baru."
fi

# ==============================================================================
# STEP 6: Graceful Teardown of Old Slot
# ==============================================================================
if [ "$OLD_SLOT" != "none" ] && docker ps -q -f name="proms-api-$OLD_SLOT" | grep -q .; then
  echo "--------------------------------------------------------------------"
  echo "🧹 Step 6: Gracefully Stopping Old Slot Container (proms-api-$OLD_SLOT)..."
  echo "--------------------------------------------------------------------"
  # Beri waktu jeda 10 detik agar koneksi HTTP in-flight selesai dilayani
  sleep 10
  docker stop "proms-api-$OLD_SLOT"
  echo "✅ Old slot $OLD_SLOT stopped."
fi

echo "===================================================================="
echo "🎉 DEPLOYMENT SUCCESSFUL!"
echo "Active Slot: $TARGET_SLOT (Port: $TARGET_PORT)"
echo "Image      : $IMAGE"
echo "===================================================================="
