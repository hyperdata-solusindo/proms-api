# Design Specification: OpenTelemetry Tracing & Sentry Error Logging

- **Date**: 2026-11-03
- **Author**: AI Assistant & Engineering Team
- **Status**: Validated (Ready for Implementation Planning)
- **Target Repository**: `hyperdata-solusindo/proms-api-java`
- **Scope**: Cross-Cutting Observability Subsystem (`common/observability/`) & Architecture Rules Update

---

## 1. Overview & Objectives

Implement an enterprise-grade observability and error-tracking pipeline in `proms-api-java` using **OpenTelemetry (OTel)** and **Sentry**, adhering to the 12 Golden Rules in `AGENTS.md`.

### Core Goals:
1. **Dual Pipeline Observability**:
   - **OpenTelemetry (OTel)**: Handles distributed tracing and span propagation via Spring Boot native Micrometer Tracing and OTLP Exporter (HTTP/Protobuf to OTel Collector / APM).
   - **Sentry**: Captures all application exceptions (5xx as `ERROR`, 4xx as `WARNING`/`INFO`) with contextual metadata, breadcrumbs, and correlated OTel `trace_id`.
2. **Unified Project & Service Identity**:
   - Single variable `PROJECT_NAME` / `APP_NAME` in `.env` automatically configures `spring.application.name`, OTel `service.name`, Sentry `server-name`/tags, and Logback context.
3. **Auto-Instrumentation for Business Layers**:
   - Spring AOP Aspect (`AutoTraceAspect`) automatically creates child spans for methods in `module..service` and `module..facade.internal` without requiring manual `@Observed` annotations on business code.
4. **Enterprise-Grade Sensitive Data Masking (PCI-DSS 4.0 & UU PDP Indonesia No. 27/2022)**:
   - Covers Credentials, Financial Data (Credit Cards, CVV, Bank Accounts), Government IDs (NIK, NPWP, Passport, BPJS), PII (Email, Phone), PEM Private Keys, and raw JWT signatures.
   - Configurable & extensible via `.env` / `application.properties`.
5. **Trace ID Propagation to HTTP Clients**:
   - Automatic injection of `X-Trace-Id` response header on all REST responses, enabling frontend/QA to report specific traces for instant investigation.
6. **Environment-Driven Configuration (`.env` support)**:
   - All parameters configurable via `.env` with safe, robust defaults for local development.
7. **Multi-Environment Logging**:
   - Local/Dev: Colored human-readable console logs with `[traceId, spanId]` and masking.
   - Production: Structured one-line JSON (Logstash/ECS format) with Sentry appender forwarding.
8. **Rules & Documentation Governance**:
   - Introduce `Rule 13: Observability, Logging & Sensitive Data Masking`, update `Rule 06`, and enhance `AGENTS.md` DoD checklist.

---

## 2. Directives & Invariants Compliance

| Directives Rule | Compliance Design |
| :--- | :--- |
| **Rule 1: 2-Layer Modular Monolith** | Placed exclusively in `com.hyperdata.proms.common.observability.*`. Does not pollute domain modules. |
| **Rule 2: Facade Contract** | Facade interfaces remain pure Java interfaces; only internal implementations in `*.facade.internal.*` receive automatic span wrapping. |
| **Rule 3 & 4: Record DTOs & Immutability** | No entity leakage; any telemetry context or metadata records use Java 21 `record`. |
| **Rule 6: Constructor Injection** | `@RequiredArgsConstructor` on all Spring configuration beans and components. |
| **Rule 9: ArchUnit Guardrails** | Verified against `ArchitectureTest.java`; no prohibited inter-package dependencies. |
| **Rule 11: Inner-loop Testing** | Targeted tests for masking converters, Sentry filters, and AOP auto-tracing run in < 3 seconds. |

---

## 3. Package & Directory Structure

```text
src/main/
├── java/com/hyperdata/proms/common/
│   ├── exception/
│   │   └── GlobalExceptionHandler.java       # Updated with Sentry capture + OTel trace correlation
│   └── observability/
│       ├── aspect/
│       │   └── AutoTraceAspect.java           # AOP pointcut for Service & Facade methods
│       ├── config/
│       │   ├── ObservabilityProperties.java   # Type-safe @ConfigurationProperties for observability & masking
│       │   ├── OpenTelemetryConfig.java       # Tracer, SpanProcessor, OTLP Exporter, Resource Attributes
│       │   └── SentryConfig.java              # SentryOptions, BeforeSendCallback sanitization
│       ├── filter/
│       │   ├── TraceIdHeaderFilter.java       # Injects X-Trace-Id into HTTP response headers
│       │   └── SensitiveHeaderFilter.java     # Redacts Authorization, Cookie, API Keys in HTTP tracing
│       └── masking/
│           ├── MaskingPatternConverter.java   # Custom Logback CompositeConverter
│           └── SensitiveDataMasker.java       # Pure utility with enterprise regex patterns & extensibility
└── resources/
    ├── logback-spring.xml                     # Profile-aware logging configuration
    └── application.properties                 # Bound to .env variables with fallbacks
```

---

## 4. Dependencies Specification (`build.gradle.kts`)

```kotlin
// OpenTelemetry & Micrometer Tracing
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")

// Sentry Spring Boot & Logback Integration
implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.0")
implementation("io.sentry:sentry-logback:7.18.0")

// Structured Logging for Production JSON logs
runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

// Spring AOP for AutoTraceAspect
implementation("org.springframework.boot:spring-boot-starter-aop")
```

---

## 5. Configuration & `.env` Variable Mapping

### 5.1 Unified Project Name & Observability in `application.properties`

```properties
# ===================================================================
# Application & Service Identity
# ===================================================================
spring.application.name=${PROJECT_NAME:${APP_NAME:proms-api}}

# ===================================================================
# OpenTelemetry & Tracing (.env mapped)
# ===================================================================
management.tracing.enabled=${OTEL_TRACING_ENABLED:true}
management.tracing.sampling.probability=${OTEL_SAMPLING_PROBABILITY:1.0}
management.otlp.tracing.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
management.otlp.tracing.timeout=${OTEL_TIMEOUT_SECONDS:2s}

# OTel Standard Resource Attributes
management.opentelemetry.resource-attributes.service.name=${spring.application.name}
management.opentelemetry.resource-attributes.service.version=${APP_VERSION:0.0.1}
management.opentelemetry.resource-attributes.deployment.environment=${SPRING_PROFILES_ACTIVE:local}

# ===================================================================
# Sentry Error Reporting (.env mapped)
# ===================================================================
sentry.dsn=${SENTRY_DSN:}
sentry.environment=${SENTRY_ENVIRONMENT:${SPRING_PROFILES_ACTIVE:local}}
sentry.server-name=${spring.application.name}
sentry.release=${spring.application.name}@${APP_VERSION:0.0.1}
sentry.traces-sample-rate=${SENTRY_TRACES_SAMPLE_RATE:1.0}
sentry.send-default-pii=false
sentry.exception-resolver-order=-2147483648

# ===================================================================
# Extensible Masking Configuration (.env mapped)
# ===================================================================
proms.observability.masking.enabled=${MASKING_ENABLED:true}
proms.observability.masking.additional-keys=${MASKING_ADDITIONAL_KEYS:}
```

### 5.2 Template `.env.example`
```bash
# Application Identity
PROJECT_NAME=proms-api
APP_VERSION=0.0.1
SPRING_PROFILES_ACTIVE=local

# OpenTelemetry Configuration
OTEL_TRACING_ENABLED=true
OTEL_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
OTEL_TIMEOUT_SECONDS=2s

# Sentry Configuration
SENTRY_DSN=
SENTRY_ENVIRONMENT=local
SENTRY_TRACES_SAMPLE_RATE=1.0

# Masking Extensibility
MASKING_ENABLED=true
MASKING_ADDITIONAL_KEYS=secretQuestion,motherMaidenName
```

---

## 6. Enterprise-Grade Sensitive Data Masking Matrix

To satisfy **PCI-DSS 4.0**, **GDPR**, and **UU Pelindungan Data Pribadi (UU PDP Indonesia No. 27/2022)**, the masking engine handles the following classifications:

| Category | Target Fields / Data | Pattern / Detection | Masked Format |
| :--- | :--- | :--- | :--- |
| **Credentials & Auth** | Password, Token, Secret, API Key, PIN, Private Key | `(?i)(password\|token\|secret\|apiKey\|authorization\|pin\|clientSecret)` | `***MASKED***` |
| **Private Keys & Certs** | PEM RSA / EC / Private Keys | `-----BEGIN (?:RSA\|EC\|DSA)? ?PRIVATE KEY-----[\s\S]+?-----END ...-----` | `[MASKED_PRIVATE_KEY]` |
| **Raw JWT Tokens** | JWT Signatures in text | `\beyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]+\b` | `eyJ...***MASKED_JWT***` |
| **Card Payment (PCI-DSS)** | Credit Card PAN (Visa, Master, Amex, JCB) | 13-19 digits with Luhn check or key `(cardNumber\|ccNumber\|pan)` | `****-****-****-1234` |
| **Card Security (PCI-DSS)**| CVV, CVC, CID | Key `(cvv\|cvc\|cid)`: 3-4 digits | `***` |
| **Card Expiry** | Expire Date | Key `(expDate\|expiryDate\|cardExp)` | `**/**` |
| **Bank Accounts** | Nomor Rekening Bank / IBAN | Key `(accountNumber\|noRekening\|rekeningBank)` | `*******1234` |
| **Indonesian Identity (UU PDP)**| NIK / KTP (16 Digits) | Key `(nik\|ktp\|identityNumber)` or 16 digit sequence | `3201************` |
| **Tax ID (UU PDP)** | NPWP (15-16 Digits) | Key `(npwp\|taxId)` or pattern `\d{2}\.\d{3}\.\d{3}\.\d-\d{3}\.\d{3}` | `01.***.***.*-***.***` |
| **Passports & BPJS** | Passport Number, No. BPJS | Key `(passportNumber\|noPaspor\|noBpjs)` | `P*******` / `0001********` |
| **Personal Contacts (PII)**| Phone / Mobile Number | `\b(?:\+?62\|08)[0-9]{8,12}\b` or key `(phone\|noHp)` | `+62812******78` |
| **Personal Email (PII)** | Email Address | `\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b` | `k****n@domain.com` |

### Multi-Layer Enforcement:
1. **Layer 1: Logback `%mask(...)`**: Intercepts console and JSON logs before output.
2. **Layer 2: OTel WebMvc Header Filter**: Redacts `Authorization`, `Cookie`, `X-Api-Key`, and query parameters.
3. **Layer 3: Sentry `BeforeSendCallback`**: Strips sensitive headers, cookies, and sanitizes payload breadcrumbs.
4. **Layer 4: Extensibility Hook**: Additional keys specified in `MASKING_ADDITIONAL_KEYS` are merged into the compiled regex at startup.

---

## 7. Global Exception Handler, Trace Headers & Sentry Categorization

### 7.1 HTTP Response Header `X-Trace-Id`
`TraceIdHeaderFilter` (registered as an `OncePerRequestFilter`) retrieves the active trace ID from `Tracer.currentSpan().context().traceId()` and attaches it to the HTTP response header:
```http
HTTP/1.1 200 OK
X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
Content-Type: application/json
```

### 7.2 Sentry Categorization in `GlobalExceptionHandler`
```java
@ExceptionHandler(ValidationException.class)
public ResponseEntity<RestResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
    captureToSentry(ex, SentryLevel.WARNING, "validation");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(RestResponse.error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors()));
}

@ExceptionHandler(BaseException.class)
public ResponseEntity<RestResponse<Void>> handleBaseException(BaseException ex) {
    SentryLevel level = ex.getHttpStatus().is4xxClientError() ? SentryLevel.WARNING : SentryLevel.ERROR;
    captureToSentry(ex, level, ex.getClass().getSimpleName());
    return ResponseEntity.status(ex.getHttpStatus())
        .body(RestResponse.error(ex.getHttpStatus(), ex.getMessage()));
}

@ExceptionHandler(Exception.class)
public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unhandled internal server error: ", ex);
    captureToSentry(ex, SentryLevel.ERROR, "unhandled_internal");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(RestResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred"));
}
```

Metadata scoped to Sentry:
- `error.category` tag (`validation`, `business`, `unhandled_internal`)
- `otel.trace_id` tag from `Tracer.currentSpan().context().traceId()`
- Application name (`spring.application.name`)
- Authenticated user ID (if available via `SecurityContextHelper`) without leaking credentials.

---

## 8. Automated Business Tracing (Spring AOP)

In `com.hyperdata.proms.common.observability.aspect.AutoTraceAspect`:
- Intercepts all public methods in:
  - `com.hyperdata.proms.module..service..*`
  - `com.hyperdata.proms.module..facade.internal..*`
- Wraps each invocation inside an `io.micrometer.tracing.ScopedSpan` named `<ClassName>#<MethodName>`.
- Tags the span with `code.namespace` and `code.function`.
- Records any thrown exception on the span before rethrowing.

Zero modification required in domain modules.

---

## 9. Logback Configuration (`logback-spring.xml`)

1. **Conversion Rule**:
   ```xml
   <conversionRule conversionWord="mask"
                   converterClass="com.hyperdata.proms.common.observability.masking.MaskingPatternConverter" />
   ```
2. **Profile `local`, `dev`**:
   - Console appender using ANSI colors:
     `%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%X{traceId:-},%X{spanId:-}]){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} : %mask(%m%n)`
3. **Profile `prod`**:
   - `net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder`
   - Injects `traceId`, `spanId`, formatted timestamp, MDC context, and masked message.
   - Sentry appender threshold set to `WARN` / `ERROR`.

---

## 10. Rules & Governance Updates (Architecture Standards)

To ensure long-term architectural adherence by all developers and AI agents, the following documentation updates are part of this design:

### 10.1 New Rule: `docs/rules/13-observability-and-logging.md`
Creates explicit standards for:
- SLF4J Logger declaration (`private static final Logger log = LoggerFactory.getLogger(X.class)`).
- Strict log levels (`DEBUG`, `INFO`, `WARN`, `ERROR`).
- Sensitive data masking mandate (PCI-DSS & UU PDP) and prohibition of plain credential logging.
- AOP auto-tracing boundaries and MDC propagation guidelines.

### 10.2 Updated Rule: `docs/rules/06-api-and-error-handling.md`
- Documents the `X-Trace-Id` HTTP response header for error traceability.
- Formalizes the Sentry error categorization policy (`ERROR` for 5xx, `WARNING` for 422/400, `INFO` for 404).

### 10.3 Updated Directives: `AGENTS.md`
- Adds **Rule 13 (Observability, Logging & Sensitive Data Masking)** to The 12 Golden Rules (expanding to 13).
- Updates Section 4 (Companion Detailed Rules) with link to `13-observability-and-logging.md`.
- Updates Section 6 (Definition of Done Checklist):
  - `- [ ] Observability: No unmasked sensitive data/credentials in logs, and masking unit tests pass.`
  - `- [ ] Distributed Tracing: X-Trace-Id header attached and OTel context active.`

---

## 11. Testing & Verification Plan

1. **Unit Tests (`src/test/java/com/hyperdata/proms/common/observability/`)**:
   - `SensitiveDataMaskerTest`: Tests masking for Credit Card, CVV, NIK, NPWP, Rekening, Email, Phone, Passwords, PEM Keys, and JWT tokens.
   - `AutoTraceAspectTest`: Verifies child span creation and error tagging during service method execution.
   - `GlobalExceptionHandlerSentryTest`: Asserts Sentry scope tagging, trace correlation, and correct SentryLevel categorization.
   - `TraceIdHeaderFilterTest`: Verifies `X-Trace-Id` header is correctly injected into HTTP responses.
2. **ArchUnit Guardrail Verification**:
   - `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
3. **Targeted Observability Test**:
   - `./gradlew test --tests "com.hyperdata.proms.common.observability.*"`
4. **Code Quality & Formatting**:
   - `./gradlew spotlessApply && ./gradlew check`
