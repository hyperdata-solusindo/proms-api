---
name: api-contract-and-yavi
description: Audits controllers, DTOs, request/response models, YAVI validators, and standardized RestResponse contract.
---

# API Contract & YAVI Review Skill

Use this specialized skill when reviewing changes involving web controllers, DTOs, request/response payloads, and input validation.

## Mandatory Audit Invariants (Rule 3, 4, 6, & 7)

1. **DTO Immutability (Java 21 Records)**:
   - ALL Request and Response DTOs MUST be defined as Java 21 `record`.
   - POJO classes with `@Data` or mutable setters are STRICTLY PROHIBITED for DTOs.
   - Field names must be camelCase, serialized to snake_case JSON where applicable.

2. **YAVI Validation Convention**:
   - Validation must be implemented using YAVI.
   - The validator constant MUST follow the standard convention:
     ```java
     // DO
     public static final Validator<LoginRequest> validator = ValidatorBuilder.<LoginRequest>of()
         .constraint(LoginRequest::username, "username", c -> c.notBlank().message("Username wajib diisi"))
         .build();
     ```
   - Do NOT use uppercase `VALIDATOR` or non-standard instance methods.

3. **Standard API Response (`RestResponse<T>`)**:
   - Controller endpoints MUST return `ResponseEntity<RestResponse<T>>`.
   - `RestResponse<T>` schema contains:
     - `statusCode`: HTTP status code integer (e.g. 200, 201)
     - `message`: User-friendly status message
     - `data`: Response payload DTO record
     - `traceId`: Distributed tracing identifier

4. **Lean Controller**:
   - Controllers must not contain business logic, database queries, or `@Transactional` annotations.
   - Only responsible for request mapping, YAVI validation triggering, and delegating to services.

## Review Output Format for API Contract & YAVI
- **DTO Immutability**: [PASS / VIOLATION] (Explanation)
- **YAVI Validation**: [PASS / VIOLATION] (Explanation)
- **API Response Contract**: [PASS / VIOLATION] (Explanation)
- **Actionable Remediation**: Provide exact DO vs DON'T code snippets for any violation.
