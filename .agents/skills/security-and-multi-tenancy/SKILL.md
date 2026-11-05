---
name: security-and-multi-tenancy
description: Audits code for tenant data isolation, RLS cache prefixing, zero hardcoded credentials, UU PDP privacy compliance, and token authentication filter boundaries.
---

# Security & Multi-Tenancy Review Skill

Use this specialized skill when reviewing changes involving authentication, authorization, filters, tenant contexts, tokens, secret management, or application configuration.

## Mandatory Audit Invariants (Rule 14 & Rule 10)

1. **Multi-Tenant Cache Key Isolation**:
   - Cache keys for tenant-scoped data MUST explicitly include the tenant ID prefix:
     ```java
     // DO
     @Cacheable(value = "user_permissions", key = "#tenantId + ':' + #userId")
     // DON'T
     @Cacheable(value = "user_permissions", key = "#userId") // Data breach across tenants!
     ```

2. **Tenant ID Propagation in Queries & Service Layer**:
   - Every multi-tenant database operation must filter by the current tenant ID extracted from `TenantContext` or authentication principal.
   - Never trust tenant ID from client request body blindly without cross-validating against the authenticated session token.

3. **Zero Entity Leakage in Security Filters**:
   - Authentication filters (e.g. `TokenAuthenticationFilter`) must NOT interact with JPA repositories or entities directly.
   - Must use an `AuthFacade` returning immutable DTO records (`VerifiedTokenDto`).

4. **Zero Hardcoded Secrets & Credentials**:
   - No passwords, API keys, private keys, or tokens hardcoded in code, YAML, properties, or test files.
   - Secrets must use environment variable interpolation (e.g., `${GEMINI_API_KEY}`, `${DB_PASSWORD}`).

5. **Data Privacy (UU PDP No. 27/2022 Compliance)**:
   - Test data and seed data must use synthetic/fictitious names, emails, and phone numbers. Never commit real user personal data.

## Review Output Format for Security
- **Tenant Isolation**: [PASS / VIOLATION] (Explanation)
- **Token & Auth Boundary**: [PASS / VIOLATION] (Explanation)
- **Secret & Privacy Hygiene**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
