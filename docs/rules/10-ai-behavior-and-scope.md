# Rule 10: AI Behavior, Scope Lockdown & Definition of Done

## 1. Scope Lockdown (Strict Guardrails)
- **NO ROGUE REFACTORING**: Do NOT rename methods, touch logic, or reformat files outside your assigned feature module without explicit user instruction.
- **NO UNNECESSARY DEPENDENCIES**: Do NOT add new dependencies to `build.gradle.kts` unless explicitly asked.
- **ZERO DEAD / UNUSED CODE**: Do NOT declare unused fields, variables, methods, parameters, or imports. Do NOT inject dependencies (repositories, services, clients) that are not actively used by the class.
- **YAGNI (You Aren't Gonna Need It)**: Do NOT create premature abstractions, empty interfaces for internal services, or generic base classes with only one implementation. Use concrete service classes directly. Interfaces are strictly reserved for cross-module `Client` contract.

## 2. Branching Strategy & Git Workflow (Mandatory)

Setiap pengembangan fitur baru, perbaikan bug, atau tugas refactoring **wajib diisolasi ke dalam branch baru**. Dilarang keras melakukan commit atau push langsung ke branch utama (`main` atau `dev`).

### A. Konvensi Penamaan Branch
Format penamaan branch adalah: `<tipe>/<nama-singkat-deskriptif>`
- **`feat/<nama-fitur>`**: Untuk penambahan fitur atau kapabilitas baru (contoh: `feat/endpoint-boards`, `feat/auth-google-oauth`).
- **`fix/<nama-bug>`**: Untuk perbaikan bug atau error (contoh: `fix/unauthenticated-security-context`, `fix/null-pointer-tenant`).
- **`refactor/<nama-modul>`**: Untuk refactoring kode tanpa mengubah behavior API (contoh: `refactor/user-facade-lookups`).
- **`chore/<task>`**: Untuk tugas pemeliharaan build, dependencies, atau tooling (contoh: `chore/update-gradle-wrapper`).
- **`docs/<topik>`**: Untuk penambahan atau pembaruan dokumentasi (contoh: `docs/branching-strategy`).

### B. Alur Kerja (Workflow) untuk AI & Manusia
1. **Pre-Flight Branch Check**: Sebelum membuat atau mengedit file implementasi apa pun, AI Agent dan developer wajib memverifikasi branch aktif saat ini (`git branch --show-current`).
2. **Checkout dari `dev`**: Jika branch aktif adalah `main` atau `dev`, wajib menarik versi terbaru (`git pull origin dev`) dan membuat branch baru:
   ```bash
   git checkout dev && git pull origin dev
   git checkout -b <tipe>/<nama-deskriptif>
   ```
3. **Isolasi Pengerjaan**: Semua perubahan kode, commit, dan pengujian dilakukan di branch terisolasi tersebut.
4. **Pull Request ke `dev`**: Setelah seluruh checklist DoD terpenuhi, ajukan Pull Request (PR) ke branch `dev`.

## 3. Conventional Commits
Format messages as:
`<type>(<scope>): <short description in present tense>`
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
- Scope: module name (e.g. `product`, `order`, `auth`, `common`).
- Note: Never run `git commit` or `git push` automatically without user instruction.

### Mandatory Pull Request Protocol
Setiap kali AI Agent atau developer membuat Pull Request (PR):
- **Wajib Mengikuti Template**: Deskripsi PR **wajib** mengikuti seluruh struktur dari [`.github/pull_request_template.md`](../../.github/pull_request_template.md) (Ringkasan Perubahan, Tipe Perubahan, AI-Assisted Development disclosure, dan checklist DoD).
- **Checklist Kepatuhan**: Seluruh butir checklist Definition of Done pada template PR wajib diverifikasi dan dicentang (`[x]`).
- **Dilarang PR Kosong**: Dilarang membuat PR dengan flag `--fill` default atau body kosong/ringkas tanpa struktur template resmi.

## 4. Self-Correction Loop
Before concluding a task, run the verification commands:
1. `./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"` (Targeted inner loop)
2. `./gradlew spotlessApply` (Automated formatting)
3. `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"` (Guardrail verification)
4. `./gradlew check` (Mandatory full DoD gate: tests + ArchUnit + SpotlessCheck)

Fix any compiler, formatting, or test failures autonomously before reporting completion.

---

## 5. Runtime Verification & Process Hygiene (Anti-Stale & Ghost Process Protocol)

When verifying changes against live endpoints (`bootRun` or HTTP requests):
1. **Source Integrity First**: Always verify `git status` or `git diff <file>` to ensure source changes are truly present on disk before running builds or tests.
2. **Process Cleanliness & Port Safety**: Never launch background servers without checking active port listeners (`lsof -i :<port>`). NEVER run blind termination (`kill -9`) on occupied ports. If a port is occupied by a process not spawned by the agent (e.g. Postgres Docker, IDE, developer services), **always ask the user for confirmation** before attempting to terminate it.
3. **Bypass Stale Daemon Cache**: When class signatures, constructors, annotations, or configurations change, compile with `--rerun-tasks` or stop daemons (`./gradlew --stop`) to ensure bytecode is genuinely rebuilt.
4. **Never Leave Dev Servers Running**: Background test servers spawned by the agent MUST be explicitly terminated immediately after verification curl checks succeed.

---

## 6. DOs and DON'Ts for AI Agents

### DOs:
- **DO verify disk state with `git status` / `git diff`** before concluding that a fix or code change is ready.
- **DO check active port bindings (`lsof -i :8080`)** before and after running test servers to avoid port collisions and stale responses.
- **DO ask user permission before killing any external process** occupying a target port.
- **DO use `--rerun-tasks` or `./gradlew --stop`** when modifying annotations, interfaces, or dependency injection constructors to avoid Gradle daemon stale bytecode cache.
- **DO kill any background process spawned by yourself during testing** immediately after verification is finished so the developer's workspace remains clean.
- **DO verify response bodies and headers (`curl -i`)** end-to-end to confirm both HTTP status code and payload structure match expectations.
- **DO use synthetic / dummy data** for testing and seeding to comply with personal data privacy laws (UU PDP No. 27/2022).
- **DO use variable-based Lookups** (3 steps: `extractIds` -> `client call` -> `Lookups.toMap`) for all in-memory cross-module data enrichment.
- **DO use Java 21 Record DTO projections** (`SELECT new com...Dto(...)`) for all repository aggregations and partial column projections.
- **DO specify `Collection<ID>`** on Client batch lookup methods for caller compatibility.
- **DO ask explicit confirmation before running destructive commands** (e.g., `./scripts/db.sh fresh`, `rollback`, database drop/truncate/reset).

### DON'Ts:
- **DON'T return raw `Object[]` or `List<Object[]>` from repositories** or access projection rows via index/casting (`row[0]`, `((Number) row[1]).intValue()`).
- **DON'T write fluent Lookups pipelines (`Lookups.of(...)` / `loadMap(...)`)** in service classes; always use explicit variable-based steps.
- **DON'T write derived query methods with `OrderBy` or soft-delete (`DeletedAtIsNull`)** in repositories; use Level 2 `@Query` with Ubiquitous Language names.
- **DON'T execute blind `kill -9`** (`lsof -ti :<port> | xargs kill -9`) against processes not spawned by the agent. Always ask the user first.
- **DON'T read, print, or expose secret files or credentials** (`.env`, private keys, authentication tokens, connection passwords) into chat, logs, or commit history.
- **DON'T include real personal identifiable information (PII)** like real user emails, NIK, phone numbers, or addresses in code, test fixtures, logs, or prompts (UU PDP No. 27/2022).
- **DON'T execute destructive database commands** (`./scripts/db.sh fresh`, dropping tables, database resets) without explicit confirmation from the user.
- **DON'T assume code edits succeeded without checking git diff** (never trust memory over disk state).
- **DON'T leave background `bootRun` or server processes running** as zombies on developer machine.
- **DON'T rely on Gradle UP-TO-DATE outputs** when verifying behavior changes that depend on newly added constructor parameters or reflection/annotations.
- **DON'T test only unit tests if runtime/framework wiring is being verified**; test the actual Spring context or mock container cleanly.
- **DON'T execute `git commit` or `git push`** without explicit permission from the user.
