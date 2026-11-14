# Review Struktur & Desain API — Kesesuaian Best Practice

- **Tanggal**: 2026-11-10
- **Basis review**: branch `main` @ `e73eb70`
- **Metode**: membaca kode (static review). Test tidak dijalankan.
- **Cakupan**: `common/` (response, exception, security, config), modul `identity/auth`,
  `identity/user`, `platform/tenant`, `sample/employee`, `sample/product`,
  `src/main/resources/application.properties`, `db/changelog/changes/*.sql`, `ArchitectureTest`

## Ringkasan

Struktur modularnya sudah bagus. Masalahnya ada di **security dan multi-tenancy, yang belum terpasang
secara nyata**. Untuk API multi-tenant, ini risiko terbesar dan harus diperbaiki sebelum ada modul
bisnis baru.

## Yang Sudah Sesuai Best Practice

- **Modular monolith + facade boundary** jelas (`module/<domain>/<feature>`, facade berupa interface,
  implementasi di `facade/internal`) dan dijaga ArchUnit, termasuk larangan mengakses package internal
  modul lain.
- **Kontrak API dipisah dari implementasi** (`<Feature>Api` interface + controller), jadi anotasi
  OpenAPI tidak mengotori controller.
- **DTO berupa record**, entity tidak bocor ke luar, mapping memakai MapStruct.
- **Anti-N+1 sudah benar** di endpoint daftar: `Lookups.extractIds` → `getTenantSummaries` → `toMap`
  (`EmployeeService#getEmployees`).
- **Token disimpan sebagai hash SHA-256**, session stateless, `spring.jpa.open-in-view=false`,
  `ddl-auto=none`.
- **Envelope error konsisten** (`code`, `fields`, `traceId`) dengan integrasi Sentry/OTel.

## Temuan

### 🔴 Kritis

#### 1. Semua endpoint bisnis terbuka tanpa autentikasi — `[RESOLVED]`

`SecurityConfig` memakai `.anyRequest().permitAll()`, sehingga `/api/v1/employees` dan
`/api/v1/products` bisa diakses tanpa token. `/actuator/**` juga di-`permitAll`, termasuk `metrics` dan
`prometheus`.

**Rekomendasi**: deny-by-default dengan `.anyRequest().authenticated()` dan whitelist eksplisit. Batasi
actuator ke `health`/`info` untuk publik, sisanya lewat jaringan internal atau autentikasi.

> **Status [RESOLVED]**: `SecurityConfig` telah diperbarui dengan deny-by-default (`anyRequest().authenticated()`). Endpoint publik dibatasi hanya untuk login/auth dan dokumentasi OpenAPI/Scalar. Actuator publik dibatasi hanya untuk `/health` dan `/info`.

#### 2. RLS terpasang di database, tapi tidak pernah diaktifkan dari aplikasi — `[RESOLVED]`

`09-rls-policies.sql` memakai `platform.current_tenant()`, yang membaca
`current_setting('app.tenant_id')`. Tidak ada kode Java yang menjalankan `SET LOCAL app.tenant_id` atau
`set_config(...)`. Akibatnya salah satu dari dua hal ini terjadi:

- role aplikasi terkena RLS, sehingga semua query tenant mengembalikan 0 baris; atau
- role aplikasi adalah owner/superuser, sehingga RLS terlewati dan **isolasi tenant tidak ada**.

Ini bertentangan dengan `docs/rules/14-security-and-multi-tenancy.md` (fail-closed, 403 jika tenant
kosong).

**Rekomendasi**: buat komponen tenant context (mis. aspect di sekitar `@Transactional` atau
`ConnectionPreparer`) yang menjalankan `select set_config('app.tenant_id', ?, true)` di awal setiap
transaksi berdasarkan `CurrentUser.activeTenantId()`, dan tolak request jika tenant kosong. Pastikan
role aplikasi bukan owner tabel dan tidak punya `BYPASSRLS`.

> **Status [RESOLVED]**: Diimplementasikan menggunakan `@TenantScoped` dan `TenantRlsAspect`. Setiap method transaksi pada class bertanda `@TenantScoped` otomatis mengeksekusi `set_config('app.tenant_id', ?, true)` dan mengisolasi session RLS. Jika `activeTenantId` bernilai `null`, request langsung ditolak dengan `TenantRequiredException` (HTTP 403 Forbidden - Fail-Closed).

#### 3. Tenant diambil dari input client

`tenantId` ada di `CreateEmployeeRequest`, `UpdateEmployeeRequest`, dan `EmployeeFilter`, sehingga client
bisa membaca atau menulis data tenant lain.

**Rekomendasi**: hapus `tenantId` dari request DTO dan ambil dari `CurrentUser.activeTenantId()`.

#### 4. Cache key tanpa prefix tenant

`@Cacheable(key = "#id")` di `EmployeeService` dan `ProductService` melanggar rule 14
(`#tenantId + ':' + #id`). Response yang di-cache juga menyertakan `TenantSummaryDto`, jadi datanya bisa
basi sampai TTL habis.

**Rekomendasi**: tambahkan prefix tenant di semua `@Cacheable`/`@CacheEvict` untuk resource tenant, dan
pertimbangkan untuk hanya meng-cache data milik modul sendiri.

### 🟠 Menengah

#### 5. Exception framework berubah menjadi 500 — `[RESOLVED]`

`GlobalExceptionHandler` menangkap `Exception.class` tanpa meng-extend
`ResponseEntityExceptionHandler`. Kasus-kasus berikut akhirnya dibalas 500, dicatat sebagai error, dan
dikirim ke Sentry:

| Kasus | Exception | Status yang benar |
| --- | --- | --- |
| UUID path tidak valid | `MethodArgumentTypeMismatchException` | 400 |
| Route tidak ada | `NoResourceFoundException` | 404 |
| Method salah | `HttpRequestMethodNotSupportedException` | 405 |
| Unique violation (race) | `DataIntegrityViolationException` | 409 |

**Rekomendasi**: extend `ResponseEntityExceptionHandler` atau tambahkan handler eksplisit untuk kasus di
atas.

> **Status [RESOLVED]**: `GlobalExceptionHandler` kini menangani `MethodArgumentTypeMismatchException` (400), `NoResourceFoundException` (404), `HttpRequestMethodNotSupportedException` (405), dan `DataIntegrityViolationException` (409) secara eksplisit.

#### 6. Tracer di `GlobalExceptionHandler` kemungkinan tidak ter-inject — `[RESOLVED]`

Class ini punya tiga constructor tanpa `@Autowired`. Dalam kondisi itu Spring memakai constructor tanpa
argumen, sehingga `tracerProvider = null` dan `traceId` hanya diambil dari MDC.

**Rekomendasi**: gunakan satu constructor yang menerima `ObjectProvider<Tracer>`. Untuk test, sediakan
helper di sisi test, bukan constructor tambahan di kode produksi.

> **Status [RESOLVED]**: Disederhanakan menjadi constructor tunggal yang menerima `ObjectProvider<Tracer>`.

#### 7. Kontrak response tidak konsisten dengan `docs/rules/06` — `[RESOLVED]`

- `GET /api/v1/employees` dan `/products` mengembalikan `PageResponse` mentah tanpa envelope
  `RestResponse`, sedangkan rule 06 menetapkan `meta.nextCursor` (cursor pagination). Kode memakai
  offset pagination.
- Bahasa pesan campur: "Berhasil", pesan Indonesia di handler, "Employee created successfully" di
  controller, dan "An unexpected internal server error occurred" di handler generik.
- Prefix path tidak seragam: `/auth/login`, `/auth/logout`, dan `/me` tanpa versi, sementara yang lain
  memakai `/api/v1/...`.

**Rekomendasi**: putuskan satu standar (envelope + jenis pagination, bahasa pesan, prefix path), tulis di
rule 06, lalu seragamkan.

> **Status [RESOLVED]**:
> 1. Seluruh endpoint daftar berhalaman distandarisasi dibungkus dengan `RestResponse<PageResponse<T>>`.
> 2. Seluruh string pesan dipindahkan ke arsitektur modular i18n terpusat (`messages/{domain}/{feature}.properties`) dan di-resolve dinamis via `Messages.get(...)` berdasarkan header `Accept-Language` (default: `id-ID`).
> 3. Seluruh route distandarisasi ke prefix `/api/v1/...` dengan backward-compatibility alias untuk endpoint legacy (`/auth/login`, `/auth/logout`, `/me`).

#### 8. Optimistic locking setengah jadi `[RESOLVED]`

`STALE_VERSION`, `IF_MATCH_REQUIRED`, `StaleVersionException`, dan `IfMatchRequiredException` sudah ada,
tapi entity tidak punya `@Version` dan endpoint `PUT` tidak membaca header `If-Match`.

> **Status Perbaikan (2026-11-11)**:
> 1. Dibuat helper terpusat `ConcurrencyGuard.requireMatch(...)` yang otomatis membaca header `If-Match`, mendukung Epoch Milliseconds (`"1731301234000"`) & ISO-8601 (`"2026-11-10T10:15:30.123456Z"`), menormalisasi quotes (`"..."`) dan weak ETag (`W/"..."`), serta memotong presisi mikrodetik (`ChronoUnit.MICROS`) untuk menyelaraskan `timestamptz` PostgreSQL dengan Java `Instant`.
> 2. Diterapkan pada endpoint `PUT /api/v1/boards/{boardId}` di `BoardService` dan `BoardController` dengan proteksi 428 (`IF_MATCH_REQUIRED`) dan 409 (`STALE_VERSION` beserta snapshot `current`).
> 3. Dibuat reusable OpenAPI annotations: `@ApiConcurrencyControl`, `@IfMatchHeader`, `@ApiResponseNotFound`, `@ApiResponseStaleVersion`, `@ApiResponseIfMatchRequired`, dan `@VersionEpochSchema` yang otomatis terintegrasi dengan `OpenApiConfig`.

#### 9. Unique constraint global, bukan per tenant

`Employee.email` dan `Employee.employeeNumber` diberi `unique = true` secara global. Di sistem
multi-tenant biasanya cukup unik per tenant (`(tenant_id, email)`). Pola cek `exists` lalu `save` juga
rawan race condition, yang kemudian berujung ke temuan 5.

### 🟡 Minor

- `CommonErrorCode` di `common/` berisi kode domain (`BOARD_NOT_FOUND`, `TASK_NOT_FOUND`,
  `BR-WF-06..09`). Kode seperti ini sebaiknya pindah ke modul pemiliknya.
- `TokenAuthenticationFilter` menelan semua exception tanpa log, dan `recordTokenUsage` menulis ke DB di
  setiap request. Sebaiknya dibatasi (throttle), mis. hanya update jika `lastUsedAt` lebih dari 1 menit
  yang lalu.
- `statusCode` di body menduplikasi HTTP status. Ini masih bisa diterima karena sudah menjadi kontrak
  untuk mobile.
- Modul `sample/employee` dan `sample/product` tidak punya changelog Liquibase. Itu wajar untuk contoh,
  tapi perlu ditandai jelas supaya tidak ikut ke production.

## Urutan Perbaikan yang Disarankan

1. Deny-by-default di `SecurityConfig` dan amankan actuator (temuan 1).
2. Komponen tenant context dengan `SET LOCAL app.tenant_id` yang fail-closed, lalu hapus `tenantId` dari
   request DTO (temuan 2–3).
3. Tambahkan prefix tenant di cache key (temuan 4).
4. Lengkapi `GlobalExceptionHandler` (400/404/405/409) dan rapikan constructor-nya (temuan 5–6).
5. Seragamkan envelope daftar data, bahasa pesan, dan prefix path (temuan 7).
6. Optimistic locking dan unique constraint per tenant (temuan 8–9).
