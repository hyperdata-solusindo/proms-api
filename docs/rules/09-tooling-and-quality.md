# Rule 09: Automated Quality Gates (ArchUnit, Spotless & IDE Formatting)

## 1. Spotless Java Formatter (Palantir Java Format)
Spotless menggunakan **Palantir Java Format** (`palantirJavaFormat()`), yang dirancang khusus untuk memformat lambda, stream, dan fluent builder (seperti YAVI dan CriteriaBuilder) secara rapi dan linear tanpa nesting bertingkat (*staircase indentation*) yang terjadi pada Google Java Format biasa.

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
   - **Default Formatter Java**: `richardwillis.vscode-spotless-gradle` (menjalankan engine Spotless Palantir langsung di IDE setiap kali `Ctrl+S` / Save sehingga hasil format 100% identik dengan AI dan build pipeline).
   - **Language Support & Import Management (Red Hat Java)**: `redhat.java` menangani IntelliSense, navigasi, dan import otomatis. `java.completion.importOrder` diset `["#", ""]` agar import tersusun dalam 1 blok alfabetis tanpa baris kosong pemisah package (selaras dengan Palantir/Google Java Style), serta `starThreshold: 99` untuk melarang wildcard (`.*`).
   - **Fallback Profil Formatter Eclipse**: [`.vscode/eclipse-java-formatter.xml`](../../.vscode/eclipse-java-formatter.xml) (`PromsJavaFormat`).
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
5. Controllers must be annotated with `@RestController`.
6. Services must be annotated with `@Service`.

Run ArchUnit checks:
```bash
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"
```
