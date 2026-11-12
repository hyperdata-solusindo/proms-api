# Distributed Hybrid Rate Limiter & Concurrency Throttler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun sistem keamanan *Distributed Hybrid Rate Limiter & In-Flight Concurrency Throttler* (Two-Tier: Pre-Auth Edge Gate + Post-Auth Identity Gate) bertenaga Redis dan Bucket4j untuk melindungi endpoint publik/auth dan API bisnis terautentikasi.

**Architecture:** Menerapkan pertahanan 2-Tier berlapis:
1. **Tier 1 (Pre-Auth Edge Gate)**: `PreAuthRateLimitingFilter` ditempatkan sebelum `TokenAuthenticationFilter` untuk membatasi IP flood dan DoS tanpa menyentuh database/token verification.
2. **Tier 2 (Post-Auth Identity Gate)**: `PostAuthRateLimitingFilter` ditempatkan setelah `TokenAuthenticationFilter` untuk membatasi User ID, Tenant ID (anti-noisy neighbor), dan Account Target Email.
3. **Mekanisme Hybrid**: Menggabungkan *Token Bucket* (Bucket4j untuk pace/volume request per waktu) dan *In-Flight Semaphore* (Redis atomic INCR/DECR untuk konkurensi simultan yang slotnya langsung kosong kembali setelah response selesai dikirim di blok `finally`).
4. **API Envelope Compliance**: Menghasilkan error envelope standar `RestResponse.error` dengan HTTP 429 Too Many Requests, kode terpusat `RATE_LIMIT_EXCEEDED`, serta header standar `Retry-After` dan `X-RateLimit-*`.

**Tech Stack:** Java 21, Spring Boot 4.1.x, Spring Data Redis, Lettuce, Bucket4j (`bucket4j_jdk17-core`, `bucket4j_jdk17-lettuce`), Redis 7 Alpine, Spring Security 6, JUnit 5, Mockito, MockMvc.

**Spec:** [`docs/superpowers/specs/2026-11-11-rate-limiter-throttling-design.md`](../specs/2026-11-11-rate-limiter-throttling-design.md)

## Global Constraints
- Java 21 (Eclipse Temurin 21) & Spring Boot 4.1.x
- 2-Layer Modular Monolith: Kode infrastruktur diletakkan di `com.hyperdata.proms.common.ratelimit`
- Zero Entity Leakage: DTO dan Model murni menggunakan Java 21 `record`
- Constructor Injection: Menggunakan `@RequiredArgsConstructor` pada final fields (zero `@Autowired` pada field)
- Zero Magic Strings: Kode error `"RATE_LIMIT_EXCEEDED"` dideklarasikan di `CommonErrorCode.java`, header di `RateLimitHeaders.java`
- Envelope Standard: HTTP 429 mengembalikan `RestResponse<T>` sesuai Rule 06
- Observability: Response menyertakan `X-Trace-Id` (Rule 13) dan penanganan koneksi Redis bersifat fail-open dengan error logging
- Formatting & Quality Gate: Spotless, Checkstyle, dan ArchUnit wajib lolos (`./gradlew spotlessCheck`, `./gradlew checkstyleMain`, `./gradlew test --tests "*ArchitectureTest*"`)
- Git Policy: Jangan pernah menjalankan `git commit` atau `git push` otomatis

---

### Task 1: Scaffolding Infrastruktur, Dependensi, dan Pesan Lokalisasi

**Files:**
- Modify: `build.gradle.kts:30-70`
- Modify: `docker-compose.yml:1-40`
- Modify: `docker-compose.staging.yml:1-60`
- Modify: `docker-compose.prod.yml:1-60`
- Modify: `src/main/resources/application.properties:1-90`
- Modify: `src/main/java/com/hyperdata/proms/common/exception/CommonErrorCode.java:30-45`
- Modify: `src/main/resources/messages/common/common.properties:20-35`
- Modify: `src/main/resources/messages/common/common_en.properties:20-35`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/CommonErrorCodeRateLimitTest.java`

**Interfaces:**
- Produces: Konstanta `CommonErrorCode.RATE_LIMIT_EXCEEDED`, pesan i18n `error.rate_limit_exceeded`, dan ketersediaan library `spring-boot-starter-data-redis` serta `bucket4j_jdk17-lettuce`.

- [x] **Step 1: Tulis unit test untuk verifikasi konstanta error code RATE_LIMIT_EXCEEDED**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/CommonErrorCodeRateLimitTest.java`:
```java
package com.hyperdata.proms.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommonErrorCodeRateLimitTest {

    @Test
    @DisplayName("Should define RATE_LIMIT_EXCEEDED constant in CommonErrorCode")
    void shouldDefineRateLimitExceededConstant() {
        assertThat(CommonErrorCode.RATE_LIMIT_EXCEEDED).isEqualTo("RATE_LIMIT_EXCEEDED");
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.CommonErrorCodeRateLimitTest"
```
Ekspektasi: Gagal kompilasi karena `CommonErrorCode.RATE_LIMIT_EXCEEDED` belum didefinisikan.

- [x] **Step 3: Tambahkan dependensi di build.gradle.kts, service Redis di docker-compose, konfigurasi properti, dan konstanta error code**

1. Tambahkan dependensi di `build.gradle.kts`:
```kotlin
    // Redis & Distributed Rate Limiting (Bucket4j)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.10.1")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.10.1")
```

2. Tambahkan service Redis di `docker-compose.yml`:
```yaml
  redis:
    image: redis:7-alpine
    container_name: proms-redis
    restart: unless-stopped
    command: ["redis-server", "--appendonly", "yes", "--requirepass", "${REDIS_PASSWORD:-platform_redis_pass}"]
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    networks:
      - proms-net
```
*(Tambahkan juga `redis_data:` di bagian `volumes:`)*. Lakukan hal yang sama pada `docker-compose.staging.yml` dan `docker-compose.prod.yml`.

3. Tambahkan properti Redis & Rate Limit di `src/main/resources/application.properties`:
```properties
# ===================================================================
# Redis & Rate Limiter Configuration
# ===================================================================
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:platform_redis_pass}
spring.data.redis.timeout=2000ms

app.rate-limit.enabled=${RATE_LIMIT_ENABLED:true}
# Tier 1: Edge Pre-Auth
app.rate-limit.edge.ip-capacity=100
app.rate-limit.edge.ip-refill-tokens=100
app.rate-limit.edge.ip-refill-duration=1s
app.rate-limit.edge.ip-max-concurrent=20
app.rate-limit.auth.ip-capacity=15
app.rate-limit.auth.ip-refill-tokens=15
app.rate-limit.auth.ip-refill-duration=1m
# Tier 2: Post-Auth
app.rate-limit.auth.account-capacity=5
app.rate-limit.auth.account-refill-tokens=5
app.rate-limit.auth.account-refill-duration=1m
app.rate-limit.user.capacity=60
app.rate-limit.user.refill-tokens=60
app.rate-limit.user.refill-duration=1m
app.rate-limit.user.max-concurrent=10
app.rate-limit.tenant.capacity=1000
app.rate-limit.tenant.refill-tokens=1000
app.rate-limit.tenant.refill-duration=1m
app.rate-limit.tenant.max-concurrent=50
app.rate-limit.public-general.capacity=30
app.rate-limit.public-general.refill-tokens=30
app.rate-limit.public-general.refill-duration=1m
```

4. Tambahkan konstanta di `CommonErrorCode.java`:
```java
    // 429
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
```

5. Tambahkan pesan di `src/main/resources/messages/common/common.properties`:
```properties
error.rate_limit_exceeded=Terlalu banyak permintaan. Silakan coba lagi nanti.
error.rate_limit_exceeded.detail=Batas permintaan terlampaui. Silakan tunggu {0} detik sebelum mencoba kembali.
```
dan di `src/main/resources/messages/common/common_en.properties`:
```properties
error.rate_limit_exceeded=Too many requests. Please try again later.
error.rate_limit_exceeded.detail=Rate limit exceeded. Please wait {0} seconds before retrying.
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.CommonErrorCodeRateLimitTest"
```
Ekspektasi: PASS (1 test completed, 0 failed).

- [x] **Step 5: Verifikasi Deliverable Task 1**

Verifikasi `build.gradle.kts` dengan menjalankan `./gradlew checkstyleMain`. Pastikan format dan dependensi ter-resolve dengan baik.

---

### Task 2: Rate Limit Properties & Redis Lettuce Configuration

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/config/RateLimitProperties.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/config/RedisRateLimitConfig.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/config/RateLimitPropertiesTest.java`

**Interfaces:**
- Produces: Bean `RateLimitProperties`, Bean `ProxyManager<String>` (Bucket4j Lettuce), dan Bean `StringRedisTemplate`.

- [x] **Step 1: Tulis unit test untuk verifikasi binding RateLimitProperties**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/config/RateLimitPropertiesTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RateLimitPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(RateLimitProperties.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("Should bind default values for RateLimitProperties")
    void shouldBindDefaultValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            RateLimitProperties properties = context.getBean(RateLimitProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.edge().ipCapacity()).isEqualTo(100);
            assertThat(properties.edge().ipRefillDuration()).isEqualTo(Duration.ofSeconds(1));
            assertThat(properties.user().maxConcurrent()).isEqualTo(10);
            assertThat(properties.tenant().maxConcurrent()).isEqualTo(50);
        });
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.config.RateLimitPropertiesTest"
```
Ekspektasi: Gagal karena class `RateLimitProperties` belum ada.

- [x] **Step 3: Implementasi RateLimitProperties dan RedisRateLimitConfig**

1. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/config/RateLimitProperties.java`:
```java
package com.hyperdata.proms.common.ratelimit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue EdgeProperties edge,
    @DefaultValue AuthProperties auth,
    @DefaultValue UserProperties user,
    @DefaultValue TenantProperties tenant,
    @DefaultValue PublicGeneralProperties publicGeneral) {

    public RateLimitProperties {
        if (edge == null) {
            edge = new EdgeProperties(100, 100, Duration.ofSeconds(1), 20);
        }
        if (auth == null) {
            auth = new AuthProperties(15, 15, Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(1));
        }
        if (user == null) {
            user = new UserProperties(60, 60, Duration.ofMinutes(1), 10);
        }
        if (tenant == null) {
            tenant = new TenantProperties(1000, 1000, Duration.ofMinutes(1), 50);
        }
        if (publicGeneral == null) {
            publicGeneral = new PublicGeneralProperties(30, 30, Duration.ofMinutes(1));
        }
    }

    public record EdgeProperties(
        @DefaultValue("100") long ipCapacity,
        @DefaultValue("100") long ipRefillTokens,
        @DefaultValue("1s") Duration ipRefillDuration,
        @DefaultValue("20") int ipMaxConcurrent) {
    }

    public record AuthProperties(
        @DefaultValue("15") long ipCapacity,
        @DefaultValue("15") long ipRefillTokens,
        @DefaultValue("1m") Duration ipRefillDuration,
        @DefaultValue("5") long accountCapacity,
        @DefaultValue("5") long accountRefillTokens,
        @DefaultValue("1m") Duration accountRefillDuration) {
    }

    public record UserProperties(
        @DefaultValue("60") long capacity,
        @DefaultValue("60") long refillTokens,
        @DefaultValue("1m") Duration refillDuration,
        @DefaultValue("10") int maxConcurrent) {
    }

    public record TenantProperties(
        @DefaultValue("1000") long capacity,
        @DefaultValue("1000") long refillTokens,
        @DefaultValue("1m") Duration refillDuration,
        @DefaultValue("50") int maxConcurrent) {
    }

    public record PublicGeneralProperties(
        @DefaultValue("30") long capacity,
        @DefaultValue("30") long refillTokens,
        @DefaultValue("1m") Duration refillDuration) {
    }
}
```

2. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/config/RedisRateLimitConfig.java`:
```java
package com.hyperdata.proms.common.ratelimit.config;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimitConfig {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public LettuceBasedProxyManager<String> bucketProxyManager(RedisConnectionFactory connectionFactory) {
        if (connectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            lettuceConnectionFactory.start();
            RedisClient nativeClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
            StatefulRedisConnection<String, byte[]> connection = nativeClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
        }
        throw new IllegalStateException("RedisConnectionFactory must be an instance of LettuceConnectionFactory");
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.config.RateLimitPropertiesTest"
```
Ekspektasi: PASS.

- [x] **Step 5: Verifikasi Deliverable Task 2**

Pastikan `./gradlew checkstyleMain` lolos tanpa warning.

---

### Task 3: Model Target, Result, dan Headers Contract

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/constant/RateLimitHeaders.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/model/RateLimitTarget.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/model/RateLimitResult.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/model/RateLimitModelTest.java`

**Interfaces:**
- Produces:
  - `RateLimitHeaders` (konstanta `RETRY_AFTER`, `X_RATELIMIT_LIMIT`, dll.)
  - `RateLimitTarget` (record konfigurasi bucket & semaphore)
  - `RateLimitResult` (record hasil evaluasi)

- [x] **Step 1: Tulis unit test untuk verifikasi model Target dan Result**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/model/RateLimitModelTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitModelTest {

    @Test
    @DisplayName("Should create RateLimitTarget and evaluate RateLimitResult correctly")
    void shouldEvaluateRateLimitTargetAndResult() {
        RateLimitTarget target = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        assertThat(target.bucketKey()).isEqualTo("rl:edge:ip:127.0.0.1");
        assertThat(target.maxConcurrent()).isEqualTo(20);

        RateLimitResult allowedResult = RateLimitResult.allowed(99, 1);
        assertThat(allowedResult.allowed()).isTrue();
        assertThat(allowedResult.remainingTokens()).isEqualTo(99);
        assertThat(allowedResult.currentInFlight()).isEqualTo(1);
        assertThat(allowedResult.waitSeconds()).isZero();

        RateLimitResult rejectedResult = RateLimitResult.rejected(0, 45, "Token bucket empty");
        assertThat(rejectedResult.allowed()).isFalse();
        assertThat(rejectedResult.waitSeconds()).isEqualTo(45);
        assertThat(rejectedResult.rejectedReason()).isEqualTo("Token bucket empty");

        assertThat(RateLimitHeaders.RETRY_AFTER).isEqualTo("Retry-After");
        assertThat(RateLimitHeaders.X_RATELIMIT_LIMIT).isEqualTo("X-RateLimit-Limit");
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.model.RateLimitModelTest"
```
Ekspektasi: Gagal karena class-class tersebut belum ada.

- [x] **Step 3: Implementasi RateLimitHeaders, RateLimitTarget, dan RateLimitResult**

1. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/constant/RateLimitHeaders.java`:
```java
package com.hyperdata.proms.common.ratelimit.constant;

public final class RateLimitHeaders {

    private RateLimitHeaders() {
    }

    public static final String RETRY_AFTER = "Retry-After";
    public static final String X_RATELIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String X_RATELIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String X_RATELIMIT_RESET = "X-RateLimit-Reset";
    public static final String X_RATELIMIT_INFLIGHT = "X-RateLimit-InFlight";
}
```

2. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/model/RateLimitTarget.java`:
```java
package com.hyperdata.proms.common.ratelimit.model;

import java.time.Duration;

public record RateLimitTarget(
    String bucketKey,
    long capacity,
    long refillTokens,
    Duration refillDuration,
    int maxConcurrent) {

    public String inFlightKey() {
        return "inflight:" + bucketKey;
    }
}
```

3. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/model/RateLimitResult.java`:
```java
package com.hyperdata.proms.common.ratelimit.model;

public record RateLimitResult(
    boolean allowed,
    long remainingTokens,
    long waitSeconds,
    int currentInFlight,
    String rejectedReason) {

    public static RateLimitResult allowed(long remainingTokens, int currentInFlight) {
        return new RateLimitResult(true, remainingTokens, 0L, currentInFlight, null);
    }

    public static RateLimitResult rejected(long remainingTokens, long waitSeconds, String reason) {
        return new RateLimitResult(false, remainingTokens, waitSeconds, 0, reason);
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.model.RateLimitModelTest"
```
Ekspektasi: PASS.

- [x] **Step 5: Verifikasi Deliverable Task 3**

Jalankan `./gradlew checkstyleMain`.

---

### Task 4: Client IP Resolver & Key Resolvers (Tier 1 & Tier 2)

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/ClientIpResolver.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/RateLimitKeyResolver.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/EdgeRateLimitResolver.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/UserRateLimitResolver.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/resolver/ClientIpResolverTest.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/resolver/EdgeRateLimitResolverTest.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/resolver/UserRateLimitResolverTest.java`

**Interfaces:**
- Produces:
  - `ClientIpResolver.resolveClientIp(HttpServletRequest)`
  - `RateLimitKeyResolver<T>` interface
  - `EdgeRateLimitResolver` (resolves IP targets)
  - `UserRateLimitResolver` (resolves User & Tenant targets)

- [x] **Step 1: Tulis unit test untuk ClientIpResolver dan EdgeRateLimitResolver**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/resolver/ClientIpResolverTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver ipResolver = new ClientIpResolver();

    @Test
    @DisplayName("Should extract first IP from X-Forwarded-For header")
    void shouldExtractFirstIpFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP if X-Forwarded-For is missing")
    void shouldExtractIpFromXRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.42");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Should fallback to RemoteAddr if headers are absent")
    void shouldFallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        assertThat(ipResolver.resolveClientIp(request)).isEqualTo("127.0.0.1");
    }
}
```

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/resolver/EdgeRateLimitResolverTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class EdgeRateLimitResolverTest {

    private final RateLimitProperties properties = new RateLimitProperties(
        true,
        new RateLimitProperties.EdgeProperties(100, 100, Duration.ofSeconds(1), 20),
        new RateLimitProperties.AuthProperties(15, 15, Duration.ofMinutes(1), 5, 5, Duration.ofMinutes(1)),
        new RateLimitProperties.UserProperties(60, 60, Duration.ofMinutes(1), 10),
        new RateLimitProperties.TenantProperties(1000, 1000, Duration.ofMinutes(1), 50),
        new RateLimitProperties.PublicGeneralProperties(30, 30, Duration.ofMinutes(1))
    );
    private final ClientIpResolver ipResolver = new ClientIpResolver();
    private final EdgeRateLimitResolver edgeResolver = new EdgeRateLimitResolver(ipResolver, properties);

    @Test
    @DisplayName("Should resolve global edge IP target for regular requests")
    void shouldResolveGlobalEdgeIpTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.setRemoteAddr("192.168.1.10");

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).bucketKey()).isEqualTo("rl:edge:ip:192.168.1.10");
        assertThat(targets.get(0).capacity()).isEqualTo(100);
        assertThat(targets.get(0).maxConcurrent()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should resolve auth login IP target specifically for auth login endpoint")
    void shouldResolveAuthLoginIpTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.10");

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        assertThat(targets).hasSize(2);
        assertThat(targets).anyMatch(t -> t.bucketKey().equals("rl:auth:ip:192.168.1.10") && t.capacity() == 15);
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.resolver.*Test"
```
Ekspektasi: Gagal kompilasi.

- [x] **Step 3: Implementasi ClientIpResolver, RateLimitKeyResolver, EdgeRateLimitResolver, dan UserRateLimitResolver**

1. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/ClientIpResolver.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    public String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            int commaIndex = xForwardedFor.indexOf(',');
            if (commaIndex > 0) {
                return xForwardedFor.substring(0, commaIndex).trim();
            }
            return xForwardedFor.trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
```

2. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/RateLimitKeyResolver.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface RateLimitKeyResolver {

    List<RateLimitTarget> resolve(HttpServletRequest request);
}
```

3. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/EdgeRateLimitResolver.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EdgeRateLimitResolver implements RateLimitKeyResolver {

    private final ClientIpResolver ipResolver;
    private final RateLimitProperties properties;

    @Override
    public List<RateLimitTarget> resolve(HttpServletRequest request) {
        String clientIp = ipResolver.resolveClientIp(request);
        List<RateLimitTarget> targets = new ArrayList<>(2);

        // 1. Global IP Edge Rate Limit
        var edgeProps = properties.edge();
        targets.add(new RateLimitTarget(
            "rl:edge:ip:" + clientIp,
            edgeProps.ipCapacity(),
            edgeProps.ipRefillTokens(),
            edgeProps.ipRefillDuration(),
            edgeProps.ipMaxConcurrent()));

        // 2. Auth Login IP Rate Limit
        String uri = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && (uri.endsWith("/auth/login") || uri.endsWith("/login"))) {
            var authProps = properties.auth();
            targets.add(new RateLimitTarget(
                "rl:auth:ip:" + clientIp,
                authProps.ipCapacity(),
                authProps.ipRefillTokens(),
                authProps.ipRefillDuration(),
                0)); // Concurrency handled by edge
        }

        return targets;
    }
}
```

4. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/resolver/UserRateLimitResolver.java`:
```java
package com.hyperdata.proms.common.ratelimit.resolver;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.security.CurrentUser;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRateLimitResolver implements RateLimitKeyResolver {

    private final RateLimitProperties properties;
    private final ClientIpResolver ipResolver;

    @Override
    public List<RateLimitTarget> resolve(HttpServletRequest request) {
        Optional<CurrentUser> currentUserOpt = SecurityContextHelper.getCurrentUser();
        List<RateLimitTarget> targets = new ArrayList<>(2);

        if (currentUserOpt.isPresent()) {
            CurrentUser user = currentUserOpt.get();

            // 1. User Rate Limit
            var userProps = properties.user();
            targets.add(new RateLimitTarget(
                "rl:user:" + user.userId(),
                userProps.capacity(),
                userProps.refillTokens(),
                userProps.refillDuration(),
                userProps.maxConcurrent()));

            // 2. Tenant Rate Limit (Anti-Noisy Neighbor)
            if (user.activeTenantId() != null) {
                var tenantProps = properties.tenant();
                targets.add(new RateLimitTarget(
                    "rl:tenant:" + user.activeTenantId(),
                    tenantProps.capacity(),
                    tenantProps.refillTokens(),
                    tenantProps.refillDuration(),
                    tenantProps.maxConcurrent()));
            }
        } else {
            // Anonymous public resources (e.g. swagger, docs)
            String clientIp = ipResolver.resolveClientIp(request);
            var publicProps = properties.publicGeneral();
            targets.add(new RateLimitTarget(
                "rl:public:ip:" + clientIp,
                publicProps.capacity(),
                publicProps.refillTokens(),
                publicProps.refillDuration(),
                0));
        }

        return targets;
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.resolver.*Test"
```
Ekspektasi: PASS (semua test lolos).

- [x] **Step 5: Verifikasi Deliverable Task 4**

Jalankan `./gradlew checkstyleMain`.

---

### Task 5: Hybrid Token Bucket & In-Flight Concurrency Service

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/service/RateLimitService.java`
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/service/DefaultRateLimitService.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/service/RateLimitServiceTest.java`

**Interfaces:**
- Produces:
  - `RateLimitService.tryAcquire(RateLimitTarget)`
  - `RateLimitService.releaseInFlight(RateLimitTarget)`
  - Fail-Open handling logic when Redis is down.

- [x] **Step 1: Tulis unit test untuk RateLimitService**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/service/RateLimitServiceTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RateLimitServiceTest {

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BucketProxy bucketProxy;

    private DefaultRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RateLimitProperties properties = new RateLimitProperties(
            true, null, null, null, null, null);
        rateLimitService = new DefaultRateLimitService(proxyManager, redisTemplate, properties);
    }

    @Test
    @DisplayName("Should allow request when token is available and concurrency is under limit")
    @SuppressWarnings("unchecked")
    void shouldAllowWhenTokenAndSlotAvailable() {
        RateLimitTarget target = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        when(proxyManager.builder()).thenReturn(mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class));
        when(proxyManager.getProxy(eq("rl:user:1"), any(Supplier.class))).thenReturn(bucketProxy);

        ConsumptionProbe probe = ConsumptionProbe.consumed(59);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(valueOperations.increment(target.inFlightKey())).thenReturn(3L);

        RateLimitResult result = rateLimitService.tryAcquire(target);
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(59);
        assertThat(result.currentInFlight()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should reject request when in-flight concurrency exceeds limit")
    @SuppressWarnings("unchecked")
    void shouldRejectWhenConcurrencyExceeded() {
        RateLimitTarget target = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 2);
        when(proxyManager.builder()).thenReturn(mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class));
        when(proxyManager.getProxy(eq("rl:user:1"), any(Supplier.class))).thenReturn(bucketProxy);

        ConsumptionProbe probe = ConsumptionProbe.consumed(59);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(valueOperations.increment(target.inFlightKey())).thenReturn(3L); // exceeds 2

        RateLimitResult result = rateLimitService.tryAcquire(target);
        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectedReason()).contains("In-flight concurrency limit exceeded");
        verify(valueOperations).decrement(target.inFlightKey()); // rollback slot
    }

    @Test
    @DisplayName("Should fail-open when Redis throws connection error")
    void shouldFailOpenOnRedisException() {
        RateLimitTarget target = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        when(proxyManager.getProxy(anyString(), any(Supplier.class)))
            .thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        RateLimitResult result = rateLimitService.tryAcquire(target);
        assertThat(result.allowed()).isTrue(); // Fail-open
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.service.RateLimitServiceTest"
```
Ekspektasi: Gagal kompilasi.

- [x] **Step 3: Implementasi RateLimitService dan DefaultRateLimitService**

1. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/service/RateLimitService.java`:
```java
package com.hyperdata.proms.common.ratelimit.service;

import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;

public interface RateLimitService {

    RateLimitResult tryAcquire(RateLimitTarget target);

    void releaseInFlight(RateLimitTarget target);
}
```

2. Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/service/DefaultRateLimitService.java`:
```java
package com.hyperdata.proms.common.ratelimit.service;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultRateLimitService implements RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRateLimitService.class);

    private final LettuceBasedProxyManager<String> proxyManager;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    @Override
    public RateLimitResult tryAcquire(RateLimitTarget target) {
        if (!properties.enabled()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, 0);
        }

        try {
            // 1. Evaluate Token Bucket
            BucketProxy bucket = proxyManager.getProxy(target.bucketKey(), () -> createConfiguration(target));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (!probe.isConsumed()) {
                long waitNanos = probe.getNanosToWaitForRefill();
                long waitSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(waitNanos));
                return RateLimitResult.rejected(0, waitSeconds, "Rate limit exceeded for key: " + target.bucketKey());
            }

            // 2. Evaluate In-Flight Concurrency Slot (if configured)
            int currentInFlight = 0;
            if (target.maxConcurrent() > 0) {
                String inFlightKey = target.inFlightKey();
                Long inFlight = redisTemplate.opsForValue().increment(inFlightKey);
                if (inFlight != null) {
                    currentInFlight = inFlight.intValue();
                    if (currentInFlight == 1) {
                        redisTemplate.expire(inFlightKey, Duration.ofMinutes(2)); // TTL safeguard
                    }
                    if (currentInFlight > target.maxConcurrent()) {
                        // Rollback slot
                        redisTemplate.opsForValue().decrement(inFlightKey);
                        return RateLimitResult.rejected(
                            probe.getRemainingTokens(),
                            2L,
                            "In-flight concurrency limit exceeded for key: " + target.bucketKey());
                    }
                }
            }

            return RateLimitResult.allowed(probe.getRemainingTokens(), currentInFlight);
        } catch (Exception ex) {
            log.error("Rate limiter Redis failure. Failing open for target: {}", target.bucketKey(), ex);
            return RateLimitResult.allowed(Long.MAX_VALUE, 0); // Fail-Open resilience
        }
    }

    @Override
    public void releaseInFlight(RateLimitTarget target) {
        if (target.maxConcurrent() <= 0 || !properties.enabled()) {
            return;
        }

        try {
            String inFlightKey = target.inFlightKey();
            Long remaining = redisTemplate.opsForValue().decrement(inFlightKey);
            if (remaining != null && remaining <= 0) {
                redisTemplate.delete(inFlightKey);
            }
        } catch (Exception ex) {
            log.warn("Failed to decrement in-flight slot for key: {}", target.inFlightKey(), ex);
        }
    }

    private BucketConfiguration createConfiguration(RateLimitTarget target) {
        Bandwidth bandwidth = Bandwidth.classic(
            target.capacity(),
            Refill.intervally(target.refillTokens(), target.refillDuration()));
        return BucketConfiguration.builder()
            .addLimit(bandwidth)
            .build();
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.service.RateLimitServiceTest"
```
Ekspektasi: PASS (3 tests passed).

- [x] **Step 5: Verifikasi Deliverable Task 5**

Jalankan `./gradlew checkstyleMain`.

---

### Task 6: Tier 1 (Pre-Auth Edge) Rate Limiting Filter

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/filter/PreAuthRateLimitingFilter.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/filter/PreAuthRateLimitingFilterTest.java`

**Interfaces:**
- Consumes: `EdgeRateLimitResolver`, `RateLimitService`, `Messages`
- Produces: `PreAuthRateLimitingFilter` (Before `TokenAuthenticationFilter`)

- [x] **Step 1: Tulis unit test untuk PreAuthRateLimitingFilter**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/filter/PreAuthRateLimitingFilterTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.EdgeRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PreAuthRateLimitingFilterTest {

    @Mock
    private EdgeRateLimitResolver edgeResolver;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private PreAuthRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new PreAuthRateLimitingFilter(edgeResolver, rateLimitService, new ObjectMapper());
    }

    @Test
    @DisplayName("Should pass request and release slot in finally block when tokens allowed")
    void shouldPassRequestWhenAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget target = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(request)).thenReturn(List.of(target));
        when(rateLimitService.tryAcquire(target)).thenReturn(RateLimitResult.allowed(99, 1));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitService).releaseInFlight(target);
        assertThat(response.getHeader(RateLimitHeaders.X_RATELIMIT_REMAINING)).isEqualTo("99");
    }

    @Test
    @DisplayName("Should block request and return HTTP 429 when rate limit exceeded")
    void shouldBlockWhenRateLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget target = new RateLimitTarget("rl:edge:ip:127.0.0.1", 100, 100, Duration.ofSeconds(1), 20);
        when(edgeResolver.resolve(request)).thenReturn(List.of(target));
        when(rateLimitService.tryAcquire(target)).thenReturn(RateLimitResult.rejected(0, 30, "Too many requests"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitHeaders.RETRY_AFTER)).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.filter.PreAuthRateLimitingFilterTest"
```
Ekspektasi: Gagal kompilasi.

- [x] **Step 3: Implementasi PreAuthRateLimitingFilter**

Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/filter/PreAuthRateLimitingFilter.java`:
```java
package com.hyperdata.proms.common.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.EdgeRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import com.hyperdata.proms.common.response.ErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.util.Messages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class PreAuthRateLimitingFilter extends OncePerRequestFilter {

    private final EdgeRateLimitResolver edgeResolver;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        List<RateLimitTarget> targets = edgeResolver.resolve(request);
        List<RateLimitTarget> acquiredTargets = new ArrayList<>(targets.size());

        try {
            for (RateLimitTarget target : targets) {
                RateLimitResult result = rateLimitService.tryAcquire(target);
                if (!result.allowed()) {
                    writeRateLimitError(response, result, target);
                    return;
                }
                acquiredTargets.add(target);
                response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
                response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, String.valueOf(result.remainingTokens()));
            }

            filterChain.doFilter(request, response);
        } finally {
            for (RateLimitTarget target : acquiredTargets) {
                rateLimitService.releaseInFlight(target);
            }
        }
    }

    private void writeRateLimitError(
        HttpServletResponse response,
        RateLimitResult result,
        RateLimitTarget target) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(RateLimitHeaders.RETRY_AFTER, String.valueOf(result.waitSeconds()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, "0");

        String message = Messages.get("error.rate_limit_exceeded");
        String detail = Messages.get("error.rate_limit_exceeded.detail", result.waitSeconds());

        ErrorDetail errorDetail = new ErrorDetail(CommonErrorCode.RATE_LIMIT_EXCEEDED, detail);
        RestResponse<Void> errorResponse = RestResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, errorDetail);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.filter.PreAuthRateLimitingFilterTest"
```
Ekspektasi: PASS.

- [x] **Step 5: Verifikasi Deliverable Task 6**

Jalankan `./gradlew checkstyleMain`.

---

### Task 7: Tier 2 (Post-Auth Identity) Rate Limiting Filter

**Files:**
- Create: `src/main/java/com/hyperdata/proms/common/ratelimit/filter/PostAuthRateLimitingFilter.java`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/filter/PostAuthRateLimitingFilterTest.java`

**Interfaces:**
- Consumes: `UserRateLimitResolver`, `RateLimitService`, `Messages`
- Produces: `PostAuthRateLimitingFilter` (After `TokenAuthenticationFilter`)

- [x] **Step 1: Tulis unit test untuk PostAuthRateLimitingFilter**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/filter/PostAuthRateLimitingFilterTest.java`:
```java
package com.hyperdata.proms.common.ratelimit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.UserRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PostAuthRateLimitingFilterTest {

    @Mock
    private UserRateLimitResolver userResolver;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private FilterChain filterChain;

    private PostAuthRateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new PostAuthRateLimitingFilter(userResolver, rateLimitService, new ObjectMapper());
    }

    @Test
    @DisplayName("Should evaluate user & tenant limits and release slots in finally block")
    void shouldEvaluateAndReleaseSlots() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        RateLimitTarget userTarget = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        RateLimitTarget tenantTarget = new RateLimitTarget("rl:tenant:1", 1000, 1000, Duration.ofMinutes(1), 50);

        when(userResolver.resolve(request)).thenReturn(List.of(userTarget, tenantTarget));
        when(rateLimitService.tryAcquire(userTarget)).thenReturn(RateLimitResult.allowed(59, 1));
        when(rateLimitService.tryAcquire(tenantTarget)).thenReturn(RateLimitResult.allowed(999, 1));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitService).releaseInFlight(userTarget);
        verify(rateLimitService).releaseInFlight(tenantTarget);
    }
}
```

- [x] **Step 2: Jalankan test untuk memastikan test gagal (Red Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.filter.PostAuthRateLimitingFilterTest"
```
Ekspektasi: Gagal kompilasi.

- [x] **Step 3: Implementasi PostAuthRateLimitingFilter**

Buat file `src/main/java/com/hyperdata/proms/common/ratelimit/filter/PostAuthRateLimitingFilter.java`:
```java
package com.hyperdata.proms.common.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.CommonErrorCode;
import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import com.hyperdata.proms.common.ratelimit.resolver.UserRateLimitResolver;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import com.hyperdata.proms.common.response.ErrorDetail;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.util.Messages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class PostAuthRateLimitingFilter extends OncePerRequestFilter {

    private final UserRateLimitResolver userResolver;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        List<RateLimitTarget> targets = userResolver.resolve(request);
        List<RateLimitTarget> acquiredTargets = new ArrayList<>(targets.size());

        try {
            for (RateLimitTarget target : targets) {
                RateLimitResult result = rateLimitService.tryAcquire(target);
                if (!result.allowed()) {
                    writeRateLimitError(response, result, target);
                    return;
                }
                acquiredTargets.add(target);
                response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
                response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, String.valueOf(result.remainingTokens()));
            }

            filterChain.doFilter(request, response);
        } finally {
            for (RateLimitTarget target : acquiredTargets) {
                rateLimitService.releaseInFlight(target);
            }
        }
    }

    private void writeRateLimitError(
        HttpServletResponse response,
        RateLimitResult result,
        RateLimitTarget target) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(RateLimitHeaders.RETRY_AFTER, String.valueOf(result.waitSeconds()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_LIMIT, String.valueOf(target.capacity()));
        response.setHeader(RateLimitHeaders.X_RATELIMIT_REMAINING, "0");

        String message = Messages.get("error.rate_limit_exceeded");
        String detail = Messages.get("error.rate_limit_exceeded.detail", result.waitSeconds());

        ErrorDetail errorDetail = new ErrorDetail(CommonErrorCode.RATE_LIMIT_EXCEEDED, detail);
        RestResponse<Void> errorResponse = RestResponse.error(HttpStatus.TOO_MANY_REQUESTS, message, errorDetail);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

- [x] **Step 4: Jalankan kembali unit test (Green Phase)**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.filter.PostAuthRateLimitingFilterTest"
```
Ekspektasi: PASS.

- [x] **Step 5: Verifikasi Deliverable Task 7**

Jalankan `./gradlew checkstyleMain`.

---

### Task 8: SecurityConfig Integration & End-to-End Filter Chain Verification

**Files:**
- Modify: `src/main/java/com/hyperdata/proms/common/security/SecurityConfig.java:20-80`
- Test: `src/test/java/com/hyperdata/proms/common/ratelimit/RateLimitingIntegrationTest.java`
- Test: `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`

**Interfaces:**
- Produces: Integrated Spring Security Filter Chain with `PreAuthRateLimitingFilter` -> `TokenAuthenticationFilter` -> `PostAuthRateLimitingFilter`.

- [x] **Step 1: Daftarkan kedua filter di SecurityConfig.java**

Perbarui `src/main/java/com/hyperdata/proms/common/security/SecurityConfig.java`:
Injeksi `PreAuthRateLimitingFilter` dan `PostAuthRateLimitingFilter`, lalu daftarkan di filter chain:
```java
            .addFilterBefore(preAuthRateLimitingFilter, TokenAuthenticationFilter.class)
            .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(postAuthRateLimitingFilter, TokenAuthenticationFilter.class);
```

- [x] **Step 2: Buat integration test RateLimitingIntegrationTest**

Buat file `src/test/java/com/hyperdata/proms/common/ratelimit/RateLimitingIntegrationTest.java`:
```java
package com.hyperdata.proms.common.ratelimit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyperdata.proms.common.ratelimit.constant.RateLimitHeaders;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit is exceeded at filter level")
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        when(rateLimitService.tryAcquire(any()))
            .thenReturn(RateLimitResult.rejected(0, 45, "Rate limit exceeded"));

        mockMvc.perform(get("/docs").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string(RateLimitHeaders.RETRY_AFTER, "45"))
            .andExpect(jsonPath("$.statusCode").value(429))
            .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
```

- [x] **Step 3: Jalankan Integration Test dan Architecture Test**

Jalankan perintah:
```bash
./gradlew test --tests "com.hyperdata.proms.common.ratelimit.RateLimitingIntegrationTest"
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"
```
Ekspektasi: Semua test PASS.

- [x] **Step 4: Jalankan Format Code & Quality Gate Penuh (Definition of Done)**

Jalankan perintah:
```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain
./gradlew test
```
Ekspektasi: Build SUCCESS, seluruh quality gate (Spotless, Checkstyle, JUnit, ArchUnit) lolos tanpa pelanggaran.

- [x] **Step 5: Verifikasi Deliverable Akhir**

Jalankan `git status` untuk memverifikasi perubahan file bersih dan tidak ada file sampah.
