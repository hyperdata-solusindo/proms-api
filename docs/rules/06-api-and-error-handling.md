# Rule 06: API Contract, Envelopes & Error Handling

All REST endpoints return responses wrapped in `RestResponse<T>` or `PageResponse<T>`. Class `RestResponse<T>` uses `@JsonInclude(Include.NON_NULL)` so null fields are never serialized in JSON responses.

## 1. Success Envelope (`RestResponse.ok` / `RestResponse.created`)
```json
{
  "statusCode": 200,
  "message": "Resource retrieved successfully",
  "data": { ... }
}
```
*(Catatan: `traceId` bersifat opsional dan hanya muncul jika disertakan atau di-resolve oleh tracing context).*

## 2. Paginated Envelope (`PageResponse<T>`)
```json
{
  "items": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "isFirst": true,
  "isLast": false
}
```

## 3. Error Envelopes (`RestResponse.error`)

### Client / Validation Error (422 Unprocessable Entity)
```json
{
  "statusCode": 422,
  "message": "Validation failed",
  "data": {
    "email": "Format email tidak valid",
    "password": "Kata sandi tidak boleh kosong"
  },
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

### Business / Resource Error (e.g. 404 Not Found, 409 Conflict)
```json
{
  "statusCode": 404,
  "message": "Product with ID [a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11] not found",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

## 4. Exception Handling
- Throw specific custom exceptions extending `BaseException` (`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessException`, `ValidationException`).
- ❌ **NEVER throw raw `RuntimeException`**.
- All exceptions are captured automatically by `GlobalExceptionHandler`.

## 5. Trace Correlation & Sentry Integration
- All HTTP responses include `X-Trace-Id: <traceId>` header for end-to-end debugging.
- Unhandled 500 exceptions are captured in Sentry as `ERROR` with linked `otel.trace_id`.
- Handled client errors (422/400) are captured in Sentry as `WARNING` under tag `error.category`.
