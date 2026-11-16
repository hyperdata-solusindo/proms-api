package com.hyperdata.proms.common.ratelimit.service;

import com.hyperdata.proms.common.ratelimit.config.RateLimitProperties;
import com.hyperdata.proms.common.ratelimit.model.RateLimitResult;
import com.hyperdata.proms.common.ratelimit.model.RateLimitTarget;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final ProxyManager<String> proxyManager;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitResult tryAcquire(RateLimitTarget target) {
        if (!properties.enabled()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, 0);
        }

        try {
            // 1. Evaluate Token Bucket
            BucketProxy bucket = proxyManager.builder().build(target.bucketKey(), () -> createConfiguration(target));
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
