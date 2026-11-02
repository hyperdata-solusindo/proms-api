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
CHANGELOG_FILE="db/changelog/db.changelog-master.yaml"
SEED_CONFIG_FILE="db/seed/db.seed-config-master.yaml"
SEED_DUMMY_FILE="db/seed/db.seed-dummy-master.yaml"
RESOURCES_DIR="${PROJECT_ROOT}/src/main/resources"
LIQUIBASE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

# Check docker or local liquibase availability
USE_DOCKER=false
if ! command -v liquibase &> /dev/null; then
    if command -v docker &> /dev/null; then
        USE_DOCKER=true
    else
        echo "[ERROR] Neither 'liquibase' CLI nor 'docker' is installed."
        exit 1
    fi
fi

run_liquibase() {
    local target_changelog="${1:-${CHANGELOG_FILE}}"
    shift || true

    if [ "${USE_DOCKER}" = true ]; then
        docker run --rm \
            --network host \
            -v "${RESOURCES_DIR}":/liquibase/resources:ro \
            liquibase/liquibase:4.30-alpine \
            --search-path=/liquibase/resources \
            --changelog-file="${target_changelog}" \
            --url="${LIQUIBASE_URL}" \
            --username="${DB_USER}" \
            --password="${DB_PASSWORD}" \
            "$@"
    else
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
    echo "  Target: ${LIQUIBASE_URL} (User: ${DB_USER})"
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
            echo "[ERROR] Please specify a tag name. Example: ./scripts/db.sh rollback-to v1.0.0"
            exit 1
        fi
        print_banner
        echo "[ACTION] Rolling back to tag: ${TAG}...\""
        run_liquibase "${CHANGELOG_FILE}" rollback "${TAG}"
        echo "[SUCCESS] Rollback to tag ${TAG} completed."
        ;;

    tag)
        TAG="${2}"
        if [ -z "${TAG}" ]; then
            echo "[ERROR] Please specify a tag name. Example: ./scripts/db.sh tag v1.0.0"
            exit 1
        fi
        print_banner
        echo "[ACTION] Tagging database with: ${TAG}..."
        run_liquibase "${CHANGELOG_FILE}" tag "${TAG}"
        echo "[SUCCESS] Tag ${TAG} created."
        ;;

    status)
        print_banner
        echo "[ACTION] Checking migration status..."
        run_liquibase "${CHANGELOG_FILE}" status --verbose
        ;;

    history)
        print_banner
        echo "[ACTION] Viewing migration history..."
        run_liquibase "${CHANGELOG_FILE}" history
        ;;

    seed-config)
        print_banner
        echo "[ACTION] Running system configuration seeder (dev, staging, prod)..."
        run_liquibase "${SEED_CONFIG_FILE}" update
        echo "[SUCCESS] Configuration seed completed."
        ;;

    seed-dummy)
        # Production Safety Guard
        if [[ "${SPRING_PROFILES_ACTIVE}" =~ "prod" ]] || [[ "${ENVIRONMENT}" =~ "prod" ]]; then
            echo "[FATAL] 'seed-dummy' is strictly forbidden in PRODUCTION!"
            exit 1
        fi
        print_banner
        echo "[ACTION] Running test dummy data seeder (dev, staging only)..."
        run_liquibase "${SEED_DUMMY_FILE}" update
        echo "[SUCCESS] Dummy data seed completed."
        ;;

    seed)
        # Production Safety Guard
        if [[ "${SPRING_PROFILES_ACTIVE}" =~ "prod" ]] || [[ "${ENVIRONMENT}" =~ "prod" ]]; then
            echo "[FATAL] 'seed' (all) contains dummy data and is strictly forbidden in PRODUCTION!"
            echo "Use './scripts/db.sh seed-config' for production initial master data."
            exit 1
        fi
        print_banner
        echo "[ACTION] 1/2: Seeding system configurations..."
        run_liquibase "${SEED_CONFIG_FILE}" update
        echo "[ACTION] 2/2: Seeding test dummy data..."
        run_liquibase "${SEED_DUMMY_FILE}" update
        echo "[SUCCESS] All seeders completed successfully."
        ;;

    fresh|refresh)
        # Production Safety Guard
        if [[ "${SPRING_PROFILES_ACTIVE}" =~ "prod" ]] || [[ "${ENVIRONMENT}" =~ "prod" ]]; then
            echo "[FATAL] 'fresh' command is strictly forbidden in PRODUCTION!"
            exit 1
        fi

        print_banner
        echo "[WARNING] This will DROP ALL database objects in ${DB_NAME} and re-run all migrations!"
        read -r -p "Are you sure you want to proceed? [y/N] " response
        case "$response" in
            [yY][eE][sS]|[yY])
                echo "[ACTION] Dropping all database objects..."
                run_liquibase "${CHANGELOG_FILE}" dropAll --schemas=public,platform,proms
                echo "[ACTION] Re-running all migrations..."
                run_liquibase "${CHANGELOG_FILE}" update
                echo "[SUCCESS] Database has been refreshed cleanly."
                ;;
            *)
                echo "[CANCELLED] Fresh migration aborted."
                exit 0
                ;;
        esac
        ;;

    *)
        echo "Usage: $0 {migrate|rollback [count]|rollback-to <tag>|tag <name>|status|history|fresh|seed-config|seed-dummy|seed}"
        exit 1
        ;;
esac
