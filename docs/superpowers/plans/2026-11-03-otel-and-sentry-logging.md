# OpenTelemetry & Sentry Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement an enterprise-grade observability and error-tracking pipeline in `proms-api-java` with OpenTelemetry distributed tracing, Sentry error monitoring, automated business method tracing (AOP), PCI-DSS/UU PDP multi-pattern data masking, and `X-Trace-Id` response header propagation without touching any domain business modules.

**Architecture:** A non-intrusive dual pipeline located purely within `common/observability/`, `common/exception/`, and standard Spring configuration. OpenTelemetry captures traces and span propagation (OTLP exporter via Micrometer Tracing); Sentry captures categorized application errors; Logback handles multi-environment (Local ANSI vs Prod JSON) with a custom `%mask` converter; and Spring AOP wraps domain Service/Facade invocations into child spans automatically without modifying existing service classes.

**Tech Stack:** Java 21, Spring Boot 4.1.2, Gradle Kotlin DSL, Micrometer Tracing (OTel Bridge), OpenTelemetry OTLP Exporter, Sentry Spring Boot Starter Jakarta 7.18.0, Logstash Logback Encoder 8.0, Spring AOP, Spotless, ArchUnit, JUnit 5, Mockito.

**Spec:** [`docs/superpowers/specs/2026-11-03-otel-and-sentry-logging-design.md`](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-03-otel-and-sentry-logging-design.md)

## Global Constraints
- **Strict Scope Lockdown**: Zero changes to domain modules (`com.hyperdata.proms.module.**`), JPA Entities, Liquibase migrations, or Security Filters.
- Java 21 toolchain and Spring Boot 4.1.2 conventions.
- 2-Layer Modular Monolith: code strictly in `com.hyperdata.proms.common.observability.*` and `com.hyperdata.proms.common.exception.*`.
- Preserved Defaults: `spring.application.name` default remains `api` (`${PROJECT_NAME:api}`) so existing setups are unbroken.
- DTO Immutability: any metadata transfer objects must be Java 21 `record`.
- Constructor Injection: use Lombok `@RequiredArgsConstructor` on final fields. No `@Autowired` fields.
- ArchUnit compliance: no prohibited dependencies or entity leakage.
- Spotless compliance: line width <= 120, 4-space indent, imports alphabetically sorted.
- Git constraint: NEVER run `git commit` or `git push` automatically.

---

### Task 1: Dependencies, Environment Mapping & Observability Configuration

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application.properties`
- Create: `.env.example`

**Interfaces:**
- Produces: Actuator, Micrometer Tracing, OTel, Sentry, Logstash, and AOP dependencies; `.env` configuration mapping for `PROJECT_NAME`, `OTEL_*`, `SENTRY_*`, and `MASKING_*`.

- [ ] **Step 1: Update build.gradle.kts with Observability & AOP Dependencies**

Add only the required starters and libraries to the `dependencies` block of `build.gradle.kts`:
```kotlin
    // Observability: OpenTelemetry & Micrometer Tracing
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Sentry Spring Boot & Logback Integration
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.0")
    implementation("io.sentry:sentry-logback:7.18.0")

    // Structured JSON Logging for Production
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    // Spring AOP for AutoTraceAspect
    implementation("org.springframework.boot:spring-boot-starter-aop")
```

- [ ] **Step 2: Update application.properties with observability configurations and safe default app name**

Keep existing configurations intact and update `spring.application.name` to safely fall back to the existing `api` default:
```properties
# Application name with backward-compatible default
spring.application.name=${PROJECT_NAME:api}

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

- [ ] **Step 3: Create .env.example**

Create `.env.example` at project root with documented defaults:
```bash
# Application Identity
PROJECT_NAME=api
APP_VERSION=0.0.1
SPRING_PROFILES_ACTIVE=local

# OpenTelemetry Tracing
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
MASKING_ADDITIONAL_KEYS=
```

- [ ] **Step 4: Verify dependencies resolution & build compilation**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Verify deliverable and mark step complete**

Review modified files and verify no dependency conflicts.

---

### Task 2: Enterprise Sensitive Data Masking Engine

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/observability/masking/SensitiveDataMasker.java`
- Create: `src/main/java/com/hyperdata/proms/common/observability/masking/MaskingPatternConverter.java`
- Test: `src/test/java/com/hyperdata/proms/common/observability/masking/SensitiveDataMaskerTest.java`

**Interfaces:**
- Produces: `SensitiveDataMasker.mask(String input, Set<String> customKeys)` and `MaskingPatternConverter` for Logback `%mask`.

- [ ] **Step 1: Write the failing unit test for SensitiveDataMasker**

Create `src/test/java/com/hyperdata/proms/common/observability/masking/SensitiveDataMaskerTest.java`:
```java
package com.hyperdata.proms.common.observability.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    @DisplayName("Should mask credentials like password, token, and secret")
    void shouldMaskCredentials() {
        String log = "{\"username\": \"john\", \"password\": \"secret123\", \"token\": \"abcXYZ123\"}";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("\"password\": \"***MASKED***\"");
        assertThat(masked).contains("\"token\": \"***MASKED***\"");
        assertThat(masked).contains("\"username\": \"john\"");
    }

    @Test
    @DisplayName("Should mask credit card preserving only last 4 digits")
    void shouldMaskCreditCard() {
        String log = "Payment processed with card: 4111222233334444 and cvv: 123";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("****-****-****-4444");
        assertThat(masked).contains("***MASKED_CVV***");
    }

    @Test
    @DisplayName("Should mask Indonesian NIK and NPWP")
    void shouldMaskIndonesianIdentities() {
        String log = "User NIK: 3201234567890123 and NPWP: 01.234.567.8-901.000";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("3201************");
        assertThat(masked).contains("01.***.***.*-***.***");
    }

    @Test
    @DisplayName("Should mask phone numbers and email addresses")
    void shouldMaskPhoneAndEmail() {
        String log = "Contact: budi.santoso@hyperdata.com or phone: +6281234567890";
        String masked = SensitiveDataMasker.mask(log);
        assertThat(masked).contains("b****o@hyperdata.com");
        assertThat(masked).contains("+62812******90");
    }

    @Test
    @DisplayName("Should mask raw JWT tokens and PEM private keys")
    void shouldMaskJwtAndPemKey() {
        String jwt = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String pem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0\n-----END RSA PRIVATE KEY-----";

        String maskedJwt = SensitiveDataMasker.mask(jwt);
        String maskedPem = SensitiveDataMasker.mask(pem);

        assertThat(maskedJwt).contains("eyJ***MASKED_JWT***");
        assertThat(maskedPem).isEqualTo("[MASKED_PRIVATE_KEY]");
    }

    @Test
    @DisplayName("Should respect extensible custom keys")
    void shouldMaskCustomKeys() {
        String log = "{\"secretQuestion\": \"mother maiden name\", \"pinAtm\": \"1234\"}";
        String masked = SensitiveDataMasker.mask(log, Set.of("secretQuestion", "pinAtm"));
        assertThat(masked).contains("\"secretQuestion\": \"***MASKED***\"");
        assertThat(masked).contains("\"pinAtm\": \"***MASKED***\"");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.masking.SensitiveDataMaskerTest"`
Expected: Compilation failure or test failure because `SensitiveDataMasker` is not yet implemented.

- [ ] **Step 3: Implement SensitiveDataMasker**

Create `src/main/java/com/hyperdata/proms/common/observability/masking/SensitiveDataMasker.java`:
```java
package com.hyperdata.proms.common.observability.masking;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final String DEFAULT_KEYS = "password|token|secret|accessToken|refreshToken|apiKey|authorization|pin|clientSecret";

    private static final Pattern PEM_KEY_PATTERN = Pattern.compile(
        "-----BEGIN (?:RSA|EC|DSA)? ?PRIVATE KEY-----[\\s\\S]+?-----END (?:RSA|EC|DSA)? ?PRIVATE KEY-----");

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "\\b(eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.)[A-Za-z0-9_-]+\\b");

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d{4}[- ]?){3}(\\d{4})\\b");

    private static final Pattern CVV_PATTERN = Pattern.compile(
        "(?i)\\b(cvv|cvc|cid)\\s*[:=]\\s*[\"']?\\d{3,4}[\"']?\\b");

    private static final Pattern NIK_PATTERN = Pattern.compile(
        "(?i)\\b(nik|ktp|identityNumber)\\s*[:=]\\s*[\"']?(\\d{4})\\d{8}(\\d{4})[\"']?\\b");

    private static final Pattern NPWP_PATTERN = Pattern.compile(
        "\\b(\\d{2})\\.\\d{3}\\.\\d{3}\\.\\d-\\d{3}\\.(\\d{3})\\b");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(\\+?62|08)(\\d{2})\\d{4,6}(\\d{2})\\b");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b([A-Za-z0-9])[A-Za-z0-9._%+-]*([A-Za-z0-9])@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    private SensitiveDataMasker() {}

    public static String mask(String input) {
        return mask(input, Collections.emptySet());
    }

    public static String mask(String input, Set<String> customKeys) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // 1. Mask PEM Private Keys
        result = PEM_KEY_PATTERN.matcher(result).replaceAll("[MASKED_PRIVATE_KEY]");

        // 2. Mask JWT Signatures
        result = JWT_PATTERN.matcher(result).replaceAll("eyJ***MASKED_JWT***");

        // 3. Mask Key-Value credentials
        String combinedKeys = DEFAULT_KEYS;
        if (customKeys != null && !customKeys.isEmpty()) {
            combinedKeys = DEFAULT_KEYS + "|" + String.join("|", customKeys);
        }
        Pattern keyValuePattern = Pattern.compile(
            "(?i)(\"?(?:" + combinedKeys + ")\"?\\s*[:=]\\s*[\"'])([^\"'\\s,;]+)([\"']?)");
        result = keyValuePattern.matcher(result).replaceAll("$1***MASKED***$3");

        // 4. Mask CVV
        result = CVV_PATTERN.matcher(result).replaceAll("$1: ***MASKED_CVV***");

        // 5. Mask Credit Card (Preserve last 4)
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("****-****-****-$1");

        // 6. Mask NIK
        result = NIK_PATTERN.matcher(result).replaceAll("$1: $2************");

        // 7. Mask NPWP
        result = NPWP_PATTERN.matcher(result).replaceAll("$1.***.***.*-***.$2");

        // 8. Mask Phone Number
        result = PHONE_PATTERN.matcher(result).replaceAll("$1$2******$3");

        // 9. Mask Email
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1****$2@$3");

        return result;
    }
}
```

- [ ] **Step 4: Implement MaskingPatternConverter for Logback**

Create `src/main/java/com/hyperdata/proms/common/observability/masking/MaskingPatternConverter.java`:
```java
package com.hyperdata.proms.common.observability.masking;

import ch.qos.logback.classic.pattern.CompositeConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null) {
            return null;
        }
        return SensitiveDataMasker.mask(in);
    }
}
```

- [ ] **Step 5: Run tests and verify they pass**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.masking.SensitiveDataMaskerTest"`
Expected: `BUILD SUCCESSFUL` with all 6 tests passing.

- [ ] **Step 6: Verify deliverable**

Check test execution time (< 1s) and ensure zero external state mutation.

---

### Task 3: OpenTelemetry Tracing & TraceId Response Filter

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/observability/config/ObservabilityProperties.java`
- Create: `src/main/java/com/hyperdata/proms/common/observability/filter/TraceIdHeaderFilter.java`
- Test: `src/test/java/com/hyperdata/proms/common/observability/filter/TraceIdHeaderFilterTest.java`

**Interfaces:**
- Produces: `TraceIdHeaderFilter` attaching `X-Trace-Id` response header to all HTTP responses.

- [ ] **Step 1: Write failing test for TraceIdHeaderFilter**

Create `src/test/java/com/hyperdata/proms/common/observability/filter/TraceIdHeaderFilterTest.java`:
```java
package com.hyperdata.proms.common.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdHeaderFilterTest {

    @Test
    @DisplayName("Should inject X-Trace-Id header into HTTP response when span is active")
    void shouldInjectTraceIdHeaderWhenSpanActive() throws ServletException, IOException {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        TraceIdHeaderFilter filter = new TraceIdHeaderFilter(tracer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdHeaderFilter.TRACE_ID_HEADER))
            .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        verify(filterChain).doFilter(request, response);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.filter.TraceIdHeaderFilterTest"`
Expected: Compilation failure (class `TraceIdHeaderFilter` not found).

- [ ] **Step 3: Implement ObservabilityProperties**

Create `src/main/java/com/hyperdata/proms/common/observability/config/ObservabilityProperties.java`:
```java
package com.hyperdata.proms.common.observability.config;

import java.util.Collections;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proms.observability.masking")
public record ObservabilityProperties(
    boolean enabled,
    Set<String> additionalKeys
) {
    public ObservabilityProperties {
        if (additionalKeys == null) {
            additionalKeys = Collections.emptySet();
        }
    }
}
```

- [ ] **Step 4: Implement TraceIdHeaderFilter**

Create `src/main/java/com/hyperdata/proms/common/observability/filter/TraceIdHeaderFilter.java`:
```java
package com.hyperdata.proms.common.observability.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class TraceIdHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && currentSpan.context() != null) {
            String traceId = currentSpan.context().traceId();
            if (traceId != null && !traceId.isBlank()) {
                response.setHeader(TRACE_ID_HEADER, traceId);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 5: Run filter test and verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.filter.TraceIdHeaderFilterTest"`
Expected: `BUILD SUCCESSFUL` (1 test passes).

---

### Task 4: Spring AOP AutoTraceAspect for Business Services & Facades

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/observability/aspect/AutoTraceAspect.java`
- Test: `src/test/java/com/hyperdata/proms/common/observability/aspect/AutoTraceAspectTest.java`

**Interfaces:**
- Produces: AOP Aspect automatically wrapping `com.hyperdata.proms.module..service..*` and `com.hyperdata.proms.module..facade.internal..*` in child spans without modifying domain classes.

- [ ] **Step 1: Write failing test for AutoTraceAspect**

Create `src/test/java/com/hyperdata/proms/common/observability/aspect/AutoTraceAspectTest.java`:
```java
package com.hyperdata.proms.common.observability.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoTraceAspectTest {

    @Test
    @DisplayName("Should create scoped span with method name and proceed joinpoint")
    void shouldCreateSpanAndProceed() throws Throwable {
        Tracer tracer = mock(Tracer.class);
        ScopedSpan span = mock(ScopedSpan.class);
        when(tracer.startScopedSpan(anyString())).thenReturn(span);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn((Class) String.class);
        when(signature.getName()).thenReturn("testMethod");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("result");

        AutoTraceAspect aspect = new AutoTraceAspect(tracer);
        Object result = aspect.traceBusinessMethod(pjp);

        assertThat(result).isEqualTo("result");
        verify(tracer).startScopedSpan("String#testMethod");
        verify(span).tag("code.namespace", "java.lang.String");
        verify(span).tag("code.function", "testMethod");
        verify(span).end();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.aspect.AutoTraceAspectTest"`
Expected: Compilation failure (`AutoTraceAspect` class not found).

- [ ] **Step 3: Implement AutoTraceAspect**

Create `src/main/java/com/hyperdata/proms/common/observability/aspect/AutoTraceAspect.java`:
```java
package com.hyperdata.proms.common.observability.aspect;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AutoTraceAspect {

    private final Tracer tracer;

    @Pointcut("execution(public * com.hyperdata.proms.module..service..*(..)) || " +
              "execution(public * com.hyperdata.proms.module..facade.internal..*(..))")
    public void businessMethods() {}

    @Around("businessMethods()")
    public Object traceBusinessMethod(ProceedingJoinPoint pjp) throws Throwable {
        Class<?> declaringType = pjp.getSignature().getDeclaringType();
        String methodName = pjp.getSignature().getName();
        String spanName = declaringType.getSimpleName() + "#" + methodName;

        ScopedSpan span = tracer.startScopedSpan(spanName);
        try {
            span.tag("code.namespace", declaringType.getName());
            span.tag("code.function", methodName);
            return pjp.proceed();
        } catch (Throwable ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
```

- [ ] **Step 4: Run aspect test and verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.aspect.AutoTraceAspectTest"`
Expected: `BUILD SUCCESSFUL` with all tests passing.

---

### Task 5: Sentry Integration & GlobalExceptionHandler Enhancement

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/observability/config/SentryConfig.java`
- Modify: `src/main/java/com/hyperdata/proms/common/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/hyperdata/proms/common/exception/GlobalExceptionHandlerSentryTest.java`

**Interfaces:**
- Produces: Integration of `Sentry.captureException` into `GlobalExceptionHandler` with level categorization (`ERROR`, `WARNING`, `INFO`) and `otel.trace_id` correlation.

- [ ] **Step 1: Write test for GlobalExceptionHandler Sentry integration**

Create `src/test/java/com/hyperdata/proms/common/exception/GlobalExceptionHandlerSentryTest.java`:
```java
package com.hyperdata.proms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.response.RestResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerSentryTest {

    private Tracer tracer;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("sample-trace-id-12345");

        handler = new GlobalExceptionHandler(tracer);
    }

    @Test
    @DisplayName("Should handle ValidationException with 422 and return error envelope")
    void shouldHandleValidationException() {
        ValidationException ex = new ValidationException(Map.of("email", "Email is required"));
        ResponseEntity<RestResponse<Map<String, String>>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().errors()).containsEntry("email", "Email is required");
    }

    @Test
    @DisplayName("Should handle GenericException with 500 and log internal error")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Database timeout");
        ResponseEntity<RestResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).contains("unexpected internal server error");
    }
}
```

- [ ] **Step 2: Implement SentryConfig**

Create `src/main/java/com/hyperdata/proms/common/observability/config/SentryConfig.java`:
```java
package com.hyperdata.proms.common.observability.config;

import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            if (event.getRequest() != null && event.getRequest().getHeaders() != null) {
                event.getRequest().getHeaders().remove("Authorization");
                event.getRequest().getHeaders().remove("Cookie");
                event.getRequest().getHeaders().remove("Set-Cookie");
                event.getRequest().getHeaders().remove("X-Api-Key");
            }
            return event;
        };
    }
}
```

- [ ] **Step 3: Update GlobalExceptionHandler with Sentry capture & Tracer injection**

Update `src/main/java/com/hyperdata/proms/common/exception/GlobalExceptionHandler.java`:
```java
package com.hyperdata.proms.common.exception;

import com.hyperdata.proms.common.response.RestResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Tracer tracer;

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<RestResponse<Map<String, String>>> handleValidationException(ValidationException ex) {
        captureToSentry(ex, SentryLevel.WARNING, "validation");
        RestResponse<Map<String, String>> response = RestResponse.error(
            HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<RestResponse<Void>> handleBaseException(BaseException ex) {
        SentryLevel level = ex.getHttpStatus().is4xxClientError() ? SentryLevel.WARNING : SentryLevel.ERROR;
        captureToSentry(ex, level, ex.getClass().getSimpleName());

        RestResponse<Void> response = RestResponse.error(ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);
        captureToSentry(ex, SentryLevel.ERROR, "unhandled_internal");

        RestResponse<Void> response = RestResponse.error(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private void captureToSentry(Throwable ex, SentryLevel level, String category) {
        Sentry.withScope(scope -> {
            scope.setLevel(level);
            scope.setTag("error.category", category);
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                scope.setTag("otel.trace_id", currentSpan.context().traceId());
            }
            Sentry.captureException(ex);
        });
    }
}
```

- [ ] **Step 4: Run exception handler test and verify it passes**

Run: `./gradlew test --tests "com.hyperdata.proms.common.exception.GlobalExceptionHandlerSentryTest"`
Expected: `BUILD SUCCESSFUL` (2 tests pass).

---

### Task 6: Multi-Environment Logback Configuration

**Files:**
- Create: `src/main/resources/logback-spring.xml`

**Interfaces:**
- Produces: Logback configuration providing colored console output in `local`/`dev` and structured JSON logs with Sentry integration in `prod`.

- [ ] **Step 1: Create logback-spring.xml**

Create `src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Register Sensitive Data Masking Converter -->
    <conversionRule conversionWord="mask"
                    converterClass="com.hyperdata.proms.common.observability.masking.MaskingPatternConverter" />

    <!-- Sentry Appender for Logback -->
    <appender name="SENTRY" class="io.sentry.logback.SentryAppender">
        <minimumEventLevel>WARN</minimumEventLevel>
        <minimumBreadcrumbLevel>INFO</minimumBreadcrumbLevel>
    </appender>

    <!-- LOCAL & DEV PROFILE: Human-readable Colored Console with Trace/Span IDs -->
    <springProfile name="!prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%X{traceId:-},%X{spanId:-}]){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} : %mask(%m%n)</pattern>
                <charset>UTF-8</charset>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="SENTRY"/>
        </root>
    </springProfile>

    <!-- PROD PROFILE: Structured Single-Line JSON (Logstash/ECS) -->
    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp>
                        <timeZone>UTC</timeZone>
                    </timestamp>
                    <logLevel/>
                    <threadName/>
                    <loggerName/>
                    <pattern>
                        <pattern>
                            {
                            "traceId": "%X{traceId:-}",
                            "spanId": "%X{spanId:-}",
                            "message": "%mask(%m)"
                            }
                        </pattern>
                    </pattern>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
            <appender-ref ref="SENTRY"/>
        </root>
    </springProfile>
</configuration>
```

- [ ] **Step 2: Verify logback configuration runs smoothly**

Run: `./gradlew test --tests "com.hyperdata.proms.common.exception.GlobalExceptionHandlerSentryTest"`
Expected: Clean startup without Logback configuration errors.

---

### Task 7: Architecture Rules & Directives Update

**Files:**
- Create: `docs/rules/13-observability-and-logging.md`
- Modify: `docs/rules/06-api-and-error-handling.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Produces: Official documentation for logging standards, masking mandate, `X-Trace-Id` headers, and Golden Rule 13.

- [ ] **Step 1: Create docs/rules/13-observability-and-logging.md**

Create `docs/rules/13-observability-and-logging.md`:
```markdown
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
- Public methods in `module..service` and `module..facade.internal` are automatically traced into child spans.
- `X-Trace-Id` response header is automatically attached to all HTTP REST responses.
```

- [ ] **Step 2: Update docs/rules/06-api-and-error-handling.md**

Add Section 4 to `docs/rules/06-api-and-error-handling.md`:
```markdown
## 4. Trace Correlation & Sentry Integration
- All HTTP responses include `X-Trace-Id: <traceId>` header for end-to-end debugging.
- Unhandled 500 exceptions are captured in Sentry as `ERROR` with linked `otel.trace_id`.
- Handled client errors (422/400) are captured in Sentry as `WARNING` under tag `error.category`.
```

- [ ] **Step 3: Update AGENTS.md**

1. In Section 2, add Golden Rule 13:
   `13. **Observability & Sensitive Data Masking**: All logs must follow SLF4J standards with zero plain credential leakage. Methods in Service and Facade are auto-traced via OTel, and error responses attach an `X-Trace-Id` header.`
2. In Section 4, add `13-observability-and-logging.md` to Companion Detailed Rules.
3. In Section 6 (Definition of Done), add:
   - `- [ ] Observability: No unmasked sensitive data/credentials in logs, and masking unit tests pass.`
   - `- [ ] Distributed Tracing: X-Trace-Id header attached and OTel context active.`

- [ ] **Step 4: Verify documentation integrity**

Review all markdown links and formatting.

---

### Task 8: Full Project Verification & Quality Gates

**Files:**
- Test verification across all test suites.

- [ ] **Step 1: Run all targeted observability unit tests**

Run: `./gradlew test --tests "com.hyperdata.proms.common.observability.*"`
Expected: All tests pass.

- [ ] **Step 2: Run all exception handling unit tests**

Run: `./gradlew test --tests "com.hyperdata.proms.common.exception.*"`
Expected: All tests pass.

- [ ] **Step 3: Run ArchUnit architecture tests**

Run: `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`
Expected: `BUILD SUCCESSFUL` (Architecture boundaries respected).

- [ ] **Step 4: Run Spotless Code Formatter**

Run: `./gradlew spotlessApply`
Run: `./gradlew spotlessCheck`
Expected: Formatting compliant with Eclipse JDT standards.

- [ ] **Step 5: Run full project verification**

Run: `./gradlew check`
Expected: `BUILD SUCCESSFUL` across all verification gates.
