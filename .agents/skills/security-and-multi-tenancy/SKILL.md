---
name: security-and-multi-tenancy
description: Audits code for tenant data isolation, RLS cache prefixing, zero hardcoded credentials, UU PDP privacy compliance, token authentication filter boundaries, and Cloudflare security attack classes.
---

# Security & Multi-Tenancy Review Skill (Cloudflare Security Audit Standard)

Use this specialized skill when reviewing changes involving authentication, authorization, filters, tenant contexts, tokens, secret management, caching, database queries, or application configuration. This skill synthesizes project Golden Rules (Rule 10 & 14) with battle-tested attack classes from Cloudflare's Security Audit Framework.

> [!NOTE]
> Skill ini bekerja bahu-membahu dengan skill resmi Cloudflare di [.agents/skills/cloudflare-security-audit/SKILL.md](.agents/skills/cloudflare-security-audit/SKILL.md) (Upstream: [cloudflare/security-audit-skill](https://github.com/cloudflare/security-audit-skill)). Untuk taksonomi lengkap kerentanan multi-tier, hunting workflow, dan validation harness, rujuk berkas pendamping Cloudflare murni.

## Core Audit Disciplines (Cloudflare Standard)

1. **A Tenant Field on a Record is NOT Isolation**:
   - Having `tenantId` in an entity or DTO does not mean data is isolated.
   - Reviewer must verify the query, predicate, or filter enforcing that isolation on **every read and write path**.
   - **BOLA / IDOR Prevention**: A query like `findById(id)` without verifying `tenantId == currentTenantId` is a severe vulnerability.
   - **Cross-Tenant IDOR Disguise (404 Not Found)**: Akses ke resource milik tenant lain WAJIB melempar `ResourceNotFoundException` (HTTP 404), BUKAN HTTP 403 Forbidden, guna mencegah *tenant resource enumeration* / information disclosure.

2. **Composite-Key & Namespace Collision (Cache & Redis)**:
   - Cache keys for tenant-scoped data MUST explicitly include the tenant ID prefix:
     ```java
     // DO: Isolated cache namespace per tenant
     @Cacheable(value = "user_permissions", key = "#tenantId + ':' + #userId")

     // DON'T: Namespace collision / cross-tenant cache poisoning!
     @Cacheable(value = "user_permissions", key = "#userId")
     ```

3. **Authentication & Token Boundary (Zero Entity Leakage & PAT Architecture)**:
   - Security filters (e.g. `TokenAuthenticationFilter`) must NOT interact with JPA repositories or entities directly.
   - Must use an `AuthClient` returning immutable DTO records (`VerifiedTokenDto`).
   - Authentication uses stateful Personal Access Token (PAT) with `<tokenId>|<plainToken>` format:
     - Verified against database hashed token (SHA-256 via `HashingUtil.sha256()`).
     - Enforces sliding window expiration (30-day TTL, sliding renewal when `< 7 days remaining`).

4. **Resource Exhaustion & Availability (Availability Attack Class)**:
   - **Unbounded Queries**: Every list query exposed to API callers must enforce pagination (`Pageable` / `limit`). Queries without boundaries cause Out-Of-Memory (OOM) DoS.
   - **Anti-N+1**: Dilarang melakukan SQL queries di dalam perulangan loop. Gunakan The Golden Stack (batch lookups in-memory via `Lookups`).

5. **Distributed 2-Tier Rate Limiting (Bucket4j + Redis)**:
   - **Tier 1 (Pre-Auth IP Limiter)**: Melindungi endpoint publik (`/api/v1/auth/**`) dari brute force & credential stuffing berbasis client IP (mendukung header `CF-Connecting-IP`, `X-Forwarded-For`).
   - **Tier 2 (Post-Auth Tenant & User Limiter)**: Anti-noisy-neighbor per tenant dan in-flight concurrency limiter per user token. Melempar HTTP 429 `RATE_LIMIT_EXCEEDED` dengan header `Retry-After`, `X-RateLimit-Limit`, dan `X-RateLimit-Remaining`.

6. **Zero Hardcoded Secrets & Credentials**:
   - No passwords, API keys, private keys, or tokens hardcoded in code, YAML, properties, or test files.
   - Secrets must use environment variable interpolation (e.g., `${GEMINI_API_KEY}`, `${DB_PASSWORD}`).

7. **Data Privacy & Lifecycle (UU PDP No. 27/2022 Compliance)**:
   - Test data and seed data must use synthetic/fictitious names, emails, and phone numbers.
   - Soft-deleted entities must not leak through direct lookup, search, or un-filtered relation traversal.
   - Sensitive fields (passwords, tokens, PII) must never be emitted into application logs, error traces, or diagnostic dumps.

## Review Output Format for Security
- **Tenant & Object Isolation (BOLA/IDOR & 404 Disguise)**: [PASS / VIOLATION] (Catatan evaluasi isolasi query, cross-tenant 404 disguise & cache prefix `#tenantId + ':' + #id`)
- **Token & PAT Sliding Window Boundary**: [PASS / VIOLATION] (Catatan evaluasi validasi PAT hash SHA-256, isolasi filter, & DTO immutability)
- **Resource Exhaustion & 2-Tier Rate Limiting**: [PASS / VIOLATION] (Catatan evaluasi batas paginasi, anti-N+1, & Bucket4j Redis throttling)
- **Secret & Privacy Hygiene (UU PDP No. 27/2022)**: [PASS / VIOLATION] (Catatan evaluasi sanitasi kredensial, log masking, & data sintetis)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
