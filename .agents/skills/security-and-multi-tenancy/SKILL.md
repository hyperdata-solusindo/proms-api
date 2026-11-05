---
name: security-and-multi-tenancy
description: Audits code for tenant data isolation, RLS cache prefixing, zero hardcoded credentials, UU PDP privacy compliance, token authentication filter boundaries, and Cloudflare security attack classes.
---

# Security & Multi-Tenancy Review Skill (Cloudflare Security Audit Standard)

Use this specialized skill when reviewing changes involving authentication, authorization, filters, tenant contexts, tokens, secret management, caching, database queries, or application configuration. This skill synthesizes project Golden Rules (Rule 10 & 14) with battle-tested attack classes from Cloudflare's Security Audit Framework.

## Core Audit Disciplines (Cloudflare Standard)

1. **A Tenant Field on a Record is NOT Isolation**:
   - Having `tenantId` in an entity or DTO does not mean data is isolated.
   - Reviewer must verify the query, predicate, or filter enforcing that isolation on **every read and write path**.
   - **BOLA / IDOR Prevention**: A query like `findById(id)` without verifying `tenantId == currentTenantId` is a severe vulnerability.

2. **Composite-Key & Namespace Collision (Cache & Redis)**:
   - Cache keys for tenant-scoped data MUST explicitly include the tenant ID prefix:
     ```java
     // DO: Isolated cache namespace per tenant
     @Cacheable(value = "user_permissions", key = "#tenantId + ':' + #userId")

     // DON'T: Namespace collision / cross-tenant cache poisoning!
     @Cacheable(value = "user_permissions", key = "#userId")
     ```

3. **Authentication & Token Boundary (Zero Entity Leakage)**:
   - Security filters (e.g. `TokenAuthenticationFilter`) must NOT interact with JPA repositories or entities directly.
   - Must use an `AuthFacade` returning immutable DTO records (`VerifiedTokenDto`).
   - Token validation must enforce server-pinned signature algorithms, validity window (`exp`, `nbf`), audience (`aud`), and issuer (`iss`).

4. **Resource Exhaustion & Availability (Availability Attack Class)**:
   - **Unbounded Queries**: Every list query exposed to API callers must enforce pagination (`Pageable` / `limit`). Queries without boundaries cause Out-Of-Memory (OOM) DoS.
   - **Anti-N+1**: Dilarang melakukan SQL queries di dalam perulangan loop. Gunakan The Golden Stack (batch lookups in-memory via `Lookups`).

5. **Zero Hardcoded Secrets & Credentials**:
   - No passwords, API keys, private keys, or tokens hardcoded in code, YAML, properties, or test files.
   - Secrets must use environment variable interpolation (e.g., `${GEMINI_API_KEY}`, `${DB_PASSWORD}`).

6. **Data Privacy & Lifecycle (UU PDP No. 27/2022 Compliance)**:
   - Test data and seed data must use synthetic/fictitious names, emails, and phone numbers.
   - Soft-deleted entities must not leak through direct lookup, search, or un-filtered relation traversal.
   - Sensitive fields (passwords, tokens, PII) must never be emitted into application logs, error traces, or diagnostic dumps.

## Review Output Format for Security
- **Tenant & Object Isolation (BOLA/IDOR)**: [PASS / VIOLATION] (Catatan evaluasi)
- **Token & Auth Boundary**: [PASS / VIOLATION] (Catatan evaluasi)
- **Resource Exhaustion & Limits**: [PASS / VIOLATION] (Catatan evaluasi)
- **Secret & Privacy Hygiene (UU PDP)**: [PASS / VIOLATION] (Catatan evaluasi)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
