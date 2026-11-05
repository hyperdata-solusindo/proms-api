# Employee + Tenant Sample — Design

- **Date:** 2026-11-05
- **Status:** Approved for planning
- **Scope:** Bounded sample feature (not a production HR domain)

## 1. Goal

Provide a reference CRUD feature for master employee that demonstrates every project
standard end-to-end: contract interface, record DTOs, YAVI validation, MapStruct,
JPA 3-level queries, caching, Facade boundary, OpenAPI annotations, and The Golden
Stack for cross-module tenant enrichment.

## 2. Placement

Package: `com.hyperdata.proms.module.sample.employee`, mirroring the existing
`module.sample.product` reference feature.

No Liquibase migration is included. Like `sample.product`, this feature is a
compile-and-unit-test reference; `spring.jpa.hibernate.ddl-auto=none` means it is not
runnable against a real database without a future changelog.

## 3. Data Model

Entity `Employee` (`employees` table name) stores the tenant as a UUID column, not a
JPA association. Importing `platform.tenant.entity.Tenant` is forbidden.

| Field | Type | Rules |
| :--- | :--- | :--- |
| `id` | `UUID` | Generated |
| `tenantId` | `UUID` | Required, indexed in a future migration |
| `employeeNumber` | `String` | Required, unique, max 32 |
| `fullName` | `String` | Required, max 128 |
| `email` | `String` | Required, unique, valid email, max 128 |
| `phone` | `String` | Optional, max 32 |
| `department` | `String` | Required, max 64 |
| `position` | `String` | Required, max 64 |
| `joinDate` | `LocalDate` | Required, immutable after create |
| `status` | `EmployeeStatus` | Required enum: `ACTIVE`, `INACTIVE`, `ON_LEAVE` |
| `createdAt` | `Instant` | Set on persist |

## 4. API Contract

Base path: `/api/v1/employees`

| Method | Path | Success | Notes |
| :--- | :--- | :--- | :--- |
| `POST` | `/` | `201` | Creates an employee |
| `GET` | `/{id}` | `200` | Detail, includes tenant name/slug |
| `GET` | `/` | `200` | Paginated list with filters |
| `PUT` | `/{id}` | `200` | Updates an employee |
| `DELETE` | `/{id}` | `200` | Deletes an employee |

Filters: `name` (contains on `fullName`), `employeeNumber` (exact), `department`
(contains), `tenantId` (exact), `status` (exact).

All responses use `RestResponse<T>` for single resources and `PageResponse<T>` for
lists. No entity is returned from controller or facade.

## 5. Validation Rules (YAVI)

Create and update validators enforce:

- `tenantId`: not null.
- `employeeNumber`: not blank, max 32.
- `fullName`: not blank, max 128.
- `email`: not blank, valid email, max 128.
- `phone`: optional, max 32 (null is valid).
- `department`: not blank, max 64.
- `position`: not blank, max 64.
- `joinDate`: not null on create only.
- `status`: not null.

## 6. Cross-Module Tenant Enrichment (The Golden Stack)

The employee feature calls only `TenantFacade`:

- Detail: `tenantFacade.getTenantSummary(tenantId)`.
- List: one batch call `tenantFacade.getTenantSummaries(ids)` after
  `Lookups.extractIds`, indexed via `Lookups.toMap`, then assembled with MapStruct.
- Create/update: `tenantFacade.getTenantSummary(tenantId)` must be present, otherwise
  `ResourceNotFoundException("Tenant", tenantId)`.

Forbidden: foreign repository/service injection, cross-module SQL joins, facade calls
inside loops.

## 7. Caching

`EmployeeCacheNames.EMPLOYEE_DETAILS = "employee-details"`:

- `@Cacheable` on `getEmployeeById`.
- `@CacheEvict` on update and delete.
- Tenant data is cached by the tenant module itself.

## 8. Error Handling

- Duplicate `employeeNumber` or `email` → `DuplicateResourceException` (`409`).
- Unknown employee → `ResourceNotFoundException` (`404`).
- Unknown tenant on create/update → `ResourceNotFoundException` (`404`).
- Invalid payload → `ValidationException` (`422`) via `YaviValidator`.

## 9. Testing

- YAVI validator tests for create/update, including null optional phone.
- Specification tests for filter composition.
- Mapper tests for tenant enrichment, including null tenant.
- Facade tests for single and batch lookups.
- Service tests for CRUD, duplicates, unknown tenant, and exactly one batch tenant call
  per list request.
- Controller tests (MockMvc standalone) for all five endpoints.
- Cache integration test for hit and eviction.

## 10. Non-Goals

- No Liquibase changelog or seed data.
- No authentication/authorization changes.
- No UI, export, or bulk import.
- No employee-to-user account linking.
