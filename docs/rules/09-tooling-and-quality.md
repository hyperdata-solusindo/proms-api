# Rule 09: Automated Quality Gates (ArchUnit, Spotless & IDE Formatting)

## 1. Spotless Java Formatter (Eclipse JDT Formatter Engine)
Spotless dikonfigurasikan menggunakan **Eclipse JDT Formatter Engine** dengan profil standar internal `PromsJavaFormat` ([`.vscode/eclipse-java-formatter.xml`](../../.vscode/eclipse-java-formatter.xml)). Engine ini menjamin integrasi 100% konsisten dengan language server **Red Hat Java** (`redhat.java`) di VS Code, Cursor, Antigravity, dan CI pipeline.

Karakteristik formatting standar:
- **Line Width**: Maksimum 120 karakter.
- **Indentation**: 4 spasi (tab policy: spaces only).
- **Continuation Indent**: 1 level (+4 spasi).
- **Braces**: Egyptian Braces / 1TBS (`end_of_line`).
- **Imports**: Diurutkan secara alfabetis tanpa wildcard (`.*`), dilarang menggunakan FQCN di dalam tubuh kode.

Commands:
- Apply formatting: `./gradlew spotlessApply`
- Check compliance: `./gradlew spotlessCheck`

Always run `./gradlew spotlessApply` before completing any task.

## 2. Multi-Layer Zero-Conflict Formatting Strategy

Untuk menjamin konsistensi 100% di semua IDE (VS Code, Cursor, Antigravity, IntelliJ) dan seluruh model AI:
1. **Lapis Universal ([`.editorconfig`](../../.editorconfig))**:
   - Indentasi: 4 spasi (Java/Kotlin), 2 spasi (JSON/YAML/Markdown).
   - Line Endings: Wajib `LF` (`\n`), trim trailing whitespace, insert final newline.
2. **Lapis IDE / Formatter di VS Code ([`.vscode/settings.json`](../../.vscode/settings.json))**:
   - **Java Formatter Engine**: `redhat.java` menggunakan profil [`.vscode/eclipse-java-formatter.xml`](../../.vscode/eclipse-java-formatter.xml) (`PromsJavaFormat`).
   - **Language Support & Import Management (Red Hat Java)**: `redhat.java` menangani IntelliSense, navigasi, dan import otomatis. `java.completion.importOrder` diset `["#", ""]` agar import tersusun dalam 1 blok alfabetis tanpa baris kosong pemisah package, serta `starThreshold: 99` untuk melarang wildcard (`.*`).
   - **Spotless Integration**: Ekstensi `richardwillis.vscode-spotless-gradle` memastikan build pipeline dan IDE berjalan selaras.
3. **Lapis Prettier ([`.prettierrc.json`](../../.prettierrc.json))**:
   - Formatter default untuk JSON, YAML, dan Markdown dengan `tabWidth: 2` dan `singleQuote: true`.
4. **Lapis Git Pre-commit Hook ([`.githooks/pre-commit`](../../.githooks/pre-commit))**:
   - Terpasang otomatis via `./gradlew installGitHooks` (terkait ke `./gradlew check`).
   - Menjalankan Spotless secara otomatis sebelum setiap git commit.

## 3. Red Hat Java / Eclipse JDT Compiler Diagnostics ([`.vscode/org.eclipse.jdt.core.prefs`](../../.vscode/org.eclipse.jdt.core.prefs))

Untuk mencegah peringatan palsu (*false positive warnings*) dari compiler internal Eclipse JDT yang tidak relevan dengan standar javac / Gradle:
- Konfigurasi compiler diimpor secara otomatis melalui `"java.settings.url": ".vscode/org.eclipse.jdt.core.prefs"` di [`.vscode/settings.json`](../../.vscode/settings.json).
- **Type Safety (`Java(16777748)`)**: Diabaikan (`org.eclipse.jdt.core.compiler.problem.uncheckedTypeOperation=ignore`) saat konversi unchecked type expression (misalnya `Specification` generic konversi ke `Specification<T>`).
- **Null Type Safety (`Java(67109822)`)**: Diabaikan (`org.eclipse.jdt.core.compiler.problem.nullUncheckedConversion=ignore` dan `"java.compile.nullAnalysis.mode": "disabled"`) pada parameter method reference/descriptor (misalnya `BiFunction.apply` atau stream mapping).

## 4. ArchUnit Architecture Guardrails
Located in `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`.

ArchUnit strictly enforces:
1. Controllers do not depend on Repositories.
2. Facades in `module.*.*.facade` must be Java interfaces.
3. Facade implementations must reside in `*.facade.internal.*` dan carry `@Component`.
4. Facade methods must not return JPA `@Entity` types.
5. Controllers (excluding interfaces) must be annotated with `@RestController`.
6. Controllers must implement their `<Feature>Api` contract interface.
7. Services must be annotated with `@Service`.
8. Inter-Module Boundary: Modul luar (`module` lain maupun `common`) dilarang mengakses package internal modul (`service`, `repository`, `entity`, `mapper`, `constant`, `controller`, internal `dto`, dan `facade.internal`). Komunikasi lintas modul HANYA boleh melalui `facade` dan `facade.dto`.
9. Field Injection Prohibition: Dilarang menggunakan field injection `@Autowired`.
10. Entity Lombok Prohibition: JPA Entities dilarang menggunakan `@Data` (juga diblokir pada level compiler via `lombok.config`).
11. Transactional Boundary: Dilarang menggunakan anotasi `@Transactional` pada level class maupun method di controller.
12. DTO Immutability: Seluruh class di package `..dto..` wajib berupa Java 21 `record`.

Run ArchUnit checks:
```bash
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"
```

## 5. JaCoCo Code Coverage Quality Gate
JaCoCo terintegrasi secara native di `./gradlew check` untuk memastikan setiap logika bisnis baru diuji dengan baik:
- **Exclusion Filter**: Class tanpa logika bisnis (`**/dto/**`, `**/entity/**`, `**/model/**`, `**/config/**`, `*Application*`, `*MapperImpl*`) otomatis dikecualikan dari perhitungan coverage.
- **Coverage Ratchet**: Nilai minimal coverage (`jacocoTestCoverageVerification`) dilarang diturunkan untuk meloloskan build yang gagal.
- **Laporan Visual HTML**:
  - Dihasilkan di `build/reports/jacoco/test/html/index.html`.
  - Di-generate otomatis via `./gradlew jacocoTestReport`.

