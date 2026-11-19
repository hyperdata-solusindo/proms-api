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
