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
