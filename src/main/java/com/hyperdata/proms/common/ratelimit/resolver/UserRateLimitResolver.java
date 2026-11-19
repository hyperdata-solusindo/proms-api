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
