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
