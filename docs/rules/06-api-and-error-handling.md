# Rule 06: API Contract, Envelopes & Error Handling

All REST endpoints return responses wrapped in `RestResponse<T>` or `PageResponse<T>`.

## 1. Success Envelope (`RestResponse.ok` / `RestResponse.created`)
```json
{
  "success": true,
  "status": 200,
  "message": "Resource retrieved successfully",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-10-30T10:00:00Z"
}
```

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

## 3. Exception Handling
- Throw specific custom exceptions extending `BaseException` (`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessException`, `ValidationException`).
- ❌ **NEVER throw raw `RuntimeException`**.
- All exceptions are captured automatically by `GlobalExceptionHandler`.

## 4. Trace Correlation & Sentry Integration
- All HTTP responses include `X-Trace-Id: <traceId>` header for end-to-end debugging.
- Unhandled 500 exceptions are captured in Sentry as `ERROR` with linked `otel.trace_id`.
- Handled client errors (422/400) are captured in Sentry as `WARNING` under tag `error.category`.
