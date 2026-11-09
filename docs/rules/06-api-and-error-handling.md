# Rule 06: API Contract, Envelopes & Error Handling

All REST endpoints return responses wrapped in `RestResponse<T>` or `PageResponse<T>`.

## 1. Response Envelope Contract

| Field | Tipe | Aturan |
|---|---|---|
| `statusCode` | integer | **Selalu sama** dengan HTTP status. Server tidak pernah mengirim HTTP 200 untuk error |
| `message` | string | Kalimat singkat bahasa Indonesia, aman ditampilkan ke user (toast/sheet) |
| `data` | object / array / `null` | Isi respons. Objek tunggal = objek, daftar = array, tanpa isi = `null`. |
| `meta` | object | Hanya untuk daftar: `nextCursor`, ditambah angka ringkasan bila ada (mis. `unreadCount`, `summary`). Opsi `null` dihilangkan otomatis |
| `error` | object | Hanya untuk error (`statusCode` ≥ 400). Untuk sukses, field ini tidak dikirim |

### Objek `error`
| Field `error` | Tipe | Isi & Kegunaan |
|---|---|---|
| `code` | string | Kode BR (`BR-WF-09`) atau kode teknis (`STALE_VERSION`, `VALIDATION`). **App/Frontend membuat keputusan berdasarkan `code`, bukan `message`** |
| `detail` | string | Penjelasan tambahan yang dapat membantu user memperbaiki input (opsional) |
| `fields` | array | Daftar kesalahan per field untuk `422 VALIDATION` (opsional), berupa `[{ field: string, message: string }]` |
| `current` | object | Data payload terbaru untuk `409 STALE_VERSION` (opsional) |

---

## 2. Success Envelopes (`RestResponse.ok` / `RestResponse.created`)

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

### List with Cursor & Meta
```json
{
  "statusCode": 200,
  "message": "Berhasil memuat daftar",
  "data": [
    { "id": "1", "title": "Task 1" }
  ],
  "meta": {
    "nextCursor": "eyJpZCI6MTB9",
    "unreadCount": 3
  }
}
```

---

## 3. Error Envelopes (`RestResponse.error`)

### 3.1 Kode Error yang Wajib Ditangani App
Kolom `code` di bawah dibaca dari `error.code`.

| Status | `code` | Tampilan di app (brief §5) |
|---|---|---|
| 401 | `UNAUTHENTICATED` | Kembali ke login |
| 403 | `TENANT_ACCESS_REVOKED` | Layar pilih workspace |
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
  "message": "Product with ID [a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11] not found",
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

## 4. Exception Handling
- Throw specific custom exceptions extending `BaseException`:
  - `InvalidCredentialsException` (401 `UNAUTHENTICATED`)
  - `ResourceNotFoundException` (404 `RESOURCE_NOT_FOUND`, `BOARD_NOT_FOUND`, dll)
  - `DuplicateResourceException` (409 `DUPLICATE_RESOURCE`)
  - `StaleVersionException` (409 `STALE_VERSION` dengan payload `current`)
  - `IfMatchRequiredException` (428 `IF_MATCH_REQUIRED`)
  - `ValidationException` (422 `VALIDATION` dengan `fieldErrors`)
  - `BusinessException` (Aturan alur kerja bisnis dinamis, misal `BR-WF-06`, `BR-WF-08`)
- Selalu gunakan konstanta dari `CommonErrorCode` untuk menghindari *typo* pada `code`.
- ❌ **NEVER throw raw `RuntimeException`**.
- All exceptions are captured automatically by `GlobalExceptionHandler`.
- Always supply machine-readable business rule codes (`code`) on `BusinessException` for frontend state transitions.

## 5. Trace Correlation & Sentry Integration
- All HTTP responses include `X-Trace-Id: <traceId>` header for end-to-end debugging.
- Unhandled 500 exceptions are captured in Sentry as `ERROR` with linked `otel.trace_id`.
- Handled client errors (422/400) are captured in Sentry as `WARNING` under tag `error.category`.
