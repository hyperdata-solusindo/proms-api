# Controller Contract Interface Pattern & Architecture Rules Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Contract Interface Pattern (`<Feature>Api.java`) for Spring Boot controllers, configure global OpenAPI error responses via `OperationCustomizer`, eliminate FQCN and redundant attributes codebase-wide, and update all governance rules (`01`, `02`, `09`, `12`).

**Architecture:** 
1. Move HTTP routing and OpenAPI documentation metadata out of controllers into strongly typed contract interfaces (`<Feature>Api.java`) colocated in each module's `controller/` package.
2. Register automated global OpenAPI error schemas (`400 Bad Request` and `500 Internal Server Error`) via `OperationCustomizer` in `OpenApiConfig` .
3. Enforce the architecture via ArchUnit test guardrails (`ArchitectureTest.java`).
4. Update governance documentation across Rules `01`, `02`, `09`, and `12` with explicit standards (strict no-FQCN, Red Hat Java / Eclipse JDT Formatter, and Contract Interface Pattern).

**Tech Stack:** Spring Boot 3.4.x, Java 21, Springdoc OpenAPI 2.8.x, ArchUnit 1.4.x, Spotless (Eclipse JDT Formatter engine).

**Spec:** [`docs/superpowers/specs/2026-11-03-controller-contract-interface-and-rules-design.md`](docs/superpowers/specs/2026-11-03-controller-contract-interface-and-rules-design.md)

## Global Constraints
- Target Java version: OpenJDK 21 (record DTOs, modern idioms).
- No FQCN anywhere in Java source files (strict explicit imports).
- Formatter line width: 120 characters, 4 spaces indentation, 1 level (+4 spaces) continuation indent.
- No auto-commit / auto-push (`git commit` and `git push` are strictly reserved for the user).
- Controllers must remain clean: inject dependencies, delegate to services, return typed `ApiResponse`.

---

### Task 1: Update Architecture Rules Documentation (`01`, `02`, `09`, `12`)

**Files:**
- Modify: `docs/rules/01-architecture-and-structure.md`
- Modify: `docs/rules/02-coding-standards.md`
- Modify: `docs/rules/09-tooling-and-quality.md`
- Modify: `docs/rules/12-openapi-and-scalar-documentation.md`

**Interfaces:**
- Produces: Updated rule guidelines establishing `<Feature>Api` contract interface pattern, strict no-FQCN rule across all layers, Eclipse JDT formatter sync with Red Hat Java, and global OpenAPI error response handling.

- [x] **Step 1: Update Rule 01 (Architecture & Structure)**
Update `docs/rules/01-architecture-and-structure.md` to specify `<Feature>Api.java` in the module package layout under `controller/` and declare the boundary rule that all REST Controllers must implement their feature's API contract interface.

- [x] **Step 2: Update Rule 02 (Coding Standards)**
Update `docs/rules/02-coding-standards.md` to add Section 6: "Strict Explicit Imports & Strict No-FQCN Rule" prohibiting fully qualified class names across the entire codebase (Entities, DTOs, Services, Controllers, Mappers, Repositories, and Tests) with required standard imports.

- [x] **Step 3: Update Rule 09 (Tooling & Quality)**
Update `docs/rules/09-tooling-and-quality.md` to reflect the migration from Palantir to the Eclipse JDT Formatter Engine (`.vscode/eclipse-java-formatter.xml`), IDE alignment with `redhat.java`, and ArchUnit enforcement for controller contracts.

- [x] **Step 4: Update Rule 12 (OpenAPI & Scalar Documentation)**
Update `docs/rules/12-openapi-and-scalar-documentation.md` to replace the direct-annotated controller pattern with the **Contract Interface Pattern** (`<Feature>Api.java`), document the global `OperationCustomizer` for 400 & 500 error responses, remove redundant `consumes`/`produces` JSON declarations, and enforce collapsed parenthetical syntax (`)))`).

- [x] **Step 5: Verify deliverable**
Review the updated markdown files to ensure all links and formatting are intact.

---

### Task 2: Configure Global Error Responses in `OpenApiConfig.java`

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/common/docs/OpenApiConfig.java`
- Test: Run `./gradlew check` to verify Spring context and compilation.

**Interfaces:**
- Produces: `@Bean public OperationCustomizer globalErrorResponsesCustomizer()` that automatically attaches standard 400 (YAVI validation failure envelope) and 500 (Internal server error) schemas to every registered endpoint.

- [x] **Step 1: Implement `OperationCustomizer` Bean**
In `OpenApiConfig.java`, add the `OperationCustomizer` bean:
```java
    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            var apiResponseSchema = new Schema<ApiResponse<?>>()
                .description("Envelope standar respon API");

            if (!responses.containsKey("400")) {
                responses.addApiResponse(
                    "400",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Format request tidak valid / validasi YAVI gagal")
                        .content(new Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            if (!responses.containsKey("500")) {
                responses.addApiResponse(
                    "500",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Kesalahan internal server")
                        .content(new Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            return operation;
        };
    }
```

- [x] **Step 2: Ensure clean imports**
Verify that all Swagger/OpenAPI models (`Content`, `MediaType`, `Schema`, `ApiResponses`, `OperationCustomizer`) are cleanly imported at the top of `OpenApiConfig.java` without FQCN.

- [x] **Step 3: Run compilation and test**
Run `./gradlew compileJava test` to ensure Spring Boot starts and the configuration compiles cleanly.

---

### Task 3: Update ArchUnit Guardrails in `ArchitectureTest.java`

**Files:**
- Modify: `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`

**Interfaces:**
- Produces: ArchUnit rules allowing interfaces inside `..controller..` and verifying that `@RestController` classes implement an API contract interface.

- [x] **Step 1: Update existing controller rule**
Modify `controllers_must_be_annotated_with_rest_controller` to exclude interfaces:
```java
    @ArchTest
    static final ArchRule controllers_must_be_annotated_with_rest_controller = classes().that()
        .resideInAPackage("..controller..")
        .and().areNotInterfaces()
        .should()
        .beAnnotatedWith(RestController.class);
```

- [x] **Step 2: Add contract implementation rule**
Add ArchUnit rule ensuring all `@RestController` classes implement at least one interface (their `<Feature>Api` contract), excluding internal/infrastructure controllers:
```java
    @ArchTest
    static final ArchRule controllers_must_implement_api_interface = classes().that()
        .resideInAPackage("com.hyperdata.proms.module..controller..")
        .and().areNotInterfaces()
        .should()
        .implement(resideInAPackage("com.hyperdata.proms.module..controller.."));
```

- [x] **Step 3: Run ArchUnit tests to verify**
Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
(Expected: Passes once controllers implement the interfaces in Task 4).

---

### Task 4: Implement Contract Interface Pattern Across Existing Controllers

**Files:**
- Create: `src/main/java/com/hyperdata/proms/module/identity/auth/controller/AuthApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/identity/auth/controller/AuthController.java`
- Create: `src/main/java/com/hyperdata/proms/module/identity/user/controller/UserApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/identity/user/controller/UserController.java`
- Create: `src/main/java/com/hyperdata/proms/module/sample/product/controller/ProductApi.java`
- Modify: `src/main/java/com/hyperdata/proms/module/sample/product/controller/ProductController.java`

**Interfaces:**
- `AuthApi`:
  - `ApiResponse<LoginResponse> login(LoginRequest request)`
  - `ApiResponse<Void> logout()`
- `UserApi`:
  - `ApiResponse<MeResponse> getMe()`
- `ProductApi`:
  - `ApiResponse<ProductDetailResponse> createProduct(CreateProductRequest request)`
  - `ApiResponse<ProductDetailResponse> getProductById(UUID id)`
  - `PageResponse<ProductListItemResponse> getProducts(ProductFilter filter, Pageable pageable)`
  - `ApiResponse<ProductDetailResponse> updateProduct(UUID id, UpdateProductRequest request)`
  - `ApiResponse<Void> deleteProduct(UUID id)`

- [x] **Step 1: Create `AuthApi.java` and refactor `AuthController.java`**
Extract all `@Tag`, `@Operation`, `@ApiResponses` (status 200, 401), `@SecurityRequirements`, and `@PostMapping("/auth/login")` into `AuthApi.java`.
Make `AuthController` implement `AuthApi` with clean, annotation-free business methods. Clean all FQCN to imported `ApiResponse`.

- [x] **Step 2: Create `UserApi.java` and refactor `UserController.java`**
Extract `@Tag("User Profile")`, `@Operation`, and `@GetMapping("/me")` into `UserApi.java`.
Make `UserController` implement `UserApi`.

- [x] **Step 3: Create `ProductApi.java` and refactor `ProductController.java`**
Extract `@Tag("Products")`, `@RequestMapping("/api/v1/products")`, and all CRUD operation mappings into `ProductApi.java`.
Make `ProductController` implement `ProductApi`.

- [x] **Step 4: Format with Spotless**
Run: `./gradlew spotlessApply`
Verify that Red Hat Java formatting profile formats all new interfaces and controllers cleanly.

- [x] **Step 5: Run complete test suite and ArchUnit**
Run: `./gradlew check`
Expected: `BUILD SUCCESSFUL` with all ArchUnit checks passing.

---

### Task 5: Final Review & Verification

**Files:**
- Review all touched files in git status.

- [x] **Step 1: Check git diff**
Run `git status -s` and inspect changes to ensure no unintended modifications or leftover dead code.

- [x] **Step 2: Run Spotless & Quality Check**
Run: `./gradlew spotlessCheck && ./gradlew test`
Expected: 100% compliant and passing.
