## Ringkasan Perubahan
<!-- Jelaskan secara singkat tujuan PR ini, fitur baru yang ditambahkan, atau bug yang diperbaiki -->

## Tipe Perubahan
- [ ] `feat`: Fitur baru
- [ ] `fix`: Perbaikan bug
- [ ] `refactor`: Refactoring kode tanpa mengubah fungsionalitas
- [ ] `docs`: Pembaruan dokumentasi atau rules
- [ ] `test`: Penambahan atau perbaikan unit/integration tests
- [ ] `chore`: Perubahan tooling, gradle, CI, atau dependencies

## AI-Assisted Development
- [ ] PR ini dibuat atau dibantu oleh AI Agent
  - **Tool / LLM yang digunakan**: <!-- e.g., Antigravity, Claude Code, Cursor, Copilot -->
  - **Model**: <!-- e.g., gemini-3.8-flash, claude-3-7-sonnet -->
  - **Pernyataan Pengembang**: Kode hasil AI telah diperiksa secara manual baris demi baris sebelum diajukan.

## Checklist Definition of Done (DoD)
Wajib diverifikasi sebelum meminta review:
- [ ] **Struktur & Penempatan**: Kode diletakkan di `module.<domain>.<feature>/` atau `common/`.
- [ ] **DTO Immutability**: Seluruh request & response DTO menggunakan Java 21 `record`.
- [ ] **JPA Entities**: Menggunakan Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`, dan **TIDAK menggunakan `@Data`**.
- [ ] **Dependency Injection**: Menggunakan Constructor Injection (`@RequiredArgsConstructor` pada field `final`), tidak ada `@Autowired` pada field.
- [ ] **Batas Modularitas & Facade**:
  - Modul lain hanya mengimpor interface di `facade/` dan DTO di `facade/dto/`.
  - Tidak ada import langsung ke `service/`, `repository/`, `entity/`, atau `constant/` modul lain.
  - Implementasi Facade berada di `facade.internal/` dan beranotasi `@Component`.
- [ ] **Anti-Entity Leakage**: Controller dan Facade tidak mengembalikan entity JPA `@Entity`.
- [ ] **Cross-Module Enrichment**: Menggunakan **The Golden Stack** (L1 Cache + `Lookups` in-memory + MapStruct) tanpa SQL JOIN lintas modul atau query di dalam loop.
- [ ] **Multi-Tenancy & Cache**: Cache key untuk data tenant menyertakan tenant prefix (`#tenantId + ':' + #id`).
- [ ] **Validasi YAVI**: Input divalidasi menggunakan YAVI dengan konvensi nama `public static final Validator<T> validator`.
- [ ] **Testing & Quality Gates**:
  - [ ] Unit test ditulis dengan format BDD `@DisplayName`.
  - [ ] Format kode rapi via `./gradlew spotlessApply`.
  - [ ] Semua pengujian & guardrail arsitektur lolos: `./gradlew check`.
- [ ] **Keamanan & Kebersihan**:
  - [ ] Tidak ada file `.env`, token, atau password yang ter-commit.
  - [ ] Menggunakan data sintetis/dummy (kepatuhan UU PDP No. 27/2022).
  - [ ] Tidak ada dead code / dependensi spekulatif yang tidak digunakan.
