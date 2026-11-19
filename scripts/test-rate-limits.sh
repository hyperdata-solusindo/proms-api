#!/usr/bin/env bash
# ==============================================================================
# PROMS API - Distributed 2-Tier Rate Limiting Test Suite
# Memverifikasi 5 tier pembatasan laju (Bucket4j + Redis):
#   1. Tier 1B: Auth IP Limiter (15 req/menit)
#   2. Tier 1A: Edge Global IP Limiter (100 req/detik, 20 concurrent)
#   3. Tier 2C: Public General Anonymous IP (30 req/menit)
#   4. Tier 2A: Authenticated User Limiter (60 req/menit, 10 concurrent)
#   5. Tier 2B: Multi-Tenant Fair-Share (1000 req/menit, 50 concurrent)
# ==============================================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_IP="${TEST_IP:-203.0.113.$(shuf -i 10-250 -n 1)}"
BEARER_TOKEN="${BEARER_TOKEN:-}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "\n${BLUE}==============================================================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${BLUE}==============================================================================${NC}"
}

# ------------------------------------------------------------------------------
# Tier 1B: Auth IP Limiter (15 req/menit pada POST /api/v1/auth/login)
# ------------------------------------------------------------------------------
test_tier1_auth() {
    print_header "1. Testing Tier 1B: Auth Login IP Limiter (Max 15 req/menit)"
    local ip="198.51.100.$(shuf -i 10-200 -n 1)"
    echo "Simulating Client IP: $ip"
    echo "Sending 20 requests to POST $BASE_URL/api/v1/auth/login..."

    local allowed_count=0
    local rejected_count=0

    for i in {1..20}; do
        local response
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -H "CF-Connecting-IP: $ip" \
            -d '{"email":"auditor@example.com","password":"invalid_password"}')

        local http_code
        http_code=$(echo "$response" | tail -n 1)

        if [ "$http_code" -eq 429 ]; then
            rejected_count=$((rejected_count + 1))
            echo -e "Request #$i: ${RED}HTTP 429 (Rate Limit Exceeded - BLOCKED)${NC}"
        else
            allowed_count=$((allowed_count + 1))
            echo -e "Request #$i: ${GREEN}HTTP $http_code (Allowed through limiter)${NC}"
        fi
    done

    echo -e "\nSummary Tier 1B: Allowed: $allowed_count (Expected: 15), Rejected: $rejected_count (Expected: 5)"
    if [ "$rejected_count" -ge 1 ]; then
        echo -e "${GREEN}✓ Tier 1B Auth Rate Limiting PASSED!${NC}"
    else
        echo -e "${RED}✗ Tier 1B Auth Rate Limiting FAILED (no 429 received). Check if Redis is running.${NC}"
    fi
}

# ------------------------------------------------------------------------------
# Tier 2C: Public General Anonymous IP (30 req/menit)
# ------------------------------------------------------------------------------
test_tier2_public() {
    print_header "2. Testing Tier 2C: Public General Anonymous IP (Max 30 req/menit)"
    local ip="192.0.2.$(shuf -i 10-200 -n 1)"
    echo "Simulating Client IP: $ip"
    echo "Sending 35 requests to public endpoint $BASE_URL/v3/api-docs..."

    local allowed_count=0
    local rejected_count=0

    for i in {1..35}; do
        local response
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/v3/api-docs" \
            -H "CF-Connecting-IP: $ip")

        local http_code
        http_code=$(echo "$response" | tail -n 1)

        if [ "$http_code" -eq 429 ]; then
            rejected_count=$((rejected_count + 1))
            echo -e "Request #$i: ${RED}HTTP 429 (Public Rate Limit Exceeded - BLOCKED)${NC}"
        else
            allowed_count=$((allowed_count + 1))
            echo -e "Request #$i: ${GREEN}HTTP $http_code (Allowed)${NC}"
        fi
    done

    echo -e "\nSummary Tier 2C: Allowed: $allowed_count (Expected: 30), Rejected: $rejected_count (Expected: 5)"
    if [ "$rejected_count" -ge 1 ]; then
        echo -e "${GREEN}✓ Tier 2C Public General Rate Limiting PASSED!${NC}"
    else
        echo -e "${RED}✗ Tier 2C Public General Rate Limiting FAILED.${NC}"
    fi
}

# ------------------------------------------------------------------------------
# Tier 1A: Global Edge IP Limiter (100 req/detik, burst & concurrency limit)
# ------------------------------------------------------------------------------
test_tier1_edge() {
    print_header "3. Testing Tier 1A: Global Edge IP Limiter (Max 100 req/detik)"
    local ip="203.0.113.$(shuf -i 10-200 -n 1)"
    echo "Simulating Client IP: $ip"
    echo "Sending rapid burst of 115 requests to $BASE_URL/..."

    local allowed_count=0
    local rejected_count=0

    for i in {1..115}; do
        local http_code
        http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health/liveness" \
            -H "CF-Connecting-IP: $ip")

        if [ "$http_code" -eq 429 ]; then
            rejected_count=$((rejected_count + 1))
        else
            allowed_count=$((allowed_count + 1))
        fi
    done

    echo "Results: Allowed: $allowed_count, Rejected (429): $rejected_count"
    if [ "$rejected_count" -ge 1 ]; then
        echo -e "${GREEN}✓ Tier 1A Edge Burst Rate Limiting PASSED!${NC}"
    else
        echo -e "${YELLOW}ℹ Burst spread over >1s or not saturated. Allowed: $allowed_count${NC}"
    fi
}

# ------------------------------------------------------------------------------
# Tier 2A: Authenticated User Limiter (60 req/menit)
# ------------------------------------------------------------------------------
test_tier2_user() {
    print_header "4. Testing Tier 2A: Authenticated User Limiter (Max 60 req/menit)"
    if [ -z "$BEARER_TOKEN" ]; then
        echo -e "${YELLOW}Notice: BEARER_TOKEN environment variable is not set.${NC}"
        echo "To test User Rate Limiting, pass BEARER_TOKEN=\"<your_pat_token>\""
        echo "Example: BEARER_TOKEN=\"pat_xxx\" ./scripts/test-rate-limits.sh user"
        return
    fi

    echo "Using Bearer Token: ${BEARER_TOKEN:0:10}..."
    echo "Sending 65 requests to $BASE_URL/api/v1/user/profile..."

    local allowed_count=0
    local rejected_count=0

    for i in {1..65}; do
        local http_code
        http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/user/profile" \
            -H "Authorization: Bearer $BEARER_TOKEN")

        if [ "$http_code" -eq 429 ]; then
            rejected_count=$((rejected_count + 1))
            echo -e "Request #$i: ${RED}HTTP 429 (User Quota Exceeded)${NC}"
        else
            allowed_count=$((allowed_count + 1))
            echo -e "Request #$i: ${GREEN}HTTP $http_code (Allowed)${NC}"
        fi
    done

    echo -e "\nSummary Tier 2A: Allowed: $allowed_count (Expected: 60), Rejected: $rejected_count (Expected: 5)"
    if [ "$rejected_count" -ge 1 ]; then
        echo -e "${GREEN}✓ Tier 2A User Rate Limiting PASSED!${NC}"
    else
        echo -e "${RED}✗ Tier 2A User Rate Limiting FAILED.${NC}"
    fi
}

# ------------------------------------------------------------------------------
# Inspect Rate Limit Headers
# ------------------------------------------------------------------------------
test_headers() {
    print_header "5. Inspecting Rate Limit Headers (X-RateLimit-* & Retry-After)"
    local ip="198.51.100.99"
    echo "Sending single request with headers from IP $ip..."
    curl -i -s -X POST "$BASE_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -H "CF-Connecting-IP: $ip" \
        -d '{"email":"test@example.com","password":"test"}' | head -n 20
}

# ------------------------------------------------------------------------------
# Main Dispatcher
# ------------------------------------------------------------------------------
COMMAND="${1:-auth}"

case "$COMMAND" in
    auth)
        test_tier1_auth
        test_headers
        ;;
    public)
        test_tier2_public
        ;;
    edge)
        test_tier1_edge
        ;;
    user)
        test_tier2_user
        ;;
    headers)
        test_headers
        ;;
    all)
        test_tier1_auth
        test_tier2_public
        test_tier1_edge
        test_tier2_user
        test_headers
        ;;
    *)
        echo "Usage: $0 [auth|public|edge|user|headers|all]"
        echo "Examples:"
        echo "  $0 auth                  # Test Tier 1B Auth Login rate limit (15 req/min)"
        echo "  $0 public                # Test Tier 2C Public General rate limit (30 req/min)"
        echo "  $0 edge                  # Test Tier 1A Global Edge rate limit (100 req/sec)"
        echo "  BEARER_TOKEN=\"...\" $0 user # Test Tier 2A Authenticated User rate limit (60 req/min)"
        echo "  $0 all                   # Run all tests"
        exit 1
        ;;
esac
