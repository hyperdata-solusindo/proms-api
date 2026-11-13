---
name: modular-facade-boundary
description: Audits modular monolith package structure, inter-module boundaries, Client contracts, Constructor Injection, and ArchUnit compliance.
---

# Modular Client Contract & Boundary Review Skill

Use this specialized skill when reviewing changes involving services, clients, inter-module interactions, dependency injection, or architecture test suites.

## Mandatory Audit Invariants (Rule 1, 2, 6, & 8)

1. **Client Contract Boundary**:
   - Each domain module must encapsulate its internal logic.
   - Other modules/domains can ONLY import:
     - The public Client interface in `module.<domain>.client.contract.<Feature>Client.java`
     - The public DTO records in `module.<domain>.client.dto.*`
   - **STRICTLY PROHIBITED**: Direct imports of internal `client/impl/`, `service/`, `repository/`, `entity/`, or `constant/` from outside their owning domain.

2. **Client Implementation Placement**:
   - Implementation classes must be located in `module.<domain>.client.impl.<Feature>ClientImpl.java` and annotated with `@Component`.
   - Never expose internal implementation details or entities through the Client contract interface.

3. **Dependency Injection**:
   - MUST use Constructor Injection exclusively.
   - Use Lombok `@RequiredArgsConstructor` with `private final` fields.
   - `@Autowired` on fields is strictly prohibited and caught by ArchUnit.

4. **ArchUnit & Test Gates**:
   - Architecture test (`ArchitectureTest.java`) must remain green and enforceable.
   - Unit tests must follow the BDD naming pattern with `@DisplayName("Given ..., When ..., Then ...")`.

## Review Output Format for Modular Boundary
- **Module Boundary & Client Contract**: [PASS / VIOLATION] (Explanation)
- **Dependency Injection**: [PASS / VIOLATION] (Explanation)
- **Architecture Test Integrity**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
