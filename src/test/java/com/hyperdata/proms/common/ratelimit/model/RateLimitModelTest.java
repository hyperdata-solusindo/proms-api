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
        assertThat(target.inFlightKey()).isEqualTo("inflight:rl:edge:ip:127.0.0.1");

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
