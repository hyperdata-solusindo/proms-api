---
name: modular-facade-boundary
description: Audits modular monolith package structure, inter-module boundaries, Facade contracts, Constructor Injection, and ArchUnit compliance.
---

# Modular Facade & Boundary Review Skill

Use this specialized skill when reviewing changes involving services, facades, inter-module interactions, dependency injection, or architecture test suites.

## Mandatory Audit Invariants (Rule 1, 2, 6, & 8)

1. **Facade Contract Boundary**:
   - Each domain module must encapsulate its internal logic.
   - Other modules can ONLY import:
     - The public Facade interface in `module.<domain>.<feature>.facade.<Feature>Facade.java`
     - The public DTO records in `module.<domain>.<feature>.facade.dto.*`
   - **STRICTLY PROHIBITED**: Direct imports of internal `service/`, `repository/`, `entity/`, or `constant/` from outside their owning module.

2. **Facade Implementation Placement**:
   - Implementation classes must be located in `facade.internal/Default<Feature>Facade.java` and annotated with `@Component`.
   - Never expose internal implementation details or entities through the Facade interface.

3. **Dependency Injection**:
   - MUST use Constructor Injection exclusively.
   - Use Lombok `@RequiredArgsConstructor` with `private final` fields.
   - `@Autowired` on fields is strictly prohibited and caught by ArchUnit.

4. **ArchUnit & Test Gates**:
   - Architecture test (`ArchitectureTest.java`) must remain green and enforceable.
   - Unit tests must follow the BDD naming pattern with `@DisplayName("Given ..., When ..., Then ...")`.

## Review Output Format for Modular Boundary
- **Module Boundary & Facade**: [PASS / VIOLATION] (Explanation)
- **Dependency Injection**: [PASS / VIOLATION] (Explanation)
- **Architecture Test Integrity**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
