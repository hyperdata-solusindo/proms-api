# Employee + Tenant Sample Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a reference full-CRUD master employee feature under `module.sample.employee` that enriches tenant data through `TenantFacade` using The Golden Stack.

**Architecture:** A new feature package mirroring `module.sample.product`, with `Employee` storing `tenantId` as a UUID (no cross-module JPA association). Controllers implement a contract interface, services own transactions and caching, and cross-module tenant data is resolved in one batch call per listing.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data JPA, YAVI 0.14.1, MapStruct 1.6.3, Lombok, Mockito, JUnit 5, Gradle Kotlin DSL.

**Spec:** `docs/superpowers/specs/2026-11-05-employee-tenant-sample-design.md`

## Global Constraints

- Package root: `com.hyperdata.proms.module.sample.employee`.
- Every DTO is a Java 21 `record`; controllers and facades never return JPA entities.
- Cross-module access is limited to `TenantFacade` and `TenantSummaryDto`. Never import `Tenant`, `TenantRepository`, or `TenantService`.
- Never use `@ManyToOne`/`@OneToMany` to `Tenant`; store `tenantId` as `UUID`.
- No cross-module SQL joins and no facade calls inside loops.
- No Liquibase migration in this plan; the feature is a compile-and-unit-test sample.
- YAVI validators live on Request records and are executed in the service.
- MapStruct: `componentModel = MappingConstants.ComponentModel.SPRING`, `unmappedTargetPolicy = ReportingPolicy.IGNORE`.
- No comments in generated code. No unused imports, fields, or methods.
- Commit steps run only when the human partner has authorized commits; otherwise leave changes uncommitted.
- Formatting gate: `./gradlew spotlessApply`.
- Architecture gate: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`.

## Review Focus

1. Duplicate `employeeNumber` or `email` on create/update must return `409 CONFLICT`, not `500`.
2. Unknown `tenantId` on create/update must return `404` and must not persist the employee.
3. Blank or oversized required fields must return `422`; a null `phone` must be accepted.
4. A listed employee whose tenant is missing must still render with `tenantName` null, without NPE.
5. An empty employee page must return an empty `PageResponse` without calling the tenant facade.

## File Structure

All files are new.

```text
src/main/java/com/hyperdata/proms/module/sample/employee/
├── constant/EmployeeCacheNames.java
├── controller/EmployeeApi.java
├── controller/EmployeeController.java
├── dto/CreateEmployeeRequest.java
├── dto/UpdateEmployeeRequest.java
├── dto/EmployeeFilter.java
├── dto/EmployeeDetailResponse.java
├── dto/EmployeeListItemResponse.java
├── entity/Employee.java
├── entity/EmployeeStatus.java
├── mapper/EmployeeMapper.java
├── repository/EmployeeRepository.java
├── repository/spec/EmployeeSpecs.java
├── service/EmployeeService.java
├── facade/EmployeeFacade.java
├── facade/dto/EmployeeSummaryDto.java
└── facade/internal/DefaultEmployeeFacade.java

src/test/java/com/hyperdata/proms/module/sample/employee/
├── dto/EmployeeRequestValidationTest.java
├── repository/spec/EmployeeSpecsTest.java
├── mapper/EmployeeMapperTest.java
├── facade/internal/DefaultEmployeeFacadeTest.java
├── service/EmployeeServiceTest.java
├── service/EmployeeServiceCacheIntegrationTest.java
└── controller/EmployeeControllerTest.java
```

## Shared Values

Use these examples in every `@Schema(example = "...")` unless stated otherwise.

| Value | Example |
| :--- | :--- |
| Employee ID | `9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d` |
| Tenant ID | `b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22` |
| Employee number | `EMP-0001` |
| Full name | `Budi Santoso` |
| Email | `budi.santoso@hyperdata.co.id` |
| Phone | `+6281234567890` |
| Department | `Engineering` |
| Position | `Backend Engineer` |
| Join date | `2024-01-15` |
| Status | `ACTIVE` |
| Created at | `2026-11-05T08:30:00Z` |
| Tenant name | `PT Hyperdata Solusindo` |
| Tenant slug | `hyperdata-solusindo` |

---

### Task 1: Domain Types, DTOs, and YAVI Validators

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/entity/EmployeeStatus.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/entity/Employee.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/constant/EmployeeCacheNames.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/dto/CreateEmployeeRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/dto/UpdateEmployeeRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/dto/EmployeeFilter.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/dto/EmployeeDetailResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/dto/EmployeeListItemResponse.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/dto/EmployeeRequestValidationTest.java`

**Interfaces:**
- Produces: `EmployeeStatus` constants `ACTIVE`, `INACTIVE`, `ON_LEAVE`.
- Produces: `Employee` Lombok getters/setters and builder with the fields from the spec.
- Produces: `EmployeeCacheNames.EMPLOYEE_DETAILS` = `"employee-details"`.
- Produces: `CreateEmployeeRequest.validator`, `UpdateEmployeeRequest.validator`.
- Produces: record components consumed by later tasks:

```java
record CreateEmployeeRequest(UUID tenantId, String employeeNumber, String fullName, String email,
    String phone, String department, String position, LocalDate joinDate, EmployeeStatus status)

record UpdateEmployeeRequest(UUID tenantId, String employeeNumber, String fullName, String email,
    String phone, String department, String position, EmployeeStatus status)

record EmployeeFilter(String name, String employeeNumber, String department, UUID tenantId,
    EmployeeStatus status)

record EmployeeDetailResponse(UUID id, String employeeNumber, String fullName, String email,
    String phone, String department, String position, LocalDate joinDate, EmployeeStatus status,
    UUID tenantId, String tenantName, String tenantSlug, Instant createdAt)

record EmployeeListItemResponse(UUID id, String employeeNumber, String fullName, String department,
    String position, EmployeeStatus status, UUID tenantId, String tenantName)
```

- [ ] **Step 1: Write the failing validation test**

Create `EmployeeRequestValidationTest` with these cases:

```java
package com.hyperdata.proms.module.sample.employee.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hyperdata.proms.module.sample.employee.entity.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmployeeRequestValidationTest {

    private static CreateEmployeeRequest validCreate() {
        return new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("Given a valid create request with null phone, when validated, then it is valid")
    void shouldAcceptValidCreateRequestWithNullPhone() {
        assertTrue(CreateEmployeeRequest.validator.validate(validCreate()).isValid());
    }

    @Test
    @DisplayName("Given a create request without tenant, when validated, then it is invalid")
    void shouldRejectMissingTenant() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            null, "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given a create request with invalid email, when validated, then it is invalid")
    void shouldRejectInvalidEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "not-an-email",
            null, "Engineering", "Backend Engineer", LocalDate.parse("2024-01-15"),
            EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given a create request without join date, when validated, then it is invalid")
    void shouldRejectMissingJoinDate() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", null, EmployeeStatus.ACTIVE);
        assertFalse(CreateEmployeeRequest.validator.validate(request).isValid());
    }

    @Test
    @DisplayName("Given an update request without status, when validated, then it is invalid")
    void shouldRejectUpdateWithoutStatus() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
            UUID.randomUUID(), "EMP-0001", "Budi Santoso", "budi.santoso@hyperdata.co.id",
            null, "Engineering", "Backend Engineer", null);
        assertFalse(UpdateEmployeeRequest.validator.validate(request).isValid());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.dto.EmployeeRequestValidationTest"`
Expected: FAIL to compile because the employee types do not exist.

- [ ] **Step 3: Implement the domain types**

`EmployeeStatus`:

```java
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE,
    ON_LEAVE
}
```

`Employee` must use `@Entity`, `@Table(name = "employees")`, `@Getter`, `@Setter`,
`@NoArgsConstructor`, `@AllArgsConstructor(access = AccessLevel.PROTECTED)`, `@Builder`.
Fields and columns:

```java
@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
@Column(name = "tenant_id", nullable = false) private UUID tenantId;
@Column(name = "employee_number", nullable = false, unique = true, length = 32) private String employeeNumber;
@Column(name = "full_name", nullable = false, length = 128) private String fullName;
@Column(nullable = false, unique = true, length = 128) private String email;
@Column(length = 32) private String phone;
@Column(nullable = false, length = 64) private String department;
@Column(nullable = false, length = 64) private String position;
@Column(name = "join_date", nullable = false) private LocalDate joinDate;
@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private EmployeeStatus status;
@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
```

Add a `@PrePersist protected void onCreate()` that sets `createdAt = Instant.now()`.

`EmployeeCacheNames`:

```java
public final class EmployeeCacheNames {

    private EmployeeCacheNames() {
    }

    public static final String EMPLOYEE_DETAILS = "employee-details";
}
```

Implement the five records exactly as their component lists above. Annotate each record
class with `@Schema(description = "...")` and every component with
`@Schema(description = "...", example = "...", requiredMode = Schema.RequiredMode.REQUIRED)`
where required. Optional components (`phone`, `tenantName`, `tenantSlug`) omit
`requiredMode`. Use the Shared Values table for examples.

`CreateEmployeeRequest.validator`:

```java
public static final Validator<CreateEmployeeRequest> validator =
    ValidatorBuilder.<CreateEmployeeRequest>of()
        .constraint(CreateEmployeeRequest::tenantId, "tenantId",
            c -> c.notNull().message("Tenant is required"))
        .constraint(CreateEmployeeRequest::employeeNumber, "employeeNumber",
            c -> c.notBlank().message("Employee number is required")
                .lessThanOrEqual(32).message("Employee number max 32 chars"))
        .constraint(CreateEmployeeRequest::fullName, "fullName",
            c -> c.notBlank().message("Full name is required")
                .lessThanOrEqual(128).message("Full name max 128 chars"))
        .constraint(CreateEmployeeRequest::email, "email",
            c -> c.notBlank().message("Email is required")
                .email().message("Email format is invalid")
                .lessThanOrEqual(128).message("Email max 128 chars"))
        .constraint(CreateEmployeeRequest::phone, "phone",
            c -> c.lessThanOrEqual(32).message("Phone max 32 chars"))
        .constraint(CreateEmployeeRequest::department, "department",
            c -> c.notBlank().message("Department is required")
                .lessThanOrEqual(64).message("Department max 64 chars"))
        .constraint(CreateEmployeeRequest::position, "position",
            c -> c.notBlank().message("Position is required")
                .lessThanOrEqual(64).message("Position max 64 chars"))
        .constraint(CreateEmployeeRequest::joinDate, "joinDate",
            c -> c.notNull().message("Join date is required"))
        .constraint(CreateEmployeeRequest::status, "status",
            c -> c.notNull().message("Status is required"))
        .build();
```

`UpdateEmployeeRequest.validator` uses the same constraints except `joinDate`; it also
includes `tenantId`, `employeeNumber`, `fullName`, `email`, `phone`, `department`,
`position`, and `status`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.dto.EmployeeRequestValidationTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hyperdata/proms/module/sample/employee src/test/java/com/hyperdata/proms/module/sample/employee/dto
git commit -m "feat(employee): add domain types, DTOs, and YAVI validators"
```

---

### Task 2: Repository and Specifications

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/repository/EmployeeRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/repository/spec/EmployeeSpecs.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/repository/spec/EmployeeSpecsTest.java`

**Interfaces:**
- Consumes: `Employee`, `EmployeeFilter`, `EmployeeStatus` from Task 1.
- Produces:

```java
public interface EmployeeRepository
        extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmail(String email);
    boolean existsByEmployeeNumberAndIdNot(String employeeNumber, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
```

```java
public final class EmployeeSpecs {
    public static Specification<Employee> of(EmployeeFilter filter);
    public static Specification<Employee> fullNameContains(String name);
    public static Specification<Employee> employeeNumberEquals(String employeeNumber);
    public static Specification<Employee> departmentContains(String department);
    public static Specification<Employee> hasTenantId(UUID tenantId);
    public static Specification<Employee> hasStatus(EmployeeStatus status);
}
```

- [ ] **Step 1: Write the failing specification test**

Create `EmployeeSpecsTest`:

```java
package com.hyperdata.proms.module.sample.employee.repository.spec;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hyperdata.proms.module.sample.employee.dto.EmployeeFilter;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.entity.EmployeeStatus;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class EmployeeSpecsTest {

    @Test
    @DisplayName("Given null filter, when EmployeeSpecs.of, then return a non-null specification")
    void shouldReturnSpecificationForNullFilter() {
        assertNotNull(EmployeeSpecs.of(null));
    }

    @Test
    @DisplayName("Given all filter fields, when EmployeeSpecs.of, then compose every criterion")
    void shouldComposeAllFilterCriteria() {
        EmployeeFilter filter = new EmployeeFilter(
            "Budi", "EMP-0001", "Engineering", UUID.randomUUID(), EmployeeStatus.ACTIVE);
        Specification<Employee> spec = EmployeeSpecs.of(filter);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Given a status-only filter, when EmployeeSpecs.of, then compose the status criterion")
    void shouldComposeStatusOnlyFilter() {
        Specification<Employee> spec = EmployeeSpecs.of(
            new EmployeeFilter(null, null, null, null, EmployeeStatus.ON_LEAVE));
        assertNotNull(spec);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.repository.spec.EmployeeSpecsTest"`
Expected: FAIL to compile because `EmployeeSpecs` does not exist.

- [ ] **Step 3: Implement repository and specifications**

`EmployeeSpecs.of` starts from `(root, query, cb) -> cb.conjunction()` and returns it
immediately when `filter == null`. Otherwise compose with `spec.and(...)` in this order:
`fullNameContains`, `employeeNumberEquals`, `departmentContains`, `hasTenantId`,
`hasStatus`. Each helper is a one-line lambda:

- `fullNameContains`: `cb.like(cb.lower(root.get("fullName")), "%" + name.toLowerCase() + "%")`
- `employeeNumberEquals`: `cb.equal(cb.lower(root.get("employeeNumber")), value.toLowerCase())`
- `departmentContains`: `cb.like(cb.lower(root.get("department")), "%" + value.toLowerCase() + "%")`
- `hasTenantId`: `cb.equal(root.get("tenantId"), tenantId)`
- `hasStatus`: `cb.equal(root.get("status"), status)`

Blank strings and nulls are skipped by `of`. The class is final with a private
constructor.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.repository.spec.EmployeeSpecsTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hyperdata/proms/module/sample/employee/repository src/test/java/com/hyperdata/proms/module/sample/employee/repository
git commit -m "feat(employee): add repository and dynamic specifications"
```

---

### Task 3: Mapper and Facade

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/mapper/EmployeeMapper.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/facade/dto/EmployeeSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/facade/EmployeeFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/facade/internal/DefaultEmployeeFacade.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/mapper/EmployeeMapperTest.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/facade/internal/DefaultEmployeeFacadeTest.java`

**Interfaces:**
- Consumes: `Employee`, DTOs from Task 1; `EmployeeRepository` from Task 2;
  `TenantSummaryDto` from `com.hyperdata.proms.module.platform.tenant.facade.dto`.
- Produces:

```java
public interface EmployeeFacade {
    Optional<EmployeeSummaryDto> findEmployeeSummaryById(UUID id);
    List<EmployeeSummaryDto> findAllByIds(Collection<UUID> ids);
}

record EmployeeSummaryDto(UUID id, String employeeNumber, String fullName, String department,
    EmployeeStatus status, UUID tenantId)
```

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);

    @Mapping(target = "tenantName", source = "tenant.name")
    @Mapping(target = "tenantSlug", source = "tenant.slug")
    EmployeeDetailResponse toDetailResponse(Employee employee, TenantSummaryDto tenant);

    @Mapping(target = "tenantName", source = "tenant.name")
    EmployeeListItemResponse toListItemResponse(Employee employee, TenantSummaryDto tenant);

    EmployeeSummaryDto toSummaryDto(Employee employee);
}
```

- [ ] **Step 1: Write the failing mapper test**

Create `EmployeeMapperTest`:

```java
package com.hyperdata.proms.module.sample.employee.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.hyperdata.proms.module.platform.tenant.facade.dto.TenantSummaryDto;
import com.hyperdata.proms.module.sample.employee.dto.EmployeeDetailResponse;
import com.hyperdata.proms.module.sample.employee.entity.Employee;
import com.hyperdata.proms.module.sample.employee.entity.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EmployeeMapperTest {

    private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

    private Employee employee(UUID tenantId) {
        return Employee.builder()
            .id(UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"))
            .tenantId(tenantId)
            .employeeNumber("EMP-0001")
            .fullName("Budi Santoso")
            .email("budi.santoso@hyperdata.co.id")
            .phone("+6281234567890")
            .department("Engineering")
            .position("Backend Engineer")
            .joinDate(LocalDate.parse("2024-01-15"))
            .status(EmployeeStatus.ACTIVE)
            .build();
    }

    @Test
    @DisplayName("Given tenant summary, when toDetailResponse, then tenant fields are populated")
    void shouldMapTenantIntoDetail() {
        UUID tenantId = UUID.randomUUID();
        TenantSummaryDto tenant = new TenantSummaryDto(
            tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

        EmployeeDetailResponse response = mapper.toDetailResponse(employee(tenantId), tenant);

        assertEquals("PT Hyperdata Solusindo", response.tenantName());
        assertEquals("hyperdata-solusindo", response.tenantSlug());
    }

    @Test
    @DisplayName("Given null tenant, when toDetailResponse, then tenant fields are null")
    void shouldMapNullTenantSafely() {
        EmployeeDetailResponse response = mapper.toDetailResponse(employee(UUID.randomUUID()), null);

        assertNull(response.tenantName());
        assertNull(response.tenantSlug());
    }
}
```

- [ ] **Step 2: Write the failing facade test**

Create `DefaultEmployeeFacadeTest` with `@Mock EmployeeRepository`,
`@Mock EmployeeMapper`, `@InjectMocks DefaultEmployeeFacade`, and these cases:

- `findEmployeeSummaryById` returns the mapped summary when the repository finds one.
- `findEmployeeSummaryById` returns `Optional.empty()` when not found.
- `findAllByIds` maps two entities and returns two summaries.
- `findAllByIds` returns an empty list for `null` and empty input and never calls the
  repository.

Use this skeleton for `DefaultEmployeeFacadeTest`:

```java
@ExtendWith(MockitoExtension.class)
class DefaultEmployeeFacadeTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private DefaultEmployeeFacade employeeFacade;

    @Test
    @DisplayName("Given existing ID, when findEmployeeSummaryById, then return the summary")
    void shouldReturnSummaryWhenFound() {
        UUID id = UUID.randomUUID();
        Employee employee = Employee.builder().id(id).employeeNumber("EMP-0001")
            .fullName("Budi Santoso").department("Engineering")
            .status(EmployeeStatus.ACTIVE).tenantId(UUID.randomUUID()).build();
        EmployeeSummaryDto summary = new EmployeeSummaryDto(
            id, "EMP-0001", "Budi Santoso", "Engineering", EmployeeStatus.ACTIVE,
            employee.getTenantId());

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));
        when(employeeMapper.toSummaryDto(employee)).thenReturn(summary);

        Optional<EmployeeSummaryDto> result = employeeFacade.findEmployeeSummaryById(id);

        assertTrue(result.isPresent());
        assertEquals("Budi Santoso", result.get().fullName());
    }

    @Test
    @DisplayName("Given unknown ID, when findEmployeeSummaryById, then return empty")
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());
        assertTrue(employeeFacade.findEmployeeSummaryById(id).isEmpty());
    }

    @Test
    @DisplayName("Given batch IDs, when findAllByIds, then map every entity")
    void shouldReturnSummariesForBatchIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Employee first = Employee.builder().id(id1).employeeNumber("EMP-0001")
            .fullName("Budi Santoso").build();
        Employee second = Employee.builder().id(id2).employeeNumber("EMP-0002")
            .fullName("Siti Aminah").build();
        EmployeeSummaryDto firstDto = new EmployeeSummaryDto(
            id1, "EMP-0001", "Budi Santoso", "Engineering", EmployeeStatus.ACTIVE, null);
        EmployeeSummaryDto secondDto = new EmployeeSummaryDto(
            id2, "EMP-0002", "Siti Aminah", "Finance", EmployeeStatus.ACTIVE, null);

        when(employeeRepository.findAllById(Set.of(id1, id2))).thenReturn(List.of(first, second));
        when(employeeMapper.toSummaryDto(first)).thenReturn(firstDto);
        when(employeeMapper.toSummaryDto(second)).thenReturn(secondDto);

        List<EmployeeSummaryDto> result = employeeFacade.findAllByIds(Set.of(id1, id2));

        assertEquals(2, result.size());
        assertEquals("Budi Santoso", result.get(0).fullName());
        assertEquals("Siti Aminah", result.get(1).fullName());
    }

    @Test
    @DisplayName("Given null or empty IDs, when findAllByIds, then return empty without querying")
    void shouldReturnEmptyListWhenIdsNullOrEmpty() {
        assertTrue(employeeFacade.findAllByIds(null).isEmpty());
        assertTrue(employeeFacade.findAllByIds(Collections.emptyList()).isEmpty());
        verify(employeeRepository, never()).findAllById(any());
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapperTest" --tests "com.hyperdata.proms.module.sample.employee.facade.internal.DefaultEmployeeFacadeTest"`
Expected: FAIL to compile because the mapper, facade, and summary DTO do not exist.

- [ ] **Step 4: Implement the mapper, summary DTO, and facade**

`EmployeeSummaryDto` is a record with `@Schema` on the class and on each component,
using the Shared Values table.

`DefaultEmployeeFacade` uses `@Component`, `@RequiredArgsConstructor`, and
`@Transactional(readOnly = true)`. `findAllByIds` returns `Collections.emptyList()` for
null or empty input and otherwise returns
`Lookups.map(employeeRepository.findAllById(ids), employeeMapper::toSummaryDto)`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.mapper.EmployeeMapperTest" --tests "com.hyperdata.proms.module.sample.employee.facade.internal.DefaultEmployeeFacadeTest"`
Expected: PASS (6 tests total).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/hyperdata/proms/module/sample/employee/mapper src/main/java/com/hyperdata/proms/module/sample/employee/facade src/test/java/com/hyperdata/proms/module/sample/employee/mapper src/test/java/com/hyperdata/proms/module/sample/employee/facade
git commit -m "feat(employee): add mapper and public facade"
```

---

### Task 4: Employee Service with Tenant Enrichment

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/service/EmployeeService.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/service/EmployeeServiceTest.java`

**Interfaces:**
- Consumes: `EmployeeRepository`, `EmployeeSpecs`, `EmployeeMapper`, `EmployeeFacade`
  types, `EmployeeCacheNames`, and `TenantFacade`.
- Produces:

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    @Transactional
    public EmployeeDetailResponse createEmployee(CreateEmployeeRequest request);

    @Cacheable(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeById(UUID id);

    @Transactional(readOnly = true)
    public PageResponse<EmployeeListItemResponse> getEmployees(EmployeeFilter filter, Pageable pageable);

    @CacheEvict(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional
    public EmployeeDetailResponse updateEmployee(UUID id, UpdateEmployeeRequest request);

    @CacheEvict(cacheNames = EmployeeCacheNames.EMPLOYEE_DETAILS, key = "#id")
    @Transactional
    public void deleteEmployee(UUID id);
}
```

- [ ] **Step 1: Write the failing service test**

Create `EmployeeServiceTest` with `@ExtendWith(MockitoExtension.class)` and mocks for
`EmployeeRepository`, `EmployeeMapper`, `TenantFacade`. Include these cases:

- `shouldCreateEmployeeSuccessfully`: valid request, tenant found, duplicate checks
  false, `employeeMapper.toEntity(request)` returns the entity, save returns the entity,
  and `employeeMapper.toDetailResponse(saved, tenant)` returns the detail.
- `shouldThrowWhenEmployeeNumberExists`: `existsByEmployeeNumber` true →
  `DuplicateResourceException`, repository never saves.
- `shouldThrowWhenEmailExists`: `existsByEmail` true → `DuplicateResourceException`,
  repository never saves.
- `shouldThrowWhenTenantNotFoundOnCreate`: tenant returns `Optional.empty()` →
  `ResourceNotFoundException`, repository never saves.
- `shouldReturnDetailWithTenantWhenFound`: repository finds the employee; tenant summary
  present; detail contains tenant name.
- `shouldThrowWhenEmployeeNotFound`: repository returns empty →
  `ResourceNotFoundException`.
- `shouldUpdateEmployeeSuccessfully`: update finds the employee, tenant found, duplicate
  checks excluding the same ID false, save returns the entity.
- `shouldThrowWhenEmployeeNumberExistsOnUpdate`:
  `existsByEmployeeNumberAndIdNot` true → `DuplicateResourceException`.
- `shouldThrowWhenEmailExistsOnUpdate`: `existsByEmailAndIdNot` true →
  `DuplicateResourceException`.
- `shouldReturnListWithNullTenantWhenTenantMissing`: the tenant batch returns an empty
  list → `tenantName` is null and no exception is thrown.
- `shouldDeleteEmployee`: `existsById` true → `deleteById` called.
- `shouldThrowWhenDeleteNotFound`: `existsById` false → `ResourceNotFoundException`.

The anti-N+1 test is required:

```java
@Test
@DisplayName("Given a non-empty page, when getEmployees, then tenant batch is called exactly once")
void shouldCallTenantBatchOnceForList() {
    UUID tenantId = UUID.randomUUID();
    Employee employee = Employee.builder()
        .id(UUID.randomUUID())
        .tenantId(tenantId)
        .employeeNumber("EMP-0001")
        .fullName("Budi Santoso")
        .email("budi.santoso@hyperdata.co.id")
        .department("Engineering")
        .position("Backend Engineer")
        .joinDate(LocalDate.parse("2024-01-15"))
        .status(EmployeeStatus.ACTIVE)
        .build();
    Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 20), 1);
    TenantSummaryDto tenant = new TenantSummaryDto(
        tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");
    EmployeeListItemResponse item = new EmployeeListItemResponse(
        employee.getId(), "EMP-0001", "Budi Santoso", "Engineering",
        "Backend Engineer", EmployeeStatus.ACTIVE, tenantId, "PT Hyperdata Solusindo");

    when(employeeRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);
    when(tenantFacade.getTenantSummaries(Set.of(tenantId))).thenReturn(List.of(tenant));
    when(employeeMapper.toListItemResponse(employee, tenant)).thenReturn(item);

    PageResponse<EmployeeListItemResponse> result =
        employeeService.getEmployees(new EmployeeFilter(null, null, null, null, null),
            PageRequest.of(0, 20));

    assertEquals(1, result.items().size());
    assertEquals("PT Hyperdata Solusindo", result.items().get(0).tenantName());
    verify(tenantFacade, times(1)).getTenantSummaries(Set.of(tenantId));
    verify(tenantFacade, never()).getTenantSummary(any());
}
```

Add the missing-tenant list test:

```java
@Test
@DisplayName("Given a tenant missing from the batch result, when getEmployees, then tenantName is null")
void shouldReturnListWithNullTenantWhenTenantMissing() {
    Employee employee = Employee.builder()
        .id(UUID.randomUUID())
        .tenantId(UUID.randomUUID())
        .employeeNumber("EMP-0001")
        .fullName("Budi Santoso")
        .department("Engineering")
        .position("Backend Engineer")
        .joinDate(LocalDate.parse("2024-01-15"))
        .status(EmployeeStatus.ACTIVE)
        .build();
    Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 20), 1);
    EmployeeListItemResponse item = new EmployeeListItemResponse(
        employee.getId(), "EMP-0001", "Budi Santoso", "Engineering",
        "Backend Engineer", EmployeeStatus.ACTIVE, employee.getTenantId(), null);

    when(employeeRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);
    when(tenantFacade.getTenantSummaries(Set.of(employee.getTenantId())))
        .thenReturn(List.of());
    when(employeeMapper.toListItemResponse(employee, null)).thenReturn(item);

    PageResponse<EmployeeListItemResponse> result = employeeService.getEmployees(
        new EmployeeFilter(null, null, null, null, null), PageRequest.of(0, 20));

    assertNull(result.items().get(0).tenantName());
}
```

Add a test that an empty page returns an empty `PageResponse` and never calls
`tenantFacade.getTenantSummaries`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.service.EmployeeServiceTest"`
Expected: FAIL to compile because `EmployeeService` does not exist.

- [ ] **Step 3: Implement `EmployeeService`**

Field order for the constructor:
`EmployeeRepository employeeRepository`, `EmployeeMapper employeeMapper`,
`TenantFacade tenantFacade`.

Rules:

- `createEmployee`: `YaviValidator.validateAndThrow(CreateEmployeeRequest.validator,
  request)`; `TenantSummaryDto tenant = requireTenant(request.tenantId())`; reject
  duplicates with `DuplicateResourceException` messages
  `"Employee with number [%s] already exists"` and
  `"Employee with email [%s] already exists"`; save; return
  `employeeMapper.toDetailResponse(saved, tenant)`.
- `getEmployeeById`: load or throw `new ResourceNotFoundException("Employee", id)`;
  resolve `tenantFacade.getTenantSummary(employee.getTenantId()).orElse(null)`; map with
  `toDetailResponse(employee, tenant)`.
- `getEmployees`: page via `employeeRepository.findAll(EmployeeSpecs.of(filter),
  pageable)`; return `PageResponse.from(employeePage, List.of())` when empty. Otherwise
  extract tenant IDs with `Lookups.extractIds(employees, Employee::getTenantId)`, load
  once with `tenantFacade.getTenantSummaries(tenantIds)`, index with
  `Lookups.toMap(..., TenantSummaryDto::id)`, and map each employee with
  `employeeMapper.toListItemResponse(employee, tenantMap.get(employee.getTenantId()))`
  through `Lookups.map`.
- `updateEmployee`: validate; load employee; `requireTenant(request.tenantId())`; reject
  duplicates using `existsByEmployeeNumberAndIdNot` and `existsByEmailAndIdNot`; copy
  `tenantId`, `employeeNumber`, `fullName`, `email`, `phone`, `department`, `position`,
  and `status`; save; map with the tenant.
- `deleteEmployee`: throw when `existsById` is false; otherwise `deleteById`.
- `requireTenant(UUID)`: return
  `tenantFacade.getTenantSummary(tenantId).orElseThrow(() ->
  new ResourceNotFoundException("Tenant", tenantId))`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.service.EmployeeServiceTest"`
Expected: PASS (14 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hyperdata/proms/module/sample/employee/service src/test/java/com/hyperdata/proms/module/sample/employee/service/EmployeeServiceTest.java
git commit -m "feat(employee): add CRUD service with tenant enrichment"
```

---

### Task 5: Controller and OpenAPI Contract

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/controller/EmployeeApi.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/employee/controller/EmployeeController.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/employee/controller/EmployeeControllerTest.java`

**Interfaces:**
- Consumes: `EmployeeService` from Task 4.
- Produces: HTTP endpoints under `/api/v1/employees`; `EmployeeController implements
  EmployeeApi`.

- [ ] **Step 1: Write the failing controller test**

Create `EmployeeControllerTest` modeled on `ProductControllerTest`, but initialize the
object mapper with Java time support:

```java
private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
```

Use `MockMvcBuilders.standaloneSetup(employeeController)
.setControllerAdvice(new GlobalExceptionHandler())
.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())`.

Cases:

- `POST /api/v1/employees` returns `201`, `$.statusCode` 201, `$.data.id`,
  `$.data.employeeNumber`, `$.data.tenantName`.
- `GET /api/v1/employees/{id}` returns `200` with `$.data.fullName` and
  `$.data.tenantSlug`.
- `GET /api/v1/employees/{id}` for an unknown ID returns `404` when the service throws
  `ResourceNotFoundException("Employee", id)`.
- `GET /api/v1/employees?name=Budi&page=0&size=20` returns `200`,
  `$.items[0].tenantName`, and `$.totalElements`.
- `PUT /api/v1/employees/{id}` returns `200` with the updated `$.data.fullName`.
- `DELETE /api/v1/employees/{id}` returns `200` with the message
  `"Employee deleted successfully"`.

Build request payloads with `objectMapper.writeValueAsString(...)`; include
`tenantId`, `employeeNumber`, `email`, `department`, `position`, `joinDate`, and
`status`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.controller.EmployeeControllerTest"`
Expected: FAIL to compile because the controller and API interface do not exist.

- [ ] **Step 3: Implement `EmployeeApi` and `EmployeeController`**

`EmployeeApi` is an interface with `@Tag(name = "Employees", description = "Manajemen
data master karyawan")` and `@RequestMapping("/api/v1/employees")`. Declare all routing
and OpenAPI annotations here:

- `createEmployee`: `@PostMapping`, `@ResponseStatus(HttpStatus.CREATED)`,
  `@Operation(summary = "Tambah Karyawan Baru", ...)`, `@ApiResponse(responseCode =
  "201", ...)`.
- `getEmployeeById`: `@GetMapping("/{id}")`, `@Operation(summary = "Ambil Detail
  Karyawan", ...)`, `@ApiResponse` for `200` and `404`; `@Parameter` with the employee
  ID example.
- `getEmployees`: `@GetMapping`, `@Operation(summary = "Daftar Karyawan", ...)`,
  `@ApiResponse(responseCode = "200", ...)`, `@PageableDefault(size = 20)`.
- `updateEmployee`: `@PutMapping("/{id}")`, `200` and `404` responses, employee ID
  `@Parameter`.
- `deleteEmployee`: `@DeleteMapping("/{id}")`, `200` and `404` responses, employee ID
  `@Parameter`.

Return types:

```java
RestResponse<EmployeeDetailResponse> createEmployee(@RequestBody CreateEmployeeRequest request)
RestResponse<EmployeeDetailResponse> getEmployeeById(@PathVariable UUID id)
PageResponse<EmployeeListItemResponse> getEmployees(EmployeeFilter filter, Pageable pageable)
RestResponse<EmployeeDetailResponse> updateEmployee(@PathVariable UUID id, @RequestBody UpdateEmployeeRequest request)
RestResponse<Void> deleteEmployee(@PathVariable UUID id)
```

`EmployeeController` uses `@RestController`, `@RequiredArgsConstructor`, holds
`private final EmployeeService employeeService`, implements `EmployeeApi`, and returns:

- create: `RestResponse.created(response, "Employee created successfully")`
- get: `RestResponse.ok(response, "Employee retrieved successfully")`
- list: `employeeService.getEmployees(filter, pageable)`
- update: `RestResponse.ok(response, "Employee updated successfully")`
- delete: `RestResponse.ok(null, "Employee deleted successfully")`

No OpenAPI annotation may appear in `EmployeeController`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.controller.EmployeeControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/hyperdata/proms/module/sample/employee/controller src/test/java/com/hyperdata/proms/module/sample/employee/controller
git commit -m "feat(employee): add REST controller and OpenAPI contract"
```

---

### Task 6: Cache Integration and Final Verification

**Files:**
- Create: `src/test/java/com/hyperdata/proms/module/sample/employee/service/EmployeeServiceCacheIntegrationTest.java`

**Interfaces:**
- Consumes: `EmployeeService`, `EmployeeRepository`, `EmployeeMapper`, `TenantFacade`,
  `CacheConfig`.

- [ ] **Step 1: Write the cache integration test**

Model it on `ProductServiceCacheIntegrationTest` with this test configuration:

```java
@Configuration
@Import(CacheConfig.class)
static class TestConfig {

    @Bean
    public EmployeeRepository employeeRepository() {
        return mock(EmployeeRepository.class);
    }

    @Bean
    public TenantFacade tenantFacade() {
        return mock(TenantFacade.class);
    }

    @Bean
    public EmployeeMapper employeeMapper() {
        return Mappers.getMapper(EmployeeMapper.class);
    }

    @Bean
    public EmployeeService employeeService(EmployeeRepository employeeRepository,
        EmployeeMapper employeeMapper, TenantFacade tenantFacade) {
        return new EmployeeService(employeeRepository, employeeMapper, tenantFacade);
    }
}
```

Autowire `EmployeeService`, `EmployeeRepository`, and `TenantFacade` in the test class.

Use these two tests:

```java
@Test
@DisplayName("Given an existing employee, when getEmployeeById is called twice, then findById runs once")
void givenExistingEmployee_whenGetCalledTwice_thenRepositoryQueriedOnce() {
    UUID id = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Employee employee = Employee.builder()
        .id(id)
        .tenantId(tenantId)
        .employeeNumber("EMP-CACHE-1")
        .fullName("Cached Employee")
        .email("cached.employee@hyperdata.co.id")
        .department("Engineering")
        .position("Backend Engineer")
        .joinDate(LocalDate.parse("2024-01-15"))
        .status(EmployeeStatus.ACTIVE)
        .build();
    TenantSummaryDto tenant = new TenantSummaryDto(
        tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

    given(employeeRepository.findById(id)).willReturn(Optional.of(employee));
    given(tenantFacade.getTenantSummary(tenantId)).willReturn(Optional.of(tenant));

    EmployeeDetailResponse first = employeeService.getEmployeeById(id);
    EmployeeDetailResponse second = employeeService.getEmployeeById(id);

    assertThat(first.tenantName()).isEqualTo("PT Hyperdata Solusindo");
    assertThat(second.fullName()).isEqualTo("Cached Employee");
    verify(employeeRepository, times(1)).findById(id);
}

@Test
@DisplayName("Given a cached employee, when updateEmployee runs, then the cache is evicted")
void givenCachedEmployee_whenUpdateCalled_thenCacheIsEvicted() {
    UUID id = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Employee employee = Employee.builder()
        .id(id)
        .tenantId(tenantId)
        .employeeNumber("EMP-CACHE-2")
        .fullName("Old Name")
        .email("old.name@hyperdata.co.id")
        .department("Engineering")
        .position("Backend Engineer")
        .joinDate(LocalDate.parse("2024-01-15"))
        .status(EmployeeStatus.ACTIVE)
        .build();
    TenantSummaryDto tenant = new TenantSummaryDto(
        tenantId, "hyperdata-solusindo", "PT Hyperdata Solusindo");

    given(employeeRepository.findById(id)).willReturn(Optional.of(employee));
    given(employeeRepository.save(any(Employee.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(employeeRepository.existsByEmployeeNumberAndIdNot("EMP-CACHE-2", id))
        .willReturn(false);
    given(employeeRepository.existsByEmailAndIdNot("new.name@hyperdata.co.id", id))
        .willReturn(false);
    given(tenantFacade.getTenantSummary(tenantId)).willReturn(Optional.of(tenant));

    employeeService.getEmployeeById(id);
    employeeService.updateEmployee(id, new UpdateEmployeeRequest(
        tenantId, "EMP-CACHE-2", "New Name", "new.name@hyperdata.co.id",
        null, "Engineering", "Senior Backend Engineer", EmployeeStatus.ACTIVE));
    employeeService.getEmployeeById(id);

    verify(employeeRepository, times(3)).findById(id);
}
```

- [ ] **Step 2: Run test to verify it passes**

The cache annotations were implemented in Task 4, so this integration test verifies
them rather than driving new production code.

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.service.EmployeeServiceCacheIntegrationTest"`
Expected: PASS (2 tests).

- [ ] **Step 3: Run the full targeted suite**

Run: `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.*"`
Expected: PASS for all employee tests.

- [ ] **Step 4: Format and run architecture guardrails**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL with formatting applied.

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/hyperdata/proms/module/sample/employee/service/EmployeeServiceCacheIntegrationTest.java
git commit -m "test(employee): add cache integration coverage"
```

---

## Definition of Done

- All seven files in `src/test/java/com/hyperdata/proms/module/sample/employee` pass.
- `./gradlew test --tests "com.hyperdata.proms.module.sample.employee.*"` passes.
- `./gradlew spotlessApply` reports success.
- `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"` passes.
- No entity is returned from controller or facade.
- No foreign repository or service is imported.
- `TenantFacade.getTenantSummaries` is called at most once per list request.
- `git status` shows no unintended files.
