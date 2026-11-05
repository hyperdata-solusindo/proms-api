#!/usr/bin/env bash
# ==============================================================================
# PROMS Liquibase Migration Helper Script
# Usage:
#   ./scripts/db.sh migrate                 # Apply pending migrations (DDL only)
#   ./scripts/db.sh rollback [N]            # Rollback last N changesets (default: 1)
#   ./scripts/db.sh rollback-to <tag>       # Rollback to specific tag
#   ./scripts/db.sh tag <tag_name>          # Tag current state
#   ./scripts/db.sh status                  # Check pending/applied status
#   ./scripts/db.sh history                 # View deployment history
#   ./scripts/db.sh fresh                   # Drop all & re-migrate (DEV ONLY)
#   ./scripts/db.sh seed-config             # Seed system configurations (dev, staging, prod)
#   ./scripts/db.sh seed-dummy              # Seed dummy data (DEV & STAGING ONLY)
#   ./scripts/db.sh seed                    # Seed both config and dummy (DEV & STAGING)
#
# Execution Runner:
#   By default, uses gradle wrapper (native Java, no Docker needed).
#   Override runner via:
#     RUNNER=gradle ./scripts/db.sh ...     # Run via ./gradlew liquibase
#     RUNNER=liquibase ./scripts/db.sh ...  # Run via system liquibase CLI
#     RUNNER=docker ./scripts/db.sh ...     # Run via Docker container
#     USE_DOCKER=false ./scripts/db.sh ...  # Disallow Docker completely
# ==============================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Load .env if present
if [ -f "${PROJECT_ROOT}/.env" ]; then
    set -a
    # shellcheck disable=SC1090
    . "${PROJECT_ROOT}/.env"
    set +a
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5443}"
DB_NAME="${DB_NAME:-app_platform}"
DB_USER="${DB_USER:-platform_user}"
DB_PASSWORD="${DB_PASSWORD:-platform_password}"
DB_APP_USER="${DB_APP_USER:-${DB_USER:-platform_user}}"
CHANGELOG_FILE="db/changelog/db.changelog-master.yaml"
SEED_CONFIG_FILE="db/seed/db.seed-config-master.yaml"
SEED_DUMMY_FILE="db/seed/db.seed-dummy-master.yaml"
RESOURCES_DIR="${PROJECT_ROOT}/src/main/resources"
LIQUIBASE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

# Determine runner: gradle (native), liquibase (cli), or docker
RUNNER="${RUNNER:-}"

if [ -n "${RUNNER}" ]; then
    case "${RUNNER}" in
        gradle|liquibase|docker) ;;
        *)
            echo "[ERROR] Invalid RUNNER='${RUNNER}'. Must be one of: gradle, liquibase, docker."
            exit 1
            ;;
    esac
elif [ "${USE_DOCKER:-}" = "true" ]; then
    RUNNER="docker"
elif [ "${USE_DOCKER:-}" = "false" ]; then
    if [ -x "${PROJECT_ROOT}/gradlew" ] && command -v java &> /dev/null; then
        RUNNER="gradle"
    elif command -v liquibase &> /dev/null; then
        RUNNER="liquibase"
    else
        echo "[ERROR] Docker is disabled (USE_DOCKER=false), but neither 'gradlew' (with java) nor 'liquibase' CLI is available."
        exit 1
    fi
else
    # Auto-detection: prefer native non-docker execution via project gradlew or local liquibase
    if [ -x "${PROJECT_ROOT}/gradlew" ] && command -v java &> /dev/null; then
        RUNNER="gradle"
    elif command -v liquibase &> /dev/null; then
        RUNNER="liquibase"
    elif command -v docker &> /dev/null; then
        RUNNER="docker"
    else
        echo "[ERROR] No runner available. Neither 'gradlew' (with java), 'liquibase' CLI, nor 'docker' is available."
        exit 1
    fi
fi

run_liquibase() {
    local target_changelog="${1:-${CHANGELOG_FILE}}"
    shift || true

    # Unset Spring-specific liquibase env vars that trigger Liquibase CLI warnings
    local liquibase_clean_env=(env -u LIQUIBASE_ENABLED -u LIQUIBASE_CONTEXTS -u LIQUIBASE_LABELS)

    if [ "${RUNNER}" = "docker" ]; then
        docker run --rm \
            --network host \
            -e JAVA_OPTS="-Dapp_user=${DB_APP_USER}" \
            -v "${RESOURCES_DIR}":/liquibase/resources:ro \
            liquibase/liquibase:4.30-alpine \
            --search-path=/liquibase/resources \
            --changelog-file="${target_changelog}" \
            --url="${LIQUIBASE_URL}" \
            --username="${DB_USER}" \
            --password="${DB_PASSWORD}" \
            "$@"
    elif [ "${RUNNER}" = "gradle" ]; then
        "${liquibase_clean_env[@]}" "${PROJECT_ROOT}/gradlew" -q -p "${PROJECT_ROOT}" \
            -Dapp_user="${DB_APP_USER}" \
            liquibase --args="\"--search-path=${RESOURCES_DIR}\" \"--changelog-file=${target_changelog}\" \"--url=${LIQUIBASE_URL}\" \"--username=${DB_USER}\" \"--password=${DB_PASSWORD}\" $*"
    else
        "${liquibase_clean_env[@]}" JAVA_OPTS="-Dapp_user=${DB_APP_USER} ${JAVA_OPTS:-}" \
        liquibase \
            --search-path="${RESOURCES_DIR}" \
            --changelog-file="${target_changelog}" \
            --url="${LIQUIBASE_URL}" \
            --username="${DB_USER}" \
            --password="${DB_PASSWORD}" \
            "$@"
    fi
}

print_banner() {
    echo "============================================================"
    echo "  PROMS Liquibase Migration Helper"
    echo "  Target: ${LIQUIBASE_URL} (User: ${DB_USER}, App User: ${DB_APP_USER})"
    echo "  Runner: ${RUNNER}"
    echo "============================================================"
}

COMMAND="${1:-status}"

case "${COMMAND}" in
    migrate|update)
        print_banner
        echo "[ACTION] Running database migration (update)..."
        run_liquibase "${CHANGELOG_FILE}" update
        echo "[SUCCESS] Migration completed successfully."
        ;;

    rollback)
        COUNT="${2:-1}"
        print_banner
        echo "[ACTION] Rolling back ${COUNT} changeset(s)..."
        run_liquibase "${CHANGELOG_FILE}" rollback-count --count="${COUNT}"
        echo "[SUCCESS] Rollback of ${COUNT} changeset(s) completed."
        ;;

    rollback-to)
        TAG="${2}"
        if [ -z "${TAG}" ]; then
            echo "[ERROR] Target tag must be specified: ./scripts/db.sh rollback-to <tag>"
            exit 1
        fi
        print_banner
        echo "[ACTION] Rolling back to tag '${TAG}'..."
        run_liquibase "${CHANGELOG_FILE}" rollback --tag="${TAG}"
        echo "[SUCCESS] Rollback to tag '${TAG}' completed."
        ;;

    tag)
        TAG="${2}"
        if [ -z "${TAG}" ]; then
            echo "[ERROR] Tag name must be specified: ./scripts/db.sh tag <tag_name>"
            exit 1
        fi
        print_banner
        echo "[ACTION] Tagging current database state with '${TAG}'..."
        run_liquibase "${CHANGELOG_FILE}" tag --tag="${TAG}"
        echo "[SUCCESS] Database tagged with '${TAG}'."
        ;;

    status)
        print_banner
        echo "[ACTION] Checking migration status..."
        run_liquibase "${CHANGELOG_FILE}" status --verbose
        ;;

    history)
        print_banner
        echo "[ACTION] Fetching deployment history..."
        run_liquibase "${CHANGELOG_FILE}" history
        ;;

    fresh)
        print_banner
        echo "[WARNING] You are about to drop all database objects in '${DB_NAME}'!"
        if [ "${SPRING_PROFILES_ACTIVE}" = "prod" ] || [ "${SPRING_PROFILES_ACTIVE}" = "production" ]; then
            echo "[FORBIDDEN] Cannot run 'fresh' on production environment!"
            exit 1
        fi

        echo "[ACTION] Dropping all database objects..."
        run_liquibase "${CHANGELOG_FILE}" drop-all

        echo "[ACTION] Re-running migration from scratch..."
        run_liquibase "${CHANGELOG_FILE}" update

        echo "[SUCCESS] Database freshly recreated and migrated."
        ;;

    seed-config)
        print_banner
        echo "[ACTION] Seeding system configurations..."
        run_liquibase "${SEED_CONFIG_FILE}" update
        echo "[SUCCESS] Configuration seed completed successfully."
        ;;

    seed-dummy)
        print_banner
        if [ "${SPRING_PROFILES_ACTIVE}" = "prod" ] || [ "${SPRING_PROFILES_ACTIVE}" = "production" ]; then
            echo "[FORBIDDEN] Cannot seed dummy data on production environment!"
            exit 1
        fi
        echo "[ACTION] Seeding test dummy data..."
        run_liquibase "${SEED_DUMMY_FILE}" update
        echo "[SUCCESS] Test dummy seed completed successfully."
        ;;

    seed)
        print_banner
        if [ "${SPRING_PROFILES_ACTIVE}" = "prod" ] || [ "${SPRING_PROFILES_ACTIVE}" = "production" ]; then
            echo "[FORBIDDEN] Cannot seed dummy data on production environment!"
            exit 1
        fi
        echo "[ACTION] 1/2: Seeding system configurations..."
        run_liquibase "${SEED_CONFIG_FILE}" update
        echo "[ACTION] 2/2: Seeding test dummy data..."
        run_liquibase "${SEED_DUMMY_FILE}" update
        echo "[SUCCESS] All seeders completed successfully."
        ;;

    *)
        echo "Usage: $0 {migrate|rollback|rollback-to|tag|status|history|fresh|seed-config|seed-dummy|seed}"
        exit 1
        ;;
esac
