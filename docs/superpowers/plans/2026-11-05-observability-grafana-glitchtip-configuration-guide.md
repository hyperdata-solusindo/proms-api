# Observability Configuration Guide (OTel/Grafana & Sentry/GlitchTip) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a production-ready operational configuration guide and environment templates for connecting `proms-api-java` to OpenTelemetry (`https://otel.hyperdata.co.id`), Grafana Tempo (`https://grafana.hyperdata.co.id`), and GlitchTip (`https://glitchtip.hyperdata.co.id`).

**Architecture:** Environment-driven 12-factor configuration using existing Spring Boot Micrometer Tracing (OTel) and Sentry SDK without modifying Java application business code. The deliverables include an updated `.env.example` and a comprehensive documentation runbook in `docs/observability-guide.md`.

**Tech Stack:** Spring Boot 3/4, Micrometer Tracing, OpenTelemetry OTLP Exporter, Sentry SDK 7.x, GlitchTip, Grafana Tempo, Markdown.

**Spec:** [`docs/superpowers/specs/2026-11-05-observability-grafana-glitchtip-configuration-guide-design.md`](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-05-observability-grafana-glitchtip-configuration-guide-design.md)

## Global Constraints

- No application code changes: zero modifications to `src/main/java`.
- HTTPS only: OTel ingestion must use `https://otel.hyperdata.co.id/v1/traces` (no custom ports like `:4318`).
- GlitchTip instance: `https://glitchtip.hyperdata.co.id`.
- PII Masking: Sensitive data masking must remain strictly enabled (`MASKING_ENABLED=true`, `sentry.send-default-pii=false`) adhering to Indonesian UU PDP No. 27/2022.
- Git policy: Never run `git commit` or `git push` automatically.

---

### Task 1: Update `.env.example` with Hyperdata Observability Endpoints

**Files:**
- Modify: `.env.example:100-130`

**Interfaces:**
- Consumes: Environment property mappings in `src/main/resources/application.properties`
- Produces: Updated `.env.example` with clear comments for staging & production endpoints

- [x] **Step 1: Inspect current `.env.example` OTel and Sentry sections**

Run:
```bash
grep -n -C 5 "OTEL_EXPORTER_OTLP_ENDPOINT" .env.example
```
Verify the line numbers and existing default values.

- [x] **Step 2: Update `.env.example` with exact Hyperdata production & staging configurations**

Modify `.env.example` to provide clear guidance and defaults:
```env
# ==============================================================================
# 5. OPENTELEMETRY TRACING & OBSERVABILITY
# ==============================================================================
# Enable/disable distributed tracing
OTEL_TRACING_ENABLED=true

# Sampling probability: 1.0 (100% in staging/dev), 0.1 - 0.2 (10-20% in production)
OTEL_SAMPLING_PROBABILITY=1.0

# OTLP Ingestion Endpoint (HTTPS standard port 443)
# Target server: https://otel.hyperdata.co.id/v1/traces
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel.hyperdata.co.id/v1/traces
OTEL_TIMEOUT_SECONDS=2s


# ==============================================================================
# 6. SENTRY / GLITCHTIP ERROR TRACKING
# ==============================================================================
# GlitchTip DSN obtained from https://glitchtip.hyperdata.co.id/<organization>/<project>
# Example: https://0123456789abcdef0123456789abcdef@glitchtip.hyperdata.co.id/1
SENTRY_DSN=
SENTRY_ENVIRONMENT=local
SENTRY_TRACES_SAMPLE_RATE=1.0
```

- [x] **Step 3: Verify `.env.example` syntax and formatting**

Run:
```bash
git diff .env.example
```
Verify that variables are properly commented and match `application.properties`.

---

### Task 2: Create Comprehensive Observability Runbook (`docs/observability-guide.md`)

**Files:**
- Create: `docs/observability-guide.md`

**Interfaces:**
- Consumes: Specification from `docs/superpowers/specs/2026-11-05-observability-grafana-glitchtip-configuration-guide-design.md`
- Produces: Single source of truth operational guide for DevOps and engineers

- [x] **Step 1: Write `docs/observability-guide.md`**

Write the complete markdown guide containing:
1. Architecture diagram and signal flow.
2. Step-by-step GlitchTip configuration (creating project, getting DSN, alert configuration).
3. Grafana Tempo verification and Trace-to-Logs configuration.
4. Environment configuration guide (`.env` setup for staging & production).
5. Verification runbook with ready-to-run `curl` commands.
6. Incident triage workflow: Cross-referencing `trace_id` from GlitchTip to Grafana Tempo.
7. Troubleshooting matrix (TLS trust, network firewall, DSN syntax).

- [x] **Step 2: Verify all internal links and code snippets in the guide**

Run:
```bash
test -f docs/observability-guide.md && echo "File exists"
```
Verify the file is created with all sections populated and no placeholders.

---

### Task 3: Quality & Consistency Verification

**Files:**
- Verify: `.env.example`
- Verify: `docs/observability-guide.md`

- [x] **Step 1: Run Spotless check**

Run:
```bash
./gradlew spotlessCheck
```
Expected: `BUILD SUCCESSFUL`. (Ensures no inadvertent code or formatting violations occurred).

- [x] **Step 2: Verify git status**

Run:
```bash
git status --short
```
Expected: Only `.env.example`, `docs/observability-guide.md`, and spec/plan docs appear as modified/untracked. Do not commit.
