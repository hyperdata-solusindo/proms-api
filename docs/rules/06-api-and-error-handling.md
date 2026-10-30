# Rule 06: API Contract, Envelopes & Error Handling

All REST endpoints return responses wrapped in `ApiResponse<T>` or `PageResponse<T>`.

## 1. Success Envelope (`ApiResponse.ok` / `ApiResponse.created`)
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
