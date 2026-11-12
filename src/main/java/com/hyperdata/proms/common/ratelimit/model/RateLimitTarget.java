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
