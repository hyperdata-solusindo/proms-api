# Design Specification: AI Agent Guidelines & Architecture Directives

- **Date**: 2026-10-30
- **Status**: Ready for Implementation Planning
- **Author**: Antigravity Brainstorming Session
- **Target Repository**: `hyperdata-solusindo/proms-api-java` (`/home/kholifan/html/project-promps/api`)

---

## 1. Executive Summary & Objective

Tujuan dari dokumen spesifikasi ini adalah merancang sistem panduan arsitektur dan konvensi kode (*AI Agent Directive System*) yang menjadi *Single Source of Truth* di dalam repositori `proms-api-java`. Sistem ini dirancang agar berbagai model AI (Claude, GPT, Gemini, DeepSeek) maupun tool agentic AI (T3 Code, Cursor, Claude Code, GitHub Copilot, Windsurf) selalu menghasilkan kode yang seragam, deterministik, dan terhindar dari *hallucination drift* atau *unauthorized refactoring*.

---

## 2. Struktur Distribusi Panduan (Hub-and-Spoke Architecture)

Untuk menghindari file monolitik yang menyebabkan *instruction drift*, panduan dipecah secara modular:

```text
api/
├── AGENTS.md                                # Master Hub & Entry Point universal
├── CLAUDE.md                                # Symlink ke AGENTS.md
├── .cursorrules                             # Pointer/Symlink ke AGENTS.md
├── .github/
│   └── copilot-instructions.md              # Pointer ke AGENTS.md
└── docs/
    └── rules/
        ├── 01-architecture-and-structure.md # 2-Layer module.<domain>.<feature>, Facade Contract & Impl rules
        ├── 02-coding-standards.md          # Java 21 Records, Lombok Entity, Constructor Injection
        ├── 03-validation-and-yavi.md        # 6 Pola YAVI & Local Few-Shot Reference
        ├── 04-dto-and-mapstruct.md          # Standar DTO Request/Response, MapStruct & Static Mappers
        ├── 05-repository-and-jpa.md         # 3 Level Query JPA (Derived, @Query JPQL, Specifications)
        ├── 06-api-and-error-handling.md     # Envelope ApiResponse<T>, Global Exception Handler
        ├── 07-database-and-liquibase.md    # Naming convention, Liquibase XML/YAML changelogs
        ├── 08-testing-and-verification.md   # Targeted vs Full testing, BDD @DisplayName
        ├── 09-tooling-and-quality.md        # Konfigurasi Spotless & ArchUnit rules
        └── 10-ai-behavior-and-scope.md      # Scope Lockdown, YAGNI, Conventional Commits, DoD
```

---

## 3. Pilar-Pilar Konsistensi Kode

### Pilar 1: Tech Stack Boundaries
- **Runtime**: Java 21 (Eclipse Temurin 21)
- **Framework**: Spring Boot 4.1.x
- **Build Tool**: Gradle Wrapper (`./gradlew`) dengan Kotlin DSL (`build.gradle.kts`)
- **Database**: PostgreSQL 16 (Port 5432/5443)
- **Migration Engine**: Liquibase (Containerized Init-Container & Gradle CLI)
- **Aturan**: Dilarang menggunakan API usang (misal `java.util.Date`, raw loops menggantikan Stream, format lama).

### Pilar 2: 2-Layer Modular Monolith & Strict Facade Contract Pattern (Pendekatan A)
- Struktur paket di bawah `com.hyperdata.proms`:
  - `common/`: Komponen lintas modul (`config`, `exception`, `response`, `util`, `validation`).
  - `module.<domain>.<feature>/`: Setiap domain bisnis memiliki sub-fitur dengan struktur seragam:
    ```text
    com.hyperdata.proms.module.<domain>.<feature>/
    ├── controller/                       # REST API HTTP (Public Web)
    ├── facade/                           # 👈 Public Contract untuk modul lain
    │   ├── <Feature>Facade.java          # Public Interface (Contract resmi yang di-expose)
    │   ├── dto/                          # Public DTOs (Record) milik Facade
    │   │   └── <Feature>Dto.java
    │   └── internal/                     # 👈 Implementation tersembunyi (Package-Private)
    │       └── Default<Feature>Facade.java # @Component implementation pendelegasi ke service
    ├── service/                          # Internal business logic (Private to feature)
    ├── repository/                       # Internal Spring Data JPA Repository
    ├── entity/                           # Internal JPA Entities
    ├── mapper/                           # MapStruct interfaces (*Mapper.java)
    └── dto/                              # Internal Request & Response DTOs
    ```
- **Layer Boundary & Inter-Module Rules**:
  1. **Strict Interface Expose**: Modul luar **HANYA** boleh meng-import dan berinteraksi dengan interface `<Feature>Facade` dan Record DTO di `facade/dto/`.
  2. **Hidden Implementation**: Modul luar **DILARANG KERAS** meng-import `facade/internal/Default<Feature>Facade` maupun `service/` / `repository/` modul lain.
  3. **No Entity Leakage**: Facade **DILARANG** mengembalikan JPA `@Entity`. Seluruh data yang melintasi batas modul wajib berupa Java 21 `record` DTO.
  4. Controller **HANYA** boleh memanggil Service fitur bersangkutan (atau Facade jika relevan). Dilarang meng-inject Repository di Controller.
  5. Controller **DILARANG** mengembalikan JPA `@Entity` secara langsung. Wajib mapping ke DTO Record.
  6. Transaksi database (`@Transactional`) **HANYA** boleh berada di Service layer.

### Pilar 3: Coding Standards & Idioms
- **Dependency Injection**:
  - Wajib **Constructor Injection** via Lombok `@RequiredArgsConstructor` pada class final fields.
  - **DILARANG KERAS** menggunakan Field Injection (`@Autowired private ...`).
- **JPA Entity**:
  - Menggunakan class Java biasa dengan Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor(access = AccessLevel.PROTECTED)`.
  - **DILARANG KERAS** menggunakan `@Data` pada JPA Entity (mencegah recursion loop & memory leak lazy loading).

### Pilar 4: DTOs & Mapping (MapStruct Standards)
- **Format DTO**: Seluruh DTO Wajib menggunakan **Java 21 `record`**.
- **Sufiks Baku**:
  - `*Request`: Input HTTP Controller (contoh: `CreateProductRequest`).
  - `*Response`: Output HTTP Controller (contoh: `ProductDetailResponse`, `ProductListItemResponse`).
  - `*Dto`: Output antar modul via Facade (contoh: `ProductSummaryDto`).
- **Pemisahan Response**: Pisahkan antara `*DetailResponse` (komprehensif) dan `*ListItemResponse` (ringan/paginasi).
- **MapStruct Convention**:
  - File mapper ditempatkan di `module.<domain>.<feature>.mapper.<Feature>Mapper.java`.
  - Anotasi wajib:
    ```java
    @Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
    )
    public interface ProductMapper {
        Product toEntity(CreateProductRequest request);
        ProductDetailResponse toDetailResponse(Product product);
        ProductListItemResponse toListItemResponse(Product product);
        ProductSummaryDto toSummaryDto(Product product);
    }
    ```
  - Untuk record sederhana (1–3 field), diperbolehkan menggunakan static factory `Record.fromEntity(entity)`.

### Pilar 5: JPA Repository Standards (3-Level Rules)
- **Level 1: Query Sederhana (1–2 parameter)**:
  - Gunakan **Derived Query Methods** bawaan Spring Data: `findByEmail(String email)`, `existsBySku(String sku)`.
- **Level 2: Query Sedang / JOIN (> 2 parameter atau butuh JOIN)**:
  - Dilarang membuat nama derived method panjang.
  - Gunakan **Explicit `@Query` JPQL** dengan text block multi-line yang mudah dibaca dan diberi nama bisnis jelas.
- **Level 3: Filter ERP Dinamis**:
  - Wajib mewarisi `JpaSpecificationExecutor<T>`.
  - Menggunakan `Specification<T>` yang dapat digabungkan secara modular untuk endpoint pagination/search.

### Pilar 6: Request Validation dengan YAVI (Local Few-Shot Reference)
- Menggunakan library **YAVI (`com.github.toshimaki:yavi`)**.
- Validasi didefinisikan sebagai `public static final Validator<T> validator` di dalam record DTO itu sendiri atau validator factory.
- **6 Pola Validasi yang Wajib Disediakan di Reference**:
  1. Dasar (notBlank, email, regex, range angka).
  2. Nullable/Opsional dengan kondisi.
  3. Bersarang (`.nest()`) dan Collection (`.forEach()`).
  4. Kondisional Antar Field (Cross-Field logic).
  5. Password Confirmation equality check.
  6. Eksekusi standar di Controller melempar `ValidationException` terpusat.

### Pilar 7: API Contract & Error Envelope
- Seluruh endpoint REST wajib mengembalikan response seragam:
  ```json
  {
    "success": true,
    "status": 200,
    "message": "Resource retrieved successfully",
    "data": { ... },
    "errors": null,
    "timestamp": "2026-10-30T10:00:00Z"
  }
  ```
- Dilarang melempar generic exception (`RuntimeException`). Gunakan Custom Exceptions di `common/exception`.

### Pilar 8: Database & Liquibase Rules
- Tabel dan kolom database menggunakan penamaan `snake_case`.
- JPA `hibernate.ddl-auto` disetel ke `validate` atau `none`. Dilarang menggunakan `update` atau `create-drop`.
- Seluruh perubahan skema wajib melalui Liquibase changelog di `src/main/resources/db/changelog/`.
- Setiap tabel wajib memiliki kolom audit: `created_at`, `updated_at`, `created_by`, `updated_by`, dan soft-delete jika relevan.

### Pilar 9: Testing Standards & Human Readability
- **Siklus Pengujian (Inner Loop vs Outer Loop)**:
  - *Saat iterasi fitur (AI)*: **HANYA** jalankan test modul yang dikerjakan (`./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"`). Feedback cepat (< 5 detik).
  - *Saat Definition of Done (DoD)*: Jalankan `./gradlew check` untuk verifikasi regresi penuh, ArchUnit, dan Spotless.
- **Keterbacaan Test (Developer-Friendly)**:
  - Menggunakan JUnit 5 dengan anotasi `@DisplayName` bertutur manusiawi (BDD: *Given - When - Then*).
- **Hirarki Test**:
  - `ServiceTest`: Unit test murni menggunakan Mockito (tanpa `@SpringBootTest`).
  - `ControllerTest`: Slice test menggunakan `@WebMvcTest(FeatureController.class)`.

### Pilar 10: AI Behavior & Scope Rules (Guardrails Sikap AI)
1. **Scope Lockdown**: Dilarang memformat ulang, mengubah nama method, atau mengedit logika file di luar modul tugas tanpa persetujuan eksplisit.
2. **YAGNI (Pragmatic Architecture)**: Di dalam internal service, buat concrete class service secara langsung tanpa interface jika hanya ada 1 implementasi.
3. **Dependency Freeze**: Dilarang menambahkan dependensi baru di `build.gradle.kts` tanpa instruksi developer.
4. **Conventional Commits**: Format commit wajib `<type>(<scope>): <description>`.
5. **Self-Correction Verification Loop**: Sebelum menyatakan selesai, AI wajib menjalankan verifikasi build/test lokal dan memperbaiki kesalahan secara mandiri.

---

## 4. Jaring Pengaman ArchUnit untuk Pendekatan A

Aturan ArchUnit yang wajib ditambahkan di `ArchitectureTest.java`:
1. **Facade Must Be Interface**:
   ```java
   classes()
       .that().resideInAPackage("com.hyperdata.proms.module.(*).(*).facade")
       .should().beInterfaces();
   ```
2. **Facade Implementation Must Reside in `internal` and Annotated with `@Component`**:
   ```java
   classes()
       .that().resideInAPackage("..facade.internal..")
       .should().beAnnotatedWith(Component.class);
   ```
3. **Inter-Module Boundary Lockdown**:
   ```java
   classes()
       .that().resideInAPackage("com.hyperdata.proms.module.(*).(*)..")
       .should().onlyDependOnClassesThat()
       .resideInAnyPackage(
           "com.hyperdata.proms.common..",
           "com.hyperdata.proms.module.${1}.${2}..",
           "com.hyperdata.proms.module.(*).(*).facade",
           "com.hyperdata.proms.module.(*).(*).facade.dto..",
           "java..",
           "org.springframework..",
           "com.github.toshimaki.yavi.."
       );
   ```
4. **No JPA Entity Leakage**:
   ```java
   methods()
       .that().areDeclaredInClassesThat().resideInAPackage("..facade..")
       .should().notHaveRawReturnType(resideInAPackage("..entity.."));
   ```

---

## 5. Rencana Penambahan Dependensi & Tooling

Pada tahap implementasi, dependensi berikut akan ditambahkan ke `build.gradle.kts`:
1. **Lombok**:
   - `compileOnly("org.projectlombok:lombok")`
   - `annotationProcessor("org.projectlombok:lombok")`
2. **MapStruct & Lombok Binding**:
   - `implementation("org.mapstruct:mapstruct:1.6.3")`
   - `annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")`
   - `annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")`
3. **YAVI Validation**:
   - `implementation("com.github.toshimaki:yavi:0.14.1")`
4. **ArchUnit (JUnit 5)**:
   - `testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")`
5. **Spotless Gradle Plugin**:
   - `id("com.diffplug.spotless") version "7.0.2"`

---

## 6. Definition of Done (DoD) untuk Setiap Task AI

Setiap AI Agent yang menyelesaikan pekerjaan di repositori ini harus memenuhi checklist berikut:
- [ ] File baru berada di modul yang sesuai (`com.hyperdata.proms.module.<domain>.<feature>/...` atau `common/...`).
- [ ] Kontrak publik inter-modul menggunakan Interface di `facade/` dan implementasi di `facade/internal/`.
- [ ] DTO menggunakan Java 21 `record` dengan sufiks yang benar (`*Request`, `*Response`, `*Dto`).
- [ ] Pemetaan Entity/DTO menggunakan MapStruct dengan `@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)`.
- [ ] Query repository mematuhi aturan 3 level (tidak ada derived method dengan >2 parameter atau JOIN).
- [ ] Validasi DTO mengimplementasikan validator YAVI dengan format standar.
- [ ] Akses lintas modul hanya melalui `<Feature>Facade`, bukan Service/Repository langsung.
- [ ] Service layer menangani business logic, tidak melempar `RuntimeException` generik.
- [ ] Unit test service dibuat dengan BDD `@DisplayName` dan lolos via `./gradlew test --tests "..."`.
- [ ] File diformat rapi sesuai Spotless (`./gradlew spotlessApply` atau `spotlessCheck`).
- [ ] ArchUnit test lolos (`./gradlew test --tests "*ArchitectureTest*"`).
