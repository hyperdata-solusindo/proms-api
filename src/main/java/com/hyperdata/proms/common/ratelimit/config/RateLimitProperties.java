package com.hyperdata.proms.common.ratelimit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true")
    boolean enabled,
    @DefaultValue
    EdgeProperties edge,
    @DefaultValue
    AuthProperties auth,
    @DefaultValue
    UserProperties user,
    @DefaultValue
    TenantProperties tenant,
    @DefaultValue
    PublicGeneralProperties publicGeneral) {

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
        @DefaultValue("100")
        long ipCapacity,
        @DefaultValue("100")
        long ipRefillTokens,
        @DefaultValue("1s")
        Duration ipRefillDuration,
        @DefaultValue("20")
        int ipMaxConcurrent) {
    }

    public record AuthProperties(
        @DefaultValue("15")
        long ipCapacity,
        @DefaultValue("15")
        long ipRefillTokens,
        @DefaultValue("1m")
        Duration ipRefillDuration,
        @DefaultValue("5")
        long accountCapacity,
        @DefaultValue("5")
        long accountRefillTokens,
        @DefaultValue("1m")
        Duration accountRefillDuration) {
    }

    public record UserProperties(
        @DefaultValue("60")
        long capacity,
        @DefaultValue("60")
        long refillTokens,
        @DefaultValue("1m")
        Duration refillDuration,
        @DefaultValue("10")
        int maxConcurrent) {
    }

    public record TenantProperties(
        @DefaultValue("1000")
        long capacity,
        @DefaultValue("1000")
        long refillTokens,
        @DefaultValue("1m")
        Duration refillDuration,
        @DefaultValue("50")
        int maxConcurrent) {
    }

    public record PublicGeneralProperties(
        @DefaultValue("30")
        long capacity,
        @DefaultValue("30")
        long refillTokens,
        @DefaultValue("1m")
        Duration refillDuration) {
    }
}
