# Rule 13: Observability, Logging & Sensitive Data Masking Standards

## 1. SLF4J Logger Declaration
Every class performing logging must declare:
```java
private static final Logger log = LoggerFactory.getLogger(TargetClass.class);
```

## 2. Strict Log Levels
- `DEBUG`: Internal technical flow (local testing only).
- `INFO`: Business milestones (e.g. `User registered with ID {}`).
- `WARN`: 4xx client errors, validation failures, recoverable issues.
- `ERROR`: 5xx system failures, unhandled exceptions, database disconnects.

## 3. Sensitive Data Masking (PCI-DSS & UU PDP Indonesia No. 27/2022)
- ❌ **NEVER log plain credentials**: Passwords, tokens, secrets, API keys.
- ❌ **NEVER log full credit card details**: Only last 4 digits allowed (`****-****-****-1234`).
- ❌ **NEVER log raw NIK or NPWP**: Must be redacted using standard patterns.
- All logs automatically pass through `%mask(...)` via `MaskingPatternConverter`.
- Extensible custom keys can be defined via `.env` (`MASKING_ADDITIONAL_KEYS=key1,key2`).

## 4. Distributed Tracing & AOP Auto-Trace
- Public methods in `module..service` and `module..client.impl` are automatically traced into child spans.
- `X-Trace-Id` response header is automatically attached to all HTTP REST responses.
