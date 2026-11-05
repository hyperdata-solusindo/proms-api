# Review Panduan AI Agent — Kesiapan Level Enterprise

- **Tanggal**: 2026-11-05
- **Basis review**: branch `dev` @ `4cfaa5f`
- **Cakupan**: `AGENTS.md` (+ `CLAUDE.md` symlink), `docs/rules/01–13`, `.cursorrules`,
  `.github/copilot-instructions.md`, `.agents/skills/project-brainstorming/SKILL.md`,
  `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`, `build.gradle.kts`

## Ringkasan

Fondasinya sudah baik, di atas rata-rata tim, tapi **belum level enterprise**. Celah utamanya:

1. Sebagian besar aturan hanya tertulis dan **tidak dipaksakan otomatis** (belum ada CI, ArchUnit baru
   mencakup sebagian aturan).
2. **Belum ada aturan keamanan & multi-tenancy**, padahal database memakai Row Level Security per tenant.
3. **Belum ada tata kelola** (CODEOWNERS, template PR, ADR, kepemilikan dokumen aturan).

## Yang Sudah Baik

- **Single source of truth**: `AGENTS.md` sebagai pusat. `CLAUDE.md`, `.cursorrules`, Copilot, dan
  Gemini hanya merujuk ke sana, jadi semua tool AI membaca aturan yang sama.
- **Aturan modular per topik** (`docs/rules/01–13`) dengan contoh kode DO/DON'T. Untuk AI, contoh kode
  lebih efektif daripada penjelasan abstrak.
- **Definition of Done**, scope lockdown (larangan refactor di luar modul), dan larangan commit/push
  otomatis.
- **Guardrail ArchUnit** untuk sebagian aturan: controller tidak boleh bergantung pada repository,
  facade wajib interface, implementasi facade di `facade.internal`, facade tidak mengembalikan entity,
  controller wajib mengimplementasikan kontrak `<Feature>Api`.
- **Alur spec → plan** (`docs/superpowers/`) untuk fitur besar.

## Temuan

### 1. Aturan tidak dipaksakan otomatis — prioritas tertinggi

- **Belum ada CI.** Isi `.github/` hanya `copilot-instructions.md`. ArchUnit dan `spotlessCheck` baru
  jalan kalau developer ingat menjalankannya, dan pre-commit hook bisa dilewati (`--no-verify`).
- **Aturan inti belum dicek ArchUnit**:
  - Rule #2 / Rule 01 §2: modul lain dilarang meng-import `service`, `repository`, `entity`, `mapper`,
    atau `constant` milik modul lain.
  - Rule #6: larangan field injection `@Autowired`.
  - Rule #5: larangan `@Data` di JPA entity.
  - Rule 01 §2.5: `@Transactional` dilarang di controller.
  - Rule #4: DTO wajib `record`.

Aturan tanpa pengecekan otomatis pada praktiknya hanya saran. Pelanggaran oleh AI maupun developer
akan lolos diam-diam.

### 2. Aturan bertentangan dengan kode atau dengan aturan lain

| Lokasi | Masalah |
|---|---|
| Rule 03 vs Rule 12 | Nama konstanta validator berbeda: `validator` (Rule 03, `CreateProductRequest`) vs `VALIDATOR` (Rule 12, `LoginRequest`). AI akan memilih acak. |
| Rule 06 §1 | Contoh respons menampilkan `"errors": null`, padahal `@JsonInclude(NON_NULL)` membuat field itu tidak muncul. |
| Rule 08 §3 | Menyebut "test containers", padahal dependency Testcontainers tidak ada di `build.gradle.kts`. |
| `AGENTS.md` §5 vs Rule 10 §3 | DoD di `AGENTS.md` mewajibkan `./gradlew check`, tapi Self-Correction Loop di Rule 10 hanya menjalankan test modul + ArchUnit. AI bisa menyatakan "selesai" tanpa menjalankan full test. |

### 3. Belum ada aturan keamanan & multi-tenancy

- **Tenant context / RLS tidak diatur.** Policy RLS (`09-rls-policies.sql`) membaca
  `current_setting('app.tenant_id')`, tapi tidak ada aturan cara mengisi tenant context per transaksi,
  dan tidak ada larangan query tanpa tenant. Ini risiko kebocoran data terbesar.
- **Cache dapat melewati RLS.** Rule 11 mencontohkan `@Cacheable(key = "#id")`. Data yang diambil dari
  cache tidak melewati pengecekan tenant di database. Jika satu endpoint lupa mengecek kepemilikan, data
  tenant A bisa terbaca tenant B. Cache lokal (Caffeine) juga akan basi pada deployment multi-instance.
- **Belum ada aturan secure coding**: kewajiban cek otorisasi per endpoint, penanganan input, dan
  larangan membocorkan detail error/internal ke klien.
- **Belum ada aturan keamanan untuk AI agent**:
  - larangan membaca/mencetak isi `.env` dan secret;
  - larangan memasukkan data pribadi (UU PDP No. 27/2022) ke prompt atau log;
  - kewajiban konfirmasi sebelum perintah destruktif (`./scripts/db.sh fresh`, `rollback`, drop data).
- **Instruksi berbahaya di Rule 10 §4**: `lsof -ti :<port> | xargs kill -9` menyuruh AI mematikan
  proses apa pun di port target tanpa bertanya, termasuk proses milik developer (Postgres Docker, IDE,
  server lain).

### 4. Skill `project-brainstorming` kurang portabel dan boros konteks

- Menyebut nama tool spesifik (`view_file`, `client_view_file`) yang tidak ada di Claude Code, Cursor,
  atau Copilot.
- Mewajibkan membaca seluruh aturan di awal (sekitar 1.100 baris), padahal yang relevan biasanya hanya
  2–3 file. Konteks terbuang dan aturan penting tenggelam.
- Kewajiban menampilkan "Directives Auto-Injected" banner bersifat seremonial dan tidak menambah
  kualitas hasil.
- Gaya bahasa huruf kapital ("CRITICAL DIRECTIVE", "NEVER guess") kurang efektif untuk model modern.
  Model lebih patuh bila aturan disertai alasannya ("jangan X karena Y").

### 5. Tata kelola belum ada

- Belum ada `CODEOWNERS`, template PR, maupun ADR (Architecture Decision Record).
- Keputusan besar ("dilarang SQL JOIN lintas modul", Golden Stack, Caffeine L1 cache) tidak
  terdokumentasi alasan dan trade-off-nya, sehingga tidak jelas kapan pengecualian diperbolehkan.
- Dokumen aturan belum punya pemilik dan versi, serta belum ada proses review untuk kode hasil AI
  (misalnya wajib review manusia dan label PR "AI-assisted").
- Bahasa Indonesia dan Inggris bercampur dalam satu file. Sebaiknya pilih satu bahasa per dokumen.

## Rekomendasi (Urut Prioritas)

| # | Rekomendasi | Dampak |
|---|---|---|
| 1 | Buat CI di GitHub Actions yang menjalankan `./gradlew check` (test + ArchUnit + `spotlessCheck`) pada setiap PR, dan jadikan status check wajib sebelum merge (branch protection). | Aturan benar-benar dipaksakan. |
| 2 | Tambah aturan ArchUnit: batas antar-modul, larangan `@Autowired` field, larangan `@Data` pada entity, larangan `@Transactional` di controller, DTO wajib `record`. | Aturan inti tercek otomatis. |
| 3 | Tulis **Rule 14: Security & Multi-tenancy**: tenant context/RLS, cache key wajib menyertakan tenant, cek otorisasi, serta aturan keamanan AI agent (`.env`, data pribadi, konfirmasi perintah destruktif). Ganti instruksi `kill -9` dengan "tanyakan dulu sebelum mematikan proses yang bukan milik agent". | Menutup risiko kebocoran data. |
| 4 | Rapikan kontradiksi: satu konvensi nama validator, perbaiki contoh Rule 06, tambahkan Testcontainers atau hapus rujukannya, dan satukan definisi DoD. | AI tidak lagi menebak. |
| 5 | Ringkas skill brainstorming: hapus nama tool spesifik dan banner, baca aturan sesuai topik saja, sertakan alasan pada setiap larangan. | Hemat konteks, portabel di semua tool. |
| 6 | Tata kelola: `CODEOWNERS`, template PR berisi checklist DoD, folder `docs/adr/` untuk keputusan arsitektur (mulai dari Golden Stack). | Standar enterprise untuk tim. |

Rekomendasi #3 memerlukan keputusan desain (sumber tenant: JWT claim, header, atau subdomain), jadi
perlu didiskusikan dan dibuatkan spec tersendiri sebelum diimplementasikan.
