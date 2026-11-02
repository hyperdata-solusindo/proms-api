# AI Agent Guidelines & Architecture Directives Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fondasi arsitektur, tooling verifikasi otomatis (Spotless & ArchUnit), core library (Lombok, MapStruct, YAVI), referensi modul nyata, serta dokumen panduan multi-agent (`AGENTS.md`, symlink tools, dan `docs/rules/*.md`) agar seluruh AI model menghasilkan kode yang 100% konsisten.

**Architecture:** 2-Layer Modular Monolith (`com.hyperdata.proms.module.<domain>.<feature>`) dengan strict public facade contract di `facade/` dan implementasi di `facade/internal/`. Dilengkapi Global API Response Envelope, YAVI validation, MapStruct compile-time mapping, JPA 3-level repository query, dan automated ArchUnit testing gates.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Gradle Kotlin DSL (`build.gradle.kts`), PostgreSQL 16, Liquibase, Lombok, MapStruct 1.6.3, YAVI 0.14.1, ArchUnit 1.3.0, Spotless 7.0.2.

**Spec:** [`docs/superpowers/specs/2026-10-30-ai-agent-guidelines-and-architecture-design.md`](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-10-30-ai-agent-guidelines-and-architecture-design.md)

## Global Constraints
- Target package root: `com.hyperdata.proms`
- Java 21 language level & toolchain
- DTO wajib Java 21 `record`
- JPA Entity dilarang menggunakan `@Data` (wajib `@Getter`, `@Setter`, `@NoArgsConstructor`)
- Komunikasi lintas modul WAJIB melalui interface di `facade/`, dilarang meng-inject Service/Repository modul lain
- Facade dilarang mengembalikan JPA `@Entity` (wajib Record DTO)
- Dilarang menjalankan auto-commit atau auto-push git

---

### Task 1: Gradle Build Configuration & Tooling Setup

**Files:**
- Modify: `build.gradle.kts:1-50`

**Interfaces:**
- Consumes: Existing Gradle setup
- Produces: Dependencies for Lombok, MapStruct, YAVI, ArchUnit, and Spotless plugin

- [x] **Step 1: Update `build.gradle.kts` with Spotless plugin and required dependencies**
- [x] **Step 2: Verify dependency resolution and Gradle build**
- [x] **Step 3: Verify Spotless task registration**

---

### Task 2: Root Package Standardization & Application Entry Point

**Files:**
- Delete / Move: `src/main/java/com/hyperdata/proms/api/ApiApplication.java`
- Create: `src/main/java/com/hyperdata/proms/ApiApplication.java`
- Delete / Move: `src/test/java/com/hyperdata/proms/api/ApiApplicationTests.java`
- Create: `src/test/java/com/hyperdata/proms/ApiApplicationTests.java`

**Interfaces:**
- Produces: `com.hyperdata.proms.ApiApplication` as root scan parent

- [x] **Step 1: Create `src/main/java/com/hyperdata/proms/ApiApplication.java`**
- [x] **Step 2: Remove old `ApiApplication.java` in subpackage `api`**
- [x] **Step 3: Create `src/test/java/com/hyperdata/proms/ApiApplicationTests.java`**
- [x] **Step 4: Remove old `src/test/java/com/hyperdata/proms/api/ApiApplicationTests.java`**
- [x] **Step 5: Verify build compiles cleanly**

---

### Task 3: Common Foundation: Global Response Envelope & Exceptions

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/response/ApiResponse.java`
- Create: `src/main/java/com/hyperdata/proms/common/response/PageResponse.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/BaseException.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/ResourceNotFoundException.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/DuplicateResourceException.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/BusinessException.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/ValidationException.java`
- Create: `src/main/java/com/hyperdata/proms/common/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/hyperdata/proms/common/response/ApiResponseTest.java`

**Interfaces:**
- Produces: `ApiResponse<T>`, `PageResponse<T>`, standard exceptions, and REST exception handler

- [x] **Step 1: Write Unit Test for ApiResponse in `ApiResponseTest.java`**
- [x] **Step 2: Create `ApiResponse.java` and `PageResponse.java`**
- [x] **Step 3: Create Custom Exceptions Hierarchy**
- [x] **Step 4: Run unit test for ApiResponse**

---

### Task 4: Common Validation Engine: YAVI Helper & Validator Utilities

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/validation/YaviValidator.java`
- Test: `src/test/java/com/hyperdata/proms/common/validation/YaviValidatorTest.java`

**Interfaces:**
- Produces: `YaviValidator.validateAndThrow(Validator<T>, T)` helper utility

- [x] **Step 1: Write Unit Test in `YaviValidatorTest.java`**
- [x] **Step 2: Implement `YaviValidator.java`**
- [x] **Step 3: Run the test**

---

### Task 5: ArchUnit Architecture Guardrail Tests

**Files:**
- Create: `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`

**Interfaces:**
- Produces: Automated architecture compliance checks executed on `./gradlew test`

- [x] **Step 1: Write Architecture Rules in `ArchitectureTest.java`**
- [x] **Step 2: Run ArchUnit Test suite**

---

### Task 6: Reference Implementation: Few-Shot Feature Example (`module.sample.product`)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/entity/Product.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/dto/CreateProductRequest.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/dto/ProductDetailResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/dto/ProductListItemResponse.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/facade/dto/ProductSummaryDto.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/facade/ProductFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/facade/internal/DefaultProductFacade.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/repository/ProductRepository.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/repository/spec/ProductSpecs.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/mapper/ProductMapper.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/service/ProductService.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/controller/ProductController.java`
- Test: `src/test/java/com/hyperdata/proms/module/sample/product/service/ProductServiceTest.java`

**Interfaces:**
- Produces: Concrete working reference feature for AI agents to model after.

- [x] **Step 1: Create Entity in `entity/Product.java`**
- [x] **Step 2: Create DTOs with YAVI Validator in `dto/`**
- [x] **Step 3: Create Facade Interface & DTO in `facade/`**
- [x] **Step 4: Create Repository & Specs in `repository/`**
- [x] **Step 5: Create MapStruct Mapper in `mapper/ProductMapper.java`**
- [x] **Step 6: Implement Service & DefaultFacade**
- [x] **Step 7: Implement REST Controller in `controller/ProductController.java`**
- [x] **Step 8: Write BDD Unit Test for ProductService in `ProductServiceTest.java`**
- [x] **Step 9: Run tests for sample product module and ArchUnit**

---

### Task 7: Comprehensive Documentation Suite (`AGENTS.md` & `docs/rules/*.md`)

**Files:**
- Create: `AGENTS.md`
- Create: `CLAUDE.md` (Symlink or pointer)
- Create: `.cursorrules` (Pointer)
- Create: `.github/copilot-instructions.md` (Pointer)
- Create: `docs/rules/01-architecture-and-structure.md`
- Create: `docs/rules/02-coding-standards.md`
- Create: `docs/rules/03-validation-and-yavi.md`
- Create: `docs/rules/04-dto-and-mapstruct.md`
- Create: `docs/rules/05-repository-and-jpa.md`
- Create: `docs/rules/06-api-and-error-handling.md`
- Create: `docs/rules/07-database-and-liquibase.md`
- Create: `docs/rules/08-testing-and-verification.md`
- Create: `docs/rules/09-tooling-and-quality.md`
- Create: `docs/rules/10-ai-behavior-and-scope.md`

**Interfaces:**
- Produces: The complete AI instruction ecosystem and multi-agent pointers.

- [x] **Step 1: Create `AGENTS.md` in repository root**
- [x] **Step 2: Create Multi-Agent Symlinks / Pointers**
- [x] **Step 3: Create detailed rule files in `docs/rules/`**
- [x] **Step 4: Verify all markdown links**

---

### Task 8: Full Verification Loop & Quality Gate

**Files:**
- None (Runs build and checks)

**Interfaces:**
- Verifies: Spotless code formatting and entire test suite including ArchUnit

- [x] **Step 1: Run Spotless Apply to format all Java source files**
- [x] **Step 2: Run Spotless Check**
- [x] **Step 3: Run Full Test Suite**
- [x] **Step 4: Verify Git Status**
