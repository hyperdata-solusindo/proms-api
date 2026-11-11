# Rule 06: API Contract, Envelopes & Error Handling

All REST endpoints return responses wrapped in `RestResponse<T>` (termasuk daftar berhalaman dibungkus sebagai `RestResponse<PageResponse<T>>`).

## 1. API Versioning & Path Prefixing
- **Standar Prefix**: Seluruh endpoint REST bisnis dan identity **WAJIB** menggunakan prefix versi URI `/api/v1/` (contoh: `/api/v1/auth/login`, `/api/v1/users/me`, `/api/v1/employees`, `/api/v1/products`, `/api/v1/boards`).
- **Backward Compatibility Alias**: Jika terdapat rute lama/legacy (misalnya `/auth/login` atau `/me`), controller wajib menyediakan alias backward-compatibility melalui array path:
  ```java
  @PostMapping({"/api/v1/auth/login", "/auth/login"})
  @GetMapping({"/api/v1/users/me", "/me"})
  ```

## 2. Response Envelope Contract

| Field | Tipe | Aturan |
|---|---|---|
| `statusCode` | integer | **Selalu sama** dengan HTTP status. Server tidak pernah mengirim HTTP 200 untuk error |
| `message` | string | Pesan ter-lokalisasi (multi-language i18n) berdasarkan header `Accept-Language`, aman ditampilkan ke user (toast/sheet) |
| `data` | object / array / `null` | Isi respons. Objek tunggal = objek, daftar = array (atau objek `PageResponse` berisi list items), tanpa isi = `null`. |
| `meta` | object | Metadata pendukung (opsional): pagination meta, `nextCursor`, atau ringkasan angka (`unreadCount`, `summary`). Opsi `null` dihilangkan otomatis |
| `error` | object | Hanya untuk error (`statusCode` ≥ 400). Untuk sukses, field ini tidak dikirim |

### Objek `error`
| Field `error` | Tipe | Isi & Kegunaan |
|---|---|---|
| `code` | string | Kode BR (`BR-WF-09`) atau kode teknis (`STALE_VERSION`, `VALIDATION`). **App/Frontend membuat keputusan berdasarkan `code`, bukan `message`** |
| `detail` | string | Penjelasan tambahan yang dapat membantu user memperbaiki input (opsional) |
| `fields` | array | Daftar kesalahan per field untuk `422 VALIDATION` (opsional), berupa `[{ field: string, message: string }]` |
| `current` | object | Data payload terbaru untuk `409 STALE_VERSION` (opsional) |

---

## 3. Success Envelopes (`RestResponse.ok` / `RestResponse.created`)

### Single Resource (200 OK)
```json
{
  "statusCode": 200,
  "message": "Resource retrieved successfully",
  "data": {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "name": "MacBook Pro"
  }
}
```

### Resource Creation (201 Created)
```json
{
  "statusCode": 201,
  "message": "Resource created successfully",
  "data": {
    "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
  }
}
```

### Paginated List (`RestResponse<PageResponse<T>>`)
```json
{
  "statusCode": 200,
  "message": "Berhasil memuat daftar karyawan",
  "data": {
    "items": [
      { "id": "1", "name": "Budi Santoso" }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

## 4. Error Envelopes (`RestResponse.error`)

### 4.1 Kode Error yang Wajib Ditangani App
Kolom `code` di bawah dibaca dari `error.code`.

| Status | `code` | Tampilan di app (brief §5) |
|---|---|---|
| 401 | `UNAUTHENTICATED` | Kembali ke login |
| 403 | `TENANT_ACCESS_REVOKED`, `TENANT_REQUIRED` | Layar pilih workspace |
| 403 | `TENANT_READ_ONLY` | Banner "Workspace dalam mode baca saja", tombol tulis nonaktif |
| 403 | `FORBIDDEN_*`, `BR-WF-06`, `BR-WF-07` | Pesan `message` dari server di sheet/toast |
| 404 | `BOARD_NOT_FOUND`, `TASK_NOT_FOUND`, … | "Board tidak ditemukan" (jangan tulis "tidak punya akses") |
| 409 | `STALE_VERSION` | Toast "Data sudah berubah, dimuat ulang" lalu muat ulang dari `error.current` |
| 409 | `BOARD_ARCHIVED` | Toast "Board diarsipkan, tidak bisa diubah" |
| 409 | `BR-WF-08` | Kolom penuh (WIP), pesan dari server |
| 422 | `BR-WF-09` | Sheet meminta alasan Blocked |
| 422 | `VALIDATION` | Pesan per field dari `error.fields` |
| 428 | `IF_MATCH_REQUIRED` | Bug di app; tidak ditampilkan ke user |

### Malformed Request Body (400 Bad Request)
```json
{
  "statusCode": 400,
  "message": "Format data tidak valid",
  "data": null,
  "error": {
    "code": "BAD_REQUEST",
    "detail": "Format payload request JSON tidak valid atau rusak"
  }
}
```

### Client / Validation Error (422 Unprocessable Entity)
```json
{
  "statusCode": 422,
  "message": "Alasan wajib diisi saat memindah task ke Blocked",
  "data": null,
  "error": {
    "code": "BR-WF-09",
    "detail": "Isi alasan Blocked lalu coba lagi.",
    "fields": [
      {
        "field": "blockedReason",
        "message": "Wajib diisi"
      }
    ]
  }
}
```

### Business Rule / Resource Error (404 Not Found, 409 Conflict)
```json
{
  "statusCode": 404,
  "message": "Produk dengan ID [a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11] tidak ditemukan",
  "data": null,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "detail": null
  }
}
```

### Stale Version Conflict (409 Conflict)
```json
{
  "statusCode": 409,
  "message": "Data telah diperbarui oleh pengguna lain",
  "data": null,
  "error": {
    "code": "STALE_VERSION",
    "detail": "Silakan muat ulang data terbaru sebelum menyimpan kembali.",
    "current": {
      "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
      "version": 4
    }
  }
}
```

---

## 5. Exception Handling
- Throw specific custom exceptions extending `BaseException`:
  - `InvalidCredentialsException` (401 `UNAUTHENTICATED`)
  - `TenantRequiredException` (403 `TENANT_REQUIRED`)
  - `ResourceNotFoundException` (404 `RESOURCE_NOT_FOUND`, `BOARD_NOT_FOUND`, dll)
  - `DuplicateResourceException` (409 `DUPLICATE_RESOURCE`)
  - `StaleVersionException` (409 `STALE_VERSION` dengan payload `current`)
  - `IfMatchRequiredException` (428 `IF_MATCH_REQUIRED`)
  - `ValidationException` (422 `VALIDATION` dengan `fieldErrors`)
  - `BusinessException` (Aturan alur kerja bisnis dinamis, misal `BR-WF-06`, `BR-WF-08`)
- Seluruh pesan default exception wajib di-resolve dinamis via `Messages.get(...)` (mendukung lokalisasi i18n).
- Selalu gunakan konstanta dari `CommonErrorCode` untuk menghindari *typo* pada `code`.
- ❌ **NEVER throw raw `RuntimeException`**.
- All exceptions are captured automatically by `GlobalExceptionHandler`.
- Always supply machine-readable business rule codes (`code`) on `BusinessException` for frontend state transitions.

## 6. Trace Correlation & Sentry Integration
- All HTTP responses include `X-Trace-Id: <traceId>` header for end-to-end debugging.
- Unhandled 500 exceptions are captured in Sentry as `ERROR` with linked `otel.trace_id`.
- Handled client errors (422/400) are captured in Sentry as `WARNING` under tag `error.category`.

---

## 7. Multi-Language & Modular i18n Architecture (Pola 1: Domain Bounded Context)

### 7.1 Struktur File Resource Bundles
File pesan properties disusun per domain dan fitur di dalam `src/main/resources/messages/`:
```text
src/main/resources/messages/
├── common/
│   ├── common.properties           # Bahasa Indonesia (Default Fallback)
│   └── common_en.properties        # Bahasa Inggris
├── identity/
│   ├── auth.properties
│   ├── auth_en.properties
│   ├── user.properties
│   └── user_en.properties
├── pm/
│   ├── board.properties
│   └── board_en.properties
└── sample/
    ├── employee.properties
    └── product.properties
```

### 7.2 Dynamic Message Resolution via `Messages.get(...)`
- `LocalizationConfig` memuat seluruh resource bundle secara otomatis melalui pola `classpath*:messages/**/*.properties` menggunakan `ReloadableResourceBundleMessageSource`.
- Seluruh pengambilan pesan di Controller, Service, Facade, dan Exception **WAJIB** menggunakan helper terpusat:
  ```java
  // Mengambil pesan sesuai header HTTP "Accept-Language" (default: id-ID, en)
  String msg = Messages.get("product.deleted.success");
  String notFound = Messages.get("product.not.found", productId);
  ```
- ❌ **DILARANG KERAS melakukan hardcode string pesan bisnis/respons langsung di kode Java**. Seluruh teks yang dapat tampil ke pengguna harus dimasukkan ke dalam file `.properties` modul masing-masing.
