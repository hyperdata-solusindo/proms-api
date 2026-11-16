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

## 2. The 16 Golden Rules (Quick Summary)

1. **2-Layer Modular Monolith**: Code resides in `com.hyperdata.proms.module.<domain>.<feature>`.
2. **Inter-Module Client Contract Rule**: Module A calling Module B **MUST** use `client/contract/<Feature>Client.java` (Interface) and `client/dto/`. Never inject foreign `Service` or `Repository`.
3. **No JPA Entity Leakage**: Client Contract and Controller **NEVER** return JPA `@Entity`. Always return Java 21 `record` DTOs.
4. **DTO Immutability**: All DTOs are Java 21 `record`.
5. **No `@Data` on Entities & UUIDv7 Mandatory**: JPA Entities use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor(access = AccessLevel.PROTECTED)`. All UUID-based business entities **MUST** use `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` for sequential B-tree indexing in PostgreSQL (NO `@GeneratedValue(strategy = GenerationType.UUID)`), with `@PrePersist`/`@PreUpdate` for auditing.
6. **Constructor Injection Only**: Use `@RequiredArgsConstructor` on final fields. No `@Autowired` fields.
7. **YAVI Validation**: Use YAVI fluent builder (`Validator<T>`) inside Request records or validator factories.
8. **MapStruct Mapping**: Use `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)` in `mapper/`.
9. **JPA 3-Level Query Standards**:
   - **Level 1 (Derived)**: STRICTLY max 1–2 entity properties (max 1 `And`). STRICTLY FORBIDDEN to include `OrderBy` or soft-delete (`DeletedAtIsNull`) in derived method names. For dynamic sorting in simple queries, pass Spring Data `Sort`/`Pageable`.
   - **Level 2 (Explicit `@Query` JPQL)**: MANDATORY for any query involving `deletedAt IS NULL`, fixed business sorting, >2 filter criteria, or `JOIN FETCH`. MUST use Java Text Blocks `"""..."""` and Ubiquitous Language domain names (e.g. `findActiveColumnsByBoardId`, NOT SQL technical soup).
   - **Level 3 (`JpaSpecificationExecutor`)**: MANDATORY for dynamic multi-filter ERP search tables.
10. **The Golden Stack (Anti-N+1 In-Memory Datatables)**: Datatables and cross-module enrichment **MUST** use the Golden Stack (L1 Cache + Variable-Based `Lookups` 3-step pattern: `extractIds` -> `client call` -> `toMap` + MapStruct assembly). Client batch lookups must accept `Collection<ID> ids`. Repositories must NEVER return raw `Object[]` (use Java 21 Record DTO projections with `SELECT new ...`). Never write cross-module SQL joins or call clients inside loops.
11. **Targeted Inner-Loop Testing**: During development, only run the test for your module (`./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"`).
12. **OpenAPI & Scalar Documentation**: All `@RestController` endpoints must have `@Tag`, `@Operation`, and `@ApiResponses`. Tags must be hierarchically organized 1:1 per `<domain>` via `x-tagGroups` in Scalar UI, and non-business controllers must use `@Hidden`.
13. **Observability & Sensitive Data Masking**: All logs must follow SLF4J standards with zero plain credential leakage. Methods in Service and Client are auto-traced via OTel, and error responses attach an `X-Trace-Id` header.
14. **Security, Multi-Tenancy & Rate Limiting Standards**: Tenant data isolation enforced via PostgreSQL Row Level Security (RLS) with session context (`SET LOCAL app.tenant_id`). Cross-tenant resource queries MUST return HTTP 404 disguise (IDOR defense). Distributed 2-Tier rate limiting (Bucket4j + Redis) enforces HTTP 429 `RATE_LIMIT_EXCEEDED`. Stateful PAT authentication uses SHA-256 and sliding window expiration (`Authorization: Bearer <id>|<plainToken>`). Cache keys for tenant data must include tenant prefix (`#tenantId + ':' + #id`). Zero secret leakage (`.env`), UU PDP compliance (synthetic dummy data only), and mandatory confirmation for destructive commands.
15. **CI/CD, Container & Deployment Standards**: Dedicated Liquibase migration runner before app booting (`LIQUIBASE_ENABLED=false` on backend server for dynamic scaling); Production zero-downtime via Nginx Blue/Green; Staging slots via PR labels (`deploy:staging-*`); Trivy container vulnerability gate exclusively during CD deployment before pushing to GHCR; and mandatory PR template compliance (`.github/pull_request_template.md`).
16. **Zero Magic Strings (Domain Constants & Enums)**: All status, roles, tabs, and categorical state values **MUST** be declared as centralized constants in `constant/<Feature>Status.java` or `constant/<Feature>Role.java` (`public final class` with private constructor) or Enums. Strictly FORBIDDEN to write hardcoded string literals across services, entities, clients, YAVI validators, or JPA criteria specifications.

---

## 3. Directory & Architecture Map

```text
com.hyperdata.proms/
├── ApiApplication.java                         # Main application runner
├── common/                                     # Cross-cutting concerns
│   ├── config/                                 # Central infrastructure config (e.g. CacheConfig)
│   ├── exception/                              # BaseException, GlobalExceptionHandler, custom exceptions
│   ├── observability/                          # OTel tracing, filters, AOP aspects, masking engine
│   ├── response/                               # RestResponse<T>, PageResponse<T>
│   ├── util/                                   # Lookups (Anti-N+1 in-memory engine)
│   └── validation/                             # YaviValidator helper
└── module/
    └── <domain>/                               # e.g., inventory, sales, identity, pm
        ├── client/                             # 🌐 Public Contract for other modules / services
        │   ├── contract/                       # 📜 Public Interface (<Feature>Client.java)
        │   ├── dto/                            # 📦 Public Record DTOs returned by Client
        │   └── impl/                           # 🔒 Hidden implementation (@Component <Feature>ClientImpl)
        └── <feature>/                          # ⚙️ Private domain feature (e.g. board, task, user, auth)
            ├── constant/                       # Domain constants (<Feature>Status, <Feature>Role, <Feature>Tab) & cache names (<Feature>CacheNames)
            ├── controller/                     # REST API Controllers (for web/mobile clients)
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
- [14. Security & Multi-tenancy Standards](docs/rules/14-security-and-multi-tenancy.md)
- [15. CI/CD, Container Hardening & Deployment Standards](docs/rules/15-ci-cd-and-deployment-standards.md)

---

## 5. Daily Development Commands

```bash
# 0. Start every new feature or fix on a dedicated branch from dev (NEVER work directly on main or dev)
git checkout dev && git pull origin dev
git checkout -b feat/<feature-name>   # for new features (e.g. feat/endpoint-boards)
# or: git checkout -b fix/<bug-name>  # for bug fixes (e.g. fix/unauthenticated-security-context)

# 1. Run targeted unit test during task implementation (FAST feedback < 5s)
./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"

# 2. Run architecture guardrail test
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"

# 3. Format code with Spotless
./gradlew spotlessApply

# 4. Check formatting without modifying
./gradlew spotlessCheck

# 5. Full project verification (Definition of Done: Spotless, ArchUnit, JUnit, JaCoCo)
./gradlew check

# 6. Create Pull Request using official repository template
gh pr create --template .github/pull_request_template.md
```

---

## 6. Definition of Done (DoD) Checklist

Before finishing any task, every AI Agent MUST verify:
- [ ] Branch Hygiene: Changes developed in dedicated feature/fix branch (`feat/*`, `fix/*`, `refactor/*`, `chore/*`), NEVER directly on `main` or `dev`.
- [ ] Code is placed in `module.<domain>.<feature>/` or `common/`.
- [ ] DTOs are Java 21 `record`.
- [ ] Entity uses Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, NOT `@Data`, and UUID business entities use `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` with `@PrePersist`/`@PreUpdate` lifecycle auditing.
- [ ] Client public contract uses interface in `client/contract/`, DTO in `client/dto/`, and implementation in `client/impl/`.
- [ ] No JPA `@Entity` returned from Client Contract or Controller.
- [ ] Cross-module calls only use foreign `*Client`, not foreign `*Service` or `*Repository`.
- [ ] Datatables / DTO enrichment across modules uses The Golden Stack with variable-based `Lookups` (`extractIds` -> `client call` -> `toMap`), without cross-module SQL joins or loop queries.
- [ ] Client batch lookup methods accept `Collection<ID> ids` and return `List<Dto>`.
- [ ] Zero dead code: No unused variables, fields, methods, or unused injected dependencies.
- [ ] Validation implemented using YAVI.
- [ ] Zero Magic Strings: All status, roles, tabs, and categories use centralized constants/enums in `constant/` (e.g. `BoardStatus`, `BoardMemberRole`, `BoardTab`), NEVER hardcoded string literals.
- [ ] MapStruct mappers configured with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.IGNORE`.
- [ ] Repositories adhere to 3-Level Query Standards: No derived method contains `OrderBy`, `DeletedAt`, or > 1 `And`; soft-delete and complex queries use Level 2 `@Query` JPQL with Ubiquitous Language names.
- [ ] No repository method returns raw `Object[]` or `List<Object[]>`; aggregations and projections use Java 21 Record DTOs (`SELECT new ...`) or interface projections.
- [ ] Endpoints and DTOs documented with OpenAPI `@Tag`, `@Operation`, and `@Schema` with realistic `example` values`.
- [ ] Observability: No unmasked sensitive data/credentials in logs, and masking unit tests pass.
- [ ] Distributed Tracing: X-Trace-Id header attached and OTel context active.
- [ ] Multi-tenancy & Security: Cross-tenant queries return HTTP 404 disguise (IDOR defense); tenant-scoped cache keys include tenant prefix; rate limiting headers (`X-RateLimit-*`) respected; no blind `kill -9` or unconfirmed destructive commands; no secrets or real PII committed.
- [ ] Unit tests written with BDD `@DisplayName` format (Controller tests use fast `standaloneSetup`, not slow `@WebMvcTest`).
- [ ] Targeted tests pass (`./gradlew test --tests "..."`).
- [ ] Code formatted cleanly with `./gradlew spotlessApply`.
- [ ] Architecture tests pass (`./gradlew test --tests "*ArchitectureTest*"`).
- [ ] Full quality gate passes (`./gradlew check` including JaCoCo coverage threshold).
- [ ] Pull Request (PR) description strictly adheres to `.github/pull_request_template.md` with all DoD checklist items marked.
- [ ] Process & Disk Hygiene: Verified with `git status`/`git diff`, no stale daemon cache, and no leftover background processes on ports.

---

## 7. AI Agent Skills & Brainstorming Protocol

When exploring new features, planning changes, or brainstorming architecture:
- **Mandatory Skill**: Use `project-brainstorming` (`.agents/skills/project-brainstorming/SKILL.md`).
- **Directives-First**: Any proposal, spike, or design MUST strictly adhere to the 16 Golden Rules above, companion rules in `docs/rules/`, and Architecture Decision Records in `docs/adr/` before presenting designs or generating implementation plans.
