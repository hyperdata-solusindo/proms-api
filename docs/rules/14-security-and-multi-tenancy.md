# Rule 14: Security & Multi-tenancy Standards

## 1. Overview & Core Philosophy

PROMS API follows an enterprise-grade **Defense in Depth** security model. Security is not an afterthought or an isolated filter; it is enforced across the entire stack:
1. **Database Layer**: PostgreSQL Row Level Security (RLS) policies isolating tenant data at the storage engine level.
2. **Application Layer**: Explicit tenant context resolution, connection session variable injection, and fail-closed transaction boundaries.
3. **Cache Layer**: Tenant-prefixed cache keys preventing cache-level RLS bypass.
4. **API Layer**: Strict endpoint authorization, YAVI input validation, and zero internal error/stack trace leakage.
5. **AI Agent Operational Guardrails**: Zero secret exposure, UU PDP personal data compliance, and confirmation gates on destructive commands.

---

## 2. Multi-Tenancy & Row Level Security (RLS)

### 2.1. PostgreSQL RLS Architecture
All tenant-scoped tables in schemas `platform` and `proms` have Row Level Security enabled (defined in `src/main/resources/db/changelog/changes/09-rls-policies.sql`).
- Policies isolate records via:
  ```sql
  CREATE POLICY tenant_isolation ON <schema>.<table>
      USING ((tenant_id = platform.current_tenant()))
      WITH CHECK ((tenant_id = platform.current_tenant()));
  ```
- The function `platform.current_tenant()` reads the PostgreSQL session setting:
  ```sql
  nullif(current_setting('app.tenant_id', true), '')::uuid
  ```
- Role-based restrictive policies (e.g. `finance_only`) read:
  ```sql
  platform.current_workspace_role() -- coalesces current_setting('app.workspace_role', true)
  ```

#### 2.2. Tenant Context Lifecycle & Session Injection
For RLS policies to evaluate correctly, every database transaction executing tenant-scoped operations **MUST** configure the PostgreSQL session settings:
1. **Extraction**: The active tenant is extracted from the authenticated user's session (`CurrentUser.activeTenantId()`) verified via `TokenAuthenticationFilter` and `AuthClient`.
2. **Session Variable Injection**: Before executing queries in a transaction, the connection must execute:
   ```sql
   SET LOCAL app.tenant_id = '<activeTenantId>';
   SET LOCAL app.workspace_role = '<workspaceRole>';
   ```
   *(Using `SET LOCAL` ensures that parameters are scoped strictly to the current transaction and are automatically cleared when the transaction commits or rolls back).*
3. **Thread-Bound Cleanup**: In-memory context holders (e.g. `SecurityContextHolder` or `TenantContext`) must be cleared in a `finally` block or filter completion callback to prevent thread-pool leakage.

### 2.3. The Fail-Closed Principle
- ❌ **DILARANG mengeksekusi query entitas ber-tenant saat tenant context kosong**. Jika sebuah request mengakses resource tenant tetapi `activeTenantId` bernilai `null`, sistem wajib menolak request dengan HTTP 403 Forbidden (*Fail-Closed*).
- ❌ **DILARANG menggunakan superuser database (`postgres`) untuk koneksi aplikasi runtime**. Aplikasi harus terkoneksi menggunakan role `platform_app` atau `proms_app` agar bypass RLS tidak dapat terjadi.

### 2.4. Penerapan Anotasi `@TenantScoped` & `TenantRlsAspect`
Untuk menjamin prinsip Fail-Closed dan integrasi session PostgreSQL RLS berjalan otomatis tanpa boilerplate:
1. **Kewajiban Penggunaan `@TenantScoped`**:
   - Seluruh class `Service` atau `Client` implementation yang mengelola entitas ber-tenant (contoh: `EmployeeService`, `ProductService`, `BoardService`) **WAJIB dianotasi `@TenantScoped`** (di level class atau method).
   ```java
   @Service
   @TenantScoped
   @RequiredArgsConstructor
   public class ProductService { ... }
   ```
2. **Automated Guardrail oleh `TenantRlsAspect`**:
   - **Fail-Closed Gate**: Jika method bertanda `@TenantScoped` dipanggil saat `activeTenantId == null`, aspek langsung melempar `TenantRequiredException` (diterjemahkan menjadi HTTP 403 Forbidden dengan kode `TENANT_REQUIRED`).
   - **Automated PostgreSQL RLS Session Injection**: Pada setiap transaksi aktif (`@Transactional`), aspek secara otomatis mengeksekusi:
     ```sql
     SELECT set_config('app.tenant_id', ?, true),
            set_config('app.workspace_role', ?, true),
            set_config('app.user_id', ?, true)
     ```
     Parameter di-scope per transaksi dan flag `PROMS_RLS_APPLIED_TX` memastikan perintah ini hanya dieksekusi 1 kali per transaksi.
3. **Pengecualian**:
   - Class/method publik atau cross-tenant tanpa konteks tenant (misal: `AuthService#login`, endpoint registrasi) **DILARANG** menggunakan `@TenantScoped`.

### 2.5. Cross-Tenant IDOR Disguise (404 Not Found Standard)
- **Zero Information Leakage**: Saat mengambil atau memutasi resource spesifik berdasarkan ID, service layer **WAJIB** memvalidasi bahwa `entity.getTenantId().equals(currentUser.activeTenantId())`.
- **404 (Disguise)**: Jika resource dengan ID tersebut ada di database tetapi milik tenant lain, sistem **WAJIB melempar `ResourceNotFoundException(entityName, id)` (HTTP 404)**.
- ❌ **DILARANG melempar HTTP 403 Forbidden untuk akses resource lintas-tenant**. Melempar 403 bertindak sebagai *Oracle* yang membocorkan keberadaan ID valid milik tenant lain kepada penyerang (*IDOR probing / enumeration attack*).
- **HTTP 403 Forbidden Standar**: HTTP 403 **hanya digunakan untuk otorisasi internal di dalam tenant yang sama** (contoh: user sudah berada di tenant yang benar, namun bukan member board, bukan owner, atau memiliki role `VIEWER` saat mencoba operasi create/update).

---

## 3. Tenant-Aware Caching Standards (Anti-RLS Bypass)

In-memory L1 cache (Caffeine) and distributed L2 cache (Redis) operate in application memory and **completely bypass database Row Level Security**. Without proper cache isolation, Tenant B can read Tenant A's cached data by requesting the same ID.

### Rules for Multi-Tenant Caching:
1. **Mandatory Tenant Prefix**: Every cache key for tenant-owned entities **MUST** include the tenant ID:
   ```java
   // ✅ DO: Key includes tenant context
   @Cacheable(
       cacheNames = ProductCacheNames.PRODUCT_DETAILS,
       key = "#tenantId + ':' + #id",
       unless = "#result == null"
   )
   public Optional<ProductDetailResponse> getProduct(UUID tenantId, UUID id) { ... }
   ```
   Or via authenticated context:
   ```java
   @Cacheable(
       cacheNames = <Feature>CacheNames.<NAME>,
       key = "#currentUser.activeTenantId() + ':' + #id",
       unless = "#result == null"
   )
   ```
2. ❌ **NEVER use bare IDs for tenant resources**:
   ```java
   // ❌ DON'T: Bypasses RLS across tenants!
   @Cacheable(cacheNames = ProductCacheNames.PRODUCT_DETAILS, key = "#id")
   ```
3. **Cache Eviction**: Mutation methods (create, update, delete) must evict cache using the exact same tenant prefix:
   ```java
   @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_DETAILS, key = "#tenantId + ':' + #id")
   ```
4. **Global Data Exception**: Only platform-wide master data that has no `tenant_id` (e.g. subscription plans, public feature flags, or resolving a tenant itself by `tenantId`) is allowed to use single ID keys.

---

## 4. Endpoint Authorization & Secure Coding

### 4.1. Endpoint Authorization
- Every endpoint in `<Feature>Api` or `<Feature>Controller` must explicitly declare security requirements:
  - Public endpoints: Explicitly configured in `SecurityConfig` (`permitAll()`) and marked with appropriate documentation.
  - Authenticated endpoints: Require valid authentication and verify tenant membership via `UserClient.resolveUserMembership(userId, Optional.of(tenantId))`.
  - Role-restricted endpoints: Enforce role requirements (e.g. `owner`, `admin`, `finance`, `member`) before executing domain logic.

### 4.2. Input Validation & SQL Injection Prevention
- All incoming DTOs must be validated using **YAVI** builders before processing.
- Dynamic filtering must use Spring Data JPA `JpaSpecificationExecutor` and parameterized Criteria API.
- ❌ **DILARANG melakukan konkatenasi string mentah ke dalam query SQL/JPQL**.

### 4.3. Information Leakage Prevention
- **Error Envelope**: All error responses must use `RestResponse.error(...)`.
- **Zero Stack Trace Leakage**: Internal exception messages, SQL syntax details, table names, or stack traces **DILARANG** dikembalikan ke klien HTTP.
- **Traceability**: All error responses must include an `X-Trace-Id` header so developers can correlate client errors with internal server logs without exposing server internals.

---

## 5. AI Agent Operational Security & Safety Guardrails

All AI Agents (Claude, Gemini, Cursor, Copilot, DeepSeek) operating in this repository must strictly adhere to the following operational security directives:

### 5.1. Confidentiality & Secret Protection
- ❌ **DILARANG KERAS membaca, mencetak, atau menyalin isi file `.env`, `.env.local`, private key, token JWT, atau password** ke dalam jendela chat, prompt LLM, file scratch publik, atau commit git.
- Jika memerlukan konfigurasi environment, gunakan file template `.env.example` yang hanya memuat dummy placeholder.

### 5.2. Personal Data Protection (UU PDP No. 27/2022 Compliance)
- ❌ **DILARANG KERAS menggunakan data pribadi asli** (nama orang riil, alamat email pribadi nyata, NIK/KTP, nomor telepon, atau data finansial asli) dalam kode sumber, test fixtures, unit test data, seed database, atau commit message.
- **Wajib menggunakan Synthetic / Dummy Data**:
  - Email: `user@hyperdata.co.id`, `alice@example.com`, `test.member@proms.local`
  - UUID: `UUID.randomUUID()`
  - Nama: `User Test`, `Sample Customer`, `Demo Product`

### 5.3. Safe Process Hygiene (No Blind `kill -9`)
- ❌ **DILARANG KERAS menjalankan perintah blind `kill -9`** (misalnya `lsof -ti :<port> | xargs kill -9`).
- AI Agent wajib memeriksa proses apa yang sedang berjalan pada port target (`lsof -i :<port>`). Jika proses tersebut bukan background server yang di-spawn oleh sesi agent itu sendiri (misalnya Docker Postgres, runner IDE, atau backend lain milik developer), **AI AGENT WAJIB BERTANYA DAN MEMINTA KONFIRMASI KEPADA USER** sebelum mematikannya.

### 5.4. Mandatory Confirmation for Destructive Commands
AI Agent **DILARANG** menjalankan perintah destruktif berikut secara otomatis tanpa persetujuan eksplisit dari user:
1. `./scripts/db.sh fresh` atau `./scripts/db.sh reset` (menghapus dan membuat ulang seluruh database).
2. Perintah Liquibase rollback atau `DROP TABLE` / `DROP DATABASE`.
3. Perintah SQL `TRUNCATE` atau `DELETE FROM ...` dalam jumlah besar.
4. Perintah `git reset --hard` atau `git clean -fd` yang berpotensi menghapus pekerjaan developer.

---

## 6. Distributed Rate Limiting & Throttling (Bucket4j + Redis)

Untuk melindungi infrastruktur dari serangan brute-force, DoS, serta efek *noisy neighbor* antar-tenant, PROMS API menerapkan sistem pembatasan laju trafik terdistribusi 2-Tier:

### 6.1. Tier 1: Edge Pre-Auth Rate Limiting (`PreAuthRateLimitingFilter`)
- Berjalan sebelum rantai autentikasi (`LogoutFilter.class`).
- **Resolusi Klien IP**: Mendukung deteksi header edge reverse proxy/Cloudflare (`CF-Connecting-IP`, `X-Forwarded-For`).
- **Global IP Limiting**: Membatasi laju per IP klien (`rl:edge:ip:<ip>`).
- **Brute-Force Auth Protection**: Endpoint login (`POST /api/v1/auth/login`) memiliki bucket pembatasan ketat tersendiri (`rl:auth:ip:<ip>`).

### 6.2. Tier 2: Post-Auth Rate Limiting (`PostAuthRateLimitingFilter`)
- Berjalan setelah `TokenAuthenticationFilter` terverifikasi.
- **User-Level Limiting**: Membatasi laju request per pengguna terautentikasi (`rl:user:<userId>`).
- **Tenant Anti-Noisy-Neighbor**: Mengisolasi kapasitas agregat per workspace (`rl:tenant:<tenantId>`) sehingga aktivitas lonjakan request satu tenant tidak menghabiskan kuota atau merusak performa tenant lain.
- **In-Flight Concurrency Guard**: Membatasi jumlah request bersamaan (*max concurrent*) yang sedang dieksekusi via `releaseInFlight` pada blok `finally`.

### 6.3. Kontrak Respon HTTP 429
Saat kuota token bucket terlampaui, filter langsung memutus filter chain dan mengembalikan respon JSON standar:
- HTTP Status: `429 Too Many Requests`
- Error Code: `CommonErrorCode.RATE_LIMIT_EXCEEDED`
- Header Wajib:
  - `Retry-After`: Jumlah detik klien harus menunggu sebelum mencoba kembali.
  - `X-RateLimit-Limit`: Kapasitas maksimum bucket.
  - `X-RateLimit-Remaining`: Sisa token yang tersedia (bernilai `0`).

---

## 7. Personal Access Token (PAT) Architecture & Sliding Window Expiration

Sistem autentikasi PROMS API menggunakan **Stateful Personal Access Token (PAT)** berbasis basis data (`platform.personal_access_tokens`), bukan Stateless JWT:

### 7.1. Format & Hashing Token
- **Format Header**: `Authorization: Bearer <tokenId>|<plainToken>` (pola pipe-delimited).
- **Keamanan Hashing**: Token mentah (*plainToken*) hanya ditampilkan 1 kali ke user saat login/generasi token. Di database, nilai token disimpan dalam bentuk hash kriptografis **SHA-256** (`pat.setToken(hashToken(plainToken))`).
- **Resolusi Cepat**: `<tokenId>` digunakan sebagai index pencarian utama, lalu hash dicocokkan via `tokenRepository.findByIdAndToken(tokenId, hashed)`.

### 7.2. Sliding Window Expiration
- **TTL Default**: Token memiliki masa aktif `app.security.token.ttl-days` (default 30 hari).
- **Perpanjangan Otomatis (Sliding Window)**: Setiap request valid memicu `authClient.recordTokenUsage(tokenId)`. Jika sisa waktu aktif token kurang dari `app.security.token.sliding-window-days` (default 7 hari), tanggal kedaluwarsa (`expires_at`) otomatis diperpanjang 30 hari ke depan, menjaga sesi pengguna aktif tanpa memaksa re-login berulang.
