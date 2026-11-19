package com.hyperdata.proms.common.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
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
    private ProxyManager<String> proxyManager;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BucketProxy bucketProxy;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RateLimitProperties properties = new RateLimitProperties(
            true, null, null, null, null, null);
        rateLimitService = new RateLimitService(proxyManager, redisTemplate, properties);
    }

    @Test
    @DisplayName("Should allow request when token is available and concurrency is under limit")
    @SuppressWarnings("unchecked")
    void shouldAllowWhenTokenAndSlotAvailable() {
        RateLimitTarget target = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        io.github.bucket4j.distributed.proxy.RemoteBucketBuilder<String> remoteBuilder = mock(
            io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class);
        when(proxyManager.builder()).thenReturn(remoteBuilder);
        when(remoteBuilder.build(eq("rl:user:1"), any(Supplier.class))).thenReturn(bucketProxy);

        ConsumptionProbe probe = ConsumptionProbe.consumed(59L, 0L);
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
        io.github.bucket4j.distributed.proxy.RemoteBucketBuilder<String> remoteBuilder = mock(
            io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class);
        when(proxyManager.builder()).thenReturn(remoteBuilder);
        when(remoteBuilder.build(eq("rl:user:1"), any(Supplier.class))).thenReturn(bucketProxy);

        ConsumptionProbe probe = ConsumptionProbe.consumed(59L, 0L);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
        when(valueOperations.increment(target.inFlightKey())).thenReturn(3L); // exceeds 2

        RateLimitResult result = rateLimitService.tryAcquire(target);
        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectedReason()).contains("In-flight concurrency limit exceeded");
        verify(valueOperations).decrement(target.inFlightKey()); // rollback slot
    }

    @Test
    @DisplayName("Should fail-open when Redis throws connection error")
    @SuppressWarnings("unchecked")
    void shouldFailOpenOnRedisException() {
        RateLimitTarget target = new RateLimitTarget("rl:user:1", 60, 60, Duration.ofMinutes(1), 10);
        when(proxyManager.builder())
            .thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        RateLimitResult result = rateLimitService.tryAcquire(target);
        assertThat(result.allowed()).isTrue(); // Fail-open
    }
}
