# AGENTS.md — Master Directive for AI Agents & Developers

> **CRITICAL DIRECTIVE**: You are an expert software engineer operating within `hyperdata-solusindo/proms-api-java`. Every AI Agent, LLM (Claude, GPT, Gemini, DeepSeek), and developer MUST follow this document and its companion rules in `docs/rules/`. Never guess or deviate from these rules.

---

## 1. System Identity & Core Tech Stack

- **Runtime**: Java 21 (Eclipse Temurin 21)
- **Framework**: Spring Boot 4.1.x
- **Build Tool**: Gradle Wrapper (`./gradlew`) with Kotlin DSL (`build.gradle.kts`)
- **Database**: PostgreSQL 16 (Port 5432/5443)
- **Migration**: Liquibase (Containerized & CLI changelogs)
- **Root Package**: `com.hyperdata.proms`

---

## 2. The 13 Golden Rules (Quick Summary)

1. **2-Layer Modular Monolith**: Code resides in `com.hyperdata.proms.module.<domain>.<feature>`.
2. **Inter-Module Facade Rule**: Module A calling Module B **MUST** use `facade/<Feature>Facade.java` (Interface). Never inject foreign `Service` or `Repository`.
3. **No JPA Entity Leakage**: Facade and Controller **NEVER** return JPA `@Entity`. Always return Java 21 `record` DTOs.
4. **DTO Immutability**: All DTOs are Java 21 `record`.
5. **No `@Data` on Entities**: JPA Entities use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor(access = AccessLevel.PROTECTED)`.
6. **Constructor Injection Only**: Use `@RequiredArgsConstructor` on final fields. No `@Autowired` fields.
7. **YAVI Validation**: Use YAVI fluent builder (`Validator<T>`) inside Request records or validator factories.
8. **MapStruct Mapping**: Use `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)` in `mapper/`.
9. **JPA 3-Level Query**: Derived (1-2 params), explicit `@Query` JPQL (>2 params/JOIN), or `JpaSpecificationExecutor` (dynamic filters).
10. **The Golden Stack (Anti-N+1 In-Memory Datatables)**: Datatables and cross-module enrichment **MUST** use the Golden Stack (L1 Cache + `Lookups` batch ID extraction & indexing + MapStruct assembly). Never write cross-module SQL joins or call facades inside loops.
11. **Targeted Inner-Loop Testing**: During development, only run the test for your module (`./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"`).
12. **OpenAPI & Scalar Documentation**: All `@RestController` endpoints must have `@Tag`, `@Operation`, explicit `produces/consumes`, and `@ApiResponses`. All Request/Response `record` DTOs must have `@Schema` with descriptions and realistic `example` values.
13. **Observability & Sensitive Data Masking**: All logs must follow SLF4J standards with zero plain credential leakage. Methods in Service and Facade are auto-traced via OTel, and error responses attach an `X-Trace-Id` header.

---

## 3. Directory & Architecture Map

```text
com.hyperdata.proms/
├── ApiApplication.java                         # Main application runner
├── common/                                     # Cross-cutting concerns
│   ├── exception/                              # BaseException, GlobalExceptionHandler, custom exceptions
│   ├── observability/                          # OTel tracing, filters, AOP aspects, masking engine
│   ├── response/                               # RestResponse<T>, PageResponse<T>
│   ├── util/                                   # Lookups (Anti-N+1 in-memory engine)
│   └── validation/                             # YaviValidator helper
└── module/
    └── <domain>/                               # e.g., inventory, sales, identity
        └── <feature>/                          # e.g., product, order, auth
            ├── controller/                     # REST API Controllers (for web/mobile clients)
            ├── facade/                         # 👉 Public Contract for other modules
            │   ├── <Feature>Facade.java        # Public Interface (ONLY contract exposed)
            │   ├── dto/                        # Public Record DTOs returned by Facade
            │   └── internal/                   # Hidden implementation (@Component Default<Feature>Facade)
            ├── service/                        # Internal business logic (Private to feature)
            ├── repository/                     # Spring Data JPA Repository + Specs
            ├── entity/                         # JPA Entities
            ├── mapper/                         # MapStruct Mapper interfaces
            └── dto/                            # Internal Request/Response Record DTOs
```

---

## 4. Companion Detailed Rules (`docs/rules/`)

Read the specific rule file before implementing features in that domain:

- [01. Architecture & 2-Layer Modular Monolith](docs/rules/01-architecture-and-structure.md)
- [02. Coding Standards & Idioms (Records, Lombok, DI)](docs/rules/02-coding-standards.md)
- [03. Validation with YAVI & 6 Few-Shot Patterns](docs/rules/03-validation-and-yavi.md)
- [04. DTOs & MapStruct Standards](docs/rules/04-dto-and-mapstruct.md)
- [05. Repository & JPA 3-Level Query Standards](docs/rules/05-repository-and-jpa.md)
- [06. API Contract, Envelopes & Error Handling](docs/rules/06-api-and-error-handling.md)
- [07. Database & Liquibase Rules](docs/rules/07-database-and-liquibase.md)
- [08. Testing Standards & BDD @DisplayName](docs/rules/08-testing-and-verification.md)
- [09. Automated Quality Gates (ArchUnit & Spotless)](docs/rules/09-tooling-and-quality.md)
- [10. AI Behavior, Scope Lockdown & Definition of Done](docs/rules/10-ai-behavior-and-scope.md)
- [11. The Golden Stack Pattern (Lookups, Anti-N+1, In-Memory Enrichment)](docs/rules/11-the-golden-stack.md)
- [12. OpenAPI & Scalar API Documentation Standards](docs/rules/12-openapi-and-scalar-documentation.md)
- [13. Observability, Logging & Sensitive Data Masking Standards](docs/rules/13-observability-and-logging.md)

---

## 5. Daily Development Commands

```bash
# 1. Run targeted unit test during task implementation (FAST feedback < 5s)
./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"

# 2. Run architecture guardrail test
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"

# 3. Format code with Spotless
./gradlew spotlessApply

# 4. Check formatting without modifying
./gradlew spotlessCheck

# 5. Full project verification (Definition of Done)
./gradlew check
```

---

## 6. Definition of Done (DoD) Checklist

Before finishing any task, every AI Agent MUST verify:
- [ ] Code is placed in `module.<domain>.<feature>/` or `common/`.
- [ ] DTOs are Java 21 `record`.
- [ ] Entity uses Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, and NOT `@Data`.
- [ ] Facade public contract uses interface in `facade/` and implementation in `facade/internal/`.
- [ ] No JPA `@Entity` returned from Facade or Controller.
- [ ] Cross-module calls only use foreign `*Facade`, not foreign `*Service` or `*Repository`.
- [ ] Datatables / DTO enrichment across modules uses The Golden Stack (`Lookups` utility) without cross-module SQL joins or loop queries.
- [ ] Zero dead code: No unused variables, fields, methods, or unused injected dependencies.
- [ ] Validation implemented using YAVI.
- [ ] MapStruct mappers configured with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.IGNORE`.
- [ ] Endpoints and DTOs documented with OpenAPI `@Tag`, `@Operation`, and `@Schema` with realistic `example` values.
- [ ] Observability: No unmasked sensitive data/credentials in logs, and masking unit tests pass.
- [ ] Distributed Tracing: X-Trace-Id header attached and OTel context active.
- [ ] Unit tests written with BDD `@DisplayName` format.
- [ ] Targeted tests pass (`./gradlew test --tests "..."`).
- [ ] Code formatted cleanly with `./gradlew spotlessApply`.
- [ ] Architecture tests pass (`./gradlew test --tests "*ArchitectureTest*"`).
- [ ] Process & Disk Hygiene: Verified with `git status`/`git diff`, no stale daemon cache, and no leftover background processes on ports.

---

## 7. AI Agent Skills & Brainstorming Protocol

When exploring new features, planning changes, or brainstorming architecture:
- **Mandatory Skill**: Use `project-brainstorming` (`.agents/skills/project-brainstorming/SKILL.md`).
- **Directives-First**: Any proposal, spike, or design MUST strictly adhere to the 13 Golden Rules above and companion rules in `docs/rules/` before presenting designs or generating implementation plans.
